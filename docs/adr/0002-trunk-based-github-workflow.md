# 使用短生命周期分支和 Pull Request 协作

项目使用 `main` 作为唯一长期分支，功能和修复通过短生命周期分支开发，并经过 Pull Request、自动检查和人工验收后合并；合并采用 Squash Merge，阶段完成后使用版本标签。项目不设置长期存在的 `develop` 分支，以降低个人与 AI 协作时的分支同步成本，同时保留真实团队协作所需的审查和质量门槛。

## Status

accepted

## Workflow

- 功能分支：`feat/<ticket>-<slug>`
- 修复分支：`fix/<ticket>-<slug>`
- `main` 保持可运行
- PR 必须通过 lint、测试和构建检查
- 阶段版本使用 `v0.1.0` 形式的标签
