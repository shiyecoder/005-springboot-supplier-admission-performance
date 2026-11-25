package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统用户。供应商联系人也存在此表，用 supplierId 关联所属企业。 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Long orgId;
    private Long supplierId;
    private Integer status;
    private Integer loginFailCount;
    private LocalDateTime lockUntil;
    private LocalDateTime lastLogin;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String roleCode;
    @TableField(exist = false)
    private String orgName;
    @TableField(exist = false)
    private String dataScope;
}
