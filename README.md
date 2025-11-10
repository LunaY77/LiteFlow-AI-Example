# LiteFLow-AI-Example

## 什么是 LiteFlow-AI

LiteFlow-AI 是基于 LiteFlow 规则引擎框架的 AI 扩展模块，它将 AI 能力与 LiteFlow 强大的流程编排能力完美结合，为开发者提供了一种声明式、组件化的方式来构建 AI 应用。

**核心特性**：
- **注解驱动**：通过简洁的注解定义 AI 组件，无需编写大量样板代码
- **多模型支持**：支持主流 AI 模型提供商，如 OpenAI、DashScope、Ollama 等
- **流程编排**：利用 LiteFlow 的 EL 表达式轻松编排复杂的 AI 调用流程
- **流式与阻塞**：同时支持流式输出与阻塞调用，满足不同场景需求
- **工具调用**：内置工具调用机制，让 AI 能够调用你定义的函数
- **结构化输出**：支持将 AI 输出直接解析为 Java 对象

## 为什么选择 LiteFlow-AI

在构建 AI 应用时，你可能会遇到以下挑战：

1. **业务逻辑复杂**：需要将多个 AI 调用、数据处理、业务判断串联起来
2. **代码臃肿**：大量的条件判断和流程控制代码混杂在一起
3. **难以维护**：业务规则变化时需要修改大量代码
4. **可扩展性差**：添加新的 AI 能力或调整流程需要大量重构

**LiteFlow-AI 的解决方案**：

- **组件化**：将 AI 调用封装为独立组建，职责单一，易于测试和复用
- **规则驱动**：使用声明式的规则文件编排流程，业务逻辑一目了然
- **动态调整**：规则可以热刷新，业务流程调整无需重启服务
- **统一抽象**：屏蔽不同 AI 提供商的差异，切换成本低


## 快速开始

### 环境准备

- JDK 8 及以上版本
- SpringBoot 2.x 或 3.x
- Maven 或 Gradle 构建工具

### 依赖引入

#### Maven Bom 方式引入

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

#### Maven 传统方式引入

```xml
 <dependencies>
    <!-- 选择其中一个 AI 提供商的依赖 -->
    <!-- Ollama -->
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-ai-ollama</artifactId>
        <version>2.16.0</version>
    </dependency>
    <!-- OpenAI -->
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-ai-openai</artifactId>
        <version>2.16.0</version>
    </dependency>
    <!-- 通义千问 DashScope -->
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-ai-dashscope</artifactId>
        <version>2.16.0</version>
    </dependency>
</dependencies>
```

#### Gradle 方式引入

```groovy
dependencies {
    // 选择其中一个 AI 提供商的依赖
    // Ollama
    implementation 'com.yomahub:liteflow-ai-ollama:2.16.0'
    // OpenAI
    implementation 'com.yomahub:liteflow-ai-openai:2.16.0'
    // 通义千问 DashScope
    implementation 'com.yomahub:liteflow-ai-dashscope:2.16.0'
}
```

### 参数配置

需要在 `src/main/resources/application.yaml` 中配置 LiteFlow-AI 相关配置信息：

```yaml
liteflow:
  rule-source: flow.el.xml
  ai:
    enable: true
    base-packages: com.example.demo.node # 你的AI组件所在的包路径
```

如果你使用的是 DashScope 模型 或者 OpenAI 输出格式的模型，还需要配置 DashScope 或 OpenAI 的 API Key：

```yaml
liteflow:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
    openai:
      api-key: your-openai-api-key
```

---

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

-----

## HelloWorld To LiteFlow-AI

[HelloWorld To LiteFlow-AI](liteflow-ai-helloworld/README.md)

## LiteFlow-AI 基本介绍

[LiteFlow-AI 基本介绍](docs/1.LiteFlowAI基本介绍.md)

