package com.highlight.nuzip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 로그인 응답 DTO(authenticated, user 정보)
// 로그인 성공 시 프론트에 돌려줄 출력 DTO
// 비밀번호/내부키 등을 절대 노출하지 않기 위해 "필요한 정보만" 선별해서 반환
@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private boolean authenticated;
    private Long id;
    private String userId;
    private String username;
}