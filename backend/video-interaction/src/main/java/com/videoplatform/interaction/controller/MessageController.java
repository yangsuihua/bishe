package com.videoplatform.interaction.controller;

import com.videoplatform.common.dto.MessageDTO;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interaction/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<List<MessageDTO>> list(@RequestHeader("X-User-Id") Long userId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) Integer isRead) {
        return Result.success(messageService.listMessages(userId, page, size, isRead));
    }

    @PostMapping
    public Result<Long> send(@RequestBody MessageDTO dto) {
        Long id = messageService.sendMessage(dto);
        return Result.success(id);
    }

    @PostMapping("/{messageId}/read")
    public Result<Void> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long messageId) {
        messageService.markRead(userId, messageId);
        return Result.success();
    }
    
    @GetMapping("/chat/{targetUserId}")
    public Result<List<MessageDTO>> getChatMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return Result.success(messageService.getMessagesBetweenUsers(userId, targetUserId, page, size));
    }
}
