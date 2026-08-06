# 项目服务器 Docker 教学清单

状态：草案，执行旧容器清理前先确认本文件中的决策。

这份清单用于从一台已经存在其他练手项目的 Ubuntu 服务器开始，为
`learning-certification-platform` 建立独立的基础设施环境，并记录每一步的
目的、命令、校验方式和成功标准。

本文件本身只做规划和记录。当前尚未停止容器、删除容器、删除镜像或删除数据卷。

## 1. 目标边界

第一阶段只建立项目自己的基础设施：

- MySQL：保存 IAM、Content、Learning 三个数据库。
- Redis：保存验证码、登出黑名单和缓存。
- MinIO：保存课程封面等对象文件。
- 后端和前端应用暂不放进 Compose，因为仓库当前还没有完整业务代码、前端工程和应用镜像。

项目目标不是“启动三个容器就算完成”。基础设施启动后，还要验证健康状态、数据持久化、重启恢复和后续应用连接。

## 2. 当前服务器基线

| 项目 | 当前情况 | 处理方式 |
| --- | --- | --- |
| 操作系统 | Ubuntu 22.04.5 LTS，x86_64 | 保持，不为本项目重装系统 |
| CPU | 2 核 | 控制容器数量和并发 |
| 内存 | 约 3.3 GiB，无 Swap | 不盲目启动第二个 MySQL |
| 磁盘 | 根分区约 69 GiB，剩余约 45 GiB | 数据卷需要单独记录和备份 |
| Docker CLI | 28.4.0 | 当前已安装，不升级 |
| Docker Compose | v2.39.4 | 当前已安装，不升级 |
| Java | 未安装 | 只有决定在服务器运行后端时才安装 JDK 21 |
| Node.js | v24.13.0 | 前端尚未创建，暂不处理 |
| 项目后端基线 | Spring Boot 3.5.16，Java 21 | 以 `backend/pom.xml` 为准 |

服务器当前已有练手容器。清理时只处理明确列出的容器，不默认处理宿主机的 `mysql.service`。

## 3. 推荐版本基线

版本的原则是：主版本和关键小版本固定，镜像不要使用 `latest`；实际拉取后记录镜像摘要或完整版本。

| 组件 | 推荐版本 | 原因 |
| --- | --- | --- |
| Docker Engine | 现有 28.4.0 | 已满足需求，避免无关升级 |
| Docker Compose | 现有 v2.39.4 | 已满足需求 |
| MySQL | 8.4 LTS | 与仓库现有 Compose 基线一致 |
| Redis | 7.x Alpine | 与仓库现有 Compose 基线一致，资源占用较小 |
| MinIO | 仓库已经固定的 `RELEASE.2025-09-07T16-13-09Z` | 保证可复现，不使用 `latest` |
| Java | JDK 21 LTS | `backend/pom.xml` 明确要求 Java 21 |
| Spring Boot | 3.5.16 | 当前仓库已经拍板的版本 |
| Maven | 仓库 Maven Wrapper | 统一开发机、服务器和 CI 的 Maven 入口 |

MySQL 和 Redis 的精确补丁版本及镜像 digest 在实际拉取后记录。不要为了追求“最新”临时升级版本；学习项目首先需要稳定、可复现和容易排错。

## 4. 必须先做的决策

### 4.1 是否清理旧容器

已知旧容器：

```text
redis
mysql8
backtesting_frontend_1
backtesting_backend_1
```

推荐范围：

- 删除上述旧练手容器。
- 暂不删除镜像。
- 暂不删除数据卷。
- 不停止或删除宿主机 `mysql.service`。

删除容器不等于删除镜像，也不等于删除数据卷；三者必须分开确认。

### 4.2 MySQL 是否完全独立

当前服务器内存较小，完整新增一个 MySQL 容器会增加明显内存压力。

默认建议：先确认现有 MySQL 的归属和用途，再选择以下方案：

- 学习和隔离优先：项目独立 MySQL、Redis、MinIO，必要时升级服务器内存。
- 资源和运维优先：复用现有 MySQL 实例，但创建项目独立数据库和低权限账号。

不能直接把项目连接到现有 MySQL 的 root 账号，也不能因为看到容器空闲就删除其数据卷。

### 4.3 后端运行位置

- 本地运行后端：服务器只运行基础设施，本地通过 SSH 隧道连接。
- 服务器运行后端：需要在服务器安装 JDK 21，并额外规划应用进程、日志和端口。

当前推荐先让服务器只运行基础设施，等后端业务代码形成后再决定应用运行位置。

## 5. 从零开始的操作流程

### 阶段 A：认识环境

目的：确认我们操作的是哪台服务器、有哪些资源、哪些端口和容器已经存在。

```bash
hostname
cat /etc/os-release
uname -m
free -h
df -h /srv/services
docker --version
docker compose version
docker ps -a
ss -ltnp
```

通过标准：

- 操作系统、CPU、内存和磁盘有记录。
- 目标项目目录正确。
- 现有容器和端口有清单。
- 没有在确认前停止或删除任何服务。

### 阶段 B：清理旧练手容器

目的：释放旧容器占用的内存，避免容器名、端口和网络混淆。

执行前必须再次确认目标名称：

```bash
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
```

只对确认过的容器执行停止和删除。不要使用全量清理命令，不要附带卷删除参数。

通过标准：

- 旧练手容器不再运行。
- Docker 镜像和数据卷仍然保留，便于回滚或补查。
- 宿主机 `mysql.service` 状态没有被改变。
- 清理后再次检查内存和端口。

### 阶段 C：准备项目目录

目的：让代码、配置、数据、日志和运行文件有清晰边界。

建议目录：

```text
/srv/services/learning-certification-platform/
  backend/       代码
  frontend/      前端代码
  config/        服务器配置，不放 Git 密钥
  data/mysql/    MySQL 持久化数据
  data/redis/    Redis 持久化数据
  data/minio/    MinIO 持久化数据
  logs/          应用或运维日志
  run/           PID、socket 等运行文件
  docs/          项目和服务器文档
```

配置文件权限建议为 `600`。真实密码不写进 Git、不写进教学文档、不放进镜像。

### 阶段 D：建立独立 Compose

目的：让项目拥有自己的容器名、网络、数据目录和生命周期。

建议使用项目专属前缀：

```text
learning-platform-mysql
learning-platform-redis
learning-platform-minio
learning-platform-network
```

服务器端口只绑定到 `127.0.0.1`，建议初始规划为：

```text
127.0.0.1:13306 -> MySQL 3306
127.0.0.1:16379 -> Redis 6379
127.0.0.1:19000 -> MinIO API 9000
127.0.0.1:19001 -> MinIO Console 9001
```

如果后端最终也在同一个 Compose 网络中运行，应用应使用服务名连接，例如 `mysql:3306`，不能在容器内使用 `localhost:13306`。

### 阶段 E：配置检查，不启动

目的：先发现 YAML、环境变量、端口和路径错误。

```bash
docker compose -p learning-platform -f compose.server.yaml config
ss -ltnp
df -h /srv/services
free -h
```

通过标准：

- Compose 能输出完整配置。
- 端口没有冲突。
- 数据目录可写。
- 配置中没有真实密码被提交或打印。

### 阶段 F：拉取镜像并启动

先拉取，再启动，便于区分网络问题和容器问题：

```bash
docker compose -p learning-platform -f compose.server.yaml pull
docker compose -p learning-platform -f compose.server.yaml up -d
docker compose -p learning-platform -f compose.server.yaml ps
```

通过标准：

- 三个服务的容器名、网络和数据目录都是项目专属的。
- 服务状态为运行中并通过健康检查。
- 没有端口绑定失败、权限失败或反复重启。

### 阶段 G：逐个健康检查

MySQL：确认服务可响应，之后再确认三个数据库是否存在。

Redis：执行 `PING`，期望返回 `PONG`。

MinIO：访问健康端点，期望返回成功状态；业务接入前再创建项目专属 bucket 和访问策略。

容器健康不等于业务正确。健康检查只证明进程和基础依赖可用，不证明应用 SQL、权限、迁移和业务流程正确。

### 阶段 H：持久化与重启检查

目的：证明容器重启不会丢数据。

流程：

1. 写入专门的测试标记，不使用真实业务数据。
2. 重启 Compose 服务。
3. 再次读取测试标记。
4. 检查容器健康状态和日志。

注意：普通 `docker compose down` 通常保留数据卷；`docker compose down -v` 会删除项目卷，教学演示前必须明确提醒并再次确认。

### 阶段 I：应用连接检查

当前仓库后端尚未实现业务功能，现有测试还排除了数据源和 Flyway，因此当前冒烟测试不能证明数据库真的可用。

后端实现后需要分别验证：

- 应用能连接 MySQL。
- Flyway 能执行迁移。
- Redis 连接成功。
- MinIO 上传和读取成功。
- Actuator health 返回 `UP`。
- API 层错误、权限和超时行为符合预期。

## 6. 什么算成功

### 基础设施成功

- 项目自己的 Compose 可以重复启动。
- MySQL、Redis、MinIO 容器互不依赖旧练手容器。
- 端口只监听本机或明确的内网地址。
- 三个服务健康检查通过。
- 服务重启后数据仍在。
- 日志没有持续重启、权限错误或连接失败。
- 所有真实密码都不在 Git 和文档中。

### 后端基线成功

- JDK 21 和 Maven Wrapper 可用。
- Spring Boot 应用可以启动。
- Flyway 迁移成功。
- `/actuator/health` 返回 `UP`。
- 后端测试通过，且测试覆盖真实数据库、Redis 和 MinIO 的集成场景。

### 首期业务成功

只有注册登录、课程浏览、免费报名、学习进度、练习判分、课程完成和证书生成全部通过规格验收，才算完成首期业务，不以“容器启动”代替业务完成。

## 7. 必须注意的风险

- Docker 容器在运行，不代表应用真的连接到了它。
- 容器内的 `localhost` 指向当前容器，不指向 MySQL 或 Redis 容器。
- Compose 初始化 SQL 通常只在空数据目录第一次初始化时执行；修改脚本不会自动修复已有数据卷。
- Flyway 迁移和 MySQL 初始化脚本是两套机制，职责不能混淆。
- `latest` 会导致同一份配置在不同时间拉到不同版本。
- MySQL、Redis、MinIO 不应直接暴露公网。
- 没有 Swap 的小内存服务器容易发生 OOM；健康检查不会预警所有资源问题。
- `docker inspect`、容器环境变量和启动日志可能泄露密码。
- 数据卷需要备份，备份还需要实际恢复测试。
- 宿主机 MySQL 与 Docker MySQL 是两个不同实例，不能只看端口名称判断它们是否相同。
- 文件和目录权限可能导致容器启动成功但无法写数据。
- 重启策略只能负责重新拉起容器，不能替代备份、监控和迁移管理。
- 服务器直接运行后端与本地通过 SSH 隧道运行后端，配置方式不同，不能混用 `localhost`。

## 8. 当前不做的事情

- 不安装 Kubernetes、注册中心、配置中心或消息队列。
- 不为了“完整”提前创建前端、Gateway 或应用容器。
- 不直接删除所有 Docker 镜像和数据卷。
- 不停止宿主机 MySQL，除非单独确认并制定回滚方案。
- 不把开发密码当作生产密码。
- 不把容器健康误认为首期业务验收通过。

## 9. 操作记录要求

每次服务器操作记录以下内容：

- 操作日期和服务器名称。
- 执行的命令和命令目的。
- 操作前后的容器、端口、内存和磁盘状态。
- 镜像版本和 digest。
- 成功或失败结果。
- 失败时的日志、原因和回滚方式。
- 是否产生数据，以及数据备份位置。

不记录真实密码、Token、私钥和完整环境变量。

## 10. 参考文件

- `compose.yaml`
- `backend/pom.xml`
- `docs/spec/0001-个人学习闭环.md`
- `learning-records/0003-远程Docker环境方案.md`
- `/srv/services/PROJECTS.md`
