# LiteFlow-AI Chat Stream

这是一个使用 LiteFlow-AI 实现流式聊天功能的示例项目。通过该项目，你可以了解如何配置和使用 LiteFlow-AI 进行实时对话处理。

## 1. 获取 DashScope-API-Key

前往 [阿里云百炼](https://bailian.console.aliyun.com/?tab=model#/model-market/all) 注册并获取 API Key。


## 2. 新建项目

新建一个 SpringBoot 项目，并引入 liteflow-ai-dashscope 依赖 和 spring-boot-starter-webflux。

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-ai-dashscope</artifactId>
    <version>${liteflow.ai.version}</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## 3. 应用配置

在 `application.yaml` 中配置 LiteFlow-AI 相关配置信息：

```yaml
liteflow:
  rule-source: flow.el.xml
  ai:
    enable: true
    base-packages: com.lunay.liteflow.ai.chatstream.node
    dashscope:
      apikey: ${chatstream.dashscope.apikey:your-dashscope-api-key-here}
```

## 4. 创建 AI 组件

通过 接口 + 注解 的方式来创建一个 AI 组件：

```java
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
                @InputField(name = "userQuery", expression = "dataMap.userQuery", defaultValue = "请分享一些关于人工智能发展的最新趋势。"),
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
```

和 blocking 模式不同，我们需要将 streaming 设置为 true，同时将 transportType 设置为 SSE，以启用服务器发送事件的流式传输。（注：Ollama 流式调用必须使用 DnJson）

同时，在这个流式输出例子中，我们打开了请求和响应日志记录功能，方便调试和查看交互内容。

## 5. 流程编排

在 LiteFlow 的规则文件 `flow.el.xml` 中引用这个 AI 组件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE flow PUBLIC  "liteflow" "liteflow.dtd">
<flow>
    <chain name="chain1">
        THEN(chatStreamCmp);
    </chain>
</flow>
```

## 6. 编写流式处理器

我们可以在每一次流程调用之前创建一个 StreamHandler 来处理流式返回的数据，也可以通过注册为 Spring Bean 的方式全局注册一个 StreamHandler。

1. Spring Bean 全局注册方式：

```java
@Component
public class GlobalStreamHandler implements StreamHandler {
    @Override
    // ...
}
```

细节方法重写此处不进行编写了。

如果你通过Spring Bean 的方式注册了一个流式回调处理器，那么在初始化 ChatContext 的时候可以不需要进行传入, 框架会自动寻找到对应的流式回调器

对于 ChatContext 的初始化，可以参考初始化源码：

```java
public class ChatContext extends DefaultContext {
    private String chatId;
    private StreamHandler streamHandler;
    private ToolRegistry toolRegistry;

    public ChatContext() {
        this(SpringUtil.getBean(StreamHandler.class), SpringUtil.getBean(ToolRegistry.class));
    }
    
    //...
}
```

2. 每次调用前传入方式：

在本次示例中，采取该方式进行传入

```java
// 创建流式回调
StreamHandler streamHandler = StreamHandler.builder()
        .onStart(context -> {
            log.info("流式输出开始");
            sink.next("[START]");
        })
        .onClose(context -> {
            log.info("流式输出结束");
            sink.next("[DONE]");
            sink.complete();
        })
        .onThinking((content, context) -> {
            sink.next("[Thinking] " + content);
            // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
            return content;
        })
        .onText((content, context) -> {
            sink.next(content);
            // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
            return content;
        })
        .onCompletion(((chatResponse, context) -> {
            // 在流式输出完成后执行的操作
            log.info("流式输出完成");
            log.info("完整响应内容:\n {}", chatResponse.getOutput().getContent());

            return chatResponse;
        }))
        .onError((context, throwable) -> {
            log.error("流式输出异常", throwable);
            sink.next("data: [ERROR] " + throwable.getMessage() + "\n\n");
            sink.error(throwable);
        })
        .build();

// 创建 AI 上下文
ChatContext chatContext = new ChatContext(streamHandler);
```

我们可以通过 StreamHandler.builder() 方法快速创建一个流式回调处理器，并重写各个回调方法来处理不同的事件。


## 7. 编写 controller 调用流程

使用 Spring WebFlux 来实现一个流式输出的接口，同时，请记得使用 FlowExecutor 的异步执行方法 execute2Future 来调用流程：

```java
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final FlowExecutor flowExecutor;

    /**
     * 流式输出接口 - SSE (Server-Sent Events)
     * 使用 text/event-stream 格式返回流式数据
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam(value = "message", required = false, defaultValue = "你好，请介绍一下 LiteFlow 框架") String message) {
        log.info("收到流式请求，消息: {}", message);

        return Flux.create(sink -> {
            // 创建流式回调
            StreamHandler streamHandler = StreamHandler.builder()
                    .onStart(context -> {
                        log.info("流式输出开始");
                        sink.next("[START]");
                    })
                    .onClose(context -> {
                        log.info("流式输出结束");
                        sink.next("[DONE]");
                        sink.complete();
                    })
                    .onThinking((content, context) -> {
                        sink.next("[Thinking] " + content);
                        // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
                        return content;
                    })
                    .onText((content, context) -> {
                        sink.next(content);
                        // 返回内容，以便在上下文中记录 (你可以在此进行其他处理)
                        return content;
                    })
                    .onCompletion(((chatResponse, context) -> {
                        // 在流式输出完成后执行的操作
                        log.info("流式输出完成");
                        log.info("完整响应内容:\n {}", chatResponse.getOutput().getContent());

                        return chatResponse;
                    }))
                    .onError((context, throwable) -> {
                        log.error("流式输出异常", throwable);
                        sink.next("[ERROR] " + throwable.getMessage());
                        sink.error(throwable);
                    })
                    .build();

            // 创建 AI 上下文
            ChatContext chatContext = new ChatContext(streamHandler);
            chatContext.setData("userQuery", message);

            // 使用 CompletableFuture.supplyAsync 包装异步执行
            CompletableFuture.supplyAsync(() -> {
                try {
                    return flowExecutor.execute2Future("chain1", null, chatContext)
                            .get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    log.error("链路执行异常", throwable);
                    if (!sink.isCancelled()) {
                        sink.error(throwable);
                    }
                } else if (response != null && !response.isSuccess()) {
                    log.error("链路执行失败: {}", response.getMessage());
                    if (!sink.isCancelled()) {
                        sink.error(new RuntimeException(response.getMessage()));
                    }
                }
            });

            // 处理客户端断开连接
            sink.onDispose(() -> log.info("客户端断开连接"));
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}
```

## 8. 运行项目，发起请求

运行 SpringBoot 项目，发起一个 curl 请求：

```bash
curl -N -G \
  --data-urlencode "message=你好，请介绍一下 LiteFlow 框架" \
  http://localhost:8080/api/chat/stream
```

最终你会在shell看到类似如下的流式输出：

```shell
data:[START]

data:[Thinking]

data:[Thinking] 嗯

data:[Thinking] ，

data:[Thinking] 用户

data:[Thinking] 让我介绍一下LiteFlow
```

同时控制台也打印出了请求和响应的日志，方便调试和查看交互内容。

```shell
...INFO 15051 --- [liteflow-ai-chat-stream] [] c.y.l.a.e.i.transport.impl.SseTransport  : ====== SSE Request Start ======
...INFO 15051 --- [liteflow-ai-chat-stream] [] c.y.l.a.e.i.transport.impl.SseTransport  : URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
...INFO 15051 --- [liteflow-ai-chat-stream] [] c.y.l.a.e.i.transport.impl.SseTransport  : Headers: {Authorization=Bearer your-api-key-here}
...INFO 15051 --- [liteflow-ai-chat-stream] [] c.y.l.a.e.i.transport.impl.SseTransport  : Body: {
  "model" : "qwen-flash",
  "messages" : [ {
    "role" : "user",
    "content" : "你好，请介绍一下 LiteFlow 框架"
  } ],
  "stream" : true,
  "enable_thinking" : true,
  "response_format" : {
    "type" : "text"
  }
}
...INFO 15051 --- [liteflow-ai-chat-stream] [] c.y.l.a.e.i.transport.impl.SseTransport  : ======= SSE Request End =======
```

```shell
2025-11-10T15:52:50.864+08:00  INFO 15051 --- [liteflow-ai-chat-stream] [liyuncs.com/...] c.y.l.a.e.i.transport.impl.SseTransport  : SSE Response Event - id: null, type: null, data: {"choices":[{"delta":{"content":null,"role":"assistant","reasoning_content":""},"index":0,"logprobs":null,"finish_reason":null}],"object":"chat.completion.chunk","usage":null,"created":1762761171,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-2de807f0-b6fc-46ee-bbfc-1085505d7030"}
2025-11-10T15:52:50.871+08:00  INFO 15051 --- [liteflow-ai-chat-stream] [liyuncs.com/...] c.y.l.a.e.i.transport.impl.SseTransport  : SSE Response Event - id: null, type: null, data: {"choices":[{"finish_reason":null,"logprobs":null,"delta":{"content":null,"reasoning_content":"嗯"},"index":0}],"object":"chat.completion.chunk","usage":null,"created":1762761171,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-2de807f0-b6fc-46ee-bbfc-1085505d7030"}
2025-11-10T15:52:50.872+08:00  INFO 15051 --- [liteflow-ai-chat-stream] [liyuncs.com/...] c.y.l.a.e.i.transport.impl.SseTransport  : SSE Response Event - id: null, type: null, data: {"choices":[{"delta":{"content":null,"reasoning_content":"，"},"finish_reason":null,"index":0,"logprobs":null}],"object":"chat.completion.chunk","usage":null,"created":1762761171,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-2de807f0-b6fc-46ee-bbfc-1085505d7030"}
2025-11-10T15:52:50.872+08:00  INFO 15051 --- [liteflow-ai-chat-stream] [liyuncs.com/...] c.y.l.a.e.i.transport.impl.SseTransport  : SSE Response Event - id: null, type: null, data: {"choices":[{"delta":{"content":null,"reasoning_content":"用户"},"finish_reason":null,"index":0,"logprobs":null}],"object":"chat.completion.chunk","usage":null,"created":1762761171,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-2de807f0-b6fc-46ee-bbfc-1085505d7030"}
2025-11-10T15:52:50.919+08:00  INFO 15051 --- [liteflow-ai-chat-stream] [liyuncs.com/...] c.y.l.a.e.i.transport.impl.SseTransport  : SSE Response Event - id: null, type: null, data: {"choices":[{"delta":{"content":null,"reasoning_content":"让我介绍一下LiteFlow"},"finish_reason":null,"index":0,"logprobs":null}],"object":"chat.completion.chunk","usage":null,"created":1762761171,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-2de807f0-b6fc-46ee-bbfc-1085505d7030"}
```