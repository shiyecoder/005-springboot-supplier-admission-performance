package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.supplier.entity.PerformanceScore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PerformanceScoreMapper extends BaseMapper<PerformanceScore> {

    @Select("""
            SELECT * FROM performance_score WHERE task_id = #{taskId} ORDER BY create_time ASC
            """)
    List<PerformanceScore> selectByTask(@Param("taskId") Long taskId);

    @Select("""
            SELECT * FROM performance_score WHERE task_id = #{taskId} AND reviewer_id = #{reviewerId}
            """)
    PerformanceScore selectMine(@Param("taskId") Long taskId, @Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT ROUND(AVG(delivery_score), 1) AS deliveryRate,
                   ROUND(AVG(quality_score), 1) AS qualityRate,
                   ROUND(AVG(price_score), 1) AS priceScore,
                   ROUND(AVG(response_score), 1) AS responseScore
            FROM performance_score WHERE status = '已提交'
            """)
    Map<String, Object> avgIndicators();
}
