# Phase 1: 骨架搭建

## 目标

Spring Boot 项目跑起来，LangChain4j + DeepSeek 连接成功。

## 关键决策

### 1. 选 LangChain4j 而不是 Spring AI

| | LangChain4j | Spring AI |
|---|---|---|
| 社区 | 活跃，文档全 | Spring 官方，稳但慢 |
| 概念覆盖 | Planning/Memory/Tool 齐全 | 功能较少 |
| JD 匹配度 | 三个 JD 提到的概念全覆盖 | 一般 |
| 学习曲线 | 稍高 | 低（Spring 风格） |

选 LangChain4j 因为它概念覆盖最全。

### 2. 选 OpenAI 端点而不是 Anthropic

最初用 `api.deepseek.com/anthropic` + `langchain4j-anthropic`，因为 Claude Code 走这条路验证过。但实际测试中发现返回格式不兼容（enum 反序列化失败），换到 `api.deepseek.com/v1` + `langchain4j-open-ai`。OpenAI 协议是 DeepSeek 最成熟的兼容端点。

### 3. 手动配置而不是 Spring Boot Starter

手写 `@Configuration` + `@Bean`，因为：
- 从 Moon Bridge 配置读 API Key 需要自定义逻辑
- 出问题能快速定位

### 4. API Key 安全

不从环境变量读，不从 application.yml 读，不从 `.env` 读——从仓库外的 `~/tools/moon-bridge/config.yml` 读取。`.gitignore` 加了密钥防护规则。

## 依赖

```xml
Spring Boot 3.3.5     → Java 17 基线
langchain4j 0.36.2    → AI 框架
langchain4j-open-ai   → OpenAI 协议连接 DeepSeek
```

## 踩坑

- **JDK 版本**：本机只有 JDK 11 + Corretto 8，Spring Boot 3.x 需要 JDK 17。下载 Amazon Corretto 17 解压到 `~/myworkspace/tools/java17/`
- **Maven**：首次下载依赖慢，配置阿里云镜像解决
- **YAML 结构不统一**：两个 Moon Bridge 配置文件结构不同（一个 `provider.providers`，一个 `providers`），解析逻辑做了兼容

## 验证

```bash
mvn compile          # BUILD SUCCESS
mvn spring-boot:run  # 8080 端口监听
```
