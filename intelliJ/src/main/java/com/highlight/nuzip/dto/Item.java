package com.highlight.nuzip.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// 네이버 뉴스 검색 API의 각 기사 항목을 나타내는 DTO입니다.
@Data
@NoArgsConstructor
public class Item {
    private String title;        // 뉴스 제목 (HTML 태그 포함)
    private String originallink; // 원본 뉴스 기사 링크 (네이버 API 필드명 그대로 사용)
    private String link;         // 네이버 검색 결과 링크
    private String description;  // 기사 내용 요약 (HTML 태그 포함)
    private String pubDate;      // 기사 발행일 (날짜 형식 문자열)

    // 뉴스 제목에서 HTML 태그를 제거하고 반환합니다. @return HTML 태그가 제거된 깨끗한 제목
    public String getCleanTitle() {
        return removeHtmlTags(this.title);
    }

    // 기사 내용 요약에서 HTML 태그를 제거하고 반환합니다. @return HTML 태그가 제거된 깨끗한 내용 요약
    public String getCleanDescription() {
        return removeHtmlTags(this.description);
    }

    // 문자열에서 모든 HTML 태그를 제거하는 내부 헬퍼 메서드
    private String removeHtmlTags(String htmlText) {
        if (htmlText == null || htmlText.isEmpty()) {
            return "";
        }
        // 정규식을 사용하여 <...> 형태의 모든 HTML 태그를 제거합니다.
        return htmlText.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
    }
}