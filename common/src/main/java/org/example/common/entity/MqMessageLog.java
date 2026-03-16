package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MQ消息幂等日志实体，对应 mq_message_log 表。
 */
@Data
public class MqMessageLog {
    /** 全局消息ID */
    private String messageId;
    /** 处理服务名 */
    private String serviceName;
    /** 处理状态：0处理中，1成功，2失败 */
    private Integer status;
    /** 异常信息 */
    private String errorInfo;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
