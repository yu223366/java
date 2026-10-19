package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Grade;
import org.apache.ibatis.annotations.Mapper;

/** 品阶 Mapper。 */
@Mapper
public interface GradeMapper extends BaseMapper<Grade> {
}
