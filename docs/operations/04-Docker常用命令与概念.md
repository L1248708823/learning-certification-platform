# Docker 常用命令与概念

状态：持续维护；面向本地开发与服务器运维的入门学习，docker 与 docker compose 一起讲。
速查用途请看 [05-Docker命令速查表.md](05-Docker命令速查表.md)，项目服务器部署细节看
[01-服务器Docker教学清单.md](01-服务器Docker教学清单.md)。

这份文档讲两件事：先建立 Docker 的核心概念，再把这些概念落到命令上。每一类命令会说明
“为什么需要它”和“执行后能看到什么”，避免只背参数不知道自己在干嘛。

## 1. 核心概念：镜像、容器、卷、网络

这四个词是理解一切 Docker 命令的钥匙，先在这里打通，后面命令都是它们的排列组合。

### 1.1 镜像（Image）：一张只读菜谱

镜像是一个只读的模板，打包了运行某个程序所需的全部东西：操作系统基础、运行环境、依赖、
代码和启动命令。它自己不会动，就像一张菜谱，只描述怎么做，不产生任何过程。

镜像的名称带标签，比如 `mysql:8.4`，冒号后面是版本标签。没有写标签的默认是 `latest`，
但 `latest` 会随时间变化，同一份配置在不同时间可能拉到不同内容，所以项目里坚持写具体版本。

你可以用 `docker images` 看到本机有哪些镜像。镜像不会因为你运行容器而改变，这也是它
安全的地方，删容器镜像还在，随时能再创建容器。

### 1.2 容器（Container）：按菜谱做出来的那道菜

容器是镜像的运行实例。每次 `docker run` 就用一张镜像做出一道菜，也就是起一个容器。
容器有自己的进程、文件系统（一个可写层叠加在只读镜像之上）和网络，相互隔离。

一个镜像可以同时跑多个容器，就像同一张菜谱可以做很多道菜。容器是暂时的，删了容器，
做出来的菜就没了，但菜谱（镜像）还在。

容器内部看到的是隔离的环境，`localhost` 指容器自己，不指向宿主机的其他服务。这也是
教学清单里反复强调"容器内的 localhost 不指向 MySQL"的原因。

### 1.3 卷（Volume）：把菜放到冰箱里

容器一删，里面的数据跟着没了。如果数据需要保存下来，就要把数据放到容器外的宿主机目录，
这个持久化存放的位置就是卷。

卷有两种形式。绑定挂载（bind mount）直接把宿主机某个目录挂进容器，路径完全由你指定，
比如 `./deploy/mysql/init:/docker-entrypoint-initdb.d:ro`。命名卷（named volume）由 Docker
管理，你只给个名字，不关心它存在宿主机哪里，比如根目录 compose.yaml 里的 `mysql-data`。

判断方法很简单：路径以 `/` 或 `./` 开头的是绑定挂载，裸名字的是命名卷。是否加 `:ro`
决定容器只能读还是能读写，初始化脚本目录加只读防止容器改坏脚本，数据目录不加只读才能写数据。

### 1.4 网络（Network）：让容器之间、容器和外界通信

容器默认处于隔离网络，要访问容器里的服务，需要把宿主机的端口映射到容器端口，这就是
`-p 宿主机端口:容器端口`，比如 `-p 127.0.0.1:3306:3306`。

多个容器在同一个 compose 项目里会自动进入同一个网络，可以用服务名互相访问，比如
`mysql:3306`，不需要知道对方容器的 IP。这个知识点在项目里体现为：容器内用服务名连接，
宿主机或本地电脑用映射出来的端口连接，两者不能混用。

## 2. 镜像命令

镜像命令解决"镜像从哪来、怎么看、怎么删"。项目所有镜像都有具体版本，不追求 latest。

### 2.1 拉取镜像：docker pull

```bash
docker pull mysql:8.4
```

把镜像从仓库拉到本机。执行后可以看到分层下载的进度，每层都标注 SHA256 摘要，这也是
教学清单里记录镜像摘要的来源。先 pull 再 run 是规范做法，能区分网络问题和容器问题。

### 2.2 查看本机镜像：docker images

```bash
docker images
```

列出本机所有镜像，显示仓库名、标签、镜像 ID、创建时间和大小。这里能看到之前部署拉过的
mysql、redis、minio，用于确认"这台机器上到底有什么镜像"。

### 2.3 删除镜像：docker rmi

```bash
docker rmi mysql:8.4
```

删除本机镜像。前提是该镜像没有被任何容器使用，哪怕那个容器是停止状态也不行，要先删容器
再删镜像。项目教学清单里明确了"删除容器不等于删除镜像，也不等于删除数据卷"，三者必须分开确认。

## 3. 容器命令

容器命令是日常最高频的一类。掌握一条主线：创建、查看、看日志、进去执行、停止、删除。

### 3.1 创建并启动容器：docker run

```bash
docker run -d --name myapp -p 8080:80 nginx
```

- `-d` 后台运行，不加的话命令会卡在前台
- `--name` 给容器起个名字，方便后续用名字操作
- `-p 8080:80` 把宿主机 8080 映射到容器 80
- `nginx` 是镜像名，缺标签默认 latest

执行后输出一串容器 ID，表示容器已在后台启动。第一次运行会自动先拉取镜像。

如果镜像在本地不存在，run 会先执行 pull。这是常见现象，不要误以为卡住了。

### 3.2 查看容器：docker ps

```bash
docker ps
```

只列出正在运行的容器。要看到停止的容器，加 `-a`：

```bash
docker ps -a
```

输出里 CONTAINER ID、IMAGE、STATUS 最有用。STATUS 列会显示 `Up`（运行中）、`Exited`（退出）
或 `healthy`（健康检查通过），教学清单里三个服务状态为 healthy 就是从这里看到的。

### 3.3 查看日志：docker logs

```bash
docker logs myapp
docker logs -f myapp
```

`-f` 是 follow，持续跟踪输出，类似 tail -f，适合排障时盯着看。容器没有输出不代表没运行，
很多服务默认静默。日志是排查容器为什么起不来、为什么反复重启的第一手资料。

### 3.4 进入容器执行命令：docker exec

```bash
docker exec -it myapp bash
```

`-it` 表示交互式终端，进入容器内部拿到一个 shell。进去之后可以查文件、跑命令、看配置。
排障常用它进入容器确认环境。不需要持久进入，执行完几条命令就 `exit` 退出，容器不受影响。

另一种用法是不进入，直接执行单条命令：

```bash
docker exec myapp ls /etc
```

### 3.5 停止与启动：docker stop / start / restart

```bash
docker stop myapp
docker start myapp
docker restart myapp
```

stop 是优雅停止，start 启动已存在但停止的容器，restart 先停后启。容器重启后，写在卷里的
数据还在，没写进卷的数据（容器可写层的改动）会丢。这是理解"持久化靠卷"的关键验证点。

### 3.6 删除容器：docker rm

```bash
docker rm myapp
```

删除一个已停止的容器。运行中的容器要先 stop 或加 `-f` 强制删。删容器不动镜像不动数据卷，
但如果数据没放在卷里，删了容器数据就彻底没了。所以删之前先确认这个容器的数据是否已经
持久化，项目教学清单里专门做过重启探针来证明"卷里的数据容器重启不丢"。

## 4. Docker Compose：多容器的编排工具

单个容器用 docker run 就够了，但项目需要 MySQL、Redis、MinIO 三个服务一起跑，手动维护
三条 run 命令容易漏参数。Compose 用一份 YAML 描述整个服务组，一条命令启停全部。

### 4.1 compose.yaml 长什么样

Compose 文件描述 services（服务）、volumes（卷）、networks（网络）。一个服务对应一类容器。
项目根目录 compose.yaml 和服务器用的 compose.server.yaml 就是例子，结构是：

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: lp-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: "${MYSQL_ROOT_PASSWORD:?必填}"
    ports:
      - "127.0.0.1:13306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
volumes:
  mysql-data:
```

### 4.2 Compose 生命周期命令

Compose 的所有命令格式统一是 `docker compose <子命令>`。常用几条：

```bash
docker compose up -d        # 创建并启动所有服务，-d 后台运行
docker compose ps           # 查看服务状态，类似 docker ps
docker compose logs         # 查看全部服务日志
docker compose logs mysql   # 只看某个服务的日志
docker compose exec mysql bash   # 进入某个服务的容器
docker compose config       # 校验配置并打印解析结果，启动前先跑这个
```

`up` 是最常用的，第一次会先拉镜像再创建容器。`config` 是启动前安全检查，能提前暴露
YAML 语法错误、环境变量缺失、端口冲突，项目脚本 compose.sh 统一封装了这些参数。

### 4.3 down 和 down -v 的区别

```bash
docker compose down       # 停止并删除容器和网络，保留卷
docker compose down -v    # 额外删除卷，数据彻底丢失
```

`down` 是安全的日常操作，容器删了数据还在。`down -v` 会连卷一起删，数据无法找回，这是
项目里反复强调的禁区。想确认项目数据没被动过，检查 `/srv/data/learning-certification-platform`
目录里的文件是否还在即可。

### 4.4 restart 策略和 healthcheck

Compose 里两个项目用到的关键配置。

`restart: unless-stopped` 表示容器异常退出时自动重启，但被手动 stop 的不重启。云服务器重启
后容器自动拉起靠的就是这个，教学清单里"三个服务加 restart 策略"的提交就是这个配置。

`healthcheck` 定义健康检查探针，告诉 Docker 怎么判断这个服务真的可用。compose.server.yaml
里 MySQL 用 mysqladmin ping、Redis 用 redis-cli ping、MinIO 用 curl 打健康端点。容器健康
不等于业务正确，healthy 只说明预设探针通过，这是教学清单的既定边界。

## 5. 危险命令集中营

这一节收齐会删数据或破坏环境的命令，每条都标危险等级。使用时先确认目标，不要凭印象执行。

| 命令 | 危险等级 | 删什么 | 动手前必须确认 |
| --- | --- | --- | --- |
| `docker rm -f 容器` | 中 | 运行中的容器，未持久化数据 | 数据是否已在卷里 |
| `docker rmi 镜像` | 中 | 本机镜像 | 没有容器还在用它 |
| `docker compose down -v` | 高 | 项目全部卷，数据彻底丢失 | 数据是否已备份 |
| `docker volume rm 卷` | 高 | 指定卷的全部数据 | 卷里是否有需要保留的数据 |
| `docker system prune` | 中 | 未使用镜像、容器、网络、构建缓存 | 确认没有误删正在用的资源 |
| `docker system prune -a` | 高 | 全部未被容器使用的镜像 | 是否需要保留旧版本镜像 |

安全姿势只有一条主线：先 `docker ps -a` 看目标叫什么，再确认数据在哪，最后才执行删除。
项目教学清单在清理旧容器时就是这么做的，先列清单再逐个确认，不用全量清理命令。

## 6. 更完善的部分：镜像构建（进阶预告）

以上是日常运维的范围。等你后端要容器化、需要把 Spring Boot 应用打成镜像时，再来补充
这部分：`docker build` 用 Dockerfile 构建镜像，`docker tag` 打版本标签，`docker push`
推送镜像到仓库。这些命令当前阶段用不到，先知道有这几个东西存在，避免以后见到不认识。

## 7. 验证你已经懂了

能回答下面三个问题，说明核心概念通了：

- 删掉容器，什么会丢，什么不会丢？
- 容器内的 `localhost:3306` 和宿主机的 `localhost:13306` 有什么区别？
- `docker compose down -v` 为什么被项目列为禁区？

这三个问题的答案对应第 1、3、4 节的核心内容，想不起来就回去翻对应章节。
