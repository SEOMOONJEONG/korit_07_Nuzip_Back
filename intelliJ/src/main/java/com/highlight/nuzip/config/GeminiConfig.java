package com.highlight.nuzip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// Gemini API 통신에 필요한 기본 설정값들을 관리합니다. 모든 설정은 application.properties 파일에서 @Value를 통해 주입받습니다.
@Configuration // @Component 제거
public class GeminiConfig {

    // application.properties에서 설정된 spring.ai.gemini.api-key 값을 주입받습니다.
    @Value("${spring.ai.gemini.api-key}")
    private String apiKey;

    // 모델 이름 (기본값 gemini-2.5-flash)
    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    // 생성 온도 (결과의 무작위성, 기본값 0.5)
    @Value("${gemini.temperature:0.5}")
    private double temperature;

    // 카테고리, 요약, 키워드 세 가지 필드를 모두 받아야 하므로 충분한 공간 확보
    @Value("${gemini.max-tokens:2048}")
    private int maxTokens;

    // 타임아웃 시간 (초, 기본값 30초)
    @Value("${gemini.timeout-seconds:20}")
    private int timeoutSeconds;

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
}