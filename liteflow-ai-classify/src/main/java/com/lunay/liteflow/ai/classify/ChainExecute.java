package com.lunay.liteflow.ai.classify;

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
        System.out.println("================ AI Classify Chain Start ==================");
        executeChain("chain1");
        System.out.println("================ AI Classify Chain End ==================");
        System.out.println("================ AI MultiClassify Chain Start ==================");
        executeChain("chain2");
        System.out.println("================ AI MultiClassify Chain End ==================");
    }

    private void executeChain(String chainId) {
        // 执行 chat 流程，其中 ChatContext 是 LiteFlow-AI 提供的默认上下文类型，目前必须进行传入
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, null, ChatContext.class);
        // 获取到执行流程完毕的上下文
        ChatContext chatContext = response.getContextBean(ChatContext.class);
        if (response.isSuccess()) {
            // 从上下文中获取意图识别结果
            String intent = chatContext.getData("result");
            // 输出结果
            log.info("执行成功，意图识别为: \n{}", intent);
        } else {
            log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
        }
    }
}
