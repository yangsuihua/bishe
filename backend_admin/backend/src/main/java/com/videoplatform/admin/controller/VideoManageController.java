package com.videoplatform.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.VideoQueryDTO;
import com.videoplatform.admin.entity.Video;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.VideoManageService;
import com.videoplatform.admin.vo.VideoAuditVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class VideoManageController {
    
    private final VideoManageService videoManageService;
    
    @GetMapping
    public Result<Page<VideoAuditVO>> getVideoList(VideoQueryDTO dto) {
        Page<VideoAuditVO> result = videoManageService.getVideoList(dto);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<VideoAuditVO> getVideoDetail(@PathVariable Long id) {
        VideoAuditVO result = videoManageService.getVideoDetail(id);
        return Result.success(result);
    }
    
    @PutMapping("/{id}")
    public Result<String> updateVideo(@PathVariable Long id, @RequestBody Video video) {
        videoManageService.updateVideo(id, video);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/{id}")
    public Result<String> deleteVideo(@PathVariable Long id) {
        videoManageService.deleteVideo(id);
        return Result.success("删除成功");
    }
    
    @PostMapping("/{id}/status")
    public Result<String> updateVideoStatus(@PathVariable Long id, @RequestParam Integer status) {
        videoManageService.updateVideoStatus(id, status);
        return Result.success("状态更新成功");
    }
}
