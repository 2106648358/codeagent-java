# AI 编程助手本地 CLI 架构方案（核心模块：CLI Agent）

> 目标：基于 Java 25 + Spring Boot 4 + Spring Shell + AgentScope Java 2 + JOOQ + PostgreSQL + SQLite + Maven 构建一个可扩展、低耦合、高内聚的本地 CLI AI 助手。当前阶段聚焦核心模块（CLI Agent）的架构设计。产品以代码辅助为主场景，但领域层抽象为通用文件/文档操作，便于扩展至通用本地文档处理。

---

## 1. 方案总览

采用 **多模块 Maven + 分层架构 + 端口适配器（Hexagonal）** 模式，将 CLI、业务用例、领域模型与基础设施彻底解耦，确保：

- 领域层不依赖 Spring Shell、AgentScope、JOOQ、PostgreSQL 等具体技术。
- LLM Provider、本地存储、远程遥测、用户身份等均可插拔替换。
- 后续可平滑接入企业认证、更多模型、代码片段上传、通用文档处理等能力。

---

## 2. 模块结构

将当前单模块项目拆分为 3 个 Maven 子模块：

| 模块 | 坐标 | 职责 |
|------|------|------|
| `code-agent-java`（根） | `code-agent-java` | 聚合模块，无源码。统一依赖管理、插件版本、公共属性。 |
| `code-agent-java-core` | `code-agent-java-core` | 领域模型、应用层用例、领域端口（接口）。不依赖任何框架。 |
| `code-agent-java-infrastructure` | `code-agent-java-infrastructure` | 适配器：AgentScope Harness、LLM Provider 实现、文件系统仓库、JOOQ SQLite/PostgreSQL 仓库、遥测远程同步、身份存根。 |
| `code-agent-java-cli` | `code-agent-java-cli` | Spring Boot 4 + Spring Shell 4 可执行入口。包含 Shell 命令、CLI DTO、交互提示、输出渲染、`application.properties`。 |

依赖关系：

```
cli -> core
cli -> infrastructure
infrastructure -> core
```

不允许任何模块反向依赖 `cli`。

### 2.1 为什么单体应用还要拆三模块？

**这仍然是单体应用**（最终只有一个 Spring Boot 可执行进程），三模块只是代码内部的组织方式，不是微服务拆分。拆模块的核心目的是用 Maven 模块边界强制分层架构：

| 理由 | 说明 |
|------|------|
| **强制分层** | `core` 不依赖 Spring/AgentScope/JOOQ，`infrastructure` 只依赖 `core`，`cli` 只依赖上两层。越界引用会直接编译失败。 |
| **框架隔离** | 领域逻辑不会被 Spring Shell、AgentScope、PostgreSQL 驱动污染；未来换框架时影响范围最小。 |
| **可测试性** | 给 `core` 写单元测试不需要启动 Spring；应用服务测试只需 stub 端口。 |
| **未来扩展** | 后续加 REST API、Web UI、IDE 插件或企业认证模块时，可直接新增模块，无需重构。 |
| **构建缓存** | 修改 `cli` 的某个命令时，若 `core`/`infrastructure` 没变，则不需要重新编译。 |

代价：初期 pom.xml 多一点、目录深一层。当前架构目标强调“高内聚低耦合、扩展性强”，因此用多模块固化边界。

---

## 3. 包结构

基础包：`com.edianyun.codeagentjava`

```
com.edianyun.codeagentjava
├── cli
│   ├── command               # @ShellComponent：ChatCommand, GenerateCommand, ExplainCommand
│   ├── converter             # 参数转换器（Path、Language、Scope 等）
│   ├── input                 # 交互式提示、确认对话框
│   ├── output                # 控制台渲染、流式输出、diff 展示
│   └── dto                   # CLI 专用请求/响应 DTO
├── config
│   ├── CodeAgentProperties.java
│   ├── LlmProperties.java
│   ├── StorageProperties.java
│   └── TelemetryProperties.java
├── application
│   ├── port                  # 入站用例接口（CLI 直接调用）
│   │   ├── ChatUseCase.java
│   │   ├── GenerateUseCase.java
│   │   └── ExplainUseCase.java
│   ├── service               # 用例实现
│   │   ├── ChatService.java
│   │   ├── GenerateService.java
│   │   └── ExplainService.java
│   ├── dto                   # 应用层 DTO（优先使用 Java 25 record）
│   └── event                 # 应用/领域事件
├── domain
│   ├── model                 # 实体与值对象
│   │   ├── session           # Session, SessionId, UserId
│   │   ├── message           # Message, Role, Prompt
│   │   ├── content           # GenerationTask, ContentFragment, FileChange
│   │   ├── workspace         # WorkspaceContext, RelativePath, ScanOptions
│   │   └── telemetry         # TelemetryEvent, TokenUsage, ModelId
│   ├── repository            # 出站端口接口（仓库/适配器）
│   ├── service               # 纯领域逻辑（FileDiff、WorkspaceScan、PromptBuild、ContentExtract）
│   ├── exception             # 领域异常
│   └── event                 # 领域事件
├── infrastructure
│   ├── llm                   # LLM Provider 适配器
│   ├── agentscope            # AgentScope Harness / Agent / Workflow 适配器
│   ├── filesystem            # 本地文件仓库、写入器、扫描器
│   ├── persistence
│   │   ├── sqlite            # SQLite 本地存储 + JOOQ 仓库
│   │   └── postgres          # PostgreSQL 远程遥测 + JOOQ 仓库
│   ├── telemetry             # 指标采集与远程同步
│   ├── identity              # 用户身份解析存根
│   └── config                # Infrastructure Spring 配置类
└── shared
    ├── util                   # 小型工具（hash、文件类型检测）
    └── validation
```

---

## 4. 分层架构与依赖规则

| 层 | 职责 |
|----|------|
| 表现层（CLI） | Spring Shell 命令、参数解析、交互提示、流式输出。 |
| 应用层 | 用例编排、事务边界、DTO 映射、遥测事件记录。 |
| 领域层 | 业务实体、值对象、仓库端口、领域服务、业务不变量。 |
| 基础设施层 | 外部系统适配器：LLM、AgentScope、文件系统、SQLite、PostgreSQL。 |

依赖规则：由外向内

```
CLI -> Application -> Domain
Infrastructure -> Domain
```

领域层不依赖任何框架。

Spring 装配方式：
- 在 `infrastructure` 和 `cli` 中通过 `@Configuration` 将端口绑定到适配器实现。
- 应用服务为 Spring Bean，但仅使用 `@Transactional` 和仓库端口。
- 域类使用纯 Java（record/class），可在无 Spring 环境下单元测试。

---

## 5. 领域层通用化设计原则

领域层不假设“操作对象一定是代码”。统一抽象为**本地文件/文档内容**的通用操作：

- 读：`WorkspaceScanner` 扫描目录、`FileRepository` 读取文件。
- 写/改：`FileChange` 描述对某个文件的修改，`FileRepository` 应用变更。
- 生成：`GenerationTask` 产出 `ContentFragment`（内容片段），可落地为代码、Markdown、JSON、配置等。
- 解释：对任意文件内容生成说明，不区分代码或文档。
- Diff：`FileDiffService` 计算任意文本文件的前后差异。

代码辅助是第一个 CLI 命令场景，但不是领域层的硬编码语义。后续新增“文档润色”“配置生成”等命令时，可直接复用同一套领域模型。

---

## 6. 核心领域模型

### 6.1 实体与值对象

| 概念 | 说明 |
|------|------|
| `Session` | 会话范围，由 `sessionId` + `userId` 唯一标识，维护消息序列。 |
| `Message` | 一轮对话：角色（USER/ASSISTANT/SYSTEM/TOOL）、内容、时间戳、元数据。 |
| `GenerationTask` | 内容生成请求：需求、目标内容类型、约束、状态、时间戳。代码生成是其子类/特殊场景。 |
| `ContentFragment` | 拟议内容片段：文件路径、内容、说明。可表示代码、文档、配置等。 |
| `WorkspaceContext` | 用户工作目录快照：文件列表、选中文件、依赖线索、内容类型。 |
| `FileChange` | 对单个文件的变更：路径、旧内容、新内容、变更类型。 |
| `TelemetryEvent` | 遥测事件：命令类型、耗时、模型、token 数、错误标记等。 |
| `UserIdentity` | 租户/用户标识，当前由环境变量或存根提供。 |

### 6.2 值对象

- `SessionId`, `UserId`, `TenantId`
- `ModelId`（如 `openai:gpt-4.1`）
- `ProviderName`
- `RelativePath`, `FilePath`
- `ContentType`（如 `code`, `markdown`, `json`, `config`，由 `language` 与 `mimeType` 组合）
- `FileChange`（路径 + 旧内容 + 新内容）
- `Prompt`（文本 + 角色）
- `TokenUsage`（输入/输出 token）

### 6.3 领域服务

- `FileDiffService` — 计算任意文本文件变更差异。
- `WorkspaceScanner` — 从工作目录收集相关文件。
- `PromptBuilder` — 根据工作区上下文和任务组装 Prompt。
- `ContentFragmentExtractor` — 从 LLM 返回文本中解析内容块（代码块、文档段落等）。
- `ContentTypeResolver` — 根据文件扩展名/内容推断 `ContentType`。

---

## 7. 关键端口与适配器

### 7.1 LLM Provider（领域端口）

```java
public interface LlmProvider {
    LlmResponse generate(List<Prompt> prompts, LlmOptions options);
    Flux<LlmStreamChunk> stream(List<Prompt> prompts, LlmOptions options);
}
```

实现：

| 实现 | 说明 |
|------|------|
| `AgentScopeLlmProvider` | 默认实现。通过 AgentScope `ModelRegistry`/`ChatModel` 支持多模型字符串。 |
| `OpenAiLlmProvider` | 直接 OpenAI 客户端，用于 AgentScope 未覆盖的自定义端点。 |
| `StubLlmProvider` | 测试与离线演示使用。 |

### 7.2 Agent 编排器（领域端口）

```java
public interface AgentOrchestrator {
    Conversation chat(Session session, UserMessage message);
    GenerationTask generate(GenerationTask task, WorkspaceContext context);
    Explanation explain(ExplainRequest request, WorkspaceContext context);
}
```

实现：

- `AgentScopeOrchestrator` — 封装 `HarnessAgent` 及其子代理，将领域对象转换为 AgentScope 的 `RuntimeContext`、`UserMessage`、`AgentEvent` 等。

### 7.3 文件仓库（领域端口）

```java
public interface FileRepository {
    WorkspaceContext scan(Path workingDir, ScanOptions options);
    List<ContentFragment> readFiles(List<RelativePath> paths);
    void apply(FileChange change, WriteOptions options);
    void dryRun(List<FileChange> changes);
}
```

实现：`LocalFileSystemFileRepository` — 支持 dry-run、备份、diff。

### 7.4 本地存储

```java
public interface LocalStorage {
    void saveSession(Session session);
    Optional<Session> loadSession(SessionId id);
    void saveGenerationTask(GenerationTask task);
    Optional<GenerationTask> loadGenerationTask(UUID id);
    List<Message> listMessages(SessionId id);
}
```

实现：`JooqLocalStorage` — 基于 SQLite + JOOQ。

### 7.5 遥测采集器

```java
public interface TelemetryCollector {
    void record(TelemetryEvent event);
    List<TelemetryEvent> flush();
}
```

实现：

- `InMemoryTelemetryCollector` — 命令内缓冲。
- `SQLiteBufferedTelemetryCollector` — 持久化到本地 SQLite，离线时也可保留。

### 7.6 远程同步

```java
public interface RemoteSyncService {
    void sync(List<TelemetryEvent> events);
}
```

实现：`PostgreSqlRemoteSyncService` — 批量插入 PostgreSQL 遥测表。

### 7.7 用户身份解析

```java
public interface UserIdentityResolver {
    UserIdentity resolve();
}
```

实现：

- `EnvironmentUserIdentityResolver` — 读取 `CODEAGENT_USER_ID` / `CODEAGENT_TENANT_ID`。
- 预留：`EnterpriseAuthUserIdentityResolver`（后续接入企业认证）。

---

## 8. MVP 命令数据流

### 8.1 `chat`

1. CLI 解析参数（可选 `sessionId`、用户输入）。
2. `ChatCommand` 调用 `ChatUseCase.chat(ChatRequest)`。
3. `ChatService` 通过 `LocalStorage` 加载或创建 `Session`。
4. `ChatService` 解析 `UserIdentity`，将上下文交给 `AgentOrchestrator`。
5. `AgentScopeOrchestrator` 将用户消息转发给 `HarnessAgent`。
6. LLM 返回助手回复（流式或阻塞）。
7. `ChatService` 将新的 `Message` 对持久化到 `LocalStorage`。
8. CLI 渲染回复（流式分块或完整文本）。
9. `TelemetryCollector` 记录 `command=chat`、耗时、模型、token 用量。

### 8.2 `generate`

1. CLI 解析参数：需求、目标文件、内容类型（可选）。
2. `GenerateCommand` 调用 `GenerateUseCase.generate(GenerateRequest)`。
3. `GenerateService` 创建 `GenerationTask`。
4. `WorkspaceScanner` 构建 `WorkspaceContext`（文件列表、选中文件、依赖线索）。
5. `PromptBuilder` 组装生成 Prompt（代码场景时附带语言/框架上下文；文档场景时附带格式要求）。
6. `AgentOrchestrator` 调用 LLM / 内容生成子代理。
7. 解析返回的 `ContentFragment` 列表（若 LLM 返回原始文本，则通过 `ContentFragmentExtractor` 解析）。
8. `FileDiffService` 计算差异。
9. CLI 展示 diff 并请求确认（除非 `--yes`）。
10. `FileRepository.apply(...)` 写入文件并备份。
11. `GenerateService` 将任务与片段持久化到 SQLite。
12. `TelemetryCollector` 记录 `command=generate`、文件数、内容类型、模型、token 用量、状态。

### 8.3 `explain`

1. CLI 解析目标：文件路径、符号名或选中范围。
2. `ExplainCommand` 调用 `ExplainUseCase.explain(ExplainRequest)`。
3. `ExplainService` 读取目标文件到 `WorkspaceContext`。
4. `PromptBuilder` 构建解释 Prompt（如“解释这段代码”或“总结这份文档”）。
5. `AgentOrchestrator` 发送 Prompt 给 LLM。
6. LLM 返回解释。
7. CLI 渲染解释（支持 Markdown 终端输出）。
8. `TelemetryCollector` 记录 `command=explain`、内容类型、模型、token 用量。

---

## 9. AgentScope Java 2 集成策略

将 AgentScope 2 隔离在 `AgentOrchestrator` 适配器后，领域代码不直接依赖 AgentScope 类。

### 9.1 运行时

- 启动时由 `AgentScopeAgentFactory` 构建一个主 `HarnessAgent`。
- 使用 AgentScope `RuntimeContext` 承载每次调用的 session/user 隔离信息。
- 模型字符串来自配置：`codeagent.llm.model=openai:gpt-4.1` 或 `dashscope:qwen-plus` 等。
- 工作区目录：`~/.codeagent/workspace/` 或项目内 `.codeagent/workspace/`。

### 9.2 子代理策略（第一阶段简化）

第一阶段将 `chat`/`generate`/`explain` 作为同一 `HarnessAgent` 的不同 Prompt 模板处理，降低复杂度。后续再拆分为：

- `PlannerAgent` — 拆分生成步骤。
- `ContentAgent` — 产出 `ContentFragment`。
- `ExplainerAgent` — 产出解释。

### 9.3 消息与流式

- 阻塞命令：`agent.call(UserMessage, runtimeContext)`。
- 流式命令：`agent.streamEvents(...)`，将 `TextBlockDeltaEvent` 等映射为应用层事件，CLI 实时打印 token。

### 9.4 状态与记忆

- 第一阶段使用 AgentScope 默认的 `JsonFileAgentStateStore`。
- 预留 `SqliteAgentStateStore` 实现，未来将 Agent 状态统一存入 SQLite。

### 9.5 中间件

通过 `HarnessAgent.builder().middleware(...)` 添加：

- 每次 LLM 调用前后记录遥测。
- 安全护栏：禁止写入工作目录之外的路径。

---

## 10. SQLite 本地存储

### 10.1 定位

SQLite 是 **本地元数据、缓存和离线缓冲**：

- 会话与对话历史。
- 生成任务记录与生成片段。
- 工作区文件索引与校验和。
- 遥测事件缓冲（远程 PostgreSQL 不可用时）。
- 用户身份缓存。
- 未来可扩展为 AgentScope 状态存储。

### 10.2 建议表

| 表 | 用途 |
|----|------|
| `session` | `session_id`, `user_id`, `tenant_id`, `title`, `created_at`, `updated_at` |
| `message` | `id`, `session_id`, `role`, `content_hash`, `content`, `model_id`, `token_usage`, `created_at` |
| `generation_task` | `id`, `session_id`, `status`, `requirements`, `content_type`, `created_at`, `completed_at` |
| `content_fragment` | `id`, `task_id`, `file_path`, `content_hash`, `content`, `content_type`, `applied_at` |
| `workspace_file` | `path`, `last_modified`, `checksum`, `content_type`, `indexed_at` |
| `local_cache_kv` | 通用键值缓存（Prompt 缓存、嵌入等） |
| `telemetry_buffer` | 尚未同步到 PostgreSQL 的事件 |
| `identity` | 缓存 `user_id`, `tenant_id`, `machine_id`, `resolved_at` |

**隐私说明**：`message` 和 `content_fragment` 中的 `content` 仅本地保存；远程同步不会发送 `content` 或 `content_hash`，除非后续显式开启内容上传功能。

---

## 11. PostgreSQL 远程遥测

### 11.1 定位

PostgreSQL 是 **跨开发者统计数据的中心仓库**，第一阶段仅收集指标，不存储实际内容。

### 11.2 建议表

| 表 | 用途 |
|----|------|
| `tenant` | `tenant_id`, `name`, `created_at`（为企业认证预留） |
| `developer` | `developer_id`, `tenant_id`, `external_user_id`, `first_seen_at` |
| `session_metrics` | `session_id`, `developer_id`, `tenant_id`, `command_type`, `started_at`, `duration_ms`, `model_id`, `provider`, `status` |
| `llm_usage` | `session_id`, `model_id`, `input_tokens`, `output_tokens`, `cost_estimate` |
| `feature_event` | `event_type`, `session_id`, `developer_id`, `content_type`, `file_extension`, `file_count`, `payload_json`（非敏感） |
| `content_fragment_meta` | **预留未来上传**：`fragment_id`, `developer_id`, `content_type`, `file_extension`, `size_bytes`, `fragment_hash`, `uploaded_at`, `retention_policy`；**不包含 `content` 列** |
| `sync_checkpoint` | `developer_id`, `last_sync_at`, `cursor` |

所有表均保留 `tenant_id` 和 `developer_id`，为企业多租户认证预留。

---

## 12. JOOQ 策略

### 12.1 代码生成

在 `code-agent-java-infrastructure` 模块中使用 `jooq-codegen-maven` 插件，配置两个独立执行：

- `sqlite-codegen`：基于本地 SQLite schema。
- `postgres-codegen`：基于远程 PostgreSQL schema。

生成到不同包：

- `com.edianyun.codeagentjava.infrastructure.persistence.sqlite.jooq`
- `com.edianyun.codeagentjava.infrastructure.persistence.postgres.jooq`

### 12.2 仓库实现

- `JooqLocalStorage` 使用 SQLite DSL 实现 `LocalStorage`。
- `JooqRemoteMetricsRepository` 使用 PostgreSQL DSL 实现 `RemoteSyncService`。
- 仅 `persistence` 包内部持有 `DSLContext`。

### 12.3 数据源

配置两个 `DataSource` Bean：

- `sqliteDataSource`
- `postgresDataSource`

对应两个 `DSLContext` Bean。

### 12.4 数据库迁移

- PostgreSQL：使用 **Flyway** 管理 schema 迁移。
- SQLite：在 `src/main/resources/schema-sqlite.sql` 中定义建表 SQL，启动时通过 `JdbcTemplate` 执行。

---

## 13. 配置方案

使用 `@ConfigurationProperties(prefix = "codeagent")` 统一管理配置。

### 13.1 建议配置项

```properties
# Identity（为企业认证预留）
codeagent.identity.user-id=${CODEAGENT_USER_ID:anonymous}
codeagent.identity.tenant-id=${CODEAGENT_TENANT_ID:default}
codeagent.identity.resolver=environment

# LLM
codeagent.llm.provider=agentscope
codeagent.llm.model=openai:gpt-4.1
codeagent.llm.temperature=0.2
codeagent.llm.max-tokens=4096
codeagent.llm.timeout-seconds=60
codeagent.llm.openai.api-key=${OPENAI_API_KEY}
codeagent.llm.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1}
codeagent.llm.dashscope.api-key=${DASHSCOPE_API_KEY}

# AgentScope
codeagent.agent.workspace=${user.home}/.codeagent/workspace
codeagent.agent.compaction.enabled=true
codeagent.agent.compaction.trigger-messages=30
codeagent.agent.compaction.keep-messages=10

# Storage
codeagent.storage.sqlite.path=${user.home}/.codeagent/codeagent.db
codeagent.storage.sqlite.pool-size=2

# Telemetry
codeagent.telemetry.enabled=true
codeagent.telemetry.postgres.url=${CODEAGENT_TELEMETRY_URL}
codeagent.telemetry.postgres.user=${CODEAGENT_TELEMETRY_USER}
codeagent.telemetry.postgres.password=${CODEAGENT_TELEMETRY_PASSWORD}
codeagent.telemetry.batch-size=100
codeagent.telemetry.flush-interval-seconds=30

# CLI
codeagent.cli.stream-output=true
codeagent.cli.confirm-writes=true
```

### 13.2 配置原则

- 所有密钥通过环境变量注入，不提交到仓库。
- 开发时可通过 `.env` + `spring-dotenv`（可选）加载本地配置。

---

## 14. 测试策略

### 14.1 单元测试

- 纯领域逻辑：`FileDiffService`、`PromptBuilder`、`WorkspaceScanner`、值对象校验。
- 使用 JUnit 5 + AssertJ + Mockito。

### 14.2 应用服务测试

- 使用 Stub 实现 `LlmProvider`、`FileRepository`、`LocalStorage` 测试 `ChatService`、`GenerateService`、`ExplainService`。

### 14.3 Spring Shell 命令测试

- 使用 `spring-shell-starter-test` + `SpringShellTest` 测试参数解析、校验和输出渲染。

### 14.4 基础设施测试

- SQLite：使用内存 SQLite 实例测试 JOOQ 仓库。
- PostgreSQL：使用 Testcontainers `postgres:17`。
- LLM Provider：使用 WireMock 桩 HTTP 调用。
- AgentScope：使用 `StubLlmProvider` 或本地 Ollama 容器避免 CI 中调用云 API。

### 14.5 集成测试

- `CliIntegrationTest`：启动 Spring Shell 上下文，将所有基础设施 Bean 替换为 stub，验证 `chat`/`generate`/`explain` 端到端流程。

---

## 15. 需要添加的依赖

### 15.1 根模块（dependencyManagement）

- `org.jooq:jooq-bom` 或显式声明 jooq 版本。
- `org.testcontainers:testcontainers-bom`（测试）。
- `org.flywaydb:flyway-bom`（可选）。

### 15.2 `core` 模块

- `org.projectlombok:lombok`（可选，Java 25 record 优先）。
- `org.apache.commons:commons-lang3`。
- `org.apache.commons:commons-io`（可选）。

### 15.3 `infrastructure` 模块

- `io.agentscope:agentscope-harness`（已存在）。
- `io.agentscope:agentscope-core`（如需直接访问 `ModelRegistry`）。
- `org.jooq:jooq`
- `org.jooq:jooq-meta`
- `org.jooq:jooq-codegen`（maven plugin 依赖）
- `org.postgresql:postgresql`
- `org.xerial:sqlite-jdbc`
- `com.zaxxer:HikariCP`
- `org.springframework.boot:spring-boot-starter-jdbc`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.flywaydb:flyway-core`（PostgreSQL 迁移）
- `org.flywaydb:flyway-database-postgresql`（PostgreSQL 17+）

### 15.4 `cli` 模块

- `org.springframework.shell:spring-shell-starter`（已存在）。
- `org.springframework.shell:spring-shell-starter-test`（已存在）。
- `org.springframework.boot:spring-boot-starter`。
- `org.springframework.boot:spring-boot-starter-test`（已存在）。

### 15.5 测试依赖

- `org.testcontainers:postgresql`
- `org.testcontainers:junit-jupiter`
- `org.wiremock:wiremock-standalone`
- `org.mockito:mockito-core`
- `org.assertj:assertj-core`

---

## 16. 关键待改文件清单

| 文件 | 操作 |
|------|------|
| `pom.xml` | 改为聚合模块，添加 dependencyManagement 与三个子模块。 |
| `code-agent-java-core/pom.xml` | 新建，纯领域模块。 |
| `code-agent-java-infrastructure/pom.xml` | 新建，包含 AgentScope、JOOQ、DB 驱动。 |
| `code-agent-java-cli/pom.xml` | 新建，Spring Boot 可执行模块。 |
| `src/main/java/com/edianyun/codeagentjava/CodeAgentJavaApplication.java` | 移动到 `code-agent-java-cli` 模块。 |
| `src/main/resources/application.properties` | 移动到 `code-agent-java-cli`，并扩展为完整配置。 |
| `src/test/java/.../CodeAgentJavaApplicationTests.java` | 移动到 `code-agent-java-cli` 模块。 |

---

## 17. 实施路线图（建议）

1. **第 1 步：工程结构** — 拆分多模块，调整根 `pom.xml`，移动 `CodeAgentJavaApplication` 与 `application.properties`。
2. **第 2 步：领域层** — 定义 `domain/model` 通用实体、值对象、领域异常与端口接口。
3. **第 3 步：应用层** — 定义 `ChatUseCase`/`GenerateUseCase`/`ExplainUseCase` 接口及实现，引入 `TelemetryCollector` 事件记录。
4. **第 4 步：基础设施层** — 实现 `LocalFileSystemFileRepository`、`AgentScopeOrchestrator`（默认 Provider 使用 OpenAI）、`EnvironmentUserIdentityResolver`。
5. **第 5 步：SQLite 持久化** — 引入 JOOQ + SQLite，建库，实现 `JooqLocalStorage`，本地保存会话/任务/内容片段。
6. **第 6 步：CLI 命令** — 实现 `chat`/`generate`/`explain` 三个 Spring Shell 命令，包含参数解析、交互确认、输出渲染。
7. **第 7 步：PostgreSQL 遥测** — 引入 JOOQ + PostgreSQL + Flyway，定义遥测表，实现 `PostgreSqlRemoteSyncService` 与后台同步。
8. **第 8 步：测试与打磨** — 单元测试、应用服务测试、JOOQ 仓库测试、Shell 命令测试、集成测试。

---

## 18. 需要用户确认/决策的默认假设

以下为本方案采用的默认假设，如需调整请反馈：

1. **多模块拆分**：按 `core` / `infrastructure` / `cli` 三模块拆分。当前项目为空，拆分成本低，且利于后续扩展。
2. **领域通用化**：领域层抽象为通用文件/文档操作（`ContentFragment`、`FileChange`、`WorkspaceContext`），代码生成只是首要场景。CLI 命令仍可命名为 `generate`/`explain`，但内部模型不硬编码代码语义。
3. **默认 LLM Provider**：`AgentScopeLlmProvider` + 默认模型 `openai:gpt-4.1`。可替换为 `dashscope:qwen-plus` 或 `anthropic:claude-3-5-sonnet`。
4. **SQLite 范围**：第一阶段仅用于本地元数据、会话历史、任务记录和遥测缓冲；不替换 AgentScope 默认状态存储。
5. **远程遥测**：仅上传非敏感指标（命令类型、耗时、模型、token、内容类型、文件扩展名、文件数）；不上传文件内容。
6. **用户身份**：第一阶段通过环境变量 `CODEAGENT_USER_ID` / `CODEAGENT_TENANT_ID` 解析；为企业认证预留接口。

如确认以上假设，即可进入实现阶段。
