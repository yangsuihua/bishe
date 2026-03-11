package com.videoplatform.interaction.controller;

import com.videoplatform.common.result.Result;
import com.videoplatform.common.dto.CommentDTO;
import com.videoplatform.interaction.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/interaction/comment")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 获取评论列表
     */
    @GetMapping("/{videoId}")
    public Result<List<CommentDTO>> getComments(@RequestHeader("X-User-Id") Long userId,
                                                 @PathVariable Long videoId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer size) {
        List<CommentDTO> comments = commentService.getComments(videoId, page, size);
        return Result.success(comments);
    }
    
    /**
     * 发表评论
     */
    @PostMapping
    public Result<CommentDTO> postComment(@RequestHeader("X-User-Id") Long userId,
                                           @RequestBody CommentDTO dto) {
        dto.setUserId(userId);
        CommentDTO comment = commentService.postComment(dto);
        return Result.success(comment);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
        return Result.success();
    }
}
