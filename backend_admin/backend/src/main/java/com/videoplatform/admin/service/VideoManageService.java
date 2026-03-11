package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.VideoQueryDTO;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.entity.Video;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.mapper.VideoMapper;
import com.videoplatform.admin.vo.VideoAuditVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoManageService {
    
    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    
    /**
     * 分页查询视频列表
     */
    public Page<VideoAuditVO> getVideoList(VideoQueryDTO dto) {
        Page<Video> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(Video::getDeleted, 0);
        
        if (dto.getStatus() != null) {
            wrapper.eq(Video::getStatus, dto.getStatus());
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(Video::getCategoryId, dto.getCategoryId());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(Video::getUserId, dto.getUserId());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(Video::getTitle, dto.getKeyword())
                    .or().like(Video::getDescription, dto.getKeyword()));
        }
        
        wrapper.orderByDesc(Video::getCreatedAt);
        
        Page<Video> result = videoMapper.selectPage(page, wrapper);
        
        Page<VideoAuditVO> voPage = new Page<>(dto.getPage(), dto.getSize(), result.getTotal());
        List<VideoAuditVO> voList = result.getRecords().stream().map(video -> {
            VideoAuditVO vo = new VideoAuditVO();
            vo.setId(video.getId());
            vo.setTitle(video.getTitle());
            vo.setDescription(video.getDescription());
            vo.setCoverUrl(video.getCoverUrl());
            vo.setVideoUrl(video.getVideoUrl());
            vo.setUserId(video.getUserId());
            vo.setStatus(video.getStatus());
            vo.setCreatedAt(video.getCreatedAt());
            
            User user = userMapper.selectById(video.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    /**
     * 获取视频详情
     */
    public VideoAuditVO getVideoDetail(Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        VideoAuditVO vo = new VideoAuditVO();
        vo.setId(video.getId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setVideoUrl(video.getVideoUrl());
        vo.setUserId(video.getUserId());
        vo.setStatus(video.getStatus());
        vo.setCreatedAt(video.getCreatedAt());
        
        User user = userMapper.selectById(video.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }
        
        return vo;
    }
    
    /**
     * 更新视频信息
     */
    @Transactional
    public void updateVideo(Long id, Video video) {
        Video existVideo = videoMapper.selectById(id);
        if (existVideo == null || existVideo.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        if (StringUtils.hasText(video.getTitle())) {
            existVideo.setTitle(video.getTitle());
        }
        if (video.getDescription() != null) {
            existVideo.setDescription(video.getDescription());
        }
        if (video.getCategoryId() != null) {
            existVideo.setCategoryId(video.getCategoryId());
        }
        
        videoMapper.updateById(existVideo);
    }
    
    /**
     * 删除视频（逻辑删除）
     */
    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        videoMapper.deleteById(id);
    }
    
    /**
     * 修改视频状态
     */
    @Transactional
    public void updateVideoStatus(Long id, Integer status) {
        Video video = videoMapper.selectById(id);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        video.setStatus(status);
        videoMapper.updateById(video);
    }
}
