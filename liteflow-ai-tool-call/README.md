# LiteFlow-AI 工具调用

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
    base-packages: com.lunay.liteflow.ai.toolcall.node
    dashscope:
      apikey: ${toolcall.dashscope.apikey:your-dashscope-api-key-here}
```

## 4. 工具调用

### 4.1 创建工具

1. 通过 SpringBean 自动注册发现工具

在 LiteFlow-AI 中，定义一个工具非常简单，只需要使用 `@Tool` 和 `@ToolParam` 注解。

```java
@Component
public class ToolConfig {

    @Tool(name = "assemble_tool", value = {"组装工具", "将 a 和 b 组装成答案"})
    public String assemble(@ToolParam("a") String a, @ToolParam("b") String b) {
        return "Assembled result: " + a + " and " + b;
    }
}
```

- `@Component`: 将这个工具类注册为 Spring Bean，这样 LiteFlow-AI 才能发现它。
- `@Tool`: 标记一个方法为可供 AI 调用的工具。
    - `name`: 工具的唯一名称，必须是英文字符。AI 将通过这个名称来决定调用哪个工具。
    - `value`: 工具的描述。这个描述非常重要，AI 会根据这个描述来判断在什么情况下应该使用该工具。描述越清晰，AI 的判断越准确。
- `@ToolParam`: 标记方法参数，并提供参数的描述。
    - `value`: 参数的描述，同样非常重要，AI 会根据它来提取和填充参数。

我们通过两个注解定义了一个组装工具，它用来将两个输入参数组装为答案。同时将这个类注册为一个 SpringBean，LiteFLow-AI
框架将通过懒加载机制自动发现这个工具

注意：如果工具入参仅仅只有一个**基本类型**参数（如：String 类型），强烈建议将输入参数使用一个对象进行一层封装，能大大提高工具调用成功率。

如果你好奇 LiteFlow-AI 框架是如何自动发现 tool 的，可以阅读 `com.yomahub.liteflow.ai.tool.SpringBeanToolRegistry` 的源码

2. 自定义工具注册发现逻辑

除了框架内置的通过SpringBean 自动注册发现工具，你同样可以选择自行实现工具的注册和发现。

我们首先定义一个 service 服务类，代表我们的业务工具

```java
@Service
public class MysqlService {

    @Tool(name = "mysql_select_tool", value = "数据库选择工具")
    public String selectData(@ToolParam("数据库输入") SqlInput sqlInput) {
        if ("users".equalsIgnoreCase(sqlInput.getTableName()) && "123".equals(sqlInput.getId())) {
            return "User found: {id: 123, name: 'John Doe', email: '1234567@email.com'}";
        } else {
            return "No data found for id: " + sqlInput.getId() + " in table: " + sqlInput.getTableName();
        }
    }

    @Tool(name = "mysql_delete_tool", value = "数据库删除工具")
    public String deleteData(@ToolParam("数据库输入") SqlInput sqlInput) {
        if ("users".equalsIgnoreCase(sqlInput.getTableName()) && "123".equals(sqlInput.getId())) {
            return "User with id: 123 has been deleted.";
        } else {
            return "No data found for id: " + sqlInput.getId() + " in table: " + sqlInput.getTableName() + ". Deletion failed.";
        }
    }
}
```

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlInput {

    @ToolParam("目标ID")
    private String id;

    @ToolParam("目标表名")
    private String tableName;
}
```

然后定义一个 config 类，注册我们自己的 ToolRegistry，以此来覆盖框架自带的 SpringBean 自动发现工具注册中心

有两种方式：

```java

@Configuration
public class ToolCallConfiguration {

  /**
   * 方式 1：通过包扫描初始化工具注册中心
   */
  @Bean("scanningToolRegistry")
  public ToolRegistry scanningToolRegistry() {
    ScanningToolRegistry toolRegistry = new ScanningToolRegistry("com.lunay.liteflow.ai.toolcall.service");
    assert toolRegistry.getAllTools().size() == 2;
    return toolRegistry;
  }

  /**
   * 方式 2：通过手动注册初始化工具注册中心
   */
  @Bean("functionToolRegistry")
  @Primary
  public ToolRegistry functionToolRegistry(MysqlService mysqlService) {
    StaticToolRegistry toolRegistry = new StaticToolRegistry();
    toolRegistry.register(
            FunctionToolCallback
                    .builder(mysqlService::selectData)
                    .name("mysql_select_tool")
                    .inputType(SqlInput.class)
                    .description("数据库选择工具")
                    .build()
    );
    toolRegistry.register(
            FunctionToolCallback
                    .builder(mysqlService::deleteData)
                    .name("mysql_delete_tool")
                    .inputType(SqlInput.class)
                    .description("数据库删除工具")
                    .build()
    );
    assert toolRegistry.getAllTools().size() == 2;
    return toolRegistry;
  }
}
```

### 4.2 创建 AI 组件并启用工具

在 `@AIChat` 注解中，通过 `toolNames` 属性指定要启用的工具名称。

```java
@AIComponent(
        nodeId = "toolCallCmp",
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
@AIChat(
        userPrompt = "请你在 users 表中查找 id 为 123的用户，确定其存在后，再删除该用户。",
        // 设置输出为非流式，传输模式为 HTTP
        streaming = false,
        transportType = TransportType.HTTP,
        toolNames = {"mysql_select_tool", "mysql_delete_tool"}
)
@AIOutput(
        // 设置响应类型为文本，模型节点输出将为框架定义的 AssistantMessage 对象
        responseType = ResponseType.TEXT,
        // 使用上下文默认的setData方法，将输出结果放置在 dataMap 中
        methodExpress = "setData(\"assistantMessage\", $output)"
)
public interface ToolCallCmp {
}
```

## 5. 流程编排

在 LiteFlow 的规则文件 `flow.el.xml` 中引用这个 AI 组件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
  <chain name="chain1">
    THEN(toolCallCmp);
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
  private final ToolRegistry toolRegistry;

  @Override
  public void run(String... args) throws Exception {
    // 执行 chat 流程，其中 ChatContext 是 LiteFlow-AI 提供的默认上下文类型，目前必须进行传入
    ChatContext chatContext = new ChatContext(toolRegistry);
    LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, chatContext);
    // 获取到执行流程完毕的上下文
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

通过注入 ToolRegistry，手动构建 ChatContext，并传入我们的工具注册中心。

## 7. 运行项目

运行 SpringBoot 项目，查看日志输出：

```shell
2025-11-10T19:41:29.526+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Request Start ======
2025-11-10T19:41:29.526+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
2025-11-10T19:41:29.526+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Headers: {Authorization=Bearer your-api-key-here}
2025-11-10T19:41:29.526+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Body: {
  "model" : "qwen-flash",
  "messages" : [ {
    "role" : "user",
    "content" : "请你在 users 表中查找 id 为 123的用户，确定其存在后，再删除该用户。"
  } ],
  "stream" : false,
  "enable_thinking" : false,
  "tools" : [ {
    "type" : "function",
    "function" : {
      "name" : "mysql_select_tool",
      "description" : "数据库选择工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  }, {
    "type" : "function",
    "function" : {
      "name" : "mysql_delete_tool",
      "description" : "数据库删除工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  } ],
  "response_format" : {
    "type" : "text"
  }
}
2025-11-10T19:41:29.526+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Request End =======
2025-11-10T19:41:30.121+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Response Start ======
2025-11-10T19:41:30.121+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : response: {"choices":[{"message":{"content":"","role":"assistant","tool_calls":[{"function":{"arguments":"{\"id\": \"123\", \"tableName\": \"users\"}","name":"mysql_select_tool"},"id":"call_7babcd9e20224af09f5d03","index":0,"type":"function"}]},"finish_reason":"tool_calls","index":0,"logprobs":null}],"object":"chat.completion","usage":{"prompt_tokens":261,"completion_tokens":28,"total_tokens":289,"prompt_tokens_details":{"cached_tokens":0}},"created":1762774890,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-490dead5-c3a2-4e04-b180-763f1336c1f9"}
2025-11-10T19:41:30.121+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Response End =======
2025-11-10T19:41:30.128+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.liteflow.ai.engine.util.HttpUtil     : 正在关闭 OkHttp 客户端。
2025-11-10T19:41:30.129+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.liteflow.ai.engine.util.HttpUtil     : OkHttp 客户端已成功关闭。
2025-11-10T19:41:30.140+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Request Start ======
2025-11-10T19:41:30.140+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
2025-11-10T19:41:30.140+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Headers: {Authorization=Bearer your-api-key-here}
2025-11-10T19:41:30.140+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Body: {
  "model" : "qwen-flash",
  "messages" : [ {
    "role" : "user",
    "content" : "请你在 users 表中查找 id 为 123的用户，确定其存在后，再删除该用户。"
  }, {
    "role" : "assistant",
    "content" : "",
    "tool_calls" : [ {
      "id" : "call_7babcd9e20224af09f5d03",
      "type" : "function",
      "name" : "mysql_select_tool",
      "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}",
      "function" : {
        "name" : "mysql_select_tool",
        "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}"
      }
    } ]
  }, {
    "role" : "tool",
    "content" : "\"User found: {id: 123, name: 'John Doe', email: '1234567@email.com'}\"",
    "tool_call_id" : "call_7babcd9e20224af09f5d03",
    "tool_name" : "mysql_select_tool"
  } ],
  "stream" : false,
  "enable_thinking" : false,
  "tools" : [ {
    "type" : "function",
    "function" : {
      "name" : "mysql_select_tool",
      "description" : "数据库选择工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  }, {
    "type" : "function",
    "function" : {
      "name" : "mysql_delete_tool",
      "description" : "数据库删除工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  } ],
  "response_format" : {
    "type" : "text"
  }
}
2025-11-10T19:41:30.140+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Request End =======
2025-11-10T19:41:30.530+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Response Start ======
2025-11-10T19:41:30.530+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : response: {"choices":[{"message":{"content":"","role":"assistant","tool_calls":[{"function":{"arguments":"{\"id\": \"123\", \"tableName\": \"users\"}","name":"mysql_delete_tool"},"id":"call_f37c6c72555b4ad3b08e2d","index":0,"type":"function"}]},"finish_reason":"tool_calls","index":0,"logprobs":null}],"object":"chat.completion","usage":{"prompt_tokens":334,"completion_tokens":28,"total_tokens":362,"prompt_tokens_details":{"cached_tokens":0}},"created":1762774890,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-cb4c73f3-400b-4232-b57c-0c8ad9693d7f"}
2025-11-10T19:41:30.530+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Response End =======
2025-11-10T19:41:30.530+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.liteflow.ai.engine.util.HttpUtil     : 正在关闭 OkHttp 客户端。
2025-11-10T19:41:30.531+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.liteflow.ai.engine.util.HttpUtil     : OkHttp 客户端已成功关闭。
2025-11-10T19:41:30.531+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Request Start ======
2025-11-10T19:41:30.532+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
2025-11-10T19:41:30.532+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Headers: {Authorization=Bearer your-api-key-here}
2025-11-10T19:41:30.532+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : Body: {
  "model" : "qwen-flash",
  "messages" : [ {
    "role" : "user",
    "content" : "请你在 users 表中查找 id 为 123的用户，确定其存在后，再删除该用户。"
  }, {
    "role" : "assistant",
    "content" : "",
    "tool_calls" : [ {
      "id" : "call_7babcd9e20224af09f5d03",
      "type" : "function",
      "name" : "mysql_select_tool",
      "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}",
      "function" : {
        "name" : "mysql_select_tool",
        "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}"
      }
    } ]
  }, {
    "role" : "tool",
    "content" : "\"User found: {id: 123, name: 'John Doe', email: '1234567@email.com'}\"",
    "tool_call_id" : "call_7babcd9e20224af09f5d03",
    "tool_name" : "mysql_select_tool"
  }, {
    "role" : "assistant",
    "content" : "",
    "tool_calls" : [ {
      "id" : "call_f37c6c72555b4ad3b08e2d",
      "type" : "function",
      "name" : "mysql_delete_tool",
      "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}",
      "function" : {
        "name" : "mysql_delete_tool",
        "arguments" : "{\"id\": \"123\", \"tableName\": \"users\"}"
      }
    } ]
  }, {
    "role" : "tool",
    "content" : "\"User with id: 123 has been deleted.\"",
    "tool_call_id" : "call_f37c6c72555b4ad3b08e2d",
    "tool_name" : "mysql_delete_tool"
  } ],
  "stream" : false,
  "enable_thinking" : false,
  "tools" : [ {
    "type" : "function",
    "function" : {
      "name" : "mysql_select_tool",
      "description" : "数据库选择工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  }, {
    "type" : "function",
    "function" : {
      "name" : "mysql_delete_tool",
      "description" : "数据库删除工具",
      "parameters" : {
        "type" : "object",
        "properties" : {
          "id" : {
            "type" : "string"
          },
          "tableName" : {
            "type" : "string"
          }
        },
        "required" : [ "id", "tableName" ],
        "additionalProperties" : false
      }
    }
  } ],
  "response_format" : {
    "type" : "text"
  }
}
2025-11-10T19:41:30.532+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Request End =======
2025-11-10T19:41:31.041+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ====== HTTP Response Start ======
2025-11-10T19:41:31.041+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : response: {"choices":[{"message":{"content":"用户 ID 为 123 的用户已成功查找并删除。","role":"assistant"},"finish_reason":"stop","index":0,"logprobs":null}],"object":"chat.completion","usage":{"prompt_tokens":388,"completion_tokens":16,"total_tokens":404,"prompt_tokens_details":{"cached_tokens":0}},"created":1762774891,"system_fingerprint":null,"model":"qwen-flash","id":"chatcmpl-fe6d16ed-1d37-4ee8-a58a-bd07d56c7a63"}
2025-11-10T19:41:31.041+08:00  INFO 27221 --- [liteflow-ai-tool-call] [           main] c.y.l.a.e.i.t.impl.HttpTransport         : ======= HTTP Response End =======
```
