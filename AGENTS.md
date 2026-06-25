## Project Rules

- 边开发边写注释：所有新增和修改的代码必须同步编写清晰、有意义的注释，确保代码可读性和可维护性。
- OpenSpec 文档必须使用中文编写；为保证 OpenSpec 工具正常解析，固定格式关键字（如 `ADDED Requirements`、`Requirement`、`Scenario`、`WHEN`、`THEN`）可以保留英文。

## Code Retrieval

- 优先使用 CodeGraph（`codegraph_explore` / `codegraph_node` / `codegraph_search`）检索和理解代码，避免直接用 grep 或 Read 遍历文件。

## Architecture & Versions

- 严格保证架构清晰：模块职责分层明确（common → core → infrastructure → cli），依赖方向不可逆向。
- 基础技术栈版本必须保持当前版本（Spring Boot 4.0.7、Spring Shell 4.0.2、JDK 25、jOOQ 3.19.35、Flyway 10.20.1 等），不可随意升级或降级。
