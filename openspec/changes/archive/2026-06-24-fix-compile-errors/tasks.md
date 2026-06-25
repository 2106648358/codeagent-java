## 1. 诊断编译失败

- [x] 1.1 使用受支持的 JDK 从仓库根目录运行完整 Maven reactor 编译，并记录精确命令、JDK 版本和输出。
- [x] 1.2 运行完整 Maven reactor 的 `mvn test-compile`，因为已报告的 `mockito` IDE 错误很可能涉及测试源码 classpath。
- [x] 1.3 检查 IDE 报告的 `Cannot resolve symbol 'mockito'` 和 `Cannot resolve symbol 'Shell'` 位置，并将每个错误分类为 Maven 依赖问题、IDE 导入/source set 问题或 Java 源码不匹配问题。
- [x] 1.4 将已复现的 Maven 错误映射到所属模块和契约，包括源码文件、依赖声明或注解处理配置。

## 2. 实施最小编译修复

- [x] 2.1 更新 `code-agent-java-core` 中仍调用 `new UserIdentity(UserId, TenantId)` 的测试，为其提供必需的 `machineId` 参数，或使用能保留原测试语义的既有工厂方法。
- [x] 2.2 更新 `DefaultWorkspaceScannerTest`，改为断言 `WorkspaceContext.primaryContentType()`，不再使用过期的 `primaryType()` 访问器。
- [x] 2.3 只有当 Maven dependency 检查证明相关模块 classpath 缺少 Mockito 或 Spring Shell 符号时，才修改最小范围的 POM、依赖或 IDE 可见配置。
- [x] 2.4 对意图不明显的新增或修改代码补充有意义的注释，确保后续维护者能理解改动原因。

## 3. 验证构建稳定性

- [x] 3.1 使用受支持的 JDK 重新运行完整 Maven reactor 的 `mvn compile`，确认所有模块主源码编译成功。
- [x] 3.2 重新运行完整 Maven reactor 的 `mvn test-compile`，确认所有测试源码编译成功。
- [x] 3.3 Maven 测试编译通过后，重新检查 IDE 中无法解析符号的问题；如果 Maven 模型正确，则刷新 Maven 导入或 source root。
- [x] 3.4 在实施总结中记录最终失败原因、变更文件和验证命令输出。
