package com.highlight.nuzip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlight.nuzip.dto.NewsArticleDto;
import com.highlight.nuzip.dto.AnalysisResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity; // HttpEntity import는 유지
import java.util.Collections;
import java.util.List;
// RequestContextHolder 관련 import는 사용하지 않으므로 제거 가능하지만, 안전을 위해 유지합니다.
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;
// import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    // Spring Boot의 뉴스 데이터 제공 API URL (page=0&size=10으로 고정)
    private static final String NEWS_API_URL = "http://localhost:8080/api/news?page=0&size=10";

    // 파이썬 서버의 주소. application.properties에서 주입받거나 기본값 http://localhost:8000 사용
    @Value("${python.ml.host:http://localhost:8000}")
    private String pythonMlHost;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // 현재 로직에서 사용되지 않아 제거 가능하지만, 일단 유지합니다.

    /**
     * 전체 뉴스 데이터를 가져와 파이썬 ML 서버로 보내 감정 분석을 요청하고 결과를 반환합니다.
     */
    public List<AnalysisResultDto> runSentimentAnalysis() {
        // RestTemplateConfig의 인터셉터가 이 역할을 대신합니다.

        try {
            // ✅ 2. 내부 API 호출: 토큰은 RestTemplate 인터셉터가 자동으로 추가합니다.
            // HttpEntity에 헤더를 수동으로 설정하는 로직을 제거합니다.

            // HttpEntity<String> internalRequestEntity = null; // 이 줄을 제거

            List<NewsArticleDto> newsArticles = restTemplate.exchange(
                    NEWS_API_URL,
                    HttpMethod.GET,
                    null, // 💡 인터셉터가 헤더를 추가하므로 null 또는 new HttpEntity<>()를 사용합니다.
                    new ParameterizedTypeReference<List<NewsArticleDto>>() {}
            ).getBody();

            if (newsArticles == null || newsArticles.isEmpty()) {
                System.out.println("⚠️ 뉴스 API에서 데이터를 가져오지 못했습니다.");
                return Collections.emptyList();
            }

            // 3. 파이썬 ML 서버에 POST 요청 (뉴스 데이터를 JSON Body로 전달)
            String analyzeUrl = pythonMlHost + "/analyze";

            // 4. 파이썬 서버로부터 분석 결과(JSON List)를 받고 파싱합니다.
            List<AnalysisResultDto> analysisResults = restTemplate.exchange(
                    analyzeUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(newsArticles), // ✅ HttpEntity<>(List)를 Post 요청 본문으로 사용
                    new ParameterizedTypeReference<List<AnalysisResultDto>>() {}
            ).getBody();

            if (analysisResults == null) {
                System.err.println("파이썬 ML 서버로부터 분석 결과를 받지 못했습니다.");
                return Collections.emptyList();
            }

            return analysisResults;

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("401")) {
                System.err.println("내부 뉴스 API 호출 실패 (401 Unauthorized): 토큰 만료 또는 인증 실패. " + errorMsg);
            } else if (errorMsg != null && errorMsg.contains(pythonMlHost)) {
                System.err.println("파이썬 서버 통신 실패 (Connection Refused 또는 Timeout): " + errorMsg);
            } else {
                System.err.println("기타 처리 중 오류 발생: " + errorMsg);
            }
            throw new RuntimeException("감정 분석 서버 통신 또는 처리 실패", e);
        }
    }
}