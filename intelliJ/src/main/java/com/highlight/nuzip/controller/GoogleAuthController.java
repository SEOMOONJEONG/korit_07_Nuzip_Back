package com.highlight.nuzip.controller;

import com.highlight.nuzip.dto.GoogleTokenRequest;
import com.highlight.nuzip.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Google OAuth2 인증서 검증과 JWT 발급 처리 → GoogleAuthService
@RestController
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/api/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleTokenRequest request) throws Exception {
        String jwt = googleAuthService.authenticateByIdToken(request.getIdToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
                .build();
    }
}