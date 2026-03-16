package org.example.common.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 普通用户实体，对应 user 表。
 */
@Data
public class User {
    /** 主键ID */
    private Long id;
    /** 登录用户名(全局唯一) */
    private String username;
    /** 加密密码 */
    private String password;
    /** 前端展示昵称 */
    private String nickname;
    /** 头像URL */
    private String avatar;
    /** 绑定邮箱 */
    private String email;
    /** 账号状态：0禁用，1正常 */
    private Integer status;
    /** 逻辑删除标记：0正常，1已删除 */
    private Integer isDeleted;
    /** 注册时间 */
    private LocalDateTime createTime;
    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
