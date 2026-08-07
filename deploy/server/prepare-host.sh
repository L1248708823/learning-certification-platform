#!/usr/bin/env bash
set -Eeuo pipefail

# 服务器目录准备脚本。
#
# 目的：把代码、运行数据、密钥和备份分开。
# 运行：sudo ./deploy/server/prepare-host.sh
#
# 这个脚本只创建目录和设置权限，不删除已有文件，也不启动容器。

DATA_ROOT="${LP_DATA_ROOT:-/srv/data/learning-certification-platform}"
BACKUP_ROOT="${LP_BACKUP_ROOT:-/srv/backups/learning-certification-platform}"
CONFIG_ROOT="${LP_CONFIG_ROOT:-/etc/learning-platform}"

# 使用 sudo 时，SUDO_USER 是实际操作用户；没有 sudo 时默认使用 ubuntu。
OWNER="${SUDO_USER:-ubuntu}"
if ! id "$OWNER" >/dev/null 2>&1; then
  echo "找不到目录所有者：$OWNER" >&2
  exit 1
fi
GROUP="$(id -gn "$OWNER")"

for path in \
  "$DATA_ROOT/mysql" \
  "$DATA_ROOT/redis" \
  "$DATA_ROOT/minio" \
  "$BACKUP_ROOT/mysql" \
  "$BACKUP_ROOT/minio" \
  "$CONFIG_ROOT"; do
  if [[ -e "$path" && ! -d "$path" ]]; then
    echo "目标路径不是目录，停止处理：$path" >&2
    exit 1
  fi
  install -d -m 0750 -o "$OWNER" -g "$GROUP" "$path"
done

# 配置目录只能由 owner 访问；真实 env 文件会再收紧到 0600。
chmod 0700 "$CONFIG_ROOT"

echo "目录准备完成："
printf '  data:    %s\n' "$DATA_ROOT"
printf '  backups: %s\n' "$BACKUP_ROOT"
printf '  config:  %s\n' "$CONFIG_ROOT"
