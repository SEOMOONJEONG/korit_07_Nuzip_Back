package com.highlight.nuzip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 인증 이메일 요청
public record EmailVerificationSendRequest(
        // 이메일 형식 검사
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일을 입력해주세요.")
        String email
) {}

