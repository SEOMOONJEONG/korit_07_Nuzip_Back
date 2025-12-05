package com.highlight.nuzip.service;

import com.highlight.nuzip.model.NewsArticle;
import com.highlight.nuzip.repository.CategoryRepository;
import com.highlight.nuzip.repository.NewsArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 뉴스 조회(Read)와 관련된 비즈니스 로직을 처리하는 서비스입니다. 뉴스 수집 및 분석은 NewsCollectorService에서 담당합니다.
@Service
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;


    public NewsService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    // 전체 뉴스 기사를 페이지네이션하여 조회합니다.
    public Page<NewsArticle> findAllNews(Pageable pageable) {
        return newsArticleRepository.findAll(pageable);
    }

    // 카테고리별 뉴스 기사를 페이지네이션하여 조회합니다.
    public Page<NewsArticle> findNewsByCategory(String categoryName, Pageable pageable) {
        return newsArticleRepository.findByCategory(categoryName, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NewsArticle> searchNewsByKeyword(String keyword, Pageable pageable) {
        // NewsArticleRepository에 가정된 메소드 호출
        // 검색 키워드를 제목, 요약, 키워드 필드에 모두 전달하여 OR 검색을 수행합니다.
        return newsArticleRepository.findByTitleContainingOrSummaryContainingOrKeywordsContainingOrderByPublishedAtDesc(
                keyword,
                keyword,
                keyword,
                pageable
        );
    }
}