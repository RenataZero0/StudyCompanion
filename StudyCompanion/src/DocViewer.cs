using System;
using System.ComponentModel;
using System.Drawing;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 内置的更新日志阅读器（自绘 Markdown，不依赖系统对 .md 的文件关联）。
    ///
    /// 打开时会自动从 GitHub 拉一次最新的 CHANGELOG.md：
    /// 拉到了就用最新的（同时写入缓存），拉不到就显示缓存/内置副本，并说明原因。
    /// 所以即使程序本身是旧版本，也能看到新版本改了什么。
    /// </summary>
    public class DocViewer : Form
    {
        readonly MarkdownView _view = new MarkdownView();
        readonly Label _status = new Label();
        readonly Pill _refresh = new Pill();

        /// <summary>自检用：正文长度</summary>
        public int BodyLength { get { return _md == null ? 0 : _md.Length; } }
        string _md = "";
        bool _busy;
        bool _fetchedOnce;

        readonly bool _autoFetch = true;

        public DocViewer(string title) : this(title, null) { }

        public DocViewer(string title, string markdown) : this(title, markdown, true) { }

        /// <summary>autoFetch=false 时不会自动联网（离线预览 / 测试渲染用）</summary>
        public DocViewer(string title, string markdown, bool autoFetch)
        {
            _autoFetch = autoFetch;
            Text = title;
            BackColor = Ui.Bg;
            StartPosition = FormStartPosition.CenterParent;
            ClientSize = new Size(Ui.Px(780), Ui.Px(680));
            MinimumSize = new Size(Ui.Px(560), Ui.Px(440));
            Font = Ui.F(9f);
            AutoScaleMode = AutoScaleMode.None;
            ShowInTaskbar = false;
            try { Icon = AppIcon.Get(32); } catch { }

            var head = new Panel();
            head.Dock = DockStyle.Top;
            head.Height = Ui.Px(62);
            head.BackColor = Color.White;
            Controls.Add(head);

            var lab = new Label();
            // 顶栏里只写「更新日志」——窗口标题栏已经有全名了，写全名会和右边的状态文字撞上
            lab.Text = "更新日志";
            lab.Font = Ui.F(12f, true);
            lab.ForeColor = Ui.Ink;
            lab.AutoSize = false;
            lab.TextAlign = ContentAlignment.MiddleLeft;
            lab.BackColor = Color.White;
            lab.SetBounds(Ui.Px(22), 0, Ui.Px(140), head.Height);
            head.Controls.Add(lab);

            // 状态靠右对齐（贴着「刷新」按钮），文字要短，否则会和标题撞上
            _status.AutoSize = false;
            _status.TextAlign = ContentAlignment.MiddleRight;
            _status.ForeColor = Ui.Sub;
            _status.Font = Ui.F(8.5f);
            _status.BackColor = Color.White;
            _status.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            _status.SetBounds(ClientSize.Width - Ui.Px(220) - Ui.Px(360), 0, Ui.Px(360), head.Height);
            head.Controls.Add(_status);

            var close = new Pill();
            close.Text = "关闭";
            close.Primary = true;
            close.Click += delegate { Close(); };
            close.Bounds = new Rectangle(head.Width - Ui.Px(104), Ui.Px(17), Ui.Px(80), Ui.Px(28));
            close.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            head.Controls.Add(close);
            close.BringToFront();

            _refresh.Text = "刷新";
            _refresh.Click += delegate { FetchAsync(); };
            _refresh.Bounds = new Rectangle(head.Width - Ui.Px(196), Ui.Px(17), Ui.Px(80), Ui.Px(28));
            _refresh.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            head.Controls.Add(_refresh);
            _refresh.BringToFront();

            var host = new Panel();
            host.Dock = DockStyle.Fill;
            host.Padding = new Padding(Ui.Px(16), Ui.Px(12), Ui.Px(16), Ui.Px(16));
            host.BackColor = Ui.Bg;
            Controls.Add(host);
            host.BringToFront();

            var card = new Card();
            card.Dock = DockStyle.Fill;
            card.Radius = Ui.Px(14);
            host.Controls.Add(card);
            card.Padding = new Padding(Ui.Px(4));
            _view.Dock = DockStyle.Fill;
            card.Controls.Add(_view);

            SetContent(markdown ?? Changelog.Local());
            Shown += delegate { if (_autoFetch && !_fetchedOnce) FetchAsync(); };
        }

        // ================================================================ 内容
        void SetContent(string md)
        {
            if (string.IsNullOrEmpty(md)) md = "（还没有更新日志内容）";
            _md = md;
            _view.SetMarkdown(md);
            UpdateStatus(true);
        }

        void UpdateStatus(bool showSource)
        {
            string latest = Changelog.LatestVersionIn(_md);
            string local = GitHub.VersionTag.TrimStart('v', 'V');
            string src = Changelog.HasCache ? "GitHub" : "内置";

            // 文字要短 —— 这段是右对齐贴在「刷新」按钮左边的
            string s;
            if (latest.Length == 0) s = "来源：" + src;
            else if (string.Equals(latest, local, StringComparison.OrdinalIgnoreCase))
                s = "已是最新（v" + local + "）";
            else
                s = "日志已到 v" + latest + "　本机 v" + local;

            if (showSource && !Changelog.HasCache) s = "未联网　·　" + s;
            _status.Text = s;
        }

        // ================================================================ 拉取
        void FetchAsync()
        {
            if (_busy) return;
            _busy = true;
            _fetchedOnce = true;
            _refresh.Text = "拉取中";
            _refresh.Enabled = false;

            var w = new BackgroundWorker();
            w.DoWork += delegate
            {
                string err;
                bool ok = Changelog.Fetch(out err);
                try { BeginInvoke((MethodInvoker)delegate { OnFetched(ok, err); }); }
                catch { }
            };
            w.RunWorkerAsync();
        }

        void OnFetched(bool ok, string err)
        {
            _busy = false;
            _refresh.Text = "刷新";
            _refresh.Enabled = true;

            if (ok)
            {
                SetContent(Changelog.Local());
                DateTime? t = Changelog.CacheTime;
                _status.Text += t.HasValue ? "　·　" + t.Value.ToString("HH:mm") + " 更新" : "";
            }
            else
            {
                string shortErr = err == null ? "未知原因" : err;
                if (shortErr.Length > 40) shortErr = shortErr.Substring(0, 40) + "…";
                UpdateStatus(true);
                _status.Text = "拉取失败，显示本地副本（" + shortErr + "）";
            }
        }

        /// <summary>自检用：把窗口内容画到位图（MarkdownView 是自绘控件，可正常捕获）</summary>
        public Bitmap Snapshot()
        {
            var b = new Bitmap(Width, Height);
            DrawToBitmap(b, new Rectangle(0, 0, Width, Height));
            return b;
        }
    }
}
