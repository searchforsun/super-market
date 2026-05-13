package com.supermarket.category.controller;

import com.supermarket.category.entity.Category;
import com.supermarket.category.service.CategoryService;
import com.supermarket.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public R<Category> create(@RequestBody Category category) {
        return R.ok(categoryService.create(category));
    }

    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryService.getFullTree());
    }

    @GetMapping("/children/{parentId}")
    public R<List<Category>> children(@PathVariable Long parentId) {
        return R.ok(categoryService.getChildren(parentId));
    }

    @GetMapping("/level/{level}")
    public R<List<Category>> byLevel(@PathVariable int level) {
        return R.ok(categoryService.getByLevel(level));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
