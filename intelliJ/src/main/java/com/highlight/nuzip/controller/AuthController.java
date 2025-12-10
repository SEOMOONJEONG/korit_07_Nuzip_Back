package com.highlight.nuzip.controller;

import com.highlight.nuzip.dto.EmailVerificationConfirmRequest;
import com.highlight.nuzip.dto.EmailVerificationSendRequest;
import com.highlight.nuzip.dto.RegisterRequestDto;
import com.highlight.nuzip.dto.RegisterResponseDto;
import com.highlight.nuzip.service.UserService;
import com.highlight.nuzip.security.JwtService;
import com.highlight.nuzip.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 폼 일반 회원가입 로직만 처리 → UserService
// POST /api/auth/register (회원가입)
// DTO로 받고(@Valid 검증), Service 호출, DTO로 응답


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserService userService, JwtService jwtService, EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/register/check")
    public ResponseEntity<?> checkUserIdAvailability(@RequestParam String userId) {
        userService.assertUserIdAvailable(userId);
        return ResponseEntity.ok(Map.of("available", true));
    }

    @PostMapping("/email/verification/send")
    public ResponseEntity<?> sendEmailVerification(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationService.sendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of("sent", true));
    }

    @PostMapping("/email/verification/confirm")
    public ResponseEntity<?> confirmEmailVerification(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.confirmVerification(request.email(), request.code());
        return ResponseEntity.ok(Map.of("verified", true));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto req) {
        RegisterResponseDto res = userService.register(req);
        // 회원가입과 동시에 로그인 처리: JWT 발급을 헤더로 전달
        String jwt = jwtService.generateToken(res.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization")
                .body(res);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) { // 현재 인증된 사용자(= 로그인된 사용자)”를 자동으로 컨트롤러 메서드에 넣어주는 어노테이션
        if (user == null) {
            // 로그인 안 된 상태
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        var entity = userService.getUserOrThrow(user.getUsername());
        var categories = entity.getNewsCategory() == null
                ? List.<String>of()
                : entity.getNewsCategory().stream()
                .map(Enum::name)
                .toList();
        boolean categoriesSelected = categories.size() == 3;

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "userId", entity.getUserId(),
                "username", entity.getUsername(),
                "provider", entity.getProvider(),
                "categories", categories,
                "categoriesSelected", categoriesSelected,
                "needsCategorySelection", !categoriesSelected
        ));
    }
}