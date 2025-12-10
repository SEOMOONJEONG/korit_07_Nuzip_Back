package com.highlight.nuzip.security;

import com.highlight.nuzip.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/*
 * JWT 인증 필터
 * - 모든 요청 전에 실행되어, Authorization 헤더에 JWT가 있으면 파싱 및 인증 설정을 수행
 */

// 요청이 들어올 때마다 JWT를 꺼내서 인증을 세팅해 주는 ‘필터’
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;                 // JWT 생성/검증 담당
    private final UserDetailsServiceImpl userDetailsService; // DB 사용자 로드

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        long filterStart = System.currentTimeMillis();
        try {
            // 🔹 Authorization 헤더에서 userId(subject) 추출
            String userId = jwtService.getAuthUser(request);
            if (userId == null) {
                log.trace("[JWT] request={} Authorization 헤더 없음/무효", requestUri);
            }

            // 🔹 userId가 존재하고, 아직 SecurityContext에 인증정보가 없으면
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.debug("[JWT] request={} userId={} loadUserByUsername 시작", requestUri, userId);
                long loadStart = System.currentTimeMillis();

                // DB에서 사용자 정보 로드 (UserDetailsServiceImpl → UserRepository) 사용자 정보 조회
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                log.debug("[JWT] request={} userId={} loadUserByUsername 완료 ({} ms)",
                        requestUri, userId, System.currentTimeMillis() - loadStart);

                // 토큰 유효성 검사 추가 (선택: isValid() 구현 시)
                if (jwtService.isValidToken(request)) {
                    // 인증 객체 생성 후 SecurityContext에 등록
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContextHolder에 인증 객체(Authentication) 저장
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("[JWT] request={} userId={} SecurityContext 저장 완료", requestUri, userId);
                } else {
                    log.warn("[JWT] request={} userId={} 토큰 검증 실패", requestUri, userId);
                }
            }
        } catch (Exception ex) {
            log.error("[JWT] request={} 필터 처리 중 예외: {}", requestUri, ex.getMessage(), ex);
            throw ex;
        } finally {
            log.trace("[JWT] request={} 필터 총 소요 {} ms", requestUri, System.currentTimeMillis() - filterStart);
        }

        // 🔹 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}