package com.highlight.nuzip.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 💡 RestTemplate에 ClientHttpRequestInterceptor를 추가합니다.
                .interceptors((request, body, execution) -> {
                    // 현재 요청 컨텍스트에서 Authorization 헤더를 가져옵니다.
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest httpRequest = attributes.getRequest();
                        String authorizationHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);

                        // 헤더가 있으면 내부 요청에 그대로 설정합니다.
                        if (authorizationHeader != null) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorizationHeader);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}