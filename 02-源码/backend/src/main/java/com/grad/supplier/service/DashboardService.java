package com.grad.supplier.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.grad.supplier.entity.Supplier;
import com.grad.supplier.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 看板服务。六项核心指标 + 七个图表全部在后端聚合，
 * 前端只负责渲染，保证图表数值与明细列表总数一致。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SupplierMapper supplierMapper;
    private final SupplierCertificateMapper certMapper;
    private final AdmissionApplicationMapper applyMapper;
    private final PerformanceTaskMapper taskMapper;
    private final PerformanceScoreMapper scoreMapper;
    private final SupplierRectificationMapper rectMapper;
    private final SupplierSnapshotMapper snapshotMapper;
    private final DashboardMapper dashboardMapper;

    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        long applyTotal = applyMapper.countAll();
        long approved = applyMapper.countApproved();
        double admissionRate = applyTotal == 0 ? 0 : Math.round(approved * 1000.0 / applyTotal) / 10.0;

        Map<String, Object> avg = scoreMapper.avgIndicators();
        Map<String, Object> summary = dashboardMapper.summary();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("admissionRate", admissionRate);
        metrics.put("applyTotal", applyTotal);
        metrics.put("approvedTotal", approved);
        metrics.put("expiringCert", summary.get("expiringCert"));
        metrics.put("expiredCert", summary.get("expiredCert"));
        metrics.put("levelARatio", supplierMapper.levelARatio());
        metrics.put("deliveryRate", avg == null ? 0 : avg.get("deliveryRate"));
        metrics.put("qualityRate", avg == null ? 0 : avg.get("qualityRate"));
        metrics.put("riskSupplier", summary.get("riskSupplier"));
        metrics.put("rectifying", summary.get("rectifying"));
        metrics.put("paused", summary.get("paused"));
        metrics.put("blacklist", summary.get("blacklist"));
        metrics.put("supplierTotal", supplierMapper.countAll());
        metrics.put("dueTask", summary.get("dueTask"));
        m.put("metrics", metrics);

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("trend", mergeTrend(applyMapper.monthlyTrend(), applyMapper.monthlyApproved()));
        charts.put("level", supplierMapper.countByLevel());
        charts.put("category", supplierMapper.countByCategory());
        charts.put("status", supplierMapper.countByStatus());
        charts.put("cert", certCharts());
        charts.put("topRank", taskMapper.topRank());
        charts.put("rectCompletion", rectMapper.monthlyCompletion());
        charts.put("snapshotLevel", snapshotMapper.countByFinalLevel());
        m.put("charts", charts);

        // 创新模块效果
        long snapTotal = snapshotMapper.countAll();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("total", snapTotal);
        model.put("adopted", snapshotMapper.countAdopted());
        model.put("adjusted", snapshotMapper.countAdjusted());
        model.put("adoptRate", snapTotal == 0 ? 0 : Math.round(snapshotMapper.countAdopted() * 1000.0 / snapTotal) / 10.0);
        model.put("adjustRate", snapTotal == 0 ? 0 : Math.round(snapshotMapper.countAdjusted() * 1000.0 / snapTotal) / 10.0);
        m.put("model", model);

        m.put("riskList", supplierMapper.selectList(new QueryWrapper<Supplier>()
                .in("supplier_status", List.of("限期整改", "暂停", "黑名单"))
                .orderByDesc("update_time").last("limit 5")));
        return m;
    }

    /** 趋势图把两个查询按月份对齐，缺失月份补 0 */
    private List<Map<String, Object>> mergeTrend(List<Map<String, Object>> applied, List<Map<String, Object>> approved) {
        Map<String, Long> a = new LinkedHashMap<>();
        for (Map<String, Object> row : applied) {
            a.put(String.valueOf(row.get("name")), ((Number) row.get("value")).longValue());
        }
        Map<String, Long> b = new LinkedHashMap<>();
        for (Map<String, Object> row : approved) {
            b.put(String.valueOf(row.get("name")), ((Number) row.get("value")).longValue());
        }
        Set<String> months = new TreeSet<>();
        months.addAll(a.keySet());
        months.addAll(b.keySet());
        List<Map<String, Object>> list = new ArrayList<>();
        for (String month : months) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("applied", a.getOrDefault(month, 0L));
            row.put("approved", b.getOrDefault(month, 0L));
            list.add(row);
        }
        return list;
    }

    private Map<String, Object> certCharts() {
        Map<String, Object> bucket = certMapper.certBucket();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expired", bucket == null ? 0 : bucket.get("expired"));
        m.put("within30", bucket == null ? 0 : bucket.get("within30"));
        m.put("within90", bucket == null ? 0 : bucket.get("within90"));
        m.put("beyond90", bucket == null ? 0 : bucket.get("beyond90"));
        return m;
    }
}
