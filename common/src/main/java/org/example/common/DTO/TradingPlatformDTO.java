package org.example.common.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 交易平台公共 DTO 定义集合。
 */
public final class TradingPlatformDTO {

    private TradingPlatformDTO() {
    }

    /** 用户注册请求。 */
    @Data
    public static class UserRegisterRequest {
        /** 登录用户名。 */
        private String username;
        /** 登录密码。 */
        private String password;
        /** 邮箱地址。 */
        private String email;
        /** 用户昵称。 */
        private String nickname;
    }

    /** 用户登录请求。 */
    @Data
    public static class UserLoginRequest {
        /** 登录用户名。 */
        private String username;
        /** 登录密码。 */
        private String password;
    }

    /** 登录响应。 */
    @Data
    public static class LoginResponse {
        /** JWT 令牌。 */
        private String token;
        /** 令牌过期时间。 */
        private LocalDateTime expireAt;
        /** 用户 ID。 */
        private Long userId;
        /** 用户名。 */
        private String username;
    }

    /** 管理员注册请求。 */
    @Data
    public static class AdminRegisterRequest {
        /** 管理员用户名。 */
        private String username;
        /** 管理员密码。 */
        private String password;
        /** 角色类型（1-超级管理员，2-普通管理员）。 */
        private Integer roleType;
    }

    /** 管理员登录请求。 */
    @Data
    public static class AdminLoginRequest {
        /** 管理员用户名。 */
        private String username;
        /** 管理员密码。 */
        private String password;
    }

    /** 管理员登录响应。 */
    @Data
    public static class AdminLoginResponse {
        /** 管理员 ID。 */
        private Integer adminId;
        /** 管理员用户名。 */
        private String username;
        /** 角色类型。 */
        private Integer roleType;
        /** 账号状态。 */
        private Integer status;
    }

    /** 用户修改密码请求。 */
    @Data
    public static class UserUpdatePasswordRequest {
        /** 旧密码。 */
        private String oldPassword;
        /** 新密码。 */
        private String newPassword;
    }

    /** 用户修改昵称请求。 */
    @Data
    public static class UserUpdateNicknameRequest {
        /** 新昵称。 */
        private String nickname;
    }

    /** 用户修改邮箱请求。 */
    @Data
    public static class UserUpdateEmailRequest {
        /** 新邮箱。 */
        private String email;
    }

    /** 用户修改头像请求。 */
    @Data
    public static class UserUpdateAvatarRequest {
        /** 新头像 URL。 */
        private String avatarUrl;
    }

    /** 用户信息响应。 */
    @Data
    public static class UserInfoResponse {
        /** 用户 ID。 */
        private Long userId;
        /** 用户名。 */
        private String username;
        /** 昵称。 */
        private String nickname;
        /** 头像 URL。 */
        private String avatar;
        /** 邮箱。 */
        private String email;
        /** 账号状态。 */
        private Integer status;
        /** 逻辑删除标记。 */
        private Integer isDeleted;
        /** 创建时间。 */
        private LocalDateTime createTime;
        /** 更新时间。 */
        private LocalDateTime updateTime;
    }

    /** 用户分页响应。 */
    @Data
    public static class UserPageResponse {
        /** 总记录数。 */
        private Long total;
        /** 当前页码。 */
        private Integer pageNo;
        /** 每页条数。 */
        private Integer pageSize;
        /** 当前页记录。 */
        private List<UserInfoResponse> records;
    }

    /** 管理员修改用户状态请求。 */
    @Data
    public static class AdminUpdateUserStatusRequest {
        /** 管理员 ID。 */
        private Integer adminId;
        /** 用户 ID。 */
        private Long userId;
        /** 目标状态。 */
        private Integer status;
    }

    /** 管理员删除用户请求。 */
    @Data
    public static class AdminDeleteUserRequest {
        /** 管理员 ID。 */
        private Integer adminId;
        /** 用户 ID。 */
        private Long userId;
    }

    /** 用户注销账号请求。 */
    @Data
    public static class UserCancelAccountRequest {
        /** 当前密码。 */
        private String password;
    }

    /** 账户总览信息。 */
    @Data
    public static class AccountOverview {
        /** 用户 ID。 */
        private Long userId;
        /** 币种。 */
        private String currency;
        /** 可用余额。 */
        private BigDecimal balance;
        /** 冻结余额。 */
        private BigDecimal frozenBalance;
    }

    /** 资金流水项。 */
    @Data
    public static class TransactionLogItem {
        /** 流水 ID。 */
        private Long id;
        /** 流水类型。 */
        private String transType;
        /** 变动金额。 */
        private BigDecimal amount;
        /** 变动后余额。 */
        private BigDecimal balanceAfter;
        /** 关联业务 ID。 */
        private String refId;
        /** 备注。 */
        private String remark;
        /** 创建时间。 */
        private LocalDateTime createTime;
    }

    /** 行情报价信息。 */
    @Data
    public static class AssetQuote {
        /** 交易标的代码。 */
        private String symbol;
        /** 标的名称。 */
        private String name;
        /** 资产类型。 */
        private String assetType;
        /** 所属市场。 */
        private String market;
        /** 最新价格。 */
        private BigDecimal latestPrice;
        /** 涨跌幅。 */
        private BigDecimal changeRate;
        /** 状态。 */
        private Integer status;
    }

    /** K 线查询请求。 */
    @Data
    public static class KlineQueryRequest {
        /** 交易标的代码。 */
        private String symbol;
        /** K 线周期。 */
        private String timeframe;
        /** 返回条数上限。 */
        private Integer limit;
    }

    /** AI K 线分析请求。 */
    @Data
    public static class AiKlineAnalyzeRequest {
        /** 交易标的代码。 */
        private String symbol;
        /** K 线周期。 */
        private String timeframe;
        /** K 线 JSON 数据。 */
        private String klineJson;
    }

    /** AI K 线分析响应。 */
    @Data
    public static class AiKlineAnalyzeResponse {
        /** 趋势判断。 */
        private String trend;
        /** 支撑位。 */
        private BigDecimal supportPrice;
        /** 压力位。 */
        private BigDecimal resistancePrice;
        /** 操作建议。 */
        private String suggestion;
    }

    /** 自选列表操作请求。 */
    @Data
    public static class WatchlistOperateRequest {
        /** 用户 ID。 */
        private Long userId;
        /** 标的代码。 */
        private String symbol;
        /** 排序权重。 */
        private Integer sortOrder;
    }

    /** 下单请求。 */
    @Data
    public static class PlaceOrderRequest {
        /** 用户 ID。 */
        private Long userId;
        /** 标的代码。 */
        private String symbol;
        /** 资产类型。 */
        private String assetType;
        /** 方向（LONG/SHORT）。 */
        private String direction;
        /** 动作（OPEN/CLOSE）。 */
        private String action;
        /** 订单类型（MARKET/LIMIT/STOP_LOSS/TAKE_PROFIT）。 */
        private String orderType;
        /** 委托价格。 */
        private BigDecimal entrustPrice;
        /** 触发价格。 */
        private BigDecimal triggerPrice;
        /** 委托数量。 */
        private BigDecimal entrustQuantity;
        /** 杠杆倍数。 */
        private Integer leverage;
    }

    /** 撤单请求。 */
    @Data
    public static class CancelOrderRequest {
        /** 用户 ID。 */
        private Long userId;
        /** 订单号。 */
        private String orderNo;
    }

    /** 订单详情。 */
    @Data
    public static class OrderDetail {
        /** 订单号。 */
        private String orderNo;
        /** 标的代码。 */
        private String symbol;
        /** 订单状态。 */
        private String status;
        /** 委托数量。 */
        private BigDecimal entrustQuantity;
        /** 已成交数量。 */
        private BigDecimal filledQuantity;
        /** 成交均价。 */
        private BigDecimal avgFilledPrice;
        /** 已实现盈亏。 */
        private BigDecimal realizedPnl;
        /** 更新时间。 */
        private LocalDateTime updateTime;
    }

    /** 持仓详情。 */
    @Data
    public static class PositionDetail {
        /** 标的代码。 */
        private String symbol;
        /** 持仓方向。 */
        private String direction;
        /** 杠杆倍数。 */
        private Integer leverage;
        /** 持仓数量。 */
        private BigDecimal holdQuantity;
        /** 可平仓数量。 */
        private BigDecimal availableQuantity;
        /** 开仓均价。 */
        private BigDecimal openPrice;
        /** 保证金。 */
        private BigDecimal margin;
        /** 预估强平价。 */
        private BigDecimal liquidationPrice;
    }

    /** 新闻项。 */
    @Data
    public static class NewsItem {
        /** 新闻 ID。 */
        private Long id;
        /** 标题。 */
        private String title;
        /** AI 摘要。 */
        private String aiSummary;
        /** AI 情绪标签。 */
        private String aiSentiment;
        /** 来源。 */
        private String source;
        /** 发布时间。 */
        private LocalDateTime publishTime;
        /** 关联标的代码列表。 */
        private List<String> symbols;
    }

    /** 发布帖子请求。 */
    @Data
    public static class CreatePostRequest {
        /** 用户 ID。 */
        private Long userId;
        /** 分类 ID。 */
        private Long categoryId;
        /** 标题。 */
        private String title;
        /** 正文内容。 */
        private String content;
        /** 图片 URL 列表。 */
        private List<String> images;
    }

    /** 发布评论请求。 */
    @Data
    public static class CreateCommentRequest {
        /** 帖子 ID。 */
        private Long postId;
        /** 评论用户 ID。 */
        private Long userId;
        /** 父评论 ID。 */
        private Long parentId;
        /** 评论内容。 */
        private String content;
    }

    /** 帖子互动请求。 */
    @Data
    public static class PostInteractionRequest {
        /** 帖子 ID。 */
        private Long postId;
        /** 用户 ID。 */
        private Long userId;
        /** 互动类型（1-点赞，2-收藏）。 */
        private Integer actionType;
    }

    /** 关注关系请求。 */
    @Data
    public static class FollowRequest {
        /** 关注者 ID。 */
        private Long followerId;
        /** 被关注者 ID。 */
        private Long followeeId;
    }

    /** 站内消息项。 */
    @Data
    public static class UserMessageItem {
        /** 消息 ID。 */
        private Long id;
        /** 消息标题。 */
        private String title;
        /** 消息内容。 */
        private String content;
        /** 消息类型。 */
        private String msgType;
        /** 已读状态（0-未读，1-已读）。 */
        private Integer isRead;
        /** 创建时间。 */
        private LocalDateTime createTime;
    }

    /** 全站广播消息请求。 */
    @Data
    public static class BroadcastMessageRequest {
        /** 消息标题。 */
        private String title;
        /** 消息内容。 */
        private String content;
        /** 消息类型。 */
        private String msgType;
    }

    /** 冻结用户请求。 */
    @Data
    public static class FreezeUserRequest {
        /** 管理员 ID。 */
        private Integer adminId;
        /** 用户 ID。 */
        private Long userId;
        /** 目标状态。 */
        private Integer status;
    }

    /** 用户关注项。 */
    @Data
    public static class UserFollowItem {
        /** 用户 ID。 */
        private Long userId;
        /** 用户名。 */
        private String username;
        /** 昵称。 */
        private String nickname;
        /** 头像 URL。 */
        private String avatar;
        /** 关注时间。 */
        private LocalDateTime followTime;
    }
}