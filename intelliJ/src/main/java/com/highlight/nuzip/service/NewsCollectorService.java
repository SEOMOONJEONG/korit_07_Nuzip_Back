package com.highlight.nuzip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlight.nuzip.dto.NewsAnalysisResponse;
import com.highlight.nuzip.dto.NaverNewsResponse;
import com.highlight.nuzip.dto.Item;
import com.highlight.nuzip.model.NewsArticle;
import com.highlight.nuzip.model.Subscription;
import com.highlight.nuzip.repository.NewsArticleRepository;
import com.highlight.nuzip.repository.CategoryRepository;
import com.highlight.nuzip.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class NewsCollectorService {

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;
    private final GeminiService geminiService;
    private final NaverApiService naverApiService;
    private final ObjectMapper objectMapper;

    // 알림 생성용
    private final NotificationService notificationService;
    private final SubscriptionRepository  subscriptionRepository;

    // 병렬 작업에서 할당량 초과 상태를 안전하게 공유하기 위한 AtomicBoolean 사용
    private final AtomicBoolean quotaExceeded = new AtomicBoolean(false);

    public NewsCollectorService(NewsArticleRepository newsArticleRepository, CategoryRepository categoryRepository, GeminiService geminiService, NaverApiService naverApiService, ObjectMapper objectMapper, NotificationService notificationService, SubscriptionRepository subscriptionRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.categoryRepository = categoryRepository;
        this.geminiService = geminiService;
        this.naverApiService = naverApiService;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.subscriptionRepository = subscriptionRepository;
    }

    // 30분마다 실행되는 뉴스 수집 및 요약 스케줄러 (Gemini 호출 병렬 처리 적용)
    // + 알림 생성
    @Scheduled(initialDelay = 10, fixedDelay = 1800000, timeUnit = TimeUnit.SECONDS)
    public void collectAndSummarizeNews() {
        // 작업 시작 시 quotaExceeded 상태 초기화
        quotaExceeded.set(false);
        System.out.println(">>> [스케줄러] 뉴스 수집 및 요약 작업을 시작합니다. 시각: " + LocalDateTime.now());

        var activeCategories = categoryRepository.findActiveForCollection();
        // 기사 수집 개수 설정
        int displayCount = 10;

        if (activeCategories.isEmpty()) {
            System.out.println(">>> DB에 설정된 활성화된 카테고리가 없습니다. 수집을 건너킵니다.");
            return;
        }

        // --- 1. 모든 카테고리에 대해 네이버 API 호출 및 기사 수집 ---
        List<CompletableFuture<Void>> futures = activeCategories.stream()
                .flatMap(categoryEntity -> {
                    String categoryName = categoryEntity.getName();
                    System.out.println(">>> [수집 대상] 카테고리: " + categoryName);

                    // 네이버 API 호출 (NaverNewsResponse DTO 사용)
                    NaverNewsResponse response = naverApiService.searchNews(categoryName, displayCount)
                            .block(java.time.Duration.ofSeconds(10));

                    if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                        System.out.println(">>> 네이버 API 응답에 기사가 없거나, 오류가 발생했습니다. (카테고리: " + categoryName + ")");
                        return java.util.stream.Stream.empty();
                    }

                    // --- 2. 수집된 각 기사에 대해 비동기 분석 작업 (CompletableFuture) 생성 ---
                    // 이 단계에서 기사별 병렬 처리가 시작됩니다.
                    return response.getItems().stream()
                            .map(item -> processNewsArticleAsync(item, categoryEntity.getId()));
                })
                .collect(Collectors.toList());

        // --- 3. 모든 병렬 작업 완료 대기 ---
        try {
            // 모든 작업이 완료되거나 최대 25분이 경과할 때까지 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(25, TimeUnit.MINUTES);
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("!!! [경고] 일부 기사 분석 작업이 25분 이내에 완료되지 못했습니다. TimeOut.");
        } catch (Exception e) {
            System.err.println("!!! [치명적 오류] 뉴스 분석 중 예기치 않은 오류 발생: " + e.getMessage());
        }

        System.out.println(">>> [스케줄러] 뉴스 수집 및 요약 작업을 완료했습니다.");

        if (quotaExceeded.get()) {
            System.out.println("!!! Gemini API 할당량 초과가 감지되었습니다. 다음 스케줄을 기다립니다.");
        }
    }

    // 개별 기사 처리 (크롤링, Gemini 분석, DB 저장)를 비동기적으로 수행하는 메서드
    @Async // 이 메서드는 별도의 스레드에서 실행됩니다.
    @Transactional // DB 저장은 트랜잭션으로 보호됩니다.
    public CompletableFuture<Void> processNewsArticleAsync(Item item, Long categoryId) {
        // 할당량 초과 시 즉시 종료 (빠른 실패)
        if (quotaExceeded.get()) {
            return CompletableFuture.completedFuture(null);
        }

        // Item DTO의 메서드를 사용하여 태그가 제거된 제목을 가져옵니다.
        String cleanTitle = item.getCleanTitle();

        // 중복 확인 (DB 접근)
        if (newsArticleRepository.findByOriginalLink(item.getOriginallink()).isPresent()) {
            System.out.println("[병렬] 이미 수집된 기사입니다. 스킵: " + cleanTitle);
            return CompletableFuture.completedFuture(null);
        }

        try {
            // 3-1. 기사 본문 크롤링 (텍스트와 이미지 URL을 함께 반환)
            GeminiService.ArticleExtractionResult extractionResult = geminiService.extractArticleContent(item.getOriginallink());
            String articleContent = extractionResult.content();
            String imageUrl = extractionResult.imageUrl(); // 🌟 추출된 이미지 URL 🌟

            // 크롤링 실패 또는 내용 부족 스킵
            if (articleContent.startsWith("크롤링 실패") || articleContent.startsWith("본문이 너무 짧습니다")) {
                System.out.println("[병렬] 본문 크롤링 실패 또는 내용 부족으로 스킵: " + cleanTitle);
                return CompletableFuture.completedFuture(null);
            }

            // 3-2. Gemini에 분석 요청
            NewsAnalysisResponse analysisResponse = geminiService.analyzeNewsArticle(articleContent);

            // 4. NewsArticle 엔티티 생성 및 필드 설정
            NewsArticle article = new NewsArticle();
            article.setTitle(cleanTitle);
            article.setOriginalLink(item.getOriginallink());
            article.setImageUrl(imageUrl); // 🌟 추출된 이미지 URL 설정 🌟

            // 3-3. DTO에서 바로 데이터 추출 및 설정
            article.setSummary(analysisResponse.summary());
            article.setKeywords(analysisResponse.keywords());
            article.setCategory(analysisResponse.category().trim()); // 카테고리 설정

            // 네이버 API의 pubDate를 파싱하여 설정
            article.setPublishedAt(parseNaverPubDate(item.getPubDate()));
            article.setCollectedAt(LocalDateTime.now());

            // 5. MariaDB에 저장
            newsArticleRepository.save(article);
            System.out.println(">>> [병렬] 뉴스 수집 및 분석 완료 (카테고리: " + article.getCategory() + "): " + cleanTitle);

            // 사용자에게 Notification 생성
            List<Subscription> subscriptions = subscriptionRepository.findByCategoryId(categoryId);
            for(Subscription sub :  subscriptions) {
                notificationService.createNotification(
                        sub.getUserId(),
                        "새 기사: " + cleanTitle,
                        article.getId()
                ); // 알림 db 저장
            }

        } catch (Exception e) {
            // 6. 할당량 초과 오류 (429) 처리
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("Gemini API 할당량 초과 (429)"))) {
                System.err.println("!!! [치명적 오류] Gemini API 할당량 초과 (429) 발생. 병렬 작업 중단 플래그 설정.");
                quotaExceeded.set(true); // 모든 병렬 작업을 멈추도록 플래그 설정
            }

            // 그 외 일반적인 분석 및 저장 오류 처리
            String shortError = errorMessage != null ? errorMessage.substring(0, Math.min(errorMessage.length(), 100)) + "..." : "알 수 없는 오류";
            System.err.println("!!! [병렬] 기사 분석 및 저장 중 일반 오류 발생: " + cleanTitle + " - " + shortError);
        }

        return CompletableFuture.completedFuture(null);
    }

    // 네이버 API의 pubDate 문자열을 LocalDateTime 객체로 파싱합니다.
    private LocalDateTime parseNaverPubDate(String pubDate) {
        if (pubDate == null) {
            return LocalDateTime.now().minusDays(1);
        }
        try {
            // 네이버 날짜 형식: EEE, dd MMM yyyy HH:mm:ss Z (예: Mon, 11 Sep 2023 11:30:00 +0900)
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")
                    .withLocale(Locale.ENGLISH);

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, formatter);

            // 시스템 기본 시간대로 변환하여 LocalDateTime으로 반환
            return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        } catch (Exception e) {
            System.err.println("pubDate 파싱 오류: " + pubDate + " - " + e.getMessage());
            return LocalDateTime.now().minusHours(2);
        }
    }
}