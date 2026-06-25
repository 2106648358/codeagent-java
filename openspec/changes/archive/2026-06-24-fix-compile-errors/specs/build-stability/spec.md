## ADDED Requirements

### Requirement: Maven reactor 主源码可编译
项目在使用受支持的 JDK 和父级 Maven 配置时，SHALL 能够编译 Maven reactor 中的所有主 Java 源码。

#### Scenario: 完整 reactor 编译成功
- **WHEN** 在仓库根目录使用受支持的 JDK 运行 `mvn compile`
- **THEN** Maven MUST 成功完成 `code-agent-java-common`、`code-agent-java-core`、`code-agent-java-infrastructure` 和 `code-agent-java-cli` 的编译

### Requirement: Maven reactor 测试源码可编译
项目 SHALL 能够编译 Maven reactor 中所有已声明依赖的测试 Java 源码。

#### Scenario: 完整 reactor 测试编译成功
- **WHEN** 在仓库根目录使用受支持的 JDK 运行 `mvn test-compile`
- **THEN** Maven MUST 成功完成所有包含测试源码模块的测试编译

### Requirement: 编译修复保持模块契约
除非编译错误证明现有契约本身不一致，否则编译相关变更 SHALL 保持现有模块边界和公开应用契约。

#### Scenario: 跨模块 API 不匹配被修复
- **WHEN** 编译失败由跨模块的方法签名、构造器签名、包名或依赖声明不一致导致
- **THEN** 实现 MUST 同步更新拥有该契约的代码和所有编译期调用方

### Requirement: 编译诊断可复现
实现 SHALL 使用可复现的 Maven 命令验证编译健康状态；当已报告的编译失败无法复现时，必须记录具体环境原因。

#### Scenario: 已报告编译失败无法在本地复现
- **WHEN** 本地受支持的构建命令在修改源码前已经成功
- **THEN** 实现 MUST 先识别导致失败所需的命令、JDK、profile 或模块目标，再修改编译相关代码

### Requirement: IDE 依赖解析匹配 Maven 模型
项目 SHALL 保持 IDE 可见依赖与 Maven 依赖模型一致，尤其是测试框架和 CLI 框架相关符号。

#### Scenario: IDE 报告框架符号无法解析
- **WHEN** IDE 报告 `mockito` 或 `Shell` 等符号无法解析
- **THEN** 实现 MUST 先判断原因是 Maven 依赖、IDE Maven 导入状态还是 source set 分类，再添加或修改依赖声明
