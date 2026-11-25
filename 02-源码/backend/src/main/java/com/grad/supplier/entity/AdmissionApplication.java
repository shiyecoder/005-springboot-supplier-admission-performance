package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 准入申请单。状态机：待提交 → 准入审核 → 现场评估 → 待审批 → 已准入/已驳回 → 合作评价 → 分级调整。
 * 每次流转写入 sys_log，记录操作人、时间、前后状态与原因。
 */
@Data
@TableName("admission_application")
public class AdmissionApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applicationNo;
    private Long supplierId;
    private String supplierName;
    private String creditCode;
    private Long categoryId;
    private String contactName;
    private String contactPhone;
    private String address;
    private String annualCapacity;
    private String mainCustomers;
    private String capacityDesc;
    private String status;
    private String currentNode;
    private LocalDateTime submitTime;
    private LocalDateTime deadline;
    private Long reviewerId;
    private Long approverId;
    private String rejectReason;
    private String remark;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String reviewerName;
    @TableField(exist = false)
    private Integer certRequired;
    @TableField(exist = false)
    private Integer certUploaded;
    @TableField(exist = false)
    private Integer scoreCount;
    @TableField(exist = false)
    private Double avgScore;
    @TableField(exist = false)
    private String coverImage;
}
