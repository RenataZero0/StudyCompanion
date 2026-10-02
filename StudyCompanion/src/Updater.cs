using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 每天一次的自动检查更新。
    ///
    /// 以前这段逻辑写在 MainForm 的构造函数里 —— 但程序开机自启是 `--tray`，
    /// **只起托盘图标、不建主窗口**，所以那个检查实际上从来没跑过。
    /// 现在挪到程序启动时自己起一条后台线程，跟主窗口开不开没关系。
    /// </summary>
    public static class Updater
    {
        static Control _ui;

        /// <summary>在 Main 里调用一次。ui 只是用来把结果切回 UI 线程的隐藏控件。</summary>
        public static void Start(Control ui)
        {
            _ui = ui;
            var t = new Thread(Run);
            t.IsBackground = true;
            t.Start();
        }

        static void Run()
        {
            // 让主界面/托盘先起来，别抢启动时间
            try { Thread.Sleep(4000); } catch { }

            string today = DateTime.Today.ToString("yyyy-MM-dd");
            string stamp = Path.Combine(Store.DataDir, "lastupdate.txt");
            try
            {
                if (File.Exists(stamp) && File.ReadAllText(stamp).Trim() == today) return;
            }
            catch { }

            try
            {
                var rel = GitHub.LatestRelease();
                try { File.WriteAllText(stamp, today); } catch { }

                // 顺手把更新日志也刷一下，这样打开阅读器就是最新的
                try { string e2; Changelog.Fetch(out e2); } catch { }

                if (rel == null || string.IsNullOrEmpty(rel.Tag)) return;
                // 远端不比本机新就什么都不做（之前用 equals，降级也会被当成升级）
                if (CompareVersion(rel.Tag, GitHub.VersionTag) <= 0) return;
                if (string.IsNullOrEmpty(rel.ExeDownload(GitHub.LoggedIn))) return;

                Post(delegate { Prompt(rel); });
            }
            catch
            {
                // 没网 / 被墙 / 限流都无所谓，静默跳过
            }
        }

        static void Post(Action a)
        {
            try
            {
                if (_ui != null && _ui.IsHandleCreated) _ui.BeginInvoke(a);
                else a();
            }
            catch { }
        }

        /// <summary>版本号比较：忽略前面的 v，2.1.1 == v2.1.1</summary>
        public static bool SameVersion(string a, string b)
        {
            return CompareVersion(a, b) == 0;
        }

        /// <summary>
        /// 数值比较版本号：a &gt; b 返回 1，相等 0，a &lt; b 返回 -1。
        /// 不能只判断「相不相等」—— 那样远端版本比本机旧时也会被当成有新版本。
        /// 也顺便修掉 2.1.10 &lt; 2.1.9 这种字符串比较的坑。
        /// </summary>
        public static int CompareVersion(string a, string b)
        {
            int[] pa = ParseVer(a), pb = ParseVer(b);
            for (int i = 0; i < 3; i++)
            {
                if (pa[i] != pb[i]) return pa[i] > pb[i] ? 1 : -1;
            }
            return 0;
        }

        static int[] ParseVer(string v)
        {
            var r = new int[3];
            v = Clean(v);
            var parts = v.Split('.');
            for (int i = 0; i < 3 && i < parts.Length; i++)
            {
                string digits = "";
                foreach (char c in parts[i])
                {
                    if (c < '0' || c > '9') break;
                    digits += c;
                }
                int n = 0;
                int.TryParse(digits, out n);
                r[i] = n;
            }
            return r;
        }

        static string Clean(string v)
        {
            if (v == null) return "";
            v = v.Trim();
            return v.StartsWith("v", StringComparison.OrdinalIgnoreCase) ? v.Substring(1) : v;
        }

        /// <summary>
        /// 发现新版本时的提示。
        /// 用正经的对话框 + MarkdownView 渲染改动说明 ——
        /// 以前是塞进 MessageBox，原始 Markdown 全露在外面。
        /// </summary>
        static void Prompt(GitHub.Release rel)
        {
            var owner = MainForm.Instance;

            UpdateDialog dlg = null;
            try
            {
                dlg = new UpdateDialog(rel);
                if (owner != null && owner.Visible) dlg.ShowDialog(owner);
                else dlg.ShowDialog();
            }
            catch
            {
                // 万一对话框出问题，退回到最简单的提示
                var ans = MessageBox.Show(owner,
                    "发现新版本 " + rel.Tag + "\n当前：" + GitHub.VersionTag
                    + "\n\n现在下载并安装吗？",
                    "学习助手 · 发现新版本", MessageBoxButtons.OKCancel, MessageBoxIcon.Information);
                if (ans == DialogResult.OK) DownloadAndReplace(rel, owner);
                return;
            }

            if (dlg.Result == UpdateDialog.Choice.Download) DownloadAndReplace(rel, owner);
            else if (dlg.Result == UpdateDialog.Choice.ReleasePage)
            {
                try
                {
                    string url = rel.PageUrl;
                    if (!string.IsNullOrEmpty(url)) System.Diagnostics.Process.Start(url);
                }
                catch { }
            }
        }

        /// <summary>下载新 exe 并替换自己、重启</summary>
        public static void DownloadAndReplace(GitHub.Release rel, IWin32Window owner)
        {
            string tmp = Path.Combine(Path.GetTempPath(), "StudyCompanion-" + rel.Tag + ".exe");
            try
            {
                if (File.Exists(tmp)) File.Delete(tmp);
                GitHub.Download(rel.ExeDownload(GitHub.LoggedIn), tmp, null);
                var ok = MessageBox.Show(owner,
                    "已下载到：\n" + tmp + "\n\n点「确定」后程序会退出，自动替换并重启。",
                    "下载完成", MessageBoxButtons.OKCancel, MessageBoxIcon.Information);
                if (ok != DialogResult.OK) return;
                ReplaceSelf(tmp, owner);
            }
            catch (Exception ex)
            {
                MessageBox.Show(owner, "下载失败：" + ex.Message, "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        }

        /// <summary>
        /// 正在运行的 exe 不能被覆盖，写个批处理：等本进程退出 → 覆盖 → 重启。
        /// 批量文件必须用系统 ANSI 写 —— cmd.exe 是按 ANSI 读 .bat 的，
        /// 用 UTF-8 写的话路径里的中文会变乱码。
        /// </summary>
        public static void ReplaceSelf(string newExe, IWin32Window owner)
        {
            try
            {
                string self = Application.ExecutablePath;
                string bat = Path.Combine(Path.GetTempPath(), "sc-update.bat");
                var sb = new StringBuilder();
                sb.AppendLine("@echo off");
                sb.AppendLine("ping 127.0.0.1 -n 3 > nul");
                sb.AppendLine(":retry");
                sb.AppendLine("copy /y \"" + newExe + "\" \"" + self + "\" > nul 2>&1");
                sb.AppendLine("if errorlevel 1 ( ping 127.0.0.1 -n 2 > nul & goto retry )");
                sb.AppendLine("del \"" + newExe + "\" > nul 2>&1");
                sb.AppendLine("start \"\" \"" + self + "\"");
                sb.AppendLine("del \"%~f0\" > nul 2>&1");
                File.WriteAllText(bat, sb.ToString(), Encoding.Default);

                var psi = new ProcessStartInfo("cmd.exe", "/c \"" + bat + "\"");
                psi.WindowStyle = ProcessWindowStyle.Hidden;
                psi.UseShellExecute = true;
                Process.Start(psi);
                MainForm.ForceQuit();
            }
            catch (Exception ex)
            {
                MessageBox.Show(owner, "自动替换失败：" + ex.Message, "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        }

        /// <summary>手动「检查更新」按钮用</summary>
        public static void CheckManually(IWin32Window owner)
        {
            try
            {
                var rel = GitHub.LatestRelease();
                if (rel == null || string.IsNullOrEmpty(rel.Tag))
                {
                    MessageBox.Show(owner, "没有查到版本信息（可能是网络问题）。", "检查更新");
                    return;
                }
                int cmp = CompareVersion(rel.Tag, GitHub.VersionTag);
                if (cmp == 0)
                {
                    MessageBox.Show(owner, "已经是最新版本 " + GitHub.VersionTag + "。", "检查更新");
                    return;
                }
                if (cmp < 0)
                {
                    MessageBox.Show(owner,
                        "本机版本比线上还新。\n\n本机：" + GitHub.VersionTag + "\n线上：" + rel.Tag,
                        "检查更新");
                    return;
                }
                Prompt(rel);
            }
            catch (Exception ex)
            {
                MessageBox.Show(owner, "检查更新失败：" + ex.Message, "提示");
            }
        }
    }
}
