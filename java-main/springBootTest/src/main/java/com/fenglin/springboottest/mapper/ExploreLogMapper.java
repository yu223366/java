package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.ExploreLog;
import org.apache.ibatis.annotations.Mapper;

/** 秘境探索记录 Mapper。 */
@Mapper
public interface ExploreLogMapper extends BaseMapper<ExploreLog> {
}
