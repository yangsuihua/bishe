package com.videoplatform.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.entity.User;
import com.videoplatform.common.entity.Video;
import com.videoplatform.common.entity.VideoCategory;
import com.videoplatform.common.entity.VideoTag;
import com.videoplatform.common.entity.VideoTagRelation;
import com.videoplatform.common.dto.VideoDTO;
import com.videoplatform.common.dto.VideoPublishDTO;
import com.videoplatform.video.mapper.AuditLogMapper;
import com.videoplatform.video.mapper.UserMapper;
import com.videoplatform.video.mapper.VideoCategoryMapper;
import com.videoplatform.video.mapper.VideoMapper;
import com.videoplatform.video.mapper.VideoTagMapper;
import com.videoplatform.video.mapper.VideoTagRelationMapper;
import com.videoplatform.video.entity.AuditLog;
import com.videoplatform.video.service.VideoService;
import com.videoplatform.video.service.VideoCompressionService;
import com.videoplatform.common.feign.SearchFeignClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 视频服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VideoMapper videoMapper;
    private final VideoCategoryMapper videoCategoryMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoTagRelationMapper videoTagRelationMapper;
    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    private final MinioClient minioClient;
    private final VideoCompressionService videoCompressionService;
    private final SearchFeignClient searchFeignClient;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public List<VideoDTO> getVideoFeed(Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        Page<Video> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, 1)
                .eq(Video::getDeleted, 0)
                .orderByDesc(Video::getPublishedAt)
                .orderByDesc(Video::getViewCount)
                .orderByDesc(Video::getLikeCount);
        videoMapper.selectPage(pager, wrapper);
        List<Video> videos = pager.getRecords();
        return convertToDTOs(videos);
    }

    @Override
    public VideoDTO getVideoDetail(Long videoId) {
        if (videoId == null) {
            return null;
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null || (video.getDeleted() != null && video.getDeleted() == 1) || (video.getStatus() != null && video.getStatus() != 1)) {
            return null;
        }
        List<VideoDTO> list = convertToDTOs(Collections.singletonList(video));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public String uploadVideoFile(MultipartFile file, Long userId) {
        File compressedFile = null;
        try {
            // 先压缩视频
            compressedFile = videoCompressionService.compressVideo(file);
            
            // 如果压缩成功，上传压缩后的文件；否则上传原文件
            if (compressedFile != null && compressedFile.exists()) {
                log.info("使用压缩后的视频文件上传: {}", compressedFile.getAbsolutePath());
                return storeCompressedFile(compressedFile, "videos", userId, file.getOriginalFilename());
            } else {
                log.info("视频未压缩，使用原始文件上传");
                return storeFile(file, "videos", userId);
            }
        } catch (Exception e) {
            log.error("视频上传失败", e);
            // 如果压缩失败，尝试上传原文件
            try {
                log.warn("压缩失败，尝试上传原始文件");
                return storeFile(file, "videos", userId);
            } catch (Exception ex) {
                log.error("上传原始文件也失败", ex);
                throw new RuntimeException("视频上传失败: " + ex.getMessage(), ex);
            }
        } finally {
            // 清理压缩后的临时文件
            if (compressedFile != null && compressedFile.exists()) {
                try {
                    boolean deleted = compressedFile.delete();
                    if (!deleted) {
                        log.warn("临时压缩文件删除失败: {}", compressedFile.getAbsolutePath());
                        compressedFile.deleteOnExit();
                    }
                } catch (Exception e) {
                    log.warn("删除临时压缩文件时出错", e);
                    compressedFile.deleteOnExit();
                }
            }
        }
    }

    @Override
    public String uploadCoverFile(MultipartFile file, Long userId) {
        return storeFile(file, "covers", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishVideo(Long userId, VideoPublishDTO dto) {
        if (userId == null || dto == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        Video video = new Video();
        video.setUserId(userId);
        video.setTitle(dto.getTitle());
        video.setDescription(dto.getDescription());
        video.setCoverUrl(dto.getCoverUrl());
        video.setVideoUrl(dto.getVideoUrl());
        video.setCategoryId(dto.getCategoryId());
        video.setStatus(0);
        video.setIsPrivate(dto.getIsPrivate() == null ? 0 : dto.getIsPrivate());
        video.setViewCount(0L);
        video.setLikeCount(0);
        video.setCommentCount(0);
        video.setShareCount(0);
        video.setFavoriteCount(0);
        video.setPublishedAt(now);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        videoMapper.insert(video);

        handleTags(dto.getTags(), video.getId());
        return video.getId();
    }

    @Override
    public List<VideoDTO> getUserVideos(Long userId, Integer page, Integer size, Long currentUserId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        Page<Video> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getUserId, userId)
                .eq(Video::getDeleted, 0);
        
        // 如果当前用户不是视频所有者，只返回已发布的视频（status=1）
        // 如果当前用户是视频所有者（currentUserId != null && currentUserId.equals(userId)），返回所有状态的视频
        if (currentUserId == null || !currentUserId.equals(userId)) {
            wrapper.eq(Video::getStatus, 1);
        }
        
        wrapper.orderByDesc(Video::getCreatedAt);
        videoMapper.selectPage(pager, wrapper);
        return convertToDTOs(pager.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return;
        }
        if (!userId.equals(video.getUserId())) {
            throw new IllegalArgumentException("无权限删除该视频");
        }
        LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .set(Video::getDeleted, 1)
                .set(Video::getUpdatedAt, LocalDateTime.now());
        videoMapper.update(null, update);
        
        // 同步删除 ES 索引
        try {
            searchFeignClient.deleteVideo(videoId);
        } catch (Exception e) {
            log.error("同步删除 ES 索引失败, videoId: {}", videoId, e);
        }
    }

    @Override
    public List<VideoCategory> getCategories() {
        LambdaQueryWrapper<VideoCategory> wrapper = new LambdaQueryWrapper<VideoCategory>()
                .eq(VideoCategory::getStatus, 1)
                .orderByAsc(VideoCategory::getSort)
                .orderByAsc(VideoCategory::getId);
        return videoCategoryMapper.selectList(wrapper);
    }

    @Override
    public List<VideoTag> getVideoTags() {
        return videoTagMapper.selectList(null);
    }
    
    @Override
    public List<VideoDTO> getVideosByIds(List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询视频信息，只获取status为1且未删除的视频
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .in(Video::getId, videoIds)
                .eq(Video::getStatus, 1)
                .eq(Video::getDeleted, 0);
        List<Video> videos = videoMapper.selectList(wrapper);
        
        if (videos.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询对应的用户信息
        Set<Long> userIds = new HashSet<>();
        for (Video video : videos) {
            if (video.getUserId() != null) {
                userIds.add(video.getUserId());
            }
        }
        
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(new ArrayList<>(userIds));
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }
        
        // 转换为DTO并设置用户信息
        List<VideoDTO> result = new ArrayList<>();
        for (Video video : videos) {
            VideoDTO dto = convertToDTO(video, userMap.get(video.getUserId()));
            result.add(dto);
        }
        return result;
    }
    
    private VideoDTO convertToDTO(Video video, User user) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setUserId(video.getUserId());
        
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
        }
        
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setCoverUrl(video.getCoverUrl());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setDuration(video.getDuration());
        
        // 查询分类名称
        if (video.getCategoryId() != null) {
            VideoCategory category = videoCategoryMapper.selectById(video.getCategoryId());
            if (category != null) {
                dto.setCategory(category.getName());
            }
        }
        
        dto.setViewCount(video.getViewCount());
        dto.setLikeCount(video.getLikeCount());
        dto.setCommentCount(video.getCommentCount());
        dto.setShareCount(video.getShareCount());
        dto.setFavoriteCount(video.getFavoriteCount());
        dto.setStatus(video.getStatus());
        LocalDateTime time = video.getPublishedAt() != null ? video.getPublishedAt() : video.getCreatedAt();
        dto.setCreatedAt(time == null ? null : DATE_TIME_FORMATTER.format(time));
        
        dto.setIsLiked(Boolean.FALSE);
        dto.setIsFavorited(Boolean.FALSE);
        dto.setIsFollowing(Boolean.FALSE);
        
        return dto;
    }

    @Override
    public List<VideoDTO> getVideosByCategory(Integer categoryId, Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        Page<Video> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getCategoryId, categoryId)
                .eq(Video::getStatus, 1)
                .eq(Video::getDeleted, 0)
                .orderByDesc(Video::getPublishedAt)
                .orderByDesc(Video::getViewCount)
                .orderByDesc(Video::getLikeCount);
        videoMapper.selectPage(pager, wrapper);
        List<Video> videos = pager.getRecords();
        return convertToDTOs(videos);
    }

    private List<VideoDTO> convertToDTOs(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<Long>();
        Set<Integer> categoryIds = new HashSet<Integer>();
        for (Video video : videos) {
            if (video.getUserId() != null) {
                userIds.add(video.getUserId());
            }
            if (video.getCategoryId() != null) {
                categoryIds.add(video.getCategoryId());
            }
        }

        Map<Long, User> userMap = loadUsers(userIds);
        Map<Integer, VideoCategory> categoryMap = loadCategories(categoryIds);

        List<VideoDTO> result = new ArrayList<VideoDTO>(videos.size());
        for (Video video : videos) {
            VideoDTO dto = new VideoDTO();
            dto.setId(video.getId());
            dto.setUserId(video.getUserId());

            User user = userMap.get(video.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setAvatar(user.getAvatar());
            }

            dto.setTitle(video.getTitle());
            dto.setDescription(video.getDescription());
            dto.setCoverUrl(video.getCoverUrl());
            dto.setVideoUrl(video.getVideoUrl());
            dto.setDuration(video.getDuration());
            VideoCategory category = categoryMap.get(video.getCategoryId());
            if (category != null) {
                dto.setCategory(category.getName());
            }
            dto.setViewCount(video.getViewCount());
            dto.setLikeCount(video.getLikeCount());
            dto.setCommentCount(video.getCommentCount());
            dto.setShareCount(video.getShareCount());
            dto.setFavoriteCount(video.getFavoriteCount());
            dto.setStatus(video.getStatus());
            LocalDateTime time = video.getPublishedAt() != null ? video.getPublishedAt() : video.getCreatedAt();
            dto.setCreatedAt(time == null ? null : DATE_TIME_FORMATTER.format(time));

            dto.setIsLiked(Boolean.FALSE);
            dto.setIsFavorited(Boolean.FALSE);
            dto.setIsFollowing(Boolean.FALSE);
            result.add(dto);
        }
        return result;
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(new ArrayList<Long>(userIds));
        Map<Long, User> map = new HashMap<Long, User>();
        for (User user : users) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Integer, VideoCategory> loadCategories(Set<Integer> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<VideoCategory> wrapper = new LambdaQueryWrapper<VideoCategory>()
                .in(VideoCategory::getId, categoryIds);
        List<VideoCategory> categories = videoCategoryMapper.selectList(wrapper);
        Map<Integer, VideoCategory> map = new HashMap<Integer, VideoCategory>();
        for (VideoCategory category : categories) {
            map.put(category.getId(), category);
        }
        return map;
    }

    private String storeFile(MultipartFile file, String folder, Long userId) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String extension = originalName != null ? originalName.substring(originalName.lastIndexOf('.')) : ".mp4";
            String fileName = folder + "/" + userId + "/" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // 上传到MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // 拼接访问URL
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + fileName;
            
            return fileUrl;
        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传压缩后的文件到MinIO
     */
    private String storeCompressedFile(File compressedFile, String folder, Long userId, String originalFileName) {
        if (compressedFile == null || !compressedFile.exists()) {
            return null;
        }
        try {
            // 生成唯一文件名（使用mp4扩展名）
            String fileName = folder + "/" + userId + "/" + UUID.randomUUID().toString().substring(0, 8) + ".mp4";
            
            // 读取压缩后的文件并上传到MinIO
            try (InputStream inputStream = new FileInputStream(compressedFile)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, compressedFile.length(), -1)
                        .contentType("video/mp4")
                        .build()
                );
            }
            
            // 拼接访问URL
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + fileName;
            
            log.info("压缩后的视频上传成功: {} (大小: {}MB)", fileUrl, compressedFile.length() / 1024.0 / 1024.0);
            
            return fileUrl;
        } catch (Exception e) {
            log.error("上传压缩后的文件到MinIO失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    private void handleTags(String tags, Long videoId) {
        if (!StringUtils.hasText(tags) || videoId == null) {
            return;
        }
        String[] tagArr = tags.split(",");
        LocalDateTime now = LocalDateTime.now();
        for (String tagName : tagArr) {
            String name = tagName.trim();
            if (!StringUtils.hasText(name)) {
                continue;
            }
            VideoTag tag = videoTagMapper.selectOne(new LambdaQueryWrapper<VideoTag>().eq(VideoTag::getName, name));
            if (tag == null) {
                tag = new VideoTag();
                tag.setName(name);
                tag.setVideoCount(1);
                tag.setCreatedAt(now);
                videoTagMapper.insert(tag);
            } else {
                LambdaUpdateWrapper<VideoTag> update = new LambdaUpdateWrapper<VideoTag>()
                        .eq(VideoTag::getId, tag.getId())
                        .setSql("video_count = video_count + 1");
                videoTagMapper.update(null, update);
            }
            VideoTagRelation relation = new VideoTagRelation();
            relation.setVideoId(videoId);
            relation.setTagId(tag.getId());
            relation.setCreatedAt(now);
            videoTagRelationMapper.insert(relation);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVideo(Long videoId, Integer status, String reason) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) return;

        // 更新状态
        video.setStatus(status);
        video.setUpdatedAt(LocalDateTime.now());
        if (status == 1) {
            video.setPublishedAt(LocalDateTime.now());
        }
        videoMapper.updateById(video);

        // 记录审核日志
        AuditLog auditLog = new AuditLog();
        auditLog.setVideoId(videoId);
        auditLog.setAuditStatus(status);
        auditLog.setReason(reason);
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);

        // 如果审核通过，同步到 ES
        if (status == 1) {
            try {
                searchFeignClient.syncVideo(videoId);
            } catch (Exception e) {
                log.error("增量同步到 ES 失败, videoId: {}", videoId, e);
            }
        }
    }

    @Override
    public String getLatestRejectReason(Long videoId) {
        if (videoId == null) {
            return null;
        }
        // 查询最新的审核失败记录（auditStatus = 0）
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getVideoId, videoId)
                .eq(AuditLog::getAuditStatus, 0) // 0表示不通过
                .orderByDesc(AuditLog::getCreateTime)
                .last("LIMIT 1");
        AuditLog auditLog = auditLogMapper.selectOne(wrapper);
        return auditLog != null ? auditLog.getReason() : null;
    }
}
