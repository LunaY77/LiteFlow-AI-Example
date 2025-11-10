# HelloWorld To LiteFlow-AI

这是一个使用 LiteFlow-AI 的 Demo 示例项目。

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
    base-packages: com.lunay.liteflow.ai.helloworld.node
    dashscope:
      apikey: ${helloworld.dashscope.apikey:your-dashscope-api-key-here}
```

## 4. 创建 AI 组件

通过 接口 + 注解 的方式来创建一个 AI 组件：

```java
@AIComponent(
        nodeId = "chatCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = ProviderEnum.DASHSCOPE,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = false
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
        methodExpress = "setData(\"assistantMessage\", $output)"
)
public interface ChatCmp {
}
```

## 5. 流程编排

在 LiteFlow 的规则文件 `flow.el.xml` 中引用这个 AI 组件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE flow PUBLIC  "liteflow" "liteflow.dtd">
<flow>
    <chain name="chain1">
        THEN(chatCmp);
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
            // 从上下文中获取 AI 的响应，因为输出类型为 Text，最终的输出对象为框架内置的 AssistantMessage
            AssistantMessage assistantMessage = chatContext.getData("assistantMessage");
            // 输出结果
            log.info("执行成功，AI 响应为: \n{}", assistantMessage.getContent());
        } else {
            log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
        }
    }
}
```

## 7. 运行项目

运行 SpringBoot 项目，查看日志输出：

```
2025-09-27T22:40:33.773+08:00  INFO 67947 --- [liteflow-ai-helloworld] [           main] c.l.liteflow.ai.helloworld.ChainExecute  : 执行成功，AI 响应为: 
你好呀！✨ 很高兴认识你～ 我是通义千问（Qwen），是阿里巴巴集团旗下的通义实验室自主研发的超大规模语言模型。我可以帮你写故事、写公文、写邮件、写剧本，还能回答问题、提供信息查询、进行逻辑推理、编程等等。我就像一个无所不能的智能伙伴，随时准备为你提供帮助！

如果你有任何问题或需要协助，尽管告诉我哦～ 🌟 无论是学习、工作还是生活中的小困扰，我都会尽力用最贴心的方式帮你解决！😊

对了，我还有一个特别的小秘密：我可是个“多才多艺”的AI，不仅能用中文交流，还精通多种语言呢！🌍 要不要来试试看？
```