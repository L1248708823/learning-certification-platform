# Flyway 概念与入门

状态：持续维护；面向本地开发与服务器运维的入门学习，Flyway 数据库迁移一起讲。
速查用途请看 [07-Flyway命令速查表.md](07-Flyway命令速查表.md)，项目服务器部署细节看
[01-服务器Docker教学清单.md](01-服务器Docker教学清单.md)。

这份文档回答一个问题：为什么一个只管执行 SQL 的工具，会变成数据库结构的"版本管理"。
先建立核心概念，再把概念落到项目里的配置和真实日志上。

## 1. 核心概念：迁移脚本、版本号、history 表

### 1.1 迁移脚本：一笔提前写好的账

日常改动数据库结构，你可能会打开 MySQL 客户端敲一条 ALTER TABLE 就完事。问题在于没人
记录这条 SQL 是哪天、为什么敲的，换一台机器、换一个人，就不知道结构该是什么样。

Flyway 把结构改动写成文件，一个文件叫一个迁移脚本（migration），按文件名里的版本号
从小到大依次执行。它就像是记账，每一笔改动都写下来，Flyway 照着账本一笔一笔复现。

### 1.2 版本号：账本上的页码

迁移脚本的文件名有固定格式：`V<版本号>__<描述>.sql`。版本号决定执行顺序，比如：

```text
V1__baseline.sql
V2__create_user_table.sql
V3__add_email_column.sql
```

Flyway 先跑 V1，再跑 V2，再跑 V3，永远不会乱序。文件名里的 `__` 是两个下划线，前面是
版本号，后面是这段迁移想干什么，给人和日志看。项目里的基线迁移就是
`V1__baseline.sql`。

### 1.3 history 表：已经盖过章的账本

Flyway 每次执行完一个迁移，会在数据库里记一笔。这个记录放在一张专门的表里，叫
`flyway_schema_history`。它像是账本上盖的章，记着哪些迁移已经执行过、什么时间执行的、
执行结果如何。

下次启动时，Flyway 只看两件事：账本上已经盖了哪些章，文件系统里有哪些迁移脚本。
两者一比，没盖过章的按版本号顺序补上，盖过章的跳过。这就是它不会重复执行同一段 SQL
的原因。项目里这张表落在 iam 库，启动日志里那句
`Creating Schema history table iam.flyway_schema_history` 就是在建这张账本。

### 1.4 baseline：承认历史遗留的老账

现实里经常会遇到"库已经存在，里面已经有一些手工建的表"。这时候直接跑 Flyway 会报
"库不为空，不知道从哪开始"。baseline 就是告诉 Flyway：这个库的现状，我当作版本 1，
你从这往后接管。它只往 history 表里记一个版本号，不执行任何 SQL。

项目里 V1__baseline.sql 的正文只有注释，正是这个道理。三个库由 Docker Compose 的
初始化脚本创建（见 [04-Docker常用命令与概念.md](04-Docker常用命令与概念.md) 的
init 脚本），Flyway 不重复建库，只标记"基线从版本 1 开始"，后续业务表从 V2 开始落。

## 2. 三库方案：一套 Flyway 管三个库

项目是 iam / content / learning 三个库，但 Flyway 只配了一个数据源，指向 iam。
看 backend/src/main/resources/application.yml：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:13306/iam?...}
  flyway:
    enabled: true
    locations: classpath:db/migration
```

`locations` 是迁移脚本放哪，`classpath:db/migration` 对应
backend/src/main/resources/db/migration 目录。单数据源只连 iam 一个库，history 表自然
落在 iam。content 和 learning 的表不是靠切换数据源，而是靠全限定表名直接写
`content.course`、`learning.learning_eligibility`，这一点在 V1__baseline.sql 的注释里
约定过，也是三库方案的关键。

## 3. Flyway 启动时到底干了什么

有了上面的概念，再看实际日志就不会懵。本地启动后端的完整链路是：

1. HikariCP 拿到数据库连接，日志出现 `HikariPool-1 - Starting...`
2. Flyway 连接数据库，日志出现 `Database: jdbc:mysql://... (MySQL 8.4)`
3. 查 history 表，日志出现 `Schema history table iam.flyway_schema_history does not exist yet`
4. 建 history 表，日志出现 `Creating Schema history table ...`
5. 执行没跑过的迁移，日志出现 `Migrating schema iam to version "1 - baseline"`
6. 记一笔账，日志出现 `Successfully applied 1 migration to schema iam, now at version v1`

最后 Spring Boot 才报告 `Started LearningPlatformApplication`。所以看到
`Successfully applied` 就说明数据库结构这块真的落地了。

## 4. 常见报错排查

Flyway 是启动链路里最先碰数据库的一环，报错基本都在它这暴露。项目里实际踩过的两种：

### 4.1 Communications link failure：网络到不了数据库

日志最后一行是 `Connection refused`，说明 SQL 包发出去了，服务器一个字没回。多半是
SSH 隧道没开，本地 13306 端口没人监听。先在另一个窗口确认隧道，`Test-NetConnection
127.0.0.1 -Port 13306` 返回 True 再启动后端。这一步和数据库本身无关，先排除网络。

### 4.2 Access denied for user 'lp_dev'@'...'：账号密码不对

报错里的 `Access denied ... (using password: YES)` 说明网络通了、MySQL 也收到请求了，
只是身份验证没过。常见原因是环境变量 DB_PASSWORD 没设，后端拿着 application.yml 里的
中文占位符去认证，必然被拒。IDE 启动时要在运行配置的"环境变量"里填真实密码，只在你
手动启动的那个终端窗口设是传不过去的。

### 4.3 Validate failed：本地脚本和已执行记录对不上

改了已执行过的迁移脚本内容，或文件被删了，Flyway 会拒绝启动，提示校验失败。这是它的
安全机制，防止历史账目被篡改。正确做法是不改旧脚本，新改动开新版本号。项目目前还是
单迁移基线，等 V2 落地后这个约束会开始生效。

## 5. 更完善的部分：进阶功能（预告）

入门范围之外，Flyway 还有几个常用能力，先知道存在，需要时再展开。

`repair` 修复 history 表与脚本不一致，`validate` 单独校验脚本与账本是否匹配，`clean`
清空整个库的数据和结构，危险度极高。Maven 项目里这些是通过 mvn 插件命令触发的，具体
命令见 [07-Flyway命令速查表.md](07-Flyway命令速查表.md)。

## 6. 验证你已经懂了

能回答下面三个问题，说明核心概念通了：

- history 表是干什么的？Flyway 怎么做到不重复执行同一个迁移？
- 为什么项目里 V1__baseline.sql 只有注释，却没有建任何表？
- `Access denied` 和 `Communications link failure` 分别说明网络和认证到哪一步了？

这三个问题的答案对应第 1、2、4 节的核心内容，想不起来就回去翻对应章节。
