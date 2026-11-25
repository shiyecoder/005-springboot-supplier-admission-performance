package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.SupplierCertificate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SupplierCertificateMapper extends BaseMapper<SupplierCertificate> {

    @Select("""
            <script>
            SELECT sc.*, s.supplier_name
            FROM supplier_certificate sc
            JOIN supplier s ON s.id = sc.supplier_id
            WHERE 1 = 1
            <if test="supplierId != null"> AND sc.supplier_id = #{supplierId} </if>
            <if test="certType != null and certType != ''"> AND sc.cert_type = #{certType} </if>
            <if test="certStatus != null and certStatus != ''"> AND sc.cert_status = #{certStatus} </if>
            <if test="kw != null and kw != ''">
              AND (s.supplier_name LIKE CONCAT('%', #{kw}, '%') OR s.supplier_code LIKE CONCAT('%', #{kw}, '%'))
            </if>
            ORDER BY sc.expire_date ASC, sc.id ASC
            </script>
            """)
    IPage<SupplierCertificate> selectCertPage(Page<SupplierCertificate> page,
                                              @Param("supplierId") Long supplierId,
                                              @Param("certType") String certType,
                                              @Param("certStatus") String certStatus,
                                              @Param("kw") String kw);

    @Select("""
            SELECT sc.*, s.supplier_name FROM supplier_certificate sc
            JOIN supplier s ON s.id = sc.supplier_id
            WHERE sc.supplier_id = #{supplierId}
            ORDER BY sc.cert_type, sc.version_no DESC
            """)
    List<SupplierCertificate> selectBySupplier(@Param("supplierId") Long supplierId);

    /** 证照到期分桶：已过期 / 30 天内 / 31-90 天 / 90 天以上 */
    @Select("""
            SELECT
              SUM(CASE WHEN expire_date < CURDATE() THEN 1 ELSE 0 END) AS expired,
              SUM(CASE WHEN expire_date >= CURDATE() AND expire_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS within30,
              SUM(CASE WHEN expire_date > DATE_ADD(CURDATE(), INTERVAL 30 DAY) AND expire_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY) THEN 1 ELSE 0 END) AS within90,
              SUM(CASE WHEN expire_date > DATE_ADD(CURDATE(), INTERVAL 90 DAY) THEN 1 ELSE 0 END) AS beyond90
            FROM supplier_certificate WHERE cert_status != '已作废'
            """)
    Map<String, Object> certBucket();

    @Select("""
            SELECT COUNT(*) FROM supplier_certificate
            WHERE cert_status = '已过期'
            """)
    long countExpired();
}
