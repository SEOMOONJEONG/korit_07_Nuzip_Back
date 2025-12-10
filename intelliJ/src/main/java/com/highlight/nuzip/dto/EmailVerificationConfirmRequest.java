package com.highlight.nuzip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 이메일 인증 확인요청 DTO
// 이메일 인증 서버로 전달되는 요청
public record EmailVerificationConfirmRequest(
        // 아이디 이메일 형식인지 검증
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일을 입력해주세요.")
        String email,

        // 인증코드 6자리 제한
        @NotBlank(message = "인증 코드를 입력해주세요.")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리 숫자입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자입니다.")
        String code
) {}

