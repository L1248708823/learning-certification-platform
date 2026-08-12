# learning-platform-backend

项目后端，Spring Boot 3.5，Java 21。本文件是本地调试启动指引，只讲怎么把后端跑起来
连上数据库。服务器基础设施的部署细节看 [../docs/operations/01-服务器Docker教学清单.md](../docs/operations/01-服务器Docker教学清单.md)，SSH 隧道和客户端配置看
[../docs/operations/02-连接服务器.md](../docs/operations/02-连接服务器.md)。

## 1. 环境约定

本机不装 Docker，MySQL 等依赖跑在云服务器上，本机通过 SSH 隧道访问，见
[../learning-records/0003-远程Docker环境方案.md](../learning-records/0003-远程Docker环境方案.md)。
因此启动后端前必须先开隧道，且要提供服务器数据库密码。

## 2. 启动前准备

### 2.1 开 SSH 隧道

在一个单独的 PowerShell 窗口执行，窗口保持打开：

```powershell
ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=60 -L 13306:127.0.0.1:13306 -L 16379:127.0.0.1:16379 -L 19000:127.0.0.1:19000 -L 19001:127.0.0.1:19001 ubuntu@106.55.229.119

```

先验证隧道通了再启动，用 Navicat 或以下命令确认 `127.0.0.1:13306` 能连：

```powershell
Test-NetConnection 127.0.0.1 -Port 13306
```

看到 `TcpTestSucceeded : True` 才继续。

### 2.2 设置数据库密码

密码在服务器上，SSH 登录服务器读取：

```bash
cat /etc/learning-platform/learning-platform.env
```

找到 `MYSQL_APP_PASSWORD=` 的值，然后在启动后端的那个窗口里设置环境变量：

```powershell
$env:DB_PASSWORD = "服务器读到的密码"
```

地址和用户名不需要设置，`application.yml` 里有默认值 `127.0.0.1:13306` 和 `lp_dev`。

## 3. 启动

在设置好 `DB_PASSWORD` 的同一个窗口执行：

```bash
./mvnw spring-boot:run
```

Windows 也可以直接用 `mvnw.cmd spring-boot:run`。

## 4. 验证启动成功

看到以下日志说明数据库连接通了：

- Flyway 创建 `flyway_schema_history` 表并完成基线迁移
- `Started LearningPlatformApplication` 启动完成
- 浏览器访问 `http://localhost:8080/actuator/health` 返回 `UP`

## 5. 常见故障

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `Connection refused` | 隧道没开或端口不对 | 确认隧道窗口还在，`Test-NetConnection` 验证 13306 |
| `Access denied for user 'lp_dev'` | 密码不对 | 重新读服务器 env 文件，确认 `MYSQL_APP_PASSWORD` |
| `Unknown database 'iam'` | 服务器库未初始化 | 检查服务器三个库是否存在，见教学清单阶段 G |
| `DB_PASSWORD` 相关报错 | 环境变量未设置 | 确认在启动的同一个窗口执行了 `$env:DB_PASSWORD = ...` |

## 6. 不要在仓库里放真实密码

`application.yml` 使用 `${DB_PASSWORD:...}` 占位符，真实密码通过环境变量注入，不落仓库。
不要为了省事把服务器读到的密码直接写进 `application.yml` 或任何提交的文件。
