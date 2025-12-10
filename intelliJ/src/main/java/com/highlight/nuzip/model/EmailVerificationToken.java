package com.highlight.nuzip.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
// 코드 발급, 확인, 만료, 사용처리
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    // 기본키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 이메일 주소 저장
    @Column(nullable = false, length = 120)
    private String email;
    // 인증코드
    @Column(nullable = false, length = 12)
    private String code;
    // 만료시간
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    // 인증여부
    @Column(nullable = false)
    private boolean verified;
    // 인증시간
    private LocalDateTime verifiedAt;
    // 사용 완료 여부(한번 사용한 토큰 다시 사용 못하도록)
    @Column(nullable = false)
    private boolean consumed;
    // 생성시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 새 인증 요청시 기본값 설정 자동 실행
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.verified = false;
        this.consumed = false;
    }
    // 토큰 만료여부 확인
    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }
}

