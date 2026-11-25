package com.grad.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 绩效评价、整改与规则入参。 */
public class PerformanceDtos {

    public record TaskReq(@NotNull(message = "不能为空") Long supplierId,
                          @NotBlank(message = "不能为空") String periodType,
                          @NotBlank(message = "不能为空") String period,
                          LocalDate startDate,
                          LocalDate endDate,
                          LocalDate deadline,
                          String remark) {
    }

    public record ScoreReq(@NotNull(message = "不能为空") BigDecimal qualityScore,
                           @NotNull(message = "不能为空") BigDecimal deliveryScore,
                           @NotNull(message = "不能为空") BigDecimal priceScore,
                           @NotNull(message = "不能为空") BigDecimal responseScore,
                           @NotNull(message = "不能为空") BigDecimal complaintScore,
                           String comment,
                           Boolean submit) {
    }

    public record RectifyReq(@NotNull(message = "不能为空") Long supplierId,
                             Long taskId,
                             String source,
                             @NotBlank(message = "不能为空") String issueDesc,
                             LocalDate deadline) {
    }

    public record MeasureReq(@NotBlank(message = "不能为空") String measure) {
    }

    /** 验证：verify=true 通过关闭，false 退回重新整改，extend=true 延期 */
    public record VerifyReq(@NotBlank(message = "不能为空") String conclusion,
                            @NotBlank(message = "验证意见不少于 5 个字") String comment,
                            LocalDate newDeadline) {
    }

    public record RuleReq(@NotNull BigDecimal weightDelivery,
                          @NotNull BigDecimal weightQuality,
                          @NotNull BigDecimal weightPrice,
                          @NotNull BigDecimal weightResponse,
                          @NotNull BigDecimal weightComplaint,
                          @NotNull BigDecimal weightCert,
                          @NotNull BigDecimal thresholdA,
                          @NotNull BigDecimal thresholdB,
                          @NotNull BigDecimal thresholdC,
                          String ruleName) {
    }

    public record EvaluateReq(@NotNull(message = "不能为空") Long supplierId,
                              String period) {
    }

    public record AdjustReq(@NotBlank(message = "不能为空") String level,
                            @NotBlank(message = "原因不少于 5 个字") String reason) {
    }
}
