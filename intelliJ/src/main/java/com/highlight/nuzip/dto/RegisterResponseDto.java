package com.highlight.nuzip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 회원가입 응답 DTO
// 저장 결과에서 필요한 정보만 내려주기
@Getter
@AllArgsConstructor
public class RegisterResponseDto {
    private Long id;
    private String userId;
    private String username;
}