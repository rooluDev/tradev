package com.tradev.common.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorPageResponse<T> {

    private final List<T> items;
    private final String nextCursor;
    private final boolean hasNext;

    public CursorPageResponse(List<T> items, String nextCursor, boolean hasNext) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorPageResponse<T> of(List<T> items, int pageSize, java.util.function.Function<T, String> cursorExtractor) {
        boolean hasNext = items.size() > pageSize;
        List<T> pageItems = hasNext ? items.subList(0, pageSize) : items;
        String nextCursor = hasNext ? cursorExtractor.apply(pageItems.get(pageItems.size() - 1)) : null;
        return new CursorPageResponse<>(pageItems, nextCursor, hasNext);
    }
}
