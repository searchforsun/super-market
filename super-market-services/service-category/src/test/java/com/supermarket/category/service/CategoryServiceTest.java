package com.supermarket.category.service;

import com.supermarket.category.entity.Category;
import com.supermarket.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        Category cat = new Category();
        cat.setName("电子产品");
        cat.setParentId(0L);
        Category saved = categoryService.create(cat);
        assertThat(saved.getLevel()).isEqualTo(1);
    }

    @Test
    void shouldCreateSubCategoryWithCorrectLevel() {
        Category parent = new Category();
        parent.setName("服装");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("男装");
        child.setParentId(parent.getId());
        Category saved = categoryService.create(child);
        assertThat(saved.getLevel()).isEqualTo(2);
    }

    @Test
    void shouldNotDeleteCategoryWithChildren() {
        Category parent = new Category();
        parent.setName("家电");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("冰箱");
        child.setParentId(parent.getId());
        categoryService.create(child);

        assertThatThrownBy(() -> categoryService.delete(parent.getId()))
            .hasMessageContaining("子类目");
    }

    @Test
    void shouldGetChildren() {
        Category parent = new Category();
        parent.setName("食品");
        parent.setParentId(0L);
        categoryService.create(parent);

        Category child = new Category();
        child.setName("零食");
        child.setParentId(parent.getId());
        categoryService.create(child);

        var children = categoryService.getChildren(parent.getId());
        assertThat(children).hasSize(1);
    }
}
