package com.highlight.nuzip.dto;

import lombok.Data;

@Data
public class RatingRequest {

    private Long scrapId;
    private Integer rating;
    private String feedback;
    private Boolean sendToGemini; // Gemini 전송 여부
}
