package com.videoplatform.user.service;

import com.videoplatform.common.dto.UserProfileVO;
import java.util.List;

/**
 * 关注服务接口
 */
public interface FollowService {
    
    /**
     * 关注用户
     */
    void followUser(Long userId, Long targetUserId);
    
    /**
     * 取消关注
     */
    void unfollowUser(Long userId, Long targetUserId);
    
    /**
     * 检查是否关注
     */
    boolean isFollowing(Long userId, Long targetUserId);
    
    /**
     * 获取关注列表
     */
    List<UserProfileVO> getFollowingList(Long userId);
    
    /**
     * 获取粉丝列表
     */
    List<UserProfileVO> getFollowersList(Long userId);
}
