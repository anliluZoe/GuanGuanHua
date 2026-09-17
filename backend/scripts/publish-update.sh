#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "用法: $0 <apk路径> <versionCode> <versionName> [更新说明]" >&2
  echo "数据目录: SAVE_MONEY_DATA（未设置则用 backend/data）" >&2
  exit 1
fi

apk=$1
code=$2
name=$3
notes=${4:-}

if [[ ! -f "$apk" ]]; then
  echo "找不到 APK: $apk" >&2
  exit 1
fi

if [[ ! "$code" =~ ^[1-9][0-9]*$ ]]; then
  echo "versionCode 必须是正整数" >&2
  exit 1
fi

filename=$(basename "$apk")
if [[ ! "$filename" =~ \.[Aa][Pp][Kk]$ ]]; then
  echo "文件必须是 .apk" >&2
  exit 1
fi

script_dir=$(cd "$(dirname "$0")" && pwd)
backend_dir=$(cd "$script_dir/.." && pwd)
data_root=${SAVE_MONEY_DATA:-"$backend_dir/data"}
updates_dir="$data_root/updates"
mkdir -p "$updates_dir"

cp -f "$apk" "$updates_dir/$filename"

node -e '
const fs = require("fs");
const payload = {
  versionCode: Number(process.argv[1]),
  versionName: process.argv[2],
  filename: process.argv[3],
};
const notes = process.argv[4];
if (notes) payload.notes = notes;
fs.writeFileSync(process.argv[5], JSON.stringify(payload, null, 2) + "\n");
' "$code" "$name" "$filename" "$notes" "$updates_dir/latest.json"

echo "已发布 $name+$code -> $updates_dir/$filename"
echo "latest.json:"
cat "$updates_dir/latest.json"
