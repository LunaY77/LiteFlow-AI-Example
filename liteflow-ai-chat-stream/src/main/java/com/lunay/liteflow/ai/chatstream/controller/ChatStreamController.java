package com.lunay.liteflow.ai.chatstream.controller;

import com.yomahub.liteflow.ai.context.ChatContext;
import com.yomahub.liteflow.ai.context.StreamHandler;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.CompletableFuture;

/**
 * ChatStream 控制器
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final FlowExecutor flowExecutor;

    /**
     * 流式输出接口 - SSE (Server-Sent Events)
     * 使用 text/event-stream 格式返回流式数据
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam(value = "message", required = false, defaultValue = "你好，请介绍一下 LiteFlow 框架") String message) {
        log.info("收到流式请求，消息: {}", message);

        return Flux.create(sink -> {
            // 创建流式回调
            StreamHandler streamHandler = StreamHandler.builder()
                    .onStart(context -> {
                        log.info("流式输出开始");
                        sink.next("[START]");
                    })
                    .onClose(context -> {
                        log.info("流式输出结束");
                        sink.next("[DONE]");
                        sink.complete();
                    })
                    .onThinking((content, context) -> {
                        sink.next("[Thinking] " + content);
                        // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
                        return content;
                    })
                    .onText((content, context) -> {
                        sink.next(content);
                        // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
                        return content;
                    })
                    .onCompletion(((chatResponse, context) -> {
                        // 在流式输出完成后执行的操作
                        log.info("流式输出完成");
                        log.info("完整响应内容:\n {}", chatResponse.getOutput().getContent());

                        return chatResponse;
                    }))
                    .onError((context, throwable) -> {
                        log.error("流式输出异常", throwable);
                        sink.next("[ERROR] " + throwable.getMessage());
                        sink.error(throwable);
                    })
                    .build();

            // 创建 AI 上下文
            ChatContext chatContext = new ChatContext(streamHandler);
            chatContext.setData("userQuery", message);

            // 使用 CompletableFuture.supplyAsync 包装异步执行
            CompletableFuture.supplyAsync(() -> {
                try {
                    return flowExecutor.execute2Future("chain1", null, chatContext)
                            .get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    log.error("链路执行异常", throwable);
                    if (!sink.isCancelled()) {
                        sink.error(throwable);
                    }
                } else if (response != null && !response.isSuccess()) {
                    log.error("链路执行失败: {}", response.getMessage());
                    if (!sink.isCancelled()) {
                        sink.error(new RuntimeException(response.getMessage()));
                    }
                }
            });

            // 处理客户端断开连接
            sink.onDispose(() -> log.info("客户端断开连接"));
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
