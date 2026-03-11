package com.videoplatform.interaction.service;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.entity.Friendship;
import java.util.List;

public interface FriendshipService {
    /**
     * 添加好友关系
     */
    boolean addFriendship(Long userId1, Long userId2);

    /**
     * 删除好友关系
     */
    boolean removeFriendship(Long userId1, Long userId2);

    /**
     * 检查是否为好友
     */
    boolean isFriends(Long userId1, Long userId2);

    /**
     * 获取用户的好友列表（返回 Friendship 实体）
     */
    List<Friendship> getFriends(Long userId);
    
    /**
     * 获取用户的好友列表（返回用户信息）
     */
    List<UserProfileVO> getFriendList(Long userId);
}