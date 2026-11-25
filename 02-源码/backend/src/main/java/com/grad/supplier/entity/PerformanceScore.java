package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单个评分人对某任务的评分，五项指标 + 加权总分。 */
@Data
@TableName("performance_score")
public class PerformanceScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long reviewerId;
    private String reviewerName;
    private BigDecimal qualityScore;
    private BigDecimal deliveryScore;
    private BigDecimal priceScore;
    private BigDecimal responseScore;
    private BigDecimal complaintScore;
    private BigDecimal totalScore;
    private String comment;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
