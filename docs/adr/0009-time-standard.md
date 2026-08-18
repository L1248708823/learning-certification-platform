# 时间口径：全链路固定 Asia/Shanghai（UTC+8）

所有时间写入和展示统一按 Asia/Shanghai 固定，不做跨时区转换。数据库服务器、应用 JVM、容器环境、接口输出全部钉在 +8。产品面向中国大陆用户，中国无夏令时，固定 +8 不存在时区跳变。

## Status

accepted

## Considered Options

- **全链路固定 +8**：`created_at` 等列由数据库 `CURRENT_TIMESTAMP` 写入，取服务器本地时间。把数据库容器、JVM、应用统一钉成 Asia/Shanghai，语义最简单，无夏令时跳变，前端也少一层转换。代价是要显式配置数据库容器 `TZ`、JVM `-Duser.timezone`（或容器 `TZ`）、JDBC `connectionTimeZone`，否则 Docker 镜像默认 UTC 会和本地开发相差 8 小时。
- **存储 UTC、展示转换**：数据库和 JVM 存 UTC，接口或前端展示时转 +8。符合国际化工程惯例，日志跨机器对比方便，但每个展示点都要做一次转换，前端也得配合。当前产品没有跨时区用户，收益为零，复杂度全落在眼前。
- **跟随系统时区**：什么都不配，时间随服务器系统时区漂移。Docker 镜像默认 UTC，云服务器又各有差异，同一套代码在不同环境差 8 小时，最不可控。

## Decision

采用全链路固定 +8。语义约定：`LocalDateTime` 一律指上海本地时间，接口不返回时区偏移。

## Consequences

- 数据库容器：compose.yaml 的 MySQL 已配置 `TZ: Asia/Shanghai`，`CURRENT_TIMESTAMP` 写入即 +8，无需改动。
- 应用 JVM：后端首次容器化时必须在镜像环境设置 `TZ=Asia/Shanghai`（或 JVM `-Duser.timezone=Asia/Shanghai`），保证 `LocalDateTime.now()` 与数据库口径一致。application.yml 可加 `spring.jackson.time-zone: Asia/Shanghai` 兜底时间字段序列化，JDBC URL 加 `connectionTimeZone=Asia/Shanghai` 明确服务端会话时区。
- 重新评估触发条件：出现跨时区用户、或审计或合规要求时间统一按 UTC 记录时，再回到本决策讨论迁移路径。
