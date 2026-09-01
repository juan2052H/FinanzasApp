package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.CategoryDtos;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/categories")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryDtos.CategoryResponse> list(Authentication authentication,
                                                    @PathVariable UUID workspaceId,
                                                    @RequestParam(required = false) CategoryType type,
                                                    @RequestParam(defaultValue = "false") boolean includeArchived,
                                                    @RequestParam(required = false) String q) {
        return categories.list(CurrentUser.id(authentication), workspaceId, type, includeArchived, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.CategoryResponse create(Authentication authentication, @PathVariable UUID workspaceId,
                                                @Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return categories.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @PutMapping("/{categoryId}")
    public CategoryDtos.CategoryResponse update(Authentication authentication, @PathVariable UUID workspaceId,
                                                @PathVariable UUID categoryId,
                                                @Valid @RequestBody CategoryDtos.CategoryRequest request) {
        return categories.update(CurrentUser.id(authentication), workspaceId, categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID categoryId) {
        categories.archive(CurrentUser.id(authentication), workspaceId, categoryId);
    }

    @PostMapping("/{categoryId}/restore")
    public CategoryDtos.CategoryResponse restore(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID categoryId) {
        return categories.restore(CurrentUser.id(authentication), workspaceId, categoryId);
    }
}
