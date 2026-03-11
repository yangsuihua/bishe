package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.admin.dto.CategoryDTO;
import com.videoplatform.admin.entity.VideoCategory;
import com.videoplatform.admin.mapper.VideoCategoryMapper;
import com.videoplatform.admin.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryManageService {
    
    private final VideoCategoryMapper categoryMapper;
    private final VideoMapper videoMapper;
    
    /**
     * 获取分类树形列表
     */
    public List<CategoryDTO> getCategoryTree() {
        List<VideoCategory> allCategories = categoryMapper.selectList(null);
        
        // 构建树形结构
        List<CategoryDTO> rootCategories = allCategories.stream()
                .filter(cat -> cat.getParentId() == null || cat.getParentId() == 0)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        for (CategoryDTO root : rootCategories) {
            buildChildren(root, allCategories);
        }
        
        return rootCategories;
    }
    
    private void buildChildren(CategoryDTO parent, List<VideoCategory> allCategories) {
        List<CategoryDTO> children = allCategories.stream()
                .filter(cat -> parent.getId().equals(cat.getParentId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        parent.setChildren(children);
        
        for (CategoryDTO child : children) {
            buildChildren(child, allCategories);
        }
    }
    
    private CategoryDTO convertToDTO(VideoCategory category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setSort(category.getSort());
        dto.setIcon(category.getIcon());
        dto.setStatus(category.getStatus());
        dto.setChildren(new ArrayList<>());
        return dto;
    }
    
    /**
     * 创建分类
     */
    @Transactional
    public void createCategory(CategoryDTO dto) {
        VideoCategory category = new VideoCategory();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0);
        category.setSort(dto.getSort() != null ? dto.getSort() : 0);
        category.setIcon(dto.getIcon());
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        categoryMapper.insert(category);
    }
    
    /**
     * 更新分类
     */
    @Transactional
    public void updateCategory(Integer id, CategoryDTO dto) {
        VideoCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        category.setName(dto.getName());
        if (dto.getParentId() != null) {
            category.setParentId(dto.getParentId());
        }
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        
        categoryMapper.updateById(category);
    }
    
    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Integer id) {
        VideoCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否有子分类
        LambdaQueryWrapper<VideoCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoCategory::getParentId, id);
        Long childCount = categoryMapper.selectCount(wrapper);
        if (childCount > 0) {
            throw new RuntimeException("该分类下还有" + childCount + "个子分类，无法删除");
        }
        
        // 检查是否有视频使用该分类
        LambdaQueryWrapper<com.videoplatform.admin.entity.Video> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.eq(com.videoplatform.admin.entity.Video::getCategoryId, id)
                .eq(com.videoplatform.admin.entity.Video::getDeleted, 0);
        Long videoCount = videoMapper.selectCount(videoWrapper);
        if (videoCount > 0) {
            throw new RuntimeException("该分类下还有" + videoCount + "个视频，无法删除");
        }
        
        categoryMapper.deleteById(id);
    }
    
    /**
     * 调整分类排序
     */
    @Transactional
    public void updateCategorySort(Integer id, Integer sort) {
        VideoCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        category.setSort(sort);
        categoryMapper.updateById(category);
    }
}
