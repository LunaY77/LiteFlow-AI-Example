package com.lunay.liteflow.ai.toolcall;

import com.yomahub.liteflow.ai.context.ChatContext;
import com.yomahub.liteflow.ai.engine.model.chat.message.AssistantMessage;
import com.yomahub.liteflow.ai.engine.tool.registry.ToolRegistry;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 流程触发器
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class ChainExecute implements CommandLineRunner {

    private final FlowExecutor flowExecutor;
    private final ToolRegistry toolRegistry;

    @Override
    public void run(String... args) throws Exception {
        // 执行 chat 流程，其中 ChatContext 是 LiteFlow-AI 提供的默认上下文类型，目前必须进行传入
        ChatContext chatContext = new ChatContext(toolRegistry);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, chatContext);
        // 获取到执行流程完毕的上下文
        if (response.isSuccess()) {
            // 从上下文中获取 AI 的响应，因为输出类型为 Text，最终的输出对象为框架内置的 AssistantMessage
            AssistantMessage assistantMessage = chatContext.getData("assistantMessage");
            // 输出结果
            log.info("执行成功，AI 响应为: \n{}", assistantMessage.getContent());
        } else {
            log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
        }
    }
}
