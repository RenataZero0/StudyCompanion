using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 内置的更新日志阅读器（自绘 Markdown，不依赖系统对 .md 的文件关联）。
    ///
    /// 打开时会自动从 GitHub 拉一次最新的 CHANGELOG.md：
    /// 拉到了就用最新的（同时写入缓存），拉不到就显示缓存/内置副本，并说明原因。
    ///
    /// 日志很长，所以**按版本分页**：标题下面一排版本标签，点哪个看哪个；
    /// 第一个「全部」显示完整日志。
    /// </summary>
    public class DocViewer : Form
    {
        /// <summary>一个版本一节</summary>
        class Sec
        {
            public string Label;   // "全部" 或 "v2.1.2"
            public string Md;      // 这一节要渲染的 markdown
        }

        readonly MarkdownView _view = new MarkdownView();
        readonly Label _status = new Label();
        readonly Pill _refresh = new Pill();
        readonly Panel _chips = new Panel();
        readonly List<Pill> _chipCtl = new List<Pill>();

        readonly List<Sec> _sections = new List<Sec>();
        int _sel;

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
            ClientSize = new Size(Ui.Px(800), Ui.Px(720));
            MinimumSize = new Size(Ui.Px(580), Ui.Px(480));
            Font = Ui.F(9f);
            AutoScaleMode = AutoScaleMode.None;
            ShowInTaskbar = false;
            try { Icon = AppIcon.Get(32); } catch { }

            // ---------------- 顶栏 ----------------
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

            // ---------------- 版本标签行 ----------------
            _chips.Dock = DockStyle.Top;
            _chips.Height = Ui.Px(54);   // 含横向滚动条
            _chips.BackColor = Color.White;
            _chips.AutoScroll = true;
            _chips.Paint += delegate (object s, PaintEventArgs e)
            {
                using (var p = new Pen(Ui.Line, 1))
                    e.Graphics.DrawLine(p, 0, _chips.Height - 1, _chips.Width, _chips.Height - 1);
            };
            Controls.Add(_chips);

            // ---------------- 内容 ----------------
            var host = new Panel();
            host.Dock = DockStyle.Fill;
            host.Padding = new Padding(Ui.Px(16), Ui.Px(12), Ui.Px(16), Ui.Px(16));
            host.BackColor = Ui.Bg;
            Controls.Add(host);
            // WinForms 的 Dock 顺序按 z-order 反着来：index 越大越先 Dock（越靠外）。
            // host 拉到最前（index 0，填满剩余），head 丢到最后（index 最大，贴最上面），
            // 标签行自然就夹在中间。
            host.BringToFront();
            head.SendToBack();

            var card = new Card();
            card.Dock = DockStyle.Fill;
            card.Radius = Ui.Px(14);
            host.Controls.Add(card);
            card.Padding = new Padding(Ui.Px(4));
            _view.Dock = DockStyle.Fill;
            card.Controls.Add(_view);

            BuildSections(markdown ?? Changelog.Local());
            _sel = 0;
            if (_sections.Count > 1) _sel = 1;
            RebuildChips();
            RenderCurrent();

            Shown += delegate { if (_autoFetch && !_fetchedOnce) FetchAsync(); };
        }

        // ================================================================ 分页
        /// <summary>
        /// 按 "## 标题" 把日志切成若干节。第一项固定是「全部」，其余每个版本一项。
        /// </summary>
        void BuildSections(string src)
        {
            _sections.Clear();
            _sections.Add(new Sec { Label = "全部", Md = src });
            _sel = 0;
            if (string.IsNullOrEmpty(src)) return;

            var cur = new StringBuilder();
            string curTitle = null;
            bool started = false;

            foreach (var raw in src.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n'))
            {
                string t = raw.Trim();
                if (t.StartsWith("## ") && !t.StartsWith("### "))
                {
                    if (started) AddSection(curTitle, cur.ToString());
                    cur.Length = 0;
                    curTitle = t.Substring(3).Trim();
                    cur.AppendLine(raw);
                    started = true;
                }
                else if (started)
                {
                    cur.AppendLine(raw);
                }
            }
            if (started) AddSection(curTitle, cur.ToString());

            // 默认选中最新那一版（第 0 项是「全部」）
            _sel = _sections.Count > 1 ? 1 : 0;
        }

        void AddSection(string title, string body)
        {
            if (string.IsNullOrEmpty(title)) return;
            // 标题形如 "v2.1.2 —— 列表续行…"，标签上只取版本号
            int sp = title.IndexOf(' ');
            string label = sp > 0 ? title.Substring(0, sp) : title;
            _sections.Add(new Sec { Label = label, Md = body });
        }

        void RebuildChips()
        {
            foreach (var c in _chipCtl) { _chips.Controls.Remove(c); c.Dispose(); }
            _chipCtl.Clear();

            int x = Ui.Px(18), y = Ui.Px(8), h = Ui.Px(28);
            for (int i = 0; i < _sections.Count; i++)
            {
                var p = new Pill();
                p.Text = _sections[i].Label;
                p.Font = Ui.F(9f, true);
                int w = Math.Max(Ui.Px(58), Pill.Measure(p.Text, p.Font));
                p.SetBounds(x, y, w, h);
                p.Primary = (i == _sel);
                int idx = i;
                p.Click += delegate { SelectSection(idx); };
                _chips.Controls.Add(p);
                _chipCtl.Add(p);
                x += w + Ui.Px(8);
            }
        }

        void SelectSection(int i)
        {
            if (i < 0 || i >= _sections.Count || i == _sel) return;
            _sel = i;
            RenderCurrent();
            for (int k = 0; k < _chipCtl.Count; k++)
            {
                _chipCtl[k].Primary = (k == _sel);
                _chipCtl[k].Invalidate();
            }
            try { if (i < _chipCtl.Count) _chips.ScrollControlIntoView(_chipCtl[i]); }
            catch { }
        }

        void RenderCurrent()
        {
            if (_sections.Count == 0) return;
            SetContent(_sections[_sel].Md);
            UpdateStatus(true);
        }

        // ================================================================ 内容
        void SetContent(string md)
        {
            _md = string.IsNullOrEmpty(md) ? "（还没有更新日志内容）" : md;
            _view.SetMarkdown(_md);
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
                string keep = _sections.Count > 0 ? _sections[_sel].Label : "";
                BuildSections(Changelog.Local());
                // 尽量停在原来那一版
                for (int i = 1; i < _sections.Count; i++)
                {
                    if (_sections[i].Label == keep) { _sel = i; break; }
                }
                if (_sel == 0 && _sections.Count > 1) _sel = 1;
                RebuildChips();
                RenderCurrent();

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
