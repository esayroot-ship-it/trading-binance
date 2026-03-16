package org.example.tradingsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.tradingsvc.dto.AccountAdjustRequest;
import org.example.tradingsvc.service.TradingAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

/**
 * 交易账户接口。
 */
@RestController
@RequestMapping("/api/trading/account")
@RequiredArgsConstructor
public class TradingAccountController {

    private final TradingAccountService tradingAccountService;

    /**
     * 账户概览。
     */
    @GetMapping
    public R<TradingPlatformDTO.AccountOverview> overview(@RequestHeader("X-User-Id") Long userId) {
        return tradingAccountService.getAccountOverview(userId);
    }

    /**
     * 调整账户。
     */
    @PostMapping("/adjust")
    public R<TradingPlatformDTO.AccountOverview> adjust(@RequestHeader("X-User-Id") Long userId,
                                                        @RequestBody AccountAdjustRequest request) {
        return tradingAccountService.adjustAccount(userId, request);
    }
}
