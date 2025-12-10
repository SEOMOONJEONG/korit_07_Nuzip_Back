package com.highlight.nuzip.model;

// enum: 카테고리 코드+라벨
public enum NewsCategory {
    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    LIFE_CULTURE("생활ㆍ문화"),
    IT_SCIENCE("ITㆍ과학"),
    WORLD("세계"),
    ENTERTAINMENT("엔터"),
    SPORTS("스포츠");

    private final String label;

    NewsCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}