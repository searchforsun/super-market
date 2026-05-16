package com.supermarket.category.controller;

import com.supermarket.category.entity.Category;
import com.supermarket.category.service.CategoryService;
import com.supermarket.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Tag(name = "分类服务", description = "商品分类树、层级管理接口")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "新增分类")
    @PostMapping
    public R<Category> create(@Parameter(description = "分类信息") @RequestBody Category category) {
        return R.ok(categoryService.create(category));
    }

    @Operation(summary = "修改分类")
    @PutMapping
    public R<Category> update(@Parameter(description = "分类信息") @RequestBody Category category) {
        return R.ok(categoryService.update(category));
    }

    @Operation(summary = "获取全部分类树")
    @GetMapping("/tree")
    public R<List<Category>> tree() {
        return R.ok(categoryService.getFullTree());
    }

    @Operation(summary = "获取子分类列表")
    @GetMapping("/children/{parentId}")
    public R<List<Category>> children(@Parameter(description = "父分类ID") @PathVariable Long parentId) {
        return R.ok(categoryService.getChildren(parentId));
    }

    @Operation(summary = "按层级获取分类")
    @GetMapping("/level/{level}")
    public R<List<Category>> byLevel(@Parameter(description = "分类层级") @PathVariable int level) {
        return R.ok(categoryService.getByLevel(level));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
