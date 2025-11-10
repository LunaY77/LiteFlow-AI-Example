package com.lunay.liteflow.ai.structure.node;

import com.yomahub.liteflow.ai.annotation.AIComponent;
import com.yomahub.liteflow.ai.annotation.model.io.AIOutput;
import com.yomahub.liteflow.ai.annotation.model.node.AIChat;
import com.yomahub.liteflow.ai.domain.enums.ProviderEnum;
import com.yomahub.liteflow.ai.engine.interact.transport.TransportType;
import com.yomahub.liteflow.ai.engine.model.output.ResponseType;

/**
 * Chat节点
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@AIComponent(
        nodeId = "structureOutputCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = ProviderEnum.DASHSCOPE,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = false,
        // 开启请求响应日志记录，便于调试
        logRequests = true,
        logResponses = true
)
@AIChat(
        systemPrompt = "你是一位数学辅导老师",
        userPrompt = "使用中文解题: 8x + 9 = 32 and x + y = 1",
        // 设置输出为非流式，传输模式为 HTTP
        streaming = false,
        transportType = TransportType.HTTP
)
@AIOutput(
        // 设置响应类型为JSON，模型节点输出将为指定的结构化输出对象
        responseType = ResponseType.JSON,
        // 指定输出结构化数据的类型，通过类全限定名进行指定
        typeName = "com.lunay.liteflow.ai.structure.output.MathReasoning",
        // 使用上下文默认的setData方法，将输出结果放置在 dataMap 中
        methodExpress = "setData(\"output\", $output)"
)
public interface StructureOutputCmp {
}
