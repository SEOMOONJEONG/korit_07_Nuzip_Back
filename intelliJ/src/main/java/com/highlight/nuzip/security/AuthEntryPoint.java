package com.highlight.nuzip.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
/*
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 401을 JSON으로 반환.
 * - 프론트에서 일관되게 처리할 수 있도록 Content-Type/charset 고정
 * - 캐시 방지 헤더 추가(선택)
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        // 선택: 캐시 방지 (브라우저/프록시가 401을 캐시하지 않도록)
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        // 프론트가 토스트/리다이렉트 등에 활용할 수 있는 최소 정보
        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"로그인 세션이 유효하지 않습니다. 다시 로그인해 주세요.\"}");
    }
}