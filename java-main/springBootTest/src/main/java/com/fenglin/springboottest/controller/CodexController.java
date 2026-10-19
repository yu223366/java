package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.*;
import com.fenglin.springboottest.service.CodexService;
import org.springframework.web.bind.annotation.*;

/**
 * 藏经阁控制器，对接前端 src/api/codex.js 的接口契约。
 * 所有接口前缀 /api/codex（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 接口清单：
 *   GET /api/codex/items        条目检索 ?element=&category=&grade=&q= → { total, items }
 *   GET /api/codex/spirit-roots 灵根列表 ?element=&grade= → { total, items }
 *   GET /api/codex/realms       秘境列表 ?element=&grade= → { total, items }
 */
@RestController
@RequestMapping("/api/codex")
public class CodexController {

    private final CodexService codexService;

    public CodexController(CodexService codexService) {
        this.codexService = codexService;
    }

    /**
     * 条目检索（功法 / 心得 / 修炼资源）。
     * 查询参数：element 五行 / category 分类（父类自动含子类）/ grade 品阶 / q 关键词。
     */
    @GetMapping("/items")
    public Result<PageVo<ItemVo>> items(@RequestParam(required = false) String element,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String grade,
                                        @RequestParam(required = false) String q) {
        return Result.ok(codexService.items(element, category, grade, q));
    }

    /** 灵根列表。查询参数：element / grade */
    @GetMapping("/spirit-roots")
    public Result<PageVo<SpiritRootVo>> spiritRoots(@RequestParam(required = false) String element,
                                                    @RequestParam(required = false) String grade) {
        return Result.ok(codexService.spiritRoots(element, grade));
    }

    /** 秘境列表。查询参数：element / grade */
    @GetMapping("/realms")
    public Result<PageVo<RealmVo>> realms(@RequestParam(required = false) String element,
                                          @RequestParam(required = false) String grade) {
        return Result.ok(codexService.realms(element, grade));
    }
}
