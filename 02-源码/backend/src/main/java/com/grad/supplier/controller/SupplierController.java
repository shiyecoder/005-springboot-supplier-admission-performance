package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.SupplierDtos;
import com.grad.supplier.entity.Supplier;
import com.grad.supplier.entity.SupplierCategory;
import com.grad.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 供应商主档与类目字典。 */
@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/page")
    public R<PageResult<Supplier>> page(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String kw,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String supplierStatus,
                                        @RequestParam(required = false) String level,
                                        @RequestParam(required = false) String riskLevel,
                                        @RequestParam(required = false) String certStart,
                                        @RequestParam(required = false) String certEnd) {
        return R.ok(supplierService.page(page, size, kw, categoryId, supplierStatus, level, riskLevel, certStart, certEnd));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(supplierService.detail(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody SupplierDtos.SupplierReq req) {
        return R.ok(supplierService.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SupplierDtos.SupplierReq req) {
        supplierService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        supplierService.changeStatus(id, body.get("status"), body.get("reason"));
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return R.ok();
    }

    @GetMapping("/options")
    public R<List<Supplier>> options() {
        return R.ok(supplierService.options());
    }

    @GetMapping("/categories")
    public R<List<SupplierCategory>> categories() {
        return R.ok(supplierService.categories());
    }
}
