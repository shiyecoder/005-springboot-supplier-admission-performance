package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.common.BizException;
import com.grad.supplier.common.PageResult;
import com.grad.supplier.common.UserContext;
import com.grad.supplier.dto.AdmissionDtos;
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
 * 准入申请服务，核心是状态机：
 * 待提交 → 准入审核 → 现场评估 → 待审批 → 已准入 / 已驳回 → 合作评价 → 分级调整。
 *
 * 设计要点：
 * 1) 所有流转由后端校验，前端按钮只是入口；
 * 2) 退回、驳回必须填写不少于 5 个字的原因，并写入操作日志；
 * 3) 同一申请同一评审人只能提交一次评分，提交后不可修改；
 * 4) 审批通过时同步落供应商主档，等级由评审均分换算。
 */
@Service
@RequiredArgsConstructor
public class AdmissionService {

    private static final Map<String, Set<String>> ALLOWED = new HashMap<>();

    static {
        // 当前状态 -> 允许的下一状态
        ALLOWED.put("待提交", Set.of("准入审核"));
        ALLOWED.put("准入审核", Set.of("现场评估", "待提交"));
        ALLOWED.put("现场评估", Set.of("待审批", "准入审核"));
        ALLOWED.put("待审批", Set.of("已准入", "已驳回"));
        ALLOWED.put("已准入", Set.of("合作评价"));
        ALLOWED.put("合作评价", Set.of("分级调整"));
        ALLOWED.put("已驳回", Set.of("待提交"));
        ALLOWED.put("分级调整", Set.of());
    }

    private final AdmissionApplicationMapper applyMapper;
    private final ReviewScoreMapper reviewMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierCertificateMapper certMapper;
    private final SupplierCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final LogService logService;
    private final NotificationService notificationService;

    public PageResult<AdmissionApplication> page(long page, long size, String kw, Long categoryId,
                                                 String status, String tab) {
        Long forceSupplierId = UserContext.isSupplierSide() ? UserContext.supplierId() : null;
        IPage<AdmissionApplication> result = applyMapper.selectApplicationPage(new Page<>(page, size),
                kw, categoryId, status, tab, forceSupplierId);
        result.getRecords().forEach(a -> {
            a.setCertRequired(countRequired(a.getCategoryId()));
            if (a.getAvgScore() != null) {
                a.setAvgScore(Math.round(a.getAvgScore() * 10) / 10.0);
            }
        });
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    private int countRequired(Long categoryId) {
        if (categoryId == null) {
            return 6;
        }
        SupplierCategory c = categoryMapper.selectById(categoryId);
        return c == null || c.getRequiredCert() == null || c.getRequiredCert().isBlank()
                ? 6 : c.getRequiredCert().split(",").length;
    }

    public Map<String, Object> detail(Long id) {
        AdmissionApplication a = applyMapper.selectDetail(id);
        if (a == null) {
            throw new BizException(404, "申请不存在");
        }
        if (UserContext.isSupplierSide() && !Objects.equals(a.getSupplierId(), UserContext.supplierId())) {
            throw new BizException(403, "无权查看其他企业的申请");
        }
        a.setCertRequired(countRequired(a.getCategoryId()));
        List<ReviewScore> scores = reviewMapper.selectByApplication(id);
        a.setScoreCount(scores.size());
        OptionalDouble avg = scores.stream().filter(s -> "已提交".equals(s.getStatus()))
                .mapToDouble(s -> s.getTotalScore() == null ? 0 : s.getTotalScore().doubleValue()).average();
        a.setAvgScore(avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("application", a);
        m.put("scores", scores);
        m.put("certificates", a.getSupplierId() == null ? List.of() : certMapper.selectBySupplier(a.getSupplierId()));
        m.put("requiredCerts", a.getCategoryId() == null ? List.of()
                : Arrays.asList(categoryMapper.selectById(a.getCategoryId()).getRequiredCert().split(",")));
        return m;
    }

    /** 新建申请（草稿），供应商侧自动带出企业信息 */
    @Transactional
    public Long create(AdmissionDtos.ApplyReq req) {
        if (UserContext.isSupplierSide() && supplierMapper.selectCreditCode(req.creditCode().trim()) != null) {
            Long exists = supplierMapper.selectCreditCodeId(req.creditCode().trim());
            if (exists != null && !exists.equals(UserContext.supplierId())) {
                throw new BizException("统一信用代码已被其他企业占用");
            }
        }
        AdmissionApplication a = new AdmissionApplication();
        a.setApplicationNo(nextNo());
        a.setSupplierId(req.supplierId() != null ? req.supplierId() : UserContext.supplierId());
        a.setSupplierName(req.supplierName().trim());
        a.setCreditCode(req.creditCode().trim());
        a.setCategoryId(req.categoryId());
        a.setContactName(req.contactName());
        a.setContactPhone(req.contactPhone());
        a.setAddress(req.address());
        a.setAnnualCapacity(req.annualCapacity());
        a.setMainCustomers(req.mainCustomers());
        a.setCapacityDesc(req.capacityDesc());
        a.setRemark(req.remark());
        a.setStatus("待提交");
        a.setCurrentNode("供应商填报");
        a.setVersion(1);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        applyMapper.insert(a);
        logService.record("准入", "新建申请", a.getApplicationNo(), "状态：草稿（待提交）");
        return a.getId();
    }

    public void updateDraft(Long id, AdmissionDtos.ApplyReq req) {
        AdmissionApplication old = requireApply(id);
        if (!"待提交".equals(old.getStatus())) {
            throw new BizException("仅草稿状态可编辑");
        }
        AdmissionApplication a = new AdmissionApplication();
        a.setId(id);
        a.setSupplierName(req.supplierName().trim());
        a.setCreditCode(req.creditCode().trim());
        a.setCategoryId(req.categoryId());
        a.setContactName(req.contactName());
        a.setContactPhone(req.contactPhone());
        a.setAddress(req.address());
        a.setAnnualCapacity(req.annualCapacity());
        a.setMainCustomers(req.mainCustomers());
        a.setCapacityDesc(req.capacityDesc());
        a.setRemark(req.remark());
        a.setUpdateTime(LocalDateTime.now());
        applyMapper.updateById(a);
        logService.record("准入", "编辑草稿", old.getApplicationNo(), "更新申请信息");
    }

    /**
     * 状态流转统一入口。
     * 准入审核 → 现场评估 需要材料齐全；现场评估 → 待审批 需要至少一名评审人已提交评分。
     */
    @Transactional
    public void transition(Long id, AdmissionDtos.TransitionReq req) {
        AdmissionApplication a = requireApply(id);
        String from = a.getStatus();
        String to = req.targetStatus();
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BizException("不允许从「" + from + "」流转到「" + to + "」");
        }
        String role = UserContext.roleCode();
        // 退回（现场评估→准入审核）、驳回（待审批→已驳回）、退回草稿都必须留原因
        boolean needReason = "待提交".equals(to) || "已驳回".equals(to)
                || ("准入审核".equals(to) && "现场评估".equals(from));
        if (needReason && (req.reason() == null || req.reason().trim().length() < 5)) {
            throw new BizException("退回或驳回必须填写不少于 5 个字的原因");
        }
        if (UserContext.isSupplierSide() && !"准入审核".equals(to)) {
            throw new BizException(403, "供应商只能提交申请，审核动作由内部人员完成");
        }
        if ("评审人员".equals(role) && !"待审批".equals(to) && !"准入审核".equals(to)) {
            throw new BizException(403, "评审人员只能完成评审并提交下一节点");
        }

        if ("准入审核".equals(to)) {
            long expired = a.getSupplierId() == null ? 0 : certMapper.selectCount(
                    new QueryWrapper<SupplierCertificate>().eq("supplier_id", a.getSupplierId())
                            .eq("cert_status", "已过期"));
            long uploaded = a.getSupplierId() == null ? 0 : certMapper.selectCount(
                    new QueryWrapper<SupplierCertificate>().eq("supplier_id", a.getSupplierId())
                            .ne("cert_status", "已作废"));
            int required = countRequired(a.getCategoryId());
            if (uploaded < required) {
                throw new BizException("材料不齐：该类目要求 " + required + " 项证照，当前仅上传 " + uploaded + " 项");
            }
            if (expired > 0) {
                throw new BizException("存在 " + expired + " 张过期证照，需换证后再提交");
            }
        }
        if ("待审批".equals(to)) {
            int submitted = reviewMapper.countSubmitted(id);
            if (submitted == 0) {
                throw new BizException("至少需要一名评审人提交评分后才能进入审批");
            }
        }

        AdmissionApplication upd = new AdmissionApplication();
        upd.setId(id);
        upd.setStatus(to);
        upd.setCurrentNode(nodeName(to));
        upd.setUpdateTime(LocalDateTime.now());
        if ("准入审核".equals(to)) {
            upd.setSubmitTime(LocalDateTime.now());
            upd.setDeadline(LocalDateTime.now().plusDays(7));
        }
        if ("已驳回".equals(to)) {
            upd.setRejectReason(req.reason());
        }
        applyMapper.updateById(upd);

        // 审批通过：落供应商主档
        if ("已准入".equals(to)) {
            grantSupplier(a, req.reason());
        }
        logService.record("准入", "状态流转", a.getApplicationNo(),
                from + " → " + to + (req.reason() == null ? "" : "，原因：" + req.reason()));
        notificationService.pushToSupplier(a.getSupplierId(), "准入进度更新",
                a.getApplicationNo() + " 已流转至 " + to,
                a.getApplicationNo(), "admission", "/admission?no=" + a.getApplicationNo());
    }

    private void grantSupplier(AdmissionApplication a, String reason) {
        List<ReviewScore> scores = reviewMapper.selectByApplication(a.getId());
        double avg = scores.stream().filter(s -> "已提交".equals(s.getStatus()))
                .mapToDouble(s -> s.getTotalScore() == null ? 0 : s.getTotalScore().doubleValue())
                .average().orElse(0);
        String level = avg >= 85 ? "A" : avg >= 75 ? "B" : avg >= 60 ? "C" : "D";
        LocalDate today = LocalDate.now();
        LocalDate expire = certMapper.selectList(new QueryWrapper<SupplierCertificate>()
                        .eq("supplier_id", a.getSupplierId()).ne("cert_status", "已作废")
                        .isNotNull("expire_date").orderByAsc("expire_date"))
                .stream().findFirst().map(SupplierCertificate::getExpireDate).orElse(today.plusYears(1));

        if (a.getSupplierId() == null) {
            Supplier s = new Supplier();
            s.setSupplierCode("SUP-" + today.getYear() + "-" + String.format("%04d", (supplierMapper.countAll() + 1) * 3 % 9000 + 100));
            s.setSupplierName(a.getSupplierName());
            s.setCreditCode(a.getCreditCode());
            s.setCategoryId(a.getCategoryId());
            s.setContactName(a.getContactName());
            s.setContactPhone(a.getContactPhone());
            s.setAddress(a.getAddress());
            s.setAnnualCapacity(a.getAnnualCapacity());
            s.setMainCustomers(a.getMainCustomers());
            s.setCapacityDesc(a.getCapacityDesc());
            s.setSupplierStatus("合格");
            s.setLevel(level);
            s.setRiskLevel("低");
            s.setAdmissionDate(today);
            s.setCertExpireDate(expire);
            s.setCoverImage("/uploads/seed/supplier/02.jpg");
            s.setOwnerId(UserContext.userId());
            s.setOrgId(UserContext.get() == null ? null : UserContext.get().orgId());
            s.setVersion(1);
            s.setCreateTime(LocalDateTime.now());
            s.setUpdateTime(LocalDateTime.now());
            supplierMapper.insert(s);

            AdmissionApplication upd = new AdmissionApplication();
            upd.setId(a.getId());
            upd.setSupplierId(s.getId());
            applyMapper.updateById(upd);
        } else {
            Supplier s = new Supplier();
            s.setId(a.getSupplierId());
            s.setSupplierStatus("合格");
            s.setLevel(level);
            s.setAdmissionDate(today);
            s.setCertExpireDate(expire);
            s.setUpdateTime(LocalDateTime.now());
            supplierMapper.updateById(s);
        }
        logService.record("准入", "审批准入", a.getApplicationNo(),
                "评审均分 " + String.format("%.1f", avg) + "，等级 " + level + (reason == null ? "" : "，意见：" + reason));
    }

    /** 专业评审打分：五维加权，提交后锁定 */
    @Transactional
    public BigDecimal review(Long applicationId, AdmissionDtos.ReviewReq req) {
        AdmissionApplication a = requireApply(applicationId);
        if (!List.of("准入审核", "现场评估").contains(a.getStatus())) {
            throw new BizException("当前状态不可评分");
        }
        ReviewScore exist = reviewMapper.selectOne(new QueryWrapper<ReviewScore>()
                .eq("application_id", applicationId).eq("reviewer_id", UserContext.userId()).last("limit 1"));
        if (exist != null && "已提交".equals(exist.getStatus())) {
            throw new BizException("评分已提交，不可修改；如需更正请作废后重新评审");
        }
        BigDecimal total = req.qualityScore().multiply(new BigDecimal("0.30"))
                .add(req.techScore().multiply(new BigDecimal("0.25")))
                .add(req.deliveryScore().multiply(new BigDecimal("0.20")))
                .add(req.priceScore().multiply(new BigDecimal("0.15")))
                .add(req.serviceScore().multiply(new BigDecimal("0.10")))
                .setScale(1, RoundingMode.HALF_UP);

        ReviewScore rs = new ReviewScore();
        rs.setApplicationId(applicationId);
        rs.setReviewerId(UserContext.userId());
        rs.setReviewerName(UserContext.get().realName());
        rs.setQualityScore(req.qualityScore());
        rs.setTechScore(req.techScore());
        rs.setDeliveryScore(req.deliveryScore());
        rs.setPriceScore(req.priceScore());
        rs.setServiceScore(req.serviceScore());
        rs.setTotalScore(total);
        rs.setConclusion(req.conclusion() == null ? (total.doubleValue() >= 75 ? "建议准入" : "建议不准入") : req.conclusion());
        rs.setComment(req.comment());
        rs.setStatus(Boolean.FALSE.equals(req.submit()) ? "草稿" : "已提交");
        rs.setUpdateTime(LocalDateTime.now());
        if (exist == null) {
            rs.setCreateTime(LocalDateTime.now());
            reviewMapper.insert(rs);
        } else {
            rs.setId(exist.getId());
            reviewMapper.updateById(rs);
        }
        logService.record("准入", "提交评分", a.getApplicationNo(),
                "加权总分 " + total + "（" + rs.getConclusion() + "）");
        return total;
    }

    /** 指派评审人，支持多人独立评分 */
    public void assign(Long id, Long reviewerId) {
        AdmissionApplication a = requireApply(id);
        SysUser u = userMapper.selectById(reviewerId);
        if (u == null) {
            throw new BizException("评审人不存在");
        }
        AdmissionApplication upd = new AdmissionApplication();
        upd.setId(id);
        upd.setReviewerId(reviewerId);
        upd.setUpdateTime(LocalDateTime.now());
        applyMapper.updateById(upd);
        logService.record("准入", "指派评审", a.getApplicationNo(), "评审人：" + u.getRealName());
        notificationService.pushToUser(u.getId(), "新的评审任务", a.getApplicationNo() + " 已指派给你评审",
                a.getApplicationNo(), "admission", "/admission?no=" + a.getApplicationNo());
    }

    private AdmissionApplication requireApply(Long id) {
        AdmissionApplication a = applyMapper.selectDetail(id);
        if (a == null) {
            throw new BizException(404, "申请不存在");
        }
        return a;
    }

    private String nextNo() {
        return "ADM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String nodeName(String status) {
        return switch (status) {
            case "待提交" -> "供应商填报";
            case "准入审核" -> "采购专员形式审查";
            case "现场评估" -> "评审人员评分";
            case "待审批" -> "采购管理员审批";
            case "已准入" -> "已进入合格名录";
            case "已驳回" -> "已驳回";
            case "合作评价" -> "周期绩效评价";
            case "分级调整" -> "等级调整";
            default -> status;
        };
    }
}
