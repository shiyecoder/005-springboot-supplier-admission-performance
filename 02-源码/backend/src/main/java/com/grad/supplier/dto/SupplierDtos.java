package com.grad.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 供应商与证照入参。 */
public class SupplierDtos {

    public record SupplierReq(
            @NotBlank(message = "不能为空") String supplierCode,
            @NotBlank(message = "不能为空") String supplierName,
            @NotBlank(message = "不能为空") String creditCode,
            @NotNull(message = "不能为空") Long categoryId,
            @NotBlank(message = "不能为空") String contactName,
            String contactPhone,
            String email,
            String address,
            String supplierStatus,
            String level,
            String riskLevel,
            LocalDate admissionDate,
            LocalDate certExpireDate,
            String annualCapacity,
            String mainCustomers,
            String capacityDesc,
            String coverImage,
            Long ownerId,
            Integer version) {
    }

    public record CertReq(@NotNull(message = "不能为空") Long supplierId,
                          @NotBlank(message = "不能为空") String certType,
                          @NotBlank(message = "不能为空") String certNo,
                          @NotBlank(message = "证照附件不能为空") String filePath,
                          LocalDate issueDate,
                          LocalDate expireDate,
                          String remark) {
    }
}
