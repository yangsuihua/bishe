package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.TagDTO;
import com.videoplatform.admin.entity.VideoTag;
import com.videoplatform.admin.mapper.VideoTagMapper;
import com.videoplatform.admin.mapper.VideoTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagManageService {
    
    private final VideoTagMapper tagMapper;
    private final VideoTagRelationMapper tagRelationMapper;
    
    /**
     * 分页查询标签列表
     */
    public Page<TagDTO> getTagList(Integer page, Integer size) {
        Page<VideoTag> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<VideoTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VideoTag::getCreatedAt);
        
        Page<VideoTag> result = tagMapper.selectPage(pageObj, wrapper);
        
        Page<TagDTO> dtoPage = new Page<>(page, size, result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(tag -> {
            TagDTO dto = new TagDTO();
            dto.setId(tag.getId());
            dto.setName(tag.getName());
            dto.setVideoCount(tag.getVideoCount());
            return dto;
        }).toList());
        
        return dtoPage;
    }
    
    /**
     * 创建标签
     */
    @Transactional
    public void createTag(TagDTO dto) {
        VideoTag tag = new VideoTag();
        tag.setName(dto.getName());
        tag.setVideoCount(0);
        tagMapper.insert(tag);
    }
    
    /**
     * 更新标签
     */
    @Transactional
    public void updateTag(Integer id, TagDTO dto) {
        VideoTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        tag.setName(dto.getName());
        tagMapper.updateById(tag);
    }
    
    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Integer id) {
        VideoTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        // 检查是否有关联视频
        int count = tagRelationMapper.countByTagId(id);
        if (count > 0) {
            throw new RuntimeException("该标签下还有" + count + "个视频，无法删除");
        }
        
        tagMapper.deleteById(id);
    }
    
    /**
     * 获取标签详情
     */
    public TagDTO getTagDetail(Integer id) {
        VideoTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setVideoCount(tag.getVideoCount());
        return dto;
    }
}
