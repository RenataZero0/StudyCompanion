using System;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// GitHub 同步 —— 从主界面右侧栏里独立出来的专用窗口。
    ///
    /// 主界面顶栏上那个「GitHub 同步」按钮点开的就是它。
    /// 所有网络操作都放在后台线程里跑，窗口本身保持响应。
    /// </summary>
    public class GitHubForm : Form
    {
        readonly Label _status = new Label();
        readonly Label _log = new Label();
        readonly Panel _buttons = new Panel();
        readonly Label _proxy = new Label();
        readonly System.Windows.Forms.Timer _busy = new System.Windows.Forms.Timer();

        bool _working;
        int _dots;

        public GitHubForm()
        {
            Text = "GitHub 同步";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            StartPosition = FormStartPosition.CenterParent;
            MaximizeBox = false;
            MinimizeBox = false;
            ShowInTaskbar = false;
            ClientSize = new Size(Ui.Px(520), Ui.Px(356));
            BackColor = Color.White;
            Font = Ui.F(9.5f);
            try { Icon = AppIcon.Get(32); } catch { }

            var title = new Label();
            title.Text = "GitHub 同步";
            title.Font = Ui.F(14f, true);
            title.ForeColor = Ui.Ink;
            title.SetBounds(Ui.Px(24), Ui.Px(18), Ui.Px(470), Ui.Px(30));
            Controls.Add(title);

            var hint = new Label();
            hint.Text = "登录后可以同步手机与电脑的打卡记录，并检查、下载新版本。";
            hint.Font = Ui.F(9f);
            hint.ForeColor = Ui.Sub;
            hint.SetBounds(Ui.Px(24), Ui.Px(48), Ui.Px(470), Ui.Px(22));
            Controls.Add(hint);

            _status.Text = "";
            _status.Font = Ui.F(10f, true);
            _status.ForeColor = Ui.Ink;
            _status.SetBounds(Ui.Px(24), Ui.Px(80), Ui.Px(470), Ui.Px(26));
            Controls.Add(_status);

            var line = new Panel();
            line.BackColor = Ui.Line;
            line.SetBounds(Ui.Px(24), Ui.Px(112), Ui.Px(472), 1);
            Controls.Add(line);

            _log.Text = "";
            _log.Font = Ui.F(9f);
            _log.ForeColor = Ui.Sub;
            _log.SetBounds(Ui.Px(24), Ui.Px(124), Ui.Px(472), Ui.Px(110));
            Controls.Add(_log);

            _buttons.SetBounds(0, Ui.Px(270), Ui.Px(520), Ui.Px(60));
            _buttons.BackColor = Color.White;
            Controls.Add(_buttons);

            // 代理入口：内地直连 github.com 基本不通，连不上时用户得能自己配一个
            _proxy = new Label();
            _proxy.Font = Ui.F(9f);
            _proxy.ForeColor = Ui.Accent;
            _proxy.Cursor = Cursors.Hand;
            _proxy.SetBounds(Ui.Px(24), Ui.Px(240), Ui.Px(472), Ui.Px(22));
            _proxy.Click += delegate
            {
                using (var f = new ProxyForm()) f.ShowDialog(this);
                RefreshProxy();
            };
            Controls.Add(_proxy);
            RefreshProxy();

            var close = new Pill();
            close.Text = "关闭";
            close.SetBounds(Ui.Px(520) - Ui.Px(24) - Ui.Px(90), Ui.Px(10), Ui.Px(90), Ui.Px(32));
            close.Click += delegate { Close(); };
            _buttons.Controls.Add(close);

            _busy.Interval = 400;
            _busy.Tick += delegate
            {
                _dots = (_dots + 1) % 4;
                _status.Text = _working ? ("处理中" + new string('.', _dots)) : _status.Text;
            };

            Rebuild();
        }

        // ============================================================== 界面
        void Rebuild()
        {
            foreach (Control c in _buttons.Controls.Cast<Control>().ToList())
                if (!(c is Pill) || ((Pill)c).Text != "关闭") _buttons.Controls.Remove(c);

            int x = Ui.Px(24);
            int y = Ui.Px(10);
            int h = Ui.Px(32);

            if (!GitHub.Configured)
            {
                _status.Text = "尚未配置";
                _status.ForeColor = Ui.Amber;
                _log.Text = "程序里还没有填入 OAuth App 的 Client ID，"
                          + "请参考仓库根目录的 GITHUB_SETUP.md 配置后重新编译。";
                return;
            }

            if (GitHub.LoggedIn)
            {
                _status.Text = "已登录：" + (GitHub.User.Length > 0 ? GitHub.User : "(已授权)");
                _status.ForeColor = Ui.Green;

                Add(x, y, "立即同步", true, delegate { DoSync(); }); x += Ui.Px(116) + Ui.Px(10);
                Add(x, y, "检查更新", false, delegate { DoCheckUpdate(); }); x += Ui.Px(116) + Ui.Px(10);
                Add(x, y, "退出登录", false, delegate { DoLogout(); });
            }
            else
            {
                _status.Text = "未登录";
                _status.ForeColor = Ui.Sub;
                Add(x, y, "登录 GitHub", true, delegate { DoLogin(); });
            }
        }

        void Add(int x, int y, string text, bool primary, EventHandler onClick)
        {
            var p = new Pill();
            p.Text = text; p.Primary = primary;
            p.SetBounds(x, y, Ui.Px(116), Ui.Px(32));
            p.Click += onClick;
            _buttons.Controls.Add(p);
        }

        void SetLog(string s)
        {
            _log.Text = s;
            _log.ForeColor = Ui.Sub;
        }

        void RefreshProxy()
        {
            string p = Store.Proxy;
            _proxy.Text = p.Length == 0
                ? "网络代理：未设置（连不上 GitHub 时点这里）▸"
                : "网络代理：" + p + "  （点击修改）▸";
        }

        void SetBusy(bool on)
        {
            _working = on;
            if (on) { _dots = 0; _busy.Start(); }
            else { _busy.Stop(); Rebuild(); }
            foreach (Control c in _buttons.Controls)
                if (c is Pill && ((Pill)c).Text != "关闭") c.Enabled = !on;
        }

        static string Brief(Exception e)
        {
            string m = e.Message;
            if (m.Contains("SSL/TLS") || m.Contains("SSL/TLS 安全通道"))
                m += "（TLS 握手失败，多为网络/代理问题）";
            if (m.Contains("超时") || m.Contains("timed out") || m.Contains("无法连接")
                || m.Contains("远程服务器") || m.Contains("no such host") || m.Contains("名称")
                || m.Contains("network") || m.Contains("Network"))
                m += "　→　如果是内地网络，先点下面的「网络代理」配一个。";
            return m;
        }

        // ============================================================== 登录
        void DoLogin()
        {
            var dlg = new Form();
            dlg.Text = "登录 GitHub";
            dlg.FormBorderStyle = FormBorderStyle.FixedDialog;
            dlg.StartPosition = FormStartPosition.CenterParent;
            dlg.ClientSize = new Size(Ui.Px(460), Ui.Px(210));
            dlg.MaximizeBox = false; dlg.MinimizeBox = false;
            dlg.BackColor = Color.White;

            var lbl = new Label();
            lbl.Text = "正在向 GitHub 申请登录码…";
            lbl.Font = Ui.F(10f);
            lbl.SetBounds(Ui.Px(20), Ui.Px(18), Ui.Px(420), Ui.Px(130));
            dlg.Controls.Add(lbl);

            var open = new Pill();
            open.Text = "打开浏览器"; open.Primary = true;
            open.SetBounds(Ui.Px(20), Ui.Px(158), Ui.Px(128), Ui.Px(32));
            open.Enabled = false;
            dlg.Controls.Add(open);

            var cancel = new Pill();
            cancel.Text = "取消";
            cancel.SetBounds(Ui.Px(158), Ui.Px(158), Ui.Px(90), Ui.Px(32));
            dlg.Controls.Add(cancel);

            bool stop = false;
            cancel.Click += delegate { stop = true; dlg.Close(); };

            var worker = new System.ComponentModel.BackgroundWorker();
            worker.DoWork += delegate
            {
                try
                {
                    var dc = GitHub.DeviceStart();
                    dlg.BeginInvoke((MethodInvoker)delegate
                    {
                        lbl.Text = "在浏览器里完成授权\n\n"
                                 + "代码：  " + dc.UserCode + "\n\n"
                                 + "已复制到剪贴板。请在打开的页面粘贴它，再点绿色的 Authorize。\n"
                                 + "没自动打开就手动访问 " + dc.VerifyUrl;
                        try { Clipboard.SetText(dc.UserCode); } catch { }
                        open.Enabled = true;
                        open.Click += delegate { OpenUrl(dc.VerifyUrl); };
                        OpenUrl(dc.VerifyUrl);
                    });

                    string token = GitHub.DevicePoll(dc, delegate { return stop; });
                    // 必须用刚拿到的 token 查用户名 —— 这时 GitHub.Token 还是空的
                    string user = GitHub.CurrentUser(token);
                    GitHub.SaveToken(token, user);
                    dlg.BeginInvoke((MethodInvoker)delegate
                    {
                        if (!stop) dlg.Close();
                        SetLog("登录成功。可以点「立即同步」把两边的打卡记录合并。");
                        Rebuild();
                        MainForm.NotifyHeaderChanged();
                    });
                }
                catch (Exception ex)
                {
                    dlg.BeginInvoke((MethodInvoker)delegate
                    {
                        if (!stop) dlg.Close();
                        SetLog("登录失败：" + Brief(ex));
                        Rebuild();
                    });
                }
            };
            worker.RunWorkerAsync();
            dlg.ShowDialog(this);
            stop = true;
        }

        static void OpenUrl(string url)
        {
            try { System.Diagnostics.Process.Start(url); } catch { }
        }

        void DoLogout()
        {
            if (MessageBox.Show(this, "退出后无法同步打卡记录，也不能检查更新。\n已经同步过的记录不受影响。",
                    "退出 GitHub 登录？", MessageBoxButtons.OKCancel) != DialogResult.OK) return;
            GitHub.Logout();
            SetLog("已退出登录。");
            Rebuild();
            MainForm.NotifyHeaderChanged();
        }

        // ============================================================== 同步
        void DoSync()
        {
            SetBusy(true);
            _status.Text = "正在同步";
            var worker = new System.ComponentModel.BackgroundWorker();
            worker.DoWork += delegate
            {
                try
                {
                    var r = GitHub.Sync();
                    BeginInvoke((MethodInvoker)delegate
                    {
                        SetBusy(false);
                        SetLog("同步完成　从云端新增 " + r.Pulled + " 条　合计 " + r.Total + " 条\n"
                             + (r.Uploaded ? "已把本地记录上传到仓库。" : "云端已是最新，无需上传。")
                             + "\nCSV：" + (r.CsvUploaded ? "已更新 sync/StudyRecord.csv" : "云端已是最新"));
                        MainForm.NotifyDataChanged();
                    });
                }
                catch (Exception ex)
                {
                    BeginInvoke((MethodInvoker)delegate
                    {
                        SetBusy(false);
                        SetLog("同步失败：" + Brief(ex));
                    });
                }
            };
            worker.RunWorkerAsync();
        }

        // ============================================================== 更新
        void DoCheckUpdate()
        {
            SetBusy(true);
            _status.Text = "正在检查更新";
            var worker = new System.ComponentModel.BackgroundWorker();
            worker.DoWork += delegate
            {
                try
                {
                    var rel = GitHub.LatestRelease();
                    BeginInvoke((MethodInvoker)delegate
                    {
                        SetBusy(false);
                        if (string.Equals(rel.Tag, GitHub.VersionTag, StringComparison.OrdinalIgnoreCase))
                        {
                            SetLog("已是最新版本。\n当前：" + GitHub.VersionTag + "　最新：" + rel.Tag);
                            return;
                        }
                        if (string.IsNullOrEmpty(rel.ExeDownload(GitHub.LoggedIn)))
                        {
                            SetLog("最新版是 " + rel.Tag + "，但这个 Release 里没有 exe 附件。");
                            return;
                        }

                        string notes = rel.Notes == null ? "" : rel.Notes;
                        if (notes.Length > 420) notes = notes.Substring(0, 420) + "…";
                        var ans = MessageBox.Show(this,
                            "发现新版本 " + rel.Tag + "\n当前：" + GitHub.VersionTag
                            + "\n大小：" + (rel.ExeSize / 1024) + " KB\n\n" + notes
                            + "\n\n现在下载并安装吗？",
                            "发现新版本", MessageBoxButtons.OKCancel);
                        if (ans == DialogResult.OK) DownloadAndReplace(rel);
                    });
                }
                catch (Exception ex)
                {
                    BeginInvoke((MethodInvoker)delegate
                    {
                        SetBusy(false);
                        SetLog("检查更新失败：" + Brief(ex));
                    });
                }
            };
            worker.RunWorkerAsync();
        }

        void DownloadAndReplace(GitHub.Release rel)
        {
            string tmp = Path.Combine(Path.GetTempPath(), "StudyCompanion-" + rel.Tag + ".exe");
            var prog = new Form();
            prog.Text = "正在下载新版本";
            prog.FormBorderStyle = FormBorderStyle.FixedDialog;
            prog.StartPosition = FormStartPosition.CenterParent;
            prog.ClientSize = new Size(Ui.Px(380), Ui.Px(100));
            prog.MaximizeBox = false; prog.MinimizeBox = false; prog.ControlBox = false;
            prog.BackColor = Color.White;
            var lbl = new Label();
            lbl.Text = "准备下载…";
            lbl.Font = Ui.F(10f);
            lbl.SetBounds(Ui.Px(20), Ui.Px(32), Ui.Px(340), Ui.Px(40));
            prog.Controls.Add(lbl);

            var worker = new System.ComponentModel.BackgroundWorker();
            worker.DoWork += delegate
            {
                try
                {
                    GitHub.Download(rel.ExeDownload(GitHub.LoggedIn), tmp, delegate (long got, long total)
                    {
                        int pct = total > 0 ? (int)(got * 100 / total) : -1;
                        try
                        {
                            prog.BeginInvoke((MethodInvoker)delegate
                            {
                                lbl.Text = pct >= 0
                                    ? ("已下载 " + (got / 1024) + " / " + (total / 1024) + " KB  (" + pct + "%)")
                                    : ("已下载 " + (got / 1024) + " KB");
                            });
                        }
                        catch { }
                    });
                    prog.BeginInvoke((MethodInvoker)delegate { prog.Close(); ReplaceSelf(tmp); });
                }
                catch (Exception ex)
                {
                    prog.BeginInvoke((MethodInvoker)delegate
                    {
                        prog.Close();
                        SetLog("下载失败：" + Brief(ex));
                    });
                }
            };
            worker.RunWorkerAsync();
            prog.ShowDialog(this);
        }

        /// <summary>
        /// 正在运行的 exe 不能被覆盖，所以写一个批处理：
        /// 等本进程退出 → 覆盖 → 重新启动。
        /// </summary>
        void ReplaceSelf(string newExe)
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

                var psi = new System.Diagnostics.ProcessStartInfo("cmd.exe", "/c \"" + bat + "\"");
                psi.WindowStyle = System.Diagnostics.ProcessWindowStyle.Hidden;
                psi.UseShellExecute = true;
                System.Diagnostics.Process.Start(psi);

                MainForm.ForceQuit();
            }
            catch (Exception ex)
            {
                SetLog("自动替换失败：" + GitHub.Friendly(ex));
            }
        }
    }
}
