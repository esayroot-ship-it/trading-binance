-- ==========================================================
-- 建立数据库
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `trading_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `trading_platform`;

-- ==========================================================
-- 模块一：账户与权限系统 (User & Admin)
-- ==========================================================

-- 1. 普通用户表
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '登录用户名(全局唯一)',
  `password` varchar(128) NOT NULL COMMENT '加密密码(强制要求BCrypt加密存入)',
  `nickname` varchar(64) DEFAULT NULL COMMENT '前端展示的昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(128) DEFAULT NULL COMMENT '绑定的邮箱',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用(冻结), 1-正常',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0-正常, 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前端普通用户基础信息表';

-- 2. 后台管理员表
CREATE TABLE `sys_admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '后台登录账号',
  `password` varchar(128) NOT NULL COMMENT '密码(BCrypt加密)',
  `role_type` tinyint(4) NOT NULL DEFAULT '2' COMMENT '角色：1-超级管理员, 2-普通运营/审核员',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用, 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理系统管理员账号表';

-- 3. 站内信与系统通知表
CREATE TABLE `user_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID (0代表全站全局广播)',
  `title` varchar(128) NOT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息正文(支持富文本/HTML)',
  `msg_type` varchar(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM(系统), OFFICIAL(官方公告), TRADE(交易状态提醒)',
  `is_read` tinyint(4) NOT NULL DEFAULT '0' COMMENT '阅读状态：0-未读, 1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息下发时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信与业务通知系统表';

-- ==========================================================
-- 模块二：行情标的与自选 (Market & Watchlist)
-- ==========================================================

-- 4. 交易标的基础信息表
CREATE TABLE `asset_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `symbol` varchar(64) NOT NULL COMMENT '全局唯一资产代码 (如 AAPL, BTCUSDT, CL-Oil)',
  `name` varchar(128) NOT NULL COMMENT '展示名称 (如 苹果公司, 比特币)',
  `asset_type` varchar(32) NOT NULL COMMENT '资产大类：A_STOCK, US_STOCK, CRYPTO, FUTURES',
  `market` varchar(32) DEFAULT NULL COMMENT '所属交易所/市场 (如 NASDAQ, BINANCE)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '交易状态：0-下架/停牌, 1-正常上架',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '前端列表排序权重(值越大越靠前)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol` (`symbol`),
  KEY `idx_type_status` (`asset_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易标的(金融资产)基础信息表';

-- 5. K线历史数据缓存表 (分离大字段JSON)
CREATE TABLE `asset_kline` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `symbol` varchar(64) NOT NULL COMMENT '资产代码',
  `timeframe` varchar(16) NOT NULL COMMENT 'K线周期 (1m, 15m, 1h, 1d等)',
  `kline_json` json NOT NULL COMMENT '第三方获取的标准化 OHLCV 数组数据',
  `last_kline_time` bigint(20) DEFAULT NULL COMMENT '最后一条K线的时间戳(用于增量更新)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_time` (`symbol`, `timeframe`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标的历史K线大字段JSON缓存表';

-- 6. 用户个人自选列表
CREATE TABLE `user_watchlist` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `symbol` varchar(64) NOT NULL COMMENT '资产代码',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '用户自定义拖拽排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_symbol` (`user_id`,`symbol`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人自选(关注)资产列表';

-- ==========================================================
-- 模块三：核心交易引擎 (Unified Trading Engine)
-- ==========================================================

-- 7. 用户虚拟资金总账户表
CREATE TABLE `user_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `currency` varchar(16) NOT NULL DEFAULT 'USD' COMMENT '计价基础货币',
  `balance` decimal(20,8) NOT NULL DEFAULT '100000.00000000' COMMENT '可用资金余额(赠送10万模拟金)',
  `frozen_balance` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '冻结资金(挂单占用)',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT 'CAS乐观锁版本号(防并发扣款)',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_curr` (`user_id`,`currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户虚拟资金统一总账户表';

-- 8. 资金变动审计流水表 (微服务交易核心)
CREATE TABLE `account_transaction_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `currency` varchar(16) NOT NULL DEFAULT 'USD' COMMENT '变动币种',
  `trans_type` varchar(32) NOT NULL COMMENT '流水类型：DEPOSIT, FREEZE, UNFREEZE, REALIZED_PNL, FEE',
  `amount` decimal(20,8) NOT NULL COMMENT '变动金额(正加负减)',
  `balance_after` decimal(20,8) NOT NULL COMMENT '变动后的可用余额(用于财务对账)',
  `ref_id` varchar(64) DEFAULT NULL COMMENT '关联的订单号或业务ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '流水备注说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_trans` (`user_id`, `trans_type`),
  KEY `idx_ref_id` (`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资金变动流水账本表';

-- 9. 全品类统一持仓表 (现货/合约通用)
CREATE TABLE `trade_position` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `symbol` varchar(64) NOT NULL COMMENT '资产代码',
  `asset_type` varchar(32) NOT NULL COMMENT '用于快速区分计算规则',
  `direction` varchar(16) NOT NULL COMMENT '持仓方向：LONG(做多/现货买入), SHORT(做空)',
  `leverage` int(11) NOT NULL DEFAULT '1' COMMENT '杠杆倍数(现货强制为1)',
  `hold_quantity` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '总持仓数量',
  `available_quantity` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '当前可平仓数量(防超卖)',
  `open_price` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '开仓/买入均价',
  `margin` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '占用保证金(本金)',
  `liquidation_price` decimal(20,8) DEFAULT NULL COMMENT '预估强平价(爆仓价，现货为NULL)',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-持仓中, 0-已清仓',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sym_dir` (`user_id`, `symbol`, `direction`) COMMENT '同标的同方向自动合并',
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全品类统一持仓数据表(支持现货与合约)';

-- 10. 全品类统一委托订单表
CREATE TABLE `trade_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL COMMENT '全局唯一订单号',
  `user_id` bigint(20) NOT NULL,
  `symbol` varchar(64) NOT NULL,
  `asset_type` varchar(32) NOT NULL,
  `direction` varchar(16) NOT NULL COMMENT '方向：LONG, SHORT',
  `action` varchar(16) NOT NULL COMMENT '动作：OPEN(开仓/买入), CLOSE(平仓/卖出)',
  `order_type` varchar(32) NOT NULL COMMENT '类型：MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT',
  `trigger_price` decimal(20,8) DEFAULT NULL COMMENT '条件触发价(止盈止损单)',
  `entrust_price` decimal(20,8) DEFAULT NULL COMMENT '限价委托价(市价单为空)',
  `entrust_quantity` decimal(20,8) NOT NULL COMMENT '委托数量',
  `filled_quantity` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '已成交数量',
  `avg_filled_price` decimal(20,8) DEFAULT '0.00000000' COMMENT '成交均价',
  `realized_pnl` decimal(20,8) DEFAULT '0.00000000' COMMENT '平仓动作产生的已实现落袋盈亏',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, PARTIAL_FILLED, FILLED, CANCELED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_sym` (`user_id`, `symbol`),
  KEY `idx_status` (`status`) COMMENT '供后台撮合引擎高频扫表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全品类统一委托订单表';

-- ==========================================================
-- 模块四：内容分类与标签引擎 (Category & Tags)
-- ==========================================================

-- 11. 系统专栏与分类表
CREATE TABLE `sys_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category_name` varchar(64) NOT NULL COMMENT '分类名(如: 宏观经济, 个股分析)',
  `module_type` varchar(16) NOT NULL COMMENT '模块：NEWS(新闻大类), POST(社区帖子大类)',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序权重',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1-正常, 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module_status` (`module_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统内容板块分类表';

-- 12. 多态内容标签映射表 (资产即标签核心机制)
CREATE TABLE `content_tag_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_id` bigint(20) NOT NULL COMMENT '关联内容ID(新闻ID或帖子ID)',
  `target_type` varchar(16) NOT NULL COMMENT '路由类型：NEWS, POST',
  `symbol` varchar(64) NOT NULL COMMENT '金融资产代码(标签主体，如 AAPL)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_symbol` (`target_type`, `target_id`, `symbol`),
  KEY `idx_symbol_type` (`symbol`, `target_type`) COMMENT '通过代码极速反查相关内容的入口'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻/帖子内容与金融标的的多态关联标签表';

-- ==========================================================
-- 模块五：资讯与社区系统 (News & Community)
-- ==========================================================

-- 13. 金融新闻表 (已包含 AI 赋能字段)
CREATE TABLE `news_article` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) NOT NULL COMMENT '关联 sys_category.id',
  `title` varchar(255) NOT NULL COMMENT '大标题',
  `summary` varchar(1024) DEFAULT NULL COMMENT '原新闻摘要',
  `ai_summary` varchar(512) DEFAULT NULL COMMENT '【AI生成】精简一句话摘要',
  `ai_sentiment` varchar(16) DEFAULT 'NEUTRAL' COMMENT '【AI分析】情绪态度：BULLISH(利好), BEARISH(利空), NEUTRAL(中性)',
  `ai_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '【AI处理状态】：0-未处理, 1-处理成功, 2-处理失败',
  `content` text COMMENT '深度正文',
  `source` varchar(128) DEFAULT NULL COMMENT '出处(如 路透社)',
  `image_url` varchar(255) DEFAULT NULL COMMENT '封面头图URL',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0-隐藏下架, 1-正常展示',
  `publish_time` datetime NOT NULL COMMENT '原发文时间(排序依据)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '系统入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_status` (`status`),
  KEY `idx_ai_status` (`ai_status`) COMMENT '供后台定时任务拉取未AI处理的新闻'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方金融资讯与AI分析表';

-- 14. 社区帖子表
CREATE TABLE `post` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '发帖人',
  `category_id` bigint(20) NOT NULL COMMENT '板块分类',
  `title` varchar(255) DEFAULT NULL COMMENT '选填标题',
  `content` text NOT NULL COMMENT '正文(富文本)',
  `images` json DEFAULT NULL COMMENT '多图URL数组',
  `view_count` int(11) NOT NULL DEFAULT '0',
  `like_count` int(11) NOT NULL DEFAULT '0',
  `comment_count` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '风控状态：0-违规隐藏, 1-正常',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '软删除标记',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户交流互动帖子表';

-- 15. 帖子点赞与收藏聚合记录表
CREATE TABLE `post_interaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `action_type` tinyint(4) NOT NULL COMMENT '类型：1-点赞, 2-收藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user_act` (`post_id`,`user_id`,`action_type`),
  KEY `idx_user_act` (`user_id`,`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交动作流水记录表(防刷机制)';

-- 16. 帖子树状评论表
CREATE TABLE `post_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL COMMENT '评论人ID',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '盖楼回复体系(0代表直评帖子，非0代表回复的评论ID)',
  `content` varchar(1024) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '审核状态',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子多级树状评论表';

-- 17. 用户关注关系映射表 (大V/粉丝生态)
CREATE TABLE `user_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `follower_id` bigint(20) NOT NULL COMMENT '关注者(粉丝)',
  `followee_id` bigint(20) NOT NULL COMMENT '被关注者(博主/大V)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`,`followee_id`),
  KEY `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系网络拓扑表';

-- ==========================================================
-- 模块六：系统基础设施补充 (Infrastructure)
-- ==========================================================

-- 18. MQ 分布式消息幂等去重表
CREATE TABLE `mq_message_log` (
  `message_id` varchar(64) NOT NULL COMMENT '全局唯一的MQ消息ID',
  `service_name` varchar(64) NOT NULL COMMENT '处理该消息的微服务名',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '处理状态：0-处理中, 1-成功, 2-失败',
  `error_info` text DEFAULT NULL COMMENT '异常堆栈信息(用于补偿重试)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分布式消息防重复消费(幂等)记录表';