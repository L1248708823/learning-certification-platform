# 使用 Maven 单模块分包承载模块化单体

后端采用 Maven 单模块（一个 pom、一个可执行 jar），三个业务模块（IAM / Content / Learning）通过包分层隔离：`com.learningplatform.iam`、`com.learningplatform.content`、`com.learningplatform.learning`。不拆 Maven 多模块。

## Status

accepted

## Considered Options

- **Maven 多模块**：backend 下按 iam / content / learning 拆成多个 artifact，再加一个 app 模块聚合启动。编译期强制模块边界，模块可独立复用。但当前只有一个可部署单元（单数据源、单 Flyway 实例、单进程），多模块带来的构建复杂度（reactor、parent pom、模块间依赖管理）对初学者是额外负担，部署隔离收益为零；抽取微服务时仍要重新组织构建结构，现在多模块省下的迁移成本有限。
- **Maven 单模块 + 包分层**：一个 pom、一个 jar，包名承载模块归属。构建概念最少，与"单数据源、单 Flyway 实例"的形态直接对应；模块边界靠包规范和代码评审维护，编译期不强制。

## Decision

采用 Maven 单模块 + 包分层。跨模块访问纪律（跨模块读走模块接口、事务外、禁止直接查表）从第一天在代码评审中执行，不引入 ArchUnit / Spring Modulith 自动验证（两者留在调研文档 B 层清单，微服务阶段再评估）。

## Consequences

- 包结构成为模块边界的事实载体，模块间 import 方向靠评审纪律保证。
- 抽取微服务时的一次性构建重构成本，由「微服务抽取」学习目标本身吸收，届时多模块本身就是学习内容（ADR 0006 门槛）。
- 仓库根目录保持语言无关：backend / frontend / docs / deploy 并列，compose.yaml 在根目录管理基础依赖。
- 重新评估触发条件：ADR 0006 的 Content 抽取门槛达成，或模块边界在代码中频繁被越。
