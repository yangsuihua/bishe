package com.videoplatform.interaction.service;

import com.videoplatform.common.dto.MessageDTO;

import java.util.List;

public interface MessageService {
    List<MessageDTO> listMessages(Long userId, Integer page, Integer size, Integer isRead);

    void markRead(Long userId, Long messageId);

    Long sendMessage(MessageDTO dto);
    
    /**
     * 获取两个用户之间的聊天记录
     */
    List<MessageDTO> getMessagesBetweenUsers(Long userId1, Long userId2, Integer page, Integer size);
}
