package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.supplier.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 登录查询：一次带出角色编码与数据范围，避免额外两次查询 */
    @Select("""
            SELECT u.*, r.role_code, r.role_name, r.data_scope
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.username = #{username} AND u.deleted = 0
            LIMIT 1
            """)
    SysUser selectByUsername(@Param("username") String username);

    @Select("""
            SELECT r.role_code FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /** 用户列表带角色名与组织名，角色多选时用 GROUP_CONCAT 合并展示 */
    @Select("""
            <script>
            SELECT u.id, u.username, u.real_name, u.phone, u.email, u.avatar, u.org_id, u.supplier_id,
                   u.status, u.last_login, u.create_time, u.update_time,
                   GROUP_CONCAT(r.role_name) AS role_name, GROUP_CONCAT(r.role_code) AS role_code,
                   o.org_name
            FROM sys_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id
            LEFT JOIN sys_org o ON o.id = u.org_id
            WHERE u.deleted = 0
            <if test="kw != null and kw != ''">
              AND (u.username LIKE CONCAT('%', #{kw}, '%') OR u.real_name LIKE CONCAT('%', #{kw}, '%'))
            </if>
            <if test="roleCode != null and roleCode != ''">
              AND r.role_code = #{roleCode}
            </if>
            GROUP BY u.id
            ORDER BY u.id ASC
            </script>
            """)
    List<SysUser> selectUserPage(@Param("kw") String kw, @Param("roleCode") String roleCode);
}
