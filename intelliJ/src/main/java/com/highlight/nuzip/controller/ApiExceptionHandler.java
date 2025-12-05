package com.highlight.nuzip.controller;

// 검증/비즈니스 예외 400 통일
// 예외처리 : 컨트롤러 전역에서 예외를 가로채서 메시지 통일 (에러 응답 통일)

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValid(MethodArgumentNotValidException e) {
        var first = e.getBindingResult().getFieldErrors().stream().findFirst();
        String msg = first.map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("검증 오류");
        return ResponseEntity.badRequest().body(msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}