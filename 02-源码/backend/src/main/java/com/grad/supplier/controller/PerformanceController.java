package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.PerformanceDtos;
import com.grad.supplier.entity.PerformanceTask;
import com.grad.supplier.service.PerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 绩效评价：任务、评分、发布、作废。 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/page")
    public R<PageResult<PerformanceTask>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String kw,
                                               @RequestParam(required = false) String period,
                                               @RequestParam(required = false) String status) {
        return R.ok(performanceService.page(page, size, kw, period, status));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(performanceService.detail(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody PerformanceDtos.TaskReq req) {
        return R.ok(performanceService.create(req));
    }

    @PostMapping("/{id}/score")
    public R<BigDecimal> score(@PathVariable Long id, @Valid @RequestBody PerformanceDtos.ScoreReq req) {
        return R.ok(performanceService.score(id, req));
    }

    @PostMapping("/{id}/publish")
    public R<Map<String, Object>> publish(@PathVariable Long id) {
        return R.ok(performanceService.publish(id));
    }

    @PostMapping("/{id}/void")
    public R<Void> voidTask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        performanceService.voidTask(id, body.get("reason"));
        return R.ok();
    }
}
