# 阿里云 CI/CD 部署

推送到 `main`（或在 Actions 里手动 **Run workflow**）后，GitHub Actions 会：

1. SSH 到阿里云：`git pull`（没有仓库则 clone）→ 构建并重启 `savemoney-api` 容器 → 检查 `http://127.0.0.1:8080/api/health`
2. 用 JDK 21 + Android SDK 打 debug APK（`setup-android` 只装 `platform-tools`、`build-tools;35.0.0`、`platforms;android-35`，**不装**已下线的 `tools` 包，并用 `yes | sdkmanager --licenses` 非交互接受许可），`versionCode = 1000 + github.run_number`，`versionName = 1.2.{run_number}`，输出改名为 `saveMoney.apk`
3. `scp` 到服务器，再 `docker cp` 进容器，执行 `/app/scripts/publish-update.sh`，核对 `http://127.0.0.1:8080/api/update/latest` 已是新版本

`applicationId` 保持 `com.savemoney.app`，不要改，否则无法覆盖安装。

工作流文件：`.github/workflows/deploy.yml`。

## GitHub Secrets（精确名称）

在仓库 **Settings → Secrets and variables → Actions** 里配置。不要把私钥写进仓库或 PR。

| 名称 | 必填 | 说明 |
| --- | --- | --- |
| `DEPLOY_HOST` | 是 | 阿里云 ECS 公网 IP 或域名，例如 `8.153.195.112` |
| `DEPLOY_USER` | 是 | SSH 用户名（能跑 `docker` 的那个，常见是 `root` 或已加入 docker 组的普通用户） |
| `DEPLOY_SSH_KEY` | 是 | **私钥全文**，包含 `-----BEGIN … PRIVATE KEY-----` 和结尾行。对应服务器 `~/.ssh/authorized_keys` 里的公钥 |
| `DEPLOY_PATH` | 否 | 服务器上 git 仓库的绝对路径。不设则按下面规则自动检测 |

缺必填项时，workflow 会在「检查 Secrets」这一步直接失败，并在日志里列出缺哪些名字。

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

手机「我们」→「检查更新」。允许「管管花」安装未知应用。

## 常见失败

- **缺少 Secrets**：把上面四格名字配全（最后一项可选）。
- **SSH 失败**：公钥是否在 `authorized_keys`，私钥是否完整，安全组是否放行 22。
- **docker 权限**：`DEPLOY_USER` 不在 `docker` 组。
- **没有 `/data/savemoney`**：按首次准备创建。
- **8080 占用**：停掉旧的 compose/进程，不要删数据目录。
- **git pull 失败**（私有仓库）：给服务器配 Deploy key。
- **健康检查失败**：看 Actions 日志里的 `docker logs savemoney-api`。
- **`Failed to find package 'tools'`**：Google 已移除旧 SDK 包 `tools`。`build-apk` 必须显式指定 `platform-tools` / `build-tools;35.0.0` / `platforms;android-35`，不要再装 `tools`。
