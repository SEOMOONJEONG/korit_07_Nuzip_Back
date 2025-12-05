package com.highlight.nuzip.service;

import com.highlight.nuzip.dto.MemoRequest;
import com.highlight.nuzip.dto.MemoResponse;
import com.highlight.nuzip.model.Memo;
import com.highlight.nuzip.model.Scrap;
import com.highlight.nuzip.repository.MemoRepository;
import com.highlight.nuzip.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final ScrapRepository scrapRepository;

    // 메모 작성 (스크랩과 연결)
    @Transactional
    public MemoResponse createMemo(MemoRequest request){
        Scrap scrap = scrapRepository.findById(request.scrapId())
                .orElseThrow(() -> new IllegalArgumentException("Scrap not found"));

        Memo memo = Memo.builder()
                .content(request.content())
                .scrap(scrap)                // scrap 연관관계 설정
                .userId(scrap.getUserId())
                .build();

        Memo saved = memoRepository.save(memo);
        return toResponse(saved);
    }

    // 기사별 메모 조회
    @Transactional(readOnly = true)
    public List<MemoResponse> getMemos(Long scrapId){
        return memoRepository.findByScrapIdWithScrap(scrapId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 메모 수정
    @Transactional
    public MemoResponse updateMemo(Long id, MemoRequest request){
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memo not found with id: " + id));
        memo.updateContent(request.content());
        memoRepository.save(memo);
        return toResponse(memo);
    }

    // 메모 삭제
    @Transactional
    public void deleteMemo(Long id){
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memo not found with id: " + id));

        memoRepository.delete(memo);
    }

    // Entity -> Response 변환
    private MemoResponse toResponse(Memo memo){
        String formattedDate = memo.getUpdatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return new MemoResponse(
                memo.getId(),
                memo.getScrap().getUserId(),
                memo.getContent(), formattedDate
        );
    }
}