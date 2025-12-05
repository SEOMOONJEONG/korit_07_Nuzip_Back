package com.highlight.nuzip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

//뉴스 기사 엔티티 (NewsArticle)
@Entity
@Table(name = "news_article")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter // 🌟 Lombok Getter 적용
@Setter // 🌟 Lombok Setter 적용
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryId;

    private String title;

    // 원본 링크는 중복 불가 및 길이 제한 설정
    @Column(name = "original_link", unique = true, nullable = false, length = 500)
    private String originalLink;

    private String imageUrl; // 이미지 URL

    // Summary는 매우 길 수 있으므로 TEXT 타입으로 지정
    @Column(columnDefinition = "TEXT")
    private String summary;

    // Keywords 역시 길어질 수 있으므로 TEXT 타입으로 지정
    @Column(columnDefinition = "TEXT")
    private String keywords;

    private String category;
    private LocalDateTime publishedAt;
    private LocalDateTime collectedAt;

    // 이 필드는 DB에 필수(nullable=false)이며, 생성 후에는 업데이트되지 않도록 설정합니다.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 엔티티가 영속화(DB에 저장)되기 전에 실행되어 createdAt 및 collectedAt 필드를 현재 시각으로 자동 설정합니다.
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // collectedAt이 null일 경우에만 현재 시각으로 설정합니다.
        if (this.collectedAt == null) {
            this.collectedAt = LocalDateTime.now();
        }
    }
}