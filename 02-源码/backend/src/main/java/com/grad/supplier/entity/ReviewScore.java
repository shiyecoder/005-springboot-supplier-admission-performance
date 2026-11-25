package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 专业评审多人独立评分。status=已提交 后不可修改，需作废重评。 */
@Data
@TableName("review_score")
public class ReviewScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Long reviewerId;
    private String reviewerName;
    private BigDecimal qualityScore;
    private BigDecimal techScore;
    private BigDecimal deliveryScore;
    private BigDecimal priceScore;
    private BigDecimal serviceScore;
    private BigDecimal totalScore;
    private String conclusion;
    private String comment;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
