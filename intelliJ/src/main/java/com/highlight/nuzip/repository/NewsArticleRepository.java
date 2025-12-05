package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    // 링크 중복 검사를 위한 메서드 (기존)
    Optional<NewsArticle> findByOriginalLink(String originalLink);

    // 카테고리별로 뉴스 기사를 페이지네이션하여 조회하는 메서드
    Page<NewsArticle> findByCategory(String category, Pageable pageable);

    // 중복 기사 체크
    boolean existsByOriginalLink(String originalLink);

    // 데일리 메일링 기능 각 카테고리별 5개 뉴스 선정
    List<NewsArticle> findTop5ByCategoryOrderByPublishedAtDesc(String label);

    Page<NewsArticle> findByTitleContainingOrSummaryContainingOrKeywordsContainingOrderByPublishedAtDesc(
            String titleKeyword,
            String summaryKeyword,
            String keywordsKeyword,
            Pageable pageable
    );
}