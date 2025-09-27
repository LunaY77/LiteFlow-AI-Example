# LiteFLow-AI-Example

LiteFlow-AI 是一个基于 LiteFlow 框架开发的插件模块，故引入 LiteFlow-AI 依赖后，也同样引入了 LiteFlow 依赖。

同时，LiteFlow-AI 基于 SpringBoot 环境进行开发，所以运行 LiteFlow-AI 需要引入 SpringBoot 依赖。

以及，LiteFlow-AI 与 LiteFlow 框架一致，从JDK8到JDK25，统统支持。

如何引入依赖？可参考如下方式：

1. 通过 bom 在包管理 pom 文件中引入

```xml
<dependencyManagement>
    <dependencies>
        <!-- LiteFlow-AI的依赖配置-->
        <dependency>
            <groupId>com.yomahub</groupId>
            <artifactId>liteflow-ai-bom</artifactId>
            <version>${liteflow.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

2. 在具体的业务模块中按需导入对应的依赖，比如假设你希望引入调用 DashScope模型 的能力：

```xml
<dependencies>
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-ai-dashscope</artifactId>
    </dependency>
</dependencies>
```

## 组价介绍

LiteFlow-AI 中创建一个 AI 组件通过定义一个**接口类 + 注解**的方式来实现, 有如下的组织结构：

```java
@AIComponent(...)
@AIChat(...)
@AIInput(...)
@AIOutput(...)
public interface MyAICmp {}
```

共计 4 大注解类型：

1. `@AIComponent`：标记这是一个AI组件，并配置与AI模型厂商相关的基础信息，如API地址、模型名称、超时等。
2. `@AIChat` & `@AIClassify`：标记AI组件的具体任务类型。@AIChat 用于对话场景，@AIClassify 用于分类场景。
3. `@AIInput`：配置AI组件的输入，定义如何从LiteFlow上下文中提取数据并填充到Prompt模板中。
4. `@AIOutput`：配置AI组件的输出，定义如何将AI模型的返回结果（无论是文本还是结构化JSON）映射回LiteFlow上下文中。

当我们创建了一个接口并定义好这些注解后，就可以在LiteFlow的规则文件中直接引用这个AI组件, 对应的组件名称可以使用 AIComponent 中的 nodeId
设置，也可以使用默认值，即类名。假设我们定义了一个 nodeId 为 ai 的 aiCmp，那么规则文件与 LiteFlow 原生书写方式无异，如下所示：

```xml
<flow>
    <chain name="chain1">
        THEN(ai);
    </chain>
</flow>
```

下面分别介绍各个注解的具体使用方式


### 1. @AIComponent

`@AIComponent` 注解是所有AI组件的入口，用于标记一个接口为AI组件，并配置其基础和通用参数。

### 主要参数详解

#### 基础信息

* `provider`: `String` - AI服务提供商的标识，例如 "openai", "dashscope", "ollama" 等。
* `nodeId`: `String` - 组件在LiteFlow中的唯一ID。
* `nodeName`: `String` - 组件的名称。

#### 连接与模型参数

* `apiUrl`: `String` - AI服务的API基础URL。框架会自动处理URL末尾的斜杠。
* `endPoint`: `String` - 具体的API端点。如果留空，将使用对应模型提供商的默认端点。
* `model`: `String` - 要使用的模型名称，例如 "gpt-4", "qwen-turbo" 等。
* `version`: `String` - 模型的版本。

#### 核心调用参数

* `temperature`: `double` (默认: -1.0) - 控制生成文本的随机性。值越高，输出越随机。-1.0表示使用默认值。
* `topP`: `double` (默认: -1.0) - 控制核心采样的概率阈值。-1.0表示使用默认值。
* `topK`: `int` (默认: -1) - 限制了模型在生成下一个词时考虑的候选词数量。-1表示使用默认值。
* `maxTokens`: `int` (默认: -1) - 生成响应的最大token数。-1表示使用默认值。
* `stop`: `String[]` (默认: {}) - 一个或多个停止序列，当模型生成这些序列时会停止输出。
* `seed`: `int` (默认: -1) - 随机种子，用于可复现的输出。-1表示不设置。

#### 惩罚参数

* `repeatPenalty`: `double` (默认: -1.0) - 重复惩罚系数。
* `presencePenalty`: `double` (默认: -1.0) - 存在惩罚系数。
* `frequencyPenalty`: `double` (默认: -1.0) - 频率惩罚系数。

#### Tool Calling 相关

* `autoToolCallEnabled`: `TriState` (默认: `TRUE`) - 是否自动执行Tool Call。

#### 网络与日志

* `connectTimeout`: `String` (默认: "") - 连接超时时间。支持标准ISO-8601格式 (如 "PT30S") 或简化格式 (如 "30s", "5m")。留空则使用全局默认值（60s）。
* `readTimeout`: `String` (默认: "") - 读取超时时间，格式同 `connectTimeout`。
* `maxRetries`: `int` (默认: -1) - 最大重试次数。
* `logRequests`: `TriState` (默认: `UNSET`) - 是否记录请求日志。
* `logResponses`: `TriState` (默认: `UNSET`) - 是否记录响应日志。

#### 其他

* `customHeaders`: `KeyValue[]` (默认: {}) - 自定义HTTP请求头。

---

## 2\. @AIChat

`@AIChat` 注解定义了一个对话类型的AI组件。它专注于处理聊天场景，支持多轮对话、流式输出和工具调用。

### 主要参数详解

* `history`: `String` (默认: "") - 指定一个LiteFlow上下文路径表达式，用于获取聊天历史记录 (`List<Message>`)。启用后，`systemPrompt` 和 `userPrompt` 将被忽略。

    * **示例**: `"chatHistoryList"` 或 `"myContext.history"`。

* `systemPrompt`: `String` (默认: "") - 系统提示词，用于设定AI的角色和行为。

* `userPrompt`: `String` (默认: "") - 用户提示词，即用户当前轮次的输入。

  > **提示词注入方式**：`systemPrompt` 和 `userPrompt` 都支持多种灵活的资源注入方式：

  >   * **直接文本**: `"你是一个专业的AI助手"`
  >   * **类路径资源**: `"classpath:prompts/system.txt"`
  >   * **文件系统资源**: `"file:/path/to/your/prompt.txt"`
  >   * **URL资源**: `"http://example.com/prompt.txt"`
  >   * **文本前缀**: `"text:你是一个专业的AI助手"`

* `streaming`: `boolean` (默认: `true`) - 是否开启流式输出。

* `transportType`: `TransportType` (默认: `SSE`) - 流式输出的传输类型，默认为 Server-Sent Events。

* `toolNames`: `String[]` (默认: {}) - 需要为本次调用启用的工具名称列表。如果为空，则默认不启用任何工具。工具需要在上下文中或Spring容器中注册。

-----

## 3\. @AIClassify

`@AIClassify` 注解定义了一个分类类型的AI组件。它专门用于意图识别和文本分类任务。

### 主要参数详解

* `history`: `String` (默认: "") - 与 `@AIChat` 中的 `history` 类似，用于提供对话历史以上下文。启用后，框架将不会自动生成用于意图识别的系统提示词。
* `systemPrompt`: `String` (默认: "") - 自定义系统提示词。如果留空，框架会根据 `categories` 自动生成一个用于意图分类的系统提示词。
* `userPrompt`: `String` (默认: "") - 需要进行分类的用户输入文本。支持与 `@AIChat` 相同的多种注入方式。
* `categories`: `String[]` (默认: {}) - 预定义的分类标签列表。这是进行分类的基础。
* `multiLabel`: `boolean` (默认: `false`) - 是否支持多标签分类。如果为 `true`，模型可能会返回多个匹配的分类。
* `toolNames`: `String[]` (默认: {}) - 需要启用的工具列表。在分类场景中，这通常用于实现更复杂的、需要外部工具辅助的分类逻辑。

-----

## 4\. @AIInput

`@AIInput` 注解用于精确控制如何从LiteFlow上下文中提取数据，并将其填充到 `userPrompt` 或 `systemPrompt` 的模板占位符中。

### 主要参数详解

* `mapping`: `InputField[]` (默认: {}) - 一个输入字段映射规则的数组。

#### `InputField` 详解

`@InputField` 用于定义单个字段的映射规则。

* `name`: `String` (**必需**) - 字段名称，必须与Prompt模板中的占位符完全对应。例如，如果Prompt中有 `{{productName}}`，则 `name` 必须是 `"productName"`。
* `expression`: `String` (**必需**) - LiteFlow上下文路径表达式，用于获取数据。
    * **直接属性**: `"productName"`
    * **嵌套属性** (如Map): `"productInfo.name"`
* `defaultValue`: `String` (默认: "") - 当上下文中找不到对应值时的默认值。
* `required`: `boolean` (默认: `true`) - 该字段是否为必需。如果为 `true` 且在上下文中找不到值（也没有默认值），可能会导致错误。

### 使用示例

假设 `userPrompt` 为 `"请介绍一下产品 {{productName}} 的特点，并与 {{competitor}} 对比。"`，可以这样配置 `@AIInput`：

```java
@AIInput(mapping = {
    @InputField(name = "productName", expression = "product.name"),
    @InputField(name = "competitor", expression = "requestData.competitorName", required = false, defaultValue = "市场上的同类产品")
})
```

-----

## 5\. @AIOutput

`@AIOutput` 注解负责处理AI模型的输出，将其映射并设置回LiteFlow的上下文中，支持文本和结构化的JSON输出。

### 主要参数详解

#### 整体输出配置

* `responseType`: `ResponseType` (默认: `TEXT`) - 指定输出类型。
    * `ResponseType.TEXT`: 输出纯文本。
    * `ResponseType.JSON`: 输出结构化的JSON对象。
* `typeName`: `String` (默认: `"java.lang.String"`) - 当 `responseType` 为 `JSON` 时，指定输出JSON所对应的目标类名。支持泛型，如 `"com.example.MyEntity"` 或 `"java.util.List<com.example.MyEntity>"`。
* `strict`: `boolean` (默认: `true`) - 在JSON输出模式下，是否启用严格模式，确保输出严格符合 `typeName` 定义的结构。

#### 默认映射规则

* `methodExpress`: `String` (默认: `"setData"`) - 指定将输出结果设置到上下文的方法名。例如，对于 `DefaultContext`，`"setData"` 会调用 `context.setData(key, value)`；对于自定义上下文，`"setProductName"` 会调用 `context.setProductName(output)`。
* `useKeyIndex`: `boolean` (默认: `false`) - 是否启用基于 `key` 或 `index` 的映射策略。
* `key`: `String` (默认: "") - 当 `useKeyIndex` 为 `true` 且目标是Map时，指定存入的键。
* `index`: `int` (默认: -1) - 当 `useKeyIndex` 为 `true` 且目标是List或数组时，指定存入的索引。

#### 精细化字段映射

* `mapping`: `OutputField[]` (默认: {}) - 字段级别的输出映射规则数组，仅在 `responseType` 为 `JSON` 时生效。用于将JSON输出对象中的特定字段映射到上下文的不同位置。

#### `OutputField` 详解

`@OutputField` 用于定义JSON输出对象中单个字段的精细映射规则。

* `sourceField`: `String` (**必需**) - 源字段名称，即从 `typeName` 定义的结构化输出对象中要读取的字段名。
* `methodExpress`: `String` (默认: "") - 针对该字段的目标方法表达式。如果留空，则继承 `@AIOutput` 中定义的 `methodExpress`。
* `useKeyIndex`: `boolean` (默认: `false`) - 是否对该字段启用基于 `key` 或 `index` 的映射。
* `key`: `String` (默认: "") - 该字段的目标键名（当 `useKeyIndex` 为 `true` 时）。
* `index`: `int` (默认: -1) - 该字段的目标索引（当 `useKeyIndex` 为 `true` 时）。

### 使用示例

**简单文本输出：**

```java
// 将AI返回的文本，通过 context.setData("aiResult", text) 存入上下文
@AIOutput(methodExpress = "setData", useKeyIndex = true, key = "aiResult")
```

**结构化JSON输出并精细映射：**

假设AI返回一个JSON，其对应的 `typeName` 为 `"com.example.ProductAnalysis"`，类定义如下：

```java
class ProductAnalysis {
    private String summary;
    private List<String> keyFeatures;
}
```

可以这样配置 `@AIOutput`，将不同字段存到上下文的不同位置：

```java
@AIOutput(
    responseType = ResponseType.JSON,
    typeName = "com.example.ProductAnalysis",
    mapping = {
        // 将 summary 字段的值，通过 context.setSummary(summary) 存入
        @OutputField(sourceField = "summary", methodExpress = "setSummary"),
        // 将 keyFeatures 字段的值，通过 context.setData("features", keyFeatures) 存入
        @OutputField(sourceField = "keyFeatures", methodExpress = "setData", useKeyIndex = true, key = "features")
    }
)
```

-----


