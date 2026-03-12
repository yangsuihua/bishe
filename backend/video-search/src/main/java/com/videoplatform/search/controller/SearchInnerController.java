package com.videoplatform.search.controller;

import com.videoplatform.common.result.Result;
import com.videoplatform.search.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/search/inner")
@RequiredArgsConstructor
public class SearchInnerController {

    private final DataSyncService dataSyncService;

    /**
     * 增量同步视频（发布或更新）
     */
    @PostMapping("/sync/{videoId}")
    public Result<Void> syncVideo(@PathVariable Long videoId) {
        dataSyncService.syncOne(videoId);
        return Result.success();
    }

    /**
     * 物理删除视频同步
     */
    @DeleteMapping("/sync/{videoId}")
    public Result<Void> deleteVideo(@PathVariable Long videoId) {
        dataSyncService.deleteById(videoId);
        return Result.success();
    }

    /**
     * 全量同步视频
     */
    @PostMapping("/sync/all")
    public Result<String> syncAll() {
        dataSyncService.syncAll();
        return Result.success("全量同步完成");
    }
}
