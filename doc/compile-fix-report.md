# 编译问题报告

> 生成日期: 2026-06-24
> 相关 Change: `fix-compile-errors`（已归档）

---

## 1. 问题概述

项目框架版本升级（Spring Boot 3.x → 4.0.7，Spring Shell 3.x → 4.0.2）后，部分测试文件未同步更新，导致以下两类编译失败：

### 1.1 `code-agent-java-core` 测试编译失败（已修复）

| 问题 | 涉及文件 | 根因 |
|------|---------|------|
| `new UserIdentity(UserId, TenantId)` 缺少 `machineId` 参数 | `ChatServiceTest.java`、`GenerateServiceTest.java`、`ExplainServiceTest.java` | `UserIdentity` record 构造签增加第 3 参 `String machineId`，测试未更新 |
| `ctx.primaryType()` 方法不存在 | `DefaultWorkspaceScannerTest.java` | 访问器已重命名为 `primaryContentType()` |

### 1.2 `code-agent-java-cli` 测试编译失败（已修复）

| 问题 | 涉及文件 | 根因 |
|------|---------|------|
| `@MockBean` 不可用 | `ChatCommandTest.java`、`GenerateCommandTest.java`、`ExplainCommandTest.java`、`CliIntegrationTest.java` | Spring Boot 4.0.7 移除了 `org.springframework.boot.test.mock.mockito.MockBean` |
| `Shell` 类不可用 | 同上 4 个文件 | Spring Shell 4.0.2 移除了 `org.springframework.shell.core.command.Shell`，替换为 `ShellTestClient` |

---

## 2. 详细分析

### 2.1 `@MockBean` 被移除

**现象**: `import org.springframework.boot.test.mock.mockito.MockBean` 编译报错。
**原因**: Spring Boot 4.0.7 从 `spring-boot-test-autoconfigure` 中移除了 `@MockBean` 和 `@SpyBean` 注解，`spring-boot-test-4.0.7.jar` 中已无相关类。
**解决**: 使用 `@TestConfiguration` 内类 + `@Bean @Primary` + `Mockito.mock()` 替代。

### 2.2 `Shell` 类被移除

**现象**: `import org.springframework.shell.core.command.Shell` 编译报错。
**原因**: Spring Shell 4.0.2 重构了测试 API，移除了 `Shell` 类。
**解决**: 改用 `ShellTestClient`（来自 `spring-shell-test`），通过 `shellTestClient.sendCommand(String)` 方法模拟命令输入。

### 2.3 测试遗漏的根因

生产代码已适配新版 API（如使用 `@Command`、`@Option` 注解），但测试代码未同步更新，属于框架升级过程中的遗漏。

---

## 3. 修复方案

### 3.1 core 模块测试（构造参数 / 方法名）

```java
// 旧
new UserIdentity(new UserId("dev-1"), TenantId.defaultTenant())
// 新  
new UserIdentity(new UserId("dev-1"), TenantId.defaultTenant(), "test-machine")

// 旧
ctx.primaryType()
// 新
ctx.primaryContentType()
```

### 3.2 CLI 模块测试（MockBean → TestConfiguration）

```java
// 删除旧 import
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.shell.core.command.Shell;

// 添加新 import
import org.springframework.shell.test.ShellTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import static org.mockito.Mockito.mock;

// 替换 Shell → ShellTestClient
// @Autowired private Shell shell;
@Autowired private ShellTestClient shellTestClient;

// 替换 @MockBean → TestConfiguration
@Autowired private ChatUseCase chatUseCase;

@SpringBootTest
class ChatCommandTest {
    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        ChatUseCase chatUseCase() { return mock(ChatUseCase.class); }
        @Bean
        ShellTestClient shellTestClient(CommandParser parser, CommandRegistry registry) {
            return new ShellTestClient(parser, registry);
        }
    }
}
// shell.evaluate(() -> "command --flag") → shellTestClient.sendCommand("command --flag")
```

---

## 4. 受影响文件清单

| 文件 | 修改类型 |
|------|---------|
| `code-agent-java-core/.../ChatServiceTest.java` | 修改构造参数 |
| `code-agent-java-core/.../GenerateServiceTest.java` | 修改构造参数 |
| `code-agent-java-core/.../ExplainServiceTest.java` | 修改构造参数 |
| `code-agent-java-core/.../DefaultWorkspaceScannerTest.java` | 方法名替换 |
| `code-agent-java-cli/.../ChatCommandTest.java` | MockBean → TestConfiguration + Shell → ShellTestClient |
| `code-agent-java-cli/.../GenerateCommandTest.java` | 同上 |
| `code-agent-java-cli/.../ExplainCommandTest.java` | 同上 |
| `code-agent-java-cli/.../CliIntegrationTest.java` | 同上 |

---

## 5. 验证结果

| 验证命令 | JDK 版本 | 结果 |
|---------|---------|------|
| `mvn compile` | OpenJDK 25.0.1 | BUILD SUCCESS |
| `mvn test-compile` | OpenJDK 25.0.1 | BUILD SUCCESS |
