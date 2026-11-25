package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

/**
 * 供应商动态分层模型（创新点）。
 *
 * 设计原则：
 * 1) 纯函数式计算：相同输入 + 相同规则版本必然得到相同结果，便于复现；
 * 2) 每次计算保存输入快照、规则版本、各项得分与解释，结果可追溯；
 * 3) 缺失指标不参与计算，权重按剩余项归一，并在解释中提示补录；
 * 4) 人工修正只写 finalLevel，原始 resultLevel 永不覆盖；
 * 5) 看板统计推荐采纳率、人工调整率，用于验证模型是否有效。
 */
@Service
@RequiredArgsConstructor
public class ModelService {

    private final SupplierRuleMapper ruleMapper;
    private final SupplierSnapshotMapper snapshotMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierCertificateMapper certMapper;
    private final PerformanceTaskMapper taskMapper;
    private final PerformanceScoreMapper scoreMapper;
    private final SupplierRectificationMapper rectMapper;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    public SupplierRule activeRule() {
        SupplierRule rule = ruleMapper.selectActive();
        if (rule == null) {
            throw new BizException("尚未配置分层模型规则，请先在规则配置页保存");
        }
        return rule;
    }

    public List<SupplierRule> rules() {
        return ruleMapper.selectList(new QueryWrapper<SupplierRule>().orderByDesc("effect_time"));
    }

    /**
     * 保存规则：校验权重合计 100、阈值递减，然后停用旧版本并生成新版本。
     * 修改权重只影响新计算或主动重算的数据，历史快照保留原版本号。
     */
    @Transactional
    public SupplierRule saveRule(PerformanceDtos.RuleReq req) {
        BigDecimal sum = req.weightDelivery().add(req.weightQuality()).add(req.weightPrice())
                .add(req.weightResponse()).add(req.weightComplaint()).add(req.weightCert());
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException("六项权重合计必须等于 100%，当前为 " + sum.stripTrailingZeros().toPlainString() + "%");
        }
        if (req.thresholdA().compareTo(req.thresholdB()) <= 0 || req.thresholdB().compareTo(req.thresholdC()) <= 0) {
            throw new BizException("等级阈值必须满足 A > B > C");
        }
        SupplierRule old = ruleMapper.selectActive();
        int nextVersion = 1;
        if (old != null) {
            nextVersion = Integer.parseInt(old.getVersion().replaceAll("\\D", "")) + 1;
            SupplierRule stop = new SupplierRule();
            stop.setId(old.getId());
            stop.setStatus("停用");
            ruleMapper.updateById(stop);
        }
        SupplierRule rule = new SupplierRule();
        rule.setRuleCode("RULE-LAYER");
        rule.setRuleName(req.ruleName() == null || req.ruleName().isBlank() ? "供应商动态分层模型" : req.ruleName());
        rule.setVersion("R" + LocalDateTime.now().getYear() + "." + nextVersion);
        rule.setStatus("启用");
        rule.setEffectTime(LocalDateTime.now());
        rule.setWeightDelivery(req.weightDelivery());
        rule.setWeightQuality(req.weightQuality());
        rule.setWeightPrice(req.weightPrice());
        rule.setWeightResponse(req.weightResponse());
        rule.setWeightComplaint(req.weightComplaint());
        rule.setWeightCert(req.weightCert());
        rule.setThresholdA(req.thresholdA());
        rule.setThresholdB(req.thresholdB());
        rule.setThresholdC(req.thresholdC());
        rule.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        logService.record("分层模型", "保存规则版本", rule.getVersion(),
                "权重 " + req.weightQuality() + "/" + req.weightDelivery() + "/" + req.weightPrice() + "/"
                        + req.weightResponse() + "/" + req.weightComplaint() + "/" + req.weightCert());
        return rule;
    }

    /** 采集指标：优先取已发布评价的均分，缺失则置空并在解释里提示补录 */
    private Map<String, BigDecimal> collectInputs(Long supplierId) {
        Map<String, BigDecimal> in = new LinkedHashMap<>();
        List<PerformanceTask> tasks = taskMapper.selectList(new QueryWrapper<PerformanceTask>()
                .eq("supplier_id", supplierId).eq("status", "已发布"));
        List<PerformanceScore> scores = new ArrayList<>();
        for (PerformanceTask t : tasks) {
            scores.addAll(scoreMapper.selectList(new QueryWrapper<PerformanceScore>()
                    .eq("task_id", t.getId()).eq("status", "已提交")));
        }
        in.put("qualityRate", avg(scores, PerformanceScore::getQualityScore));
        in.put("deliveryRate", avg(scores, PerformanceScore::getDeliveryScore));
        in.put("priceScore", avg(scores, PerformanceScore::getPriceScore));
        in.put("responseScore", avg(scores, PerformanceScore::getResponseScore));
        Long complaints = rectMapper.selectCount(new QueryWrapper<SupplierRectification>()
                .eq("supplier_id", supplierId).eq("source", "投诉"));
        in.put("complaintCount", BigDecimal.valueOf(complaints == null ? 0 : complaints));
        // 资质有效性：过期 0 分、30 天内到期 60 分、正常 100 分
        List<SupplierCertificate> certs = certMapper.selectList(new QueryWrapper<SupplierCertificate>()
                .eq("supplier_id", supplierId).ne("cert_status", "已作废"));
        boolean expired = certs.stream().anyMatch(c -> "已过期".equals(c.getCertStatus()));
        boolean soon = certs.stream().anyMatch(c -> c.getExpireDate() != null
                && !c.getExpireDate().isAfter(LocalDate.now().plusDays(30)));
        in.put("certScore", BigDecimal.valueOf(expired ? 0 : soon ? 60 : 100));
        in.put("certExpired", BigDecimal.valueOf(expired ? 1 : 0));
        return in;
    }

    private BigDecimal avg(List<PerformanceScore> scores, java.util.function.Function<PerformanceScore, BigDecimal> getter) {
        List<BigDecimal> vals = scores.stream().map(getter).filter(Objects::nonNull).toList();
        if (vals.isEmpty()) {
            return null;
        }
        BigDecimal sum = vals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(vals.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 执行分层计算并保存快照。
     * 公式：总分 = Σ(指标得分 × 权重) / Σ(参与计算的权重)，缺失项不计入分母。
     */
    @Transactional
    public Map<String, Object> evaluate(Long supplierId, String period) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new BizException(404, "供应商不存在");
        }
        SupplierRule rule = activeRule();
        Map<String, BigDecimal> in = collectInputs(supplierId);
        Map<String, Object> explain = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        BigDecimal quality = in.get("qualityRate");
        BigDecimal delivery = in.get("deliveryRate");
        BigDecimal price = in.get("priceScore");
        BigDecimal response = in.get("responseScore");
        BigDecimal complaintRate = BigDecimal.valueOf(100).subtract(
                in.get("complaintCount").multiply(BigDecimal.valueOf(15))).max(BigDecimal.ZERO);
        BigDecimal certScore = in.get("certScore");

        addDetail(details, missing, "质量合格率", quality, rule.getWeightQuality(), "%", "质量合格率越高得分越高");
        addDetail(details, missing, "交付及时率", delivery, rule.getWeightDelivery(), "%", "按承诺交期统计");
        addDetail(details, missing, "价格稳定性", price, rule.getWeightPrice(), "分", "价格偏离越小得分越高");
        addDetail(details, missing, "服务响应", response, rule.getWeightResponse(), "分", "响应时长越短得分越高");
        addDetail(details, missing, "投诉情况", complaintRate, rule.getWeightComplaint(), "分", "每次投诉扣 15 分");
        addDetail(details, missing, "资质有效性", certScore, rule.getWeightCert(), "分",
                in.get("certExpired").intValue() > 0 ? "存在过期证照" : "证照均在有效期内");

        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (Map<String, Object> d : details) {
            BigDecimal w = (BigDecimal) d.get("weight");
            BigDecimal s = (BigDecimal) d.get("score");
            if (s != null) {
                weightSum = weightSum.add(w);
                weighted = weighted.add(s.multiply(w));
            }
        }
        if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("该供应商缺少全部评价数据，请先完成绩效评价后再计算");
        }
        BigDecimal total = weighted.divide(weightSum, 4, RoundingMode.HALF_UP)
                .setScale(1, RoundingMode.HALF_UP);
        String level = total.compareTo(rule.getThresholdA()) >= 0 ? "A"
                : total.compareTo(rule.getThresholdB()) >= 0 ? "B"
                : total.compareTo(rule.getThresholdC()) >= 0 ? "C" : "D";

        // 结果解释：加分、减分、未满足条件、可改进项
        List<String> positive = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        List<String> unmet = new ArrayList<>();
        List<String> improve = new ArrayList<>();
        if (quality != null && quality.doubleValue() >= 98) {
            positive.add("质量合格率 " + quality + "%，达到目标值");
        } else if (quality != null) {
            negative.add("质量合格率 " + quality + "%，低于 98% 目标");
            improve.add("将质量合格率提升至 98% 可增加约 1.2 分");
        }
        if (delivery != null && delivery.doubleValue() >= 95) {
            positive.add("交付及时率 " + delivery + "%，优于 95% 目标");
        } else if (delivery != null) {
            negative.add("交付及时率 " + delivery + "%，低于 95% 目标");
            improve.add("改善排产与物流衔接，交付及时率每提升 3% 约加 1.5 分");
        }
        if (in.get("complaintCount").intValue() == 0) {
            positive.add("本周期无投诉记录");
        } else {
            negative.add("本周期投诉 " + in.get("complaintCount").intValue() + " 次");
        }
        if (in.get("certExpired").intValue() > 0) {
            unmet.add("资质有效性不满足要求：存在过期证照，换证前不得新增合作类目");
            improve.add("30 日内完成证照换证，资质项可得满分");
        }
        if (missing.size() > 0) {
            unmet.add("以下指标缺少数据，未参与计算：" + String.join("、", missing));
        }
        if (price != null && price.doubleValue() < 80) {
            negative.add("价格稳定性得分 " + price + "，偏离市场均价较多");
        }

        explain.put("positive", positive);
        explain.put("negative", negative);
        explain.put("unmet", unmet);
        explain.put("improve", improve);
        explain.put("details", details);
        explain.put("weightUsed", weightSum);
        explain.put("missing", missing);
        explain.put("level", level);
        explain.put("total", total);
        explain.put("ruleVersion", rule.getVersion());
        explain.put("summary", "综合得分 " + total + "，等级 " + level + "；" +
                (unmet.isEmpty() ? "无未满足条件" : "存在 " + unmet.size() + " 项未满足条件"));

        SupplierSnapshot snap = new SupplierSnapshot();
        snap.setSupplierId(supplierId);
        snap.setRuleId(rule.getId());
        snap.setRuleVersion(rule.getVersion());
        snap.setPeriod(period == null || period.isBlank()
                ? LocalDate.now().getYear() + " Q" + ((LocalDate.now().getMonthValue() - 1) / 3 + 1) : period);
        snap.setInputJson(toJson(in));
        snap.setQualityScore(quality);
        snap.setDeliveryScore(delivery);
        snap.setPriceScore(price);
        snap.setResponseScore(response);
        snap.setComplaintScore(complaintRate);
        snap.setCertScore(certScore);
        snap.setTotalScore(total);
        snap.setResultLevel(level);
        snap.setFinalLevel(level);
        snap.setExplainJson(toJson(explain));
        snap.setCreateTime(LocalDateTime.now());
        snapshotMapper.insert(snap);

        // 等级回写主档，评级变化必须保留明细与原因，因此不再单独存一份"当前等级"
        Supplier upd = new Supplier();
        upd.setId(supplierId);
        upd.setLevel(level);
        upd.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(upd);

        logService.record("分层模型", "执行计算", supplier.getSupplierCode(),
                "版本 " + rule.getVersion() + "，得分 " + total + "，等级 " + level);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snapshot", snap);
        result.put("supplier", supplier);
        result.put("explain", explain);
        return result;
    }

    private void addDetail(List<Map<String, Object>> details, List<String> missing, String name,
                           BigDecimal score, BigDecimal weight, String unit, String note) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", name);
        d.put("score", score);
        d.put("weight", weight);
        d.put("unit", unit);
        d.put("note", note);
        d.put("weighted", score == null ? null
                : score.multiply(weight).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        if (score == null) {
            missing.add(name);
        }
        details.add(d);
    }

    public PageResult<SupplierSnapshot> snapshots(long page, long size, Long supplierId, String period, String level) {
        Long force = UserContext.isSupplierSide() ? UserContext.supplierId() : supplierId;
        IPage<SupplierSnapshot> r = snapshotMapper.selectSnapshotPage(new Page<>(page, size), force, period, level);
        return PageResult.of(r.getTotal(), page, size, r.getRecords());
    }

    public Map<String, Object> snapshotDetail(Long id) {
        SupplierSnapshot snap = snapshotMapper.selectById(id);
        if (snap == null) {
            throw new BizException(404, "计算记录不存在");
        }
        Supplier s = supplierMapper.selectById(snap.getSupplierId());
        snap.setSupplierName(s == null ? "" : s.getSupplierName());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("snapshot", snap);
        try {
            m.put("explain", snap.getExplainJson() == null ? Map.of() : objectMapper.readValue(snap.getExplainJson(), Map.class));
            m.put("inputs", snap.getInputJson() == null ? Map.of() : objectMapper.readValue(snap.getInputJson(), Map.class));
        } catch (Exception e) {
            m.put("explain", Map.of());
            m.put("inputs", Map.of());
        }
        return m;
    }

    /** 人工修正：只改 finalLevel，原始 resultLevel 保留，原因必填 */
    @Transactional
    public void adjust(Long id, PerformanceDtos.AdjustReq req) {
        SupplierSnapshot snap = snapshotMapper.selectById(id);
        if (snap == null) {
            throw new BizException(404, "计算记录不存在");
        }
        if (req.reason() == null || req.reason().trim().length() < 5) {
            throw new BizException("人工修正必须填写不少于 5 个字的原因");
        }
        if (!List.of("A", "B", "C", "D").contains(req.level())) {
            throw new BizException("修正等级只能是 A/B/C/D");
        }
        if (Objects.equals(req.level(), snap.getResultLevel())) {
            throw new BizException("修正等级与原等级相同，无需修正");
        }
        SupplierSnapshot upd = new SupplierSnapshot();
        upd.setId(id);
        upd.setFinalLevel(req.level());
        upd.setAdjustReason(req.reason().trim());
        upd.setAdjustBy(UserContext.userId());
        upd.setAdjustTime(LocalDateTime.now());
        snapshotMapper.updateById(upd);
        Supplier s = new Supplier();
        s.setId(snap.getSupplierId());
        s.setLevel(req.level());
        s.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(s);
        logService.record("分层模型", "人工修正", String.valueOf(snap.getSupplierId()),
                "原等级 " + snap.getResultLevel() + " → " + req.level() + "，原因：" + req.reason());
    }

    /** 模型效果统计：推荐采纳率、人工调整率，用于验证创新模块有效性 */
    public Map<String, Object> stats() {
        long total = snapshotMapper.countAll();
        long adopted = snapshotMapper.countAdopted();
        long adjusted = snapshotMapper.countAdjusted();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("adopted", adopted);
        m.put("adjusted", adjusted);
        m.put("adoptRate", total == 0 ? 0 : Math.round(adopted * 1000.0 / total) / 10.0);
        m.put("adjustRate", total == 0 ? 0 : Math.round(adjusted * 1000.0 / total) / 10.0);
        m.put("levelDistribution", snapshotMapper.countByFinalLevel());
        return m;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
