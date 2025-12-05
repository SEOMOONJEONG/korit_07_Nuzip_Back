package com.highlight.nuzip.dto;

import lombok.Data;

// 예시: com.highlight.nuzip.dto.AnalysisResultDto.java
@Data
public class AnalysisResultDto {
    private Long id;
    private String title;
    private String sentiment; // 긍정, 부정, 중립
}