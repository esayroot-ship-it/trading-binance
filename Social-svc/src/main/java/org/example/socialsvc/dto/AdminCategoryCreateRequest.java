package org.example.socialsvc.dto;

import lombok.Data;

/**
 * 管理员新增分类请求。
 */
@Data
public class AdminCategoryCreateRequest {

    /** 分类名称。 */
    private String categoryName;

    /** 模块类型：NEWS 或 POST。 */
    private String moduleType;

    /** 排序权重。 */
    private Integer sortOrder;

    /** 状态：1启用，0禁用。 */
    private Integer status;
}
