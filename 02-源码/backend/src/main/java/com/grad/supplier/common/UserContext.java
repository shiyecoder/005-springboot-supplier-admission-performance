package com.grad.supplier.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller 只读。
 * 使用 ThreadLocal 保证同一请求线程内可取到角色与数据范围。
 */
public class UserContext {

    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, Long supplierId, String dataScope) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long supplierId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.supplierId();
    }

    /** 外部供应商联系人只能看自己企业的数据 */
    public static boolean isSupplierSide() {
        return "supplier".equals(roleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
