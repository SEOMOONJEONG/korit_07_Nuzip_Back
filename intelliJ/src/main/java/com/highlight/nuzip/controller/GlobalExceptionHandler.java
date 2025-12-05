package com.highlight.nuzip.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    /*
     * ⚠️ IllegalStateException 처리
     *
     * 예: 구글(OAUTH) 계정이 비밀번호 변경을 시도하거나,
     *     시스템 상태상 허용되지 않는 작업을 실행했을 때 발생시킴.
     *
     * - HTTP 상태코드: 403 (FORBIDDEN)
     *   → “요청은 이해했지만, 현재 상태에서는 허용되지 않음” 의미.
     * - 응답 형식: {"message": "예외 메시지 내용"}
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(java.util.Map.of("message", ex.getMessage()));
    }

    /*
     * ⚠️ IllegalArgumentException 처리
     *
     * 예: 잘못된 입력값(카테고리 3개 초과, 비밀번호 불일치, 형식 오류 등)
     *     유효성 검증 실패 시 개발자가 직접 throw new IllegalArgumentException(...) 하는 경우.
     *
     * - HTTP 상태코드: 400 (BAD_REQUEST)
     *   → “클라이언트가 잘못된 데이터를 보냈다”는 의미.
     * - 응답 형식: {"message": "예외 메시지 내용"}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(java.util.Map.of("message", ex.getMessage()));
    }
}