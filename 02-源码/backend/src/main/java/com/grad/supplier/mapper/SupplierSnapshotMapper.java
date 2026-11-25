package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.supplier.entity.SupplierSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SupplierSnapshotMapper extends BaseMapper<SupplierSnapshot> {

    @Select("""
            <script>
            SELECT n.*, s.supplier_name FROM supplier_snapshot n
            LEFT JOIN supplier s ON s.id = n.supplier_id
            WHERE 1 = 1
            <if test="supplierId != null"> AND n.supplier_id = #{supplierId} </if>
            <if test="period != null and period != ''"> AND n.period = #{period} </if>
            <if test="level != null and level != ''"> AND n.final_level = #{level} </if>
            ORDER BY n.create_time DESC
            </script>
            """)
    IPage<SupplierSnapshot> selectSnapshotPage(Page<SupplierSnapshot> page,
                                                @Param("supplierId") Long supplierId,
                                                @Param("period") String period,
                                                @Param("level") String level);

    @Select("""
            SELECT n.*, s.supplier_name FROM supplier_snapshot n
            LEFT JOIN supplier s ON s.id = n.supplier_id
            WHERE n.supplier_id = #{supplierId}
            ORDER BY n.create_time DESC LIMIT 12
            """)
    List<SupplierSnapshot> selectRecent(@Param("supplierId") Long supplierId);

    @Select("""
            SELECT COUNT(*) FROM supplier_snapshot
            """)
    long countAll();

    @Select("""
            SELECT COUNT(*) FROM supplier_snapshot WHERE result_level = final_level
            """)
    long countAdopted();

    @Select("""
            SELECT COUNT(*) FROM supplier_snapshot WHERE result_level != final_level
            """)
    long countAdjusted();

    @Select("""
            SELECT IFNULL(final_level, result_level) AS name, COUNT(*) AS value
            FROM supplier_snapshot GROUP BY IFNULL(final_level, result_level) ORDER BY name
            """)
    List<Map<String, Object>> countByFinalLevel();
}
