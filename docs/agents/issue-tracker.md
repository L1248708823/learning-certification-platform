# Issue tracker: GitHub

## Canonical source

仓库 `learning-certification-platform` 使用 GitHub Issues 作为活动任务、Bug、阶段 Ticket 和决策协作的唯一主库，GitHub Projects 用于按阶段查看进度、依赖和阻塞关系。

长期有效的产品和技术文档保存在仓库的 `docs/` 目录，领域词汇保存在根目录 `CONTEXT.md`，架构决策保存在 `docs/adr/`。

`.scratch/` 只用于路线图迁移前的本地草稿，不与 GitHub Issues 并行维护同一批活动任务。

## Pull requests as a request surface

否。外部 PR 不作为需求入口，也不进入 triage 流程。PR 主要用于项目成员、本人和 AI 协作产生的代码变更。

## Operations

- 创建 Issue：使用 GitHub Issue，标题使用中文，正文包含背景、问题、范围和验收条件。
- 创建 PR：关联对应 Issue，说明变更、验证方式、风险和未完成事项。
- 阶段管理：使用 GitHub Projects 表示阶段状态，不在多个文档中复制活动任务状态。
- Wayfinder map：建立在 GitHub Issue 上，使用 `wayfinder:map` 标签；子 Ticket 使用 `wayfinder:<type>` 标签。
- 依赖关系：优先使用 GitHub 原生 Issue dependencies；不可用时在正文记录 `Blocked by`。
- 任务身份：Issue 标题是人类可读名称，正文和 PR 中不要只使用编号代替名称。
