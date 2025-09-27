package com.lunay.liteflow.ai.helloworld.node;

import com.yomahub.liteflow.ai.annotation.AIComponent;
import com.yomahub.liteflow.ai.annotation.model.io.AIOutput;
import com.yomahub.liteflow.ai.annotation.model.node.AIChat;
import com.yomahub.liteflow.ai.engine.interact.transport.TransportType;
import com.yomahub.liteflow.ai.engine.model.output.ResponseType;
import com.yomahub.liteflow.ai.util.TriState;

/**
 * Chat节点
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@AIComponent(
        nodeId = "chatCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = "dashscope",
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = TriState.FALSE
)
@AIChat(
        userPrompt = "你好，介绍一下你自己。",
        // 设置输出为非流式，传输模式为 HTTP
        streaming = false,
        transportType = TransportType.HTTP
)
@AIOutput(
        // 设置响应类型为文本，模型节点输出将为框架定义的 AssistantMessage 对象
        responseType = ResponseType.TEXT,
        // 使用上下文默认的setData方法，将输出结果放置在 dataMap 中
        methodExpress = "setData",
        // 表示启用键名映射策略
        useKeyIndex = true,
        // 指定键名为 "assistantMessage"
        key = "assistantMessage"
)
public interface ChatCmp {
}
