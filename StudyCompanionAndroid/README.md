# 学习助手 · Android 版

和桌面版 `StudyCompanion` 功能一致、界面同一套设计语言的安卓应用。

**成品：`StudyCompanion.apk`（约 145 KB，minSdk 21 / targetSdk 34，已签名，可直接安装）**

> **v2.0.1 起已内置 OAuth Client ID** —— 装好后「GitHub 同步 → 登录 GitHub」直接可用，无需任何配置。

---

## 与桌面版的对应关系

| 功能 | 桌面版 | Android 版 |
|---|---|---|
| 读取 `Schedule.xlsx` 显示当天内容 | ✅ 直接解析 xlsx | ✅ 同一份解析逻辑（zip + DOM） |
| 配套资源下拉（精确资源 / YouTube / Bilibili） | ✅ | ✅ 同一张链接表 |
| 显示所需课本标题 | ✅ | ✅ |
| 到点提醒（开始 + 结束前 5 分钟） | ✅ 右下角浮窗 + 托盘气泡 | ✅ 通知栏 + 声音振动 |
| 完成打卡 / 记录持久化 | ✅ `progress.tsv` | ✅ SharedPreferences |
| 内置日历（翻月、任意日期、完成标记） | ✅ | ✅ |
| 学习统计（周进度 / 连续打卡 / 累计学时 / 本月全勤） | ✅ | ✅ |
| 更新日志阅读器 | ✅ 自绘 Markdown | ✅ 自绘 Markdown |
| CSV 导出 / 导入 | ✅ | ✅ 走系统文件选择器 |
| 开机自动启动 | ✅ 注册表 Run | ✅ BootReceiver 重排提醒 |
| **GitHub 登录 / 同步 / 自动更新** | ✅ v2.0.3（独立窗口） | ✅ v2.0.3 |
| 打开课本 PDF 并跳页 | ✅ | ❌ 见下方说明 |

### 平板适配

- **手机竖屏**（< 720dp）：单栏，从上到下依次是 任务卡片 → 日历 → 统计 → 工具
- **平板 / 横屏**（≥ 720dp）：自动切成**左右两栏** —— 左栏 60% 放任务卡片，右栏 40% 放日历 + 统计 + 工具，跟桌面版布局一致
- 内容整体居中并限制最大宽度 1180dp，超宽屏不会被拉成一条细长带；顶栏文字与下方卡片左右对齐

### 两点必要的差异

1. **没有「打开课本 PDF」按钮。** 课本 PDF 在电脑上（`Textbook\` 目录），安卓端拿不到。课本**标题**仍然照常显示。
2. **布局由左右两栏改成**：手机竖屏单栏，平板/横屏才切成两栏（见上）。配色、卡片、徽章、进度条、日历、下拉菜单的样式与桌面版完全一致。

---

## 安装

把 `StudyCompanion.apk` 传到手机（微信/QQ/网盘/数据线均可），点击安装。首次会提示"允许安装未知来源应用"。

> 这是用自签证书签名的（`debug.keystore`），所以：
> - 安装时可能提示"来源未知/存在风险"，选继续即可
> - 每次重新编译只要还用同一个 `debug.keystore`，就能覆盖安装、不会丢打卡记录

首次打开会申请**通知权限**（Android 13+ 必须手动允许，否则到点不会提醒）。

### 关于「精确闹钟」权限

Android 12 起，精确闹钟需要单独授权。如果没授权，程序会**自动降级为不精确闹钟**（可能晚几分钟提醒），不会崩。

想拿到分钟级准确提醒：系统设置 → 应用 → 学习助手 → 闹钟和提醒 → 允许。

### 关于省电策略

部分国产 ROM（小米/华为/OPPO/vivo）会冻结后台应用，导致提醒不准。建议在系统设置里把「学习助手」的**电池优化**关掉、允许**自启动**和**后台运行**。

---

## 项目结构

```
StudyCompanionAndroid\
├─ StudyCompanion.apk      成品（已签名，可直接安装）
├─ build.ps1               编译 APK（不需要 Gradle / Android Studio）
├─ selftest.ps1            编译 + 在电脑 JVM 上跑同一份解析代码做验证
├─ debug.keystore          签名用的自签证书（重新编译请保留，否则装不上覆盖）
├─ AndroidManifest.xml
├─ assets\
│   ├─ Schedule.xlsx       课表（与桌面版同一份）
│   └─ CHANGELOG.md        更新日志（应用内可查看）
├─ res\
│   ├─ mipmap-*            启动图标（5 种密度，同一套设计）
│   └─ values\strings.xml
├─ src\com\studycompanion\
│   ├─ MainActivity.java   入口、通知权限、CSV 导入导出
│   ├─ MainView.java       整个界面（自绘：顶部栏 / 卡片 / 日历 / 统计 / 工具 / 下拉菜单）
│   ├─ ScheduleData.java   xlsx 解析（只用 java.* 和 javax.xml.*，所以能在电脑上跑测试）
│   ├─ Links.java          小节 → 配套资源 / 课本名
│   ├─ Store.java          打卡记录、统计、CSV
│   ├─ Reminder.java       AlarmManager 排程 + 通知
│   ├─ ReminderReceiver.java
│   ├─ BootReceiver.java   开机后重排提醒
│   ├─ DocActivity.java    更新日志阅读器
│   └─ Ui.java             配色与绘制工具
├─ tools\
│   ├─ SelfTest.java       电脑端自检（验证课表解析与链接映射）
│   └─ make_android_icons.py
└─ preview\ic_launcher-512.png
```

---

## 重新编译

不需要 Gradle，也不需要 Android Studio，只用 SDK 里的 `aapt2 / d8 / zipalign / apksigner` 加 JDK：

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

依赖的绝对路径写在 `build.ps1` 开头（`D:\android-sdk`、`D:\Program Files\Java\jdk-21`），换机器改那两行即可。

编译流程：

```
aapt2 compile  →  aapt2 link（清单 + 资源 + assets）
   →  javac --release 8  →  d8 生成 classes.dex
   →  把 dex 塞进 APK  →  zipalign  →  apksigner 签名
```

### 电脑端自检

```powershell
powershell -ExecutionPolicy Bypass -File selftest.ps1
```

它会编译 APK，然后**在电脑 JVM 上直接跑 `ScheduleData` / `Links` 这两个类**（这两个类刻意只依赖 `java.*` 与 `javax.xml.*`，所以能脱离安卓运行），核对：

- 课表天数、日期范围
- 指定日期解析出的时段
- 课本识别与链接映射条数

这样发布前就能发现解析逻辑的问题，不用等装到手机上。

---

## v2.0.5 改动

### 1. 日历 / 学习统计 / 设置与工具 从主界面挪走（手机端）

手机主界面现在**只有今天的任务卡片**，底部多了一条按钮栏：

```
[ 月历 ]   [ 学习统计 ]   [ 设置与工具 ]
```

点任一个进入对应的独立页面，左上角标题、右上角「关闭」按钮，返回键也能关。
平板横屏仍然保持左右两栏（放得下，没必要藏起来）。

### 2. 去掉「导出 / 导入 CSV」按钮

同步时会把打卡记录导出成 CSV 一并上传到仓库 `sync/StudyRecord.csv`，
想要表格直接去仓库下载，不再需要手动导出导入。

### 3. 修复上下滑动「回滚」

`VelocityTracker.getYVelocity()` 返回的是**手指**的速度：往上滑手指 y 减小 → 速度为负。
而 `OverScroller.fling()` 要的是**滚动坐标**的速度（正 = 增大 scrollY）。

之前直接把手指速度传进去了，没取反 —— 所以每次滑完都往反方向弹回去。
改成 `scroller.fling(..., -vy, ...)`。

### 4. 修复工具卡片里说明文字压在按钮上

排版循环里「画按钮」和「画说明行」用的是同一个 `cy`，按钮那一行还没结束就把说明画上去了。
现在先把按钮全部排完（含自动换行），再画说明行。

### 5. 新增崩溃日志捕获

安卓上只看「闪退」两个字没法定位问题。现在：

- 任何未捕获异常都会把完整堆栈写到
  `/sdcard/Android/data/com.studycompanion/files/crash.txt`
- 下次启动时会弹窗显示，带「复制」「分享」两个按钮，可以直接发给我

> 如果平板还是闪退，装上新版再崩一次，然后把弹窗里的内容发我 —— 这次能看到具体是哪一行。

---

## v2.0.4 / v2.0.3 修掉的三个真机 bug

这三个都是**在模拟器上真跑起来才发现的** —— 之前只做了「编译通过 + 数据层单测」，看不出这些问题。

### 1. 一打开就黑屏闪退（v2.0.4 修）

```
java.lang.StringIndexOutOfBoundsException: length=7; index=10
	at com.studycompanion.MainView.calOf(MainView.java:692)
	at com.studycompanion.MainView.drawCalendar(MainView.java:452)
	at com.studycompanion.MainView.onDraw(MainView.java:237)
```

`calOf()` 要的是完整日期 `2026-10-02`，但日历那边传进去的是月份 `2026-10`（只有 7 个字符），
`substring(8, 10)` 直接越界 → `onDraw` 抛异常 → 窗口还没画出来就崩了。

修复：传 `displayMonthIso + "-01"`，并且让 `calOf()` 自己也能容错（只给月份就补成 1 号，
彻底解析不了就用今天），不再依赖调用方。

### 2. 日历标题、左右箭头、星期表头挤在同一行（v2.0.4 修）

三者的 Y 坐标算下来都落在 `y+20 ~ y+42`，叠在一起看不清。

修复：标题和箭头一行，星期表头单独一行并加分隔线，日期网格从 `y+72` 开始，
格子高度按卡片高度 `(h - 80) / 6` 自适应，不再写死 42。

### 3. 平板上卡片里的链接标签跑到卡片外面（v2.0.4 修）

画链接标签时只偏移了纵向 `pr.offset(0, base)`，横向没偏移。
单栏布局时 `pad ≈ 0` 看不出来，**平板切成两栏后卡片左边距变成 50+，标签就贴到屏幕左边缘了**。

修复：`pr.offset(pad, base)`。

---

## 已知限制 / 未验证项

- **已在模拟器上实机验证**（v2.0.4）。装好 Android Emulator Hypervisor Driver 后，
  分别用 **Pixel 5（1080×2340 @440dpi）** 和 **10.1" 平板（1280×800 @160dpi）** 两个 AVD 跑通：
  - 手机竖屏：单栏，任务卡 / 日历 / 统计 / 工具全部正常渲染
  - 平板横屏：自动切成左右两栏，卡片、日历、链接标签位置都正确
  - 启动无崩溃，滚动、点击正常
  - 截图见 `preview/phone.png` 与 `preview/tablet.png`
- **仍未验证**：Android 端的 GitHub 登录 / 同步 / 自动更新没有在真机上点过
  （桌面端同一套 API 已实测通过，但安卓用的是 `HttpURLConnection` + `org.json`，是另一条代码路径）
- **课本 PDF 打不开**（文件在电脑上，且体积超过 GitHub 允许的范围）
- 课表是**打包进 APK** 的。如果之后改了 `Schedule.xlsx`，需要重新编译 APK

## 编译时踩过的两个坑（改代码前请先看）

这两条都是被工具链逼出来的，**改动源码前务必遵守**：

1. **不要用 lambda。** 虽然 `javac --release 8` 能编，但 d8 处理 invokedynamic 时容易出问题。
   全部用显式匿名内部类。
2. **build-tools 不要用 34.0.0。** 它的 d8 在解析**两层嵌套的匿名类**（形如 `MainActivity$1$1`）时会内部崩溃：
   ```
   java.lang.NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null
   ```
   35.0.0 和 36.0.0 都正常。`build.ps1` 里已经写死 36.0.0。

另外，对话框和按钮监听器尽量写在**具名方法**里，后台线程只负责调方法 —— 这样嵌套层级浅，可读性也好。
