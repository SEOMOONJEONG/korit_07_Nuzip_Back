package com.highlight.nuzip.controller;

import com.highlight.nuzip.dto.AccountCredentialsDto;
import com.highlight.nuzip.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// POST /login    (로그인 자격 확인)
// /api/ogin 엔드포인트로 요청을 받고, DTO 검증 → 서비스 호출 → 응답 DTO 반환
// HTTP 레이어를 담당(상태코드, 바인딩, @Valid 검증 등)
// HTTP 입출력
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * POST /login
     * body: { "userId": "...", "password": "..." }
     * 응답: 헤더 Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AccountCredentialsDto credentials) {
        try {
            // 우리 프로젝트는 userId가 username 역할
            var authToken = new UsernamePasswordAuthenticationToken(
                    credentials.getUserId(), credentials.getPassword());

            Authentication auth = authenticationManager.authenticate(authToken);

            String jwt = jwtService.generateToken(auth.getName()); // subject=userId

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)                  // ← 한 문자열이어야 함
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")  // FE에서 헤더 읽도록 노출
                    .build();
        } catch (BadCredentialsException e) {
            // 아이디 또는 비밀번호가 잘못된 경우
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "아이디 또는 비밀번호가 잘못 되었습니다. 아이디와 비밀번호를 정확히 입력해 주세요."));
        }
    }
}