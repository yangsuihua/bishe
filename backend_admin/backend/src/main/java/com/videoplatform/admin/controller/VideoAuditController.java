package com.videoplatform.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.VideoAuditDTO;
import com.videoplatform.admin.entity.AuditLog;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.VideoAuditService;
import com.videoplatform.admin.vo.VideoAuditVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class VideoAuditController {
    
    private final VideoAuditService videoAuditService;
    
    @GetMapping("/pending")
    public Result<Page<VideoAuditVO>> getPendingVideos(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<VideoAuditVO> result = videoAuditService.getPendingVideos(page, size);
        return Result.success(result);
    }
    
    @PostMapping("/{videoId}/approve")
    public Result<String> approveVideo(
            @PathVariable Long videoId,
            @RequestBody(required = false) VideoAuditDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        String reason = dto != null ? dto.getReason() : null;
        videoAuditService.approveVideo(videoId, adminId, reason);
        return Result.success("审核通过");
    }
    
    @PostMapping("/{videoId}/reject")
    public Result<String> rejectVideo(
            @PathVariable Long videoId,
            @RequestBody VideoAuditDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            return Result.fail("拒绝原因不能为空");
        }
        videoAuditService.rejectVideo(videoId, adminId, dto.getReason());
        return Result.success("审核拒绝");
    }
    
    @GetMapping("/logs")
    public Result<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long videoId) {
        Page<AuditLog> result = videoAuditService.getAuditLogs(page, size, videoId);
        return Result.success(result);
    }
    
    /**
     * 根据审核记录将视频重新标记为通过
     */
    @PostMapping("/logs/{logId}/approve")
    public Result<String> approveFromLog(
            @PathVariable Long logId,
            @RequestBody VideoAuditDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            return Result.fail("通过原因不能为空");
        }
        videoAuditService.approveFromLog(logId, adminId, dto.getReason());
        return Result.success("已根据审核记录将视频标记为通过");
    }
    
    /**
     * 根据审核记录将视频重新标记为拒绝
     */
    @PostMapping("/logs/{logId}/reject")
    public Result<String> rejectFromLog(
            @PathVariable Long logId,
            @RequestBody VideoAuditDTO dto,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            return Result.fail("拒绝原因不能为空");
        }
        videoAuditService.rejectFromLog(logId, adminId, dto.getReason());
        return Result.success("已根据审核记录将视频标记为拒绝");
    }
    
    /**
     * 获取视频的最新审核记录
     */
    @GetMapping("/logs/latest/{videoId}")
    public Result<AuditLog> getLatestAuditLog(@PathVariable Long videoId) {
        AuditLog result = videoAuditService.getLatestAuditLog(videoId);
        return Result.success(result);
    }
    
    /**
     * 获取视频详情（用于审核，包含审核失败原因）
     */
    @GetMapping("/video/{videoId}")
    public Result<VideoAuditVO> getVideoDetailForAudit(@PathVariable Long videoId) {
        VideoAuditVO result = videoAuditService.getVideoDetailForAudit(videoId);
        return Result.success(result);
    }
}
