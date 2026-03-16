package org.example.messagesvc.service.impl;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.UserMessage;
import org.example.messagesvc.mapper.UserMessageMapper;
import org.example.messagesvc.service.UserMessageService;
import org.springframework.stereotype.Service;
import tools.R;

@Service
@RequiredArgsConstructor
public class UserMessageServiceImpl implements UserMessageService {

    private final UserMessageMapper userMessageMapper;

    @Override
    public R<List<TradingPlatformDTO.UserMessageItem>> listByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            return R.fail("userId is required");
        }
        List<UserMessage> messages = userMessageMapper.selectByUserId(userId);
        List<TradingPlatformDTO.UserMessageItem> result = new ArrayList<>();
        if (messages != null) {
            for (UserMessage message : messages) {
                TradingPlatformDTO.UserMessageItem item = new TradingPlatformDTO.UserMessageItem();
                item.setId(message.getId());
                item.setTitle(message.getTitle());
                item.setContent(message.getContent());
                item.setMsgType(message.getMsgType());
                item.setIsRead(message.getIsRead());
                item.setCreateTime(message.getCreateTime());
                result.add(item);
            }
        }
        return R.ok("query success", result);
    }

    @Override
    public R<String> markRead(Long userId, Long messageId) {
        if (userId == null || userId <= 0 || messageId == null || messageId <= 0) {
            return R.fail("userId and messageId are required");
        }
        int updated = userMessageMapper.updateReadByIdAndUserId(messageId, userId, 1);
        if (updated <= 0) {
            return R.fail("message not found");
        }
        return R.ok("mark read success", "ok");
    }

    @Override
    public R<String> delete(Long userId, Long messageId) {
        if (userId == null || userId <= 0 || messageId == null || messageId <= 0) {
            return R.fail("userId and messageId are required");
        }
        int deleted = userMessageMapper.deleteByIdAndUserId(messageId, userId);
        if (deleted <= 0) {
            return R.fail("message not found");
        }
        return R.ok("delete success", "ok");
    }
}
