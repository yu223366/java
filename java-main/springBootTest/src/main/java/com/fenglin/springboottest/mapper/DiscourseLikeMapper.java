package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DiscourseLike;
import org.apache.ibatis.annotations.Mapper;

/** 辩场点赞（一人一赞） Mapper。 */
@Mapper
public interface DiscourseLikeMapper extends BaseMapper<DiscourseLike> {
}
