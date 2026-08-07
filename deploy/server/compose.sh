#!/usr/bin/env bash
set -Eeuo pipefail

# Compose 统一入口。
#
# 目的：避免每次手写 --env-file、-f 和 -p 时漏参数。
# 示例：
#   ./deploy/server/compose.sh config
#   ./deploy/server/compose.sh pull
#   ./deploy/server/compose.sh up -d
#   ./deploy/server/compose.sh ps
#
# 真实 env 文件在仓库外，Compose 文件和代码仍然在仓库中。

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${LP_ENV_FILE:-/etc/learning-platform/learning-platform.env}"

if [[ ! -r "$ENV_FILE" ]]; then
  echo "找不到或无法读取服务器配置：$ENV_FILE" >&2
  echo "请先执行：sudo ./deploy/server/prepare-host.sh" >&2
  echo "再执行：sudo ./deploy/server/generate-env.sh" >&2
  exit 1
fi

exec docker compose \
  --env-file "$ENV_FILE" \
  --project-directory "$REPO_ROOT" \
  -f "$REPO_ROOT/compose.server.yaml" \
  -p learning-platform-server \
  "$@"
