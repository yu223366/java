package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 推送：把拍卖行的开拍、落槌、出价等事件实时推给所有在线页面，
 * 取代纯轮询的通知延迟。轮询仍保留为兜底（EventSource 失败时前端会降级）。
 */
@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 新增一个订阅者 */
    public SseEmitter subscribe() {
        // timeout 0 = 不超时，交给客户端断开时回收
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            emitter.send(SseEmitter.event().name("hello").data("已接入山门广播"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /** 广播一个事件 */
    public void broadcast(String event, Object data) {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name(event).data(data));
            } catch (Exception ex) {
                emitters.remove(e);
            }
        }
    }

    /** 当前 SSE 订阅数 */
    public int subscriberCount() {
        return emitters.size();
    }
}
