## 背景与动机

项目需要在各个 Maven 模块中保持稳定可编译，方便贡献者直接构建、测试并运行 CLI，而不必先处理源码、测试源码或依赖配置问题。本变更用于修复 IDE 报告的符号无法解析问题，以及 Maven 测试编译阶段已经复现的错误，同时保持现有模块边界和行为不变。

## 变更内容

- 复现并分类 IDE 报告的问题，包括 `Cannot resolve symbol 'mockito'` 和 `Cannot resolve symbol 'Shell'`。
- 判断 IDE 报错是由 Maven 依赖缺失、导入的 source set 不正确、IDE 索引过期，还是实际 Java 编译错误导致。
- 修复阻塞编译的源码、测试源码、依赖、注解处理或模块配置问题。
- 修复当前已在 `code-agent-java-core` 测试中复现的 `mvn test-compile` 错误，包括过期的 `UserIdentity` 构造器调用，以及已经不存在的 `WorkspaceContext.primaryType()` 访问器。
- 除非编译失败暴露出直接相关的 API 契约不一致，否则实现改动应限定在编译正确性范围内。
- 只有当编译修复改变行为或需要防止同类回归时，才新增或更新聚焦测试。
- 对所有意图不明显的新增或修改代码补充有意义的注释。

## 能力范围

### 新增能力

- `build-stability`：定义多模块 Java 项目及其测试源码必须能在受支持的 Maven 和 JDK 配置下成功编译，并且 IDE 依赖解析结果应与 Maven 模型保持一致。

### 修改能力

- 无。

## 影响范围

- 影响系统：Maven 多模块构建、Java 源码编译、测试源码编译、IDE Maven 导入、注解处理和模块依赖。
- 可能影响模块：`code-agent-java-common`、`code-agent-java-core`、`code-agent-java-infrastructure` 和 `code-agent-java-cli`。
- 已知 IDE 信号：共 8 个无法解析符号错误，包括 `mockito` 和 `Shell`。
- 已知 Maven 信号：`mvn test-compile` 在 `code-agent-java-core` 中失败，原因是多个测试仍调用 `new UserIdentity(UserId, TenantId)`，而当前 record 构造器需要 `UserIdentity(UserId, TenantId, String)`；另一个错误是 `DefaultWorkspaceScannerTest` 调用了 `WorkspaceContext.primaryType()`，而当前访问器为 `primaryContentType()`。
- 除非修复编译期 API 不一致需要对齐现有契约，否则不预期改变公开 CLI 命令行为。
- 当前本地观察：使用 JDK 25 执行 `mvn compile` 已通过，而 `mvn test-compile` 能复现测试源码错误。实施时应区分 IDE 依赖解析错误和 Maven 源码/测试源码编译错误，并在关闭变更前分别验证。
