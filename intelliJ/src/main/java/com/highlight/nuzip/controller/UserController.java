package com.highlight.nuzip.controller;

import com.highlight.nuzip.dto.UpdateMyInfoRequestDto;
import com.highlight.nuzip.dto.VerifyPasswordRequestDto;
import com.highlight.nuzip.dto.VerifyPasswordResponseDto;
import com.highlight.nuzip.model.AuthProvider;
import com.highlight.nuzip.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import com.highlight.nuzip.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;


// 로그인된 사용자의 뉴스 관심 카테고리(선호 분야) 를 DB에 저장하거나 불러오는 역할.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    // ✅ 내 기본 프로필 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인 상태가 아닙니다. 먼저 로그인해 주세요."));
        }

        var entity = userService.getUserOrThrow(principal.getUsername());
        List<String> categories = entity.getNewsCategory() == null
                ? List.of()
                : entity.getNewsCategory().stream()
                .map(Enum::name)
                .toList();

        boolean categoriesSelected = categories.size() == 3;
        var body = new LinkedHashMap<String, Object>();
        body.put("authenticated", true);
        body.put("userId", entity.getUserId());
        body.put("username", entity.getUsername());
        body.put("provider", entity.getProvider());
        body.put("phone", entity.getPhone());
        body.put("birthDate", entity.getBirthDate());
        body.put("categories", categories);
        body.put("categoriesSelected", categoriesSelected);
        body.put("needsCategorySelection", !categoriesSelected);

        return ResponseEntity.ok(body);
    }

    // ✅ 회원가입 2단계: 관심 카테고리 저장
    @PostMapping("/me/categories")
    public ResponseEntity<?> saveMyCategories(
            @AuthenticationPrincipal User principal,
            @RequestBody Map<String, List<String>> body) {

        // 1️⃣ 인증 체크
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "관심 카테고리를 저장하려면 로그인해야 합니다."));
        }

        // 2️⃣ 요청 바디에서 카테고리 목록 추출
        List<String> categories = body.get("categories");

        // 3️⃣ 서비스 호출로 DB 반영
        //  - userRepository에서 해당 유저를 찾고
        //  - 문자열 리스트를 NewsCategory(Enum)로 변환한 뒤
        //  - user.setNewsCategory(new Set<>(...)) 형태로 교체 저장
        userService.updateCategories(principal.getUsername(), categories);

        // 4️⃣ 정상 완료 응답
        return ResponseEntity.ok(Map.of("updated", true));
    }

    // ✅ 회원정보 수정 진입 전 비밀번호 검증 (LOCAL 계정 전용)
    @PostMapping("/me/verify-password")
    public ResponseEntity<?> verifyPassword(@AuthenticationPrincipal User principal,
                                            @Valid @RequestBody VerifyPasswordRequestDto req) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "회원정보 수정을 위해 다시 로그인해 주세요."));
        }
        String userId = principal.getUsername();

        AuthProvider provider = userService.getProvider(userId);
        if (provider != AuthProvider.LOCAL) {
            // 구글 가입 계정은 비밀번호 검증 대신 재인증을 유도
            return ResponseEntity.status(403).body(Map.of(
                    "message", "구글로 가입한 계정입니다. 구글 재인증을 진행해 주세요.",
                    "provider", provider.name()
            ));
        }

        boolean ok = userService.verifyCurrentPassword(userId, req.getPassword());
        if (!ok) {
            return ResponseEntity.status(401).body(Map.of(
                    "verified", false,
                    "message", "비밀번호를 확인하여 다시 입력하여 주세요."
            ));
        }

        // 검증 성공 시: 회원정보 수정화면 진입을 위한 단기 reverify 토큰 발급(예: 5분)
        Duration ttl = Duration.ofMinutes(5);
        String reverifyToken = jwtService.issueReverifyToken(userId, ttl);
        long expiresAt = Instant.now().plus(ttl).toEpochMilli();
        return ResponseEntity.ok(new VerifyPasswordResponseDto(true, reverifyToken, expiresAt));
    }

    // ✅ 회원의 관심 카테고리를 조회하는 API
    @GetMapping("/me/categories")
    public ResponseEntity<?> getMyCategories(@AuthenticationPrincipal User principal) {
        // 1️⃣ 인증 체크
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "관심 카테고리를 보려면 로그인해야 합니다."));
        }
        // 2️⃣ 서비스 호출로 해당 사용자의 카테고리 Set<NewsCategory> 조회
        var categories = userService.getCategories(principal.getUsername());

        // 3️⃣ Enum → 문자열 리스트 변환
        List<String> categoryNames = categories.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return ResponseEntity.ok(categoryNames);
    }

    // 비밀번호 확인 전용 엔드포인트
    // 비밀번호 불일치시 메시지 띄우고 일치시 통과
    @PatchMapping("/me")
    public ResponseEntity<?> updateMyInfo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                          @Valid @RequestBody UpdateMyInfoRequestDto updateMyInfoReq) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "프로필을 수정하려면 로그인해 주세요."));
        }
        // LOCAL에서만 비번 검증 API를 쓰고 싶다면: 프론트에서 /me/verify-password를 먼저 호출하도록 유지
        userService.updateMyInfo(principal.getUsername(), updateMyInfoReq);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "회원 탈퇴를 위해 로그인해 주세요."));
        }
        userService.deleteAccount(principal.getUsername());
        return ResponseEntity.ok(Map.of("deleted", true));
    }

}