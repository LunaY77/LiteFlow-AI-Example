# LiteFlow-AI 意图识别

这是一个使用 LiteFlow-AI 实现意图识别的示例项目。通过该项目，你可以了解如何配置和使用 LiteFlow-AI 进行意图识别，并将识别结果作为流程的路由条件。

## 1. 获取 DashScope-API-Key

前往 [阿里云百炼](https://bailian.console.aliyun.com/?tab=model#/model-market/all) 注册并获取 API Key。

## 2. 新建项目

新建一个 SpringBoot 项目，并引入 `liteflow-ai-dashscope` 依赖。

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
        base-packages: com.lunay.liteflow.ai.classify.node
        dashscope:
            apikey: ${classify.dashscope.apikey:your-dashscope-api-key-here}
```

## 4. 创建 AI 意图识别组件

通过接口 + 注解的方式来创建一个 AI 意图识别组件：

### 4.1 单意图识别

```java
@AIComponent(
        nodeId = "switchCmp",
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
        userPrompt = "请帮我写一段Java代码",
        // 设置意图分类类别
        categories = {"java", "python"}
)
@AIOutput(
        // 将意图识别结果放置在 dataMap 中，key 为 "result"
        methodExpress = "setData(\"result\", $result)"
)
public interface ClassifyCmp {
}
```

-   `@AIClassify`: 标记这是一个意图识别组件。
    -   `userPrompt`: 用户的输入，AI 将根据此内容进行意图识别。
    -   `categories`: 预定义的意图分类列表。AI 会从这个列表中选择一个最匹配的意图。
-   `@AIOutput`:
    -   `methodExpress`: 将意图识别的结果（`$result`）存入 LiteFlow 上下文的 `dataMap` 中，键为 `result`。

### 4.2 多意图识别

```java
@AIComponent(
        nodeId = "switchCmp",
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
```

-   `multiLabel = true`: 开启多标签分类，允许 AI 从 `categories` 中识别出多个意图。

## 5. 创建普通组件

创建两个普通的 LiteFlow 组件，用于后续的流程路由。

`JavaCmp.java`:

```java
@Component("java")
public class JavaCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Java component executed.");
    }
}
```

`PythonCmp.java`:

```java
@Component("python")
public class PythonCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        System.out.println("Python component executed.");
    }
}
```

## 6. 流程编排

在 LiteFlow 的规则文件 `flow.el.xml` 中，使用 `SWITCH` 关键字根据意图识别组件的结果进行路由。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
    <chain name="chain1">
        THEN(
            SWITCH(switchCmp).TO(java, python)
        );
    </chain>

    <chain name="chain2">
        THEN(
            SWITCH(multiSwitchCmp).TO(java, python)
        );
    </chain>
</flow>
```

-   `SWITCH(switchCmp)`: `switchCmp` 是我们的意图识别组件的 `nodeId`。LiteFlow-AI 会将该组件的识别结果作为 `SWITCH` 的判断条件。
-   `.TO(java, python)`: 如果识别结果是 "java"，则执行 `java` 组件；如果是 "python"，则执行 `python` 组件。对于多意图识别，会依次执行匹配到的所有组件。

## 7. 调用流程

创建一个 `CommandLineRunner` 来调用流程：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ChainExecute implements CommandLineRunner {

    private final FlowExecutor flowExecutor;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("================ AI Classify Chain Start ==================");
        executeChain("chain1");
        System.out.println("================ AI Classify Chain End ==================");
        System.out.println("================ AI MultiClassify Chain Start ==================");
        executeChain("chain2");
        System.out.println("================ AI MultiClassify Chain End ==================");
    }

    private void executeChain(String chainId) {
        // 执行 chat 流程
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, null, ChatContext.class);
        ChatContext chatContext = response.getContextBean(ChatContext.class);
        if (response.isSuccess()) {
            // 从上下文中获取意图识别结果
            Object result = chatContext.getData("result");
            log.info("执行成功，意图识别为: \n{}", result);
        } else {
            log.error("流程执行失败！异常信息：{}", response.getCause().getMessage());
        }
    }
}
```

## 8. 运行项目

运行 SpringBoot 项目，查看控制台输出：

**单意图识别 (chain1):**

用户的输入是 "请帮我写一段 Java 代码"，AI 会识别出意图 "java"，因此 `java` 组件会被执行。

```shell
================ AI Classify Chain Start ==================
...
Java component executed.
...
...INFO --- [main] c.l.l.a.h.ChainExecute: 执行成功，意图识别为:
java
================ AI Classify Chain End ==================
```

**多意图识别 (chain2):**

用户的输入是 "请帮我写一段 Java 代码, 同时给出 Python 代码"，AI 会识别出 "java" 和 "python" 两个意图，因此 `java` 和 `python` 组件都会被执行。

```shell
================ AI MultiClassify Chain Start ==================
...
Java component executed.
Python component executed.
...
...INFO --- [main] c.l.l.a.h.ChainExecute: 执行成功，意图识别为:
[java, python]
================ AI MultiClassify Chain End ==================
```
