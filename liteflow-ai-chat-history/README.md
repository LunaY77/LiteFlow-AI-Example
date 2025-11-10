# LiteFlow-AI 多轮对话

一个真正智能的聊天机器人必须能够理解上下文，即记住之前的对话内容。LiteFlow-AI 提供了便捷的多轮对话管理机制，让你的 AI 应用拥有“记忆”。

## 1. 多轮对话的原理

多轮对话的核心原理很简单：**在每次请求时，将之前的对话历史一并发送给 AI**。这样，AI 就能根据上下文理解当前的问题，并给出相关的回答。

这个“对话历史”通常是一个消息列表（`List<Message>`），包含了用户和 AI 之前的每一轮交互。

## 2. 维护对话历史

在 LiteFlow-AI 中，对话历史由一个 `List<Message>` 对象表示。`Message` 是一个接口，它有多个实现类，最常用的是：

-   `UserMessage`: 代表用户的消息。
-   `AssistantMessage`: 代表 AI 的消息。
-   `SystemMessage`: 代表系统提示词，通常作为列表的第一个元素，用于设定 AI 的角色。

要实现多轮对话，你需要在业务代码中维护这个列表：

1.  **初始化**: 创建一个 `List<Message>`，可以先加入一个 `SystemMessage`。
2.  **添加用户输入**: 在每次向 AI 提问前，将用户的最新消息（`UserMessage`）添加到列表中。
3.  **发送请求**: 将整个列表发送给 AI。
4.  **添加 AI 响应**: 收到 AI 的响应后，将其（`AssistantMessage`）也添加到列表中，为下一轮对话做准备。

## 3. 使用 `history` 属性

LiteFlow-AI 让你无需手动构建请求。你只需要在 LiteFlow 的上下文中维护好这个 `List<Message>`，然后通过 `@AIChat` 注解的 `history` 属性告诉框架从哪里获取它。

### 3.1 自定义上下文

我们通过继承 ChatContext 来自定义对话上下文

```java
@Getter
@Setter
public class ChatHistoryContext extends ChatContext {

    private List<Message> conversationHistory;
    
    public void addMessage(Message message) {
        this.conversationHistory.add(message);
    }
    
    public AssistantMessage getLastAssistantMessage() {
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            Message msg = conversationHistory.get(i);
            if (msg instanceof AssistantMessage) {
                return (AssistantMessage) msg;
            }
        }
        return null;
    }
}
```

### 3.2 定义 AI 组件

这次，我们不使用 `userPrompt` 和 `systemPrompt`，而是直接指定 `history` 属性。

```java
@AIComponent(
        nodeId = "chatHistoryCmp",
        // 模型提供商使用 DashScope，模型名称为 qwen-flash
        provider = ProviderEnum.DASHSCOPE,
        apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model = "qwen-flash",
        // 关闭思考
        enableThinking = false
)
@AIChat(
        history = "conversationHistory",
        // 设置输出为非流式，传输模式为 HTTP
        streaming = false,
        transportType = TransportType.HTTP
)
@AIOutput(
        // 设置响应类型为文本，模型节点输出将为框架定义的 AssistantMessage 对象
        responseType = ResponseType.TEXT,
        // 使用自定义上下文的 addMessage 方法，将输出结果自动添加到对话历史中
        methodExpress = "addMessage($output)"
)
public interface ChatHistoryCmp {
}
```

同时，我们指定 output 表达式为自定义上下文中的 addMessage 方法，以此将输出结果自动添加到对话历史中。

### 3.2 定义流程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
    <chain name="chain1">
        THEN(chatHistoryCmp);
    </chain>
</flow>
```

### 3.3 实现一个完整的聊天机器人

```java
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
```

### 3.4 执行与交互

```text
你好！我是 FlowAI 小流，有什么可以帮你的吗？(输入 'exit' 退出)
你: 你好，你是谁?
FlowAI: 你好呀！我是小流，一个使用 LiteFlow-AI 框架实现的智能助手~ 我可以陪你聊天、帮你解决问题，或者只是闲聊放松一下。有什么我可以帮你的吗？(•̀ᴗ•́)و
你: 请你用一句话定义 LiteFlow 框架是什么？
FlowAI: LiteFlow 是一个轻量级、可扩展的流程编排框架，用于灵活地定义和执行复杂的业务逻辑流程。
你: 你和 LiteFlow 框架有什么关系？
FlowAI: 我是基于 LiteFlow-AI 框架构建的智能助手，就像一个用这个框架“训练”出来的聊天机器人。LiteFlow 负责流程调度和逻辑编排，而我则在这个框架中实现对话理解与交互能力，是它的一个具体应用实例哦~ (•̀ᴗ•́)و
你: exit
再见！
```

