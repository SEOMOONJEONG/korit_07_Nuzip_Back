package com.highlight.nuzip.controller;


import com.highlight.nuzip.dto.*;
import com.highlight.nuzip.model.Rating;
import com.highlight.nuzip.service.GeminiService;
import com.highlight.nuzip.service.MemoService;
import com.highlight.nuzip.service.RatingService;
import com.highlight.nuzip.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class UserContentController {
    private final ScrapService scrapService;
    private final MemoService memoService;
    private final RatingService ratingService;
    private final GeminiService geminiService;

    // ========================== 스크랩 ==========================
    @PostMapping("/scrap")
    public ResponseEntity<ScrapResponse> createScrap(@RequestBody ScrapRequest request,
                                                     Principal principal) {
        if(principal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = principal.getName();
        // 스크랩 생성
        ScrapResponse response = scrapService.createScrap(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping("/scrap")
    public ResponseEntity<List<ScrapResponse>> getScrap(Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = principal.getName();
        return ResponseEntity.ok(scrapService.getScrapsByUser(userId));
    }

    @DeleteMapping("/scrap/{id}")
    public ResponseEntity<ScrapResponse> deleteScrap(@PathVariable Long id){
        scrapService.deleteScrap(id);
        return ResponseEntity.noContent().build();
    }


    // ========================== 메모 ==========================
    @PostMapping("/memo")
    public ResponseEntity<MemoResponse> createMemo(@RequestBody MemoRequest request){
        return ResponseEntity.status(201).body(memoService.createMemo(request));
    }

    @GetMapping("/memo/{scrapId}")
    public ResponseEntity<List<MemoResponse>> getMemosByScrap(@PathVariable Long scrapId){
        return ResponseEntity.ok(memoService.getMemos(scrapId));
    }

    @PutMapping("/memo/{id}")
    public ResponseEntity<MemoResponse> updateMemo(@PathVariable Long id, @RequestBody MemoRequest request){
        return ResponseEntity.ok(memoService.updateMemo(id, request));
    }

    @DeleteMapping("/memo/{id}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long id){
        memoService.deleteMemo(id);
        return ResponseEntity.noContent().build();
    }


    // ========================== 별점 ==========================
    @PostMapping("/rating")
    public ResponseEntity<Rating> rateArticle(@RequestBody RatingRequest request,
                                              Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = principal.getName();
        Rating saved = ratingService.saveOrUpdateRating(request, userId);

        if(request.getSendToGemini() != null && request.getSendToGemini()) {
            try {
                String feedback = "별점: " + saved.getRating() + ", 피드백: " + saved.getFeedback();
                geminiService.trainFeedback(feedback);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(saved);
    }

    @PostMapping
    public ResponseEntity<Rating> saveRating(@RequestBody RatingRequest request,
                                             Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = principal.getName();
        Rating saved = ratingService.saveOrUpdateRating(request, userId);
        return ResponseEntity.ok(saved);
    }

}
