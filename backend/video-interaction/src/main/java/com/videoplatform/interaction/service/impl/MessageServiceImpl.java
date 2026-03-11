package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.dto.MessageDTO;
import com.videoplatform.common.entity.Message;
import com.videoplatform.common.entity.Video;
import com.videoplatform.interaction.mapper.MessageMapper;
import com.videoplatform.interaction.mapper.VideoMapper;
import com.videoplatform.interaction.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageMapper messageMapper;
    private final VideoMapper videoMapper;

    @Override
    public List<MessageDTO> listMessages(Long userId, Integer page, Integer size, Integer isRead) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<Message> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .orderByDesc(Message::getCreatedAt);
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        messageMapper.selectPage(pager, wrapper);
        return toDTOs(pager.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long messageId) {
        if (userId == null || messageId == null) {
            return;
        }
        LambdaUpdateWrapper<Message> update = new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, messageId)
                .eq(Message::getToUserId, userId)
                .set(Message::getIsRead, 1);
        messageMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(MessageDTO dto) {
        if (dto == null || dto.getToUserId() == null || dto.getType() == null) {
            return null;
        }
        Message message = new Message();
        message.setFromUserId(dto.getFromUserId());
        message.setToUserId(dto.getToUserId());
        message.setType(dto.getType());
        message.setContent(dto.getContent());
        message.setRelatedId(dto.getRelatedId());
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        
        // 如果是视频分享（type=1），更新视频的分享数
        if (dto.getType() == 1 && dto.getRelatedId() != null) {
            try {
                LambdaUpdateWrapper<Video> videoUpdate = new LambdaUpdateWrapper<Video>()
                        .eq(Video::getId, dto.getRelatedId())
                        .setSql("share_count = share_count + 1");
                videoMapper.update(null, videoUpdate);
                log.debug("视频分享数+1: videoId={}", dto.getRelatedId());
            } catch (Exception e) {
                log.error("更新视频分享数失败: videoId={}", dto.getRelatedId(), e);
                // 不抛出异常，避免影响消息发送
            }
        }
        
        return message.getId();
    }

    private List<MessageDTO> toDTOs(List<Message> messages) {
        List<MessageDTO> list = new ArrayList<MessageDTO>();
        for (Message message : messages) {
            MessageDTO dto = new MessageDTO();
            dto.setId(message.getId());
            dto.setFromUserId(message.getFromUserId());
            dto.setToUserId(message.getToUserId());
            dto.setType(message.getType());
            dto.setContent(message.getContent());
            dto.setRelatedId(message.getRelatedId());
            dto.setIsRead(message.getIsRead());
            dto.setCreatedAt(message.getCreatedAt() == null ? null : FORMATTER.format(message.getCreatedAt()));
            list.add(dto);
        }
        return list;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2, Integer page, Integer size) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 50 : size;
        Page<Message> pager = new Page<>(current, pageSize);
        
        // 查询两个用户之间的消息（双向）
        // 使用 apply() 方法直接写 SQL，确保 OR 逻辑正确
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .apply("(from_user_id = {0} AND to_user_id = {1}) OR (from_user_id = {1} AND to_user_id = {0})", 
                       userId1, userId2)
                .orderByDesc(Message::getCreatedAt);
        
        List<Message> messages = messageMapper.selectPage(pager, wrapper).getRecords();
        log.debug("查询到 {} 条消息，userId1={}, userId2={}", messages.size(), userId1, userId2);
        
        // 将对方发送给自己的未读消息标记为已读
        // userId1 是当前用户，userId2 是对方用户
        // 需要标记：fromUserId = userId2, toUserId = userId1, isRead = 0
        if (!messages.isEmpty()) {
            List<Long> unreadMessageIds = new ArrayList<>();
            for (Message message : messages) {
                // 对方发送给自己的未读消息
                if (message.getFromUserId().equals(userId2) 
                    && message.getToUserId().equals(userId1) 
                    && message.getIsRead() != null 
                    && message.getIsRead() == 0) {
                    unreadMessageIds.add(message.getId());
                    // 更新内存中的状态，确保返回的 DTO 中 isRead = 1
                    message.setIsRead(1);
                }
            }
            
            // 批量更新数据库中的已读状态
            if (!unreadMessageIds.isEmpty()) {
                LambdaUpdateWrapper<Message> updateWrapper = new LambdaUpdateWrapper<Message>()
                        .in(Message::getId, unreadMessageIds)
                        .eq(Message::getFromUserId, userId2)
                        .eq(Message::getToUserId, userId1)
                        .eq(Message::getIsRead, 0)
                        .set(Message::getIsRead, 1);
                int updatedCount = messageMapper.update(null, updateWrapper);
                log.debug("标记了 {} 条消息为已读，userId1={}, userId2={}", updatedCount, userId1, userId2);
            }
        }
        
        return toDTOs(messages);
    }
}
