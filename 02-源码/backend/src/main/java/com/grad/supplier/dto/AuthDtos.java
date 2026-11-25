package com.grad.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 认证与个人中心入参。 */
public class AuthDtos {

    public record LoginReq(@NotBlank(message = "不能为空") String username,
                           @NotBlank(message = "不能为空") String password) {
    }

    public record RegisterReq(@NotBlank(message = "不能为空") String username,
                              @NotBlank(message = "不能为空") @Size(min = 6, message = "至少 6 位") String password,
                              @NotBlank(message = "不能为空") String realName,
                              @NotBlank(message = "不能为空") String phone,
                              @NotBlank(message = "不能为空") String supplierName,
                              @NotBlank(message = "不能为空") String creditCode,
                              String categoryCode,
                              String email) {
    }

    public record ProfileReq(@NotBlank(message = "不能为空") String realName,
                             String phone, String email, String avatar) {
    }

    public record PasswordReq(@NotBlank(message = "不能为空") String oldPassword,
                              @NotBlank(message = "不能为空") @Size(min = 6, message = "至少 6 位") String newPassword) {
    }
}
