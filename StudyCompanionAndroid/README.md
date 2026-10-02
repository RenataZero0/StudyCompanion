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

## 已知限制 / 未验证项

- **界面没有在真机或模拟器上跑过。** 本机 `accel-check` 报「Android Emulator hypervisor driver is not installed」，
  且 `HypervisorPresent = False`；开启需要管理员权限。手动启动模拟器直接以 `0xC0000005`（访问违例）崩溃。
  所以：
  - **已严格验证**：数据层（课表解析、链接映射）在电脑 JVM 上跑通，输出与桌面版逐项一致
  - **已验证**：APK 结构、清单、权限、图标、签名（v1 + v2）
  - **未验证**：真机上的绘制效果、触摸手感，以及**平板两栏布局的实际观感**
  - 装上后如果哪里不对，截图发我，我来调
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
