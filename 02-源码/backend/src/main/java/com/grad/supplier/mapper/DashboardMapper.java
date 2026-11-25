package com.grad.supplier.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 看板聚合查询。所有统计在后端完成，前端不做二次聚合，
 * 保证图表数值与明细列表总数一致。
 */
@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM supplier WHERE deleted = 0 AND supplier_status IN ('限期整改','暂停','黑名单')) AS riskSupplier,
              (SELECT COUNT(*) FROM supplier_certificate WHERE cert_status IN ('已过期') ) AS expiredCert,
              (SELECT COUNT(*) FROM supplier_certificate
                 WHERE cert_status != '已作废'
                   AND expire_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)) AS expiringCert,
              (SELECT COUNT(*) FROM supplier WHERE deleted = 0 AND supplier_status = '黑名单') AS blacklist,
              (SELECT COUNT(*) FROM supplier WHERE deleted = 0 AND supplier_status = '暂停') AS paused,
              (SELECT COUNT(*) FROM supplier WHERE deleted = 0 AND supplier_status = '限期整改') AS rectifying,
              (SELECT COUNT(*) FROM performance_task
                 WHERE deleted = 0 AND deadline BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)) AS dueTask
            """)
    Map<String, Object> summary();
}
