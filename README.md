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
- **Agent 框架** - 智能代理，支持多轮工具调用、长期记忆、多Agent协作
- **内置 Skill** - 文件操作、HTTP请求、数据库查询、时间日期、记忆管理等5大Skill
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

### 方式一：Maven 依赖（推荐，Maven Central 已发布）

只需在你的 Spring Boot 项目 `pom.xml` 中添加：

```xml
<!-- Spring Boot Starter（自动装配） -->
<dependency>
    <groupId>com.hongxinge</groupId>
    <artifactId>think-ai4j-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

> 框架已内置 Spring Boot Starter，引入后自动生效，无需额外配置。Maven 会自动从中央仓库下载，零配置即可使用。

### 方式二：克隆源码（适合二次开发/贡献者）

```bash
git clone https://gitee.com/hongxinge/think-ai4j.git
# 或 GitHub: https://github.com/hongxinge/ThinkAi4j.git
cd think-ai4j

# 编译并安装到本地 Maven 仓库
# Windows (PowerShell)
$env:JAVA_HOME="你的JDK路径"
mvn clean install -DskipTests
```

然后在你的项目 `pom.xml` 中引入依赖（与方式一相同）：

```xml
<dependency>
    <groupId>com.hongxinge</groupId>
    <artifactId>think-ai4j-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

> `mvn clean install` 会将 1.0.2 版本安装到你本地的 Maven 仓库，之后你的项目就可以正常引用了。

### 配置模型

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

### 开始使用

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

#### Agent 长期记忆

```java
ChatMemory memory = new InMemoryChatMemory();
AgentLongTermMemory longTermMemory = new AgentLongTermMemory("assistant-1", memory);

// 记住关键信息
longTermMemory.rememberFact("用户姓名：张三");
longTermMemory.rememberFact("用户偏好：Java开发");

Agent agent = new Agent("助手", "你是专业助手", chat)
    .longTermMemory(longTermMemory);
```

#### 多Agent协作

```java
AgentBus bus = new AgentBus();
bus.register("研究员", researcherAgent);
bus.register("写手", writerAgent);
bus.register("审核员", reviewerAgent);

// 链式执行：研究员->写手->审核员
String report = bus.chainExecute(
    List.of("研究员", "写手", "审核员"),
    "研究AI趋势并写报告"
);

// 并行执行
String results = bus.parallelExecute(Map.of(
    "研究员", "研究技术趋势",
    "写手", "写文章摘要"
));
```

#### @AiAgent 注解（Spring Boot 自动装配）

Spring Boot 环境下，给任意 Bean 标注 `@AiAgent`，容器启动时自动创建同名 Agent
（自动注入 `AiChat`，类内 `@AiTool` 方法自动注册为工具）：

```java
@Component
@AiAgent(name = "weatherAgent", description = "你是一个专业的天气查询助手")
public class WeatherAgent {

    @AiTool("查询天气")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        return "晴天，25度";
    }
}
```

启动后通过 `ApplicationContext` 获取自动创建的 Agent：

```java
@Autowired
private ApplicationContext context;

Agent agent = (Agent) context.getBean("thinkAi4jAgent:weatherAgent");
String result = agent.execute("北京天气如何？");
```

#### 可观测性（Micrometer 指标）

引入 `spring-boot-starter-actuator` 后，框架自动把请求总数、错误数、耗时、
Token 消耗等指标（`think.ai.requests.total` / `think.ai.errors.total` /
`think.ai.tokens.total` / `think.ai.request.duration`）挂进 AiChat 调用链，
无需编写任何代码：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

配合 Prometheus / Grafana 即可监控所有 AI 调用。

#### PgVector 向量存储（生产级 RAG）

`PgVectorStore` 支持通过 `EmbeddingProvider` 自动生成向量，
写入与检索均基于真实向量语义匹配：

```java
// embeddingProvider 为你实现的向量化接口（如接入各厂商 Embedding API）
PgVectorStore store = new PgVectorStore(jdbcTemplate, "ai_documents", 1024, embeddingProvider);

// 写入文档（自动向量化）
store.addDocuments(List.of(new Document("公司规定年假为15天")));

// 语义检索（查询文本自动向量化后余弦排序）
List<Document> hits = store.search("年假有几天", 3);
```

> 未提供 `EmbeddingProvider` 时，写入时必须由 `Document.setEmbedding()` 预置向量，
> 语义检索会给出明确错误提示，不会静默产生错误结果。

#### 内置 Skill - 文件操作

```java
FileSkill fileSkill = new FileSkill("/workspace");

// 读写文件
fileSkill.writeFile("notes.txt", "重要笔记");
String content = fileSkill.readFile("notes.txt");

// 列出目录
String dirList = fileSkill.listDirectory("");
```

#### 内置 Skill - HTTP 请求

```java
HttpSkill httpSkill = new HttpSkill()
    .setDefaultHeader("Authorization", "Bearer token");

// GET 请求
String response = httpSkill.httpGet("https://api.example.com/data", null);

// POST 请求
String postResult = httpSkill.httpPost(
    "https://api.example.com/submit",
    null,
    "{\"key\": \"value\"}"
);
```

#### 内置 Skill - 数据库查询

```java
// 使用 H2 内存数据库示例
DatabaseSkill dbSkill = new DatabaseSkill(
    "jdbc:h2:mem:testdb", "sa", ""
);

// SQL 查询
String results = dbSkill.executeQuery("SELECT * FROM users");

// 获取表列表
String tables = dbSkill.listTables();

// 查看表结构
String schema = dbSkill.describeTable("users");
```

#### 内置 Skill - 时间日期

```java
TimeSkill timeSkill = new TimeSkill();

// 获取当前时间
String now = timeSkill.getCurrentDateTime("Asia/Shanghai");
String date = timeSkill.getCurrentDate();
String time = timeSkill.getCurrentTime();

// 格式化时间戳
String formatted = timeSkill.formatTimestamp(1700000000, "yyyy-MM-dd HH:mm:ss");
```

#### 内置 Skill - 记忆管理

```java
MemorySkill memorySkill = new MemorySkill();

// 记住信息
memorySkill.remember("用户名", "张三");
memorySkill.remember("年龄", "25");

// 查询记忆
String name = memorySkill.recall("用户名");

// 查询所有记忆
String allMemories = memorySkill.recall(null);

// 忘记信息
memorySkill.forget("年龄");

// 清空记忆
memorySkill.clearMemory();
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
├── think-ai4j-skill/                   # 内置Skill（文件、HTTP、数据库、时间、记忆）
├── think-ai4j-rag/                     # RAG 检索增强
├── think-ai4j-agent/                   # Agent 框架（支持长期记忆、多Agent协作）
├── think-ai4j-observability/           # 可观测性/指标采集
├── think-ai4j-store-pgvector/          # PgVector 向量存储
├── think-ai4j-spring-boot-starter/     # Spring Boot 自动配置
├── think-ai4j-example/                 # 示例项目
└── think-ai4j-test/                    # 测试模块（157个测试用例，全量覆盖）
```

## 构建（开发者）

> 普通用户直接使用 Maven 依赖即可，无需克隆源码。以下适合二次开发/贡献者。

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

## 更新日志

### 1.0.2（2026-09-26）

**缺陷修复（企业级健壮性专项）**

- **[重要] 修复配置的模型名被覆盖的问题**：此前 `ask()`/`stream()`/Agent 调用链会把
  Provider 名称（如 `doubao`）误当作模型名发送给 API，导致真实模型配置不生效。
  现在 `ChatRequest.provider` 用于选择 Provider，`model` 专用于指定模型名，
  两者语义彻底分离（未指定 model 时自动使用配置的默认模型）
- **修复 system() 重复堆叠**：链式多次调用 `system()` 现在是替换语义（仅保留最新一条系统提示词）
- **修复 PgVector 向量存储**：写入 embedding 不再为空，检索不再按文本反查；
  接入 `EmbeddingProvider` 实现真实语义检索，并增加向量维度校验与表名安全校验
- **修复流式对话不落记忆**：`stream()` 与 `ask()` 行为一致，对话自动写入记忆
- **修复 Redis 记忆并发丢失**：改用 Redis List + Lua 脚本原子追加，
  高并发场景不再出现消息互相覆盖
- **修复 AgentBus.parallelExecute 假并行**：改为真实并发执行，全部完成后汇总
- **修复工具调用复杂类型参数**：`@AiTool` 方法支持 POJO / List / Map 参数自动转换
- **加固 HTTP Skill SSRF 防护**：在 DNS 解析处校验实际连接 IP，消除 DNS 重绑定绕过窗口，
  并补全 IPv6 内网地址拦截
- **修复测试模块包声明错误**导致的 `NoClassDefFoundError`
- **修复 HttpSkill.allowHost** 传入不可变集合时的 `UnsupportedOperationException`

**新增功能**

- `@AiAgent` 注解：Spring Boot 容器自动扫描并装配 Agent
- 可观测性自动装配：引入 actuator 即自动采集 AI 调用指标（请求/错误/耗时/Token）
- `ChatRequest.builder().provider(...)`：Builder 支持指定 Provider
- `Document` 支持携带 embedding 向量

### 1.0.1

- 首个 Maven Central 发布版本

## 许可证

[MIT License](LICENSE)
