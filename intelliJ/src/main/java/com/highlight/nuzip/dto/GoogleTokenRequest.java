package com.highlight.nuzip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 프론트에서 보낸 구글 ID 토큰(idToken)을 백엔드 컨트롤러가 받기 위한 껍데기 클래스
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenRequest {
    private String idToken; // 프론트 GoogleLogin 성공 시 전달되는 credential
}