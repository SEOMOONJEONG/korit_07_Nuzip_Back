package com.highlight.nuzip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlight.nuzip.config.GeminiConfig;
import com.highlight.nuzip.dto.NewsAnalysisResponse;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiService {

    private final GeminiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService(GeminiConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    // --- 새로운 내부 클래스/레코드: 텍스트와 이미지 URL을 함께 반환하기 위함 ---
    public record ArticleExtractionResult(String content, String imageUrl) {}

    // 기존 extractArticleContent 메서드를 수정하여 ArticleExtractionResult를 반환합니다.
    public ArticleExtractionResult extractArticleContent(String url) {
        System.out.println(">>> [GeminiService] 기사 본문 크롤링 시작: " + url);
        String extractedText = "";
        String extractedImageUrl = null;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            String[] commonSelectors = {
                    "#newsct_article",
                    "#articleBodyContents",
                    ".article_body",
                    ".article-view",
                    ".view_contents",
                    "article"
            };

            Element contentElement = null;
            String usedSelector = "";
            for (String selector : commonSelectors) {
                contentElement = doc.select(selector).first();
                if (contentElement != null && !contentElement.text().trim().isEmpty()) {
                    usedSelector = selector;
                    break;
                }
            }

            // 본문 요소가 주요 선택자에서 발견되지 않았다면, 일반적인 방법으로 본문을 찾고 불필요한 요소를 제거합니다.
            if (contentElement == null || usedSelector.isEmpty() || contentElement.text().trim().isEmpty()) {
                contentElement = doc.body();
                contentElement.select("header, footer, nav, aside, .sidebar, .ad, .ad-unit, .byline, #comment, .related-articles, .article_btns, .sub_copy, script, style, iframe").remove();

                if (contentElement != null) {
                    System.out.println(">>> [GeminiService] 크롤링 성공 (일반 바디)");
                } else {
                    return new ArticleExtractionResult("본문을 찾을 수 없습니다. (시도된 선택자들 모두 실패)", null);
                }
            } else {
                System.out.println(">>> [GeminiService] 크롤링 성공 (선택자: " + usedSelector + ")");
            }

            // 1. Open Graph 메타 태그에서 이미지 URL 시도
            Element ogImage = doc.select("meta[property=og:image]").first();
            if (ogImage != null) {
                extractedImageUrl = ogImage.attr("content");
                System.out.println(">>> [Image] Open Graph 메타 태그에서 이미지 추출: " + extractedImageUrl);
            }

            // 2. 만약 og:image가 없으면, 본문 요소 내 첫 번째 이미지 태그를 시도
            if (extractedImageUrl == null || extractedImageUrl.isEmpty()) {
                Elements images = contentElement.select("img");
                if (!images.isEmpty()) {
                    // 첫 번째 이미지 태그의 src 속성 가져오기
                    String firstImageUrl = images.first().attr("src");
                    // 상대 경로일 경우 절대 경로로 변환 시도 (완벽하진 않음)
                    if (firstImageUrl != null && (firstImageUrl.startsWith("//") || firstImageUrl.startsWith("/"))) {
                        try {
                            URI baseUri = new URI(url);
                            extractedImageUrl = baseUri.resolve(firstImageUrl).toString();
                        } catch (Exception uriEx) {
                            extractedImageUrl = firstImageUrl; // 변환 실패 시 그대로 사용
                        }
                    } else if (firstImageUrl != null) {
                        extractedImageUrl = firstImageUrl;
                    }
                    if (extractedImageUrl != null) {
                        System.out.println(">>> [Image] 본문 첫 번째 이미지 태그에서 이미지 추출: " + extractedImageUrl);
                    }
                }
            }

            // 불필요한 요소 제거 후 텍스트 추출
            contentElement.select(".article_btns, .byline, script, style, iframe, .sub_copy, .ad-unit, img").remove(); // <img> 태그는 텍스트로 인식될 수 있으므로 제거
            extractedText = contentElement.text().trim();

            if (extractedText.length() < 10) {
                return new ArticleExtractionResult("본문이 너무 짧습니다. (길이: " + extractedText.length() + ")", extractedImageUrl);
            }
            return new ArticleExtractionResult(extractedText, extractedImageUrl);
        } catch (Exception e) {
            System.err.println("크롤링 오류 발생: " + e.getMessage());
            return new ArticleExtractionResult("크롤링 실패: " + e.getMessage(), extractedImageUrl);
        }
    }

    // 기사 본문을 Gemini API로 보내 요약, 키워드, 카테고리를 추출합니다.
    public NewsAnalysisResponse analyzeNewsArticle(String articleContent) {
        // 병렬 처리의 효율성을 극대화하기 위해 API 호출 전 5초 지연(Thread.sleep)을 제거합니다.
        // 할당량 초과(429)는 NewsCollectorService의 quotaExceeded 플래그를 통해 처리됩니다.
        System.out.println(">>> [GeminiService] API 호출 시도.");

        // 시스템 지침 및 사용자 프롬프트
        String systemInstruction = "You are a news analysis expert. Perform the analysis tasks according to the Korean instructions provided in the user prompt and return the result only in the specified JSON format.";
        String userPrompt = String.format("""
            제공된 뉴스 기사의 내용을 분석하여 다음 세 가지 항목을 추출하세요:
            1. 'summary': 기사를 한국어로 세 문장 이내로 상세히 요약합니다.
            2. 'keywords': 기사의 핵심 키워드 5개를 쉼표(,)로 구분하여 한 줄로 나열합니다.
            3. 'category': 기사가 다음 카테고리 중 어디에 속하는지 판단하여 추출합니다. 다음 카테고리에 있는 걸로만 추출합니다. 다른 카테고리는 사용하지 않습니다.: [정치, 경제, 사회, 생활ㆍ문화, 스포츠, 엔터, ITㆍ과학, 세계].
            
            기사 내용: %s
            """, articleContent);

        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "summary": { "type": "string", "description": "기사의 핵심 내용을 한국어로 3줄 요약" },
                    "keywords": { "type": "string", "description": "기사의 핵심 키워드 5개를 쉼표(,)로 구분한 문자열" },
                    "category": { "type": "string", "description": "기사의 카테고리 (예: 경제, 정치, 생활ㆍ문화 등)" }
                },
                "required": ["summary", "keywords", "category"]
            }
            """;

        String jsonBody = createApiJsonBody(systemInstruction, userPrompt, jsonSchema);

        try {
            String apiKey = config.getApiKey();
            if (apiKey == null || apiKey.isEmpty() || "YOUR_GEMINI_API_KEY_HERE".equals(apiKey)) {
                return new NewsAnalysisResponse(
                        "[API KEY 설정 누락] application.properties에서 spring.ai.gemini.api-key를 설정하세요.",
                        "설정 필요",
                        "기타"
                );
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + config.getModel() + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                System.err.println("API Error (429 Quota Exceeded): " + response.body());
                // NewsCollectorService에서 catch할 수 있도록 RuntimeException 발생
                throw new RuntimeException("Gemini API 할당량 초과 (429) 오류 발생. 상세: " + response.body());
            }

            if (response.statusCode() != 200) {
                System.err.println("API Error (" + response.statusCode() + "): " + response.body());
                return new NewsAnalysisResponse(
                        String.format("[API 통신 오류: %d]", response.statusCode()),
                        "오류",
                        "기타"
                );
            }

            String rawJson = extractJsonFromResponse(response.body());

            return objectMapper.readValue(rawJson, NewsAnalysisResponse.class);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("요청 또는 파싱 중 오류 발생: " + e.getMessage());
            return new NewsAnalysisResponse(
                    String.format("[서버 요청 오류: %s]", e.getMessage()),
                    "오류",
                    "기타"
            );
        }
    }

    private String createApiJsonBody(String systemInstruction, String userPrompt, String responseSchema) {
        // 시스템 지침과 사용자 프롬프트를 하나로 결합합니다.
        String combinedPrompt = String.format("%s\n\n[USER INPUT START]\n%s\n[USER INPUT END]",
                systemInstruction, userPrompt);

        String escapedPrompt = combinedPrompt.replace("\"", "\\\"");

        return String.format("""
        {
          "contents": [
            { 
              "role": "user",
              "parts": [{ "text": "%s" }]
            }
          ],
          "generationConfig": {
            "temperature": %s,
            "maxOutputTokens": %s,
            "responseMimeType": "application/json",
            "responseSchema": %s
          }
        }
        """,
                escapedPrompt,
                config.getTemperature(),
                config.getMaxTokens(),
                responseSchema
        );
    }

    private String extractJsonFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidate = root.path("candidates").get(0);
            if (candidate == null) {
                System.err.println("응답에서 후보 노드를 찾을 수 없습니다: " + responseBody);
                return "{\"summary\": \"[분석 실패: API 응답 후보 없음]\", \"keywords\": \"API 오류\", \"category\": \"기타\"}";
            }
            JsonNode textNode = candidate.path("content").path("parts").get(0).path("text");
            if (textNode.isTextual()) {
                String rawJson = textNode.asText();
                try {
                    objectMapper.readTree(rawJson);
                    return rawJson;
                } catch (Exception jsonEx) {
                    System.err.println("Gemini가 반환한 내용이 유효한 JSON이 아닙니다: " + rawJson);
                    return "{\"summary\": \"[분석 실패: 응답 JSON 구조 오류]\", \"keywords\": \"JSON 오류\", \"category\": \"기타\"}";
                }
            } else {
                System.err.println("응답에서 텍스트 노드를 찾을 수 없습니다: " + responseBody);
                return "{\"summary\": \"[분석 실패: 텍스트 노드 없음]\", \"keywords\": \"API 오류\", \"category\": \"기타\"}";
            }
        } catch (Exception e) {
            System.err.println("응답 JSON 처리 중 치명적인 오류 발생: " + e.getMessage());
            return "{\"summary\": \"[분석 실패: 응답 JSON 처리 오류]\", \"keywords\": \"처리 오류\", \"category\": \"기타\"}";
        }
    }

    /**
     * 사용자 피드백 학습용 호출 (응답 무시)
     */
    public void trainFeedback(String feedback) {
        try {
            String apiKey = config.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return; // API 키 없으면 무시
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + config.getModel() + ":generateContent";

            // JSON body에 별점/피드백 그대로 전송
            String jsonBody = """
                {
                  "contents": [
                    { "parts": [{ "text": "%s" }] }
                  ],
                  "generationConfig": {
                    "temperature": %s,
                    "maxOutputTokens": %s
                  }
                }
                """.formatted(feedback, config.getTemperature(), config.getMaxTokens());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // 응답 무시
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception ignored) {
            // 실패 시 무시
        }
    }
}