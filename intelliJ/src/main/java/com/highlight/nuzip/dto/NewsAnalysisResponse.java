package com.highlight.nuzip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Gemini API의 뉴스 분석 결과를 담는 DTO (Data Transfer Object)입니다. 요약, 키워드, 그리고 카테고리 필드를 포함합니다.
public record NewsAnalysisResponse(
        // 요약
        @JsonProperty("summary")
        String summary,

        // 키워드
        @JsonProperty("keywords")
        String keywords,

        // 🌟 카테고리 필터링을 위해 추가된 카테고리
        @JsonProperty("category")
        String category
) {}