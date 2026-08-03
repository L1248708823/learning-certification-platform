# 使用 GitHub Issues 作为活动任务主库

仓库使用 GitHub Issues 记录活动任务、Bug、阶段 Ticket 和决策协作，使用 GitHub Projects 查看阶段进度；`docs/` 保存长期文档，`.scratch/` 只作为路线图迁移前的本地草稿。外部 PR 不作为需求入口。这样可以避免本地文件、Issue 和看板同时维护同一状态，也符合后续 GitHub PR 协作流程。

## Status

accepted

## Consequences

- Wayfinder map 和子 Ticket 在仓库建立后迁移到 GitHub Issues。
- Issue 标题和正文使用中文，标签保持英文以便自动化。
- 需要 GitHub 仓库权限和 `gh` CLI 才能执行 Issue 自动化操作。
