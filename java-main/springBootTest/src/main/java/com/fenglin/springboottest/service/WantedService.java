package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 求购挂单：山友挂出想收之物并标出最高愿付灵石，持有者可上门。
 */
public interface WantedService {

    /** 求购榜：进行中的挂单，按最高愿付降序 */
    List<Map<String, Object>> list();

    /** 挂出求购 */
    Map<String, Object> publish(String token, String title, Integer maxPrice, String note);

    /** 撤销自己的求购 */
    Map<String, Object> close(String token, Long id);

    /** 查与某物名相关的求购（宝库点开物品时提示"有人求购"） */
    List<Map<String, Object>> match(String title);
}
