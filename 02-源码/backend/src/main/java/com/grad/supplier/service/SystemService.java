package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.dto.SystemDtos;
import com.grad.supplier.entity.*;
import com.grad.supplier.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统管理：用户、角色、菜单、字典、日志。
 * 菜单权限与数据范围分开配置；未分配的按钮不会出现在前端工具栏。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDictMapper dictMapper;
    private final SysOrgMapper orgMapper;
    private final SysLogMapper logMapper;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public PageResult<SysUser> users(long page, long size, String kw, String roleCode) {
        List<SysUser> all = userMapper.selectUserPage(kw, roleCode);
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.of(all.size(), page, size, new ArrayList<>(all.subList(from, to)));
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
    }

    public List<SysOrg> orgs() {
        return orgMapper.selectList(new QueryWrapper<SysOrg>().orderByAsc("sort"));
    }

    @Transactional
    public void saveUser(SystemDtos.UserReq req) {
        SysRole role = roleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", req.roleCode()).last("limit 1"));
        if (role == null) {
            throw new BizException("角色不存在：" + req.roleCode());
        }
        if (req.id() == null) {
            if (userMapper.selectByUsername(req.username().trim()) != null) {
                throw new BizException("账号已存在");
            }
            SysUser u = new SysUser();
            u.setUsername(req.username().trim());
            u.setPassword(passwordEncoder.encode(req.password() == null || req.password().isBlank()
                    ? "123456" : req.password()));
            fillUser(u, req);
            u.setStatus(req.status() == null ? 1 : req.status());
            u.setLoginFailCount(0);
            u.setCreateTime(LocalDateTime.now());
            u.setUpdateTime(LocalDateTime.now());
            userMapper.insert(u);
            roleMapper.bindUser(u.getId(), role.getId());
            logService.record("系统", "新增用户", u.getUsername(), "角色 " + role.getRoleName());
        } else {
            SysUser old = userMapper.selectById(req.id());
            if (old == null) {
                throw new BizException(404, "用户不存在");
            }
            SysUser u = new SysUser();
            u.setId(req.id());
            fillUser(u, req);
            if (req.status() != null) {
                u.setStatus(req.status());
            }
            if (req.password() != null && !req.password().isBlank()) {
                u.setPassword(passwordEncoder.encode(req.password()));
            }
            u.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(u);
            // 角色变更：先解绑再绑定，保证单主角色
            roleMapper.unbindUser(req.id());
            roleMapper.bindUser(req.id(), role.getId());
            logService.record("系统", "编辑用户", old.getUsername(), "角色 " + role.getRoleName());
        }
    }

    private void fillUser(SysUser u, SystemDtos.UserReq req) {
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        u.setAvatar(req.avatar() == null ? "/uploads/seed/avatar/01.jpg" : req.avatar());
        u.setOrgId(req.orgId());
        u.setSupplierId(req.supplierId());
    }

    public void toggleUser(Long id, Integer status) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(404, "用户不存在");
        }
        if ("admin".equals(u.getUsername())) {
            throw new BizException("内置管理员不可停用");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setStatus(status);
        upd.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(upd);
        logService.record("系统", "启停用户", u.getUsername(), status == 1 ? "启用" : "停用");
    }

    public void resetPassword(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(404, "用户不存在");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setPassword(passwordEncoder.encode("123456"));
        upd.setLoginFailCount(0);
        upd.setLockUntil(null);
        userMapper.updateById(upd);
        logService.record("系统", "重置密码", u.getUsername(), "重置为默认密码");
    }

    public List<Map<String, Object>> menusWithChecked(Long roleId) {
        List<SysMenu> all = menuMapper.selectList(new QueryWrapper<SysMenu>()
                .orderByAsc("sort").orderByAsc("id"));
        List<Long> checked = roleId == null ? List.of() : roleMapper.selectMenuIds(roleId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysMenu m : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("name", m.getMenuName());
            node.put("type", m.getType());
            node.put("perms", m.getPerms());
            node.put("visible", m.getVisible());
            node.put("checked", checked.contains(m.getId()));
            list.add(node);
        }
        return list;
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        if ("admin".equals(role.getRoleCode())) {
            throw new BizException("内置管理员的菜单权限不可修改");
        }
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
        logService.record("系统", "分配菜单", role.getRoleCode(), "共 " + (menuIds == null ? 0 : menuIds.size()) + " 项");
    }

    public Map<String, Object> dicts(String type) {
        List<SysDict> list = dictMapper.selectList(new QueryWrapper<SysDict>()
                .eq(type != null && !type.isBlank(), "dict_type", type)
                .eq("status", 1).orderByAsc("dict_type").orderByAsc("sort"));
        Map<String, Object> m = new LinkedHashMap<>();
        for (SysDict d : list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) m.computeIfAbsent(d.getDictType(),
                    k -> new ArrayList<Map<String, Object>>());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", d.getDictCode());
            item.put("label", d.getDictLabel());
            items.add(item);
        }
        return m;
    }

    public PageResult<SysLog> logs(long page, long size, String module, String kw) {
        QueryWrapper<SysLog> w = new QueryWrapper<SysLog>()
                .eq(module != null && !module.isBlank(), "module", module)
                .and(kw != null && !kw.isBlank(), q -> q.like("username", kw).or().like("biz_no", kw))
                .orderByDesc("id");
        long total = logMapper.selectCount(w);
        List<SysLog> list = logMapper.selectList(w.last("limit " + size + " offset " + (page - 1) * size));
        return PageResult.of(total, page, size, list);
    }
}
