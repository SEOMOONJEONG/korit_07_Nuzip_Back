package com.highlight.nuzip.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "memos")
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 다수의 Memo가 하나의 Scrap에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrap_id", nullable = false)
    private Scrap scrap; // Scrap 엔티티와의 연관관계 (어떤 Scrap에 속하는지)

    @PrePersist // 엔티티가 db에 저장되기 전(INSERT 전)에 실행되는 메서드 지정
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(); // 수정 시간을 현재 시간으로 갱신
    }

    // 메모 수정 메서드
    // 메모의 내용을 안전하게 업데이트함
    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now(); // 수정 시간을 현재 시간으로 수동 갱신
    }
}
