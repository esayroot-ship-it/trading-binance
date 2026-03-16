package org.example.messagesvc.service;

import java.util.List;
import org.example.common.DTO.TradingPlatformDTO;
import tools.R;

public interface UserMessageService {

    R<List<TradingPlatformDTO.UserMessageItem>> listByUserId(Long userId);

    R<String> markRead(Long userId, Long messageId);

    R<String> delete(Long userId, Long messageId);
}
