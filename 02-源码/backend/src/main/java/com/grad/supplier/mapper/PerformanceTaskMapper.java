package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.PerformanceTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PerformanceTaskMapper extends BaseMapper<PerformanceTask> {

    @Select("""
            <script>
            SELECT t.*, s.supplier_name, u.real_name AS owner_name,
                   (SELECT COUNT(*) FROM performance_score ps WHERE ps.task_id = t.id AND ps.status = '已提交') AS scored_count
            FROM performance_task t
            LEFT JOIN supplier s ON s.id = t.supplier_id
            LEFT JOIN sys_user u ON u.id = t.owner_id
            WHERE t.deleted = 0
            <if test="kw != null and kw != ''">
              AND (t.task_no LIKE CONCAT('%', #{kw}, '%') OR s.supplier_name LIKE CONCAT('%', #{kw}, '%'))
            </if>
            <if test="period != null and period != ''"> AND t.period = #{period} </if>
            <if test="status != null and status != ''"> AND t.status = #{status} </if>
            <if test="supplierId != null"> AND t.supplier_id = #{supplierId} </if>
            ORDER BY t.period DESC, t.update_time DESC
            </script>
            """)
    IPage<PerformanceTask> selectTaskPage(Page<PerformanceTask> page,
                                          @Param("kw") String kw,
                                          @Param("period") String period,
                                          @Param("status") String status,
                                          @Param("supplierId") Long supplierId);

    @Select("""
            SELECT t.*, s.supplier_name FROM performance_task t
            LEFT JOIN supplier s ON s.id = t.supplier_id
            WHERE t.id = #{id}
            """)
    PerformanceTask selectDetail(@Param("id") Long id);

    @Select("""
            SELECT s.supplier_name AS name, t.avg_score AS value
            FROM performance_task t JOIN supplier s ON s.id = t.supplier_id
            WHERE t.status = '已发布' AND t.avg_score IS NOT NULL
            ORDER BY t.avg_score DESC LIMIT 5
            """)
    List<Map<String, Object>> topRank();

    @Select("""
            SELECT COUNT(*) FROM performance_task WHERE deleted = 0 AND status = '已发布'
            """)
    long countPublished();

    @Select("""
            SELECT COUNT(DISTINCT supplier_id) FROM performance_task WHERE deleted = 0 AND status = '已发布'
            """)
    long countCoveredSupplier();
}
