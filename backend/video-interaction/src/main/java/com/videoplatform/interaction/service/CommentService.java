package com.videoplatform.interaction.service;

import com.videoplatform.common.dto.CommentDTO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 获取评论列表
     */
    List<CommentDTO> getComments(Long videoId, Integer page, Integer size);
    
    /**
     * 发表评论
     */
    CommentDTO postComment(CommentDTO dto);
    
    /**
     * 删除评论
     */
    void deleteComment(Long userId, Long commentId);
}