using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>右下角提醒浮窗</summary>
    public class ToastForm : Form
    {
        readonly Timer _tick = new Timer();
        readonly Timer _slide = new Timer();
        int _targetX, _remain;
        readonly string _title, _body;
        readonly EventHandler _onAction;
        readonly bool _countdown;
        readonly Pill _btnA, _btnB;

        public ToastForm(string title, string body, int seconds, string actionText,
                         EventHandler onAction, bool countdown)
        {
            _title = title; _body = body;
            _remain = Math.Max(1, seconds);
            _countdown = countdown;
            _onAction = onAction;

            FormBorderStyle = FormBorderStyle.None;
            ShowInTaskbar = false;
            TopMost = true;
            StartPosition = FormStartPosition.Manual;
            BackColor = Ui.Bg;
            Ui.EnableDoubleBuffer(this);

            bool hasAction = !string.IsNullOrEmpty(actionText);
            int btnY = hasAction ? Ui.Px(118) : Ui.Px(96);
            ClientSize = new Size(Ui.Px(400), hasAction ? Ui.Px(168) : Ui.Px(146));

            var area = Screen.PrimaryScreen.WorkingArea;
            _targetX = area.Right - Width - Ui.Px(16);
            Location = new Point(area.Right + Ui.Px(10), area.Bottom - Height - Ui.Px(16));

            if (hasAction)
            {
                _btnA = new Pill();
                _btnA.Text = actionText; _btnA.Primary = true;
                _btnA.Click += delegate
                {
                    if (_onAction != null) _onAction(this, EventArgs.Empty);
                    Close();
                };
                Controls.Add(_btnA);
                _btnA.Bounds = new Rectangle(Ui.Px(20), btnY, Ui.Px(120), Ui.Px(32));
            }

            _btnB = new Pill();
            _btnB.Text = countdown ? DismissText() : "知道了";
            _btnB.Click += delegate { Close(); };
            Controls.Add(_btnB);

            // 倒计时的按钮要按最宽的数字预留宽度，避免数字变化时抖动
            int bw = countdown
                ? Math.Max(Ui.Px(96), Pill.Measure("知道了（88）", Ui.F(9f)) + Ui.Px(14))
                : Ui.Px(88);
            _btnB.Bounds = new Rectangle(hasAction ? Ui.Px(150) : Ui.Px(20), btnY, bw, Ui.Px(32));

            _slide.Interval = 12;
            _slide.Tick += delegate
            {
                if (Left > _targetX)
                {
                    Left -= Math.Max(Ui.Px(12), (Left - _targetX) / 4);
                    if (Left <= _targetX) { Left = _targetX; _slide.Stop(); }
                }
                else _slide.Stop();
            };
            _slide.Start();

            // 一秒一跳：既负责倒计时显示，也负责到点自动关闭
            _tick.Interval = 1000;
            _tick.Tick += delegate
            {
                _remain--;
                if (_remain <= 0) { _tick.Stop(); Close(); return; }
                if (_countdown)
                {
                    _btnB.Text = DismissText();
                    _btnB.Invalidate();
                }
            };
            _tick.Start();

            Paint += ToastPaint;
        }

        string DismissText() { return "知道了（" + _remain + "）"; }

        void ToastPaint(object sender, PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            var r = new Rectangle(0, 0, Width, Height);
            Ui.FillRound(g, r, Ui.Px(14), Color.White);
            // 左侧强调条
            Ui.FillRound(g, new Rectangle(0, Ui.Px(14), Ui.Px(6), Height - Ui.Px(28)), Ui.Px(3), Ui.Accent);

            Ui.Text(g, _title, Ui.F(12f, true), Ui.Ink, Ui.Px(22), Ui.Px(16));
            var f = Ui.F(10f);
            int y = Ui.Px(46);
            foreach (var line in Wrap(g, _body, f, Width - Ui.Px(44)))
            {
                Ui.Text(g, line, f, Ui.Sub, Ui.Px(22), y);
                y += Ui.Px(22);
                if (y > Ui.Px(104)) break;
            }
            using (var p = new Pen(Color.FromArgb(18, 0, 0, 0), 1))
                g.DrawRectangle(p, 0, 0, Width - 1, Height - 1);
        }

        static string[] Wrap(Graphics g, string s, Font f, int maxW)
        {
            var parts = new System.Collections.Generic.List<string>();
            string cur = "";
            foreach (char ch in s)
            {
                if (ch == '\n') { parts.Add(cur); cur = ""; continue; }
                if (g.MeasureString(cur + ch, f).Width > maxW && cur.Length > 0) { parts.Add(cur); cur = ""; }
                cur += ch;
            }
            parts.Add(cur);
            return parts.ToArray();
        }
    }

    /// <summary>托盘 + 提醒调度</summary>
    public class TrayApp : ApplicationContext
    {
        readonly NotifyIcon _tray;
        readonly Timer _timer = new Timer();
        MainForm _form;
        static TrayApp _self;
        bool _quitting;
        readonly System.Collections.Generic.HashSet<string> _notified = new System.Collections.Generic.HashSet<string>();
        readonly System.Collections.Generic.HashSet<string> _notifiedLate = new System.Collections.Generic.HashSet<string>();

        public TrayApp(bool startMinimized)
        {
            _self = this;
            _form = new MainForm(startMinimized);
            _form.FormClosed += OnFormClosed;

            _tray = new NotifyIcon();
            try { _tray.Icon = AppIcon.Get(16); } catch { _tray.Icon = SystemIcons.Application; }
            _tray.Text = "学习助手 · NCUK IFY";
            _tray.Visible = true;
            _tray.DoubleClick += delegate { ShowForm(); };

            var menu = new ContextMenuStrip();
            menu.Items.Add("打开主界面", null, delegate { ShowForm(); });
            menu.Items.Add("今天的学习内容", null, delegate { ShowForm(); });
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("重新读取课表", null, delegate
            { ScheduleData.Load(true); if (_form != null) _form.RefreshAll(); Toast("已重新读取", "课表已刷新。", 20); });
            var miAuto = new ToolStripMenuItem("开机自动启动");
            miAuto.CheckOnClick = true;
            miAuto.Checked = Store.AutoStartEnabled;
            miAuto.Click += delegate { Store.SetAutoStart(miAuto.Checked); };
            menu.Items.Add(miAuto);
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("退出", null, delegate { Quit(); });
            _tray.ContextMenuStrip = menu;

            _timer.Interval = 15000;
            _timer.Tick += delegate { Check(); };
            _timer.Start();

            if (!startMinimized) ShowForm();
            Check(true);
        }

        public void ShowForm()
        {
            if (_form == null || _form.IsDisposed)
            {
                _form = new MainForm(false);
                _form.FormClosed += OnFormClosed;
            }
            if (_form.WindowState == FormWindowState.Minimized)
                _form.WindowState = FormWindowState.Normal;
            _form.ShowInTaskbar = true;      // 赋值会重建句柄，放在 Show() 之前
            _form.Opacity = 1;
            _form.Show();
            _form.Activate();
            _form.BringToFront();
        }

        /// <summary>主窗口真的被关闭了（用户选了「直接退出」）→ 结束整个程序</summary>
        void OnFormClosed(object sender, FormClosedEventArgs e)
        {
            Quit(closeForm: false);
        }

        /// <summary>退出程序。closeForm=false 用于「窗口已经关了」的场合，避免重复 Close()</summary>
        void Quit(bool closeForm = true)
        {
            if (_quitting) return;
            _quitting = true;
            try { _timer.Stop(); } catch { }
            if (_tray != null) _tray.Visible = false;

            var f = _form;
            if (closeForm && f != null && !f.IsDisposed)
            {
                f.ForceClose = true;
                try { f.Close(); } catch { }
            }
            ExitThread();
        }

        // ------------------------------------------------------------- 提醒
        void Check(bool startup = false)
        {
            var plan = ScheduleData.GetDay(DateTime.Today);
            if (plan == null) return;
            DateTime now = DateTime.Now;

            foreach (var s in plan.Slots)
            {
                string key = s.Key;
                bool done = Store.IsDone(DateTime.Today, s);

                // 开始提醒
                if (!_notified.Contains(key) && now >= s.StartToday && now < s.StartToday.AddMinutes(3))
                {
                    _notified.Add(key);
                    Notify("该学习啦！　" + s.Start + "–" + s.End,
                           s.Subject + "　" + s.Title + "\n点「开始学习」查看今天的任务和配套视频。");
                }
                // 启动时正处于时段中
                else if (startup && !_notified.Contains(key) && now >= s.StartToday.AddMinutes(3) && now < s.EndToday && !done)
                {
                    _notified.Add(key);
                    Notify("正在进行：" + s.Start + "–" + s.End,
                           s.Subject + "　" + s.Title + "\n剩余 " + (int)(s.EndToday - now).TotalMinutes + " 分钟。");
                }

                // 结束前 5 分钟
                if (!done && !_notifiedLate.Contains(key) &&
                    now >= s.EndToday.AddMinutes(-5) && now < s.EndToday)
                {
                    _notifiedLate.Add(key);
                    Notify("还有 5 分钟：" + s.Subject,
                           "本时段 " + s.End + " 结束，完成后记得点「标记完成」。");
                }
            }
        }

        void Notify(string title, string body)
        {
            Toast(title, body, 120);
            try { _tray.ShowBalloonTip(8000, title, body.Replace("\n", " "), ToolTipIcon.Info); }
            catch { }
            System.Media.SystemSounds.Asterisk.Play();
        }

        /// <summary>右下角浮窗。actionText 传空串则只显示一个「知道了」按钮（可带倒计时）</summary>
        public static void Toast(string title, string body, int seconds,
                                 string actionText = "开始学习", bool countdown = false)
        {
            if (SuppressToast) return;
            try
            {
                var t = new ToastForm(title, body, seconds, actionText,
                    delegate { if (_self != null) _self.ShowForm(); }, countdown);
                t.Show();
            }
            catch { }
        }

        public static TrayApp Instance { get { return _self; } }

        /// <summary>自检时抑制弹窗，避免干扰</summary>
        public static bool SuppressToast = false;
    }
}
