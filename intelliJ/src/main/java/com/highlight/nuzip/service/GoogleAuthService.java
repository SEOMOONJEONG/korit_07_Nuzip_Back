package com.highlight.nuzip.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.highlight.nuzip.model.AuthProvider;
import com.highlight.nuzip.model.User;
import com.highlight.nuzip.repository.UserRepository;
import com.highlight.nuzip.security.GoogleTokenVerifier;
import com.highlight.nuzip.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

// 구글 토큰이 진짜인지 확인하고, 처음 로그인한 사용자는 자동으로 DB에 가입시킨 뒤 JWT 발급.
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // 토큰 검증 후 JWT 발급
    public String authenticateByIdToken(String idToken) throws Exception {
        // 구글 토큰 검증
        Payload payload = googleTokenVerifier.verify(idToken);
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        // 필요한 정보 추출
        String email = (String) payload.get("email");
        Boolean emailVerified = (Boolean) payload.get("email_verified");
        String name = (String) payload.get("name");
        // 이메일 검증 체크
        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            throw new IllegalArgumentException("Email not verified");
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        // 최초 로그인 시 자동 가입
        joinIfAbsent(email, name);

        // JWT subject=userId(email)
        return jwtService.generateToken(email);
    }
    // 리다이렉트 플로우에서 쓰기위한 가입 보장 메서드
    @Transactional
    public User joinIfAbsent(String email, String name) {
        final String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUserId(normalizedEmail).orElseGet(() -> {
            User u = new User();
            u.setUserId(normalizedEmail);
            u.setUsername(name != null ? name : normalizedEmail);
            u.setPassword(passwordEncoder.encode("GOOGLE-" + UUID.randomUUID())); // 소셜전용 더미 비번
            u.setProvider(AuthProvider.OAUTH_GOOGLE);   // 구글 회원인데도 LOCAL로 뜨던 문제 해결
            u.setEmailVerified(true);
            u.setNewsCategory(new HashSet<>());
            return userRepository.save(u);
        });

    }
}