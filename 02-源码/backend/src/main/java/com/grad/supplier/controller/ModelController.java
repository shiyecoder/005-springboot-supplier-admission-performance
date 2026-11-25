package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.PerformanceDtos;
import com.grad.supplier.entity.SupplierRule;
import com.grad.supplier.entity.SupplierSnapshot;
import com.grad.supplier.service.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 供应商动态分层模型（创新点）：规则配置、执行计算、计算留痕、人工修正、效果统计。
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping("/rule")
    public R<SupplierRule> activeRule() {
        return R.ok(modelService.activeRule());
    }

    @GetMapping("/rules")
    public R<List<SupplierRule>> rules() {
        return R.ok(modelService.rules());
    }

    @PostMapping("/rule")
    public R<SupplierRule> saveRule(@Valid @RequestBody PerformanceDtos.RuleReq req) {
        return R.ok(modelService.saveRule(req));
    }

    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@Valid @RequestBody PerformanceDtos.EvaluateReq req) {
        return R.ok(modelService.evaluate(req.supplierId(), req.period()));
    }

    @GetMapping("/snapshots")
    public R<PageResult<SupplierSnapshot>> snapshots(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) Long supplierId,
                                                     @RequestParam(required = false) String period,
                                                     @RequestParam(required = false) String level) {
        return R.ok(modelService.snapshots(page, size, supplierId, period, level));
    }

    @GetMapping("/snapshots/{id}")
    public R<Map<String, Object>> snapshot(@PathVariable Long id) {
        return R.ok(modelService.snapshotDetail(id));
    }

    @PostMapping("/snapshots/{id}/adjust")
    public R<Void> adjust(@PathVariable Long id, @Valid @RequestBody PerformanceDtos.AdjustReq req) {
        modelService.adjust(id, req);
        return R.ok();
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(modelService.stats());
    }
}
