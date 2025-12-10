package com.highlight.nuzip.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// 로그인 성공 시 헤더에 토큰발급
@Service
public class JwtService {

    static final long EXPIRATION_TIME = 86400000; // 하루
    static final String PREFIX = "Bearer ";

    // ✅ 고정 secret 사용 (서버 재시작 시 토큰 무효화 방지)
    private static final String SECRET = "replace-this-with-your-own-64-byte-secret-key-string...........";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // ✅ JWT 생성 (subject = userId)
    // 로그인 시 토큰 발급
    public String generateToken(String userId) {
        return Jwts.builder()
                .subject(userId)    // 로그인한 사용자를 토큰에 담아 JWT 생성
                .issuedAt(new Date())   // 언제 발급됐는지
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 언제 만료되는지
                .signWith(key)  // 비밀키 서명으로 위조 방지
                .compact();
    }

    // ✅ 요청 헤더에서 JWT 추출 후 사용자 ID(subject) 반환
    // 요청 헤더의 `Authorization` 값에서 JWT 추출
    public String getAuthUser(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(token != null && token.startsWith(PREFIX)) {
            try {
                String user = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token.replace(PREFIX, ""))   // 접두사 "Bearer " 제거
                        .getPayload()
                        .getSubject();

                // 잘못된 토큰이면 예외 발생 → null 반환
                if(user != null) {
                    return user;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;    // getAUthUser 메서드 호출했는데 user 안튀어나올 때 null return : 토큰이 없거나 유효하지 않음.
    }

    // 토큰이 유효한지 검사
    // Authorization 헤더가 있는지 확인하고 "Bearer "로 시작하면 토큰 부분만 잘라냄
    public boolean isValidToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token); // 검증 시도
                return true;    // 성공
            } catch (Exception e) {
                return false;   // 실패
            }
        }
        return false;
    }

    // 생성자 → 외부 설정 값으로 키 주입
    // 환경설정 파일에 있는 비밀키를 실제 암호화 키로 변환해서 보관
    private final Key signingKey;   // jwt 서명 검증 비밀키
    // application에서 jwt.secret 값을 읽어와서 암호화 키 객체로 변환
    public JwtService(@Value("${jwt.secret}") String secret) {  // 스프링 의존성 주입 키
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());    // 문자열 키를 실제 암호화 키로 변환하는 과정
    }

    // 회원정보 수정 전 비밀번호 확인하여 임시 토큰 발급
    // 용도 구분하여 로그인 토큰과 회원정보 임시 토큰과 구분하도록 함
    public String issueReverifyToken(String userId, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .setAudience("reverify")    // 용도 구분 (로그인 토큰이냐, 수정용 임시토큰이냐 구분)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .claim("scope", "profile:edit") // 권한 범위 명시 (행동 범위)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
    // 임시 토큰 검증
    // "회원정보 수정용 토큰이 진짜 맞는지" 확인하는 메서드
    public boolean verifyReverifyToken(String token, String expectedUserId) {
        var claims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        // audience가 "reverify"인지 확인 → 수정용 토큰인지 체크
        if (!"reverify".equals(claims.getAudience())) return false;
        // subject 토큰에 저장된 userId가 맞는지도 비교
        if (!expectedUserId.equals(claims.getSubject())) return false;
        // 만료는 파서에서 자동 검증됨
        return true;
    }
}