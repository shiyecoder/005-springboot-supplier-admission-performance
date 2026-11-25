package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.SupplierDtos;
import com.grad.supplier.entity.Supplier;
import com.grad.supplier.entity.SupplierCertificate;
import com.grad.supplier.mapper.SupplierCertificateMapper;
import com.grad.supplier.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 证照服务。规则：
 * 1) 换证不覆盖历史，旧版本置为已作废，版本号加一；
 * 2) 到期状态每天刷新一次，查询时也会兜底刷新；
 * 3) 过期证照只影响新增合作类目，不自动删除名录。
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final SupplierCertificateMapper certMapper;
    private final SupplierMapper supplierMapper;
    private final LogService logService;

    public PageResult<SupplierCertificate> page(long page, long size, Long supplierId, String certType,
                                               String certStatus, String kw) {
        Long force = UserContext.isSupplierSide() ? UserContext.supplierId() : supplierId;
        IPage<SupplierCertificate> result = certMapper.selectCertPage(new Page<>(page, size),
                force, certType, certStatus, kw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public List<SupplierCertificate> bySupplier(Long supplierId) {
        Long id = UserContext.isSupplierSide() ? UserContext.supplierId() : supplierId;
        return certMapper.selectBySupplier(id);
    }

    public Map<String, Object> stats() {
        Map<String, Object> bucket = certMapper.certBucket();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expired", bucket == null ? 0 : bucket.get("expired"));
        m.put("within30", bucket == null ? 0 : bucket.get("within30"));
        m.put("within90", bucket == null ? 0 : bucket.get("within90"));
        m.put("beyond90", bucket == null ? 0 : bucket.get("beyond90"));
        m.put("total", certMapper.selectCount(new QueryWrapper<SupplierCertificate>().ne("cert_status", "已作废")));
        return m;
    }

    @Transactional
    public void save(SupplierDtos.CertReq req) {
        if (UserContext.isSupplierSide() && !req.supplierId().equals(UserContext.supplierId())) {
            throw new BizException(403, "只能维护本企业证照");
        }
        if (req.filePath() == null || req.filePath().isBlank()) {
            throw new BizException("证照附件不能为空，请使用上传组件");
        }
        if (req.expireDate() != null && req.issueDate() != null && req.expireDate().isBefore(req.issueDate())) {
            throw new BizException("到期日期不能早于签发日期");
        }
        Supplier supplier = supplierMapper.selectById(req.supplierId());
        if (supplier == null) {
            throw new BizException(404, "供应商不存在");
        }
        // 同类型证照存在则视为换证：旧版本作废，版本号递增
        List<SupplierCertificate> same = certMapper.selectList(new QueryWrapper<SupplierCertificate>()
                .eq("supplier_id", req.supplierId()).eq("cert_type", req.certType()).ne("cert_status", "已作废"));
        int version = 1;
        for (SupplierCertificate old : same) {
            SupplierCertificate upd = new SupplierCertificate();
            upd.setId(old.getId());
            upd.setCertStatus("已作废");
            upd.setRemark("换证作废，由新版本替代");
            upd.setUpdateTime(LocalDateTime.now());
            certMapper.updateById(upd);
            version = Math.max(version, old.getVersionNo() == null ? 1 : old.getVersionNo() + 1);
        }
        SupplierCertificate c = new SupplierCertificate();
        c.setSupplierId(req.supplierId());
        c.setCertType(req.certType());
        c.setCertNo(req.certNo());
        c.setFilePath(req.filePath());
        c.setIssueDate(req.issueDate());
        c.setExpireDate(req.expireDate());
        c.setCertStatus(req.expireDate() != null && req.expireDate().isBefore(LocalDate.now()) ? "已过期" : "有效");
        c.setVersionNo(version);
        c.setRemark(req.remark());
        c.setUploaderId(UserContext.userId());
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        certMapper.insert(c);
        refreshSupplierCertExpire(req.supplierId());
        logService.record("证照", same.isEmpty() ? "新增证照" : "换证", supplier.getSupplierCode(),
                req.certType() + " v" + version + "，到期 " + req.expireDate());
    }

    @Transactional
    public void voidCert(Long id, String reason) {
        SupplierCertificate c = certMapper.selectById(id);
        if (c == null) {
            throw new BizException(404, "证照不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("作废原因不少于 5 个字");
        }
        SupplierCertificate upd = new SupplierCertificate();
        upd.setId(id);
        upd.setCertStatus("已作废");
        upd.setRemark("作废：" + reason);
        upd.setUpdateTime(LocalDateTime.now());
        certMapper.updateById(upd);
        refreshSupplierCertExpire(c.getSupplierId());
        logService.record("证照", "作废证照", String.valueOf(c.getSupplierId()), c.getCertType() + " 作废：" + reason);
    }

    /** 每天 07:00 刷新一次；同时提供查询前的兜底刷新 */
    @Scheduled(cron = "0 0 7 * * ?")
    public void refreshAll() {
        List<SupplierCertificate> list = certMapper.selectList(new QueryWrapper<SupplierCertificate>()
                .ne("cert_status", "已作废").isNotNull("expire_date"));
        for (SupplierCertificate c : list) {
            boolean expired = c.getExpireDate().isBefore(LocalDate.now());
            String target = expired ? "已过期" : "有效";
            if (!target.equals(c.getCertStatus())) {
                SupplierCertificate upd = new SupplierCertificate();
                upd.setId(c.getId());
                upd.setCertStatus(target);
                upd.setUpdateTime(LocalDateTime.now());
                certMapper.updateById(upd);
            }
        }
    }

    /** 回写主档的证照到期日，供列表筛选与预警使用 */
    public void refreshSupplierCertExpire(Long supplierId) {
        List<SupplierCertificate> list = certMapper.selectList(new QueryWrapper<SupplierCertificate>()
                .eq("supplier_id", supplierId).ne("cert_status", "已作废")
                .isNotNull("expire_date").orderByAsc("expire_date"));
        Supplier s = new Supplier();
        s.setId(supplierId);
        s.setCertExpireDate(list.isEmpty() ? null : list.get(0).getExpireDate());
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);
    }
}
