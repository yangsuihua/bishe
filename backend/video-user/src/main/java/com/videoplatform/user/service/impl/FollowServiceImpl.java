package com.videoplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.videoplatform.common.exception.BusinessException;
import com.videoplatform.common.result.ResultCode;
import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.entity.User;
import com.videoplatform.common.entity.UserFollow;
import com.videoplatform.common.feign.InteractionFeignClient;
import com.videoplatform.user.mapper.UserFollowMapper;
import com.videoplatform.user.mapper.UserMapper;
import com.videoplatform.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    
    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final InteractionFeignClient interactionFeignClient;
    
    @Override
    @Transactional
    public void followUser(Long userId, Long targetUserId) {
        // 不能关注自己
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.CANNOT_FOLLOW_SELF);
        }
        
        // 检查是否已关注
        UserFollow existingFollow = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .eq(UserFollow::getFollowUserId, targetUserId)
        );
        
        if (existingFollow != null) {
            throw new BusinessException(ResultCode.ALREADY_FOLLOWED);
        }
        
        // 创建关注关系
        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setFollowUserId(targetUserId);
        userFollowMapper.insert(follow);
        
        // 更新用户关注数和粉丝�?
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .setSql("following_count = following_count + 1")
                .eq(User::getId, userId));
        
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .setSql("follower_count = follower_count + 1")
                .eq(User::getId, targetUserId));
        
        // 检查是否互粉，如果是则在事务提交后添加好友关系（避免事务超时）
        UserFollow reverseFollow = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, targetUserId)
                        .eq(UserFollow::getFollowUserId, userId)
        );
        
        if (reverseFollow != null) {
            // 在事务提交后执行 Feign 调用，避免事务超时
            final Long finalUserId = userId;
            final Long finalTargetUserId = targetUserId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // 事务提交后执行，此时数据库连接已释放
                    try {
                        interactionFeignClient.addFriendship(finalUserId, finalTargetUserId);
                    } catch (Exception e) {
                        // 如果添加好友失败，记录日志但不影响关注操作
                        System.err.println("添加好友关系失败: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    @Override
    @Transactional
    public void unfollowUser(Long userId, Long targetUserId) {
        // 检查是否已关注
        UserFollow existingFollow = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .eq(UserFollow::getFollowUserId, targetUserId)
        );
        
        if (existingFollow == null) {
            throw new BusinessException(ResultCode.NOT_FOLLOWED);
        }
        
        // 删除关注关系
        userFollowMapper.deleteById(existingFollow.getId());
        
        // 更新用户关注数和粉丝�?
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .setSql("following_count = following_count - 1")
                .eq(User::getId, userId)
                .gt(User::getFollowingCount, 0));
        
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .setSql("follower_count = follower_count - 1")
                .eq(User::getId, targetUserId)
                .gt(User::getFollowerCount, 0));
    }
    
    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        UserFollow follow = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .eq(UserFollow::getFollowUserId, targetUserId)
        );
        return follow != null;
    }
    
    @Override
    public List<UserProfileVO> getFollowingList(Long userId) {
        List<UserFollow> follows = userFollowMapper.selectList(
            new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
        );
        
        return follows.stream()
            .map(follow -> {
                User user = userMapper.selectById(follow.getFollowUserId());
                UserProfileVO vo = new UserProfileVO();
                BeanUtils.copyProperties(user, vo);
                vo.setIsFollowing(true);
                return vo;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<UserProfileVO> getFollowersList(Long userId) {
        List<UserFollow> followers = userFollowMapper.selectList(
            new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowUserId, userId)
        );
        
        return followers.stream()
            .map(follow -> {
                User user = userMapper.selectById(follow.getUserId());
                UserProfileVO vo = new UserProfileVO();
                BeanUtils.copyProperties(user, vo);
                // 检查当前用户是否关注了这个粉丝
                vo.setIsFollowing(isFollowing(userId, user.getId()));
                return vo;
            })
            .collect(Collectors.toList());
    }
}

