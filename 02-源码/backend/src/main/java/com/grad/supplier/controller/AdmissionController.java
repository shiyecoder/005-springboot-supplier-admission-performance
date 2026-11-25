package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.AdmissionDtos;
import com.grad.supplier.entity.AdmissionApplication;
import com.grad.supplier.service.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/** 准入申请：列表、详情、提交、状态流转、专业评审打分。 */
@RestController
@RequestMapping("/api/admission")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @GetMapping("/page")
    public R<PageResult<AdmissionApplication>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String kw,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String tab) {
        return R.ok(admissionService.page(page, size, kw, categoryId, status, tab));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(admissionService.detail(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody AdmissionDtos.ApplyReq req) {
        return R.ok(admissionService.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AdmissionDtos.ApplyReq req) {
        admissionService.updateDraft(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @Valid @RequestBody AdmissionDtos.TransitionReq req) {
        admissionService.transition(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/review")
    public R<BigDecimal> review(@PathVariable Long id, @Valid @RequestBody AdmissionDtos.ReviewReq req) {
        return R.ok(admissionService.review(id, req));
    }

    @PostMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @Valid @RequestBody AdmissionDtos.AssignReq req) {
        admissionService.assign(id, req.reviewerId());
        return R.ok();
    }
}
