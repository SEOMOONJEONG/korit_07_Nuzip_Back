package com.highlight.nuzip.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// 네이버 뉴스 검색 API의 최상위 응답 객체입니다.
@Data
@NoArgsConstructor
public class NaverNewsResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<Item> items; // Item DTO를 List로 가집니다.
}
