package com.highlight.nuzip.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 비밀번호 확인 요청
@Getter
@Setter
@NoArgsConstructor
public class VerifyPasswordRequestDto {
    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;
}