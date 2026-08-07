# Flyway 命令速查表

状态：持续维护；只列命令，不管概念。概念和"为什么"看
[06-Flyway概念与入门.md](06-Flyway概念与入门.md)，项目服务器部署细节看
[01-服务器Docker教学清单.md](01-服务器Docker教学清单.md)。

日常开发里 Flyway 多数时候不用手动敲命令，应用启动时它自动执行迁移。只有少数场景需要
主动干预，下面按场景分组。

## 1. 迁移脚本命名

| 文件名 | 含义 |
| --- | --- |
| `V1__baseline.sql` | 基线迁移，标记库的起点，正文可以是注释 |
| `V2__create_user_table.sql` | 后续迁移，按版本号从小到大执行 |
| `V<版本>__<描述>.sql` | 通用格式，`__` 前后分别是版本号和描述 |

规则：版本号只增不减，已执行过的脚本内容不要改。要改结构就新建更高版本的脚本。

## 2. 启动时自动执行

| 场景 | 执行内容 |
| --- | --- |
| 本地 `./mvnw spring-boot:run` 启动 | 自动建 history 表并跑未执行的迁移 |
| 服务器容器启动后端 | 同上，容器内同样生效 |
| 看到 `Successfully applied 1 migration` | 说明迁移执行成功 |

项目配置在 application.yml 的 `spring.flyway.enabled: true` 和
`locations: classpath:db/migration`，迁移脚本放在 backend/src/main/resources/db/migration。

## 3. 查询迁移状态

| 命令 | 作用 |
| --- | --- |
| `docker compose exec -T mysql mysql -uroot -p密码 iam -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"` | 查看 history 表，哪些迁移已执行 |
| `docker compose logs 后端服务名` | 看 Flyway 启动日志 |

history 表 `flyway_schema_history` 在 iam 库，常用列：

| 列 | 含义 |
| --- | --- |
| `installed_rank` | 执行顺序 |
| `version` | 迁移版本号 |
| `description` | 迁移描述 |
| `success` | 是否成功，0 表示失败 |

## 4. 手动干预命令（Maven 插件）

需要手动跑迁移、修复、校验时，在 backend 目录执行。这些命令依赖 Maven 的 Flyway 插件
配置，项目当前没配，要用时先在 pom.xml 加插件，再执行下面的命令。

| 命令 | 作用 |
| --- | --- |
| `./mvnw flyway:migrate` | 手动执行未应用的迁移，平时启动已自动执行，一般用不到 |
| `./mvnw flyway:info` | 查看哪些迁移已应用、哪些待应用 |
| `./mvnw flyway:validate` | 校验脚本与 history 表是否一致 |
| `./mvnw flyway:repair` | 修复 history 表与脚本不一致的问题 |

## 5. 危险命令

| 命令 | 危险等级 | 后果 |
| --- | --- | --- |
| `./mvnw flyway:clean` | 高 | 清空库内全部表和数据，项目未配插件前不可直接执行 |
| 手动改已执行的迁移脚本 | 高 | 下次启动校验失败，且与已执行记录不一致 |

`clean` 只清理 Flyway 管理的对象，不会自动重建，执行前务必确认目标库和备份。日常
维护用不上，真正需要的是修结构和加脚本，方向是"新版本号"，不是改旧脚本。

## 6. 报错对照

| 报错特征 | 原因 | 处理 |
| --- | --- | --- |
| `Communications link failure` / `Connection refused` | 隧道没开或端口不对 | `Test-NetConnection 127.0.0.1 -Port 13306` 确认隧道 |
| `Access denied for user 'lp_dev'@'...'` | 密码不对或环境变量没设 | 设 `DB_PASSWORD`，IDE 里在运行配置设 |
| `Validate failed` | 脚本内容被改或删除 | 不改旧脚本，新改动开新版本号 |
