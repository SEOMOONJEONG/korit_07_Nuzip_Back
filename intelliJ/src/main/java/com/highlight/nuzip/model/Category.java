package com.highlight.nuzip.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 뉴스 수집 및 분류의 기준이 되는 최상위 카테고리 엔티티입니다.
@Entity
@Table(name = "category") // 테이블 이름을 'category'로 명시
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // 이 카테고리가 뉴스 수집 작업에 사용될지 여부를 나타냅니다.
    @Column(name = "is_active_for_collection", nullable = false)
    private boolean isActiveForCollection = true;
}