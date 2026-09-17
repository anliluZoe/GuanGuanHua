#!/usr/bin/env bash
# 在阿里云上执行：拉取仓库、重建后端容器、健康检查。
# 由 GitHub Actions appleboy/ssh-action 通过 script_path 下发。
set -euo pipefail

REPO_URL=${REPO_URL:-https://github.com/anliluZoe/watchMoney.git}

if ! command -v git >/dev/null 2>&1; then
  echo "缺少 git。首次部署请先安装：sudo apt-get install -y git" >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "缺少 Docker。首次部署请先安装 Docker，并把部署用户加入 docker 组。" >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "当前用户无法执行 docker。请执行：sudo usermod -aG docker $USER 然后重新登录。" >&2
  exit 1
fi
if ! command -v curl >/dev/null 2>&1; then
  echo "缺少 curl。健康检查需要它：sudo apt-get install -y curl" >&2
  exit 1
fi
if [[ ! -d /data/savemoney ]]; then
  echo "缺少数据目录 /data/savemoney。" >&2
  echo "首次部署请先创建（以后不要删，账本和 updates/ 都在这里）：" >&2
  echo "  sudo mkdir -p /data/savemoney" >&2
  exit 1
fi

if [[ -n "${DEPLOY_PATH:-}" ]]; then
  APP_DIR=$DEPLOY_PATH
elif [[ -d "$HOME/watchMoney" ]]; then
  APP_DIR=$HOME/watchMoney
elif [[ -d "$HOME/saveMoney" ]]; then
  APP_DIR=$HOME/saveMoney
else
  APP_DIR=$HOME/watchMoney
fi

case "$APP_DIR" in
  "~") APP_DIR=$HOME ;;
  "~/"*) APP_DIR=$HOME/${APP_DIR#~/} ;;
esac

echo "仓库目录: $APP_DIR"
echo "远程: $REPO_URL"
echo "数据卷: /data/savemoney （容器内 /data，不会在重建时删除）"

export GIT_TERMINAL_PROMPT=0
git config --global --add safe.directory "$APP_DIR" || true

if [[ ! -d "$APP_DIR/.git" ]]; then
  if [[ -d "$APP_DIR" ]] && [[ -n "$(ls -A "$APP_DIR" 2>/dev/null || true)" ]]; then
    echo "目录已存在但不是 git 仓库: $APP_DIR" >&2
    echo "请设 Secret DEPLOY_PATH 指向空目录或已有的 watchMoney/saveMoney 仓库。" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$APP_DIR")"
  git clone --branch main "$REPO_URL" "$APP_DIR"
else
  cd "$APP_DIR"
  git fetch origin
  git checkout main
  git pull --ff-only origin main
fi

cd "$APP_DIR/backend"
docker build -t savemoney-api .
docker rm -f savemoney-api 2>/dev/null || true
if ! docker run -d \
  --name savemoney-api \
  -p 8080:8080 \
  -e SAVE_MONEY_DATA=/data \
  -v /data/savemoney:/data \
  --restart unless-stopped \
  savemoney-api; then
  echo "docker run 失败。若 8080 已被旧的 compose 占用，请先 docker compose down（不要删 /data/savemoney）。" >&2
  docker ps -a >&2 || true
  exit 1
fi

ok=0
i=1
while [[ $i -le 30 ]]; do
  if body=$(curl -fsS --max-time 5 http://127.0.0.1:8080/api/health 2>/dev/null); then
    echo "health: $body"
    if echo "$body" | grep -q '"ok"[[:space:]]*:[[:space:]]*true'; then
      ok=1
      break
    fi
  fi
  sleep 2
  i=$((i + 1))
done

if [[ $ok -ne 1 ]]; then
  echo "后端健康检查失败：http://127.0.0.1:8080/api/health" >&2
  docker logs savemoney-api >&2 || true
  exit 1
fi

echo "后端部署完成"
