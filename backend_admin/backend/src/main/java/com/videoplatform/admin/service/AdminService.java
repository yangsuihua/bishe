package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.admin.entity.Video;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.mapper.VideoMapper;
import com.videoplatform.admin.vo.AdminStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    
    /**
     * 获取系统统计信息
     */
    public AdminStatsVO getSystemStats() {
        AdminStatsVO stats = new AdminStatsVO();
        
        // 总用户数
        LambdaQueryWrapper<com.videoplatform.admin.entity.User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(com.videoplatform.admin.entity.User::getDeleted, 0);
        stats.setTotalUsers(userMapper.selectCount(userWrapper));
        
        // 总视频数
        LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.eq(Video::getDeleted, 0);
        stats.setTotalVideos(videoMapper.selectCount(videoWrapper));
        
        // 待审核视频数
        LambdaQueryWrapper<Video> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Video::getStatus, 0)
                .eq(Video::getDeleted, 0);
        stats.setPendingAuditVideos(videoMapper.selectCount(pendingWrapper));
        
        // 今日新增用户数
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<com.videoplatform.admin.entity.User> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.eq(com.videoplatform.admin.entity.User::getDeleted, 0)
                .ge(com.videoplatform.admin.entity.User::getCreatedAt, startOfDay);
        stats.setTodayNewUsers(userMapper.selectCount(todayUserWrapper));
        
        return stats;
    }
}
