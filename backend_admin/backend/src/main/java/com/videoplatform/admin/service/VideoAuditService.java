package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.entity.AuditLog;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.entity.Video;
import com.videoplatform.admin.mapper.AuditLogMapper;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.mapper.VideoMapper;
import com.videoplatform.admin.vo.VideoAuditVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAuditService {
    
    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;
    
    /**
     * 获取待审核视频列表
     */
    public Page<VideoAuditVO> getPendingVideos(Integer page, Integer size) {
        Page<Video> videoPage = new Page<>(page, size);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, 0) // 待审核
                .eq(Video::getDeleted, 0)
                .orderByDesc(Video::getCreatedAt);
        
        Page<Video> result = videoMapper.selectPage(videoPage, wrapper);
        
        Page<VideoAuditVO> voPage = new Page<>(page, size, result.getTotal());
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
            
            // 查询是否有最新审核记录（待审核视频通常没有审核记录）
            AuditLog latestLog = getLatestAuditLog(video.getId());
            if (latestLog != null) {
                vo.setHasLatestAuditLog(true);
                vo.setLatestAuditStatus(latestLog.getAuditStatus());
            } else {
                vo.setHasLatestAuditLog(false);
                vo.setLatestAuditStatus(null);
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    /**
     * 审核通过
     */
    @Transactional
    public void approveVideo(Long videoId, Long adminId, String reason) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        video.setStatus(1); // 已审核发布
        video.setPublishedAt(LocalDateTime.now());
        videoMapper.updateById(video);
        
        // 记录审核日志
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setVideoId(videoId);
        auditLog.setAuditStatus(1); // 通过
        auditLog.setReason(reason); // 通过原因（可选）
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
        
        log.info("视频审核通过: videoId={}, adminId={}, reason={}", videoId, adminId, reason);
    }
    
    /**
     * 审核拒绝
     */
    @Transactional
    public void rejectVideo(Long videoId, Long adminId, String reason) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getDeleted() == 1) {
            throw new RuntimeException("视频不存在");
        }
        
        video.setStatus(2); // 审核失败
        videoMapper.updateById(video);
        
        // 记录审核日志
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setVideoId(videoId);
        auditLog.setAuditStatus(0); // 不通过
        auditLog.setReason(reason);
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
        
        log.info("视频审核拒绝: videoId={}, adminId={}, reason={}", videoId, adminId, reason);
    }
    
    /**
     * 获取审核日志
     */
    public Page<AuditLog> getAuditLogs(Integer page, Integer size, Long videoId) {
        Page<AuditLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (videoId != null) {
            wrapper.eq(AuditLog::getVideoId, videoId);
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);
        
        return auditLogMapper.selectPage(pageObj, wrapper);
    }
    
    /**
     * 根据视频ID获取视频详情（包含审核失败原因）
     */
    public VideoAuditVO getVideoDetailForAudit(Long videoId) {
        Video video = videoMapper.selectById(videoId);
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
        
        // 查询最新的审核记录
        AuditLog latestLog = getLatestAuditLog(videoId);
        if (latestLog != null) {
            vo.setHasLatestAuditLog(true);
            vo.setLatestAuditStatus(latestLog.getAuditStatus());
            
            // 如果最新记录是拒绝，设置拒绝原因
            if (latestLog.getAuditStatus() == 0) {
                vo.setLastRejectReason(latestLog.getReason());
            }
        } else {
            vo.setHasLatestAuditLog(false);
            vo.setLatestAuditStatus(null);
        }
        
        return vo;
    }
    
    /**
     * 根据审核日志将视频重新标记为审核通过
     */
    @Transactional
    public void approveFromLog(Long logId, Long adminId, String reason) {
        AuditLog auditLog = auditLogMapper.selectById(logId);
        if (auditLog == null) {
            throw new RuntimeException("审核记录不存在");
        }
        Long videoId = auditLog.getVideoId();
        approveVideo(videoId, adminId, reason);
        log.info("根据审核记录重新审核通过: logId={}, videoId={}, adminId={}, reason={}", logId, videoId, adminId, reason);
    }
    
    /**
     * 根据审核日志将视频重新标记为审核拒绝
     */
    @Transactional
    public void rejectFromLog(Long logId, Long adminId, String reason) {
        AuditLog auditLog = auditLogMapper.selectById(logId);
        if (auditLog == null) {
            throw new RuntimeException("审核记录不存在");
        }
        Long videoId = auditLog.getVideoId();
        rejectVideo(videoId, adminId, reason);
        log.info("根据审核记录重新审核拒绝: logId={}, videoId={}, adminId={}, reason={}", logId, videoId, adminId, reason);
    }
    
    /**
     * 获取视频的最新审核记录（用于判断按钮显示）
     */
    public AuditLog getLatestAuditLog(Long videoId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getVideoId, videoId)
                .orderByDesc(AuditLog::getCreateTime)
                .last("LIMIT 1");
        return auditLogMapper.selectOne(wrapper);
    }
}
