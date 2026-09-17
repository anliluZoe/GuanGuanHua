#!/usr/bin/env bash
# 把 CI 上传的 APK docker cp 进容器，调用 /app/scripts/publish-update.sh，并核对 /api/update/latest。
set -euo pipefail

: "${VERSION_CODE:?缺少 VERSION_CODE}"
: "${VERSION_NAME:?缺少 VERSION_NAME}"

APK_HOST=${APK_HOST_PATH:-/tmp/watchmoney-ci/saveMoney.apk}
NOTES=${UPDATE_NOTES:-}

if [[ ! -f "$APK_HOST" ]]; then
  echo "找不到刚 scp 上来的 APK: $APK_HOST" >&2
  exit 1
fi
if ! command -v curl >/dev/null 2>&1; then
  echo "缺少 curl，无法核对 /api/update/latest" >&2
  exit 1
fi
if ! docker inspect savemoney-api >/dev/null 2>&1; then
  echo "容器 savemoney-api 不存在。请先让 deploy-backend 成功。" >&2
  exit 1
fi

docker cp "$APK_HOST" savemoney-api:/tmp/saveMoney.apk
if [[ -n "$NOTES" ]]; then
  docker exec savemoney-api /app/scripts/publish-update.sh /tmp/saveMoney.apk "$VERSION_CODE" "$VERSION_NAME" "$NOTES"
else
  docker exec savemoney-api /app/scripts/publish-update.sh /tmp/saveMoney.apk "$VERSION_CODE" "$VERSION_NAME"
fi

body=$(curl -fsS --max-time 10 http://127.0.0.1:8080/api/update/latest)
echo "update/latest: $body"
if ! echo "$body" | grep -F -q "\"versionCode\":${VERSION_CODE}"; then
  echo "latest 的 versionCode 不是 ${VERSION_CODE}" >&2
  exit 1
fi
if ! echo "$body" | grep -F -q "\"versionName\":\"${VERSION_NAME}\""; then
  echo "latest 的 versionName 不是 ${VERSION_NAME}" >&2
  exit 1
fi

echo "已发布 $VERSION_NAME+$VERSION_CODE"
