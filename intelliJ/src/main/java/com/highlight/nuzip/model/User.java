package com.highlight.nuzip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;


// JPA 엔티티
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_user_id", columnNames = {"userId"})
        }
)

public class User {

    // 자동 증가 기본키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일형식 아이디(구글의 경우 Google OAuth로)
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "아이디(이메일)는 필수 입력값입니다.")
    @Column(nullable = false, unique = true, length = 100)
    private String userId;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Column(nullable = false, length = 50)
    private String username;

    // 뉴스 카테고리: 최대 3개 선택 (중복 방지 위해 Set 사용)
    @Size(max = 3, message = "카테고리는 최대 3개를 선택해야 합니다.")
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_categories",
            joinColumns = @JoinColumn(name = "user_id") // users.id 외래키
    )
    @Column(name = "category", length = 30)
    @Builder.Default
    private Set<NewsCategory> newsCategory = new java.util.HashSet<>();
    // 빈 Set로 기본값을 줘서 다음 페이지에서 카테고리 선택 가능하도록 함.

    // 선택 입력
    private LocalDate birthDate;

    // 숫자만 허용 (01011112222 형식)
    @Pattern(regexp = "^$|^\\d{11}$", message = "핸드폰 번호는 숫자만 입력해주세요.")
    @Column(length = 11)
    private String phone;

    // ✅ 추가 : 가입/인증 공급자
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    // 편의 메서드
    public boolean hasLocalPassword() {
        return this.password != null && !this.password.isBlank();
    }

    // 이메일 인증 여부 필드 추가
    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;

}