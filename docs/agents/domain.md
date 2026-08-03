# Domain docs

## Current layout

当前项目采用单上下文结构：

- 根目录 `CONTEXT.md`：项目共享的业务词汇。
- `docs/adr/`：全局架构决策。
- 服务目录暂不创建独立 `CONTEXT.md` 或 `docs/adr/`。

## Review gates

上下文结构在以下节点进行复审：

1. 初始 Gateway、IAM、Content、Learning、Assessment 五个服务的领域边界确定后。
2. 开始实现 `organization-service` 前，强制复审一次。
3. 引入 AI、社区或独立团队协作前，再复审一次。

## Split criteria

只有候选领域同时具备独立业务词汇与不变量、独立业务流程与生命周期、明确的跨领域关系，并且出现实际维护压力时，才拆分上下文。维护压力包括词义冲突、独立发布节奏、独立协作边界或文档定位困难。

不会因为微服务数量增加、文件变长或目录结构变化就自动拆分。

## Split procedure

触发拆分后：

- 根目录新增 `CONTEXT-MAP.md`。
- 每个独立领域拥有自己的 `CONTEXT.md`。
- 领域关系记录在 `CONTEXT-MAP.md`。
- 通用编程术语不进入领域文档。
- 迁移完成后删除根上下文中的重复定义。
