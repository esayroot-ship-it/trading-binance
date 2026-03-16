package org.example.tradingsvc.service;

import java.math.BigDecimal;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.UserAccount;
import org.example.tradingsvc.dto.AccountAdjustRequest;
import tools.R;

public interface TradingAccountService {

    R<TradingPlatformDTO.AccountOverview> getAccountOverview(Long userId);

    R<TradingPlatformDTO.AccountOverview> adjustAccount(Long userId, AccountAdjustRequest request);

    UserAccount applyAccountDelta(Long userId,
                                  BigDecimal balanceDelta,
                                  BigDecimal frozenDelta,
                                  String transType,
                                  String refId,
                                  String remark);

    UserAccount ensureAccount(Long userId);
}
