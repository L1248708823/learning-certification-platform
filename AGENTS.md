## 要求

始终使用 ASD-STE100 简化技术英语/中文进行交流。始终阅读 CONTEXT.md 文件，并使用它们的普遍语言。
必要的时候 把我当成弱智一样进行讲解

### 面向生产实践的学习要求

本仓库是面向生产实践的学习项目。实现时优先采用与当前 Java/Spring 技术栈、项目阶段和既有 ADR 相符的 idiomatic（符合当前技术栈惯例）、长期可维护的方案。

以 YAGNI 和 evolutionary design（随真实需求演进的设计）管理范围：完整实现当前票据，依据已定义的触发条件再引入新的能力、抽象或架构调整。

使用 `CONTEXT.md` 中的 Ubiquitous Language（统一业务语言）。业务状态、枚举和持久化编码应有单一且可追溯的语义来源。

将代码、行为测试和中文文档注释视为 living documentation（随代码保持同步的文档）。新增或修改的字段和方法都应提供中文注释，声明默认使用 Javadoc 格式。

简单字段和方法说明业务含义或职责即可。涉及接口契约、状态变化、持久化编码、默认值、空值、前置条件、副作用、异常、事务或并发时，补充相应约束和原因。

Lombok 自动生成的访问器、框架回调和纯转发成员，可由字段、类或接口的契约注释覆盖。行内注释用于解释局部且无法从代码直接看出的逻辑。

## 阶段门槛与触发检查

后置能力不在首期实现，触发条件达成时必须回到对应决策票据讨论，不得跳过或顺手实现：

| 后置能力 | 触发条件 | 票据 |
| --- | --- | --- |
| 微服务抽取（先拆 Content） | 首期验收通过，且出现跨模块事务协调需求、模块边界被越或拆分演练学习目标 | #14 |
| Commerce 进单体 | 引入商品售卖或订单支付 | #9 |
| 考试 | 练习闭环稳定后出现正式评估需求 | #8 |
| 企业培训 | 组织学习需求出现 | #10 |
| AI 顾问 | 对应需求或学习目标出现 | #11 |
| Kubernetes | 需要多服务统一部署 | #12 |
| 后台基础功能（站内信、操作日志、报表导出、定时任务、日志平台等） | 第二期管理后台与 Commerce 引入时 | docs/roadmap.md |

硬规则：进入新阶段规划前，必须检查 map（#1）仍 OPEN 的决策票据，本阶段不涉及的保持 OPEN 并维持触发条件有效。版本演进计划见 `docs/roadmap.md`，首期验收对照清单见 `docs/spec/0001-个人学习闭环.md`。

## Agent skills

### Issue tracker

GitHub Issues 是活动任务、Bug 和阶段 Ticket 的唯一主库，GitHub Projects 用于阶段视图；外部 PR 不作为需求入口。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用 `needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix` 五组默认标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

当前采用单上下文结构，根目录使用 `CONTEXT.md`，架构决策使用 `docs/adr/`；按明确门槛触发多上下文拆分。详见 `docs/agents/domain.md`。

### Temporary files

需要跨 WSL/Windows 会话传递的交接文档和临时产物统一放在仓库根目录 `.tmp/`，不依赖操作系统临时目录；`.tmp/` 不提交到 GitHub，并在交接完成或阶段结束时清理。详见 `.gitignore`。
