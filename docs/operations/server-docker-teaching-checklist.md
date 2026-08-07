# 项目服务器 Docker 教学清单

状态：基础设施已完成（2026-08-07）；后端应用尚未部署，备份恢复演练尚未完成。

这份清单用于从一台已经存在其他练手项目的 Ubuntu 服务器开始，为
`learning-certification-platform` 建立独立的基础设施环境，并记录每一步的
目的、命令、校验方式和成功标准。

本文件既是规划也是操作记录。服务器上的基础设施已经按本文件执行完成；文中同时保留
“为什么这样做”和“以后如何亲自操作”，方便学习和复盘。真实密码、Token 和私钥不写入
本文件。

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
| 内存 | 约 3.3 GiB，无 Swap；三项基础设施启动后可用约 1.4 GiB | 不再增加重量级服务，后续评估是否增加 Swap |
| 磁盘 | 根分区约 69 GiB；部署后剩余约 44 GiB，使用率 35% | 数据卷需要单独记录和备份 |
| Docker CLI | 28.4.0 | 当前已安装，不升级 |
| Docker Compose | v2.39.4 | 当前已安装，不升级 |
| Java | 未安装 | 只有决定在服务器运行后端时才安装 JDK 21 |
| Node.js | v24.13.0 | 前端尚未创建，暂不处理 |
| 项目后端基线 | Spring Boot 3.5.16，Java 21 | 以 `backend/pom.xml` 为准 |

服务器原有练手容器已经清理。宿主机的 `mysql.service` 保持不变；它仍监听 `0.0.0.0:3306`，
与本项目的 Docker MySQL（`127.0.0.1:13306`）是两个不同实例。

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

### 4.1 是否清理旧容器（已执行）

已知旧容器：

```text
redis
mysql8
backtesting_frontend_1
backtesting_backend_1
```

已确认并执行的范围：

- 停止并删除上述四个旧练手容器。
- 保留 Docker 镜像。
- 保留 Docker 数据卷。
- 不停止、不删除宿主机 `mysql.service`。

删除容器不等于删除镜像，也不等于删除数据卷；三者必须分开确认。

### 4.2 MySQL 是否完全独立（已决策）

当前服务器内存较小，完整新增一个 MySQL 容器会增加明显内存压力。

本次选择项目独立 MySQL，原因是学习阶段需要明确隔离边界，且旧容器属于练手项目。
项目使用 `mysql:8.4`，宿主机原有 MySQL 不参与本项目。

以后如果服务器资源不足，可以重新评估以下方案：

- 学习和隔离优先：项目独立 MySQL、Redis、MinIO，必要时升级服务器内存。
- 资源和运维优先：复用现有 MySQL 实例，但创建项目独立数据库和低权限账号。

不能直接把项目连接到现有 MySQL 的 root 账号，也不能因为看到容器空闲就删除其数据卷。

### 4.3 后端运行位置

- 本地运行后端：服务器只运行基础设施，本地通过 SSH 隧道连接。
- 服务器运行后端：需要在服务器安装 JDK 21，并额外规划应用进程、日志和端口。

当前仍让服务器只运行基础设施，等后端业务代码形成后再决定应用运行位置。

### 4.4 账户和密钥（已决策）

- 使用脚本在服务器上随机生成 MySQL root、应用账号、Redis 和 MinIO 密码。
- `generate-env.sh` 只在环境文件不存在时生成，不会覆盖已有密钥。
- 密钥文件位于 `/etc/learning-platform/learning-platform.env`，权限为 `600`。
- 密钥不进入 Git、不写入教学文档、不通过容器日志打印。
- 应用连接使用 `lp_dev`，不使用 MySQL root；root 只用于初始化和维护。

“随机生成”解决了密码强度问题，但没有解决密码丢失、轮换和备份问题。后续需要把
凭据放入个人密码管理器，并单独学习轮换流程；不能把环境文件复制到代码仓库。

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

目的：让代码、服务器配置、持久化数据、备份和运行文件有清晰边界。

代码和文档保留在项目目录，持久化数据不要放进 Git 工作区：

```text
/srv/services/learning-certification-platform/
  backend/       代码
  frontend/      前端代码
  docs/          项目和服务器文档

/srv/data/learning-certification-platform/
  mysql/         MySQL 持久化数据
  redis/         Redis 持久化数据
  minio/         MinIO 持久化数据

/etc/learning-platform/
  learning-platform.env  服务器密钥和环境配置

/srv/backups/learning-certification-platform/
  mysql/         MySQL 备份
  minio/         MinIO 备份或导出记录

/var/log/learning-platform/  应用和运维日志
/run/learning-platform/      PID、socket 等运行文件
```

Compose 使用环境变量或外部环境文件引用 `/srv/data/learning-certification-platform/`，不把服务器绝对路径和真实密码写死在仓库中。

`/etc/learning-platform/learning-platform.env` 权限建议为 `600`。真实密码不写进 Git、不写进教学文档、不放进镜像。

如果为了本地演示临时把数据放回项目目录，必须额外忽略 `/data/`、`/logs/`、`/run/` 和服务器环境文件；忽略规则不能替代密码管理。

### 阶段 D：建立独立 Compose

目的：让项目拥有自己的容器名、网络、数据目录和生命周期。

本次实际使用的项目专属名称：

```text
lp-mysql
lp-redis
lp-minio
learning-platform-server_default
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
./deploy/server/compose.sh config --quiet
ss -ltnp
df -h /srv/data/learning-certification-platform
free -h
```

`compose.sh` 统一注入 `/etc/learning-platform/learning-platform.env`、Compose 文件和项目名，
避免每次手敲参数时漏掉环境文件。静态配置检查已通过。

通过标准：

- Compose 能输出完整配置。
- 端口没有冲突。
- 数据目录可写。
- 配置中没有真实密码被提交或打印。

### 阶段 F：拉取镜像并启动

先拉取，再启动，便于区分网络问题和容器问题：

```bash
./deploy/server/compose.sh pull
./deploy/server/compose.sh up -d mysql
./deploy/server/compose.sh up -d redis
./deploy/server/compose.sh up -d minio
./deploy/server/compose.sh ps
```

本次按 MySQL、Redis、MinIO 的顺序启动，并在每一步之间观察健康状态和内存。逐个启动的
好处是故障定位清楚；三项都稳定后，日常可以直接使用 `up -d` 一次启动全部服务。

通过标准：

- 三个服务的容器名、网络和数据目录都是项目专属的。
- 服务状态为运行中并通过健康检查。
- 没有端口绑定失败、权限失败或反复重启。

### 阶段 G：逐个健康检查

MySQL：确认服务可响应，之后再确认三个数据库是否存在。

Redis：执行 `PING`，期望返回 `PONG`。

MinIO：访问健康端点，期望返回成功状态；业务接入前再创建项目专属 bucket 和访问策略。

容器健康不等于业务正确。健康检查只证明进程和基础依赖可用，不证明应用 SQL、权限、迁移和业务流程正确。

本次实际结果：

| 服务 | 校验 | 结果 |
| --- | --- | --- |
| MySQL | Compose healthcheck；查询数据库和 `lp_dev` 授权 | 通过；存在 `iam`、`content`、`learning`，应用用户拥有三库权限 |
| Redis | 带密码执行 `redis-cli PING` | 返回 `PONG` |
| MinIO | Compose healthcheck；服务器本机访问 `/minio/health/live` | 通过；返回 HTTP 成功状态 |

注意：在受限沙箱中直接访问服务器的 `127.0.0.1:19000` 曾失败，切换到服务器权限后成功。
检查网络服务时，必须先确认命令运行在哪个网络命名空间；“检查位置不对”会制造假故障。

### 阶段 H：持久化与重启检查

目的：证明容器重启不会丢数据。

流程：

1. 写入专门的测试标记，不使用真实业务数据。
2. 重启 Compose 服务。
3. 再次读取测试标记。
4. 检查容器健康状态和日志。

注意：普通 `docker compose down` 通常保留数据卷；`docker compose down -v` 会删除项目卷，教学演示前必须明确提醒并再次确认。

本次实际演练使用了专用临时探针，结果如下：

1. MySQL 创建 `ops_persistence_probe` 临时数据库，重启 `lp-mysql` 后读取到 `mysql-ok`。
2. Redis 写入 `_ops:persistence_probe`，重启 `lp-redis` 后读取到 `redis-ok`；Compose 已启用 AOF。
3. MinIO 创建 `lp-persistence-probe` 临时 bucket 和对象，重启 `lp-minio` 后读取到 `minio-ok`。
4. 三项探针数据均已删除，没有留下业务污染。

这证明了本次绑定目录和服务重启恢复有效，但不等于已经完成备份恢复验证。容器重启只能
应对进程或容器故障，无法应对磁盘损坏、误删和服务器丢失；备份仍是下一阶段任务。

### 阶段 I：应用连接检查

当前仓库后端尚未实现业务功能，现有测试还排除了数据源和 Flyway，因此当前冒烟测试不能证明数据库真的可用。

后端实现后需要分别验证：

- 应用能连接 MySQL。
- Flyway 能执行迁移。
- Redis 连接成功。
- MinIO 上传和读取成功。
- Actuator health 返回 `UP`。
- API 层错误、权限和超时行为符合预期。

### 阶段 J：本地通过 SSH 隧道连接

当前服务器端口只监听本机，所以本地电脑不能直接访问服务器的 `13306`、`16379`、`19000`
和 `19001`。SSH 隧道把“本地端口”安全转发到“服务器本机端口”，不需要把数据库暴露到公网。

在本地电脑执行，替换尖括号中的 SSH 信息：

```bash
ssh -N \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=60 \
  -o ServerAliveCountMax=3 \
  -L 13306:127.0.0.1:13306 \
  -L 16379:127.0.0.1:16379 \
  -L 19000:127.0.0.1:19000 \
  -L 19001:127.0.0.1:19001 \
  <ssh-user>@<server-host>
```

隧道保持运行时，本地工具使用以下地址：

| 用途 | 本地地址 |
| --- | --- |
| MySQL | `127.0.0.1:13306`，用户 `lp_dev`，数据库 `iam` |
| Redis | `127.0.0.1:16379`，使用服务器生成的 Redis 密码 |
| MinIO API | `http://127.0.0.1:19000` |
| MinIO Console | 浏览器打开 `http://127.0.0.1:19001` |

本地 Java 进程此时使用本地端口；如果以后把后端也放入同一个 Compose，容器内配置应改为
`mysql:3306`、`redis:6379`、`minio:9000`，不要把 SSH 隧道地址带进容器。

## 6. 什么算成功

### 基础设施成功

- 项目自己的 Compose 可以重复启动。
- MySQL、Redis、MinIO 容器互不依赖旧练手容器。
- 端口只监听本机或明确的内网地址。
- 三个服务健康检查通过。
- 服务重启后数据仍在。
- 日志没有持续重启、权限错误或连接失败。
- 所有真实密码都不在 Git 和文档中。

本次已经达到“服务器基础设施可供本地开发连接”的成功标准；尚未达到“生产可用”标准，
因为还没有应用迁移、备份恢复演练、监控告警和生产级密钥管理。

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
- 服务器没有 Swap，当前可用内存约 1.4 GiB；后续启动 Java、构建前端或并行运行更多服务前必须重新观察内存。
- 宿主机 `0.0.0.0:3306` 不是本项目端口；它仍有公网暴露风险，应另行确认防火墙和宿主机 MySQL 的用途，不能误以为本项目已经解决该风险。
- MinIO 的 root 凭据只用于管理；应用接入时应创建项目 bucket 和最小权限的应用账号，不要让业务代码使用 root。
- Redis 已开启 AOF，但 AOF 不是备份；Redis 数据用途变化（例如登出黑名单）后，不能再把它简单当作可随时删除的缓存。

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

### 9.1 本次实际执行记录（2026-08-07）

#### 操作范围和决策

- 项目目录：`/srv/services/learning-certification-platform`。
- 删除了旧练手容器：`redis`、`mysql8`、`backtesting_frontend_1`、`backtesting_backend_1`。
- 保留旧镜像和数据卷；保留宿主机 `mysql.service`。
- 新项目数据目录：`/srv/data/learning-certification-platform/`。
- 新项目备份目录：`/srv/backups/learning-certification-platform/`。
- 新项目密钥文件：`/etc/learning-platform/learning-platform.env`，只在服务器保存。
- 没有执行 `docker compose down -v`，没有删除本项目数据目录。

#### 实际执行的主要命令

```bash
sudo -n /srv/services/learning-certification-platform/deploy/server/prepare-host.sh
sudo -n /srv/services/learning-certification-platform/deploy/server/generate-env.sh
./deploy/server/compose.sh config --quiet
./deploy/server/compose.sh pull
./deploy/server/compose.sh up -d mysql
./deploy/server/compose.sh up -d redis
./deploy/server/compose.sh up -d minio
./deploy/server/compose.sh ps
ss -ltnp
df -h /srv/data/learning-certification-platform
free -h
```

上述命令中的脚本负责准备目录、生成密钥和统一 Compose 参数；命令本身不会把真实密码
写入仓库。服务启动后又分别执行了 MySQL 查询、Redis `PING`、MinIO 健康检查和重启探针。

#### 最终服务状态

| 容器 | 镜像 | 状态 | 监听端口 |
| --- | --- | --- | --- |
| `lp-mysql` | `mysql:8.4` | `healthy` | `127.0.0.1:13306 -> 3306` |
| `lp-redis` | `redis:7-alpine` | `healthy` | `127.0.0.1:16379 -> 6379` |
| `lp-minio` | `minio/minio:RELEASE.2025-09-07T16-13-09Z` | `healthy` | `127.0.0.1:19000 -> 9000`、`127.0.0.1:19001 -> 9001` |

项目端口均只绑定服务器本机。`ss -ltnp` 同时显示宿主机 MySQL 仍监听 `0.0.0.0:3306`，
这是旧实例的独立风险和后续治理事项，不属于本次 Docker 项目端口。

#### 镜像摘要

本次拉取到的固定镜像摘要：

```text
mysql:8.4                                  sha256:b3b90af2a6552ae30c266fdb7d5dd55f3afb72404bb78d37fe8a23eb857fd3fb
redis:7-alpine                             sha256:e7723ff73d963f5cc6d9c4643ea3d989527a402a319239054e9472a7fb9219a2
minio/minio:RELEASE.2025-09-07T16-13-09Z   sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e
```

#### 资源和权限结果

- 内存总量约 3.3 GiB，无 Swap；三项基础设施启动后可用内存约 1.4 GiB。
- 数据盘约 69 GiB，部署后剩余约 44 GiB，使用率 35%。
- `/etc/learning-platform` 权限为 `700`，环境文件权限为 `600`。
- MySQL、Redis、MinIO 数据子目录均为 `750`；Redis 和 MinIO 使用服务器 `ubuntu` 用户对应 UID 运行，
  MySQL 数据目录由其容器用户管理。

#### 过程中遇到的排障示例

第一次在受限执行环境中用 `curl http://127.0.0.1:19000/minio/health/live` 失败，后来从服务器本机
执行成功。原因不是 MinIO 服务停止，而是两个命令运行环境的网络命名空间不同。排障时要先问自己：
“我是在本地电脑、服务器宿主机，还是某个容器内执行这条命令？”

本次本地 SSH 隧道还出现了另一类连接问题：把服务器 `hostname -I` 输出中的 `10.1.8.2`
直接当作 Windows 电脑可访问的公网 SSH 地址，结果 `ssh` 报 `Connection timed out`。服务器侧
实际检查结果是：SSH 服务为 `active`，正在监听 `0.0.0.0:22` 和 `[::]:22`，UFW 为 `inactive`；
因此当前证据指向“客户端不在 `10.1.8.2` 所在内网/VPN，地址不可路由”，不是隧道参数错误。

`hostname -I` 展示的是网卡地址，不保证是公网入口。以后本地连接应使用平时能登录服务器的公网
IP、域名或 VPN 地址；如果不确定，先在 PowerShell 执行：

```powershell
Test-NetConnection <服务器SSH地址> -Port 22
ssh -v -o ConnectTimeout=10 ubuntu@<服务器SSH地址>
```

只有 SSH 单独登录成功后，再追加 `-L` 隧道参数。不要为了绕过超时直接把数据库端口开放到公网。

#### 尚未完成的服务器工作

- 尚未创建正式备份并完成一次恢复演练。
- 尚未配置监控、告警和日志轮转。
- 尚未安装 JDK 21，也没有在服务器运行后端应用。
- 尚未创建正式的 MinIO bucket、应用账号和最小权限策略。
- 尚未处理宿主机 MySQL `0.0.0.0:3306` 的防火墙和归属确认。

### 9.2 本次需要记住的“未知的未知”

1. **镜像、容器、数据是三种不同东西。** 删除容器通常不等于删除镜像和绑定目录；真正危险的
   是不看目标就使用全量清理或 `down -v`。
2. **健康检查有边界。** `healthy` 只说明预设探针成功，不能证明 Flyway、业务 SQL、权限、上传
   和 API 流程正确。
3. **初始化脚本不是迁移工具。** `deploy/mysql/init/` 只在 MySQL 数据目录为空时执行；以后改授权
   脚本，不会自动修复已有实例，必须用显式 SQL、迁移或维护脚本处理。
4. **端口和服务名取决于运行位置。** 本地通过隧道使用 `127.0.0.1:13306`；Compose 网络内使用
   `mysql:3306`。把两种配置混用，会得到看似“数据库挂了”的错误。
5. **持久化不是备份。** 绑定目录可以抵抗容器重启，但抵抗不了磁盘损坏、误删和服务器故障；必须
   定期备份，并且必须验证“能恢复”，不能只看备份文件存在。
6. **Redis 不一定只是缓存。** 本项目后续会保存登出黑名单等状态，数据策略要和业务语义一致；
   AOF 提高重启恢复能力，但不能替代备份。
7. **MinIO root 账号不能下沉到业务代码。** 业务接入时要创建 bucket 和最小权限账号；否则一个
   文件上传漏洞可能演变成整个对象存储的管理权限泄露。
8. **资源不足会在“构建或启动后端”时才暴露。** 当前基础设施已能运行，但 Java、Maven、前端构建
   和数据库并发会进一步吃内存；每次增加服务都要重新看 `free -h`、磁盘和容器状态。
9. **旧服务仍然是系统的一部分。** 本项目 Docker MySQL 正常，不代表宿主机 `3306` 的暴露、账号、
   备份和安全策略已经正确；新旧实例必须分别盘点。

## 10. 参考文件

- `compose.yaml`
- `backend/pom.xml`
- `docs/spec/0001-个人学习闭环.md`
- `learning-records/0003-远程Docker环境方案.md`
- `/srv/services/PROJECTS.md`
