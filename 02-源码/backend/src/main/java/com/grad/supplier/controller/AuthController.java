package com.grad.supplier.controller;

import com.grad.supplier.common.R;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.AuthDtos;
import com.grad.supplier.service.AuthService;
import com.grad.supplier.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证、个人中心、站内消息。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final NotificationService notificationService;

    @PostMapping("/auth/login")
    public R<Map<String, Object>> login(@Valid @RequestBody AuthDtos.LoginReq req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/auth/register")
    public R<Void> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
        authService.register(req);
        return R.ok();
    }

    @GetMapping("/auth/info")
    public R<Map<String, Object>> info() {
        return R.ok(authService.profile());
    }

    @GetMapping("/auth/menus")
    public R<Object> menus() {
        return R.ok(authService.menuTree(UserContext.userId(), UserContext.roleCode()));
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody AuthDtos.ProfileReq req) {
        authService.updateProfile(req);
        return R.ok();
    }

    @PutMapping("/profile/password")
    public R<Void> changePassword(@Valid @RequestBody AuthDtos.PasswordReq req) {
        authService.changePassword(req);
        return R.ok();
    }

    @GetMapping("/messages")
    public R<Object> messages() {
        return R.ok(notificationService.mine());
    }

    @GetMapping("/messages/unread")
    public R<Long> unread() {
        return R.ok(notificationService.unread());
    }

    @PostMapping("/messages/read-all")
    public R<Void> readAll() {
        notificationService.readAll();
        return R.ok();
    }
}
