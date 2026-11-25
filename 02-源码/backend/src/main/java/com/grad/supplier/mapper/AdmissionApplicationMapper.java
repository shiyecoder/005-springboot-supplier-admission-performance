package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.AdmissionApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface AdmissionApplicationMapper extends BaseMapper<AdmissionApplication> {

    /**
     * 准入列表：默认待本人处理优先，其次更新时间倒序。
     * cert_uploaded 统计已上传证照数，cert_required 由 service 按类目必填项填充。
     */
    @Select("""
            <script>
            SELECT a.*, c.category_name, u.real_name AS reviewer_name, s.cover_image,
                   (SELECT COUNT(*) FROM supplier_certificate sc
                     WHERE sc.supplier_id = a.supplier_id AND sc.cert_status != '已作废') AS cert_uploaded,
                   (SELECT COUNT(*) FROM review_score rs WHERE rs.application_id = a.id) AS score_count,
                   (SELECT ROUND(AVG(rs.total_score), 1) FROM review_score rs
                     WHERE rs.application_id = a.id AND rs.status = '已提交') AS avg_score
            FROM admission_application a
            LEFT JOIN supplier_category c ON c.id = a.category_id
            LEFT JOIN sys_user u ON u.id = a.reviewer_id
            LEFT JOIN supplier s ON s.id = a.supplier_id
            WHERE a.deleted = 0
            <if test="kw != null and kw != ''">
              AND (a.application_no LIKE CONCAT('%', #{kw}, '%') OR a.supplier_name LIKE CONCAT('%', #{kw}, '%')
                   OR a.credit_code LIKE CONCAT('%', #{kw}, '%'))
            </if>
            <if test="categoryId != null"> AND a.category_id = #{categoryId} </if>
            <if test="status != null and status != ''"> AND a.status = #{status} </if>
            <if test="tab == 'todo'"> AND a.status IN ('准入审核','现场评估','待审批') </if>
            <if test="tab == 'doing'"> AND a.status IN ('准入审核','现场评估') </if>
            <if test="tab == 'done'"> AND a.status IN ('已准入','合作评价','分级调整') </if>
            <if test="tab == 'abnormal'"> AND a.status = '已驳回' </if>
            <if test="supplierId != null"> AND a.supplier_id = #{supplierId} </if>
            ORDER BY (CASE WHEN a.status IN ('准入审核','现场评估','待审批') THEN 0 ELSE 1 END), a.update_time DESC
            </script>
            """)
    IPage<AdmissionApplication> selectApplicationPage(Page<AdmissionApplication> page,
                                                      @Param("kw") String kw,
                                                      @Param("categoryId") Long categoryId,
                                                      @Param("status") String status,
                                                      @Param("tab") String tab,
                                                      @Param("supplierId") Long supplierId);

    @Select("""
            SELECT a.*, c.category_name, s.cover_image
            FROM admission_application a
            LEFT JOIN supplier_category c ON c.id = a.category_id
            LEFT JOIN supplier s ON s.id = a.supplier_id
            WHERE a.id = #{id} AND a.deleted = 0
            """)
    AdmissionApplication selectDetail(@Param("id") Long id);

    @Select("""
            SELECT DATE_FORMAT(submit_time, '%Y-%m') AS name, COUNT(*) AS value
            FROM admission_application
            WHERE submit_time IS NOT NULL AND submit_time >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
            GROUP BY DATE_FORMAT(submit_time, '%Y-%m') ORDER BY name
            """)
    List<Map<String, Object>> monthlyTrend();

    @Select("""
            SELECT DATE_FORMAT(submit_time, '%Y-%m') AS name, COUNT(*) AS value
            FROM admission_application
            WHERE submit_time IS NOT NULL AND status = '已准入'
              AND submit_time >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
            GROUP BY DATE_FORMAT(submit_time, '%Y-%m') ORDER BY name
            """)
    List<Map<String, Object>> monthlyApproved();

    @Select("""
            SELECT COUNT(*) FROM admission_application WHERE deleted = 0
            """)
    long countAll();

    @Select("""
            SELECT COUNT(*) FROM admission_application
            WHERE deleted = 0 AND status IN ('已准入','合作评价','分级调整')
            """)
    long countApproved();
}
