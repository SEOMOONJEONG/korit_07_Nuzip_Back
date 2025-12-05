package com.highlight.nuzip.service;

import com.highlight.nuzip.dto.MemoResponse;
import com.highlight.nuzip.dto.ScrapRequest;
import com.highlight.nuzip.dto.ScrapResponse;
import com.highlight.nuzip.model.Scrap;
import com.highlight.nuzip.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapService {
    private final ScrapRepository scrapRepository;
    private static final int SUMMARY_MAX_LENGTH = 255;

    // 스크랩 추가(저장)
    @Transactional
    public ScrapResponse createScrap(ScrapRequest request, String userId){
        String summary = normalizeSummary(request.summary());
        Scrap scrap = Scrap.builder()
                .userId(userId)
                .title(request.title())
                .url(request.url())
                .summary(summary)
                .build();

        Scrap saved = scrapRepository.save(scrap);
        return toResponse(saved);
    }

    // 사용자별 스크랩 조회
    @Transactional(readOnly = true)
    public List<ScrapResponse> getScrapsByUser(String userId) {
        // Repository의 쿼리 메서드를 사용하여 특정 userId가 작성한 스크랩을 최신순으로 조회
        return scrapRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse) // 각 Scrap 엔티티를 ScrapResponse DTO로 변환
                .collect(Collectors.toList()); // 변환된 DTO 목록을 List로 수집하여 반환
    }

    // 단일 스크랩 상세 조회
    @Transactional(readOnly = true)
    public ScrapResponse getScraps(Long id) {
        Scrap scrap = scrapRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scrap not found"));
        return toResponse(scrap);
    }

    // 스크랩 삭제
    public void deleteScrap(Long id) {
        scrapRepository.deleteById(id);
    }

    // Entity → Response 변환
    private ScrapResponse toResponse(Scrap scrap) {
        String formattedDate = scrap.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<MemoResponse> memoResponses = scrap.getMemos() == null ?
                List.of() :
                scrap.getMemos().stream()
                        .map(memo -> new MemoResponse(
                                memo.getId(),
                                memo.getScrap().getUserId(),
                                memo.getContent(),
                                memo.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        ))
                        .collect(Collectors.toList());


        return new ScrapResponse(
                scrap.getId(),
                scrap.getTitle(),
                scrap.getUrl(),
                scrap.getSummary(),
                formattedDate,
                scrap.getUserId(),
                memoResponses
        );
    }

    private String normalizeSummary(String summary) {
        if (summary == null) {
            return "";
        }
        String trimmed = summary.trim();
        if (trimmed.length() <= SUMMARY_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, SUMMARY_MAX_LENGTH);
    }

}