package com.highlight.nuzip.service;

import com.highlight.nuzip.dto.RatingRequest;
import com.highlight.nuzip.model.Rating;
import com.highlight.nuzip.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final GeminiService geminiService;

    @Transactional
    public Rating saveOrUpdateRating(RatingRequest request, String userId) {
        Rating saved = ratingRepository.findByScrapIdAndUserId(request.getScrapId(), userId)
                .map(existing -> {
                    if (request.getRating() != null) existing.setRating(request.getRating());
                    if (request.getFeedback() != null) existing.setFeedback(request.getFeedback());
                    return ratingRepository.save(existing);
                })
                .orElseGet(() -> {
                    Rating newRating = Rating.builder()
                            .scrapId(request.getScrapId())
                            .userId(userId)
                            .rating(request.getRating())
                            .feedback(request.getFeedback())
                            .build();
                    return ratingRepository.save(newRating);
                });

        return saved;
    }

}
