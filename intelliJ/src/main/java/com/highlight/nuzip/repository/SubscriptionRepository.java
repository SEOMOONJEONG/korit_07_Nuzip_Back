package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    boolean existsByUserIdAndCategoryId(String userId, Long categoryId);
    List<Subscription> findByCategoryId(Long categoryId);
    void deleteByUserId(String userId);
}
