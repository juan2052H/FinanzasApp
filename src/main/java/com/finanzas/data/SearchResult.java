package com.finanzas.data;

public final class SearchResult {
    private final String type;
    private final String title;
    private final String detail;
    private final String section;

    public SearchResult(String type, String title, String detail, String section) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.section = section;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getSection() {
        return section;
    }
}
