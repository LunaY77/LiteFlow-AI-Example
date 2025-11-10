# LiteFlow-AI 结构化输出

## 1. 获取 DashScope-API-Key

前往 [阿里云百炼](https://bailian.console.aliyun.com/?tab=model#/model-market/all) 注册并获取 API Key。


## 2. 新建项目

新建一个 SpringBoot 项目，并引入 liteflow-ai-dashscope 依赖。

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-ai-dashscope</artifactId>
    <version>${liteflow.ai.version}</version>
</dependency>
```

## 3. 应用配置

在 `application.yaml` 中配置 LiteFlow-AI 相关配置信息：

```yaml
liteflow:
  rule-source: flow.el.xml
  ai:
    enable: true
    base-packages: com.lunay.liteflow.ai.structure.node
    dashscope:
      apikey: ${structure.dashscope.apikey:your-dashscope-api-key-here}
```

## 4. 结构化输出组件

### 4.1 定义结构化输出对象

我们需要定义一个 Java 类来表示我们期望的 Json 结构。

通过定义一个数学推导类，来指导 llm 输出推导数学问题的思考过程和步骤。

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MathReasoning {

    @JsonProperty("steps")
    private List<Step> steps;

    @JsonProperty("final_answer")
    private String finalAnswer;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MathReasoning {\n");
        sb.append("  Final Answer: \"").append(finalAnswer).append("\"\n");

        sb.append("  Steps: [\n");
        if (steps != null && !steps.isEmpty()) {
            // 迭代所有 step，并调用它们的 toString()
            for (int i = 0; i < steps.size(); i++) {
                sb.append(steps.get(i).toString());
                if (i < steps.size() - 1) {
                    sb.append(",\n"); // 在步骤之间添加逗号和换行
                } else {
                    sb.append("\n"); // 最后一个步骤后只有换行
                }
            }
        } else {
            sb.append("    (No steps)\n");
        }
        sb.append("  ]\n"); // Steps 列表的闭合方括号
        sb.append("}"); // MathReasoning 对象的闭合花括号

        return sb.toString();
    }
}
```

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Step {

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("output")
    private String output;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    Step {\n");
        sb.append("      Explanation: \"").append(explanation).append("\"\n");
        sb.append("      Output: \"").append(output).append("\"\n");
        sb.append("    }");
        return sb.toString();
    }
}
```


### 4.2 定义组件

通过 接口 + 注解 的方式来创建一个 AI 组件：

```java
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
```

注意：这里将 `responseType` 设置为了 Json，结构化输出的数据对象需要通过类的全限定名进行指定

如果是 List 列表、Map 哈希表等需要添加泛型的结构化输出对象，可以添加尖括号包裹泛型对象。

例如，假设你想要结构化输出一个字符串列表，你可以这样写 `typeName = "java.util.List<java.lang.String>"`

## 5. 流程编排

在 LiteFlow 的规则文件 `flow.el.xml` 中引用这个 AI 组件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE flow PUBLIC  "liteflow" "liteflow.dtd">
<flow>
    <chain name="chain1">
        THEN(structureOutputCmp);
    </chain>
</flow>
```

## 6. 调用流程

创建一个 CommandLineRunner 来调用这个流程：

```java
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
```

## 7. 运行项目

运行 SpringBoot 项目，查看日志输出：

```shell
2025-11-10T20:24:57.884+08:00  INFO 29627 --- [liteflow-ai-structure-output] [           main] c.l.liteflow.ai.structure.ChainExecute   : 执行成功，AI 响应为: 
MathReasoning {
  Final Answer: "x = 2.875, y = -1.875"
  Steps: [
    Step {
      Explanation: "解第一个方程 8x + 9 = 32，先减去9：8x = 23"
      Output: "8x = 23"
    },
    Step {
      Explanation: "两边同时除以8，得到 x = 23 / 8 = 2.875"
      Output: "x = 2.875"
    },
    Step {
      Explanation: "将 x = 2.875 代入第二个方程 x + y = 1，得 2.875 + y = 1"
      Output: "2.875 + y = 1"
    },
    Step {
      Explanation: "解出 y：y = 1 - 2.875 = -1.875"
      Output: "y = -1.875"
    }
  ]
}
```