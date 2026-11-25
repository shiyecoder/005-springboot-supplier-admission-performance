package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 绩效评价任务。period_type：月度/季度/年度；status：待评分/评分中/已发布/已作废。 */
@Data
@TableName("performance_task")
public class PerformanceTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long supplierId;
    private String periodType;
    private String period;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate deadline;
    private Long ownerId;
    private BigDecimal avgScore;
    private String finalLevel;
    private LocalDateTime publishedTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private Integer scoredCount;
}
