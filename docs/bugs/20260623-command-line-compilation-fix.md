# Bug Report: 命令行编译失败 — JAVA_HOME + Maven Wrapper + Lombok + 依赖缺失
> 修复时间：2026-06-23
> 环境：Windows 11, PowerShell 5.1, JDK 25.0.1 (OpenJDK), Maven 3.9.16, Spring Boot 4.0.7

---

## 根因概述

命令行 `mvn compile` 因四个独立问题全部失败：
1. `JAVA_HOME` 指向已删除的 JDK 8 路径
2. `.mvn/wrapper/` 缺少 `maven-wrapper.jar`、`mvnw.cmd`、`mvnw`
3. JDK 25 下 Maven 编译器未激活 Lombok 注解处理器
4. `code-agent-java-core` 缺少 `slf4j-api` 依赖（此前被 Lombok 处理器失败掩盖）

IDE (IntelliJ) 中编译正常是因为 IDE 的 Project Structure SDK 设置与系统环境变量 `JAVA_HOME` 完全独立，且 IDE 内置编译器对注解处理器的发现机制与 Maven 不同。

---

## Bug 1: JAVA_HOME 指向不存在的 JDK 8

**严重程度**: P0 (阻断)

**现象**: 执行 `mvnw` 或 `java` 命令时报错：
```
Error: could not open `D:\DevTools\JDK\jdk8\JRE8\lib\amd64\jvm.cfg'
```

**根因**: 系统环境变量 `JAVA_HOME` 指向 `D:\DevTools\JDK\jdk8`，该路径已被删除。`C:\ProgramData\Oracle\Java\javapath\java.exe` 是 Oracle JavaPath 重定向 stub，其注册表索引入口同样指向该失效路径。

**可用的 JDK**: `C:\Users\EDY\.jdks\openjdk-25.0.1`（IntelliJ 自动下载的 JDK 25，`java -version` 正常输出 `openjdk version "25.0.1"`）

**修复**: 需要用户手动操作——将系统环境变量 `JAVA_HOME` 设为 `C:\Users\EDY\.jdks\openjdk-25.0.1`，并将 `%JAVA_HOME%\bin` 加入 `PATH`。本次所有编译验证均通过临时 `$env:JAVA_HOME = "..."` 执行。

**状态**: ⚠️ 需用户手动设置环境变量

---

## Bug 2: .mvn/wrapper/ 缺失启动文件

**严重程度**: P1 (阻断)

**现象**: `mvnw` 无法执行，`.mvn/wrapper/` 目录下仅存在 `maven-wrapper.properties`。

**根因**: Maven Wrapper 的三个必要文件（`maven-wrapper.jar`、`mvnw.cmd`、`mvnw`）在项目初始化时未被提交或已丢失。Wrapper 3.3.4 使用 `distributionType=only-script`，启动依赖 `mvnw.cmd` 脚本本身而非 JAR。

**修复**: 通过已下载的 Maven 3.9.16 发行版执行 `mvn wrapper:wrapper -Dmaven=3.9.16` 重新生成。之后 `.mvn/wrapper/` 包含：
- `maven-wrapper.properties`（更新）
- `maven-wrapper.jar`（新增）
- `mvnw.cmd`（新增）
- `mvnw`（新增）

**状态**: ✅ 已修复

---

## Bug 3: JDK 25 下 Lombok 注解处理器未激活

**严重程度**: P0 (阻断)

**现象**: `code-agent-java-core` 中 `ChatService`、`ExplainService`、`GenerateService` 编译报错：
```
找不到符号
  符号:   变量 log
  位置: 类 com.edianyun.codeagentjava.application.service.ChatService
```

**根因**: 这三个 Service 使用了 `@Slf4j` 注解，需要 Lombok 注解处理器生成 `log` 字段。Spring Boot parent (4.0.7) 已管理 Lombok 1.18.46 和 `maven-compiler-plugin` 3.14.1，但未配置 `annotationProcessorPaths`。JDK 25 下，Maven 编译器默认不会自动从 classpath 发现注解处理器，必须通过 `annotationProcessorPaths` 显式声明。

**修复**: 在根 [pom.xml](/pom.xml) 的 `<build><pluginManagement>` 中添加：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**间接影响**: 该修复后暴露了 Bug 4（此前 `log` 变量不存在，`org.slf4j` 的 import 未被编译器实际解析）。

**状态**: ✅ 已修复

---

## Bug 4: code-agent-java-core 缺少 slf4j-api 依赖

**严重程度**: P1 (阻断)

**现象**: Bug 3 修复后，`code-agent-java-core` 新增编译错误：
```
程序包org.slf4j不存在
```

**根因**: `@Slf4j` 生成的 `log` 字段类型为 `org.slf4j.Logger`，初始化调用 `org.slf4j.LoggerFactory.getLogger()`。`code-agent-java-core/pom.xml` 仅依赖 `commons-lang3` 和 `lombok`（optional），没有 `slf4j-api`。在 IDE 中，其他模块的传递依赖（`spring-boot-starter` → `slf4j-api`）可能被 IDE classpath 合并而绕过；Maven 模块化编译时各模块 classpath 严格隔离，直接暴露该缺失。

**修复**: 在 [code-agent-java-core/pom.xml](/code-agent-java-core/pom.xml) 中添加：
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```
版本由 Spring Boot parent BOM 统一管理，无需显式指定。

**状态**: ✅ 已修复

---

## 修复清单

| 序号 | 问题 | 影响 | 优先级 | 文件 |
|------|------|------|--------|------|
| Bug 1 | JAVA_HOME 指向失效路径 | 所有命令行 Java/Maven 调用 | P0 | 系统环境变量（需手动） |
| Bug 2 | Maven Wrapper 文件缺失 | mvnw 不可用 | P1 | `.mvn/wrapper/` 多项 |
| Bug 3 | Lombok 注解处理器未激活 | core 模块编译失败 | P0 | [pom.xml](/pom.xml) |
| Bug 4 | slf4j-api 依赖缺失 | core 模块编译失败 | P1 | [code-agent-java-core/pom.xml](/code-agent-java-core/pom.xml) |

## 验证

```powershell
$env:JAVA_HOME = "C:\Users\EDY\.jdks\openjdk-25.0.1"
mvn compile -q
# exit code 0，无编译错误（仅 Lombok sun.misc.Unsafe 弃用警告）
```

## 模块依赖关系（修复后）

```
code-agent-java-common     ← 新增，零业务依赖（lombok + commons-lang3 + slf4j-api）
    ↑
code-agent-java-core       ← 新增 slf4j-api 依赖，annotationProcessorPaths 激活 Lombok
    ↑
code-agent-java-infrastructure
    ↑
code-agent-java-cli
```
