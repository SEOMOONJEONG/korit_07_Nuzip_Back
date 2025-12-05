package com.highlight.nuzip.service;

import com.highlight.nuzip.model.EmailVerificationToken;
import com.highlight.nuzip.repository.EmailVerificationTokenRepository;
import com.highlight.nuzip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final GmailOAuthMailSender gmailOAuthMailSender;

    // properties 설정파일에서 값을 읽어옴.
    @Value("${verification.email.expiration-minutes:15}")
    private long expirationMinutes;
    @Value("${verification.email.resend-interval-seconds:60}")
    private long resendIntervalSeconds;
    @Value("${verification.email.valid-after-verify-hours:24}")
    private long validAfterVerifyHours;
    @Value("${verification.email.subject:[NUZIP] 이메일 인증 코드}")
    private String subject;

    // 인증코드 6자리 랜덤 숫자
    private final Random random = new Random();

    // 인증 이메일 보내기
    @Transactional
    public void sendVerificationEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);    // 공백제거, 소문자 통일
        ensureEmailAllowed(email);  // 일반회원 gmail 가입 막음(구글계정으로 가입 유도)
        rejectAlreadyRegistered(email); // 이미 사용중인 이메일 에러 띄움
        enforceResendInterval(email);   // 인증메일 자주 보내는거 방지(60s)

        String code = generateCode();   // 6자리 랜덤 숫자 생성
        LocalDateTime now = LocalDateTime.now();    // 만료 시간 계산

        // DB에 저장
        EmailVerificationToken token = EmailVerificationToken.builder()
                .email(email)
                .code(code)
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build();
        tokenRepository.save(token);

        // 실제로 Gmail API로 메일 발송
        sendMail(email, code);
    }

    // 사용자가 인증코드 입력했을 때 그 코드가 맞는지 확인
    @Transactional
    public void confirmVerification(String rawEmail, String code) {
        // 이메일 + 가장 퇴근 토큰 가져오기
        String email = normalizeEmail(rawEmail);
        EmailVerificationToken token = tokenRepository
                .findTopByEmailAndCodeOrderByCreatedAtDesc(email, code.trim())
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 올바르지 않습니다."));

        // 토큰 만료 여부 확인
        LocalDateTime now = LocalDateTime.now();
        if (token.isExpired(now)) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }
        token.setVerified(true);
        token.setVerifiedAt(now);
    }

    // 인증 여부 판단(인증 결과 사용 표시)
    @Transactional
    public void consumeVerifiedToken(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        EmailVerificationToken token = tokenRepository
                .findTopByEmailAndVerifiedTrueAndConsumedFalseOrderByVerifiedAtDesc(email)
                .orElseThrow(() -> new IllegalStateException("이메일 인증을 먼저 완료해 주세요."));

        LocalDateTime now = LocalDateTime.now();
        if (token.getVerifiedAt() == null ||
                token.getVerifiedAt().isBefore(now.minusHours(validAfterVerifyHours))) {
            throw new IllegalStateException("이메일 인증이 만료되었습니다. 다시 인증해 주세요.");
        }
        token.setConsumed(true);    // 인증 통과
        log.warn("이메일 인증 로직이 개발 테스트를 위해 우회되었습니다: {}", rawEmail);
    }
    // 이메일 입력값 정리
    public String normalizeEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return rawEmail.trim().toLowerCase(Locale.ROOT);    // 공백 제거, 소문자
    }

    // Gmail/Naver은 가입불가 → OAuth 계정으로 가입 유도
    private void ensureEmailAllowed(String email) {
        if (email.endsWith("@gmail.com")) {
            throw new IllegalArgumentException("Gmail 계정은 구글 로그인으로 가입해 주세요.");
        }
    }
    // 이미 가입된 회원일 경우 인증메일 발송 불가
    private void rejectAlreadyRegistered(String email) {
        if (userRepository.existsByUserId(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }

    // 인증 메일 제한(너무 자주 못보내게 / 스팸방지)
    private void enforceResendInterval(String email) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(resendIntervalSeconds);
        long recentCount = tokenRepository.countByEmailAndCreatedAtAfter(email, threshold); // threshold = 지금시간 - 60초
        // 1개 이상 → 잠시 후 다시 시도
        if (recentCount > 0) {
            throw new IllegalStateException("잠시 후에 다시 시도해 주세요. (최대 1분에 한 번)");
        }
    }

    // 실제 발송 메일 내용
    private void sendMail(String email, String code) {

        String logoUrl = "https://raw.githubusercontent.com/SEOMOONJEONG/korit_07_Nuzip_Front/main/src/pages/Nuzip_logo2.png";

        String html = """
            <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
              
              <!-- 전체 컨테이너 -->
              <div style="max-width: 600px; margin: 0 auto; background-color: #f8f9fa; border-radius: 8px; overflow: hidden;">
            
                <!-- 상단 로고 영역 (파란 테마) -->
                <div style="
                    background-color: #E8F0FE;
                    padding: 28px 20px;
                    text-align: center;
                ">
                  <img
                    src=\"%s\"
                    alt=\"Nuzip Logo\"
                    style=\"height: 52px; width: auto; display: inline-block;\">
                </div>
            
                <!-- 본문 영역 (여기부터 흰색 배경으로 변경) -->
                <div style="padding: 40px 30px; color: #111111; text-align: center; background-color: #ffffff;">
            
                  <h2 style="margin-top: 10px; font-size: 22px; font-weight: bold;">
                    확인 코드
                  </h2>
            
                  <!-- 인증코드 -->
                  <div style="
                      color: #000000;
                      padding: 18px 0;
                      margin: 20px auto;
                      width: 200px;
                      font-size: 26px;
                      font-weight: bold;
                      border-radius: 8px;
                      letter-spacing: 6px;
                  ">
                    %s
                  </div>
            
                  <p style="font-size: 13px; margin-top: -5px; opacity: 0.7; color: #868e96;">
                    (이 코드는 전송 5분 후에 만료됩니다.)
                  </p>
            
                  <hr style="margin: 35px 0; border: 0; border-top: 1px solid #d0d4d8;" />
            
                  <p style="font-size: 14px; line-height: 1.6; text-align: left; color: #868e96;">
                    Nuzip은 절대 사용자의 인증코드, 신용카드 또는 은행 계좌 번호를 묻거나 
                    확인하라는 이메일을 보내지 않습니다. 계정 정보를 업데이트하라는 링크가 포함된 
                    의심스러운 이메일을 수신한 경우 링크를 클릭하지 말고 해당 이메일을 신고해 주세요.
                  </p>
            
                </div>
              </div>
            </div>
            """.formatted(logoUrl, code);

        gmailOAuthMailSender.send(email, subject, html);
    }

    // 6자리 숫자 인증 코드 생성
    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}

