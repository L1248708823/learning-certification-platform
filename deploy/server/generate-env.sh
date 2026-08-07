#!/usr/bin/env bash
set -Eeuo pipefail

# 服务器密钥生成脚本。
#
# 目的：为 MySQL、Redis、MinIO 生成不同的随机密码。
# 运行：sudo ./deploy/server/generate-env.sh
#
# 安全规则：
# - 如果配置已经存在，脚本拒绝覆盖，防止误换密码导致旧数据无法登录。
# - 密码只写入 /etc/learning-platform/learning-platform.env。
# - 脚本输出只显示文件位置，不显示密码。

CONFIG_ROOT="${LP_CONFIG_ROOT:-/etc/learning-platform}"
ENV_FILE="$CONFIG_ROOT/learning-platform.env"
OWNER="${SUDO_USER:-ubuntu}"

if [[ ! -d "$CONFIG_ROOT" ]]; then
  echo "配置目录不存在，请先运行 prepare-host.sh：$CONFIG_ROOT" >&2
  exit 1
fi

if [[ -e "$ENV_FILE" ]]; then
  echo "配置已经存在，为避免覆盖，停止处理：$ENV_FILE" >&2
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "缺少 openssl，无法安全生成随机密码" >&2
  exit 1
fi

if ! id "$OWNER" >/dev/null 2>&1; then
  echo "找不到配置文件所有者：$OWNER" >&2
  exit 1
fi
GROUP="$(id -gn "$OWNER")"

random_password() {
  openssl rand -hex 32
}

MYSQL_ROOT_PASSWORD="$(random_password)"
MYSQL_APP_PASSWORD="$(random_password)"
REDIS_PASSWORD="$(random_password)"
MINIO_ROOT_PASSWORD="$(random_password)"

umask 077
TEMP_FILE="$(mktemp "$CONFIG_ROOT/learning-platform.env.XXXXXX")"
trap 'rm -f "$TEMP_FILE"' EXIT

{
  printf '%s\n' 'TZ=Asia/Shanghai'
  printf '%s\n' 'LP_DATA_ROOT=/srv/data/learning-certification-platform'
  printf '%s\n' 'LP_BACKUP_ROOT=/srv/backups/learning-certification-platform'
  printf '%s\n' 'MYSQL_HOST_PORT=13306'
  printf '%s\n' 'REDIS_HOST_PORT=16379'
  printf '%s\n' 'MINIO_API_HOST_PORT=19000'
  printf '%s\n' 'MINIO_CONSOLE_HOST_PORT=19001'
  printf '%s\n' "MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD"
  printf '%s\n' "MYSQL_APP_PASSWORD=$MYSQL_APP_PASSWORD"
  printf '%s\n' 'MINIO_ROOT_USER=lpminioadmin'
  printf '%s\n' "MINIO_ROOT_PASSWORD=$MINIO_ROOT_PASSWORD"
  printf '%s\n' "REDIS_PASSWORD=$REDIS_PASSWORD"
} > "$TEMP_FILE"

chown "$OWNER:$GROUP" "$TEMP_FILE"
chmod 0600 "$TEMP_FILE"
mv "$TEMP_FILE" "$ENV_FILE"
trap - EXIT

echo "密钥配置已生成：$ENV_FILE"
echo "密码未打印，也没有写入 Git。需要连接信息时，通过 SSH 在服务器读取该文件。"
