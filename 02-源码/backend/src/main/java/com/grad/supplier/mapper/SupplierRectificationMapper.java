package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.SupplierRectification;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SupplierRectificationMapper extends BaseMapper<SupplierRectification> {

    @Select("""
            <script>
            SELECT r.*, s.supplier_name
            FROM supplier_rectification r
            LEFT JOIN supplier s ON s.id = r.supplier_id
            WHERE r.deleted = 0
            <if test="kw != null and kw != ''">
              AND (r.rectification_no LIKE CONCAT('%', #{kw}, '%') OR s.supplier_name LIKE CONCAT('%', #{kw}, '%'))
            </if>
            <if test="status != null and status != ''"> AND r.status = #{status} </if>
            <if test="source != null and source != ''"> AND r.source = #{source} </if>
            <if test="supplierId != null"> AND r.supplier_id = #{supplierId} </if>
            ORDER BY r.deadline ASC, r.id DESC
            </script>
            """)
    IPage<SupplierRectification> selectRectPage(Page<SupplierRectification> page,
                                                @Param("kw") String kw,
                                                @Param("status") String status,
                                                @Param("source") String source,
                                                @Param("supplierId") Long supplierId);

    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m') AS name,
                   ROUND(SUM(CASE WHEN status IN ('已关闭') THEN 1 ELSE 0 END) * 100 / COUNT(*), 1) AS value
            FROM supplier_rectification
            WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
            GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY name
            """)
    List<Map<String, Object>> monthlyCompletion();

    @Select("""
            SELECT COUNT(*) FROM supplier_rectification WHERE deleted = 0 AND status = '待验证'
            """)
    long countPendingVerify();

    @Select("""
            SELECT ROUND(SUM(CASE WHEN status = '已关闭' THEN 1 ELSE 0 END) * 100 / COUNT(*), 1)
            FROM supplier_rectification WHERE deleted = 0
            """)
    Double completionRate();
}
