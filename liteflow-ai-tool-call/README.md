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
