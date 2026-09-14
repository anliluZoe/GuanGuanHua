# 省钱助手（saveMoney）

两人协作的购买申请审核与月度消费记录。**数据存在自己的后端**，两部手机用同一个家庭码同步申请、审核、照片和账本。

- **申请人**提交购买申请（可附照片）
- **审核人**通过或拒绝；通过后自动记入当月消费
- 按月看总额、预算、分类占比和明细
- 对方提交申请或给出审核结果时收到通知：App 打开时每 30 秒刷新，后台约每 15 分钟检查一次（不依赖 Google 推送服务）

## 两部手机怎么一起用

1. 在电脑或云主机上启动后端（见下方）。
2. 第一部手机打开 App，填服务器地址（例如 `http://192.168.1.8:8080`），选「申请人」，点 **创建家庭账本**，记下 6 位家庭码。
3. 第二部手机填**同一个地址**，选「审核人」，输入家庭码，点 **加入**。
4. 之后两边的申请、照片和消费都会同步；在「我们」页下拉进入即可看到家庭码。

同一 Wi‑Fi 下用电脑的局域网 IP；不在一个网时，需要把后端放到有公网 IP 的服务器（或内网穿透）。

## 启动后端

需要 Node.js 22+。

```bash
cd backend
npm install
npm start
```

或：

```bash
cd backend && docker compose up --build
```

模拟器访问电脑上的后端请用 `http://10.0.2.2:8080`。

## 构建 App

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

需要 JDK 17+ 与 Android SDK（compileSdk 35）。未用 Android Studio 时，在根目录 `local.properties` 写入 `sdk.dir=/path/to/android-sdk`。

## 界面截图

| 申请列表 | 新建申请 | 加点照片 |
| --- | --- | --- |
| ![申请列表](docs/screenshots/request_list.png) | ![新建申请](docs/screenshots/new_request_form.png) | ![加点照片](docs/screenshots/photo_attach.png) |

| 审核人审核 | 小账本 | 我们俩 |
| --- | --- | --- |
| ![审核](docs/screenshots/request_detail_approver.png) | ![月度消费](docs/screenshots/expenses_summary.png) | ![切换身份](docs/screenshots/profile_switch_role.png) |

| 审核人收到新申请通知 | 申请人收到审核结果 |
| --- | --- |
| ![新申请通知](docs/screenshots/notification_new_request.png) | ![审核结果通知](docs/screenshots/notification_review_result.png) |

## 技术栈

- App：Kotlin 2.0 · Jetpack Compose · Retrofit
- 后端：Node.js（Express）· SQLite · 本地文件存照片
- minSdk 26，compileSdk / targetSdk 35
