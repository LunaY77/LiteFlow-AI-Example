package com.lunay.liteflow.ai.classify.node;

import com.yomahub.liteflow.ai.annotation.AIComponent;
import com.yomahub.liteflow.ai.annotation.model.io.AIOutput;
import com.yomahub.liteflow.ai.annotation.model.node.AIClassify;
import com.yomahub.liteflow.ai.domain.enums.ProviderEnum;

/**
 * 意图识别多路由组件
 *
 * @author 苍镜月
 * @since 2.16.0
 */

@AIComponent(
        nodeId = "multiSwitchCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = ProviderEnum.DASHSCOPE,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = false,
        // 开启请求和响应日志记录
        logRequests = true,
        logResponses = true
)
@AIClassify(
        userPrompt = "请帮我写一段Java代码, 同时给出 Python 代码",
        // 设置分类类别
        categories = {"java", "python"},
        // 启用多标签分类
        multiLabel = true
)
@AIOutput(
        // 将意图识别结果放置在 dataMap 中，key 为 "result"
        methodExpress = "setData(\"result\", $result)"
)
public interface MultiClassifyCmp {
}
