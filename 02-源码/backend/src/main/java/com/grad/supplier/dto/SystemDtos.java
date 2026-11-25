package com.grad.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 系统管理入参。 */
public class SystemDtos {

    public record UserReq(Long id,
                          @NotBlank(message = "不能为空") String username,
                          String password,
                          @NotBlank(message = "不能为空") String realName,
                          String phone,
                          String email,
                          String avatar,
                          Long orgId,
                          Long supplierId,
                          @NotBlank(message = "不能为空") String roleCode,
                          Integer status) {
    }

    public record RoleReq(@NotBlank(message = "不能为空") String roleCode,
                          @NotBlank(message = "不能为空") String roleName,
                          @NotBlank(message = "不能为空") String dataScope,
                          String remark) {
    }

    public record AssignMenuReq(@NotNull(message = "不能为空") Long roleId,
                                List<Long> menuIds) {
    }
}
