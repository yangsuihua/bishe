package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.dto.CommentDTO;
import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.entity.Comment;
import com.videoplatform.common.entity.Video;
import com.videoplatform.common.feign.UserFeignClient;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.mapper.CommentMapper;
import com.videoplatform.interaction.mapper.VideoMapper;
import com.videoplatform.interaction.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CommentMapper commentMapper;
    private final VideoMapper videoMapper;
    private final UserFeignClient userFeignClient;
    private final com.videoplatform.interaction.component.UserBehaviorProducer userBehaviorProducer;

    
    @Override
    public List<CommentDTO> getComments(Long videoId, Integer page, Integer size) {
        if (videoId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        Page<Comment> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getVideoId, videoId)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreatedAt);
        commentMapper.selectPage(pager, wrapper);

        List<Comment> comments = pager.getRecords();
        return toDTOs(comments);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO postComment(CommentDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getVideoId() == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setVideoId(dto.getVideoId());
        comment.setUserId(dto.getUserId());
        comment.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        comment.setReplyUserId(dto.getReplyUserId());
        comment.setContent(dto.getContent());

        comment.setStatus(1);
        comment.setCreatedAt(now);
        commentMapper.insert(comment);

        LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, dto.getVideoId())
                .setSql("comment_count = comment_count + 1");
        videoMapper.update(null, update);

        dto.setId(comment.getId());
        dto.setCreatedAt(DATE_TIME_FORMATTER.format(now));
        Map<Long, UserProfileVO> users = loadUsers(comment);
        UserProfileVO author = users.get(comment.getUserId());
        if (author != null) {
            dto.setUsername(author.getUsername());
            dto.setAvatar(author.getAvatar());
        }
        UserProfileVO replyUser = users.get(comment.getReplyUserId());
        if (replyUser != null) {
            dto.setReplyUsername(replyUser.getUsername());
        }
        dto.setIsLiked(Boolean.FALSE);

        // 发送行为消息到Kafka
        userBehaviorProducer.sendMessage(dto.getUserId(), dto.getVideoId(), com.videoplatform.common.dto.UserBehaviorMsg.BehaviorType.COMMENT);

        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            return;
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        if (!userId.equals(comment.getUserId())) {
            throw new IllegalArgumentException("无权限删除该评论");
        }
        
        // 首先获取需要删除的评论列表（只包含未删除的）
        List<Long> commentIdsToDelete = new ArrayList<>();
        collectCommentIdsToDelete(commentId, commentIdsToDelete);
        
        // 过滤出实际需要删除的评论（状态为1的）
        List<Comment> commentsToActuallyDelete = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .in(Comment::getId, commentIdsToDelete)
                .eq(Comment::getStatus, 1)
        );
        
        // 批量更新评论状态
        if (!commentsToActuallyDelete.isEmpty()) {
            List<Long> actualIdsToDelete = commentsToActuallyDelete.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
            
            LambdaUpdateWrapper<Comment> updateComment = new LambdaUpdateWrapper<Comment>()
                    .in(Comment::getId, actualIdsToDelete)
                    .set(Comment::getStatus, 0);
            commentMapper.update(null, updateComment);
            
            // 批量更新视频的评论计数 - 只减去实际删除的评论数量
            LambdaUpdateWrapper<Video> updateVideo = new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, comment.getVideoId())
                    .setSql("comment_count = CASE WHEN comment_count >= " + commentsToActuallyDelete.size() + " THEN comment_count - " + commentsToActuallyDelete.size() + " ELSE 0 END");
            videoMapper.update(null, updateVideo);
        }
    }
    
    /**
     * 递归收集需要删除的评论ID
     */
    private void collectCommentIdsToDelete(Long commentId, List<Long> commentIdsToDelete) {
        // 添加当前评论ID
        commentIdsToDelete.add(commentId);
        
        // 查找所有未删除的子评论（回复）
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, commentId)
                .eq(Comment::getStatus, 1); // 只查找未删除的评论
        List<Comment> childComments = commentMapper.selectList(queryWrapper);
        
        // 递归处理每个子评论
        for (Comment childComment : childComments) {
            collectCommentIdsToDelete(childComment.getId(), commentIdsToDelete);
        }
    }

    private List<CommentDTO> toDTOs(List<Comment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<Long>();
        for (Comment comment : comments) {
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
            if (comment.getReplyUserId() != null) {
                userIds.add(comment.getReplyUserId());
            }
        }
        Map<Long, UserProfileVO> userMap = loadUsers(userIds);

        List<CommentDTO> list = new ArrayList<CommentDTO>(comments.size());
        for (Comment comment : comments) {
            CommentDTO dto = new CommentDTO();
            dto.setId(comment.getId());
            dto.setVideoId(comment.getVideoId());
            dto.setUserId(comment.getUserId());
            dto.setParentId(comment.getParentId());
            dto.setReplyUserId(comment.getReplyUserId());
            dto.setContent(comment.getContent());
            dto.setCreatedAt(comment.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(comment.getCreatedAt()));
            UserProfileVO author = userMap.get(comment.getUserId());
            if (author != null) {
                dto.setUsername(author.getUsername());
                dto.setAvatar(author.getAvatar());
            }
            UserProfileVO replyUser = userMap.get(comment.getReplyUserId());
            if (replyUser != null) {
                dto.setReplyUsername(replyUser.getUsername());
            }
            dto.setIsLiked(Boolean.FALSE);
            list.add(dto);
        }
        return list;
    }

    private Map<Long, UserProfileVO> loadUsers(Comment comment) {
        Set<Long> ids = new HashSet<Long>();
        if (comment.getUserId() != null) {
            ids.add(comment.getUserId());
        }
        if (comment.getReplyUserId() != null) {
            ids.add(comment.getReplyUserId());
        }
        return loadUsers(ids);
    }

    private Map<Long, UserProfileVO> loadUsers(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            Result<List<UserProfileVO>> result = userFeignClient.getUsersByIds(new ArrayList<>(userIds));
            Map<Long, UserProfileVO> map = new HashMap<>();
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                for (UserProfileVO vo : result.getData()) {
                    map.put(vo.getId(), vo);
                }
            }
            return map;
        } catch (Exception e) {
            log.error("批量获取用户信息失败", e);
            return Collections.emptyMap();
        }
    }
}
