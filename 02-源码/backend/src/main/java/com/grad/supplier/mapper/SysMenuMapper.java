package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.supplier.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 按角色取菜单：菜单权限与按钮权限一起返回，前端据此渲染导航和按钮 */
    @Select("""
            SELECT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            WHERE rm.role_id = #{roleId}
            ORDER BY m.sort ASC, m.id ASC
            """)
    List<SysMenu> selectByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY m.sort ASC, m.id ASC
            """)
    List<SysMenu> selectByUserId(@Param("userId") Long userId);
}
