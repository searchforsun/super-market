package com.supermarket.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.category.entity.Category;
import com.supermarket.category.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateWhenValidBody() throws Exception {
        // Arrange
        Category category = new Category();
        category.setName("电子产品");
        category.setParentId(0L);
        category.setLevel(1);

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("电子产品");
        savedCategory.setParentId(0L);
        savedCategory.setLevel(1);

        when(categoryService.create(any(Category.class))).thenReturn(savedCategory);

        // Act & Assert
        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("电子产品"));
        verify(categoryService).create(any(Category.class));
    }

    @Test
    void shouldUpdateWhenValidBody() throws Exception {
        // Arrange
        Category category = new Category();
        category.setId(1L);
        category.setName("电子设备");

        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("电子设备");

        when(categoryService.update(any(Category.class))).thenReturn(updatedCategory);

        // Act & Assert
        mockMvc.perform(put("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(category)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("电子设备"));
        verify(categoryService).update(any(Category.class));
    }

    @Test
    void shouldGetTree() throws Exception {
        // Arrange
        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("电子产品");
        electronics.setLevel(1);

        Category clothing = new Category();
        clothing.setId(2L);
        clothing.setName("服装");
        clothing.setLevel(1);

        when(categoryService.getFullTree()).thenReturn(List.of(electronics, clothing));

        // Act & Assert
        mockMvc.perform(get("/api/category/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("电子产品"))
                .andExpect(jsonPath("$.data[1].name").value("服装"));
        verify(categoryService).getFullTree();
    }

    @Test
    void shouldGetChildrenWhenValidParentId() throws Exception {
        // Arrange
        Category child = new Category();
        child.setId(3L);
        child.setName("手机");
        child.setParentId(1L);
        child.setLevel(2);

        when(categoryService.getChildren(1L)).thenReturn(List.of(child));

        // Act & Assert
        mockMvc.perform(get("/api/category/children/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("手机"));
        verify(categoryService).getChildren(1L);
    }

    @Test
    void shouldGetByLevelWhenValidLevel() throws Exception {
        // Arrange
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("电子产品");
        cat.setLevel(1);

        when(categoryService.getByLevel(1)).thenReturn(List.of(cat));

        // Act & Assert
        mockMvc.perform(get("/api/category/level/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("电子产品"));
        verify(categoryService).getByLevel(1);
    }

    @Test
    void shouldDeleteWhenValidId() throws Exception {
        // Arrange
        doNothing().when(categoryService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        verify(categoryService).delete(1L);
    }
}
