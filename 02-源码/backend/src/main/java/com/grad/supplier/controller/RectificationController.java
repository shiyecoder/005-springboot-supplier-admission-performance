package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.PerformanceDtos;
import com.grad.supplier.entity.SupplierRectification;
import com.grad.supplier.service.RectificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 整改：创建、提交措施、验证、转黑名单。 */
@RestController
@RequestMapping("/api/rectification")
@RequiredArgsConstructor
public class RectificationController {

    private final RectificationService rectificationService;

    @GetMapping("/page")
    public R<PageResult<SupplierRectification>> page(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String kw,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String source) {
        return R.ok(rectificationService.page(page, size, kw, status, source));
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(rectificationService.stats());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody PerformanceDtos.RectifyReq req) {
        return R.ok(rectificationService.create(req));
    }

    @PostMapping("/{id}/measure")
    public R<Void> measure(@PathVariable Long id, @Valid @RequestBody PerformanceDtos.MeasureReq req) {
        rectificationService.submitMeasure(id, req.measure());
        return R.ok();
    }

    @PostMapping("/{id}/verify")
    public R<Void> verify(@PathVariable Long id, @Valid @RequestBody PerformanceDtos.VerifyReq req) {
        rectificationService.verify(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/blacklist")
    public R<Void> blacklist(@PathVariable Long id, @RequestBody Map<String, String> body) {
        rectificationService.toBlacklist(id, body.get("reason"));
        return R.ok();
    }
}
