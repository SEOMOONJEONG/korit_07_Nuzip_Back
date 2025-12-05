package com.highlight.nuzip.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMyInfoRequestDto {

    // 허용한 수정 항목
    private String username;    // 닉네임
    private Set<String> categories; // 카테고리 수정
    private LocalDate birthDate;    // 생년월일
    private String phone;       // 전화번호

    // ✅ 비밀번호 변경용 (LOCAL만 사용)
    private String newPassword; // 비밀번호 변경 요청 시 전달
    private String confirmNewPassword;   // 새 비밀번호 확인

}