package com.tradev.domain.category.dto;

import com.tradev.domain.category.entity.Category;
import lombok.Getter;

import java.util.List;

@Getter
public class CategoryResponse {

    private final Long id;
    private final String name;
    private final int depth;
    private final List<CategoryResponse> children;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.depth = category.getDepth();
        this.children = category.getChildren().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(CategoryResponse::new)
                .toList();
    }
}
