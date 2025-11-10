package com.lunay.liteflow.ai.toolcall.tool;

import com.yomahub.liteflow.ai.engine.tool.annotation.Tool;
import com.yomahub.liteflow.ai.engine.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

// SpringBean 工具自动注册发现示例
@Component
public class ToolConfig {

    @Tool(name = "assemble_tool", value = {"组装工具", "将 a 和 b 组装成答案"})
    public String assemble(@ToolParam("a") String a, @ToolParam("b") String b) {
        return "Assembled result: " + a + " and " + b;
    }
}
