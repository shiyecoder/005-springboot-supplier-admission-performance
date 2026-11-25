package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 整改任务。status：待提交措施 / 待验证 / 已关闭 / 已逾期 / 已转黑名单。 */
@Data
@TableName("supplier_rectification")
public class SupplierRectification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String rectificationNo;
    private Long supplierId;
    private Long taskId;
    private String source;
    private String issueDesc;
    private String measure;
    private LocalDate deadline;
    private String status;
    private String verifyComment;
    private Long verifyBy;
    private LocalDateTime verifyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String supplierName;
}
