# 远程 Docker 环境方案：本机零 Docker，服务器管基础设施

用户有云服务器，选择不在本机装 Docker Desktop，由服务器运行 MySQL / Redis / MinIO 容器，本机通过 SSH 隧道访问。

## Evidence

- 用户拍板：本机不装 Docker，使用现成服务器（2026-08-05）。
- compose.yaml 端口全部改为绑定 127.0.0.1，服务器场景公网不可见，本地场景不受影响。
- 本机应用配置保持 localhost 不变，靠 `ssh -L 3306:127.0.0.1:3306` 隧道转发，零配置改动。
- Testcontainers（首期-02 起）通过 DOCKER_HOST=ssh://用户@服务器 连远程容器，本机无需 docker CLI。
- 安全约束：服务器安全组只开 SSH，不裸开 2375 远程 API，Redis/MinIO 隧道用到时再开。

## Implications

- 第四课路线图第五步、手搓速查表已加入远程变体。
- 后续票据涉及 Redis、MinIO 时，先提醒用户开对应端口隧道，再讲功能。
- 数据卷在服务器上，docker compose down -v 会清服务器数据，教学演示重置环境时先说明。
- 本机 JDK 21 仍是必需（编译测试在本机），远程方案只替代 Docker 部分。
