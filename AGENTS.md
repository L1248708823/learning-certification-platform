## Agent skills

### Issue tracker

GitHub Issues 是活动任务、Bug 和阶段 Ticket 的唯一主库，GitHub Projects 用于阶段视图；外部 PR 不作为需求入口。详见 `docs/agents/issue-tracker.md`。

### Triage labels

使用 `needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix` 五组默认标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

当前采用单上下文结构，根目录使用 `CONTEXT.md`，架构决策使用 `docs/adr/`；按明确门槛触发多上下文拆分。详见 `docs/agents/domain.md`。

### Temporary files

需要跨 WSL/Windows 会话传递的交接文档和临时产物统一放在仓库根目录 `.tmp/`，不依赖操作系统临时目录；`.tmp/` 不提交到 GitHub，并在交接完成或阶段结束时清理。详见 `.gitignore`。
