package com.highlight.nuzip.dto;

public record MemoResponse(
        Long id,
        String userId,
        String content,
        String updatedAt
) {
}
