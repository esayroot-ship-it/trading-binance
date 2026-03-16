package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站内消息实体，对应 user_message 表。
 */
@Data
public class UserMessage {
    /** 主键ID */
    private Long id;
    /** 接收用户编号(0代表全站广播) */
    private Long userId;
    /** 消息标题 */
    private String title;
    /** 消息正文 */
    private String content;
    /** 消息类型 */
    private String msgType;
    /** 阅读状态：0未读，1已读 */
    private Integer isRead;
    /** 消息下发时间 */
    private LocalDateTime createTime;
}
