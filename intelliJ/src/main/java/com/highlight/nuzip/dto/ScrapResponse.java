package com.highlight.nuzip.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ScrapResponse {
    private Long id;
    private String title;
    private String url;
    private String summary;
    private String createdAt;
    private String userId;
    private List<MemoResponse> memos;

    public ScrapResponse(Long id, String title, String url, String summary, String createdAt, String userId, List<MemoResponse> memos) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.summary = summary;
        this.createdAt = createdAt;
        this.userId = userId;
        this.memos = memos;
    }
}
