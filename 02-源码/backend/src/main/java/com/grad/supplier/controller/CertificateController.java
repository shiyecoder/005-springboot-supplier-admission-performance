package com.grad.supplier.controller;

import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.R;
import com.grad.supplier.dto.SupplierDtos;
import com.grad.supplier.entity.SupplierCertificate;
import com.grad.supplier.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 证照：列表、换证、作废、到期预警。 */
@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping("/page")
    public R<PageResult<SupplierCertificate>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long supplierId,
                                                   @RequestParam(required = false) String certType,
                                                   @RequestParam(required = false) String certStatus,
                                                   @RequestParam(required = false) String kw) {
        return R.ok(certificateService.page(page, size, supplierId, certType, certStatus, kw));
    }

    @GetMapping("/supplier/{supplierId}")
    public R<List<SupplierCertificate>> bySupplier(@PathVariable Long supplierId) {
        return R.ok(certificateService.bySupplier(supplierId));
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(certificateService.stats());
    }

    @PostMapping
    public R<Void> save(@Valid @RequestBody SupplierDtos.CertReq req) {
        certificateService.save(req);
        return R.ok();
    }

    @PostMapping("/{id}/void")
    public R<Void> voidCert(@PathVariable Long id, @RequestBody Map<String, String> body) {
        certificateService.voidCert(id, body.get("reason"));
        return R.ok();
    }

    @PostMapping("/refresh")
    public R<Void> refresh() {
        certificateService.refreshAll();
        return R.ok();
    }
}
