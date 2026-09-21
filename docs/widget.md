# 家庭共享桌面组件

v1 每个家庭只有 **一张照片** 和 **一句说明**。谁上传/谁改，两部手机的桌面组件都会变成同一份。没有轮播，也没有「每人一张」。

说明叠在照片 **正中央**（水平+垂直居中），颜色可配（`widgetCaptionColor`）。组件上 **没有「管管花」标题/底栏**，也没有底部半透明遮罩或文字阴影。

`applicationId` 是 `com.guanguanhua.app`。

## 怎么加到桌面

1. 打开 App，进入家庭账本。
2. **我们 → 编辑桌面组件**：上传照片、编辑说明（建议不超过 40 字）、选文案颜色（预设或自定义 `#RRGGBB`）。空状态显示「还没有照片」。相册上传会在客户端压成 JPEG。
3. 把系统小组件加到桌面：
   - 点页面里的 **添加到桌面**（部分系统会弹出确认）；或
   - 长按桌面空白处 → **小组件 / 微件** → 找到 **管管花**（英文环境 **GuanGuanHua**）拖上去。
4. 组件至少 2×2，可拉伸。照片 cover 铺满；有说明时叠在正中间，颜色来自 `widgetCaptionColor`。点组件会打开「桌面组件」页。

这部手机在 App 里保存后会马上刷新组件。另一部手机靠大约 **15 分钟** 一次的后台同步，或下次打开 App。部分厂商会限制后台任务，必要时允许自启动、关掉电池优化。

## API

都需要登录：`Authorization: Bearer <token>`。作用域是当前令牌所在的家庭。照片文件仍走现有 `photos/` 目录（阿里云数据盘 `/data/savemoney/photos`）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/widget` | 当前家庭的组件状态 |
| `PATCH` | `/api/widget` | 改说明和/或颜色。JSON 至少带一项：`{"widgetCaption":"周末去看海","widgetCaptionColor":"#F07A5C"}`。说明最多 40 字，空字符串表示清空说明。颜色为 `#RGB` / `#RRGGBB`（可省略 `#`），缺省或空字符串为白色 `#FFFFFF` |
| `POST` | `/api/widget/image` | `multipart/form-data` 字段名 `image`，换一张共享照片（不改说明和颜色）。客户端上传前会压成 `image/jpeg` |
| `DELETE` | `/api/widget/image` | 清空共享照片（不改说明和颜色） |

`GET` / `PATCH` / `POST` / `DELETE` 都返回：

```json
{
  "widgetImageUrl": "http://host/api/files/<file>.jpg",
  "widgetCaption": "周末去看海",
  "widgetCaptionColor": "#F07A5C",
  "widgetUpdatedBy": "小红",
  "widgetUpdatedAt": 1710000000000
}
```

还没传过照片时 `widgetImageUrl`、`widgetUpdatedBy`、`widgetUpdatedAt` 为 `null`，`widgetCaption` 为 `""`，`widgetCaptionColor` 为 `"#FFFFFF"`。图片 URL 与申请照片一样走 `GET /api/files/:filename`。
