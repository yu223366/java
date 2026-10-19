package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 分类树节点 VO：{ key, label, en, children: [...] }。
 * children 为空列表表示叶子分类。
 */
@Data
public class CategoryVo {

    private String key;

    private String label;

    private String en;

    private List<CategoryVo> children;
}
