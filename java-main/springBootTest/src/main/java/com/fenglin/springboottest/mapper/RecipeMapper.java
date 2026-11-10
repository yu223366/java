package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

/** 炼丹炼器配方 Mapper。 */
@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
