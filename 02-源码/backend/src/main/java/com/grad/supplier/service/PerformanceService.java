package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.PerformanceDtos;
import com.grad.supplier.entity.*;
import com.grad.supplier.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 绩效评价服务。
 * 规则：
 * 1) 任务按周期创建，支持多人独立评分；
 * 2) 评分发布前可暂存，发布后整单锁定，如需更正必须作废重评；
 * 3) 发布时按规则版本换算等级，低于 C 级阈值自动生成整改任务。
 */
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceTaskMapper taskMapper;
    private final PerformanceScoreMapper scoreMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierRuleMapper ruleMapper;
    private final SupplierRectificationMapper rectMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    public PageResult<PerformanceTask> page(long page, long size, String kw, String period, String status) {
        Long force = UserContext.isSupplierSide() ? UserContext.supplierId() : null;
        IPage<PerformanceTask> r = taskMapper.selectTaskPage(new Page<>(page, size), kw, period, status, force);
        return PageResult.of(r.getTotal(), page, size, r.getRecords());
    }

    public Map<String, Object> detail(Long id) {
        PerformanceTask t = taskMapper.selectDetail(id);
        if (t == null) {
            throw new BizException(404, "评价任务不存在");
        }
        if (UserContext.isSupplierSide() && !Objects.equals(t.getSupplierId(), UserContext.supplierId())) {
            throw new BizException(403, "无权查看其他供应商的评价");
        }
        List<PerformanceScore> scores = scoreMapper.selectByTask(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("task", t);
        m.put("scores", scores);
        m.put("mine", scores.stream()
                .filter(s -> Objects.equals(s.getReviewerId(), UserContext.userId()))
                .findFirst().orElse(null));
        long submitted = scores.stream().filter(s -> "已提交".equals(s.getStatus())).count();
        m.put("submittedCount", submitted);
        return m;
    }

    @Transactional
    public Long create(PerformanceDtos.TaskReq req) {
        Supplier s = supplierMapper.selectById(req.supplierId());
        if (s == null) {
            throw new BizException(404, "供应商不存在");
        }
        if ("黑名单".equals(s.getSupplierStatus())) {
            throw new BizException("黑名单期间不可发起绩效评价");
        }
        Long dup = taskMapper.selectCount(new QueryWrapper<PerformanceTask>()
                .eq("supplier_id", req.supplierId()).eq("period", req.period()).ne("status", "已作废"));
        if (dup != null && dup > 0) {
            throw new BizException("该供应商本周期已存在评价任务，不能重复创建");
        }
        PerformanceTask t = new PerformanceTask();
        t.setTaskNo("PFM-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%03d", new Random().nextInt(999)));
        t.setSupplierId(req.supplierId());
        t.setPeriodType(req.periodType());
        t.setPeriod(req.period());
        t.setStatus("待评分");
        t.setStartDate(req.startDate() == null ? LocalDate.now().withDayOfMonth(1) : req.startDate());
        t.setEndDate(req.endDate() == null ? LocalDate.now() : req.endDate());
        t.setDeadline(req.deadline() == null ? LocalDate.now().plusDays(10) : req.deadline());
        t.setOwnerId(UserContext.userId());
        t.setRemark(req.remark());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(t);
        logService.record("绩效", "创建评价任务", t.getTaskNo(), s.getSupplierName() + " " + req.period());
        notificationService.pushToSupplier(req.supplierId(), "新的绩效评价任务",
                t.getTaskNo() + "（" + req.period() + "）已创建，请配合提供数据",
                t.getTaskNo(), "performance", "/portal/performance");
        return t.getId();
    }

    /** 评分：五项加权（质量 30、交付 25、服务 20、价格 15、投诉 10） */
    @Transactional
    public BigDecimal score(Long taskId, PerformanceDtos.ScoreReq req) {
        PerformanceTask t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BizException(404, "评价任务不存在");
        }
        if ("已发布".equals(t.getStatus()) || "已作废".equals(t.getStatus())) {
            throw new BizException("任务已发布或已作废，不可再评分");
        }
        PerformanceScore exist = scoreMapper.selectMine(taskId, UserContext.userId());
        if (exist != null && "已提交".equals(exist.getStatus()) && !Boolean.FALSE.equals(req.submit())) {
            throw new BizException("你的评分已提交，不可修改；如需更正请申请作废重评");
        }
        BigDecimal total = req.qualityScore().multiply(new BigDecimal("0.30"))
                .add(req.deliveryScore().multiply(new BigDecimal("0.25")))
                .add(req.responseScore().multiply(new BigDecimal("0.20")))
                .add(req.priceScore().multiply(new BigDecimal("0.15")))
                .add(req.complaintScore().multiply(new BigDecimal("0.10")))
                .setScale(1, RoundingMode.HALF_UP);

        PerformanceScore ps = new PerformanceScore();
        ps.setTaskId(taskId);
        ps.setReviewerId(UserContext.userId());
        ps.setReviewerName(UserContext.get().realName());
        ps.setQualityScore(req.qualityScore());
        ps.setDeliveryScore(req.deliveryScore());
        ps.setPriceScore(req.priceScore());
        ps.setResponseScore(req.responseScore());
        ps.setComplaintScore(req.complaintScore());
        ps.setTotalScore(total);
        ps.setComment(req.comment());
        ps.setStatus(Boolean.FALSE.equals(req.submit()) ? "草稿" : "已提交");
        ps.setUpdateTime(LocalDateTime.now());
        if (exist == null) {
            ps.setCreateTime(LocalDateTime.now());
            scoreMapper.insert(ps);
        } else {
            ps.setId(exist.getId());
            scoreMapper.updateById(ps);
        }
        if ("待评分".equals(t.getStatus())) {
            PerformanceTask upd = new PerformanceTask();
            upd.setId(taskId);
            upd.setStatus("评分中");
            upd.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(upd);
        }
        logService.record("绩效", "提交评分", t.getTaskNo(), "加权总分 " + total);
        return total;
    }

    /** 发布：汇总已提交评分，写入均分与最终等级，低于阈值自动生成整改任务 */
    @Transactional
    public Map<String, Object> publish(Long taskId) {
        PerformanceTask t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BizException(404, "评价任务不存在");
        }
        if ("已发布".equals(t.getStatus())) {
            throw new BizException("该任务已发布，不能重复发布");
        }
        if (!List.of("采购专员", "采购管理员").contains(UserContext.roleCode())) {
            throw new BizException(403, "只有采购专员或管理员可以发布评价");
        }
        List<PerformanceScore> scores = scoreMapper.selectByTask(taskId).stream()
                .filter(s -> "已提交".equals(s.getStatus())).toList();
        if (scores.isEmpty()) {
            throw new BizException("至少需要一条已提交评分才能发布");
        }
        double avg = scores.stream().mapToDouble(s -> s.getTotalScore().doubleValue()).average().orElse(0);
        BigDecimal avgScore = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        SupplierRule rule = ruleMapper.selectActive();
        String level = avgScore.compareTo(rule.getThresholdA()) >= 0 ? "A"
                : avgScore.compareTo(rule.getThresholdB()) >= 0 ? "B"
                : avgScore.compareTo(rule.getThresholdC()) >= 0 ? "C" : "D";

        PerformanceTask upd = new PerformanceTask();
        upd.setId(taskId);
        upd.setStatus("已发布");
        upd.setAvgScore(avgScore);
        upd.setFinalLevel(level);
        upd.setPublishedTime(LocalDateTime.now());
        upd.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(upd);

        Supplier s = new Supplier();
        s.setId(t.getSupplierId());
        s.setLevel(level);
        // D 级进入限期整改，C 级维持合格，A/B 级保持合格
        s.setSupplierStatus("D".equals(level) ? "限期整改" : "合格");
        s.setRiskLevel("D".equals(level) ? "高" : "C".equals(level) ? "中" : "低");
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgScore", avgScore);
        result.put("level", level);
        Long rectId = null;
        if ("D".equals(level)) {
            SupplierRectification r = new SupplierRectification();
            r.setRectificationNo("RCT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + String.format("%03d", new Random().nextInt(999)));
            r.setSupplierId(t.getSupplierId());
            r.setTaskId(taskId);
            r.setSource("绩效评价");
            r.setIssueDesc("本期综合得分 " + avgScore + "，低于 C 级阈值 " + rule.getThresholdC() + "，需限期整改");
            r.setDeadline(LocalDate.now().plusDays(15));
            r.setStatus("待提交措施");
            r.setCreateTime(LocalDateTime.now());
            r.setUpdateTime(LocalDateTime.now());
            rectMapper.insert(r);
            rectId = r.getId();
        }
        result.put("rectificationId", rectId);
        logService.record("绩效", "发布评价", t.getTaskNo(), "均分 " + avgScore + "，等级 " + level);
        notificationService.pushToSupplier(t.getSupplierId(), "绩效评价结果已发布",
                t.getPeriod() + " 评价得分 " + avgScore + "，等级 " + level, t.getTaskNo(), "performance", "/portal/performance");
        return result;
    }

    /** 作废：发布后更正必须作废重评，原因不少于 5 字 */
    @Transactional
    public void voidTask(Long taskId, String reason) {
        PerformanceTask t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BizException(404, "评价任务不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("作废原因不少于 5 个字");
        }
        if ("已作废".equals(t.getStatus())) {
            throw new BizException("该任务已作废");
        }
        PerformanceTask upd = new PerformanceTask();
        upd.setId(taskId);
        upd.setStatus("已作废");
        upd.setRemark("作废原因：" + reason);
        upd.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(upd);
        logService.record("绩效", "作废评价", t.getTaskNo(), "原因：" + reason);
    }
}
