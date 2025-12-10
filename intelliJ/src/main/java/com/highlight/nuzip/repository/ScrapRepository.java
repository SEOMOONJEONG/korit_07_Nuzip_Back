package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    List<Scrap> findByUserIdOrderByCreatedAtDesc(String userId);
}
