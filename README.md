# K4 早班车（Android）

每天早上 **8:20** 自动查一次西宁 **K4 路**「绿地·云香郡 → 青海师范大学东」方向的实时位置，
把**离「宁张路二十里铺」站最近的 3 辆车还有几站**推送到通知栏。打开 App 也能随时手动刷新。

```
K4  绿地·云香郡 → 青海师范大学东
目标站：宁张路二十里铺（第 21 站）
清晨 08:20 通知：

1. 青A-03532D · 还有 1 站 · 1.0 公里 · 约 1 分钟
2. 青A-03356D · 还有 7 站 · 5.3 公里 · 约 13 分钟
3. 青A-03697D · 还有 13 站 · 9.5 公里 · 约 26 分钟
```

## 功能

| 功能 | 说明 |
| --- | --- |
| 每日 8:20 提醒 | `AlarmManager.setExactAndAllowWhileIdle` + 每天重排；开机 / 改时区自动恢复 |
| 站点数换算 | 站序差 = 「目标站站序 21」−「车辆当前站序」，只统计还没过站的车 |
| 最近 3 辆 | 按站数升序取前 3，同时给出米数、预计到站时间、车牌 |
| 手动刷新 | 主界面按钮，进入 App 也会自动刷新一次 |
| 断网兜底 | 上次查询结果缓存，失败时仍然显示上次时间 |
| 参数可改 | 提醒时间、是否只工作日、线路 / 站点全部可在设置页改，并支持「自动获取参数」 |
| 示例通知 | 底部「发一条示例通知」可立刻验证通知样式与权限 |

## 数据从哪来

用的是「车来了」实时公交数据，通过开源的 [chelaile-api-server](https://github.com/Justintunsday/chelaile-api-server)
兼容接口（`/v1/search`、`/v1/lines/detail`、`/v1/lines/realtime`）取数。默认地址是它的托管实例，
**可以在设置页换成你自己部署的实例**（Vercel / Cloudflare Workers / 自己的服务器都行）。

固定下来的西宁参数（2026-07 调线后的走向）：

| 参数 | 值 |
| --- | --- |
| 城市编号 | `566`（西宁） |
| 线路编号 | `566308918036639`（方向：绿地·云香郡 → 青海师范大学东） |
| 目标站 | 宁张路二十里铺，站序 `21`，站点编号 `566-374` |
| 坐标（WGS-84） | `36.712286, 101.762056` |
| 首末班 | 06:30 – 19:00，票价 2 元 |

> 线路走向来自[西宁公交 2026-07-02 的调线通知](https://www.qhbtv.com.cn/m/folder9018/folder24/folder9020/folder9091/2026-07-02/3765928.html)：
> K4 调整为「青海师范大学东 ↔ 绿地云香郡」，新增青海师范大学东、西交大一附院青海医院、青海大学东、
> 莫家泉湾、宁张路二十里铺、二十里铺南站、宁张路新村等站，取消城北客运站、宁大路新村。

## 构建安装

环境：Android Studio（Koala 或更新）、JDK 17、Android SDK 34。

1. Android Studio → `File ▸ Open` → 选中本目录（`K4Bus`）。
2. 首次同步会下载 Gradle 8.7、AGP 8.5.2、Kotlin 1.9.24 与 AndroidX 依赖。
   如果提示缺少 Gradle wrapper，让 Studio 自动补，或在项目根目录执行一次
   `gradle wrapper --gradle-version 8.7`。
3. 连上手机（开启 USB 调试）→ 点 ▶ 运行；或 `Build ▸ Build APK(s)` 生成
   `app/build/outputs/apk/debug/app-debug.apk` 直接安装。

命令行等价操作（已装好 Gradle / SDK 时）：

```bash
gradle :app:assembleDebug
```

### 不想装 Android Studio？

把整个 `K4Bus` 目录推到 GitHub 仓库，`.github/workflows/build-apk.yml` 会自动用 JDK 17 + SDK 34
打出 debug APK，在 Actions 页面的 Artifacts 里下载 `k4-debug-apk` 即可。

## 装好后的三步授权

1. **通知权限**：首次打开会弹窗，选「允许」；拒绝了可以在设置页顶部黄色提示条点进去重开。
2. **闹钟和提醒**（Android 12+）：设置页顶部提示条 → 打开「闹钟和提醒」，否则 8:20 可能晚几分钟。
3. **电池优化**：系统设置 → 电池 → 找到「K4 早班车」→ 设为「不限制 / 允许后台活动」，
   国产 ROM 还要在「自启动管理」里放行，否则被杀后台后闹钟不响。

## 目录结构

```
K4Bus/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/xining/k4/
│   │   ├── MainActivity.kt        主界面：刷新、渲染最近 3 辆、权限提示
│   │   ├── SettingsActivity.kt    提醒时间 / 线路参数 / 自动获取参数
│   │   ├── Prefs.kt               全部配置与默认值（含西宁 K4 现成参数）
│   │   ├── BusApi.kt              取数：realtime 解析 + 线路自动发现
│   │   ├── Models.kt              BusInfo / QueryResult / DiscoverResult
│   │   ├── Formats.kt             站数、米数、时间的文案，通知与界面共用
│   │   ├── K4Route.kt             25 站站序（展示与自检用）
│   │   ├── AlarmScheduler.kt      每天 8:20 的精确闹钟
│   │   ├── AlarmReceiver.kt       到点查询 + 推通知（goAsync）
│   │   ├── BootReceiver.kt        开机 / 改时间后重排闹钟
│   │   └── Notifier.kt            通知渠道与通知内容
│   └── res/                       布局、颜色、矢量图标
├── tools/check_k4.js              命令行自检：不装 App 也能验证接口与站数算法
└── README.md
```

## 自检（不用装机）

```bash
node tools/check_k4.js
```

输出与 App 里的算法完全一致（站序差、米数、预计时间），可以用来确认接口是否还活着。

## 已知限制

- 「车来了」没有公开 API，这里依赖第三方兼容服务；**托管实例哪天挂了就需要自建或换地址**，
  所以接口地址做成了设置项。
- 站序差是「还有几站」的近似：车辆在两个站之间时按「即将到达的那一站」计。
- 关机期间不会查询；开机后闹钟会自动重排，当天的 8:20 若已过则顺延到第二天。
- 本项目未在打包环境里编译过（生成环境没有 Android SDK），代码按 Gradle 8.7 / AGP 8.5.2 编写，
  若 Studio 提示版本差异，按提示升级 AGP 即可。
