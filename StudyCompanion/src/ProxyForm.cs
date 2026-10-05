using System;
using System.Drawing;
using System.Net;
using System.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 「网络代理」设置窗口。
    ///
    /// 为什么需要它：程序本身**不能翻墙**，它只会发普通的 HTTPS 请求。
    /// 在内地直连 github.com 往往不通，于是同步 / 检查更新 / 下载全都会失败。
    /// 这个窗口让用户把自己已有的代理（Clash、v2ray 之类）填进来，
    /// 之后所有请求都从那儿走。
    /// </summary>
    public class ProxyForm : Form
    {
        TextBox _box;
        Label _state;
        Pill _save, _clear, _test, _close;

        public ProxyForm()
        {
            Text = "网络代理";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterParent;
            BackColor = Ui.Bg;
            Font = Ui.F(10f);
            ClientSize = new Size(Ui.Px(560), Ui.Px(400));

            var title = new Label();
            title.Text = "网络代理";
            title.Font = Ui.F(13f, true);
            title.ForeColor = Ui.Ink;
            title.AutoSize = false;
            title.Location = new Point(Ui.Px(20), Ui.Px(16));
            title.Size = new Size(Ui.Px(400), Ui.Px(30));
            Controls.Add(title);

            var help = new Label();
            help.Text =
                "填一个代理，之后「同步 / 检查更新 / 下载」都会从它走。\r\n" +
                "\r\n" +
                "格式：主机:端口　　例如 127.0.0.1:7890（Clash 默认端口）\r\n" +
                "留空 = 不用代理，改用系统设置里的代理。\r\n" +
                "\r\n" +
                "提示：本程序自己不能翻墙，需要你已经有能用的代理。\r\n" +
                "如果你在电脑上开了 Clash，又想给平板用，\r\n" +
                "把 Clash 的「允许局域网连接」打开，然后填这台电脑的 IP，\r\n" +
                "例如 192.168.1.5:7890。";
            help.Font = Ui.F(9.5f);
            help.ForeColor = Ui.Sub;
            help.AutoSize = false;
            help.Location = new Point(Ui.Px(20), Ui.Px(50));
            help.Size = new Size(Ui.Px(520), Ui.Px(150));
            Controls.Add(help);

            var cap = new Label();
            cap.Text = "代理地址";
            cap.ForeColor = Ui.Ink;
            cap.AutoSize = false;
            cap.Location = new Point(Ui.Px(20), Ui.Px(206));
            cap.Size = new Size(Ui.Px(120), Ui.Px(22));
            Controls.Add(cap);

            _box = new TextBox();
            _box.Font = Ui.F(11f);
            _box.Location = new Point(Ui.Px(20), Ui.Px(230));
            _box.Size = new Size(Ui.Px(520), Ui.Px(28));
            _box.Text = Store.Proxy;
            Controls.Add(_box);

            _state = new Label();
            _state.AutoSize = false;
            _state.Font = Ui.F(9f);
            _state.ForeColor = Ui.Sub;
            _state.Location = new Point(Ui.Px(20), Ui.Px(262));
            _state.Size = new Size(Ui.Px(520), Ui.Px(42));
            Controls.Add(_state);

            _save = new Pill();
            _save.Text = "保存";
            _save.Primary = true;
            _save.SetBounds(Ui.Px(20), Ui.Px(312), Ui.Px(90), Ui.Px(34));
            _save.Click += delegate { Save(true); };
            Controls.Add(_save);

            _test = new Pill();
            _test.Text = "测试连接";
            _test.SetBounds(Ui.Px(122), Ui.Px(312), Ui.Px(110), Ui.Px(34));
            _test.Click += delegate { Save(false); TcpTest(); };
            Controls.Add(_test);

            _clear = new Pill();
            _clear.Text = "清除";
            _clear.SetBounds(Ui.Px(244), Ui.Px(312), Ui.Px(70), Ui.Px(34));
            _clear.Click += delegate
            {
                _box.Text = "";
                Store.Proxy = "";
                Say("已清除，改用系统设置里的代理。", Ui.Sub);
            };
            Controls.Add(_clear);

            _close = new Pill();
            _close.Text = "关闭";
            _close.SetBounds(Ui.Px(326), Ui.Px(312), Ui.Px(70), Ui.Px(34));
            _close.Click += delegate { Close(); };
            Controls.Add(_close);

            ShowState();
        }

        void ShowState()
        {
            string p = Store.Proxy;
            Say(p.Length == 0 ? "当前：不使用代理。" : "当前：使用 " + p, Ui.Sub);
        }

        void Say(string s, Color c)
        {
            _state.Text = s;
            _state.ForeColor = c;
        }

        void Save(bool announce)
        {
            string raw = _box.Text == null ? "" : _box.Text.Trim();
            string norm = Store.NormalizeProxy(raw);
            if (raw.Length > 0 && norm.Length == 0)
            {
                Say("格式不对，应该像 127.0.0.1:7890 这样（要带端口号）。", Ui.Red);
                return;
            }
            Store.Proxy = norm;
            _box.Text = norm;
            if (announce)
            {
                if (norm.Length == 0) Say("已保存：不使用代理。", Ui.Green);
                else Say("已保存：" + norm + "。下次同步就走它了。", Ui.Green);
            }
        }

        /// <summary>
        /// 只做一次真实的 GitHub 请求来验证代理是否可用。
        /// 之所以不在这里开线程：这点等待（最多 12 秒）用户可以接受，
        /// 而且用 Application.DoEvents 已经能让界面不假死。
        /// </summary>
        void TcpTest()
        {
            string p = Store.Proxy;
            if (p.Length == 0)
            {
                Say("没有填代理，直接测直连……", Ui.Sub);
            }
            else
            {
                // 先测端口通不通，比直接发 HTTPS 快得多，报错也更好懂
                try
                {
                    int c = p.LastIndexOf(':');
                    string host = p.Substring(0, c);
                    int port = int.Parse(p.Substring(c + 1));
                    using (var t = new System.Net.Sockets.TcpClient())
                    {
                        var ar = t.BeginConnect(host, port, null, null);
                        if (!ar.AsyncWaitHandle.WaitOne(3000))
                        {
                            Say("连不上 " + p + "（3 秒超时）。\r\n"
                                + "请确认代理软件正在运行，并且允许了「局域网连接」。", Ui.Red);
                            return;
                        }
                        t.EndConnect(ar);
                    }
                    Say("端口 " + p + " 通了，正在试访问 GitHub……", Ui.Sub);
                }
                catch (Exception ex)
                {
                    Say("连不上 " + p + "：" + Brief(ex.Message), Ui.Red);
                    return;
                }
            }

            Application.DoEvents();
            _test.Enabled = false;
            try
            {
                var r = (HttpWebRequest)WebRequest.Create("https://api.github.com/rate_limit");
                r.Method = "GET";
                r.Timeout = 12000;
                r.ReadWriteTimeout = 12000;
                r.UserAgent = "StudyCompanion-Windows";
                r.Accept = "application/vnd.github+json";
                GitHub.ApplyProxy(r);
                using (var resp = (HttpWebResponse)r.GetResponse())
                using (var s = resp.GetResponseStream())
                using (var sr = new System.IO.StreamReader(s, Encoding.UTF8))
                    sr.ReadToEnd();

                Say("连接成功！GitHub 可以访问了。", Ui.Green);
                if (MessageBox.Show(this, "连接成功，现在就把云端记录同步一下？", "网络代理",
                        MessageBoxButtons.YesNo) == DialogResult.Yes)
                {
                    if (MainForm.Instance != null) MainForm.Instance.SyncNow();
                }
            }
            catch (Exception ex)
            {
                Say("还是连不上 GitHub：" + Brief(GitHub.Friendly(ex)), Ui.Red);
            }
            finally { _test.Enabled = true; }
        }

        static string Brief(string s)
        {
            if (string.IsNullOrEmpty(s)) return "未知错误";
            s = s.Replace("\r", " ").Replace("\n", " ").Trim();
            return s.Length > 80 ? s.Substring(0, 80) + "…" : s;
        }

        protected override void OnShown(EventArgs e)
        {
            base.OnShown(e);
            _box.Focus();
            _box.SelectAll();
        }
    }
}
