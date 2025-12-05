package com.highlight.nuzip.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "scraps")
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;           // 로그인 사용자 ID
    private String title;     // 기사 제목
    private String url;       // 기사 링크
    //    private String thumbnailUrl;     // 썸네일 이미지 (선택)
    private String summary;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 하나의 Scrap은 여러 개의 Memo를 가질 수 있음.
    @OneToMany(mappedBy = "scrap", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Memo> memos = new ArrayList<>();  // 이 Scrap에 연결된 Memo 목록 (컬렉션 초기화 필수!)


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    @PreUpdate // 엔티티가 db에 업데이트 되기 전에 실행되는 메서드
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

     // 양방향 매핑
    public void addMemo(Memo memo) {
        memos.add(memo);
        memo.setScrap(this);
    }
}
