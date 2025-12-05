package com.highlight.nuzip.security;

import com.highlight.nuzip.model.AuthProvider;
import com.highlight.nuzip.model.User;
import com.highlight.nuzip.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// 구글 OAuth2 로그인 "성공 이후의 후처리"
/*
    구글 로그인 성공 시 자동 호출됨
    → 스프링 시큐리티가 onAuthenticationSuccess()를 실행시킴.

    구글이 넘겨준 사용자 정보 가져옴 (OAuth2User)
    → 이메일, 이름, 고유 ID(sub) 등.

    DB에서 해당 이메일이 있는지 확인
    → 없으면 UserRepository로 신규 사용자 자동 등록
    (비밀번호는 “OAUTH2_USER”, 카테고리는 빈 Set으로 저장)

    JWT 토큰 생성 (JwtService)
    → 우리 서버용 인증 토큰 발급 (subject = userId)

    프론트엔드로 리다이렉트하면서 JWT 전달
    → redirectUrl#token=<JWT> 형태로 전달
    (프론트에서는 이걸 파싱해서 sessionStorage 등에 저장함)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository; // ✅ 서비스 대신 레포지토리만 주입
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${oauth2.success.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        log.info("✅ [OAuth2SuccessHandler] 호출됨 - 인증 성공");
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("➡️ 인증된 사용자: {}", oAuth2User.getAttributes());

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        String sub   = oAuth2User.getAttribute("sub"); // fallback용

        // ✅ 우리 시스템에서 userId = 구글 email 사용 (없으면 sub 사용)
        String userId = (email != null) ? email : (sub != null ? sub : oAuth2User.getName());

        // ✅ 최초 로그인 시 DB에 가입(카테고리는 빈 Set으로 초기화!)
        userRepository.findByUserId(userId).orElseGet(() -> {
            User u = User.builder()
                    .userId(userId)
                    .username((name != null && !name.isBlank()) ? name : userId)
                    .password(passwordEncoder.encode("OAUTH2_USER"))          // 더미(폼로그인 미사용)
                    .newsCategory(new HashSet<>())     // ← @NotNull 충족 (중요)
                    .provider(AuthProvider.OAUTH_GOOGLE)
                    .build();
            return userRepository.save(u);
        });

        // ✅ JWT 발급 (subject = userId)
        String jwt = jwtService.generateToken(userId);

        // 전달: 해시 프래그먼트(#token=...) 사용 (프론트에서 파싱해 sessionStorage에 저장)
        String bearerEncoded = URLEncoder.encode("Bearer " + jwt, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String targetUrl = redirectUrl + "#token=" + bearerEncoded;

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}