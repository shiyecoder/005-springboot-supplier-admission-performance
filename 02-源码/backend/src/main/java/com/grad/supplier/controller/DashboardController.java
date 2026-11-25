package com.grad.supplier.controller;

import com.grad.supplier.common.R;
import com.grad.supplier.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 首页看板：6 项指标 + 7 个图表，全部后端聚合。 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }
}
