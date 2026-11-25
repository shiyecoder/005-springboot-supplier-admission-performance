package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.supplier.entity.ReviewScore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ReviewScoreMapper extends BaseMapper<ReviewScore> {

    @Select("""
            SELECT * FROM review_score WHERE application_id = #{applicationId}
            ORDER BY create_time ASC
            """)
    List<ReviewScore> selectByApplication(@Param("applicationId") Long applicationId);

    @Select("""
            SELECT COUNT(*) FROM review_score
            WHERE application_id = #{applicationId} AND status = '已提交'
            """)
    int countSubmitted(@Param("applicationId") Long applicationId);

    @Select("""
            SELECT COUNT(*) FROM review_score
            WHERE application_id = #{applicationId} AND reviewer_id = #{reviewerId} AND status = '已提交'
            """)
    int countMySubmitted(@Param("applicationId") Long applicationId, @Param("reviewerId") Long reviewerId);
}
