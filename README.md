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
@AIComponent()
@AIChat()
@AIInput()
@AIOutput()
public interface AICmp {}
```

共计 4 大注解类型：

1. `@AIComponent`：标记这是一个 AI 组件, 同时配置 AI 组件的基础信息
2. `@AIChat` & `@AIClassify`：标记 AI 组件的具体类型、参数等
3. `@AIInput`：配置 AI 组件的输入参数
4. `@AIOutput`：配置 AI 组件的输出参数


下面分别介绍各个注解的具体使用方式


### 1. @AIComponent

`@AIComponent` 注解用于标记一个 AI 组件，并配置该组件的基础信息。它包含以下属性：

### 1. AIChat



### 2. AIClassify

