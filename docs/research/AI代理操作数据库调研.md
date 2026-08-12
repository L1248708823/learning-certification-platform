# AI 代理操作数据库调研

调研日期：2026-08-11

## 结论

当前主流 AI 编程 CLI 通常同时支持两条路径：

1. 通过内置 Shell 工具执行 `mysql`、`psql`、脚本、迁移命令或应用测试。
2. 通过 MCP 连接外部工具。MCP Server 持有数据库连接，向 Codex、Claude Code 或 Gemini CLI 暴露工具和资源。

MCP 不是数据库连接驱动，也不等于数据库权限。它只规定宿主、客户端和服务器之间如何发现和调用工具。真正连接 MySQL 或 PostgreSQL 的代码位于 Shell 客户端、应用程序或 MCP Server 内。

对于本项目，首期开发不需要建设数据库 MCP。数据库结构应由 Flyway 迁移文件管理，业务读写由 Spring Boot 完成，AI 通过 Shell、Maven 和测试链路协作。需要交互式检查数据库时，再增加一个本地、只读、受限的 MCP Server。

## 主流客户端的实际模式

### Codex CLI

OpenAI 的 Codex 官方仓库将 Codex CLI 定义为运行在本机终端的编码代理。官方实现包含 Shell 工具，工具可以执行用户默认 Shell 命令，并带有审批、沙箱、文件系统权限和网络权限参数。

官方 CLI 源码中的 `codex mcp` 支持：

- `list`、`get`、`add`、`remove`
- 本地 stdio Server
- Streamable HTTP Server
- 从环境变量读取 Bearer Token
- 对支持 OAuth 的远程 Server 执行登录和退出

因此 Codex CLI 可以直接执行数据库客户端，也可以调用 MCP。MCP 不是 Codex 操作数据库的必需条件。

来源：

- [Codex 官方仓库 README](https://github.com/openai/codex)
- [Codex MCP CLI 实现](https://github.com/openai/codex/blob/main/codex-rs/cli/src/mcp_cmd.rs)
- [Codex Shell 工具定义](https://github.com/openai/codex/blob/main/codex-rs/core/src/tools/handlers/shell_spec.rs)
- [Codex 配置类型与 MCP、审批、沙箱配置](https://github.com/openai/codex/blob/main/codex-rs/config/src/config_toml.rs)

### Claude Code

Claude Code 官方文档明确把 MCP 定位为连接工具、数据库和 API 的方式，并直接给出查询数据库的使用场景。

Claude Code 支持：

- 本地 stdio Server
- 远程 HTTP Server
- SSE Server
- WebSocket Server
- 环境变量、请求头和 OAuth 认证
- 项目级、用户级和组织级 MCP 配置
- 具体工具的 allow、ask、deny 权限规则

官方文档推荐远程场景使用 HTTP。SSE 已被标记为过时，只有服务端还不支持 HTTP 时才保留。Bash 命令默认需要审批，文件读取属于低风险只读工具，MCP 工具可以单独设置权限。

来源：

- [Claude Code MCP 官方文档](https://code.claude.com/docs/en/mcp)
- [Claude Code 权限官方文档](https://code.claude.com/docs/en/permissions)

### Gemini CLI

Gemini CLI 官方文档同样提供内置 Shell 工具和 MCP 集成。MCP Server 在 `settings.json` 的 `mcpServers` 中配置。

Gemini CLI 支持：

- stdio
- SSE
- Streamable HTTP
- MCP Server allowlist 和 denylist
- Server 级别的 `trust`
- 工具级别的 `includeTools` 和 `excludeTools`
- 环境变量引用和敏感环境变量自动清理
- Shell 和变更型工具的人工确认
- 沙箱执行

Gemini CLI 文档还说明，环境变量默认会过滤密码、Token、Key 和 Credential 等敏感变量。只有在 Server 配置中显式声明的变量才会传入 MCP Server。

来源：

- [Gemini CLI MCP Server 文档](https://github.com/google-gemini/gemini-cli/blob/main/docs/tools/mcp-server.md)
- [Gemini CLI MCP 配置教程](https://github.com/google-gemini/gemini-cli/blob/main/docs/cli/tutorials/mcp-setup.md)
- [Gemini CLI 工具和确认机制](https://github.com/google-gemini/gemini-cli/blob/main/docs/reference/tools.md)

## 四种数据库操作方案

| 方案 | 实际连接数据库的主体 | 适合场景 | 主要问题 |
| --- | --- | --- | --- |
| Shell 直连 | `mysql`、`psql`、脚本或 SSH 远程命令 | 临时检查、开发排障、运行测试 | 命令权限宽，容易执行破坏性 SQL |
| MCP 数据库 Server | MCP Server 内的 JDBC、驱动或 ORM | 多个 AI 客户端共享数据库工具 | Server 供应链、权限和数据外泄风险 |
| 业务 API 或领域工具 | 应用服务、Repository、领域命令 | 产品运行时、带业务规则的读写 | 需要先建设稳定的接口 |
| 迁移和 CI | Flyway、Liquibase 或部署流水线 | 表结构、索引、固定种子数据 | 不能替代临时查询和排障 |

### Shell 直连

调用链是：

```text
AI 代理 -> 内置 Shell 工具 -> mysql / psql / 脚本 -> 数据库
```

这条链路最简单。AI 只需要知道命令，数据库客户端负责连接数据库。Codex CLI、Claude Code 和 Gemini CLI 都具备执行 Shell 或变更型命令的能力。

它适合开发和排障，不适合作为面向用户的任意 SQL 能力。生产环境应限制数据库账号、命令范围和网络出口。

### MCP 数据库 Server

调用链是：

```text
AI 代理 -> MCP Client -> MCP Server -> 数据库驱动 -> 数据库
```

MCP Server 可以暴露以下类型的工具：

- `list_tables`
- `describe_table`
- 只读查询
- `explain`
- 健康检查
- 受限的数据变更

MCP Server 可以是本地 stdio 进程，也可以是远程 HTTP 服务。数据库密码应保存在 Server 进程的环境变量、密钥管理器或服务端配置中，不应放入提示词和仓库。

MCP 的安全责任仍由宿主和 Server 实现承担。官方规范要求实现用户同意、数据访问控制和工具安全处理，但协议本身不会替实现者限制 SQL、创建数据库账号或阻止越权查询。

来源：

- [MCP 当前规范](https://modelcontextprotocol.io/specification/2026-07-28/index.md)
- [MCP 官方参考 Server 仓库](https://github.com/modelcontextprotocol/servers)

### 业务 API 或领域工具

调用链是：

```text
AI 代理 -> registerUser / findCourse / issueCertificate 等业务工具 -> 应用服务 -> 数据库
```

这是产品内 AI 的推荐模式。工具参数是业务参数，应用负责认证、RBAC、状态机、事务和审计。AI 不需要看到 SQL，也不需要拥有数据库账号。

### 迁移和 CI

表结构变更应进入 Git：

```text
AI 修改迁移文件 -> 集成测试 -> Flyway 应用迁移 -> 验证 -> 代码审查 -> 部署
```

这条链路适合 DDL、索引、约束和固定种子数据。它也是本项目当前应采用的数据库操作方式。

## MCP 数据库 Server 的现状

MCP 官方参考 Server 仓库当前说明，其代码用于展示 MCP 特性和 SDK 用法，不能直接视为生产级解决方案。该仓库列出的 PostgreSQL 和 SQLite 参考 Server 已移动到归档仓库。

MCP 官方 Registry 中仍有大量社区数据库 Server，包含 MySQL、PostgreSQL 和 DBA 运维工具。Registry 的 `active` 状态表示发布记录处于活动状态，不等于代码经过安全审计、性能验证或生产认证。

目前值得单独评估的候选是 Bytebase 的 DBHub。Claude Code 官方 PostgreSQL 示例直接引用它。DBHub 官方仓库声明支持 PostgreSQL、MySQL、SQL Server、MariaDB 和 SQLite，并提供只读模式、返回行数限制、查询超时、SSH 隧道和 TLS 能力。它默认提供 `execute_sql` 和 `search_objects` 两个工具，额外的 `explain_sql`、健康检查和自定义工具可以选择启用。

DBHub 更适合开发环境的 schema 探索、查询验证和只读排障。它默认存在通用 SQL 工具，因此不能因为有只读模式和安全控制就直接连接生产库，仍需使用独立低权限账号、固定版本和人工确认。

因此，选择数据库 MCP Server 时应检查：

- 源码和维护者
- 发行包和容器来源
- 是否固定版本
- 是否支持只读数据库账号
- 是否限制表和操作类型
- 是否限制单次返回行数和执行时间
- 是否支持审计日志
- 是否默认关闭 INSERT、UPDATE、DELETE 和 DDL
- 是否有人工确认机制

来源：

- [MCP 官方参考仓库的生产使用警告](https://github.com/modelcontextprotocol/servers#readme)
- [MCP 官方 Registry](https://registry.modelcontextprotocol.io/)
- [MySQL Server Registry 查询](https://registry.modelcontextprotocol.io/v0.1/servers?search=mysql&limit=20)
- [PostgreSQL Server Registry 查询](https://registry.modelcontextprotocol.io/v0.1/servers?search=postgres&limit=20)
- [DBHub 官方仓库](https://github.com/bytebase/dbhub)

## 推荐决策

### 本项目首期开发

采用：

```text
Flyway 负责结构
Spring Boot 负责业务读写
Testcontainers 负责真实数据库测试
Shell 负责启动、检查和排障
```

暂不建设数据库 MCP。原因是 `#17` 的目标是实现 IAM 业务，不是建设数据库运维助手。增加 MCP 会增加 Server 供应链、密码传递、权限配置和审计成本，当前没有足够收益。

### 需要让 Codex、Claude Code、Gemini CLI 查询开发数据库时

增加一个本地 stdio、只读 MCP Server，数据库账号使用单独的只读用户，例如 `lp_ai_ro`。该用户只授予 `iam`、`content`、`learning` 的 `SELECT` 权限。

推荐工具范围：

- 查看数据库列表
- 查看表和索引
- 查询 `information_schema`
- 执行带行数限制的 SELECT
- 执行 EXPLAIN
- 查看 Flyway 历史

不要提供通用的 `execute_sql` 写权限。结构修改仍由 Flyway 文件完成，业务数据修改仍由应用服务或明确的人工命令完成。

### 需要多个项目和多个 AI 客户端共用数据库能力时

建设团队内部的远程 Streamable HTTP MCP Gateway，并加入：

- OAuth 或服务身份认证
- 数据库环境隔离
- 只读和变更工具分离
- 表和操作 allowlist
- 查询超时和返回行数上限
- 审计日志
- 写操作人工确认
- 生产账号最小权限

远程 MCP 只解决工具复用和连接标准化，不替代数据库迁移、业务权限和发布流程。

### 产品内 AI 操作业务数据时

使用业务 API 或领域工具，不给模型原始数据库连接。模型只能调用明确的业务命令，例如查询自己的学习记录、提交练习或申请证书。应用服务负责检查当前用户、学习资格、课程状态和幂等规则。

这属于产品 AI 能力，当前首期规格明确排除，不能顺手并入 `#17`。

## 推荐流程

### 只读查询

1. 明确环境和数据库账号。
2. 通过 SSH 隧道或本地容器连接。
3. 查看表结构和 Flyway 历史。
4. 使用带 LIMIT、超时和脱敏规则的查询。
5. 检查结果是否包含密码、Token 或个人敏感数据。

### 表结构变更

1. 在 Git 中创建新的 Flyway 迁移文件。
2. 使用 Testcontainers 在空数据库和已有迁移数据库上测试。
3. 通过应用启动或部署流水线运行 Flyway。
4. 检查表、索引、约束和迁移历史。
5. 做 Standards 和 Spec 审查后提交。

### 数据变更

1. 说明目标环境、目标表、条件和预计影响行数。
2. 优先通过应用服务或幂等种子迁移执行。
3. 写操作使用参数绑定、事务和审计。
4. DELETE、批量 UPDATE、TRUNCATE、DROP 需要人工确认和备份。
5. MySQL DDL 可能产生隐式提交，回滚应准备新的修复迁移，不能只依赖事务回滚。

## 本项目的额外风险

- `lp_dev` 在服务器初始化脚本中拥有三个业务库的全部权限，适合开发，不能直接作为生产 AI 账号。
- 服务器数据库只绑定回环地址，AI 需要先通过 SSH 隧道访问，不能假定公网数据库直连可用。
- 当前 Windows 环境适合通过 SSH、Maven 和 JDBC 链路工作，本地未必安装 `mysql` 或 `docker` CLI。
- 根目录 Compose 使用的本地端口和账号配置与服务器 Compose、Spring 默认配置存在差异，使用本地数据库前应先修正并验证。
- 数据库返回内容也可能包含提示注入文本。AI 不应把数据库字段内容当作操作权限或系统指令。

## 资料说明

本文只使用 Codex、Claude Code、Gemini CLI、MCP 官方规范、MCP 官方仓库和 MCP Registry 等一手资料。调研时 Codex 官方开发者手册接口返回 403，因此 Codex 的细节使用 OpenAI 官方 `openai/codex` 源码和仓库文档核对。访问日期为 2026-08-11。
