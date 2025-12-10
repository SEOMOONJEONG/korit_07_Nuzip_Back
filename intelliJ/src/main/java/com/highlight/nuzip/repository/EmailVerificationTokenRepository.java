package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

// 이메일 인증 메일발송 + 인증 코드 확인 + 인증 완료 후 등록(토큰 조회)
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    // 이메일로 생성된 가장 최근 인증 토큰 조회(발송된 인증코드가 있는지 확인할 때)
    Optional<EmailVerificationToken> findTopByEmailOrderByCreatedAtDesc(String email);

    // 가장 마지막에 생성된 인증 토큰 조회(입력된 인증코드 맞는지 확인할 때)
    Optional<EmailVerificationToken> findTopByEmailAndCodeOrderByCreatedAtDesc(String email, String code);

    // 인증 성공 + 아지 사용되지 않은 토큰 조회(이메일 인증 완료여부 확인할 때)
    Optional<EmailVerificationToken> findTopByEmailAndVerifiedTrueAndConsumedFalseOrderByVerifiedAtDesc(String email);

    // 특정 시점 이후 해당 메일로 발송된 이메일 수(1분에 1번만 인증 이메일 발송 가능 제한 구현할 때)
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}

