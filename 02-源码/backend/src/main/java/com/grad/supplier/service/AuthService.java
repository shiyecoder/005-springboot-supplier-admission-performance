package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.JwtUtil;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.AuthDtos;
import com.grad.supplier.entity.*;
import com.grad.supplier.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 认证服务：登录（含失败锁定）、供应商注册、个人资料与菜单权限。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysOrgMapper orgMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierCategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LogService logService;

    @Value("${app.login-fail-limit:5}")
    private int failLimit;
    @Value("${app.login-lock-minutes:10}")
    private int lockMinutes;

    public Map<String, Object> login(AuthDtos.LoginReq req) {
        SysUser user = userMapper.selectByUsername(req.username().trim());
        if (user == null) {
            throw new BizException(401, "账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已停用，请联系管理员");
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(403, "账号已锁定，请 " + lockMinutes + " 分钟后重试");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            // 连续失败累计，达到阈值锁定一段时间，避免暴力破解
            int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            SysUser upd = new SysUser();
            upd.setId(user.getId());
            upd.setLoginFailCount(fails);
            if (fails >= failLimit) {
                upd.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                upd.setLoginFailCount(0);
            }
            userMapper.updateById(upd);
            int left = Math.max(0, failLimit - fails);
            throw new BizException(401, "账号或密码错误" + (left > 0 ? "，还可尝试 " + left + " 次" : "，账号已锁定"));
        }

        SysUser upd = new SysUser();
        upd.setId(user.getId());
        upd.setLoginFailCount(0);
        upd.setLockUntil(null);
        upd.setLastLogin(LocalDateTime.now());
        userMapper.updateById(upd);

        String scope = user.getDataScope() == null ? "self" : user.getDataScope();
        UserContext.CurrentUser cu = new UserContext.CurrentUser(user.getId(), user.getUsername(),
                user.getRealName(), user.getRoleCode(), user.getOrgId(), user.getSupplierId(), scope);
        String token = jwtUtil.create(cu);
        logService.record("认证", "登录", user.getUsername(), "登录成功");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userInfo(user));
        data.put("menus", menuTree(user.getId(), user.getRoleCode()));
        return data;
    }

    public Map<String, Object> userInfo(SysUser user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("phone", user.getPhone());
        m.put("email", user.getEmail());
        m.put("avatar", user.getAvatar());
        m.put("roleCode", user.getRoleCode());
        m.put("roleName", user.getRoleName());
        m.put("supplierId", user.getSupplierId());
        m.put("orgId", user.getOrgId());
        if (user.getOrgId() != null) {
            SysOrg org = orgMapper.selectById(user.getOrgId());
            m.put("orgName", org == null ? "" : org.getOrgName());
        }
        if (user.getSupplierId() != null) {
            Supplier s = supplierMapper.selectById(user.getSupplierId());
            m.put("supplierName", s == null ? "" : s.getSupplierName());
            m.put("supplierLevel", s == null ? "" : s.getLevel());
            m.put("supplierStatus", s == null ? "" : s.getSupplierStatus());
        }
        return m;
    }

    /** 菜单树：只返回 visible=1 的导航，按钮权限单独以 perms 列表给前端 */
    public List<Map<String, Object>> menuTree(Long userId, String roleCode) {
        List<SysMenu> all = menuMapper.selectByUserId(userId);
        List<Map<String, Object>> roots = new ArrayList<>();
        Map<Long, Map<String, Object>> index = new HashMap<>();
        for (SysMenu m : all) {
            if (!"menu".equals(m.getType()) || m.getVisible() == null || m.getVisible() == 0) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("name", m.getMenuName());
            node.put("path", m.getPath());
            node.put("icon", m.getIcon());
            node.put("children", new ArrayList<Map<String, Object>>());
            index.put(m.getId(), node);
            if (m.getParentId() == null || m.getParentId() == 0) {
                roots.add(node);
            } else {
                Map<String, Object> parent = index.get(m.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> ch = (List<Map<String, Object>>) parent.get("children");
                    ch.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    public Set<String> perms(Long userId) {
        Set<String> set = new HashSet<>();
        for (SysMenu m : menuMapper.selectByUserId(userId)) {
            if ("button".equals(m.getType()) && m.getPerms() != null) {
                set.add(m.getPerms());
            }
        }
        return set;
    }

    /** 供应商注册：同时建主档（潜在）与账号，绑定 supplier 角色 */
    @Transactional
    public void register(AuthDtos.RegisterReq req) {
        if (userMapper.selectByUsername(req.username().trim()) != null) {
            throw new BizException("账号已存在，请更换");
        }
        if (supplierMapper.selectCreditCode(req.creditCode().trim()) != null) {
            throw new BizException("该统一信用代码已存在准入记录，不能重复注册");
        }
        SupplierCategory category = categoryMapper.selectOne(new QueryWrapper<SupplierCategory>()
                .eq("category_code", req.categoryCode() == null ? "RAW" : req.categoryCode()).last("limit 1"));

        Supplier s = new Supplier();
        s.setSupplierCode("SUP-" + LocalDateTime.now().getYear() + "-" + String.format("%04d",
                (supplierMapper.countAll() + 1) * 7 % 9000 + 100));
        s.setSupplierName(req.supplierName().trim());
        s.setCreditCode(req.creditCode().trim());
        s.setCategoryId(category == null ? null : category.getId());
        s.setContactName(req.realName());
        s.setContactPhone(req.phone());
        s.setEmail(req.email());
        s.setSupplierStatus("潜在");
        s.setRiskLevel("低");
        s.setCoverImage("/uploads/seed/supplier/01.jpg");
        s.setVersion(1);
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.insert(s);

        SysUser u = new SysUser();
        u.setUsername(req.username().trim());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        u.setSupplierId(s.getId());
        u.setStatus(1);
        u.setLoginFailCount(0);
        u.setAvatar("/uploads/seed/avatar/01.jpg");
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);

        // 绑定 supplier 角色，保证外部账号只能进入门户菜单
        SysRole role = roleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", "supplier").last("limit 1"));
        if (role != null) {
            roleMapper.bindUser(u.getId(), role.getId());
        }
        logService.record("认证", "供应商注册", s.getSupplierCode(), "企业 " + s.getSupplierName() + " 注册申请");
    }

    public Map<String, Object> profile() {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        SysRole role = roleMapper.selectByUserId(user.getId());
        if (role != null) {
            user.setRoleCode(role.getRoleCode());
            user.setRoleName(role.getRoleName());
        }
        return userInfo(user);
    }

    public void updateProfile(AuthDtos.ProfileReq req) {
        SysUser u = new SysUser();
        u.setId(UserContext.userId());
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        u.setAvatar(req.avatar());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        logService.record("个人中心", "修改资料", null, "更新姓名/联系方式/头像");
    }

    public void changePassword(AuthDtos.PasswordReq req) {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (user == null || !passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser u = new SysUser();
        u.setId(user.getId());
        u.setPassword(passwordEncoder.encode(req.newPassword()));
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        logService.record("个人中心", "修改密码", user.getUsername(), "密码已更新，其他会话失效");
    }
}
