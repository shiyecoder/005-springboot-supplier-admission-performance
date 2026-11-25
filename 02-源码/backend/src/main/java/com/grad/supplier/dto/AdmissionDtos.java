package com.grad.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 准入申请与评审入参。 */
public class AdmissionDtos {

    public record ApplyReq(
            Long supplierId,
            @NotBlank(message = "不能为空") String supplierName,
            @NotBlank(message = "不能为空") String creditCode,
            @NotNull(message = "不能为空") Long categoryId,
            @NotBlank(message = "不能为空") String contactName,
            @NotBlank(message = "不能为空") String contactPhone,
            String address,
            String annualCapacity,
            String mainCustomers,
            String capacityDesc,
            String remark) {
    }

    /** 状态流转：targetStatus + 原因（退回、驳回、作废时原因不少于 5 字） */
    public record TransitionReq(@NotBlank(message = "不能为空") String targetStatus,
                                String reason) {
    }

    public record ReviewReq(
            @NotNull(message = "不能为空") BigDecimal qualityScore,
            @NotNull(message = "不能为空") BigDecimal techScore,
            @NotNull(message = "不能为空") BigDecimal deliveryScore,
            @NotNull(message = "不能为空") BigDecimal priceScore,
            @NotNull(message = "不能为空") BigDecimal serviceScore,
            String conclusion,
            @Size(max = 500, message = "不超过 500 字") String comment,
            /** true 表示提交，false 表示暂存 */
            Boolean submit) {
    }

    public record AssignReq(@NotNull(message = "不能为空") Long reviewerId) {
    }
}
