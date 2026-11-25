package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.PerformanceDtos;
import com.grad.supplier.entity.Supplier;
import com.grad.supplier.entity.SupplierRectification;
import com.grad.supplier.mapper.SupplierMapper;
import com.grad.supplier.mapper.SupplierRectificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 整改闭环：待提交措施 → 待验证 → 已关闭 / 退回重改 / 延期。
 * 逾期未提交由定时任务标记为已逾期，连续不合格可转黑名单审批。
 */
@Service
@RequiredArgsConstructor
public class RectificationService {

    private final SupplierRectificationMapper rectMapper;
    private final SupplierMapper supplierMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    public PageResult<SupplierRectification> page(long page, long size, String kw, String status, String source) {
        Long force = UserContext.isSupplierSide() ? UserContext.supplierId() : null;
        IPage<SupplierRectification> r = rectMapper.selectRectPage(new Page<>(page, size), kw, status, source, force);
        return PageResult.of(r.getTotal(), page, size, r.getRecords());
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendingMeasure", rectMapper.selectCount(new QueryWrapper<SupplierRectification>()
                .eq("status", "待提交措施")));
        m.put("pendingVerify", rectMapper.countPendingVerify());
        m.put("closed", rectMapper.selectCount(new QueryWrapper<SupplierRectification>().eq("status", "已关闭")));
        m.put("overdue", rectMapper.selectCount(new QueryWrapper<SupplierRectification>()
                .in("status", List.of("待提交措施", "待验证")).lt("deadline", LocalDate.now())));
        m.put("completionRate", rectMapper.completionRate() == null ? 0 : rectMapper.completionRate());
        return m;
    }

    @Transactional
    public Long create(PerformanceDtos.RectifyReq req) {
        Supplier s = supplierMapper.selectById(req.supplierId());
        if (s == null) {
            throw new BizException(404, "供应商不存在");
        }
        SupplierRectification r = new SupplierRectification();
        r.setRectificationNo("RCT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%03d", new Random().nextInt(999)));
        r.setSupplierId(req.supplierId());
        r.setTaskId(req.taskId());
        r.setSource(req.source() == null ? "绩效评价" : req.source());
        r.setIssueDesc(req.issueDesc());
        r.setDeadline(req.deadline() == null ? LocalDate.now().plusDays(15) : req.deadline());
        r.setStatus("待提交措施");
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        rectMapper.insert(r);
        logService.record("整改", "创建整改", r.getRectificationNo(), s.getSupplierName() + "：" + req.issueDesc());
        notificationService.pushToSupplier(req.supplierId(), "新的整改任务",
                r.getRectificationNo() + " 需在 " + r.getDeadline() + " 前提交措施",
                r.getRectificationNo(), "rectification", "/portal/rectification");
        return r.getId();
    }

    /** 供应商提交措施：只有待提交措施/退回状态可提交 */
    @Transactional
    public void submitMeasure(Long id, String measure) {
        SupplierRectification r = rectMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "整改任务不存在");
        }
        if (!List.of("待提交措施", "已逾期").contains(r.getStatus())) {
            throw new BizException("当前状态不可提交措施");
        }
        if (UserContext.isSupplierSide() && !r.getSupplierId().equals(UserContext.supplierId())) {
            throw new BizException(403, "只能提交本企业的整改措施");
        }
        if (measure == null || measure.trim().length() < 5) {
            throw new BizException("整改措施不少于 5 个字");
        }
        SupplierRectification upd = new SupplierRectification();
        upd.setId(id);
        upd.setMeasure(measure.trim());
        upd.setStatus("待验证");
        upd.setUpdateTime(LocalDateTime.now());
        rectMapper.updateById(upd);
        logService.record("整改", "提交措施", r.getRectificationNo(), measure.trim());
    }

    /** 采购专员验证：通过则关闭，不通过退回重新整改，也可延期 */
    @Transactional
    public void verify(Long id, PerformanceDtos.VerifyReq req) {
        SupplierRectification r = rectMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "整改任务不存在");
        }
        if (!"待验证".equals(r.getStatus())) {
            throw new BizException("只有待验证的整改可以执行验证");
        }
        if (req.comment() == null || req.comment().trim().length() < 5) {
            throw new BizException("验证意见不少于 5 个字");
        }
        SupplierRectification upd = new SupplierRectification();
        upd.setId(id);
        upd.setVerifyComment(req.comment().trim());
        upd.setVerifyBy(UserContext.userId());
        upd.setVerifyTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        switch (req.conclusion()) {
            case "通过" -> upd.setStatus("已关闭");
            case "不通过" -> upd.setStatus("待提交措施");
            case "延期" -> {
                upd.setStatus("待提交措施");
                upd.setDeadline(req.newDeadline() == null ? LocalDate.now().plusDays(10) : req.newDeadline());
            }
            default -> throw new BizException("验证结论只能是 通过/不通过/延期");
        }
        rectMapper.updateById(upd);
        logService.record("整改", "验证整改", r.getRectificationNo(),
                req.conclusion() + "：" + req.comment());
        notificationService.pushToSupplier(r.getSupplierId(), "整改验证结果",
                r.getRectificationNo() + " " + req.conclusion() + "：" + req.comment(),
                r.getRectificationNo(), "rectification", "/portal/rectification");
    }

    /** 转黑名单：必须记录原因与期限，同时更新供应商状态 */
    @Transactional
    public void toBlacklist(Long id, String reason) {
        SupplierRectification r = rectMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "整改任务不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("列入黑名单必须填写不少于 5 个字的原因");
        }
        SupplierRectification upd = new SupplierRectification();
        upd.setId(id);
        upd.setStatus("已转黑名单");
        upd.setVerifyComment("列入黑名单：" + reason);
        upd.setVerifyBy(UserContext.userId());
        upd.setVerifyTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        rectMapper.updateById(upd);

        Supplier s = new Supplier();
        s.setId(r.getSupplierId());
        s.setSupplierStatus("黑名单");
        s.setRiskLevel("高");
        s.setLevel("D");
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);
        logService.record("整改", "列入黑名单", r.getRectificationNo(), "原因：" + reason);
    }

    /** 每天 08:00 扫描逾期整改 */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 8 * * ?")
    public void markOverdue() {
        List<SupplierRectification> list = rectMapper.selectList(new QueryWrapper<SupplierRectification>()
                .in("status", List.of("待提交措施", "待验证")).lt("deadline", LocalDate.now()));
        for (SupplierRectification r : list) {
            SupplierRectification upd = new SupplierRectification();
            upd.setId(r.getId());
            upd.setStatus("已逾期");
            upd.setUpdateTime(LocalDateTime.now());
            rectMapper.updateById(upd);
        }
    }
}
