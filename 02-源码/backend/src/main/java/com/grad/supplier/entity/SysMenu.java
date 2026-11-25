package com.grad.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 菜单。type=menu 为导航，type=button 为按钮权限。visible=0 表示不在侧栏展示（菜单管理、分配菜单）。 */
@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String icon;
    private String type;
    private String perms;
    private Integer sort;
    private Integer visible;

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
