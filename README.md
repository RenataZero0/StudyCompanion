# 学习助手 StudyCompanion

一个跟着 `Schedule.xlsx` 走的学习计划工具：**每天显示今天要学什么、配套教学资源在哪、到点提醒你开始，并记录有没有完成。**

同时提供 **Windows 桌面版** 和 **Android 版**，两边读取同一份课表、共用同一套界面设计。

---

## 直接下载

| 平台 | 文件 | 说明 |
|---|---|---|
| **Windows** | **[⬇ StudyCompanion.exe](https://github.com/RenataZero0/StudyCompanion/releases/latest/download/StudyCompanion.exe)** | 免安装，双击即用（.NET Framework 4.x，Windows 自带） |
| **Android** | **[⬇ StudyCompanion.apk](https://github.com/RenataZero0/StudyCompanion/releases/latest/download/StudyCompanion.apk)** | Android 5.0+，已签名，直接安装 |

上面是**永久直链**，永远指向最新版，可以收藏或分享。

也可以到 [**Releases 页面**](https://github.com/RenataZero0/StudyCompanion/releases/latest) 手动下载，或查看历史版本。

> **离线取用**：如果只想拿文件、不需要看源码，直接下这两个就行：
> ```
> https://github.com/RenataZero0/StudyCompanion/releases/latest/download/StudyCompanion.exe
> https://github.com/RenataZero0/StudyCompanion/releases/latest/download/StudyCompanion.apk
> ```
>
> 国内访问 GitHub 较慢时，可以在浏览器里用代理，或把链接发到手机再下。

---

## 它做什么

- **今天学什么**：直接解析 `Schedule.xlsx`，按当天日期取出时间段、科目、要读的小节与页码、练习范围
- **配套教学资源**：按当天小节自动匹配播放列表 / YouTube 搜索 / Bilibili 搜索（免翻墙），下拉菜单里选
- **到点提醒**：时段开始时提醒，结束前 5 分钟再提醒一次
- **完成打卡**：点一下标记完成，日历打钩、进度条前进，记录永久保存
- **内置日历与统计**：翻月回看任意一天；本周进度、连续打卡、累计学时、本月全勤
- **学习记录导出 / 导入**：CSV 格式，可备份可恢复
- **更新日志**：程序内可直接查看

---

## 目录结构

```
.
├─ Schedule.xlsx              课表（两个平台共用，日程数据源）
├─ 学习规划.md                 完整自学规划：时间安排、五阶段路线图、执行建议
├─ caie-video-resources.md    配套教学视频资源清单
├─ _plan/                     课表生成脚本（课程目录 + 排课引擎）
│
├─ StudyCompanion/            Windows 桌面版
│   ├─ StudyCompanion.exe     主程序
│   ├─ build.ps1              编译（只要 Windows 自带的 csc.exe）
│   ├─ src/                   C# 源码
│   ├─ assets/                图标与截图
│   └─ data/                  课本路径、小节页码索引
│
└─ StudyCompanionAndroid/     Android 版
    ├─ StudyCompanion.apk     主程序
    ├─ build.ps1              编译（aapt2 + javac + d8 + zipalign + apksigner，不需要 Gradle）
    ├─ selftest.ps1           在电脑 JVM 上跑安卓端同一份解析代码做验证
    ├─ src/                   Java 源码
    ├─ assets/                课表与更新日志（打包进 APK）
    └─ res/                   启动图标
```

---

## 重新编译

### Windows 版

```powershell
cd StudyCompanion
powershell -ExecutionPolicy Bypass -File build.ps1
```

只要 Windows 自带的 `csc.exe`，不需要 Visual Studio。

### Android 版

需要 JDK + Android SDK 的 `build-tools` 与 `platform-34`（不需要 Gradle，也不需要 Android Studio）：

```powershell
cd StudyCompanionAndroid
powershell -ExecutionPolicy Bypass -File build.ps1
```

依赖的绝对路径写在 `build.ps1` 开头，换机器改那两行即可。`setup_sdk.ps1` 是当初准备 SDK 用的脚本，可参考。

想先验证数据层再打包：

```powershell
powershell -ExecutionPolicy Bypass -File selftest.ps1
```

它会编译 APK，然后**在电脑 JVM 上直接跑 `ScheduleData` / `Links` 这两个类**（它们刻意只依赖 `java.*` 与 `javax.xml.*`），核对课表天数、指定日期的时段、课本识别与链接映射。

### 改课表

课表由 `_plan/` 里的脚本生成：

```powershell
python _plan\build_schedule.py
```

`curriculum.py` 是六本课本的章节与页码目录，`build_schedule.py` 是排课引擎（时间模板、假期表、每科课时队列）。改完重新跑，会覆盖生成 `Schedule.xlsx`。

> Android 版的课表是**打包进 APK** 的，改完课表要重新编译 APK。

---

## 说明

- **仓库里不含课本 PDF。** 课本是有版权的教材，仓库只记录它们的**书名、章节与页码**（`StudyCompanion/data/books.tsv`、`_plan/curriculum.py`）。本地的 PDF 请自行从正规渠道获取，放在 `Textbook/` 目录下，Windows 版会自动按文件名定位。
- Windows 版的 `data/progress.tsv`（打卡记录）已在 `.gitignore` 中排除，不会随仓库走。
- Android 版用自签证书签名（`StudyCompanionAndroid/debug.keystore`）。更新应用时请保留同一个 keystore，否则无法覆盖安装、打卡记录会丢。
