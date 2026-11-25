package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.Supplier;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SupplierMapper extends BaseMapper<Supplier> {

    /**
     * 供应商列表：联表带出类目名与责任人；外部供应商账号只看自己企业（supplierId 过滤）。
     * 表头不固定宽度由前端控制，这里只保证排序稳定。
     */
    @Select("""
            <script>
            SELECT s.*, c.category_name, u.real_name AS owner_name,
                   (SELECT COUNT(*) FROM supplier_certificate sc WHERE sc.supplier_id = s.id AND sc.cert_status != '已作废') AS cert_count,
                   (SELECT COUNT(*) FROM supplier_certificate sc WHERE sc.supplier_id = s.id AND sc.cert_status = '已过期') AS expiring_cert_count
            FROM supplier s
            LEFT JOIN supplier_category c ON c.id = s.category_id
            LEFT JOIN sys_user u ON u.id = s.owner_id
            WHERE s.deleted = 0
            <if test="kw != null and kw != ''">
              AND (s.supplier_name LIKE CONCAT('%', #{kw}, '%')
                   OR s.supplier_code LIKE CONCAT('%', #{kw}, '%')
                   OR s.credit_code LIKE CONCAT('%', #{kw}, '%'))
            </if>
            <if test="categoryId != null"> AND s.category_id = #{categoryId} </if>
            <if test="supplierStatus != null and supplierStatus != ''"> AND s.supplier_status = #{supplierStatus} </if>
            <if test="level != null and level != ''"> AND s.level = #{level} </if>
            <if test="riskLevel != null and riskLevel != ''"> AND s.risk_level = #{riskLevel} </if>
            <if test="certStart != null and certStart != ''"> AND s.cert_expire_date &gt;= #{certStart} </if>
            <if test="certEnd != null and certEnd != ''"> AND s.cert_expire_date &lt;= #{certEnd} </if>
            <if test="supplierId != null"> AND s.id = #{supplierId} </if>
            ORDER BY s.update_time DESC, s.id DESC
            </script>
            """)
    IPage<Supplier> selectSupplierPage(Page<Supplier> page,
                                       @Param("kw") String kw,
                                       @Param("categoryId") Long categoryId,
                                       @Param("supplierStatus") String supplierStatus,
                                       @Param("level") String level,
                                       @Param("riskLevel") String riskLevel,
                                       @Param("certStart") String certStart,
                                       @Param("certEnd") String certEnd,
                                       @Param("supplierId") Long supplierId);

    @Select("""
            SELECT s.*, c.category_name FROM supplier s
            LEFT JOIN supplier_category c ON c.id = s.category_id
            WHERE s.id = #{id} AND s.deleted = 0
            """)
    Supplier selectDetail(@Param("id") Long id);

    @Select("""
            SELECT credit_code FROM supplier WHERE credit_code = #{creditCode} AND deleted = 0 LIMIT 1
            """)
    String selectCreditCode(@Param("creditCode") String creditCode);

    @Select("""
            SELECT id FROM supplier WHERE credit_code = #{creditCode} AND deleted = 0 LIMIT 1
            """)
    Long selectCreditCodeId(@Param("creditCode") String creditCode);

    @Select("""
            SELECT s.id, s.supplier_name, s.level, s.supplier_status, s.risk_level
            FROM supplier s WHERE s.deleted = 0 ORDER BY s.supplier_name
            """)
    List<Supplier> selectOptions();

    @Select("""
            SELECT supplier_status AS name, COUNT(*) AS value FROM supplier
            WHERE deleted = 0 GROUP BY supplier_status
            """)
    List<Map<String, Object>> countByStatus();

    @Select("""
            SELECT IFNULL(level, '未评级') AS name, COUNT(*) AS value FROM supplier
            WHERE deleted = 0 GROUP BY level ORDER BY level
            """)
    List<Map<String, Object>> countByLevel();

    @Select("""
            SELECT c.category_name AS name, COUNT(*) AS value
            FROM supplier s JOIN supplier_category c ON c.id = s.category_id
            WHERE s.deleted = 0 GROUP BY c.category_name ORDER BY value DESC
            """)
    List<Map<String, Object>> countByCategory();

    @Select("""
            SELECT COUNT(*) FROM supplier WHERE deleted = 0
            """)
    long countAll();

    @Select("""
            SELECT COUNT(*) FROM supplier WHERE deleted = 0 AND supplier_status = #{status}
            """)
    long countByStatusValue(@Param("status") String status);

    @Select("""
            SELECT IFNULL(ROUND(AVG(CASE WHEN level = 'A' THEN 100 ELSE 0 END), 1), 0)
            FROM supplier WHERE deleted = 0
            """)
    Double levelARatio();
}
