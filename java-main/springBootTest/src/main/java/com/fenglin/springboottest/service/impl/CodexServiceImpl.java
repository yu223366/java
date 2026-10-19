package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.controller.dto.*;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.CodexService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 藏经阁业务实现：条目（功法/心得/修炼资源，查 wushan_item）+ 灵根 + 秘境。
 * 分类支持分级下钻：传一级分类（如 gongfa 功法）自动包含其全部二级子类（心法/身法/术法/阵法），
 * 与首页搜索、市集筛选口径一致。
 */
@Service
public class CodexServiceImpl implements CodexService {

    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final GradeMapper gradeMapper;
    private final SpiritRootMapper spiritRootMapper;
    private final RealmMapper realmMapper;

    public CodexServiceImpl(ItemMapper itemMapper,
                            CategoryMapper categoryMapper,
                            GradeMapper gradeMapper,
                            SpiritRootMapper spiritRootMapper,
                            RealmMapper realmMapper) {
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.gradeMapper = gradeMapper;
        this.spiritRootMapper = spiritRootMapper;
        this.realmMapper = realmMapper;
    }

    /* ------------------------------ 条目（功法 / 心得 / 修炼资源） ------------------------------ */

    @Override
    public PageVo<ItemVo> items(String element, String category, String grade, String q) {
        String kw = blankToNull(q);
        List<Long> categoryIds = resolveCategoryIds(category);
        String gradeKey = normalize(grade);

        List<Item> rows = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, 1)
                .eq(isFilter(element), Item::getElement, normalize(element))
                .in(categoryIds != null && !categoryIds.isEmpty(), Item::getCategoryId, categoryIds)
                .eq(gradeKey != null, Item::getGradeKey, gradeKey)
                .and(kw != null, w -> w.like(Item::getTitle, kw).or().like(Item::getSummary, kw))
                .orderByAsc(Item::getSort)
                .orderByAsc(Item::getId));

        Map<Long, Category> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        Map<String, Grade> gradeMap = gradeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Grade::getGradeKey, g -> g));

        List<ItemVo> items = rows.stream()
                .map(it -> toItemVo(it, catMap, gradeMap))
                .collect(Collectors.toList());
        return PageVo.of(items.size(), 1, items.size(), items);
    }

    /* ------------------------------ 灵根 ------------------------------ */

    @Override
    public PageVo<SpiritRootVo> spiritRoots(String element, String grade) {
        String gradeKey = normalize(grade);
        List<SpiritRoot> rows = spiritRootMapper.selectList(Wrappers.<SpiritRoot>lambdaQuery()
                .eq(SpiritRoot::getStatus, 1)
                .eq(isFilter(element), SpiritRoot::getElement, normalize(element))
                .eq(gradeKey != null, SpiritRoot::getGradeKey, gradeKey)
                .orderByAsc(SpiritRoot::getSort)
                .orderByAsc(SpiritRoot::getId));

        Map<String, Grade> gradeMap = gradeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Grade::getGradeKey, g -> g));

        List<SpiritRootVo> items = rows.stream().map(r -> {
            SpiritRootVo vo = new SpiritRootVo();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setElement(r.getElement());
            vo.setGrade(r.getGradeKey());
            Grade g = gradeMap.get(r.getGradeKey());
            if (g != null) {
                vo.setGradeLabel(g.getName());
            }
            vo.setDesc(r.getSummary());
            return vo;
        }).collect(Collectors.toList());
        return PageVo.of(items.size(), 1, items.size(), items);
    }

    /* ------------------------------ 秘境 ------------------------------ */

    @Override
    public PageVo<RealmVo> realms(String element, String grade) {
        String gradeKey = normalize(grade);
        List<Realm> rows = realmMapper.selectList(Wrappers.<Realm>lambdaQuery()
                .eq(Realm::getStatus, 1)
                .eq(isFilter(element), Realm::getElement, normalize(element))
                .eq(gradeKey != null, Realm::getGradeKey, gradeKey)
                .orderByAsc(Realm::getSort)
                .orderByAsc(Realm::getId));

        Map<String, Grade> gradeMap = gradeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Grade::getGradeKey, g -> g));

        List<RealmVo> items = rows.stream().map(r -> {
            RealmVo vo = new RealmVo();
            vo.setId(r.getId());
            vo.setName(r.getName());
            vo.setElement(r.getElement());
            vo.setGrade(r.getGradeKey());
            Grade g = gradeMap.get(r.getGradeKey());
            if (g != null) {
                vo.setGradeLabel(g.getName());
            }
            vo.setLayers(r.getLayers());
            vo.setDesc(r.getSummary());
            return vo;
        }).collect(Collectors.toList());
        return PageVo.of(items.size(), 1, items.size(), items);
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    /** 分类 key → id 列表；传父类（如 gongfa）自动包含其子类；all/空 返回 null（不过滤） */
    private List<Long> resolveCategoryIds(String categoryKey) {
        String key = blankToNull(categoryKey);
        if (key == null || "all".equals(key)) {
            return null;
        }
        Category cat = categoryMapper.selectOne(
                Wrappers.<Category>lambdaQuery().eq(Category::getCatKey, key));
        if (cat == null) {
            // 传了不存在的分类 → 明确返回空集（必然无结果）
            return Collections.singletonList(-1L);
        }
        List<Long> ids = new ArrayList<>();
        ids.add(cat.getId());
        // 一级分类下钻二级
        List<Category> children = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, cat.getId()));
        children.forEach(c -> ids.add(c.getId()));
        return ids;
    }

    private ItemVo toItemVo(Item it, Map<Long, Category> catMap, Map<String, Grade> gradeMap) {
        ItemVo vo = new ItemVo();
        vo.setId(it.getId());
        vo.setTitle(it.getTitle());
        vo.setDesc(it.getSummary());
        vo.setElement(it.getElement());
        Category cat = catMap.get(it.getCategoryId());
        if (cat != null) {
            vo.setCategory(cat.getCatKey());
            vo.setCategoryLabel(cat.getName());
        }
        Grade g = gradeMap.get(it.getGradeKey());
        if (g != null) {
            vo.setGrade(g.getGradeKey());
            vo.setGradeLabel(g.getName());
        }
        return vo;
    }

    /** all / 空 → null（不过滤） */
    private static String normalize(String s) {
        if (s == null || s.isBlank() || "all".equals(s.trim())) {
            return null;
        }
        return s.trim();
    }

    private static boolean isFilter(String s) {
        return normalize(s) != null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
