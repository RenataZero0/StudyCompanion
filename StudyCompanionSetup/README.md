# 学习助手 · Windows 安装程序

把「学习助手 StudyCompanion」装进 Windows 的安装包源码。

**成品：`StudyCompanion-Setup.exe`（约 292 KB，主程序 / 课表 / 课本索引全部内嵌，单文件）**

---

## 它做什么

| 功能 | 说明 |
|---|---|
| **三步安装向导** | 欢迎 → 安装位置 → 安装选项 |
| **开始菜单快捷方式** | 必定创建 |
| **桌面快捷方式** | 可选（第 3 步勾选） |
| **开机自启** | 可选，注册到 `HKCU\...\Run` |
| **出现在「应用和功能」** | 显示名称、版本、发布者、大小，带卸载按钮 |
| **卸载程序** | 可选是否一并删除学习记录 |
| **静默安装** | `StudyCompanion-Setup.exe --silent` |

**每用户安装，不需要管理员权限，全程不弹 UAC。**

```
%LOCALAPPDATA%\Programs\StudyCompanion\     程序文件
%APPDATA%\StudyCompanion\                   学习记录 / 设置 / 登录令牌
```

用户数据放在 `%APPDATA%` 而不是安装目录，因为装到 Program Files 之后程序写不了自己的目录。
从旧的绿色版升级过来时，第一次运行会自动把 exe 旁边 `data\` 里的内容搬过去，**不会丢打卡记录**。

---

## 目录结构

```
StudyCompanionSetup\
├─ StudyCompanion-Setup.exe   成品（单文件安装包）
├─ build-setup.ps1            编译
├─ src\
│   ├─ Program.cs             入口：向导 / --silent / --uninstall / --shot
│   ├─ WizardForm.cs          安装向导界面 + 卸载流程
│   ├─ Installer.cs           安装与卸载的实际动作
│   └─ Shortcuts.cs           快捷方式（WScript.Shell）与内嵌资源读取
└─ preview\                   向导各页截图（由 --shot 生成）
```

---

## 重新编译

```powershell
powershell -ExecutionPolicy Bypass -File build-setup.ps1
```

它需要先编译好主程序（`..\StudyCompanion\StudyCompanion.exe`），然后：

1. 从 `..\StudyCompanion\src\GitHub.cs` 的 `VersionTag` 读出**版本号**
   （这样安装包和程序版本永远不会对不上）
2. 生成 `build\SetupInfo.cs`
3. 用 `csc` 编译，并把主程序、`Schedule.xlsx`、`books.tsv`、`pages.tsv` 作为**资源**打进去

> 注意：`/resource:` 的参数里有逗号，PowerShell 会把它当数组分隔符，
> 所以整个参数要加引号写成 `"/resource:$path,Name"`。

---

## 命令行参数

| 参数 | 作用 |
|---|---|
| （无） | 打开安装向导 |
| `--silent` | 静默安装到默认位置（建桌面快捷方式，不开机自启） |
| `--uninstall` | 卸载（从安装目录里的「卸载 学习助手.exe」调用） |
| `--uninstall --silent` | 静默卸载，保留学习记录 |
| `--shot <png> [--page N]` | **把向导第 N 页渲染成图片**（离屏，不弹窗、不安装）—— 用来验证界面 |

`--shot` 是照着主程序的做法加的：把窗口移到屏幕外再 `DrawToBitmap`，
这样改界面时不用真的弹一个安装向导出来。

---

## 卸载时发生了什么

1. 结束正在运行的程序
2. 删除开始菜单与桌面快捷方式
3. 删除 `HKCU\...\Uninstall\StudyCompanion` 注册表项
4. 删除安装目录里的文件
5. **询问**是否删除 `%APPDATA%\StudyCompanion`（学习记录、令牌）
6. 卸载程序删不掉自己，所以写一个批处理延迟自删

> 第 6 步的批处理必须用 `Encoding.Default`（系统 ANSI）写。
> 曾经用 UTF-8 写过，结果 **cmd.exe 按 ANSI 解码，中文路径变乱码，自删失败**，
> 卸载完会在目录里留一个卸载程序。这个坑已经踩过并修好了。
