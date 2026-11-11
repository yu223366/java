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
    private final PracticeMapper practiceMapper;
    private final SectMemberMapper sectMemberMapper;
    private final SectMapper sectMapper;
    private final ConsignMapper consignMapper;

    public AdminController(UserMapper userMapper,
                           ItemMapper itemMapper,
                           SpiritRootMapper spiritRootMapper,
                           RealmMapper realmMapper,
                           CategoryMapper categoryMapper,
                           PracticeMapper practiceMapper,
                           SectMemberMapper sectMemberMapper,
                           SectMapper sectMapper,
                           ConsignMapper consignMapper) {
        this.userMapper = userMapper;
        this.itemMapper = itemMapper;
        this.spiritRootMapper = spiritRootMapper;
        this.realmMapper = realmMapper;
        this.categoryMapper = categoryMapper;
        this.practiceMapper = practiceMapper;
        this.sectMemberMapper = sectMemberMapper;
        this.sectMapper = sectMapper;
        this.consignMapper = consignMapper;
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

    /* ------------------------------ 山友管理（用户系统） ------------------------------ */

    /**
     * 山友列表：名号检索 + 身份(role) + 状态(status) 筛选 + 分页。
     * status：normal(正常) / banned(封禁) / deleted(已注销) / all(全部含注销)。
     * 默认仅看未注销（deleted=0），勾选 includeDeleted=true 才包含已注销。
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "includeDeleted", required = false) Boolean includeDeleted,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "12") Integer size,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        String kw = blankToNull(q);
        Integer roleFilter = null;
        if ("0".equals(role) || "admin".equals(role)) roleFilter = 0;
        else if ("1".equals(role) || "user".equals(role)) roleFilter = 1;

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> w = Wrappers.lambdaQuery();
        if (kw != null) w.like(User::getUsername, kw);
        if (roleFilter != null) w.eq(User::getRole, roleFilter);
        if (includeDeleted == null || !includeDeleted) w.eq(User::getDeleted, 0);
        if ("banned".equals(status)) w.eq(User::getStatus, 0);
        else if ("normal".equals(status)) w.eq(User::getStatus, 1).eq(User::getDeleted, 0);
        else if ("deleted".equals(status)) w.eq(User::getDeleted, 1);
        w.orderByDesc(User::getId);

        long total = userMapper.selectCount(w);
        int pg = Math.max(1, page == null ? 1 : page);
        int sz = Math.min(100, Math.max(1, size == null ? 12 : size));
        int offset = (pg - 1) * sz;
        List<User> rows = userMapper.selectList(w.last("LIMIT " + offset + "," + sz));
        List<Map<String, Object>> items = rows.stream().map(this::userListVo).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", pg);
        data.put("size", sz);
        data.put("items", items);
        return Result.ok(data);
    }

    /** 山友详情：基础 + 灵石 + 修为 + 宗门 + 经营统计 */
    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> userDetail(@PathVariable("id") Long id,
                                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        User u = resolveUser(id);
        return Result.ok(userDetailVo(u));
    }

    /** 封禁：status=0。自保护：执事不可封禁自身。 */
    @PostMapping("/users/{id}/ban")
    public Result<Map<String, Object>> banUser(@PathVariable("id") Long id,
                                               @RequestHeader(value = "Authorization", required = false) String auth) {
        User admin = requireAdminUser(auth);
        if (admin.getId().equals(id)) throw new BizException("执事不可封禁自身。");
        resolveUser(id);
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id).set(User::getStatus, 0));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("status", 0);
        m.put("statusLabel", "封禁");
        return Result.ok(m);
    }

    /** 解封：status=1（已注销者需先恢复）。 */
    @PostMapping("/users/{id}/unban")
    public Result<Map<String, Object>> unbanUser(@PathVariable("id") Long id,
                                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        User u = resolveUser(id);
        if (u.getDeleted() != null && u.getDeleted() == 1) {
            throw new BizException("该山籍已注销，请先恢复再解封。");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id).set(User::getStatus, 1));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("status", 1);
        m.put("statusLabel", "正常");
        return Result.ok(m);
    }

    /** 重置暗语：默认 123456，可带 password 指定（4~32 位）。 */
    @PostMapping("/users/{id}/reset-password")
    public Result<Map<String, Object>> resetPassword(@PathVariable("id") Long id,
                                                     @RequestBody(required = false) Map<String, Object> payload,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        resolveUser(id);
        String np = payload == null ? null : str(payload.get("password"));
        if (np == null || np.isEmpty()) np = "123456";
        if (np.length() < 4 || np.length() > 32) throw new BizException("暗语长度须 4~32 位。");
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id).set(User::getPassword, np));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("password", np);
        return Result.ok(m);
    }

    /**
     * 调整资源：灵石 / 修为 / 境界 增减。
     * spirit、exp 为增减量（可负）；level 为境界重数增减量（不低于 1）。
     * 灵石与修为用 SQL 算术直接落库（GREATEST 兜底非负），避免读改写竞态。
     */
    @PostMapping("/users/{id}/adjust")
    public Result<Map<String, Object>> adjust(@PathVariable("id") Long id,
                                             @RequestBody Map<String, Object> payload,
                                             @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        resolveUser(id);
        if (payload.containsKey("spirit") && payload.get("spirit") != null) {
            int d = asInt(payload.get("spirit"), "灵石");
            userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id)
                    .setSql("spirit = GREATEST(0, IFNULL(spirit,0) + " + d + ")"));
        }
        Practice p = practiceMapper.selectOne(Wrappers.<Practice>lambdaQuery().eq(Practice::getUserId, id));
        if ((payload.containsKey("exp") && payload.get("exp") != null)
                || (payload.containsKey("level") && payload.get("level") != null)) {
            if (p == null) {
                p = new Practice();
                p.setUserId(id);
                p.setLevel(1);
                p.setExp(0);
                p.setTotalCount(0);
                p.setTotalSpirit(0);
                practiceMapper.insert(p);
            }
            if (payload.containsKey("exp") && payload.get("exp") != null) {
                int d = asInt(payload.get("exp"), "修为");
                practiceMapper.update(null, Wrappers.<Practice>lambdaUpdate().eq(Practice::getUserId, id)
                        .setSql("exp = GREATEST(0, IFNULL(exp,0) + " + d + ")"));
            }
            if (payload.containsKey("level") && payload.get("level") != null) {
                int d = asInt(payload.get("level"), "境界");
                practiceMapper.update(null, Wrappers.<Practice>lambdaUpdate().eq(Practice::getUserId, id)
                        .setSql("level = GREATEST(1, IFNULL(level,1) + " + d + ")"));
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        User nu = userMapper.selectById(id);
        m.put("spirit", nu.getSpirit() == null ? 0 : nu.getSpirit());
        Practice np = practiceMapper.selectOne(Wrappers.<Practice>lambdaQuery().eq(Practice::getUserId, id));
        m.put("practiceLevel", np == null ? 1 : (np.getLevel() == null ? 1 : np.getLevel()));
        m.put("exp", np == null ? 0 : (np.getExp() == null ? 0 : np.getExp()));
        return Result.ok(m);
    }

    /** 升降执事：role 0=执事 1=山友。自保护：不可改自身；不可罢免最后一位执事。 */
    @PostMapping("/users/{id}/role")
    public Result<Map<String, Object>> setRole(@PathVariable("id") Long id,
                                              @RequestBody Map<String, Object> payload,
                                              @RequestHeader(value = "Authorization", required = false) String auth) {
        User admin = requireAdminUser(auth);
        if (admin.getId().equals(id)) throw new BizException("执事不可更改自身身份。");
        User u = resolveUser(id);
        int newRole = asInt(payload.get("role"), "身份");
        if (newRole != 0 && newRole != 1) throw new BizException("身份仅可为 0(执事) 或 1(山友)。");
        if (u.getRole() != null && u.getRole() == 0 && newRole == 1) {
            long adminCount = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getRole, 0).eq(User::getDeleted, 0));
            if (adminCount <= 1) throw new BizException("不可罢免最后一位执事。");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id).set(User::getRole, newRole));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("role", newRole);
        m.put("roleLabel", newRole == 0 ? "执事" : "山友");
        return Result.ok(m);
    }

    /** 注销山籍（软注销）：deleted=1 且封禁，保留外键依赖数据，可经 /restore 恢复。 */
    @DeleteMapping("/users/{id}")
    public Result<Map<String, Object>> deleteUser(@PathVariable("id") Long id,
                                                 @RequestHeader(value = "Authorization", required = false) String auth) {
        User admin = requireAdminUser(auth);
        if (admin.getId().equals(id)) throw new BizException("执事不可注销自身山籍。");
        User u = resolveUser(id);
        if (u.getRole() != null && u.getRole() == 0) {
            throw new BizException("执事不可注销，请先降为山友。");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id)
                .set(User::getDeleted, 1).set(User::getStatus, 0));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("deleted", 1);
        m.put("status", 0);
        return Result.ok(m);
    }

    /** 恢复山籍：deleted=0 且 status=1。 */
    @PostMapping("/users/{id}/restore")
    public Result<Map<String, Object>> restoreUser(@PathVariable("id") Long id,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        User u = resolveUser(id);
        if (u.getDeleted() == null || u.getDeleted() != 1) {
            throw new BizException("该山籍并未注销。");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate().eq(User::getId, id)
                .set(User::getDeleted, 0).set(User::getStatus, 1));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("id", id);
        m.put("deleted", 0);
        m.put("status", 1);
        return Result.ok(m);
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

    /** 解析令牌并校验执事身份，返回执事实体（含 id 用于自保护） */
    private User requireAdminUser(String auth) {
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        Object[] parsed = TokenUtil.parse(token);
        if (parsed == null) {
            throw new BizException(401, "未持执事令牌，请先入内。");
        }
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, String.valueOf(parsed[1])));
        if (user == null || user.getStatus() == null || user.getStatus() == 0
                || user.getRole() == null || user.getRole() != 0) {
            throw new BizException(403, "此处只通执事，山友请回前山。");
        }
        return user;
    }

    private User resolveUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException("查无此山籍。");
        return u;
    }

    private static String roleLabel(Integer role) {
        return (role != null && role == 0) ? "执事" : "山友";
    }

    private static String statusLabelOf(User u) {
        if (u.getDeleted() != null && u.getDeleted() == 1) return "已注销";
        if (u.getStatus() == null || u.getStatus() == 0) return "封禁";
        return "正常";
    }

    private Map<String, Object> userListVo(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("element", u.getElement());
        m.put("elementLabel", u.getElementLabel());
        m.put("role", u.getRole() == null ? 1 : u.getRole());
        m.put("roleLabel", roleLabel(u.getRole()));
        m.put("status", u.getStatus() == null ? 1 : u.getStatus());
        m.put("statusLabel", statusLabelOf(u));
        m.put("deleted", u.getDeleted() == null ? 0 : u.getDeleted());
        m.put("spirit", u.getSpirit() == null ? 0 : u.getSpirit());
        Practice p = practiceMapper.selectOne(Wrappers.<Practice>lambdaQuery().eq(Practice::getUserId, u.getId()));
        m.put("practiceLevel", p == null ? 1 : (p.getLevel() == null ? 1 : p.getLevel()));
        m.put("exp", p == null ? 0 : (p.getExp() == null ? 0 : p.getExp()));
        SectMember sm = sectMemberMapper.selectOne(Wrappers.<SectMember>lambdaQuery().eq(SectMember::getUserId, u.getId()));
        String sectName = "";
        if (sm != null && sm.getSectId() != null) {
            Sect sect = sectMapper.selectById(sm.getSectId());
            if (sect != null && sect.getName() != null) sectName = sect.getName();
        }
        m.put("sectName", sectName);
        m.put("createTime", u.getCreateTime() == null ? "" : u.getCreateTime().toString());
        return m;
    }

    private Map<String, Object> userDetailVo(User u) {
        Map<String, Object> m = userListVo(u);
        Practice p = practiceMapper.selectOne(Wrappers.<Practice>lambdaQuery().eq(Practice::getUserId, u.getId()));
        Map<String, Object> practice = new LinkedHashMap<>();
        practice.put("level", p == null ? 1 : (p.getLevel() == null ? 1 : p.getLevel()));
        practice.put("exp", p == null ? 0 : (p.getExp() == null ? 0 : p.getExp()));
        practice.put("totalCount", p == null ? 0 : (p.getTotalCount() == null ? 0 : p.getTotalCount()));
        practice.put("totalSpirit", p == null ? 0 : (p.getTotalSpirit() == null ? 0 : p.getTotalSpirit()));
        practice.put("lastTime", p == null || p.getLastTime() == null ? "" : p.getLastTime().toString());
        m.put("practice", practice);

        SectMember sm = sectMemberMapper.selectOne(Wrappers.<SectMember>lambdaQuery().eq(SectMember::getUserId, u.getId()));
        Map<String, Object> sect = new LinkedHashMap<>();
        if (sm != null) {
            Sect sect0 = sm.getSectId() != null ? sectMapper.selectById(sm.getSectId()) : null;
            sect.put("sectId", sm.getSectId());
            sect.put("sectName", sect0 == null ? "" : (sect0.getName() == null ? "" : sect0.getName()));
            sect.put("role", sm.getRole() == null ? 0 : sm.getRole());
            sect.put("roleLabel", sm.getRole() == null ? "弟子"
                    : (sm.getRole() == 1 ? "长老" : sm.getRole() == 2 ? "宗主" : "弟子"));
            sect.put("contribution", sm.getContribution() == null ? 0 : sm.getContribution());
        } else {
            sect.put("sectId", null);
            sect.put("sectName", "");
            sect.put("role", 0);
            sect.put("roleLabel", "散修");
            sect.put("contribution", 0);
        }
        m.put("sect", sect);

        long consignActive = consignMapper.selectCount(Wrappers.<Consign>lambdaQuery()
                .eq(Consign::getSellerId, u.getId()).eq(Consign::getStatus, 0));
        long consignTotal = consignMapper.selectCount(Wrappers.<Consign>lambdaQuery()
                .eq(Consign::getSellerId, u.getId()));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("consignActive", consignActive);
        stats.put("consignTotal", consignTotal);
        stats.put("practiceTotalCount", p == null ? 0 : (p.getTotalCount() == null ? 0 : p.getTotalCount()));
        m.put("stats", stats);
        return m;
    }

    private static int asInt(Object o, String label) {
        if (o == null) throw new BizException(label + "不可空。");
        int v;
        try {
            v = Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            throw new BizException(label + "须为整数。");
        }
        if (v > 100000000 || v < -100000000) throw new BizException(label + "数值过大。");
        return v;
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
