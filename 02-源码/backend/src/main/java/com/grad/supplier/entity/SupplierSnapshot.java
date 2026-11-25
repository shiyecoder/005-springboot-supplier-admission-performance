package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分层模型计算快照。保存输入快照（input_json）、规则版本、各项得分、结果等级与解释（explain_json）。
 * 人工修正写入 final_level 与 adjust_reason，原始 result_level 不覆盖。
 */
@Data
@TableName("supplier_snapshot")
public class SupplierSnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long supplierId;
    private Long ruleId;
    private String ruleVersion;
    private String period;
    private String inputJson;
    private BigDecimal deliveryScore;
    private BigDecimal qualityScore;
    private BigDecimal priceScore;
    private BigDecimal responseScore;
    private BigDecimal complaintScore;
    private BigDecimal certScore;
    private BigDecimal totalScore;
    private String resultLevel;
    private String finalLevel;
    private String adjustReason;
    private Long adjustBy;
    private LocalDateTime adjustTime;
    private String explainJson;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String supplierName;
}
