package com.tradev.domain.category.service;

import com.tradev.domain.category.dto.CategoryResponse;
import com.tradev.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getTree() {
        return categoryRepository.findAllRootWithChildren()
                .stream()
                .map(CategoryResponse::new)
                .toList();
    }
}
