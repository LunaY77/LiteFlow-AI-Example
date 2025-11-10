package com.lunay.liteflow.ai.history;

import com.lunay.liteflow.ai.history.context.ChatHistoryContext;
import com.yomahub.liteflow.ai.context.ChatContext;
import com.yomahub.liteflow.ai.engine.model.chat.message.AssistantMessage;
import com.yomahub.liteflow.ai.engine.model.chat.message.Message;
import com.yomahub.liteflow.ai.engine.model.chat.message.SystemMessage;
import com.yomahub.liteflow.ai.engine.model.chat.message.UserMessage;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
        // 1. 初始化对话历史
        List<Message> history = new ArrayList<>();
        // 创建上下文
        ChatHistoryContext context = new ChatHistoryContext();
        context.setConversationHistory(history);

        context.addMessage(new SystemMessage("你是一个叫'FlowAI'的智能助手，名字叫小流，一个使用 LiteFlow-AI 框架实现的 AI 聊天机器人，请用中文回答问题。"));

        Scanner scanner = new Scanner(System.in);
        System.out.println("你好！我是 FlowAI 小流，有什么可以帮你的吗？(输入 'exit' 退出)");

        while (true) {
            System.out.print("你: ");
            String userInput = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userInput)) {
                System.out.println("再见！");
                break;
            }

            // 2. 添加用户输入
            context.addMessage(new UserMessage(userInput));

            // 3. 执行流程
            LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, context);
            if (response.isSuccess()) {
                AssistantMessage assistantMessage = context.getLastAssistantMessage();
                // 输出结果
                System.out.println("FlowAI: " + assistantMessage.getContent());
            } else {
                log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
            }
        }
        scanner.close();
    }
}
