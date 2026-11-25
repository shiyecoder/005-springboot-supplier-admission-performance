package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商主档。统一信用代码全局唯一（逻辑删除后仍占位，避免重复准入）。
 * supplier_status 取值：潜在 / 合格 / 暂停 / 限期整改 / 黑名单 / 已退出。
 */
@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String supplierCode;
    private String supplierName;
    private String creditCode;
    private Long categoryId;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String supplierStatus;
    private String level;
    private String riskLevel;
    private LocalDate admissionDate;
    private LocalDate certExpireDate;
    private String annualCapacity;
    private String mainCustomers;
    private String capacityDesc;
    /** 厂区/企业形象照片，存相对路径 /uploads/seed/supplier/01.jpg */
    private String coverImage;
    private Long ownerId;
    private Long orgId;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private Integer certCount;
    @TableField(exist = false)
    private Integer expiringCertCount;
}
