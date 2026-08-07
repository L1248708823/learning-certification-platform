-- 服务器开发账号权限补充。
--
-- 官方 MySQL 镜像会根据 MYSQL_USER/MYSQL_PASSWORD 创建 lp_dev，
-- 但 MYSQL_DATABASE 只会自动授予一个数据库的权限。
-- 项目采用 iam/content/learning 三个数据库，因此这里补齐另外两个库。
-- 该脚本只在 MySQL 数据目录第一次初始化时执行。

GRANT ALL PRIVILEGES ON `iam`.* TO 'lp_dev'@'%';
GRANT ALL PRIVILEGES ON `content`.* TO 'lp_dev'@'%';
GRANT ALL PRIVILEGES ON `learning`.* TO 'lp_dev'@'%';
FLUSH PRIVILEGES;
