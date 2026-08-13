# 远程 Docker 环境方案：本机零 Docker，服务器管基础设施

用户有云服务器，选择不在本机装 Docker Desktop，由服务器运行 MySQL / Redis / MinIO 容器，本机通过 SSH 隧道访问。

## Evidence

- 用户拍板：本机不装 Docker，使用现成服务器（2026-08-05）。
- compose.yaml 端口全部改为绑定 127.0.0.1，服务器场景公网不可见，本地场景不受影响。
- 本机应用配置保持 localhost 不变，靠 `ssh -L 3306:127.0.0.1:3306` 隧道转发，零配置改动。
- Docker CLI 可以使用 SSH 访问远程 Docker。当前后端 Testcontainers Java 2.0.5 不接受
  `DOCKER_HOST=ssh://用户@服务器`，因此 `mvn verify` 不能直接复用这条配置。
  真实依赖集成测试需要在 Docker 所在服务器执行，或使用受控的本地 TCP/Unix Socket
  转发，并同时保证容器动态映射端口可以从测试进程访问。
- 安全约束：服务器安全组只开 SSH，不裸开 2375 远程 API，Redis/MinIO 隧道用到时再开。

## Implications

- 第四课路线图第五步、手搓速查表已加入远程变体。
- 后续票据涉及 Redis、MinIO 时，先提醒用户开对应端口隧道，再讲功能。涉及
  Testcontainers 时，先确认测试进程所在环境能访问 Docker Engine 和容器映射端口。
- 数据卷在服务器上，docker compose down -v 会清服务器数据，教学演示重置环境时先说明。
- 本机 JDK 21 仍是必需（编译测试在本机），远程方案只替代 Docker 部分。
