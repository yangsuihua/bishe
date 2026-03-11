package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.entity.Friendship;
import com.videoplatform.common.feign.UserFeignClient;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.mapper.FriendshipMapper;
import com.videoplatform.interaction.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional
    public boolean addFriendship(Long userId1, Long userId2) {
        // 确保userId1 < userId2，以符合数据库约束
        if (userId1 > userId2) {
            Long temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        // 检查是否已存在好友关系
        if (isFriends(userId1, userId2)) {
            return false; // 已存在好友关系
        }

        Friendship friendship = new Friendship();
        friendship.setUserId1(userId1);
        friendship.setUserId2(userId2);
        friendship.setCreateTime(LocalDateTime.now());
        
        int result = friendshipMapper.insert(friendship);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean removeFriendship(Long userId1, Long userId2) {
        // 确保userId1 < userId2，以符合数据库约束
        if (userId1 > userId2) {
            Long temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }

        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId1, userId1)
                .eq(Friendship::getUserId2, userId2);

        int result = friendshipMapper.delete(wrapper);
        return result > 0;
    }

    @Override
    public boolean isFriends(Long userId1, Long userId2) {
        // 确保userId1 < userId2，以符合数据库约束
        if (userId1 > userId2) {
            Long temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }
        
        // 检查是否存在好友关系（由于数据库约束，user_id_1 总是较小值）
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId1, userId1)
                .eq(Friendship::getUserId2, userId2);

        return friendshipMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Friendship> getFriends(Long userId) {
        // 获取与指定用户相关的所有好友关系
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<Friendship>()
                .and(w -> w
                    .eq(Friendship::getUserId1, userId)
                    .or()
                    .eq(Friendship::getUserId2, userId)
                );

        return friendshipMapper.selectList(wrapper);
    }
    
    @Override
    public List<UserProfileVO> getFriendList(Long userId) {
        // 获取与指定用户相关的所有好友关系
        List<Friendship> friendships = getFriends(userId);
        
        return friendships.stream()
            .map(friendship -> {
                // 确定好友的用户ID
                Long friendId = friendship.getUserId1().equals(userId) 
                    ? friendship.getUserId2() 
                    : friendship.getUserId1();
                
                // 通过 Feign 调用获取好友用户信息
                Result<UserProfileVO> result = userFeignClient.getUserProfile(friendId);
                if (result == null || result.getCode() != 200 || result.getData() == null) {
                    return null;
                }
                return result.getData();
            })
            .filter(vo -> vo != null)
            .collect(Collectors.toList());
    }
}