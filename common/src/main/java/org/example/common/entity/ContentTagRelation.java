package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 内容标签关系实体，对应 content_tag_relation 表。
 */
@Data
public class ContentTagRelation {
    /** 主键ID */
    private Long id;
    /** 目标内容ID */
    private Long targetId;
    /** 目标类型：NEWS或POST */
    private String targetType;
    /** 金融标的代码 */
    private String symbol;
    /** 创建时间 */
    private LocalDateTime createTime;
}
