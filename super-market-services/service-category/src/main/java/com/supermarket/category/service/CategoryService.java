package com.supermarket.category.service;

import com.supermarket.category.entity.Category;
import java.util.List;

public interface CategoryService {
    Category create(Category category);
    Category update(Category category);
    void delete(Long id);
    Category getById(Long id);
    List<Category> getChildren(Long parentId);
    List<Category> getFullTree();
    List<Category> getByLevel(int level);
}
