package org.example.messagesvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.messagesvc.service.UserMessageService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 用户消息接口。
 * 直接对外提供消息查询、已读、删除能力，用户编号由网关解析后透传。
 */
@RestController
@RequestMapping("/api/message/users/messages")
@RequiredArgsConstructor
public class UserMessageController {

    private final UserMessageService userMessageService;

    /**
     * 查询当前用户消息列表。
     */
    @GetMapping
    public R<List<TradingPlatformDTO.UserMessageItem>> listByUserId(@RequestHeader("X-User-Id") Long userId) {
        return userMessageService.listByUserId(userId);
    }

    /**
     * 标记消息已读。
     */
    @PutMapping("/{messageId}/read")
    public R<String> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long messageId) {
        return userMessageService.markRead(userId, messageId);
    }

    /**
     * 删除消息。
     */
    @DeleteMapping("/{messageId}")
    public R<String> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable Long messageId) {
        return userMessageService.delete(userId, messageId);
    }
}
