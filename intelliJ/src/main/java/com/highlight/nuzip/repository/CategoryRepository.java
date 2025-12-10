package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.Category; // 변경된 엔티티 사용
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Category 엔티티를 관리하는 리포지토리입니다. (뉴스 수집 대상 카테고리 관리)
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 수집 작업에 활성화된(TRUE) 모든 카테고리를 조회합니다.
    @Query("SELECT c FROM Category c WHERE c.isActiveForCollection = TRUE")
    List<Category> findActiveForCollection();

    Optional<Category> findByNameIgnoreCase(String name);
}