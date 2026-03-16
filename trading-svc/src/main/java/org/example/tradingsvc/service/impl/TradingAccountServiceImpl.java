package org.example.tradingsvc.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.AccountTransactionLog;
import org.example.common.entity.UserAccount;
import org.example.tradingsvc.constant.TradingConstants;
import org.example.tradingsvc.dto.AccountAdjustRequest;
import org.example.tradingsvc.mapper.AccountTransactionLogMapper;
import org.example.tradingsvc.mapper.UserAccountMapper;
import org.example.tradingsvc.service.TradingAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingAccountServiceImpl implements TradingAccountService {

    private static final int MAX_RETRY = 5;
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("100000.00000000");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(8, RoundingMode.DOWN);

    private final UserAccountMapper userAccountMapper;
    private final AccountTransactionLogMapper accountTransactionLogMapper;

    @Override
    public R<TradingPlatformDTO.AccountOverview> getAccountOverview(Long userId) {
        try {
            UserAccount account = ensureAccount(userId);
            return R.ok("query success", toOverview(account));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("查询账户概览失败，用户编号={}", userId, ex);
            return R.fail("query account overview failed");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public R<TradingPlatformDTO.AccountOverview> adjustAccount(Long userId, AccountAdjustRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return R.fail("amount must be greater than 0");
        }
        if (!StringUtils.hasText(request.getTargetType()) || !StringUtils.hasText(request.getOperation())) {
            return R.fail("targetType and operation are required");
        }
        String targetType = request.getTargetType().trim().toUpperCase();
        String operation = request.getOperation().trim().toUpperCase();
        BigDecimal amount = request.getAmount().setScale(8, RoundingMode.HALF_UP);

        BigDecimal balanceDelta = ZERO;
        BigDecimal frozenDelta = ZERO;
        if (TradingConstants.ACCOUNT_TARGET_BALANCE.equals(targetType)) {
            balanceDelta = resolveDelta(operation, amount);
        } else if (TradingConstants.ACCOUNT_TARGET_FROZEN.equals(targetType)) {
            frozenDelta = resolveDelta(operation, amount);
        } else {
            return R.fail("targetType must be BALANCE or FROZEN");
        }

        try {
            String transType = StringUtils.hasText(request.getTransType()) ? request.getTransType().trim() : "MANUAL_ADJUST";
            UserAccount account = applyAccountDelta(
                    userId,
                    balanceDelta,
                    frozenDelta,
                    transType,
                    request.getRefId(),
                    request.getRemark());
            return R.ok("adjust success", toOverview(account));
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            log.error("调整账户失败，用户编号={}", userId, ex);
            return R.fail("adjust account failed");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserAccount applyAccountDelta(Long userId,
                                         BigDecimal balanceDelta,
                                         BigDecimal frozenDelta,
                                         String transType,
                                         String refId,
                                         String remark) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        BigDecimal safeBalanceDelta = safeAmount(balanceDelta);
        BigDecimal safeFrozenDelta = safeAmount(frozenDelta);
        if (safeBalanceDelta.compareTo(BigDecimal.ZERO) == 0 && safeFrozenDelta.compareTo(BigDecimal.ZERO) == 0) {
            return ensureAccount(userId);
        }

        for (int i = 0; i < MAX_RETRY; i++) {
            UserAccount current = ensureAccount(userId);
            BigDecimal currentBalance = safeAmount(current.getBalance());
            BigDecimal currentFrozen = safeAmount(current.getFrozenBalance());
            BigDecimal newBalance = currentBalance.add(safeBalanceDelta).setScale(8, RoundingMode.HALF_UP);
            BigDecimal newFrozen = currentFrozen.add(safeFrozenDelta).setScale(8, RoundingMode.HALF_UP);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("insufficient balance");
            }
            if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("insufficient frozen balance");
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = userAccountMapper.updateByIdAndVersion(
                    current.getId(),
                    current.getVersion(),
                    current.getVersion() + 1,
                    newBalance,
                    newFrozen,
                    now);
            if (updated <= 0) {
                continue;
            }

            String safeTransType = StringUtils.hasText(transType) ? transType.trim() : "ACCOUNT_CHANGE";
            writeTransactionLog(userId, safeBalanceDelta, newBalance, safeTransType, refId, remark);
            writeTransactionLog(userId, safeFrozenDelta, newBalance, safeTransType + "_FROZEN", refId, remark);

            current.setBalance(newBalance);
            current.setFrozenBalance(newFrozen);
            current.setVersion(current.getVersion() + 1);
            current.setUpdateTime(now);
            return current;
        }
        throw new IllegalStateException("account update retry exceeded");
    }

    @Override
    public UserAccount ensureAccount(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        UserAccount account = userAccountMapper.selectByUserIdAndCurrency(userId, TradingConstants.DEFAULT_CURRENCY);
        if (account != null) {
            return account;
        }

        UserAccount init = new UserAccount();
        init.setUserId(userId);
        init.setCurrency(TradingConstants.DEFAULT_CURRENCY);
        init.setBalance(DEFAULT_BALANCE);
        init.setFrozenBalance(ZERO);
        init.setVersion(0);
        init.setUpdateTime(LocalDateTime.now());
        try {
            userAccountMapper.insert(init);
        } catch (Exception ex) {
            log.debug("插入默认账户被忽略，用户编号={}", userId, ex);
        }
        UserAccount created = userAccountMapper.selectByUserIdAndCurrency(userId, TradingConstants.DEFAULT_CURRENCY);
        if (created == null) {
            throw new IllegalStateException("create account failed");
        }
        return created;
    }

    private BigDecimal resolveDelta(String operation, BigDecimal amount) {
        if (TradingConstants.ACCOUNT_OP_INCREASE.equals(operation)) {
            return amount;
        }
        if (TradingConstants.ACCOUNT_OP_DECREASE.equals(operation)) {
            return amount.negate();
        }
        throw new IllegalArgumentException("operation must be INCREASE or DECREASE");
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO;
        }
        return amount.setScale(8, RoundingMode.HALF_UP);
    }

    private TradingPlatformDTO.AccountOverview toOverview(UserAccount account) {
        TradingPlatformDTO.AccountOverview overview = new TradingPlatformDTO.AccountOverview();
        overview.setUserId(account.getUserId());
        overview.setCurrency(account.getCurrency());
        overview.setBalance(safeAmount(account.getBalance()));
        overview.setFrozenBalance(safeAmount(account.getFrozenBalance()));
        return overview;
    }

    private void writeTransactionLog(Long userId,
                                     BigDecimal amount,
                                     BigDecimal balanceAfter,
                                     String transType,
                                     String refId,
                                     String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        AccountTransactionLog logEntity = new AccountTransactionLog();
        logEntity.setUserId(userId);
        logEntity.setCurrency(TradingConstants.DEFAULT_CURRENCY);
        logEntity.setTransType(transType);
        logEntity.setAmount(amount);
        logEntity.setBalanceAfter(balanceAfter);
        logEntity.setRefId(refId);
        logEntity.setRemark(remark);
        logEntity.setCreateTime(LocalDateTime.now());
        accountTransactionLogMapper.insert(logEntity);
    }
}
