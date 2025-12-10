package com.highlight.nuzip.service;

import com.highlight.nuzip.dto.NaverNewsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import jakarta.annotation.PostConstruct;

@Service
public class NaverApiService {

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    // 네이버 API의 호스트 URL로 설정하는 것이 일반적입니다.
    @Value("${naver.base-url:https://openapi.naver.com}")
    private String baseUrl;

    private final String newsSearchPath = "/v1/search/news.json"; // 경로를 상수로 관리

    private WebClient webClient;

    @PostConstruct
    public void init() {
        // baseUrl을 호스트(도메인)로 설정합니다.
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }

    // 네이버 뉴스 검색 API를 호출합니다.
    public Mono<NaverNewsResponse> searchNews(String query, int display) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(newsSearchPath) // 🌟 경로를 명시적으로 지정
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .queryParam("sort", "date")
                        .build())
                .retrieve()
                .bodyToMono(NaverNewsResponse.class)
                .doOnError(e -> System.err.println("WebClient 오류 발생: " + e.getMessage()));
    }
}