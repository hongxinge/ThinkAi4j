# ThinkAi4j

**简单、强大、开箱即用的 Spring Boot 3 AI 大模型开发框架**

[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
- **Java**: 17+
- **Maven**: 3.6.3+
- **Spring Boot**: 3.2+
- **Gitee**: [https://gitee.com/hongxinge/think-ai4j](https://gitee.com/hongxinge/think-ai4j)
- **GitHub**: [https://github.com/hongxinge/ThinkAi4j](https://github.com/hongxinge/ThinkAi4j)

## 特性

- **开箱即用** - Ollama 本地模型零配置，云模型 1 行配置即可使用
- **多模型支持** - 豆包、通义千问、智谱、百度、腾讯、Kimi、DeepSeek、MiniMax、OpenAI 等所有 OpenAI 兼容模型
- **自由切换** - 配置或代码中随时切换模型，无需改业务代码
- **流式输出** - 支持 Flux<String>、SSE 多种流式格式
- **对话记忆** - 内置内存记忆，支持 Redis 持久化
- **工具调用** - @AiTool 注解即可让 AI 调用你的方法
- **RAG 增强** - 文档问答、知识库检索增强生成
- **Agent 框架** - 智能代理，自动调用工具完成任务
- **OpenAI 标准** - 完全兼容 OpenAI API 规范
- **MIT 协议** - 完全免费，可自由商用

## 架构优势

### 通用兼容架构

ThinkAi4j 采用**通用兼容 + 特殊适配**的设计：

- **1 个通用模块** `think-ai4j-provider-openai-compat` - 支持所有 OpenAI 兼容 API 的大模型
- **配置即接入** - 新增模型只需修改配置文件，无需编写代码

### 支持的 AI 模型

| 模型 | 提供商名称 | 兼容 OpenAI | 状态 |
|------|-----------|------------|------|
| **豆包 (Doubao)** | `doubao` | ✅ 是 | ✅ |
| **百度文心 (Qianfan)** | `qianfan` | ✅ 是 | ✅ |
| **腾讯混元 (Hunyuan)** | `hunyuan` | ✅ 是 | ✅ |
| **Kimi (Moonshot)** | `moonshot` | ✅ 是 | ✅ |
| **智谱 GLM** | `glm` | ✅ 是 | ✅ |
| **MiniMax** | `minimax` | ✅ 是 | ✅ |
| **DeepSeek** | `deepseek` | ✅ 是 | ✅ |
| **通义千问 (Qwen)** | `qwen` | ✅ 是 | ✅ |
| **Ollama 本地** | `ollama` | ✅ 是 | ✅ |
| **OpenAI GPT** | `openai` | ✅ 是 | ✅ |

> 所有符合 OpenAI API 规范的模型都可通过配置接入

## 快速开始

### 第 1 步：克隆仓库

```bash
git clone https://gitee.com/hongxinge/think-ai4j.git
cd think-ai4j
```

### 第 2 步：本地安装

> **环境要求**：Java 17+、Maven 3.6.3+

```bash
# Windows (PowerShell)
$env:JAVA_HOME="你的JDK路径"
mvn clean install -DskipTests
```

安装后，所有模块即可在本地 Maven 仓库中使用。

### 第 3 步：在项目中引入

在你的 Spring Boot 项目 `pom.xml` 中添加：

```xml
<!-- Spring Boot Starter（自动装配） -->
<dependency>
    <groupId>com.thinkai4j</groupId>
    <artifactId>think-ai4j-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> 框架已内置 Spring Boot Starter，引入后自动生效，无需额外配置。

### 第 4 步：配置模型

```yaml
think:
  ai:
    default-provider: doubao
    compat:
      providers:
        - name: doubao
          baseUrl: https://ark.cn-beijing.volces.com/api/v3
          apiKey: 你的API密钥
          model: 你的模型ID

        # 可同时配置多个模型
        # - name: moonshot
        #   baseUrl: https://api.moonshot.cn/v1
        #   apiKey: 你的API密钥
        #   model: moonshot-v1-8k

        # - name: glm
        #   baseUrl: https://open.bigmodel.cn/api/paas/v4
        #   apiKey: 你的API密钥
        #   model: glm-4

      httpClient:
        connectionPool:
          max-idle-connections: 50
          keep-alive-minutes: 5
        timeout:
          connect-seconds: 30
          read-seconds: 60
          write-seconds: 30

    memory:
      type: memory      # memory=内存 | redis=持久化
      max-messages: 20
```

> **Ollama 本地模型零配置**：只要本地安装了 Ollama（默认端口 11434），无需任何配置即可使用。

### 第 5 步：开始使用

#### 简单对话

```java
@Autowired
private AiChat chat;

String result = chat.ask("你好");
```

#### 带系统提示词

```java
String result = chat.system("你是Java专家").ask("如何设计单例模式？");
```

#### 流式输出

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String q) {
    return chat.stream(q);
}
```

#### 切换模型

```java
String result = chat.provider("glm").ask("你好");
```

#### 对话记忆（Redis 持久化）

```yaml
think:
  ai:
    memory:
      type: redis
      max-messages: 50
      ttl-minutes: 120
```

```java
@Autowired
private ChatMemory memory;

memory.addMessage("user-123", AiMessage.user("我叫小明"));
List<AiMessage> history = memory.getMessages("user-123");
```

#### 工具调用

```java
@Component
public class WeatherTool {

    @AiTool("查询天气")
    public String getWeather(
        @ToolParam(description = "城市名称") String city
    ) {
        return "晴天，25度";
    }
}
```

#### RAG 文档问答

```java
@Autowired
private RagPipeline ragPipeline;

List<Document> docs = List.of(
    new Document("公司规定年假为15天"),
    new Document("加班费按每小时100元计算")
);
ragPipeline.ingest(docs);

String answer = ragPipeline.query("年假有多少天？");
```

#### Agent 智能代理

```java
Agent agent = new Agent("助手", "你是一个专业的助手", chat)
    .addToolBean(new WeatherTool())
    .addToolBean(new SearchTool());

String result = agent.execute("北京天气如何？");
```

## API 文档

| 方法 | 说明 | 示例 |
|------|------|------|
| `chat.ask(q)` | 同步对话 | `chat.ask("你好")` |
| `chat.system(s).ask(q)` | 带系统提示词 | `chat.system("你是专家").ask("问题")` |
| `chat.provider(p).ask(q)` | 指定模型 | `chat.provider("glm").ask("问题")` |
| `chat.stream(q)` | 流式输出(Flux) | `chat.stream("问题")` |
| `chat.temperature(t).ask(q)` | 控制创造性 | `chat.temperature(0.7).ask("写诗")` |

## 项目结构

```
think-ai4j/
├── think-ai4j-core/                    # 核心模块
├── think-ai4j-provider-openai-compat/  # 通用兼容模块（支持所有OpenAI格式模型）
├── think-ai4j-memory/                  # 内存记忆
├── think-ai4j-memory-redis/            # Redis 持久化记忆
├── think-ai4j-tool/                    # 工具调用
├── think-ai4j-rag/                     # RAG 检索增强
├── think-ai4j-agent/                   # Agent 框架
├── think-ai4j-observability/           # 可观测性/指标采集
├── think-ai4j-store-pgvector/          # PgVector 向量存储
├── think-ai4j-spring-boot-starter/     # Spring Boot 自动配置
├── think-ai4j-example/                 # 示例项目
└── think-ai4j-test/                    # 测试模块
```

## 构建

> **环境要求**：Java 17+、Maven 3.6.3+、Spring Boot 3.2+

```bash
set JAVA_HOME=D:\JavaSdk\sdk-17
D:\maven\apache-maven-3.9.9\bin\mvn.cmd clean install
```

## 运行示例

```bash
cd think-ai4j-example
set JAVA_HOME=D:\JavaSdk\sdk-17
D:\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

然后访问：
- 同步对话：http://localhost:8080/api/chat/ask?q=你好
- 流式输出：http://localhost:8080/api/chat/stream?q=你好

## 许可证

[MIT License](LICENSE)
