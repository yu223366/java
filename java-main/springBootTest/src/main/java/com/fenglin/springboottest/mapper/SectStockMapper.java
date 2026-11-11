package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SectStock;
import org.apache.ibatis.annotations.Mapper;

/** 宗门宝库库存 Mapper。 */
@Mapper
public interface SectStockMapper extends BaseMapper<SectStock> {
}
