using System;
using System.Collections.Generic;
using System.Text;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace StudyCompanion
{
    // ===================================================================== 顶部栏
    public class HeaderBar : Control
    {
        public DateTime Date = DateTime.Today;
        public int Done, Total;
        public int Streak, Best;
        public double Hours;

        // GitHub 入口（点开独立窗口）
        public bool GhLoggedIn;
        public string GhUser = "";
        public event EventHandler GhClick;
        Rectangle _ghRect = Rectangle.Empty;
        bool _ghHover;

        public HeaderBar()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            bool hot = _ghRect.Contains(e.Location);
            if (hot != _ghHover)
            {
                _ghHover = hot;
                Cursor = hot ? Cursors.Hand : Cursors.Default;
                Invalidate(_ghRect);
            }
            base.OnMouseMove(e);
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            if (_ghHover) { _ghHover = false; Cursor = Cursors.Default; Invalidate(_ghRect); }
            base.OnMouseLeave(e);
        }

        protected override void OnMouseClick(MouseEventArgs e)
        {
            if (_ghRect.Contains(e.Location) && GhClick != null) GhClick(this, EventArgs.Empty);
            base.OnMouseClick(e);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;

            var dp = ScheduleData.GetDay(Date);
            string wd = "周" + "日一二三四五六"[(int)Date.DayOfWeek];
            string title = Date.ToString("M 月 d 日") + "　" + wd;

            int x = Ui.Px(24), y = Ui.Px(16);
            Ui.Text(g, title, Ui.F(17f, true), Ui.Ink, x, y);

            // 徽章
            int tx = x + (int)g.MeasureString(title, Ui.F(17f, true)).Width + Ui.Px(10);
            string badge = dp == null ? "无课表" : dp.DayKind;
            Color bfg = Ui.Accent, bbg = Ui.AccentSoft;
            if (dp != null && dp.IsHoliday) { bfg = Ui.Amber; bbg = Ui.AmberSoft; }
            else if (Date.DayOfWeek == DayOfWeek.Saturday || Date.DayOfWeek == DayOfWeek.Sunday)
            { bfg = Ui.Amber; bbg = Ui.AmberSoft; }
            var bf = Ui.F(9f, true);
            int bw = (int)g.MeasureString(badge, bf).Width + Ui.Px(20);
            var br = new Rectangle(tx, y + Ui.Px(6), bw, Ui.Px(22));
            Ui.FillRound(g, br, Ui.Px(11), bbg);
            Ui.TextC(g, badge, bf, bfg, br);

            // 第二行：学习时长
            string sub = dp == null ? "今天没有安排" :
                (dp.Slots.Count + " 个时段 · 共 " + (dp.TotalMinutes / 60.0).ToString("0.#") + " 小时");
            var fsub = Ui.F(9.5f);
            Ui.Text(g, sub, fsub, Ui.Sub, x, y + Ui.Px(30));

            // ---- GitHub 入口：独立窗口，不再占用右侧栏 ----
            int subW = (int)g.MeasureString(sub, fsub).Width;
            string gtxt = GhLoggedIn ? ("GitHub " + (GhUser.Length > 0 ? GhUser : "已登录")) : "GitHub 同步";
            var gf = Ui.F(8.5f, true);
            int gw = Math.Max(Ui.Px(96), (int)g.MeasureString(gtxt, gf).Width + Ui.Px(26));
            _ghRect = new Rectangle(x + subW + Ui.Px(16), y + Ui.Px(29), gw, Ui.Px(24));
            Color gfg = GhLoggedIn ? Ui.Green : Ui.Accent;
            Color gbg = GhLoggedIn ? Ui.GreenSoft : Ui.AccentSoft;
            if (_ghHover) gbg = GhLoggedIn ? ColorTranslator.FromHtml("#D3EFE0") : ColorTranslator.FromHtml("#D9E4FD");
            Ui.FillRound(g, _ghRect, _ghRect.Height / 2, gbg);
            Ui.StrokeRound(g, _ghRect, _ghRect.Height / 2, gfg, 1f);
            Ui.TextC(g, gtxt, gf, gfg, _ghRect);

            // ---------- 右侧进度区：文字 / 进度条 / 统计 分三行，互不重叠 ----------
            int rightW = Ui.Px(320);
            int rx = Width - Ui.Px(24) - rightW;
            int ry = Ui.Px(15);

            // 第 1 行：进度文字（右对齐）
            bool allDone = Total > 0 && Done >= Total;
            string ptxt = Total == 0 ? "今日无安排" : ("今日进度 " + Done + " / " + Total + (allDone ? "　完成 ✓" : ""));
            var pf2 = Ui.F(10f, true);
            int ptw = (int)g.MeasureString(ptxt, pf2).Width;
            Ui.Text(g, ptxt, pf2, allDone ? Ui.Green : Ui.Ink, rx + rightW - ptw, ry - Ui.Px(3));

            // 第 2 行：进度条
            var pl = new Rectangle(rx, ry + Ui.Px(21), rightW, Ui.Px(9));
            Ui.FillRound(g, pl, pl.Height / 2, ColorTranslator.FromHtml("#E7EBF3"));
            double ratio = Total == 0 ? 0 : Math.Min(1.0, (double)Done / Total);
            if (ratio > 0)
            {
                var pf = new Rectangle(pl.X, pl.Y, Math.Max(pl.Height, (int)(pl.Width * ratio)), pl.Height);
                Ui.FillRound(g, pf, pl.Height / 2, allDone ? Ui.Green : Ui.Accent);
            }

            // 第 3 行：打卡统计（右对齐）
            string s2 = "连续打卡 " + Streak + " 天　·　最长 " + Best + " 天　·　累计 " + Hours.ToString("0.#") + " 小时";
            var f2 = Ui.F(9f);
            int s2w = (int)g.MeasureString(s2, f2).Width;
            Ui.Text(g, s2, f2, Ui.Sub, rx + rightW - s2w, ry + Ui.Px(36));

            using (var p = new Pen(Ui.Line, 1)) g.DrawLine(p, 0, Height - 1, Width, Height - 1);
        }
    }

    // ===================================================================== 时段卡片
    public class SlotCard : Control
    {
        public Slot Slot;
        public DateTime Date;
        public bool ReadOnlyView;
        public event EventHandler Changed;

        readonly List<Pill> _pills = new List<Pill>();
        readonly Pill _doneBtn;
        readonly string _bookTitle = "";
        List<string[]> _videoLinks = new List<string[]>();
        bool _hover;

        public SlotCard(DateTime date, Slot s)
        {
            Date = date; Slot = s;
            _bookTitle = VideoLinks.BookTitleForSlot(s);
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;

            _doneBtn = new Pill();
            _doneBtn.Toggle = true;
            _doneBtn.Click += delegate
            {
                Store.Toggle(Date, Slot);
                _doneBtn.On = Store.IsDone(Date, Slot);
                _doneBtn.Text = _doneBtn.On ? "已完成 ✓" : "标记完成";
                _doneBtn.Invalidate();
                Invalidate();
                if (Changed != null) Changed(this, EventArgs.Empty);
            };
            _doneBtn.On = Store.IsDone(Date, Slot);
            _doneBtn.Text = _doneBtn.On ? "已完成 ✓" : "标记完成";
            Controls.Add(_doneBtn);

            var links = VideoLinks.ForSlot(s);
            _videoLinks = links;
            var pdfs = VideoLinks.PdfLinksForSlot(s);

            // 只保留两个按钮：直接打开课本 + 一个「配套视频资源」下拉，避免一排标签眼花缭乱
            foreach (var l in pdfs)
            {
                var p = new Pill();
                p.Text = l[0]; p.Url = l[1];
                p.Click += delegate { OpenUrl(p.Url); };
                _pills.Add(p);
                Controls.Add(p);
            }
            if (links.Count > 0)
            {
                var drop = new Pill();
                drop.Text = "配套视频资源 ▾";
                drop.Click += delegate
                {
                    ResourceMenu.ShowAt(drop, _videoLinks, delegate (string url) { OpenUrl(url); });
                };
                _pills.Add(drop);
                Controls.Add(drop);
            }
            Cursor = Cursors.Default;
        }

        public static void OpenUrl(string url)
        {
            try
            {
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                { FileName = url, UseShellExecute = true });
            }
            catch (Exception ex) { MessageBox.Show("无法打开链接：\n" + url + "\n\n" + ex.Message, "提示"); }
        }

        /// <summary>同步「完成」状态（不重建控件）</summary>
        public void RefreshState()
        {
            bool done = Store.IsDone(Date, Slot);
            _doneBtn.On = done;
            _doneBtn.Text = done ? "已完成 ✓" : "标记完成";
            _doneBtn.Invalidate();
            Invalidate();
        }

        int LayoutPills(int width)
        {
            int x = Ui.Px(16), y = 0, rowH = Ui.Px(26), gap = Ui.Px(8);
            int maxX = width - Ui.Px(16);
            int rows = 1;
            foreach (var p in _pills)
            {
                p.Font = Ui.F(8.5f);
                int w = Pill.Measure(p.Text, p.Font);
                if (x + w > maxX && x > Ui.Px(16)) { x = Ui.Px(16); y += rowH + gap; rows++; }
                p.Bounds = new Rectangle(x, y, w, Ui.Px(24));
                x += w + gap;
            }
            return y + rowH;
        }

        public static int MeasureHeight(Slot s, int width, bool hasLinks)
        {
            int h = Ui.Px(14) + Ui.Px(26);              // 上边距 + 时间段行
            h += Ui.Px(26);                              // 标题
            h += Math.Max(0, s.Body.Count - 1) * Ui.Px(20);
            if (hasLinks) h += Ui.Px(34) + Ui.Px(10);
            h += Ui.Px(14);
            return h;
        }

        public void DoLayout(int width)
        {
            // 先按宽度排好链接（可能折行），再据此决定卡片高度，避免正文被链接盖住
            int pillsH = _pills.Count > 0 ? LayoutPills(width) : 0;

            int contentH = Ui.Px(14) + Ui.Px(26)              // 上边距 + 时间段行
                         + Ui.Px(26)                          // 标题
                         + Math.Max(0, Slot.Body.Count - 1) * Ui.Px(20);
            if (_bookTitle.Length > 0) contentH += Ui.Px(23); // 课本标题行
            if (_pills.Count > 0) contentH += Ui.Px(12);
            ClientSize = new Size(width, contentH + pillsH + Ui.Px(14));

            // 完成按钮靠右上
            _doneBtn.Font = Ui.F(9f);
            int bw = Math.Max(Ui.Px(84), Pill.Measure(_doneBtn.Text, _doneBtn.Font));
            _doneBtn.Bounds = new Rectangle(Width - Ui.Px(16) - bw, Ui.Px(14), bw, Ui.Px(28));

            int top = contentH;
            foreach (var p in _pills) p.Top += top;
        }

        protected override void OnMouseEnter(EventArgs e) { _hover = true; Invalidate(); base.OnMouseEnter(e); }
        protected override void OnMouseLeave(EventArgs e)
        {
            _hover = ClientRectangle.Contains(PointToClient(Cursor.Position));
            Invalidate(); base.OnMouseLeave(e);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            bool done = Store.IsDone(Date, Slot);

            var r = new Rectangle(0, 0, Width - 1, Height - 1);
            Ui.FillRound(g, r, Ui.Px(14), done ? Ui.GreenSoft : Ui.Card);
            Ui.StrokeRound(g, r, Ui.Px(14), done ? ColorTranslator.FromHtml("#B9E3CC") : Ui.Line, 1f);

            // 左侧色条
            var bar = new Rectangle(Ui.Px(0), Ui.Px(14), Ui.Px(4), Height - Ui.Px(28));
            Color bc = PillarColor(Slot.Subject);
            Ui.FillRound(g, bar, Ui.Px(2), done ? Ui.Green : bc);

            int x = Ui.Px(16), y = Ui.Px(14);
            // 时间
            var ft = Ui.F(12f, true);
            string timeTxt = Slot.Start + " – " + Slot.End;
            Ui.Text(g, timeTxt, ft, done ? Ui.Green : Ui.Ink, x, y);
            int tw = (int)g.MeasureString(timeTxt, ft).Width;

            // 科目
            var fs = Ui.F(10f, true);
            int sx = x + tw + Ui.Px(10);
            int swid = (int)g.MeasureString(Slot.Subject, fs).Width;
            // 与时间文字的光学中心对齐（实测徽章需比文字上移约 2px）
            var sub = new Rectangle(sx, y, swid + Ui.Px(16), Ui.Px(20));
            Ui.FillRound(g, sub, Ui.Px(10), Color.FromArgb(28, bc));
            Ui.TextC(g, Slot.Subject, fs, bc, sub);

            y += Ui.Px(28);
            if (Slot.Body.Count > 0)
            {
                var fh = Ui.F(11f, true);
                Ui.Text(g, Clip(g, Slot.Body[0], fh, Width - Ui.Px(120)), fh,
                    done ? ColorTranslator.FromHtml("#4C7A63") : Ui.Ink, x, y);
                y += Ui.Px(24);

                // ---- 所需课本标题（标签与书名共用一条行中线，保证对齐）----
                if (_bookTitle.Length > 0)
                {
                    int rowH = Ui.Px(20);
                    int chipH = Ui.Px(17);
                    var fc = Ui.F(8.5f, true);
                    int cw = (int)Math.Ceiling(g.MeasureString("课本", fc).Width) + Ui.Px(14);
                    var chip = new Rectangle(x, y + (rowH - chipH) / 2, cw, chipH);
                    Ui.FillRound(g, chip, chipH / 2, done ? Color.FromArgb(30, 33, 163, 102) : Ui.AccentSoft);
                    Ui.TextC(g, "课本", fc, done ? Ui.Green : Ui.Accent, chip);

                    var fb = Ui.F(9.5f, false);
                    // 上移 1px：让书名的视觉中心与徽章中心严格重合
                    var txt = new Rectangle(chip.Right + Ui.Px(8), y - Ui.Px(1),
                                            Width - chip.Right - Ui.Px(8) - Ui.Px(16), rowH);
                    Ui.TextVC(g, _bookTitle, fb, done ? ColorTranslator.FromHtml("#4C7A63")
                                                      : ColorTranslator.FromHtml("#3C4A60"), txt);
                    y += rowH + Ui.Px(3);
                }

                var fd = Ui.F(9f);
                for (int i = 1; i < Slot.Body.Count; i++)
                {
                    Ui.Text(g, Clip(g, Slot.Body[i], fd, Width - Ui.Px(34)), fd, Ui.Sub, x, y);
                    y += Ui.Px(20);
                }
            }
        }

        static string Clip(Graphics g, string s, Font f, int maxW)
        {
            if (g.MeasureString(s, f).Width <= maxW) return s;
            string t = s;
            while (t.Length > 1 && g.MeasureString(t + "…", f).Width > maxW) t = t.Substring(0, t.Length - 1);
            return t + "…";
        }

        public static Color PillarColor(string subject)
        {
            if (subject.Contains("纯数")) return Ui.Accent;
            if (subject.Contains("物理")) return ColorTranslator.FromHtml("#7A5AF8");
            if (subject.Contains("应用")) return ColorTranslator.FromHtml("#0E9AA7");
            if (subject.Contains("进阶")) return ColorTranslator.FromHtml("#D9548B");
            if (subject.Contains("英语")) return Ui.Amber;
            return Ui.Sub;
        }
    }

    // ===================================================================== 统计/操作卡
    public class CardGroup
    {
        public string Label = "";
        public int LabelY;
        public List<Pill> Buttons = new List<Pill>();
    }

    public class SimpleCard : Control
    {
        public string Title = "";
        public List<string> Lines = new List<string>();
        public List<CardGroup> Groups = new List<CardGroup>();

        public SimpleCard(string title)
        {
            Title = title;
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;
        }

        /// <summary>开一个分组（之后 AddButton 的按钮都归到这一组）</summary>
        public void AddGroup(string label)
        {
            var g = new CardGroup();
            g.Label = label ?? "";
            Groups.Add(g);
        }

        public void AddButton(string text, EventHandler onClick, bool primary)
        {
            if (Groups.Count == 0) AddGroup("");
            var p = new Pill();
            p.Text = text; p.Primary = primary;
            p.Click += onClick;
            Groups[Groups.Count - 1].Buttons.Add(p);
            Controls.Add(p);
        }

        public void DoLayout(int width)
        {
            int y = Ui.Px(48);
            if (Lines.Count > 0) y += Lines.Count * Ui.Px(23) + Ui.Px(2);

            foreach (var g in Groups)
            {
                if (g.Label.Length > 0)
                {
                    g.LabelY = y;
                    y += Ui.Px(22);
                }
                else g.LabelY = -1;

                int x = Ui.Px(16), rowTop = y;
                foreach (var b in g.Buttons)
                {
                    b.Font = Ui.F(9f);
                    int w = Math.Max(Ui.Px(104), Pill.Measure(b.Text, b.Font));
                    if (x + w > width - Ui.Px(16) && x > Ui.Px(16)) { x = Ui.Px(16); rowTop += Ui.Px(38); }
                    b.Bounds = new Rectangle(x, rowTop, w, Ui.Px(30));
                    x += w + Ui.Px(8);
                }
                y = rowTop + Ui.Px(30) + Ui.Px(10);
            }
            y += Ui.Px(4);
            ClientSize = new Size(width, y);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            var r = new Rectangle(0, 0, Width - 1, Height - 1);
            Ui.FillRound(g, r, Ui.Px(14), Ui.Card);
            Ui.StrokeRound(g, r, Ui.Px(14), Ui.Line, 1f);
            Ui.Text(g, Title, Ui.F(10.5f, true), Ui.Ink, Ui.Px(16), Ui.Px(16));

            int y = Ui.Px(48);
            var f = Ui.F(9.5f);
            foreach (var l in Lines)
            {
                Ui.Text(g, l, f, Ui.Sub, Ui.Px(16), y);
                y += Ui.Px(23);
            }
            // 分组标题
            var fl = Ui.F(8.5f, true);
            foreach (var grp in Groups)
                if (grp.LabelY > 0)
                    Ui.Text(g, grp.Label, fl, ColorTranslator.FromHtml("#9AA6B8"), Ui.Px(16), grp.LabelY - Ui.Px(2));
        }
    }

    // ===================================================================== 主窗体
    public class MainForm : Form
    {
        readonly Panel _leftScroll = new Panel();
        readonly Panel _rightFlow = new Panel();
        readonly HeaderBar _header = new HeaderBar();
        readonly Label _status = new Label();
        readonly MiniCalendar _cal = new MiniCalendar();
        readonly SimpleCard _stats = new SimpleCard("学习统计");
        readonly SimpleCard _tools = new SimpleCard("设置与工具");
        readonly Card _calCard = new Card();
        DateTime _viewDate = DateTime.Today;
        public bool ForceClose = false;
        public string DiagCloseReason = "";
        public int DiagCloseCount = 0;
        public bool DiagHidToTray = false;

        bool _busy;                       // 布局/重绘重入锁，防止滚动条出现→宽度变化→再布局 的抖动死循环
        int _sbW;                         // 预留给竖向滚动条的宽度，保证布局宽度恒定

        public MainForm(bool startMinimized)
        {
            Text = "学习助手 · NCUK IFY 自学计划";
            Ui.EnableDoubleBuffer(this);
            SetStyle(ControlStyles.OptimizedDoubleBuffer, true);
            BackColor = Ui.Bg;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(Ui.Px(1180), Ui.Px(856));
            MinimumSize = new Size(Ui.Px(1000), Ui.Px(640));
            Font = Ui.F(9f);
            AutoScaleMode = AutoScaleMode.None;
            _sbW = SystemInformation.VerticalScrollBarWidth + Ui.Px(6);
            try { Icon = AppIcon.Get(32); } catch { }

            // ---- 顶部
            _header.Dock = DockStyle.Top;
            _header.Height = Ui.Px(84);
            _header.BackColor = Color.White;
            Controls.Add(_header);

            // ---- 状态栏
            _status.Dock = DockStyle.Bottom;
            _status.Height = Ui.Px(30);
            _status.TextAlign = ContentAlignment.MiddleLeft;
            _status.Padding = new Padding(Ui.Px(24), 0, 0, 0);
            _status.ForeColor = Ui.Sub;
            _status.BackColor = Color.White;
            _status.Font = Ui.F(9f);
            Controls.Add(_status);

            // ---- 主体
            var body = new Panel();
            body.Dock = DockStyle.Fill;
            body.Padding = new Padding(Ui.Px(20), Ui.Px(14), Ui.Px(20), Ui.Px(10));
            body.BackColor = Ui.Bg;
            Controls.Add(body);
            body.BringToFront();

            _leftScroll.Dock = DockStyle.Fill;
            _leftScroll.AutoScroll = true;
            _leftScroll.BackColor = Ui.Bg;
            _leftScroll.Padding = new Padding(0, 0, Ui.Px(10), 0);
            Ui.EnableDoubleBuffer(_leftScroll);

            _rightFlow.Dock = DockStyle.Right;
            _rightFlow.Width = Ui.Px(360);
            _rightFlow.AutoScroll = false;   // 右侧内容整体适配可视高度，不出滚动条
            _rightFlow.BackColor = Ui.Bg;
            Ui.EnableDoubleBuffer(_rightFlow);

            body.Controls.Add(_leftScroll);
            body.Controls.Add(_rightFlow);

            // 日历卡
            _calCard.Radius = Ui.Px(14);
            _calCard.BackColor = Ui.Bg;
            _calCard.Controls.Add(_cal);
            _rightFlow.Controls.Add(_calCard);

            _rightFlow.Controls.Add(_stats);
            _rightFlow.Controls.Add(_tools);

            // 工具按钮（分三组，界面更清爽）
            _tools.AddGroup("学习记录");
            _tools.AddButton("导出记录 CSV", delegate
            {
                try { string p = Store.ExportCsv(); MessageBox.Show(this, "已导出：\n" + p, "导出成功"); }
                catch (Exception ex) { MessageBox.Show(this, "导出失败：" + ex.Message, "提示"); }
            }, false);
            _tools.AddButton("导入记录 CSV", delegate { ImportCsv(); }, false);

            _tools.AddGroup("其他");
            _tools.AddButton("查看更新日志", delegate { ShowChangelog(); }, false);
            _tools.AddButton("回到今天", delegate { GoToday(); }, false);

            _tools.AddGroup("启动与关闭");
            _tools.AddButton(AutoStartText(), delegate (object s, EventArgs e)
            {
                bool on = !Store.AutoStartEnabled;
                Store.SetAutoStart(on);
                ((Pill)s).Text = AutoStartText();
                ((Pill)s).Invalidate();
                MessageBox.Show(this, on ? "已设置为开机自动启动。" : "已取消开机自动启动。", "提示");
            }, true);
            _tools.AddButton(CloseBehaviorText(), delegate (object s, EventArgs e)
            {
                Store.CloseToTray = !Store.CloseToTray;
                ((Pill)s).Text = CloseBehaviorText();
                ((Pill)s).Invalidate();
            }, false);


            _cal.DateSelected += delegate
            {
                DateTime sel = _cal.Selected;
                if (_viewDate.Date == sel.Date) return;
                _viewDate = sel;
                RefreshAll();
            };

            _inst = this;
            FillGitHubUser();
            _header.GhClick += delegate { OpenGitHub(); };
            _rightFlow.Resize += delegate { LayoutRight(); };
            _leftScroll.Resize += delegate { LayoutLeft(); };
            Resize += delegate { LayoutRight(); LayoutLeft(); };

            _cal.Selected = DateTime.Today;
            RefreshAll();

            if (startMinimized)
            {
                Opacity = 0;
                ShowInTaskbar = false;
            }
        }

        string AutoStartText()
        {
            return Store.AutoStartEnabled ? "开机自启：已开启" : "开机自启：已关闭";
        }

        string CloseBehaviorText()
        {
            return Store.CloseToTray ? "关闭时：最小化到托盘" : "关闭时：直接退出";
        }

        /// <summary>从之前导出的 CSV 恢复打卡记录</summary>
        void ImportCsv()
        {
            using (var d = new OpenFileDialog())
            {
                d.Title = "选择之前导出的学习记录 CSV";
                d.Filter = "CSV 文件 (*.csv)|*.csv|所有文件 (*.*)|*.*";
                try { d.InitialDirectory = Store.DataDir; } catch { }
                if (d.ShowDialog(this) != DialogResult.OK) return;
                int add, ex, bad;
                try { Store.ImportCsv(d.FileName, out add, out ex, out bad); }
                catch (Exception e2) { MessageBox.Show(this, "导入失败：" + e2.Message, "提示"); return; }
                RefreshAll();
                string msg = "导入完成\n\n新增打卡：" + add + " 条\n已存在：" + ex + " 条";
                if (bad > 0) msg += "\n无法识别：" + bad + " 行";
                MessageBox.Show(this, msg, "导入学习记录");
            }
        }

        /// <summary>用内置阅读器打开 CHANGELOG.md</summary>
        void ShowChangelog()
        {
            string p = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "CHANGELOG.md");
            string md;
            try
            {
                md = File.Exists(p)
                    ? File.ReadAllText(p, System.Text.Encoding.UTF8)
                    : "没有找到 CHANGELOG.md。\n\n它应该和 StudyCompanion.exe 放在同一个文件夹里。";
            }
            catch (Exception ex) { md = "读取更新日志失败：" + ex.Message; }
            var v = new DocViewer("更新日志 · StudyCompanion", md);
            v.Show(this);
        }

        public void GoToday()
        {
            _viewDate = DateTime.Today;
            _cal.Selected = DateTime.Today;
            _cal.DisplayMonth = new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1);
            RefreshAll();
        }

        /// <summary>跳到指定日期（用于命令行截图自检）</summary>
        public void SetViewDate(DateTime d)
        {
            _viewDate = d;
            _cal.Selected = d;
            _cal.DisplayMonth = new DateTime(d.Year, d.Month, 1);
            RefreshAll();
        }

        public void RefreshAll() { RefreshCore(true); }

        /// <summary>只刷新数据与状态，不重建卡片（打卡时用，避免闪动）</summary>
        void RefreshLight() { RefreshCore(false); }

        void RefreshCore(bool rebuildCards)
        {
            if (_busy) return;                 // 已在刷新中，直接忽略重入，杜绝抖动
            _busy = true;
            try
            {
                var dp = ScheduleData.GetDay(_viewDate);
                int done = Store.DoneCount(_viewDate);

                _header.Date = _viewDate;
                _header.Done = done;
                _header.Total = dp == null ? 0 : dp.Slots.Count;
                _header.Streak = Store.Streak();
                _header.Best = Store.BestStreak();
                _header.Hours = Store.TotalHours();
                _header.GhLoggedIn = GitHub.LoggedIn;
                _header.GhUser = GitHub.User;
                _header.Invalidate();

                if (rebuildCards) BuildCards(dp);
                else foreach (Control c in _leftScroll.Controls)
                    {
                        var sc = c as SlotCard;
                        if (sc != null) { sc.RefreshState(); }
                    }

                // 统计
                int wd, wt;
                Store.WeekProgress(out wd, out wt);
                _stats.Lines = new List<string>
                {
                    "本周完成：" + wd + " / " + wt + " 个时段",
                    "累计完成：" + Store.TotalDoneSlots() + " 个时段 · " + Store.TotalHours().ToString("0.#") + " 小时",
                    "连续打卡：" + Store.Streak() + " 天（最长 " + Store.BestStreak() + " 天）",
                    "本月全勤：" + PerfectDaysThisMonth() + " 天",
                };

                DoLayoutRight();
                DoLayoutLeft();
                _cal.Invalidate();
                _stats.Invalidate();

                UpdateStatus();
            }
            finally { _busy = false; }
        }

        void UpdateStatus()
        {
            string next = "今天没有安排";
            var plan = ScheduleData.GetDay(DateTime.Today);
            if (plan != null)
            {
                var nx = plan.Slots.Where(x => x.StartToday > DateTime.Now).OrderBy(x => x.StartMinutes).FirstOrDefault();
                if (nx != null)
                {
                    var span = nx.StartToday - DateTime.Now;
                    next = "下次提醒：" + nx.Start + "（" + nx.Subject + "）· 还有 "
                         + (span.Hours > 0 ? span.Hours + " 小时 " : "") + span.Minutes + " 分钟";
                }
                else
                {
                    var cur = plan.Slots.FirstOrDefault(x => DateTime.Now >= x.StartToday && DateTime.Now < x.EndToday);
                    next = cur != null ? "正在进行：" + cur.Start + "–" + cur.End + " " + cur.Subject : "今天的学习时段已结束，记得打卡 ✔";
                }
            }
            string src = "数据源：" + (ScheduleData.LoadedFromCache ? "缓存（课表被占用）" : Path.GetFileName(ScheduleData.WorkbookPath));
            if (!string.IsNullOrEmpty(ScheduleData.LastError)) src = "⚠ " + ScheduleData.LastError;
            _status.Text = "　" + next + "         " + src + "　|　课表共 " + ScheduleData.DayCount + " 天";
        }

        /// <summary>布局稳定性探针（供 --layouttest 自检用）</summary>
        public string LayoutProbe()
        {
            var sb = new System.Text.StringBuilder();
            sb.Append("cal=").Append(_cal.Bounds).Append(" | ");
            sb.Append("calCard=").Append(_calCard.Bounds).Append(" | ");
            sb.Append("stats=").Append(_stats.Bounds).Append(" | ");
            sb.Append("tools=").Append(_tools.Bounds).Append(" | ");
            sb.Append("rightH=").Append(_rightFlow.ClientSize.Height).Append(" | ");
            sb.Append("githubBtn=").Append(_header.GhLoggedIn).Append(" | ");
            sb.Append("leftW=").Append(_leftScroll.Width).Append('/').Append(_leftScroll.ClientSize.Width).Append(" | ");
            int n = 0;
            foreach (Control c in _leftScroll.Controls)
                if (c is SlotCard) sb.Append("card").Append(n++).Append('=').Append(c.Bounds).Append(" | ");
            return sb.ToString();
        }

        public int CardCount
        {
            get { int n = 0; foreach (Control c in _leftScroll.Controls) if (c is SlotCard) n++; return n; }
        }

        int PerfectDaysThisMonth()
        {
            int n = 0;
            var t = DateTime.Today;
            for (int d = 1; d <= t.Day; d++)
                if (Store.IsPerfectDay(new DateTime(t.Year, t.Month, d))) n++;
            return n;
        }

        void BuildCards(DayPlan dp)
        {
            // 先摘下来再销毁，避免在遍历 Controls 集合时修改它
            var old = new List<Control>();
            foreach (Control c in _leftScroll.Controls) old.Add(c);
            _leftScroll.Controls.Clear();
            _leftScroll.AutoScrollPosition = new Point(0, 0);   // 换天后回到顶部

            _leftScroll.SuspendLayout();
            Native.Freeze(_leftScroll);
            try
            {
                var title = new Label();
                title.AutoSize = false;
                title.Text = _viewDate.Date == DateTime.Today
                    ? "今天要学的内容"
                    : (_viewDate.ToString("yyyy-MM-dd") + " 的安排（查看模式）");
                title.Font = Ui.F(11.5f, true);
                title.ForeColor = Ui.Ink;
                title.Height = Ui.Px(30);
                title.Tag = "title";
                _leftScroll.Controls.Add(title);

                if (dp == null || dp.Slots.Count == 0)
                {
                    var lab = new Label();
                    lab.Text = "这一天没有安排（可能不在 2026-10 ~ 2027-08 的计划范围内）。";
                    lab.Font = Ui.F(10f);
                    lab.ForeColor = Ui.Sub;
                    lab.Height = Ui.Px(60);
                    lab.Tag = "empty";
                    _leftScroll.Controls.Add(lab);
                }
                else
                {
                    foreach (var s in dp.Slots)
                    {
                        var card = new SlotCard(_viewDate, s);
                        card.Tag = "card";
                        card.Changed += delegate { RefreshLight(); };
                        _leftScroll.Controls.Add(card);
                    }
                }
            }
            finally
            {
                _leftScroll.ResumeLayout();
                Native.Unfreeze(_leftScroll);
            }
            foreach (var c in old) c.Dispose();
        }

        void LayoutLeft()
        {
            if (_busy) return;
            _busy = true;
            try { DoLayoutLeft(); } finally { _busy = false; }
        }

        void LayoutRight()
        {
            if (_busy) return;
            _busy = true;
            try { DoLayoutRight(); } finally { _busy = false; }
        }

        /// <summary>宽度一律按「面板宽度 - 预留滚动条」计算，不随滚动条出现/消失变化，避免来回抖动</summary>
        int ColWidth(Panel p)
        {
            int w = p.Width - _sbW;
            return w < Ui.Px(220) ? Ui.Px(220) : w;
        }

        void DoLayoutLeft()
        {
            int w = ColWidth(_leftScroll);
            int y = Ui.Px(6);
            foreach (Control c in _leftScroll.Controls)
            {
                if (!(c.Tag is string)) continue;
                string t = (string)c.Tag;
                if (t == "title" || t == "empty")
                {
                    c.SetBounds(Ui.Px(4), y, w, c.Height);
                    y += c.Height + Ui.Px(6);
                }
                else if (t == "card")
                {
                    var card = (SlotCard)c;
                    card.DoLayout(w - Ui.Px(8));
                    card.SetBounds(Ui.Px(4), y, card.Width, card.Height);
                    y += card.Height + Ui.Px(12);
                }
            }
        }

        // ---------------------------------------------------------------- GitHub 入口
        static MainForm _inst;

        void OpenGitHub()
        {
            using (var f = new GitHubForm())
            {
                f.ShowDialog(this);
                RefreshLight();
                _header.Invalidate();
            }
        }

        /// <summary>
        /// 登录了但用户名是空的（旧版本保存时漏了）—— 后台补拉一次，顶栏就能显示名字。
        /// </summary>
        void FillGitHubUser()
        {
            if (!GitHub.LoggedIn || GitHub.User.Length > 0) return;
            var w = new System.ComponentModel.BackgroundWorker();
            w.DoWork += delegate
            {
                string u = GitHub.CurrentUser();
                if (u.Length > 0)
                {
                    GitHub.SaveToken(GitHub.Token, u);
                    NotifyHeaderChanged();
                }
            };
            w.RunWorkerAsync();
        }

        /// <summary>登录状态变了 —— 刷新顶栏那个按钮</summary>
        public static void NotifyHeaderChanged()        {
            if (_inst != null)
            {
                try { _inst.BeginInvoke((MethodInvoker)delegate { _inst._header.Invalidate(); }); }
                catch { }
            }
        }

        /// <summary>打卡记录变了 —— 刷新统计与日历</summary>
        public static void NotifyDataChanged()
        {
            if (_inst != null)
            {
                try { _inst.BeginInvoke((MethodInvoker)delegate { _inst.RefreshLight(); }); }
                catch { }
            }
        }

        /// <summary>更新完要退出，跳过「关闭时最小化到托盘」的逻辑</summary>
        public static void ForceQuit()
        {
            if (_inst != null) _inst.ForceClose = true;
            Application.Exit();
        }

        void DoLayoutRight()
        {
            int w = ColWidth(_rightFlow);
            int gap = Ui.Px(12);

            // 先量出统计卡和工具卡需要多高（它们的高度是内容决定的）
            _stats.DoLayout(w);
            _tools.DoLayout(w);

            // 日历吃掉剩下的全部高度 —— 这样右侧永远不会超出可视区、不会出滚动条
            int avail = _rightFlow.ClientSize.Height;
            if (avail <= 0) avail = Ui.Px(720);
            int calH = avail - _stats.Height - _tools.Height - gap * 2;
            if (calH > Ui.Px(340)) calH = Ui.Px(340);       // 再高也没意义
            if (calH < Ui.Px(250)) calH = Ui.Px(250);       // 保证日历可用

            int y = 0;
            _calCard.SetBounds(0, y, w, calH); y += calH + gap;
            _cal.SetBounds(Ui.Px(10), Ui.Px(10), w - Ui.Px(20), calH - Ui.Px(22));
            _stats.SetBounds(0, y, w, _stats.Height); y += _stats.Height + gap;
            _tools.SetBounds(0, y, w, _tools.Height);
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            DiagCloseReason = e.CloseReason.ToString();
            DiagCloseCount++;
            if (!ForceClose && e.CloseReason == CloseReason.UserClosing)
            {
                if (!Store.CloseToTray)
                {
                    ForceClose = true;          // 用户选择「直接退出」，走正常关闭流程
                }
                else
                {
                    // 关键：不能在 FormClosing 里直接 Hide()。
                    // e.Cancel = true 之后关闭流程会继续走完，会把刚隐藏的窗口重新显示出来
                    //（表现就是：任务栏图标没了，窗口却还在屏幕上）。
                    // 用 BeginInvoke 推迟到关闭流程结束之后再隐藏。
                    e.Cancel = true;
                    BeginInvoke((MethodInvoker)HideToTray);
                    return;
                }
            }
            base.OnFormClosing(e);
        }

        /// <summary>隐藏到托盘（比 Hide() 多几步保险，确保真的从屏幕上消失）</summary>
        void HideToTray()
        {
            if (IsDisposed) return;
            DiagHidToTray = true;
            // ShowInTaskbar 的赋值会重建窗口句柄，必须先设置、后隐藏
            ShowInTaskbar = false;
            Hide();
            if (Visible)                    // 极端情况下再补一次
            {
                Visible = false;
                Hide();
            }
            // 3 秒后自动关闭，只有一个带倒计时的「知道了」按钮
            TrayApp.Toast("已最小化到托盘",
                "程序仍在后台运行，到点会提醒你。", 3, "", true);
        }
    }

    // ===================================================================== 应用图标
    public static class AppIcon
    {
        static readonly Dictionary<int, Icon> _cache = new Dictionary<int, Icon>();

        /// <summary>优先用编译进 exe 的 app.ico（多尺寸，按需选最合适的一档）</summary>
        public static Icon Get(int size)
        {
            Icon ic;
            if (_cache.TryGetValue(size, out ic)) return ic;

            try
            {
                var asm = System.Reflection.Assembly.GetExecutingAssembly();
                using (var s = asm.GetManifestResourceStream("StudyCompanion.app.ico"))
                {
                    if (s != null)
                    {
                        using (var ms = new System.IO.MemoryStream())
                        {
                            s.CopyTo(ms);
                            ms.Position = 0;
                            ic = new Icon(ms, new Size(size, size));
                        }
                    }
                }
            }
            catch { }

            if (ic == null)
            {
                try { ic = Icon.ExtractAssociatedIcon(Application.ExecutablePath); } catch { }
            }
            if (ic == null) ic = DrawFallback(size);

            _cache[size] = ic;
            return ic;
        }

        public static Icon Get() { return Get(32); }

        /// <summary>兜底：程序化画一个（资源缺失时才会用到）</summary>
        static Icon DrawFallback(int size)
        {
            var bmp = new Bitmap(size, size);
            using (var g = Graphics.FromImage(bmp))
            {
                g.SmoothingMode = SmoothingMode.AntiAlias;
                g.Clear(Color.Transparent);
                using (var b = new SolidBrush(ColorTranslator.FromHtml("#3568E8")))
                using (var path = Ui.Round(new Rectangle(0, 0, size, size), Math.Max(2, size / 5)))
                    g.FillPath(b, path);
                using (var pen = new Pen(Color.White, Math.Max(1.5f, size * 0.11f)))
                {
                    pen.StartCap = pen.EndCap = LineCap.Round;
                    pen.LineJoin = LineJoin.Round;
                    g.DrawLines(pen, new[]
                    {
                        new PointF(size * 0.26f, size * 0.52f),
                        new PointF(size * 0.44f, size * 0.70f),
                        new PointF(size * 0.76f, size * 0.32f)
                    });
                }
            }
            using (var tmp = Icon.FromHandle(bmp.GetHicon()))
                return (Icon)tmp.Clone();
        }
    }
}
