package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.LoginRequest;
import com.fenglin.springboottest.controller.dto.LoginVo;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 后台管理（执事）控制器，对接前端 src/api/admin.js 的接口契约。
 * 所有接口前缀 /api/admin（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 权限口径：
 *  - 只有 wushan_user.role = 0（管理员 / 执事）可登录后台、可操作宝库数据；
 *  - role = 1（普通山友，注册默认）登录后台会被拒绝「此处只通执事」。
 *  - 宝库各接口均校验 Authorization: Bearer <token> 中的执事身份。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SpiritRootMapper spiritRootMapper;
    private final RealmMapper realmMapper;
    private final CategoryMapper categoryMapper;

    public AdminController(UserMapper userMapper,
                           ItemMapper itemMapper,
                           SpiritRootMapper spiritRootMapper,
                           RealmMapper realmMapper,
                           CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.itemMapper = itemMapper;
        this.spiritRootMapper = spiritRootMapper;
        this.realmMapper = realmMapper;
        this.categoryMapper = categoryMapper;
    }

    /* ------------------------------ 执事登录 / 退出 ------------------------------ */

    /**
     * 执事登录：与普通登录同一套山籍校验，但额外要求 role=0。
     * role=1 的山友（含注册新号）一律拒绝，文案与前端 mock 保持一致。
     */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();
        if (username.isEmpty() || password.isEmpty()) {
            throw new BizException("执事名号与暗语，皆不可空。");
        }
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null) {
            throw new BizException("查无此山籍");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("山籍已被封禁");
        }
        // 明文比对（按需求不加密）
        if (!password.equals(user.getPassword())) {
            throw new BizException("暗语有误");
        }
        // 权限闸门：只有管理员（role=0）可入后台
        Integer role = user.getRole() == null ? 1 : user.getRole();
        if (role != 0) {
            throw new BizException("此处只通执事，山友请回前山。");
        }
        LoginVo vo = new LoginVo();
        vo.setToken(TokenUtil.generate(user.getId(), user.getUsername()));
        vo.setUsername(user.getUsername());
        vo.setRole(0);
        return Result.ok(vo);
    }

    /** 退出执事：本地无状态令牌，前端清会话即可，预留保持契约完整 */
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout() {
        Map<String, Object> data = new HashMap<>(2);
        data.put("ok", true);
        return Result.ok(data);
    }

    /* ------------------------------ 宝库管理（三库） ------------------------------ */
    // kind 约定与前端一致：items（条目）| roots（灵根）| realms（秘境）

    /** 宝库列表：element / grade / q 筛选，仅返回上架（status=1）数据 */
    @GetMapping("/treasury/{kind}")
    public Result<Map<String, Object>> list(@PathVariable("kind") String kind,
                                            @RequestParam(value = "element", required = false) String element,
                                            @RequestParam(value = "grade", required = false) String grade,
                                            @RequestParam(value = "q", required = false) String q,
                                            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        String el = normalize(element);
        String gk = normalize(grade);
        String kw = blankToNull(q);
        List<Map<String, Object>> items;
        switch (kind) {
            case "items": {
                List<Item> rows = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                        .eq(Item::getStatus, 1)
                        .eq(el != null, Item::getElement, el)
                        .eq(gk != null, Item::getGradeKey, gk)
                        .and(kw != null, w -> w.like(Item::getTitle, kw).or().like(Item::getSummary, kw))
                        .orderByAsc(Item::getSort).orderByAsc(Item::getId));
                Map<Long, String> catKeys = categoryMapper.selectList(null).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c.getCatKey() == null ? "" : c.getCatKey()));
                items = rows.stream().map(it -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(it.getId()));
                    m.put("title", it.getTitle());
                    m.put("category", catKeys.getOrDefault(it.getCategoryId(), ""));
                    m.put("element", it.getElement());
                    m.put("grade", it.getGradeKey());
                    m.put("desc", it.getSummary());
                    return m;
                }).collect(Collectors.toList());
                break;
            }
            case "roots": {
                List<SpiritRoot> rows = spiritRootMapper.selectList(Wrappers.<SpiritRoot>lambdaQuery()
                        .eq(SpiritRoot::getStatus, 1)
                        .eq(el != null, SpiritRoot::getElement, el)
                        .eq(gk != null, SpiritRoot::getGradeKey, gk)
                        .and(kw != null, w -> w.like(SpiritRoot::getName, kw).or().like(SpiritRoot::getSummary, kw))
                        .orderByAsc(SpiritRoot::getSort).orderByAsc(SpiritRoot::getId));
                items = rows.stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(r.getId()));
                    m.put("name", r.getName());
                    m.put("element", r.getElement());
                    m.put("grade", r.getGradeKey());
                    m.put("desc", r.getSummary());
                    return m;
                }).collect(Collectors.toList());
                break;
            }
            case "realms": {
                List<Realm> rows = realmMapper.selectList(Wrappers.<Realm>lambdaQuery()
                        .eq(Realm::getStatus, 1)
                        .eq(el != null, Realm::getElement, el)
                        .eq(gk != null, Realm::getGradeKey, gk)
                        .and(kw != null, w -> w.like(Realm::getName, kw).or().like(Realm::getSummary, kw))
                        .orderByAsc(Realm::getSort).orderByAsc(Realm::getId));
                items = rows.stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(r.getId()));
                    m.put("name", r.getName());
                    m.put("layers", r.getLayers());
                    m.put("element", r.getElement());
                    m.put("grade", r.getGradeKey());
                    m.put("desc", r.getSummary());
                    return m;
                }).collect(Collectors.toList());
                break;
            }
            default:
                throw new BizException("未知宝库集合：" + kind);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", items.size());
        data.put("items", items);
        return Result.ok(data);
    }

    /** 新录一条：payload 字段与前端契约一致（title/name、category(key)、element、grade、desc、layers） */
    @PostMapping("/treasury/{kind}")
    public Result<Map<String, Object>> create(@PathVariable("kind") String kind,
                                              @RequestBody Map<String, Object> payload,
                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        Map<String, Object> saved;
        switch (kind) {
            case "items": {
                Item it = new Item();
                it.setTitle(str(payload.get("title")));
                it.setSummary(str(payload.get("desc")));
                it.setElement(str(payload.get("element")));
                it.setGradeKey(str(payload.get("grade")));
                it.setCategoryId(resolveCategoryId(str(payload.get("category"))));
                it.setStatus(1);
                itemMapper.insert(it);
                saved = itemVo(it);
                break;
            }
            case "roots": {
                SpiritRoot r = new SpiritRoot();
                r.setName(str(payload.get("name")));
                r.setSummary(str(payload.get("desc")));
                r.setElement(str(payload.get("element")));
                r.setGradeKey(str(payload.get("grade")));
                r.setStatus(1);
                spiritRootMapper.insert(r);
                saved = rootVo(r);
                break;
            }
            case "realms": {
                Realm r = new Realm();
                r.setName(str(payload.get("name")));
                r.setLayers(str(payload.get("layers")));
                r.setSummary(str(payload.get("desc")));
                r.setElement(str(payload.get("element")));
                r.setGradeKey(str(payload.get("grade")));
                r.setStatus(1);
                realmMapper.insert(r);
                saved = realmVo(r);
                break;
            }
            default:
                throw new BizException("未知宝库集合：" + kind);
        }
        return Result.ok(saved);
    }

    /** 修订一条：按 id 覆盖可变字段 */
    @PutMapping("/treasury/{kind}/{id}")
    public Result<Map<String, Object>> update(@PathVariable("kind") String kind,
                                              @PathVariable("id") Long id,
                                              @RequestBody Map<String, Object> payload,
                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        Map<String, Object> saved;
        switch (kind) {
            case "items": {
                Item it = mustItem(id);
                if (payload.containsKey("title")) it.setTitle(str(payload.get("title")));
                if (payload.containsKey("desc")) it.setSummary(str(payload.get("desc")));
                if (payload.containsKey("element")) it.setElement(str(payload.get("element")));
                if (payload.containsKey("grade")) it.setGradeKey(str(payload.get("grade")));
                if (payload.containsKey("category")) it.setCategoryId(resolveCategoryId(str(payload.get("category"))));
                itemMapper.updateById(it);
                saved = itemVo(it);
                break;
            }
            case "roots": {
                SpiritRoot r = mustRoot(id);
                if (payload.containsKey("name")) r.setName(str(payload.get("name")));
                if (payload.containsKey("desc")) r.setSummary(str(payload.get("desc")));
                if (payload.containsKey("element")) r.setElement(str(payload.get("element")));
                if (payload.containsKey("grade")) r.setGradeKey(str(payload.get("grade")));
                spiritRootMapper.updateById(r);
                saved = rootVo(r);
                break;
            }
            case "realms": {
                Realm r = mustRealm(id);
                if (payload.containsKey("name")) r.setName(str(payload.get("name")));
                if (payload.containsKey("layers")) r.setLayers(str(payload.get("layers")));
                if (payload.containsKey("desc")) r.setSummary(str(payload.get("desc")));
                if (payload.containsKey("element")) r.setElement(str(payload.get("element")));
                if (payload.containsKey("grade")) r.setGradeKey(str(payload.get("grade")));
                realmMapper.updateById(r);
                saved = realmVo(r);
                break;
            }
            default:
                throw new BizException("未知宝库集合：" + kind);
        }
        return Result.ok(saved);
    }

    /** 出库（删除）一条 */
    @DeleteMapping("/treasury/{kind}/{id}")
    public Result<Map<String, Object>> remove(@PathVariable("kind") String kind,
                                              @PathVariable("id") Long id,
                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        switch (kind) {
            case "items":
                mustItem(id);
                itemMapper.deleteById(id);
                break;
            case "roots":
                mustRoot(id);
                spiritRootMapper.deleteById(id);
                break;
            case "realms":
                mustRealm(id);
                realmMapper.deleteById(id);
                break;
            default:
                throw new BizException("未知宝库集合：" + kind);
        }
        Map<String, Object> data = new HashMap<>(2);
        data.put("ok", true);
        return Result.ok(data);
    }

    /** 重置本库：清空该集合全部数据（谨慎使用，与前端 mock 的 resetKind 对应） */
    @PostMapping("/treasury/{kind}/reset")
    public Result<Map<String, Object>> reset(@PathVariable("kind") String kind,
                                             @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        switch (kind) {
            case "items":
                itemMapper.delete(Wrappers.<Item>lambdaQuery().ge(Item::getId, 0));
                break;
            case "roots":
                spiritRootMapper.delete(Wrappers.<SpiritRoot>lambdaQuery().ge(SpiritRoot::getId, 0));
                break;
            case "realms":
                realmMapper.delete(Wrappers.<Realm>lambdaQuery().ge(Realm::getId, 0));
                break;
            default:
                throw new BizException("未知宝库集合：" + kind);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", 0);
        data.put("items", new ArrayList<>());
        return Result.ok(data);
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    /** 执事身份校验：解析 Bearer 令牌 → 查山籍 → 必须 role=0 */
    private void requireAdmin(String auth) {
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        Object[] parsed = TokenUtil.parse(token);
        if (parsed == null) {
            throw new BizException(401, "未持执事令牌，请先入内。");
        }
        String username = String.valueOf(parsed[1]);
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user == null || user.getStatus() == null || user.getStatus() == 0
                || user.getRole() == null || user.getRole() != 0) {
            throw new BizException(403, "此处只通执事，山友请回前山。");
        }
    }

    private Item mustItem(Long id) {
        Item it = itemMapper.selectById(id);
        if (it == null) throw new BizException("条目已不在库中，可能已被移除。");
        return it;
    }

    private SpiritRoot mustRoot(Long id) {
        SpiritRoot r = spiritRootMapper.selectById(id);
        if (r == null) throw new BizException("灵根已不在库中，可能已被移除。");
        return r;
    }

    private Realm mustRealm(Long id) {
        Realm r = realmMapper.selectById(id);
        if (r == null) throw new BizException("秘境已不在库中，可能已被移除。");
        return r;
    }

    /** 分类 key（如 gongfa）→ 分类 id；空 / 未命中返回 null */
    private Long resolveCategoryId(String catKey) {
        String key = blankToNull(catKey);
        if (key == null) return null;
        Category cat = categoryMapper.selectOne(
                Wrappers.<Category>lambdaQuery().eq(Category::getCatKey, key));
        return cat == null ? null : cat.getId();
    }

    private String categoryKey(Long categoryId) {
        if (categoryId == null) return "";
        Category cat = categoryMapper.selectById(categoryId);
        return cat == null || cat.getCatKey() == null ? "" : cat.getCatKey();
    }

    private Map<String, Object> itemVo(Item it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(it.getId()));
        m.put("title", it.getTitle());
        m.put("category", categoryKey(it.getCategoryId()));
        m.put("element", it.getElement());
        m.put("grade", it.getGradeKey());
        m.put("desc", it.getSummary());
        return m;
    }

    private Map<String, Object> rootVo(SpiritRoot r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(r.getId()));
        m.put("name", r.getName());
        m.put("element", r.getElement());
        m.put("grade", r.getGradeKey());
        m.put("desc", r.getSummary());
        return m;
    }

    private Map<String, Object> realmVo(Realm r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(r.getId()));
        m.put("name", r.getName());
        m.put("layers", r.getLayers());
        m.put("element", r.getElement());
        m.put("grade", r.getGradeKey());
        m.put("desc", r.getSummary());
        return m;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank() || "all".equals(s.trim())) ? null : s.trim();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
