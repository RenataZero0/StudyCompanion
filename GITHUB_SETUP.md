# GitHub 登录配置说明

> ## ✅ 已经配好了
> 从 **v2.0.1** 起，两个程序里都已经填入了你的 OAuth App Client ID
> （Ov23limNvMWKQQ3qKGD1），下载安装后**登录按钮直接就能用，不需要再做任何配置**。
>
> 下面这份说明保留下来，以备将来需要换账号、换 OAuth App，或者重新配置时参考。

---

# 一次性配置（已完成，留档）

仓库改成私有之后，两个程序要访问它就必须先登录。登录用的是 GitHub 官方的
**设备码流程（Device Flow）**——和 `gh` 命令行工具一样：

> 点「登录 GitHub」→ 程序显示一个 8 位代码 → 浏览器里粘贴 → 点 Authorize → 完成

⚠️ **这一步需要你先在 GitHub 上注册一个 OAuth App**，把它的 **Client ID** 填进代码里。
代码本身我已经全部写好了，只差这个 ID。设备码流程**不需要 client_secret**，
所以 Client ID 直接写在客户端里是安全的。

---

## 第 1 步：注册 OAuth App

打开这个链接：

### 👉 https://github.com/settings/applications/new

用你 **RenataZero0** 这个账号（必须是你自己的账号）。

按下表填写：

| 字段 | 填什么 |
|---|---|
| **Application name** | `StudyCompanion` （随便，只是显示用） |
| **Homepage URL** | `https://github.com/RenataZero0/StudyCompanion` |
| **Application description** | 留空即可 |
| **Authorization callback URL** | `https://github.com/RenataZero0/StudyCompanion` （设备码流程用不到，但**必填**，随便填个合法网址都行） |

**✅ 关键一步：往下找到 `Enable Device Flow`，把前面的勾选框勾上！**
不勾的话程序会报 `device_flow_disabled`。

填完点绿色的 **Register application**。

---

## 第 2 步：复制 Client ID

注册成功后会自动跳到应用页面。你会看到一个 **Client ID**，形如：

```
Ov23liXXXXXXXXXXXXXXXXXX
```

**只复制 Client ID 就够了**，不要复制 Client Secret（设备码流程根本用不到它，
也别把它发给任何人）。

---

## 第 3 步：填进代码

两个文件里各有一处 `PUT_YOUR_CLIENT_ID_HERE`，替换成你的 Client ID（**两边填同一个**）：

| 平台 | 文件 | 位置 |
|---|---|---|
| Windows | `StudyCompanion/src/GitHub.cs` | `public const string ClientId = "..."` |
| Android | `StudyCompanionAndroid/src/com/studycompanion/GitHub.java` | `public static final String CLIENT_ID = "..."` |

---

## 第 4 步：重新编译

```powershell
# Windows 版
cd StudyCompanion
powershell -ExecutionPolicy Bypass -File build.ps1

# Android 版
cd ..\StudyCompanionAndroid
powershell -ExecutionPolicy Bypass -File build.ps1
```

编译完，程序右侧/下方会出现一张 **GitHub 同步** 卡片，里面就是「登录 GitHub」按钮。

---

## 登录之后能做什么

| 按钮 | 作用 |
|---|---|
| **立即同步** | 把手机和电脑的打卡记录合并。策略是**并集**——两边各自标的完成都会保留，不会互相覆盖。记录存在仓库的 `sync/progress.txt` |
| **检查更新** | 读取最新 Release，发现新版就下载。Android 会调起系统安装器；Windows 会下载后自动替换自身并重启 |
| **退出登录** | 删除本机保存的令牌 |

令牌保存方式：

- **Windows**：用 DPAPI 加密后存在 `data/github.dat`，只有当前 Windows 用户能解密
- **Android**：存在应用私有 SharedPreferences 里（其他应用读不到）

---

## 常见问题

**Q：点了登录，浏览器里显示 device_flow_disabled**
A：OAuth App 没勾 `Enable Device Flow`。回第 1 步的链接进去补勾，保存即可。

**Q：一直卡在「正在向 GitHub 申请登录码」**
A：多半是网络。GitHub 的 API 在国内需要代理。桌面版会自动使用系统代理设置；
安卓版走手机网络，通常没问题。

**Q：Android 点「下载并安装」提示需要权限**
A：Android 8 起安装 APK 需要单独授权。点「去设置」→ 允许本应用安装应用 → 回来重新点。

**Q：同步会不会把我本地的记录删掉？**
A：不会。同步只做并集，任何一边的完成记录都只增不减。

**Q：Client Secret 要填吗？**
A：**不要**。设备码流程不需要它。Client Secret 是机密，永远不要发给任何人或写进客户端。
