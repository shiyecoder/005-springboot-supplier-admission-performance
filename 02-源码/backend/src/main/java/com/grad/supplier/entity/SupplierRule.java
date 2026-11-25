package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态分层模型的规则版本。六项权重合计必须为 100，修改后生成新版本并记录生效时间。
 * 历史快照引用旧版本，保证结果可追溯。
 */
@Data
@TableName("supplier_rule")
public class SupplierRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String version;
    private String status;
    private LocalDateTime effectTime;
    private BigDecimal weightDelivery;
    private BigDecimal weightQuality;
    private BigDecimal weightPrice;
    private BigDecimal weightResponse;
    private BigDecimal weightComplaint;
    private BigDecimal weightCert;
    private BigDecimal thresholdA;
    private BigDecimal thresholdB;
    private BigDecimal thresholdC;
    private LocalDateTime createTime;
}
