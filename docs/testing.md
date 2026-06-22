# 测试指南

> 本文档说明如何运行、编写和维护项目的测试。项目采用多模块 Maven 结构，测试策略覆盖单元测试、应用服务测试、Spring Shell 命令测试、基础设施集成测试和端到端集成测试。

---

## 1. 运行测试

### 1.1 运行全部测试

```bash
mvn test
```

### 1.2 运行单个模块测试

```bash
mvn test -pl code-agent-java-core
mvn test -pl code-agent-java-infrastructure
mvn test -pl code-agent-java-cli
```

### 1.3 运行单个测试类

```bash
mvn test -pl code-agent-java-cli -Dtest=SqlitePersistenceIntegrationTest
```

### 1.4 跳过测试

```bash
mvn install -DskipTests
```

---

## 2. 测试分层策略

```
┌──────────────────────────────────────────────────────┐
│                  端到端集成测试                        │
│  启动完整 Spring Shell 上下文，stub 外部依赖           │
├──────────────────────────────────────────────────────┤
│               Spring Shell 命令测试                    │
│  参数解析、校验、输出渲染                              │
├──────────────────────────────────────────────────────┤
│              应用服务测试                              │
│  Mock 端口，测试 ChatService/GenerateService/...      │
├──────────────────────────────────────────────────────┤
│              领域层单元测试                            │
│  值对象校验、领域服务逻辑                              │
├──────────────────────────────────────────────────────┤
│              基础设施集成测试                          │
│  SQLite JOOQ、Testcontainers PostgreSQL、WireMock     │
└──────────────────────────────────────────────────────┘
```

---

## 3. 当前测试状态

| 模块 | 覆盖率 | 说明 |
|------|--------|------|
| `code-agent-java-core` | ❌ 无测试 | 缺少 `pom.xml` 测试依赖和测试源码 |
| `code-agent-java-infrastructure` | ❌ 无测试 | 缺少 `pom.xml` 测试依赖和测试源码 |
| `code-agent-java-cli` | ⚠️ 基础 | 2 个测试文件，4 个测试用例 |

---

## 4. 如何编写测试

### 4.1 领域层单元测试

**位置**：`code-agent-java-core/src/test/java/com/edianyun/codeagentjava/domain/...`

**前提**：需要在 `code-agent-java-core/pom.xml` 中添加测试依赖。

**示例 — 值对象校验：**

```java
class SessionIdTest {

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new SessionId(""));
        assertThrows(IllegalArgumentException.class, () -> new SessionId("  "));
        assertThrows(IllegalArgumentException.class, () -> new SessionId(null));
    }

    @Test
    void shouldGenerateValidId() {
        SessionId id = SessionId.generate();
        assertNotNull(id);
        assertNotNull(id.value());
    }
}
```

**示例 — 领域服务测试：**

```java
class UnifiedDiffFileDiffServiceTest {

    private final UnifiedDiffFileDiffService service = new UnifiedDiffFileDiffService();

    @Test
    void shouldReturnNoChangesForIdenticalContent() {
        String result = service.diff("hello\nworld", "hello\nworld");
        assertEquals("No changes.", result);
    }

    @Test
    void shouldDetectAddedLines() {
        String result = service.diff("a\nb", "a\nb\nc");
        assertTrue(result.contains("+c"));
    }

    @Test
    void shouldDetectRemovedLines() {
        String result = service.diff("a\nb\nc", "a\nc");
        assertTrue(result.contains("-b"));
    }
}
```

### 4.2 应用服务测试

使用 Mockito 为端口创建桩实现，测试用例服务的编排逻辑。

**示例 — ChatService 测试：**

```java
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AgentOrchestrator agentOrchestrator;
    @Mock
    private StreamingAgentOrchestrator streamingAgentOrchestrator;
    @Mock
    private LocalStorage localStorage;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private TelemetryCollector telemetryCollector;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(agentOrchestrator, streamingAgentOrchestrator,
                localStorage, userIdentityResolver, telemetryCollector);
    }

    @Test
    void shouldCreateNewSessionWhenNoSessionIdProvided() {
        when(userIdentityResolver.resolve()).thenReturn(UserIdentity.anonymous("test-machine"));
        when(localStorage.loadSession(any())).thenReturn(Optional.empty());
        when(agentOrchestrator.chat(any(), any())).thenReturn(Message.assistant("Hi!", null, null));

        ChatRequest request = new ChatRequest(null, null, "Hello");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response.sessionId());
        assertEquals("Hi!", response.reply());
        verify(localStorage).saveSession(any());
        verify(telemetryCollector).record(any());
    }
}
```

### 4.3 Spring Shell 命令测试

使用 `@SpringShellTest` 注解测试命令的参数解析和输出。

**示例 — ChatCommand 测试：**

```java
@SpringShellTest
class ChatCommandTest {

    @Autowired
    private Shell shell;

    @Test
    void shouldRejectMissingPrompt() {
        CommandResult result = shell.run(e -> e.command("chat"));
        assertThat(result.getStatus()).isEqualTo(Status.ERROR);
    }

    @Test
    void shouldAcceptPromptArgument() {
        CommandResult result = shell.run(e -> e.command("chat --prompt Hello"));
        assertThat(result.getStatus()).isEqualTo(Status.OK);
    }
}
```

### 4.4 基础设施集成测试

**SQLite 仓库测试**（已有 `SqlitePersistenceIntegrationTest` 作为参考）：

```java
@SpringBootTest
class JooqLocalStorageTest {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("codeagent.storage.sqlite.path", () -> {
            try { return Files.createTempFile("test", ".db").toString(); }
            catch (IOException e) { throw new RuntimeException(e); }
        });
    }

    @Autowired
    private LocalStorage localStorage;

    @Test
    void shouldPersistAndLoadSession() {
        SessionId id = SessionId.generate();
        Session session = new Session(id, UserId.anonymous(), TenantId.defaultTenant(), "Test");
        session.addMessage(Message.user("hello"));
        localStorage.saveSession(session);

        Optional<Session> loaded = localStorage.loadSession(id);
        assertTrue(loaded.isPresent());
        assertEquals(1, loaded.get().messages().size());
    }
}
```

**LLM Provider 测试**（使用 WireMock）：

```java
@SpringBootTest
@WireMockTest(httpPort = 8089)
class OpenAiLlmProviderTest {

    @Test
    void shouldReturnGeneratedText() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"choices\":[{\"message\":{\"content\":\"Hello!\"}}]}")));

        LlmProvider provider = new OpenAiLlmProvider("http://localhost:8089", "sk-test");
        String result = provider.generate(
                List.of(Prompt.user("Hi")),
                LlmOptions.defaults(new ModelId("openai:gpt-4.1")));

        assertEquals("Hello!", result);
    }
}
```

---

## 5. 测试依赖清单

### 5.1 `code-agent-java-core/pom.xml`（需添加）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 5.2 `code-agent-java-infrastructure/pom.xml`（需添加）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <scope>test</scope>
</dependency>
```

### 5.3 `code-agent-java-cli/pom.xml`（已有）

- `spring-boot-starter-test` ✅
- `spring-shell-starter-test` ✅

---

## 6. 测试文件目录结构（建议）

```
code-agent-java-core/src/test/java/com/edianyun/codeagentjava/
├── domain/
│   ├── model/
│   │   ├── session/SessionIdTest.java
│   │   ├── session/UserIdTest.java
│   │   ├── content/TokenUsageTest.java
│   │   ├── content/ContentTypeTest.java
│   │   ├── workspace/RelativePathTest.java
│   │   └── message/MessageTest.java
│   └── service/
│       ├── UnifiedDiffFileDiffServiceTest.java
│       ├── DefaultPromptBuilderTest.java
│       ├── DefaultWorkspaceScannerTest.java
│       ├── MarkdownContentFragmentExtractorTest.java
│       └── ExtensionContentTypeResolverTest.java
├── application/
│   ├── service/
│   │   ├── ChatServiceTest.java
│   │   ├── GenerateServiceTest.java
│   │   └── ExplainServiceTest.java
│   └── dto/
│       ├── ChatRequestTest.java
│       ├── GenerateRequestTest.java
│       └── ExplainRequestTest.java

code-agent-java-infrastructure/src/test/java/com/edianyun/codeagentjava/
└── infrastructure/
    ├── adapters/
    │   ├── JooqLocalStorageTest.java
    │   ├── SQLiteBufferedTelemetryCollectorTest.java
    │   └── InMemoryTelemetryCollectorTest.java
    └── config/
        └── InfrastructureConfigTest.java

code-agent-java-cli/src/test/java/com/edianyun/codeagentjava/
├── CodeAgentJavaApplicationTests.java              # 已有
├── SqlitePersistenceIntegrationTest.java            # 已有
├── cli/
│   ├── command/
│   │   ├── ChatCommandTest.java
│   │   ├── GenerateCommandTest.java
│   │   └── ExplainCommandTest.java
│   ├── converter/
│   │   ├── ContentTypeConverterTest.java
│   │   └── RelativePathConverterTest.java
│   └── output/
│       └── TerminalRendererTest.java
└── integration/
    └── CliIntegrationTest.java
```

---

## 7. 测试规范

- **命名规范**：测试类名 = `{被测类名}Test.java`
- **方法命名**：`should{预期行为}_when{条件}`（如 `shouldRejectBlankValue`）
- **断言风格**：优先使用 AssertJ（`assertThat(...)`），必要时用 JUnit 5 断言
- **隔离性**：每个测试方法独立，不共享状态
- **Mock 策略**：领域服务测试用真实实现；应用服务测试 Mock 端口接口
- **外部资源**：数据库使用临时文件或内存实例；HTTP 使用 WireMock；PostgreSQL 使用 Testcontainers
