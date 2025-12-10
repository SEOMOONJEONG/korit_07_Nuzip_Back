package com.highlight.nuzip.service;

import com.highlight.nuzip.repository.CategoryRepository;
import com.highlight.nuzip.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;

    // 사용자가 카테고리 구독
    public void subscribe(String userId, Long categoryId) {
        // 카테고리 존재 여부 확인
        if(!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found");
        }

        // 이미 구독했는지 확인
        if(subscriptionRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            throw new RuntimeException("Already subscribed");
        }


    }
}
