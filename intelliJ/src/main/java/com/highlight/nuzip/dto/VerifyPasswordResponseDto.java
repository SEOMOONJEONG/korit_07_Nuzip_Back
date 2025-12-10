package com.highlight.nuzip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 비밀번호 응답(토큰 포함)
@Getter
@AllArgsConstructor
public class VerifyPasswordResponseDto {
    private boolean verified;
    private String reverifyToken;   // 재검증후 임시 토큰
    private long expiresAt; // 그 임시 토큰이 만료되는 시간설정
}