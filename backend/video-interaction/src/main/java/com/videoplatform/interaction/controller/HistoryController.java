package com.videoplatform.interaction.controller;

import com.videoplatform.common.entity.UserHistory;
import com.videoplatform.common.result.Result;
import com.videoplatform.common.dto.HistoryDTO;
import com.videoplatform.interaction.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interaction/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @PostMapping
    public Result<Void> record(@RequestHeader("X-User-Id") Long userId, @RequestBody HistoryDTO dto) {
        historyService.recordHistory(userId, dto.getVideoId(), dto.getWatchDuration(), dto.getWatchProgress());
        return Result.success();
    }

    @GetMapping
    public Result<List<UserHistory>> list(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(historyService.listHistory(userId, page, size));
    }

    @DeleteMapping("/{historyId}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long historyId) {
        historyService.deleteHistory(userId, historyId);
        return Result.success();
    }
}
