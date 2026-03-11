package com.videoplatform.admin.controller;

import com.videoplatform.admin.dto.CategoryDTO;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.CategoryManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryManageController {
    
    private final CategoryManageService categoryManageService;
    
    @GetMapping
    public Result<List<CategoryDTO>> getCategoryTree() {
        List<CategoryDTO> result = categoryManageService.getCategoryTree();
        return Result.success(result);
    }
    
    @PostMapping
    public Result<String> createCategory(@RequestBody CategoryDTO dto) {
        categoryManageService.createCategory(dto);
        return Result.success("创建成功");
    }
    
    @PutMapping("/{id}")
    public Result<String> updateCategory(@PathVariable Integer id, @RequestBody CategoryDTO dto) {
        categoryManageService.updateCategory(id, dto);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/{id}")
    public Result<String> deleteCategory(@PathVariable Integer id) {
        categoryManageService.deleteCategory(id);
        return Result.success("删除成功");
    }
    
    @PutMapping("/{id}/sort")
    public Result<String> updateCategorySort(@PathVariable Integer id, @RequestParam Integer sort) {
        categoryManageService.updateCategorySort(id, sort);
        return Result.success("排序更新成功");
    }
}
