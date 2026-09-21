# 阿里云 CI/CD 部署

推送到 `main`（或在 Actions 里手动 **Run workflow**）后，GitHub Actions 会：

1. SSH 到阿里云：`git pull`（没有仓库则 clone）→ 构建并重启 `savemoney-api` 容器 → 检查 `http://127.0.0.1:8080/api/health`
2. 用 JDK 21 + Android SDK 打 debug APK（`setup-android` 只装 `platform-tools`、`build-tools;35.0.0`、`platforms;android-35`，**不装**已下线的 `tools` 包，并用 `yes | sdkmanager --licenses` 非交互接受许可），用仓库 Secrets 里的 **upload keystore** 签名（避免每次 runner 默认 `debug.keystore` 不同导致无法覆盖安装），`versionCode = 1000 + github.run_number`，`versionName = 1.2.{run_number}`，输出改名为 `saveMoney.apk`
3. `scp` 到服务器，再 `docker cp` 进容器，执行 `/app/scripts/publish-update.sh`，核对 `http://127.0.0.1:8080/api/update/latest` 已是新版本

`applicationId` 现为 `com.guanguanhua.app`。这是新的应用身份，**不能覆盖安装**旧的 `com.savemoney.app`；用户需装新包，旧 App 可自行卸载。CI 打出的包仍改名为 **`saveMoney.apk`**，发布路径仍是 `/api/update/download/saveMoney.apk`，不要改文件名，以免应用内更新失效。

工作流文件：`.github/workflows/deploy.yml`。

## GitHub Secrets（精确名称）

在仓库 **Settings → Secrets and variables → Actions** 里配置。不要把私钥写进仓库或 PR。

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| `DEPLOY_HOST` | 是 | 阿里云 ECS 公网 IP 或域名，例如 `8.153.195.112` |
| `DEPLOY_USER` | 是 | SSH 用户名（能跑 `docker` 的那个，常见是 `root` 或已加入 docker 组的普通用户） |
| `DEPLOY_SSH_KEY` | 是 | **私钥全文**，包含 `-----BEGIN … PRIVATE KEY-----` 和结尾行。对应服务器 `~/.ssh/authorized_keys` 里的公钥 |
| `DEPLOY_PATH` | 否 | 服务器上 git 仓库的绝对路径。不设则按下面规则自动检测 |
| `ANDROID_KEYSTORE_BASE64` | 是 | **upload keystore 文件**（`.jks` / `.keystore`）的 **base64 编码**。建议单行、不要换行；workflow 会去掉空白后再解码到 `$RUNNER_TEMP/upload.jks`。不要把 `.jks`、密码或这段 base64 提交进仓库 |
| `ANDROID_KEYSTORE_PASSWORD` | 是 | keystore 密码（`storePassword`） |
| `ANDROID_KEY_ALIAS` | 是 | 密钥别名（`keyAlias`） |
| `ANDROID_KEY_PASSWORD` | 是 | 密钥密码（`keyPassword`，可与 store 密码相同） |

缺必填项时，workflow 会在「检查 Secrets」或打 APK 前直接失败，并在日志里列出缺哪些名字。本地 `./gradlew assembleDebug` **不需要**这些变量，会回退到默认 debug 签名。

### Android 发布包固定签名（覆盖安装）

GitHub Actions 的 runner 每次都是新机器，默认 `~/.android/debug.keystore` 每次都不同。用它签出来的 debug APK 无法覆盖安装上一轮 CI 包，手机会报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（签名冲突）。

CI 的 `assembleDebug` 在上述四个 `ANDROID_*` 环境变量齐全时，改用 **upload** `signingConfig`，这样每一轮 Actions 打出来的包签名相同，可以覆盖安装。

在自己电脑上生成一次 keystore（只做一次，以后覆盖安装必须用同一把钥匙）：

```bash
keytool -genkeypair -v \
  -keystore upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload
```

再编成 **单行** base64（避免换行导致解码失败）：

```bash
# Linux
base64 -w 0 upload.jks
# macOS
base64 -i upload.jks | tr -d '\n'
```

把输出整段贴进 Secret `ANDROID_KEYSTORE_BASE64`（不要夹杂 `-----BEGIN`、注释或换行）。`ANDROID_KEY_ALIAS` 填上面的 `-alias`（示例是 `upload`）。生成后妥善保存 `upload.jks` 和密码；丢失后已经装过 CI 包的手机只能先卸载再装。

已经用「每轮随机 debug 签名」装过的手机，**第一次**换成固定 upload 签名时仍会冲突，需要卸载一次；之后的 CI 包之间可以互相覆盖。

### `DEPLOY_PATH` 自动检测

1. 若设置了 Secret `DEPLOY_PATH`，用它（`~/…` 会展开成 `$HOME/…`）
2. 否则若存在 `~/watchMoney`，用它
3. 否则若存在 `~/saveMoney`，用它（兼容旧目录名）
4. 否则 clone 到 **`~/watchMoney`**

该目录只是 Docker 构建用的代码副本；**账本、照片、已发布 APK 在 `/data/savemoney`，重建容器不会删。**

### 配置 SSH 密钥（不要提交私钥）

在你自己电脑上：

```bash
ssh-keygen -t ed25519 -C "github-actions-watchmoney" -f watchmoney-deploy -N ""
```

- 把 `watchmoney-deploy.pub` 追加到服务器 `~/.ssh/authorized_keys`
- 把 `watchmoney-deploy` **私钥文件全文**贴进 Secret `DEPLOY_SSH_KEY`
- 本地测试：`ssh -i watchmoney-deploy $DEPLOY_USER@$DEPLOY_HOST`

私钥不要提交到 git。用完可从本机删除，以 GitHub Secret 为准。

仓库若是 **private**，服务器还需要能 `git clone` / `git pull`：给这台机器加 [Deploy key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys)（只读），并把远程改成 `git@github.com:anliluZoe/watchMoney.git`。公开仓库用 HTTPS 即可。

## 首次在服务器上准备

CI **不会**安装 Docker / git，也 **不会**创建数据盘。第一次之前请 SSH 上去做完这些（Debian/Ubuntu 示例）：

```bash
sudo apt-get update
sudo apt-get install -y docker.io git curl ca-certificates
sudo usermod -aG docker "$USER"   # 若 DEPLOY_USER 不是 root，执行后重新登录
sudo mkdir -p /data/savemoney
# 让容器进程能写入账本和 updates/
sudo chown -R 0:0 /data/savemoney
```

必须已经具备：

- **Docker**（部署用户能无密码执行 `docker`）
- **git**
- **curl**（健康检查和核对更新接口）
- **`/data/savemoney` 目录**（绑定到容器 `/data`，对应环境变量 `SAVE_MONEY_DATA=/data`）

安全组放行 **8080**（以及你平时 SSH 的 22）。

### 如果以前用过 docker compose

CI 使用的容器名是 **`savemoney-api`**，端口 **8080**，数据在 **`/data/savemoney`**。若旧实例是 compose 起的（容器名可能是 `backend-api-1`，数据在 named volume `savemoney-data`），先迁数据再停旧容器，避免端口冲突，也避免把账本丢了：

```bash
sudo mkdir -p /data/savemoney
# compose 项目目录名不同时，卷名可能是 backend_savemoney-data，用 docker volume ls 确认
docker run --rm -v savemoney-data:/from -v /data/savemoney:/to alpine cp -a /from/. /to/
cd ~/watchMoney/backend 2>/dev/null || cd ~/saveMoney/backend
docker compose down
```

**不要** `rm -rf /data/savemoney`。`docker rm -f savemoney-api` 只删容器，绑着的宿主机目录会留下。

## 容器怎么跑

CI 里等价于：

```bash
cd "$DEPLOY_PATH/backend"    # 或自动检测到的 ~/watchMoney、~/saveMoney
docker build -t savemoney-api .
docker rm -f savemoney-api
docker run -d --name savemoney-api \
  -p 8080:8080 \
  -e SAVE_MONEY_DATA=/data \
  -v /data/savemoney:/data \
  --restart unless-stopped \
  savemoney-api
```

发布 APK 时等价于：

```bash
scp saveMoney.apk "$DEPLOY_USER@$DEPLOY_HOST:/tmp/watchmoney-ci/saveMoney.apk"
ssh ... 'docker cp /tmp/watchmoney-ci/saveMoney.apk savemoney-api:/tmp/saveMoney.apk'
ssh ... 'docker exec savemoney-api /app/scripts/publish-update.sh /tmp/saveMoney.apk <versionCode> <versionName>'
```

脚本把包写到 **`/data/updates/saveMoney.apk`**（宿主机即 `/data/savemoney/updates/`），并更新 `latest.json`。App 请求 `GET /api/update/latest`。

## 版本号

| 字段 | CI 规则 | 说明 |
| --- | --- | --- |
| `versionCode` | `1000 + github.run_number` | 必须单调增加，否则手机拒绝覆盖安装 |
| `versionName` | `1.2.{run_number}` | 例如第 15 次 workflow 是 `1.2.15`（versionCode `1015`） |

`github.run_number` 按这个 workflow 的成功/失败运行递增；**重新跑同一次 Run 不会变大**。要新的 versionCode 请再 push 或再手动触发一次新的 Run。

本地打包仍可用 `gradle.properties` 或 `-PappVersionCode=` / `-PappVersionName=`，与 CI 互不影响。只要装到手机上的 `versionCode` 比当前大即可。

## 手动触发

GitHub 仓库 → **Actions** → **Deploy** → **Run workflow**。和 push 到 `main` 同一套 jobs。

## 发完怎么验

在服务器上：

```bash
curl -sS http://127.0.0.1:8080/api/health
curl -sS http://127.0.0.1:8080/api/update/latest
```

公网（把 IP 换成你的 `DEPLOY_HOST`）：

```bash
curl -sS http://8.153.195.112:8080/api/health
curl -sS http://8.153.195.112:8080/api/update/latest
```

手机「我们」→「检查更新」。允许「管管花」安装未知应用。若手机上还装着旧的 `com.savemoney.app`，这次更新**不会**覆盖它，需要另外安装新包。

## 常见失败

- **缺少 Secrets**：把上面表格里的必填项配全（`DEPLOY_PATH` 可选）。Android 签名四项缺一则不会打出发布 APK。
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**：多半是签名不一致。确认 CI 已用同一把 `ANDROID_KEYSTORE_*` 签名；从旧的随机 debug 包切过来时先卸载再装。
- **`ANDROID_KEYSTORE_BASE64` 解码失败**：Secret 必须是 keystore **文件**的 base64，不是密码、不是 PEM。用 `base64 -w 0`（Linux）或 `base64 -i file | tr -d '\n'`（macOS）生成单行再粘贴。
- **SSH 失败**：公钥是否在 `authorized_keys`，私钥是否完整，安全组是否放行 22。
- **docker 权限**：`DEPLOY_USER` 不在 `docker` 组。
- **没有 `/data/savemoney`**：按首次准备创建。
- **8080 占用**：停掉旧的 compose/进程，不要删数据目录。
- **git pull 失败**（私有仓库）：给服务器配 Deploy key。
- **健康检查失败**：看 Actions 日志里的 `docker logs savemoney-api`。
- **`Failed to find package 'tools'`**：Google 已移除旧 SDK 包 `tools`。`build-apk` 必须显式指定 `platform-tools` / `build-tools;35.0.0` / `platforms;android-35`，不要再装 `tools`。
