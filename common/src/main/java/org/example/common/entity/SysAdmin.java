package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台管理员实体，对应 sys_admin 表。
 */
@Data
public class SysAdmin {
    /** 主键ID */
    private Integer id;
    /** 后台登录账号 */
    private String username;
    /** 加密密码 */
    private String password;
    /** 角色类型：1超级管理员，2普通管理员 */
    private Integer roleType;
    /** 状态：0禁用，1正常 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
