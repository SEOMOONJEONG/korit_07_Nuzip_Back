package com.highlight.nuzip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // 💡 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NuzipApplication {

    public static void main(String[] args) {
        SpringApplication.run(NuzipApplication.class, args);
    }

    /**
     * JSON 객체 직렬화/역직렬화 (파싱)를 위한 ObjectMapper를 Bean으로 등록하고 JavaTimeModule을 등록합니다.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // ✅ 이 모듈을 수동으로 등록하여 LocalDateTime 오류를 강제로 해결합니다.
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}