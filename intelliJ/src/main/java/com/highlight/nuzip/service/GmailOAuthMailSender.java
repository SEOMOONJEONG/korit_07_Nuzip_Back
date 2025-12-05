package com.highlight.nuzip.service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

@Component
@RequiredArgsConstructor
@Slf4j
// 토큰 받아서 Gmail API로 실제 이메일 보냄
public class GmailOAuthMailSender {

    // properties 에서 받아옴.
    @Value("${gmail.oauth.client-id}")
    private String clientId;
    @Value("${gmail.oauth.client-secret}")
    private String clientSecret;
    @Value("${gmail.oauth.refresh-token}")
    private String refreshToken;
    @Value("${gmail.oauth.sender}")
    private String sender;

    // OAuth 토큰 요청용 WebClient
    private final WebClient googleAuthClient = WebClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(10))))
            .build();

    // Gmail API 전송용 WebClient
    private final WebClient gmailApiClient = WebClient.builder()
            .baseUrl("https://gmail.googleapis.com/gmail/v1")
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(10))))
            .build();

    // 메일 전송
    public void send(String to, String subject, String textBody) {
        try {
            String accessToken = fetchAccessToken();    // 토큰 받아옴

            // 제목, 내용, charset, 보내는사람 지정
            MimeMessage mimeMessage = createMimeMessage(to, subject, textBody);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            // JavaMail(메시지 생성) 메시지를 Gmail API(발송)가 요구하는 BAse64로 인코딩
            // → Gmail API가 요구하는 메시지 포맷이 Base64url이기 떄문
            mimeMessage.writeTo(buffer);
            String encodedEmail = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(buffer.toByteArray());
            // Gmail API에 POST 요청
            gmailApiClient.post()
                    .uri("/users/me/messages/send")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("raw", encodedEmail))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            log.info("✅ Gmail OAuth2 API로 메일 전송 성공: {}", to);

            // 실패 시 예외처리
        } catch (MessagingException | IOException e) {
            log.error("❌ 메일 생성 실패: {}", to, e);
            throw new IllegalStateException("이메일 전송 준비 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("❌ Gmail API 호출 실패: {}", to, e);
            throw new IllegalStateException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // Access Token 가져오기(리프레시 토큰(서버 전용) → Access Token 교환)
    // 만료시간이 짧은 Access Token으로 보안강화
    private String fetchAccessToken() {
        // OAuth 서버에 /token 엔드포인트에 POST요청
        Map<?, ?> response = googleAuthClient.post()
                // OAuth “Refresh Token Flow” 공식 포맷
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken)
                        .with("grant_type", "refresh_token"))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        // 응답 안에 access_token 키가 없으면 실패로 간주
        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("구글 액세스 토큰 발급에 실패했습니다.");
        }
        // 성공 시 Access Token 반환
        return response.get("access_token").toString();
    }

    // 이메일 내용 작성
    private MimeMessage createMimeMessage(String to, String subject, String textBody) throws MessagingException {
        // JavaMail Session 생성
        Properties props = new Properties();
        Session session = Session.getInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(sender));   // 보내는 사람
        mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));   // 받는사람
        mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name()); // 제목 UTF-8 한글 깨짐 방지
        mimeMessage.setContent(textBody, "text/html; charset=UTF-8");
        log.info("메일 제목 = {}", subject);
        return mimeMessage;
    }
}
