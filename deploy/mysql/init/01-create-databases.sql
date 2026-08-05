-- 首期三个业务库，由 Docker Compose 首次初始化时挂载执行
-- （compose.yaml 将本目录挂载到 MySQL 容器的 /docker-entrypoint-initdb.d）
-- 应用只连接 iam 库，content / learning 的表通过全限定表名访问，见 docs/spec/0001 第 2 章。

CREATE DATABASE IF NOT EXISTS iam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS content DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
