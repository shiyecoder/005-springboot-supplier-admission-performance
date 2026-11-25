package com.grad.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.supplier.entity.SupplierRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SupplierRuleMapper extends BaseMapper<SupplierRule> {

    /** 取当前生效的规则版本，计算时只用这一版 */
    @Select("""
            SELECT * FROM supplier_rule WHERE status = '启用'
            ORDER BY effect_time DESC, id DESC LIMIT 1
            """)
    SupplierRule selectActive();

    @Select("""
            SELECT * FROM supplier_rule WHERE id = #{id}
            """)
    SupplierRule selectByIdCustom(@Param("id") Long id);
}
