# Docker 命令速查表

状态：持续维护；只列命令，不管概念。概念和“为什么”看
[04-Docker常用命令与概念.md](04-Docker常用命令与概念.md)，项目服务器部署细节看
[01-服务器Docker教学清单.md](01-服务器Docker教学清单.md)。

示例统一使用通用容器名 `myapp`、镜像 `nginx`，替换成你自己的名字即可。

## 1. 容器生命周期

| 命令 | 作用 |
| --- | --- |
| `docker run -d --name myapp -p 8080:80 nginx` | 后台创建并启动容器，映射端口 |
| `docker run -it --name myapp nginx bash` | 交互式启动，直接进入容器 shell |
| `docker ps` | 查看运行中的容器 |
| `docker ps -a` | 查看全部容器，含已停止 |
| `docker logs myapp` | 查看容器日志 |
| `docker logs -f myapp` | 持续跟踪日志 |
| `docker exec -it myapp bash` | 进入运行中容器的 shell |
| `docker exec myapp ls /etc` | 在容器里执行单条命令，不进入 |
| `docker stop myapp` | 优雅停止容器 |
| `docker start myapp` | 启动已停止的容器 |
| `docker restart myapp` | 重启容器 |
| `docker rm myapp` | 删除已停止的容器 |
| `docker rm -f myapp` | 强制删除容器，含运行中 |

## 2. 镜像管理

| 命令 | 作用 |
| --- | --- |
| `docker pull nginx` | 拉取镜像，可带版本如 `nginx:1.27` |
| `docker images` | 查看本机全部镜像 |
| `docker rmi nginx` | 删除镜像，前提是没有容器还在用 |
| `docker inspect nginx` | 查看镜像或容器的详细配置 |

## 3. Docker Compose 生命周期

以下命令默认在 compose.yaml 所在目录执行。项目服务器环境请用 `./deploy/server/compose.sh`
统一入口，它会自动带上 env 文件和项目名。

| 命令 | 作用 |
| --- | --- |
| `docker compose config` | 校验并打印解析后的完整配置，启动前先跑 |
| `docker compose pull` | 拉取所有服务镜像 |
| `docker compose up -d` | 创建并后台启动全部服务 |
| `docker compose up -d mysql` | 只启动某个服务 |
| `docker compose ps` | 查看服务状态 |
| `docker compose logs` | 查看全部服务日志 |
| `docker compose logs mysql` | 只看某个服务日志 |
| `docker compose exec mysql bash` | 进入某个服务的容器 |
| `docker compose stop` | 停止服务，保留容器 |
| `docker compose down` | 删除容器和网络，保留卷 |
| `docker compose down -v` | 连卷一起删除，数据彻底丢失，危险 |
| `docker compose restart` | 重启全部服务 |

## 4. 卷与网络

| 命令 | 作用 |
| --- | --- |
| `docker volume ls` | 查看全部命名卷 |
| `docker volume inspect 卷名` | 查看卷详情 |
| `docker network ls` | 查看网络列表 |
| `docker network inspect 网络名` | 查看网络内连接的容器 |

挂载语法：

| 语法 | 含义 |
| --- | --- |
| `-v mysql-data:/var/lib/mysql` | 命名卷挂载，Docker 管理存储位置 |
| `-v ./deploy/mysql/init:/docker-entrypoint-initdb.d:ro` | 绑定挂载，宿主机路径，只读 |
| `-p 127.0.0.1:13306:3306` | 端口映射，宿主机 13306 到容器 3306 |

## 5. 危险命令速查

执行任何一条前，先 `docker ps -a` 和 `docker volume ls` 确认目标，再确认数据已有备份。

| 命令 | 危险等级 | 后果 |
| --- | --- | --- |
| `docker rm -f myapp` | 中 | 强制删容器，未持久化数据丢失 |
| `docker rmi nginx` | 中 | 删镜像，若有容器还在用会失败 |
| `docker compose down -v` | 高 | 删项目全部卷，数据无法找回 |
| `docker volume rm 卷名` | 高 | 删指定卷全部数据 |
| `docker system prune` | 中 | 清理未使用资源 |
| `docker system prune -a` | 高 | 删除全部未被使用的镜像 |
