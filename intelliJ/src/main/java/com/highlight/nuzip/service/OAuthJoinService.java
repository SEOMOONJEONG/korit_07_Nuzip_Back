package com.highlight.nuzip.service;

import com.highlight.nuzip.model.AuthProvider;
import com.highlight.nuzip.model.NewsCategory;
import com.highlight.nuzip.model.User;
import com.highlight.nuzip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class OAuthJoinService {
    private final UserRepository userRepository;

    /*
     * 구글 계정 → userId로 받아옴
     * 구글에서 받은 이메일을 우리 시스템의 userId로 사용한다.
     * - 없으면 신규 저장
     * - 있으면 기존 사용자 반환
     */
    // joinIfAbsent = google email이 비어있을 경우
    @Transactional
    public User joinIfAbsent(String googleEmail, String name) {
        // 방어: email이 비어오면 예외 혹은 대체 ID 정책 필요
        if (googleEmail == null || googleEmail.isBlank()) {
            throw new IllegalArgumentException("Google email이 없습니다. 가입 불가");
        }
        String normalizedEmail = googleEmail.trim().toLowerCase();

        return userRepository.findByUserId(normalizedEmail)
                .orElseGet(() -> {
                    // 엔티티 제약 충족:
                    // - userId: 필수, 유니크 → 구글 email 사용
                    // - password: 필수 → 소셜계정용 더미값 저장(실제 로그인에는 사용하지 않음)
                    // - username: 필수 → 구글 name 사용(없으면 email 앞부분 등으로 대체 가능)
                    // - newsCategory: @NotNull → 빈 Set으로 초기화 (Size(max=3) 충족)
                    User user = User.builder()
                            .userId(normalizedEmail)
                            .password("OAUTH2_USER")    // 더미(비밀번호는 NotNull이니까 더미 문자열 입력 → 폼 로그인 미사용 시 검증값)
                            .username((name != null && !name.isBlank() ? name : normalizedEmail))
                            .newsCategory(Set.<NewsCategory>of())   // @NewsCaegory는 @NotNull이므로 Set.of()로 빈 세트 입력 → 나중에 선택할 때 갱신
                            .provider(AuthProvider.OAUTH_GOOGLE)
                            .emailVerified(true)
                            .build();

                    return userRepository.save(user);       // 실제 INSERT
                });
    }
}