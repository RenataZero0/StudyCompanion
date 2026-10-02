using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>重建控件时冻结重绘，消除闪动</summary>
    public static class Native
    {
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        static extern int SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);
        const int WM_SETREDRAW = 0x000B;

        public static void Freeze(Control c)
        {
            if (c != null && c.IsHandleCreated) SendMessage(c.Handle, WM_SETREDRAW, IntPtr.Zero, IntPtr.Zero);
        }

        public static void Unfreeze(Control c)
        {
            if (c == null || !c.IsHandleCreated) return;
            SendMessage(c.Handle, WM_SETREDRAW, new IntPtr(1), IntPtr.Zero);
            c.Invalidate(true);
            c.Update();
        }
    }

    public static class Ui
    {
        public static readonly Color Bg = ColorTranslator.FromHtml("#F3F5F9");
        public static readonly Color Card = Color.White;
        public static readonly Color Ink = ColorTranslator.FromHtml("#1B2432");
        public static readonly Color Sub = ColorTranslator.FromHtml("#71809A");
        public static readonly Color Line = ColorTranslator.FromHtml("#E5E9F0");
        public static readonly Color Accent = ColorTranslator.FromHtml("#3568E8");
        public static readonly Color AccentSoft = ColorTranslator.FromHtml("#E8EFFE");
        public static readonly Color Green = ColorTranslator.FromHtml("#21A366");
        public static readonly Color GreenSoft = ColorTranslator.FromHtml("#E4F5EC");
        public static readonly Color Amber = ColorTranslator.FromHtml("#DE9420");
        public static readonly Color AmberSoft = ColorTranslator.FromHtml("#FDF2DF");
        public static readonly Color Red = ColorTranslator.FromHtml("#E0533F");
        public static readonly Color RedSoft = ColorTranslator.FromHtml("#FCEAE7");

        public static string FontName = "Microsoft YaHei UI";

        /// <summary>DPI 缩放系数（96dpi = 1.0）</summary>
        public static float S = 1f;
        public static int Px(double v) { return (int)Math.Round(v * S); }

        public static Font F(float pt, bool bold = false)
        {
            return new Font(FontName, pt, bold ? FontStyle.Bold : FontStyle.Regular, GraphicsUnit.Point);
        }

        public static GraphicsPath Round(Rectangle r, int radius)
        {
            var p = new GraphicsPath();
            if (r.Width <= 1 || r.Height <= 1) { p.AddRectangle(r); return p; }
            int d = Math.Max(2, Math.Min(radius * 2, Math.Min(r.Width, r.Height)));
            p.AddArc(r.X, r.Y, d, d, 180, 90);
            p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
            p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
            p.CloseFigure();
            return p;
        }

        public static void FillRound(Graphics g, Rectangle r, int radius, Color c)
        {
            var old = g.SmoothingMode;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            using (var path = Round(r, radius))
            using (var b = new SolidBrush(c)) g.FillPath(b, path);
            g.SmoothingMode = old;
        }

        public static void StrokeRound(Graphics g, Rectangle r, int radius, Color c, float w)
        {
            var old = g.SmoothingMode;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            using (var path = Round(new Rectangle(r.X, r.Y, r.Width - 1, r.Height - 1), radius))
            using (var pen = new Pen(c, w)) g.DrawPath(pen, path);
            g.SmoothingMode = old;
        }

        public static void Text(Graphics g, string s, Font f, Color c, int x, int y)
        {
            using (var b = new SolidBrush(c)) g.DrawString(s, f, b, x, y);
        }

        public static void TextC(Graphics g, string s, Font f, Color c, Rectangle r)
        {
            var sf = new StringFormat();
            sf.Alignment = StringAlignment.Center;
            sf.LineAlignment = StringAlignment.Center;
            using (var b = new SolidBrush(c)) g.DrawString(s, f, b, r, sf);
        }

        /// <summary>左对齐 + 垂直居中 + 超出省略号（用于和徽章并排的文字，保证基线一致）</summary>
        public static void TextVC(Graphics g, string s, Font f, Color c, Rectangle r)
        {
            var sf = new StringFormat();
            sf.Alignment = StringAlignment.Near;
            sf.LineAlignment = StringAlignment.Center;
            sf.FormatFlags = StringFormatFlags.NoWrap;
            sf.Trimming = StringTrimming.EllipsisCharacter;
            using (var b = new SolidBrush(c)) g.DrawString(s, f, b, r, sf);
        }

        public static void EnableDoubleBuffer(Control c)
        {
            typeof(Control).GetProperty("DoubleBuffered",
                System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic)
                .SetValue(c, true, null);
        }
    }

    /// <summary>圆角卡片容器</summary>
    public class Card : Panel
    {
        public int Radius = 14;
        public Color Fill = Ui.Card;
        public Color Border = Ui.Line;
        public bool ShowBorder = true;

        public Card()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;
        }

        protected override void OnPaintBackground(PaintEventArgs e)
        {
            e.Graphics.Clear(Parent != null ? Parent.BackColor : Ui.Bg);
            var r = new Rectangle(0, 0, Width, Height);
            Ui.FillRound(e.Graphics, r, Radius, Fill);
            if (ShowBorder) Ui.StrokeRound(e.Graphics, r, Radius, Border, 1f);
        }
    }

    /// <summary>胶囊按钮 / 超链接</summary>
    public class Pill : Control
    {
        public string Url = "";
        public bool Primary = false;
        public bool Toggle = false;
        public bool On = false;
        bool _hover;

        public Pill()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;
            Cursor = Cursors.Hand;
            Font = Ui.F(9f);
        }

        protected override void OnMouseEnter(EventArgs e) { _hover = true; Invalidate(); base.OnMouseEnter(e); }
        protected override void OnMouseLeave(EventArgs e) { _hover = false; Invalidate(); base.OnMouseLeave(e); }

        public static int Measure(string text, Font f)
        {
            using (var g = Graphics.FromHwnd(IntPtr.Zero))
                return (int)Math.Ceiling(g.MeasureString(text, f).Width) + 18;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            Color bg, fg, bd;
            if (Toggle)
            {
                bg = On ? Ui.Green : (_hover ? Ui.GreenSoft : Color.White);
                fg = On ? Color.White : Ui.Green;
                bd = On ? Ui.Green : Ui.Green;
            }
            else if (Primary)
            {
                bg = _hover ? ColorTranslator.FromHtml("#2A57C4") : Ui.Accent;
                fg = Color.White; bd = bg;
            }
            else
            {
                bg = _hover ? Ui.AccentSoft : Color.White;
                fg = _hover ? Ui.Accent : Ui.Sub;
                bd = _hover ? Ui.Accent : Ui.Line;
            }
            var r = new Rectangle(0, 0, Width, Height);
            Ui.FillRound(g, r, Height / 2, bg);
            Ui.StrokeRound(g, r, Height / 2, bd, 1f);
            Ui.TextC(g, Text, Font, fg, r);
        }
    }

    /// <summary>内置迷你日历</summary>
    public class MiniCalendar : Control
    {
        public DateTime DisplayMonth = new DateTime(DateTime.Today.Year, DateTime.Today.Month, 1);
        public DateTime Selected = DateTime.Today;
        public event EventHandler DateSelected;

        int _hoverDay = -1;
        Rectangle[] _cells = new Rectangle[42];
        DateTime[] _dates = new DateTime[42];

        public MiniCalendar()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw |
                     ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Color.Transparent;
            Height = 250;
            Cursor = Cursors.Hand;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            int idx = HitTest(e.Location);
            if (idx != _hoverDay) { _hoverDay = idx; Invalidate(); }
            base.OnMouseMove(e);
        }

        protected override void OnMouseLeave(EventArgs e) { _hoverDay = -1; Invalidate(); base.OnMouseLeave(e); }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            int idx = HitTest(e.Location);
            if (idx >= 0 && idx < 42 && _dates[idx] != DateTime.MinValue)
            {
                var d = _dates[idx];
                bool changed = d.Date != Selected.Date;
                if (d.Month != DisplayMonth.Month)
                    DisplayMonth = new DateTime(d.Year, d.Month, 1);
                Selected = d;
                if (changed && DateSelected != null) DateSelected(this, EventArgs.Empty);
                Invalidate();
            }
            base.OnMouseDown(e);
        }

        int HitTest(Point p)
        {
            for (int i = 0; i < 42; i++) if (_cells[i].Contains(p)) return i;
            return -1;
        }

        public void GoMonth(int delta)
        {
            DisplayMonth = DisplayMonth.AddMonths(delta);
            Invalidate();
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            int w = Width, pad = Ui.Px(12);
            int headH = Ui.Px(34), weekH = Ui.Px(24);
            int gridTop = headH + weekH;
            int cellW = (w - pad * 2) / 7;
            int cellH = Math.Max(Ui.Px(24), (Height - gridTop - pad) / 6);

            // 月份标题
            Ui.Text(g, DisplayMonth.ToString("yyyy 年 M 月"), Ui.F(11f, true), Ui.Ink, pad, Ui.Px(6));
            // 左右箭头
            var left = new Rectangle(w - pad - Ui.Px(56), Ui.Px(6), Ui.Px(24), Ui.Px(22));
            var right = new Rectangle(w - pad - Ui.Px(26), Ui.Px(6), Ui.Px(24), Ui.Px(22));
            Ui.FillRound(g, left, Ui.Px(6), Color.FromArgb(15, 0, 0, 0));
            Ui.FillRound(g, right, Ui.Px(6), Color.FromArgb(15, 0, 0, 0));
            Ui.TextC(g, "‹", Ui.F(12f, true), Ui.Sub, left);
            Ui.TextC(g, "›", Ui.F(12f, true), Ui.Sub, right);
            _prevRect = left; _nextRect = right;

            string[] wd = { "一", "二", "三", "四", "五", "六", "日" };
            for (int i = 0; i < 7; i++)
            {
                var r = new Rectangle(pad + i * cellW, headH, cellW, weekH);
                Ui.TextC(g, wd[i], Ui.F(8.5f), i >= 5 ? Ui.Amber : Ui.Sub, r);
            }

            DateTime first = new DateTime(DisplayMonth.Year, DisplayMonth.Month, 1);
            int offset = ((int)first.DayOfWeek + 6) % 7;
            DateTime start = first.AddDays(-offset);

            for (int i = 0; i < 42; i++)
            {
                DateTime d = start.AddDays(i);
                _dates[i] = d;
                int col = i % 7, row = i / 7;
                var r = new Rectangle(pad + col * cellW, gridTop + row * cellH, cellW - 2, cellH - 2);
                _cells[i] = new Rectangle(pad + col * cellW, gridTop + row * cellH, cellW, cellH);

                bool inMonth = d.Month == DisplayMonth.Month;
                var dp = ScheduleData.GetDay(d);
                bool scheduled = dp != null;
                int done = scheduled ? Store.DoneCount(d) : 0;
                bool perfect = scheduled && done >= dp.Slots.Count && dp.Slots.Count > 0;
                bool isToday = d.Date == DateTime.Today;
                bool isSel = d.Date == Selected.Date;

                if (perfect) Ui.FillRound(g, r, 8, Ui.Green);
                else if (isSel) Ui.FillRound(g, r, 8, Ui.AccentSoft);
                else if (_hoverDay == i && inMonth && scheduled) Ui.FillRound(g, r, 8, Color.FromArgb(12, 0, 0, 0));

                if (isToday && !perfect) Ui.StrokeRound(g, r, 8, Ui.Accent, 1.4f);

                Color fg = !inMonth ? ColorTranslator.FromHtml("#C6CEDC")
                          : perfect ? Color.White
                          : (d.DayOfWeek == DayOfWeek.Saturday || d.DayOfWeek == DayOfWeek.Sunday) ? Ui.Amber
                          : Ui.Ink;
                Ui.TextC(g, d.Day.ToString(), Ui.F(9.5f, isToday), fg, r);

                // 完成进度小点
                if (scheduled && inMonth && !perfect && done > 0)
                {
                    int dots = Math.Min(done, 3);
                    for (int k = 0; k < dots; k++)
                    {
                        int dx = r.X + r.Width / 2 - (dots - 1) * 4 + k * 8;
                        using (var b = new SolidBrush(Ui.Green))
                            g.FillEllipse(b, dx - 2, r.Bottom - 6, 4, 4);
                    }
                }
                else if (scheduled && inMonth && !perfect && dp.Slots.Count > 0)
                {
                    using (var b = new SolidBrush(Color.FromArgb(60, 120, 140, 180)))
                        g.FillEllipse(b, r.X + r.Width / 2 - 2, r.Bottom - 6, 4, 4);
                }
            }
        }

        Rectangle _prevRect, _nextRect;
        protected override void OnMouseUp(MouseEventArgs e)
        {
            if (_prevRect.Contains(e.Location)) { GoMonth(-1); return; }
            if (_nextRect.Contains(e.Location)) { GoMonth(1); return; }
            base.OnMouseUp(e);
        }
    }
}
