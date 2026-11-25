package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.SystemDtos;
import com.grad.supplier.entity.SysLog;
import com.grad.supplier.entity.SysOrg;
import com.grad.supplier.entity.SysRole;
import com.grad.supplier.entity.SysUser;
import com.grad.supplier.service.SystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 系统管理：用户、角色、菜单分配、字典、日志。 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/users")
    public R<PageResult<SysUser>> users(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String kw,
                                        @RequestParam(required = false) String roleCode) {
        return R.ok(systemService.users(page, size, kw, roleCode));
    }

    @PostMapping("/users")
    public R<Void> saveUser(@Valid @RequestBody SystemDtos.UserReq req) {
        systemService.saveUser(req);
        return R.ok();
    }

    @PostMapping("/users/{id}/status")
    public R<Void> toggle(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        systemService.toggleUser(id, body.get("status"));
        return R.ok();
    }

    @PostMapping("/users/{id}/reset")
    public R<Void> reset(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }

    @GetMapping("/roles")
    public R<List<SysRole>> roles() {
        return R.ok(systemService.roles());
    }

    @GetMapping("/orgs")
    public R<List<SysOrg>> orgs() {
        return R.ok(systemService.orgs());
    }

    @GetMapping("/menus")
    public R<List<Map<String, Object>>> menus(@RequestParam(required = false) Long roleId) {
        return R.ok(systemService.menusWithChecked(roleId));
    }

    @PostMapping("/menus/assign")
    public R<Void> assign(@Valid @RequestBody SystemDtos.AssignMenuReq req) {
        systemService.assignMenus(req.roleId(), req.menuIds());
        return R.ok();
    }

    @GetMapping("/dicts")
    public R<Map<String, Object>> dicts(@RequestParam(required = false) String type) {
        return R.ok(systemService.dicts(type));
    }

    @GetMapping("/logs")
    public R<PageResult<SysLog>> logs(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String module,
                                      @RequestParam(required = false) String kw) {
        return R.ok(systemService.logs(page, size, module, kw));
    }
}
