package com.highlight.nuzip.service;

import com.highlight.nuzip.model.User;
import com.highlight.nuzip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
/*  Spring Security에서 userId 던지면,
    DB에서 유저를 찾아서 인증 가능한 형태로 바꿔주는 역할
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;    // 사용자 찾기
    private final EmailVerificationService emailVerificationService;    // 이메일 비교

    // DB에서 유저 찾아서 UserDetails로 변환
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        long start = System.currentTimeMillis();
        String normalizedEmail = emailVerificationService.normalizeEmail(userId);
        log.debug("[UserDetails] loadUserByUsername userId={} normalized={}", userId, normalizedEmail);
        // 유저 찾기
        User u = userRepository.findByUserId(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[UserDetails] userId={} (normalized={}) 조회 실패", userId, normalizedEmail);
                    return new UsernameNotFoundException("이메일을 찾을 수 없습니다." + userId);
                });
        log.debug("[UserDetails] userId={} DB 조회 완료 ({} ms)", userId, System.currentTimeMillis() - start);
        // UserDetails 객체 생성
        return new org.springframework.security.core.userdetails.User(
                u.getUserId(),
                u.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))    // 기본 권한 부여
        );
    }
}