# 项目实施任务清单

> 按优先级排列的任务清单。已完成的任务保留用于追踪整体进度，待完成任务按阶段展开。

---

## 已完成

### 阶段一：架构设计与文档
- [x] 确定技术栈与约束：Java 25 + Spring Boot 4 + Spring Shell + AgentScope Java 2 + JOOQ + PostgreSQL + SQLite
- [x] 输出架构方案到 `docs/architecture.md`
- [x] 明确领域层通用化设计：代码辅助只是首个场景，领域模型抽象为文件/文档操作
- [x] 补充多模块拆分理由到 `docs/architecture.md`

### 阶段二：项目框架搭建
- [x] 单模块 Maven 拆分为 `core` / `infrastructure` / `cli` 三模块
- [x] 调整根 `pom.xml` 为聚合模块，统一 dependencyManagement
- [x] 创建三个子模块 `pom.xml`
- [x] 迁移 `CodeAgentJavaApplication` / `application.properties` / 测试到 `cli` 模块
- [x] 创建核心包目录结构
- [x] 扩展 `application.properties` 为完整配置模板
- [x] 修复 `jooq.version` 为 `3.19.35`（适配本地/Aliyun 仓库可用版本）
- [x] 修复 `spring.autoconfigure.exclude` 禁用默认 `DataSourceAutoConfiguration`
- [x] `mvn compile` / `mvn test` 验证通过

### 阶段三：领域层与应用层定义
- [x] 定义核心值对象：`SessionId`, `UserId`, `TenantId`, `Prompt`, `ModelId`, `ContentType`, `TokenUsage`, `RelativePath`
- [x] 定义核心实体：`Session`, `Message`, `GenerationTask`, `ContentFragment`, `FileChange`, `WorkspaceContext`, `TelemetryEvent`, `UserIdentity`
- [x] 定义领域异常：`DomainException`
- [x] 定义领域服务接口：`FileDiffService`, `WorkspaceScanner`, `PromptBuilder`, `ContentFragmentExtractor`, `ContentTypeResolver`
- [x] 定义领域端口（仓库接口）：`LlmProvider`, `AgentOrchestrator`, `FileRepository`, `LocalStorage`, `TelemetryCollector`, `RemoteSyncService`, `UserIdentityResolver`
- [x] 定义应用层 DTO：`ChatRequest/Response`, `GenerateRequest/Response`, `ExplainRequest/Response`
- [x] 定义应用层端口：`ChatUseCase`, `GenerateUseCase`, `ExplainUseCase`
- [x] 实现应用层服务：`ChatService`, `GenerateService`, `ExplainService`
- [x] 创建基础设施配置类：`CodeAgentProperties`, `LlmProperties`, `StorageProperties`, `TelemetryProperties`, `CodeAgentConfigurationProperties`

### 阶段四：基础设施适配器实现
- [x] `AgentScopeOrchestrator`：实现 `AgentOrchestrator` / `StreamingAgentOrchestrator`，封装 `HarnessAgent`，延迟构建避免启动强依赖 API Key
- [x] `LocalFileSystemFileRepository`：实现 `FileRepository` 端口，支持工作区扫描、文件读取、dry-run、备份、写入
- [x] `EnvironmentUserIdentityResolver`：实现 `UserIdentityResolver`，从环境变量读取 `CODEAGENT_USER_ID` / `CODEAGENT_TENANT_ID`
- [x] `InMemoryTelemetryCollector`：实现 `TelemetryCollector`（Phase A 占位）
- [x] `SQLiteBufferedTelemetryCollector`：实现 `TelemetryCollector`，将未同步事件持久化到本地 SQLite
- [x] `JooqLocalStorage`：实现 `LocalStorage` 端口，使用 SQLite + JOOQ 保存会话、消息、生成任务、内容片段
- [x] `PostgreSqlRemoteSyncService`：实现 `RemoteSyncService`，批量 upsert 遥测事件到远程 PostgreSQL（租户/开发者幂等写入 + 功能事件 + LLM 用量）
- [x] `TelemetrySyncScheduler`：实现 `@Scheduled` 定时同步调度器（定时 + 阈值双重触发）
- [x] `InfrastructureConfig`：将应用服务与基础设施适配器装配为 Spring Bean

### 阶段五：SQLite 本地存储落地
- [x] 定义 SQLite 建表 SQL：`sessions`, `messages`, `generation_tasks`, `content_fragments`, `telemetry_events`
- [x] 配置 `DataSource`（HikariCP）与 `DSLContext`（JOOQ SQLite）
- [x] 启动时通过 Flyway 自动初始化 SQLite schema
- [x] 配置 JOOQ 代码生成插件（`jooq-codegen-maven` + `DDLDatabase`）
- [x] 新增 `SqlitePersistenceIntegrationTest` 覆盖会话、生成任务、遥测事件持久化

### 阶段六：PostgreSQL 远程遥测落地（完整实现）
- [x] 定义 PostgreSQL 建表 SQL：`tenant`, `developer`, `session_metrics`, `llm_usage`, `feature_event`, `content_fragment_meta`, `sync_checkpoint`
- [x] 配置 Flyway 迁移（PostgreSQL 专用 locations：`classpath:db/migration-pg`）
- [x] 配置 `postgresDataSource` 与 `postgresDSLContext`（`@ConditionalOnProperty`，未配置时优雅跳过）
- [x] 配置 JOOQ 代码生成插件（`postgres-codegen` execution）
- [x] 实现后台批量同步逻辑（`TelemetrySyncScheduler` 定时 + `SQLiteBufferedTelemetryCollector` 阈值触发）

### 阶段七：CLI 命令实现
- [x] `ChatCommand`：解析参数、调用 `ChatUseCase`、支持流式/阻塞输出
- [x] `GenerateCommand`：解析参数、调用 `GenerateUseCase`、展示 diff、确认写入
- [x] `ExplainCommand`：解析参数、调用 `ExplainUseCase`、渲染解释
- [x] 配置 `spring.shell.interactive.enabled=false` 支持非交互模式
- [x] 交互式确认与输出渲染工具：`TerminalRenderer`

### 阶段八：测试与验证（全量覆盖）
- [x] `contextLoads` 测试：Spring 上下文可正常启动（SQLite + Flyway 自动初始化）
- [x] 基础设施测试：SQLite JOOQ 仓库持久化集成测试
- [x] 单元测试：领域逻辑、值对象校验（13 个测试文件）
- [x] 应用服务测试：使用 Mockito stub 端口测试 `ChatService`, `GenerateService`, `ExplainService`（3 个测试文件）
- [x] Spring Shell 命令测试（3 个命令 + 2 个转换器 + 1 个渲染器 = 6 个测试文件）
- [x] 基础设施集成测试：`InMemoryTelemetryCollectorTest`, `EnvironmentUserIdentityResolverTest`
- [x] 端到端集成测试：`CliIntegrationTest` 覆盖 chat/generate/explain 流程和组合场景
- [x] `mvn clean verify` 全量验证通过

### 阶段九：工程收尾
- [x] 统一日志输出：`logback-spring.xml`（彩色控制台 + 按天滚动文件），关键类添加 `@Slf4j` + 业务日志
- [x] 错误处理：应用服务中遥测记录失败场景，TelemetrySyncScheduler 日志记录
- [x] CLI 启动脚本：`bin/code-agent-java.cmd`（Windows）与 `bin/code-agent-java`（Git Bash/Unix）
- [x] 安装说明文档：`docs/INSTALL.md`
- [x] 更新 `docs/architecture.md` 中与实际实现不一致的部分
- [x] 清理临时文件与 TODO 标记：删除遗留 `infrastructure/` 目录，`PostgreSqlRemoteSyncService` TODO 已实现

---

## 备注

- 构建时请使用本地 Java 25 JDK：
  ```bash
  export JAVA_HOME="C:/Users/EDY/.jdks/openjdk-25.0.1"
  ./mvnw -q clean verify
  ```
- 当前默认 LLM 模型为 `openai:deepseek-v4-flash`，可通过 `codeagent.llm.model` 切换为 `dashscope:qwen-plus` 等
- 远程遥测默认不上传代码内容，仅收集非敏感指标
- SQLite 数据文件默认位置：`${user.home}/.codeagent/codeagent.db`
