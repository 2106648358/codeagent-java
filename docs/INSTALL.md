# Code Agent Java — 安装指南

> AI 编程助手本地 CLI 的安装、配置和快速入门说明。

---

## 1. 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | **Java 25** | 使用 `openjdk-25.0.1` 或更高版本 |
| Maven | 3.9+ | 项目自带 Maven Wrapper（`mvnw`），无需手动安装 |
| 操作系统 | Windows / macOS / Linux | 跨平台 |
| 磁盘空间 | ~500 MB | 包含依赖下载和本地 SQLite 数据库 |

### 1.1 验证 Java 版本

```bash
java -version
# 应输出: openjdk version "25.0.1" ...
```

### 1.2 推荐 JDK 安装方式

- **Windows**：从 [jdk.java.net](https://jdk.java.net/25/) 下载，解压到 `C:/Users/<用户名>/.jdks/openjdk-25.0.1/`
- **macOS**：`brew install openjdk@25`
- **Linux**：`sdk install java 25.0.1-open`

---

## 2. 快速开始

### 2.1 克隆项目

```bash
git clone <repository-url>
cd code-agent-java
```

### 2.2 配置 API Key

创建本地配置文件（已加入 `.gitignore`，不会提交到 Git）：

```bash
# 创建 application-local.properties 并填写 API Key
echo "codeagent.llm.openai.api-key=sk-your-deepseek-api-key" > code-agent-java-cli/src/main/resources/application-local.properties
```

> **注意**：默认使用 DeepSeek API（OpenAI 兼容接口）。可在 `application.properties` 中修改 `codeagent.llm.model` 和 `codeagent.llm.openai.base-url` 切换其他模型。

### 2.3 编译项目

```bash
# 设置 JAVA_HOME（Windows）
set JAVA_HOME=C:/Users/<用户名>/.jdks/openjdk-25.0.1

# 编译 + 运行测试
mvnw.cmd clean verify   # Windows
./mvnw clean verify     # macOS / Linux
```

### 2.4 启动交互式 Shell

```bash
# Windows
bin\code-agent-java.cmd

# macOS / Linux
bin/code-agent-java
```

---

## 3. 配置说明

### 3.1 主配置文件

`code-agent-java-cli/src/main/resources/application.properties`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `codeagent.identity.user-id` | `anonymous` | 用户标识 |
| `codeagent.identity.tenant-id` | `default` | 租户标识 |
| `codeagent.llm.model` | `openai:deepseek-v4-flash` | LLM 模型 |
| `codeagent.llm.openai.base-url` | `https://api.deepseek.com` | API 端点 |
| `codeagent.llm.openai.api-key` | `${DEEPSEEK_API_KEY:}` | API Key |
| `codeagent.storage.sqlite.path` | `~/.codeagent/codeagent.db` | 本地数据库路径 |
| `codeagent.telemetry.postgres.url` | （空） | 远程遥测 PostgreSQL URL |
| `codeagent.cli.stream-output` | `true` | 是否流式输出 |
| `codeagent.cli.confirm-writes` | `true` | 写入前是否确认 |

### 3.2 远程遥测 PostgreSQL（可选）

若需将遥测数据同步到远程 PostgreSQL 进行分析：

```properties
codeagent.telemetry.postgres.url=jdbc:postgresql://your-host:5432/codeagent_telemetry
codeagent.telemetry.postgres.user=telemetry_user
codeagent.telemetry.postgres.password=your-password
codeagent.telemetry.batch-size=100
codeagent.telemetry.flush-interval-seconds=30
```

不配置时，遥测仅保存在本地 SQLite 中，不会上传。

### 3.3 环境变量

| 变量 | 说明 |
|------|------|
| `JAVA_HOME` | JDK 安装路径 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key（或在 application-local.properties 中配置） |
| `CODEAGENT_USER_ID` | 用户标识（覆盖配置文件） |
| `CODEAGENT_TENANT_ID` | 租户标识（覆盖配置文件） |

---

## 4. 基本用法

### 4.1 对话（chat）

```bash
chat --prompt "用 Java 写一个单例模式"
chat --prompt "再解释下双重检查锁定" --session my-session
```

### 4.2 生成文件（generate）

```bash
# 基本生成
generate --requirements "创建一个 Spring Boot REST 控制器"

# 指定文件类型
generate --requirements "Maven POM 配置" --type XML

# 跳过确认直接写入
generate --requirements "添加 .gitignore" --files ".gitignore" --yes
```

### 4.3 解释文件（explain）

```bash
explain --target "pom.xml"
explain --target "ChatService.java" --scope "chat logic"
```

---

## 5. 卸载

删除以下目录即可完全卸载：

- 项目目录
- `~/.codeagent/`（本地数据库和日志）

---

## 6. 常见问题

### Q: 启动时提示 "JAVA_HOME environment variable is not defined correctly"

确保 `JAVA_HOME` 指向 Java 25 的安装目录，且 `%JAVA_HOME%/bin/java` 存在。

### Q: API 调用返回 401

检查 `application-local.properties` 中的 API Key 是否正确，或是否设置了 `DEEPSEEK_API_KEY` 环境变量。

### Q: 如何切换模型

修改 `application.properties`：

```properties
# DeepSeek V4
codeagent.llm.model=openai:deepseek-v4-flash

# 或使用阿里云 DashScope
codeagent.llm.model=dashscope:qwen-plus
codeagent.llm.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
```

### Q: 如何查看本地遥测数据

使用任意 SQLite 客户端打开 `~/.codeagent/codeagent.db`，查看 `telemetry_events` 表。
