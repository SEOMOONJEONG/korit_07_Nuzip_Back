package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByScrapIdAndUserId(Long scrapId, String userId);
}
