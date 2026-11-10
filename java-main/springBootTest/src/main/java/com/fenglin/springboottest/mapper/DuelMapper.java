package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Duel;
import org.apache.ibatis.annotations.Mapper;

/** 五行问对决 Mapper。 */
@Mapper
public interface DuelMapper extends BaseMapper<Duel> {
}
