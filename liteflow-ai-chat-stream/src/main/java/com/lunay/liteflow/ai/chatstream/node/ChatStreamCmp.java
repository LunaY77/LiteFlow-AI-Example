package com.lunay.liteflow.ai.chatstream.node;

import com.yomahub.liteflow.ai.annotation.AIComponent;
import com.yomahub.liteflow.ai.annotation.model.io.AIInput;
import com.yomahub.liteflow.ai.annotation.model.io.AIOutput;
import com.yomahub.liteflow.ai.annotation.model.io.InputField;
import com.yomahub.liteflow.ai.annotation.model.node.AIChat;
import com.yomahub.liteflow.ai.domain.enums.ProviderEnum;
import com.yomahub.liteflow.ai.engine.interact.transport.TransportType;
import com.yomahub.liteflow.ai.engine.model.output.ResponseType;

/**
 * 流式输出节点
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@AIComponent(
        nodeId = "chatStreamCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = ProviderEnum.DASHSCOPE,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = true,
        // 开启请求和响应日志记录
        logRequests = true,
        logResponses = true
)
@AIChat(
        // 使用动态输入的方式，获取用户输入的问题
        userPrompt = "{{userQuery}}",
        // 设置输出为流式，传输模式为 SSE
        streaming = true,
        transportType = TransportType.SSE
)
@AIInput(
        mapping = {
                @InputField(name = "userQuery", expression = "dataMap.userQuery", defaultValue = "你好，请介绍一下 LiteFlow 框架"),
        }
)
@AIOutput(
        // 设置响应类型为文本，模型节点输出将为框架定义的 AssistantMessage 对象
        responseType = ResponseType.TEXT,
        // 使用上下文默认的setData方法，将输出结果放置在 dataMap 中
        methodExpress = "setData(\"assistantMessage\", $output)"
)
public interface ChatStreamCmp {
}
