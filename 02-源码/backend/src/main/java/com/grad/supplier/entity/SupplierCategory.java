package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 供货类目。requiredCert 列出该类目必填证照，准入时用于材料校验。 */
@Data
@TableName("supplier_category")
public class SupplierCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String requiredCert;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
