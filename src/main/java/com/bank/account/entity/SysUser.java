package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码(BCrypt加密) —— 永不序列化给前端 */
    @JsonIgnore
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态:0-禁用,1-正常 */
    private Integer status;

    /**
     * 角色:USER-普通用户,ADMIN-管理员
     * 后台管理控制台按该字段做接口级鉴权(见 AdminInterceptor)
     */
    private String role;
}