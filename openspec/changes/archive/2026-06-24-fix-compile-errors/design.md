## 上下文

当前仓库是一个 Maven reactor，包含四个模块：`code-agent-java-common`、`code-agent-java-core`、`code-agent-java-infrastructure` 和 `code-agent-java-cli`。父 POM 目标 Java 版本为 25，并通过 Maven compiler plugin 配置 Lombok 注解处理。

本变更用于修复项目编译错误。当前工作区使用 JDK 25 执行 `mvn compile` 可以通过，但 IDE 报告 8 个无法解析符号错误，涉及 `mockito` 和 `Shell`。使用非沙箱方式执行 `mvn test-compile` 也能在 `code-agent-java-core` 中复现测试源码编译失败：测试仍使用过期的 `UserIdentity` 构造器调用，并调用了已经不存在的 `WorkspaceContext.primaryType()` 访问器。

## 目标 / 非目标

**目标：**

- 使用干净的 Maven 命令复现编译失败，或者识别 IDE 专属报错无法在 Maven 中复现的环境差异。
- 以最小范围修复源码、测试源码、依赖、注解处理器或模块配置问题，使 `mvn compile` 和相关测试编译在完整 reactor 中通过。
- 让 Mockito 和 Spring Shell 相关类的 IDE 依赖解析结果与 Maven 模型保持一致。
- 保持现有 CLI 行为和模块职责不变，除非编译失败由不一致的 API 契约导致。
- 遵守仓库规则：对意图不明显的新增或修改代码补充解释性注释。

**非目标：**

- 重设计模块架构。
- 升级无关框架或依赖版本。
- 改变运行时行为、命令语义、持久化行为或遥测行为，除非这是编译正确性所必需。
- 修复编译通过后才暴露出来的无关测试断言失败。

## 技术决策

- 使用完整 Maven reactor 作为编译健康状态的事实来源。
  - 理由：只编译单个模块可能遗漏父 reactor 下才会出现的依赖或注解处理问题。
  - 备选方案：只编译疑似模块。该方式适合诊断，但不能作为最终验收门槛。

- 将 `test-compile` 纳入本变更的验收门槛。
  - 理由：当前已经复现的失败位于测试源码中，而 IDE 的 `mockito` 无法解析通常也与测试 classpath 有关。
  - 备选方案：只检查 `mvn compile`。该方式会漏掉已经复现的 `UserIdentity` 和 `WorkspaceContext` 测试源码错误。

- 区分 IDE 依赖解析问题和 Java 源码正确性问题。
  - 理由：相关模块已经声明 Spring Shell 和测试依赖，因此 `mockito`、`Shell` 无法解析可能来自 IDE Maven 导入或 source set 配置，而不一定是项目依赖缺失。
  - 备选方案：立即添加显式依赖。只有当 dependency tree 或编译输出证明 Maven 模型不完整时才应这么做。

- 先诊断再修改。
  - 理由：主源码编译已通过，测试源码编译失败，因此实现应聚焦已复现的失败范围，而不是改动正常工作的生产代码。
  - 备选方案：主动重构可疑代码或 POM 设置。该做法会在没有明确信号时扩大风险。

- 将修复限定在失败契约附近。
  - 理由：编译失败通常对应具体的方法签名不匹配、缺失依赖、无效 import、注解处理配置问题或模块边界违规。
  - 备选方案：大范围升级依赖或重组包结构。除非编译输出证明必要，否则应避免。

## 风险 / 权衡

- IDE 无法解析符号无法在 Maven 中复现 -> 先检查 Maven 导入、source set 配置和 dependency tree，再决定是否修改 POM。
- 测试源码失败遮蔽 IDE 依赖问题 -> 先修复已复现的测试源码 API 不匹配，再重新运行 IDE/Maven 检查看剩余无法解析符号。
- 修复需要依赖或插件配置变更 -> 优先选择最窄的版本或配置调整，并在修改后验证完整 reactor 编译。
- API 不匹配跨越多个模块 -> 同步更新拥有该抽象的契约和所有编译期调用方；只有当名称和类型不能表达意图时才补充注释。
