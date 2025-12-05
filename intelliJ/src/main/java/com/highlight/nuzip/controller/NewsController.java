package com.highlight.nuzip.controller;

import com.highlight.nuzip.dto.NewsArticleDto;
import com.highlight.nuzip.service.NewsService;
import com.highlight.nuzip.service.NewsAnalysisService;
import com.highlight.nuzip.dto.AnalysisResultDto; // ✅ 추가
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// 사용자에게 뉴스 기사 목록을 제공하는 REST API 컨트롤러입니다.
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final NewsAnalysisService newsAnalysisService;

    public NewsController(NewsService newsService, NewsAnalysisService newsAnalysisService) {
        this.newsService = newsService;
        this.newsAnalysisService = newsAnalysisService;
    }

    // (기존 getAllNews, getNewsByCategory, searchNewsByKeyword 메서드는 생략)

    // [GET /api/news] 전체 뉴스 기사 목록을 페이지네이션하여 반환합니다.
    @GetMapping
    public ResponseEntity<List<NewsArticleDto>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 최신순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

        // NewsService에서 전체 뉴스 조회
        Page<com.highlight.nuzip.model.NewsArticle> articlePage = newsService.findAllNews(pageable);

        // 엔티티 목록을 DTO 목록으로 변환
        List<NewsArticleDto> dtos = articlePage.getContent().stream()
                .map(NewsArticleDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // [GET /api/news/category/{categoryName}] 특정 카테고리 뉴스 기사 목록을 페이지네이션하여 반환합니다.
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<NewsArticleDto>> getNewsByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 카테고리 이름의 공백을 제거하여 서비스에 전달
        String trimmedCategoryName = categoryName.trim();

        // 최신순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

        // Service에서 카테고리별 뉴스 조회
        Page<com.highlight.nuzip.model.NewsArticle> articlePage =
                newsService.findNewsByCategory(trimmedCategoryName, pageable);

        // 엔티티 목록을 DTO 목록으로 변환
        List<NewsArticleDto> dtos = articlePage.getContent().stream()
                .map(NewsArticleDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/search")
    public ResponseEntity<List<NewsArticleDto>> searchNewsByKeyword(
            // 필수 파라미터로 'q' (검색 쿼리)를 받습니다.
            @RequestParam(name = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (keyword == null || keyword.trim().isEmpty()) {
            // 키워드가 없으면 400 Bad Request 반환 (또는 빈 목록 반환)
            return ResponseEntity.badRequest().build();
        }

        // 최신순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

        // NewsService에서 키워드 검색 메서드 호출 (제목, 요약, 키워드 대상)
        Page<com.highlight.nuzip.model.NewsArticle> articlePage =
                newsService.searchNewsByKeyword(keyword.trim(), pageable);

        // 엔티티 목록을 DTO 목록으로 변환
        List<NewsArticleDto> dtos = articlePage.getContent().stream()
                .map(NewsArticleDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // -------------------------------------------------------------------
    // ✅ 수정된 부분: 반환 타입을 List<AnalysisResultDto>로 변경하고 오류 처리를 단순화
    // -------------------------------------------------------------------
    @GetMapping("/analysis")
    public ResponseEntity<List<AnalysisResultDto>> runSentimentAnalysis() {
        System.out.println("API 호출: 감정 분석 작업 요청됨.");

        try {
            // NewsAnalysisService의 runSentimentAnalysis 메서드를 호출
            // 이 메서드는 이제 파이썬 서버와 통신 후 List<AnalysisResultDto>를 반환합니다.
            List<AnalysisResultDto> analysisResults = newsAnalysisService.runSentimentAnalysis();

            // 성공 시 200 OK와 함께 객체 리스트 반환
            return ResponseEntity.ok(analysisResults);
        } catch (Exception e) {
            // 통신/파싱 중 발생할 수 있는 모든 예외를 500으로 처리
            System.err.println("❌ 감정 분석 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // 분석 서버 오류 시 null 또는 빈 리스트 반환 (클라이언트 요구사항에 따라 결정)
        }
    }
}