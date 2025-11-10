package com.lunay.liteflow.ai.structure;

import com.lunay.liteflow.ai.structure.output.MathReasoning;
import com.yomahub.liteflow.ai.context.ChatContext;
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

    @Override
    public void run(String... args) throws Exception {
        // 执行 chat 流程，其中 ChatContext 是 LiteFlow-AI 提供的默认上下文类型，目前必须进行传入
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, ChatContext.class);
        // 获取到执行流程完毕的上下文
        ChatContext chatContext = response.getContextBean(ChatContext.class);
        if (response.isSuccess()) {
            // 从上下文中获取 AI 的响应，因为输出类型为 JSON, 最终的输出对象为目标结构化输出对象 MathReasoning
            MathReasoning mathReasoning = chatContext.getData("output");
            // 输出结果
            log.info("执行成功，AI 响应为: \n{}", mathReasoning);
        } else {
            log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
        }
    }
}
