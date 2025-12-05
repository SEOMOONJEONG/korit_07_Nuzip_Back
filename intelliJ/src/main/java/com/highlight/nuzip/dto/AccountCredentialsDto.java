package com.highlight.nuzip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 로그인 요청 DTO(아이디/비번)
// 엔티티(User)와 분리해서 입력 검증, 보안(불필요 필드 차단), API 스펙 독립성 확보
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCredentialsDto {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank
    @Size(max = 100)
    private String userId;

    @NotBlank
    private String password;
}