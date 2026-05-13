package com.supermarket.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supermarket.category.entity.Category;
import com.supermarket.category.mapper.CategoryMapper;
import com.supermarket.category.service.CategoryService;
import com.supermarket.common.core.exception.BizException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public Category create(Category category) {
        if (category.getParentId() != null && category.getParentId() > 0) {
            Category parent = getById(category.getParentId());
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(1);
        }
        save(category);
        return category;
    }

    @Override
    public Category update(Category category) {
        getById(category.getId());
        updateById(category);
        return getById(category.getId());
    }

    @Override
    public void delete(Long id) {
        List<Category> children = getChildren(id);
        if (!children.isEmpty()) {
            throw new BizException(400, "存在子类目,无法删除");
        }
        removeById(id);
    }

    @Override
    public Category getById(Long id) {
        Category cat = super.getById(id);
        if (cat == null) {
            throw new BizException(404, "类目不存在");
        }
        return cat;
    }

    @Override
    public List<Category> getChildren(Long parentId) {
        return list(new LambdaQueryWrapper<Category>()
            .eq(Category::getParentId, parentId)
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));
    }

    @Override
    public List<Category> getFullTree() {
        List<Category> all = list(new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));

        Map<Long, List<Category>> childrenMap = all.stream()
            .collect(Collectors.groupingBy(Category::getParentId));

        return buildTree(0L, childrenMap);
    }

    private List<Category> buildTree(Long parentId, Map<Long, List<Category>> childrenMap) {
        return childrenMap.getOrDefault(parentId, new ArrayList<>());
    }

    @Override
    public List<Category> getByLevel(int level) {
        return list(new LambdaQueryWrapper<Category>()
            .eq(Category::getLevel, level)
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSortOrder));
    }
}
