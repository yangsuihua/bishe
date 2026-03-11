package com.videoplatform.interaction.controller;

import com.videoplatform.common.entity.UserFavorite;
import com.videoplatform.common.entity.Video;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interaction/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{videoId}")
    public Result<Void> add(@RequestHeader("X-User-Id") Long userId, @PathVariable Long videoId) {
        favoriteService.addFavorite(userId, videoId);
        return Result.success();
    }

    @DeleteMapping("/{videoId}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable Long videoId) {
        favoriteService.removeFavorite(userId, videoId);
        return Result.success();
    }

    @GetMapping("/check/{videoId}")
    public Result<Boolean> check(@RequestHeader("X-User-Id") Long userId, @PathVariable Long videoId) {
        return Result.success(favoriteService.isFavorited(userId, videoId));
    }

    @GetMapping("/user")
    public Result<List<UserFavorite>> list(@RequestHeader("X-User-Id") Long userId,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(favoriteService.listFavoriteRecords(userId, page, size));
    }
    
    @GetMapping("/user/{targetUserId}")
    public Result<List<UserFavorite>> listByUserId(@PathVariable Long targetUserId,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(favoriteService.listFavoriteRecords(targetUserId, page, size));
    }
}
