package com.highlight.nuzip.dto;

import com.highlight.nuzip.model.NewsArticle;
import lombok.Data; // Lombok의 @Data 어노테이션 사용
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// 클라이언트에게 전송할 기사 데이터를 담는 DTO (Data Transfer Object). Lombok의 @Data를 사용하여 Getter/Setter/equals/hashCode/toString을 자동 생성합니다.
@Data
@NoArgsConstructor
public class NewsArticleDto {

    private Long id;
    private String title;
    private String originalLink;
    private String imageUrl;
    private String summary;
    private String keywords;
    private String category;
    private LocalDateTime publishedAt;
    private LocalDateTime collectedAt;

    // NewsArticle 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static NewsArticleDto fromEntity(NewsArticle article) {
        NewsArticleDto dto = new NewsArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setOriginalLink(article.getOriginalLink());
        dto.setImageUrl(article.getImageUrl());
        dto.setSummary(article.getSummary());

        // 엔티티의 category 필드 값을 DTO에 설정 (엔티티에 필드가 존재한다고 가정)
        dto.setCategory(article.getCategory());

        dto.setKeywords(article.getKeywords());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setCollectedAt(article.getCollectedAt());
        return dto;
    }
}