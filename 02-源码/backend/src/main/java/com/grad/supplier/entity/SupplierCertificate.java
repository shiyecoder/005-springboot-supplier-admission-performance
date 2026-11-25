package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证照。支持换证与作废，version_no 递增保留历史版本；
 * cert_status 取值：有效 / 已过期 / 已作废。
 */
@Data
@TableName("supplier_certificate")
public class SupplierCertificate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long supplierId;
    private String certType;
    private String certNo;
    private String filePath;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String certStatus;
    private Integer versionNo;
    private String remark;
    private Long uploaderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
