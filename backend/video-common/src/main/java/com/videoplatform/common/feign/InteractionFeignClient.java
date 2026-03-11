package com.videoplatform.common.feign;

import com.videoplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 互动服务 Feign 客户端
 */
// contextId 避免未来新增同名客户端冲突
@FeignClient(name = "video-interaction", contextId = "videoInteractionFriendshipClient", path = "/interaction/friendship")
public interface InteractionFeignClient {
    
    /**
     * 添加好友关系
     */
    @PostMapping
    Result<Boolean> addFriendship(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam("targetUserId") Long targetUserId);
}
