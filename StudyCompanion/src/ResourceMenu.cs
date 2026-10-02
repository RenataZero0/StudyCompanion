using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 卡片上的「配套视频资源」下拉菜单。
    /// 点标签后弹出，列出全部链接；点任意一项直接用默认浏览器打开。
    /// </summary>
    public class ResourceMenu : Form
    {
        static ResourceMenu _open;

        readonly List<string[]> _items;
        readonly int _rowH, _pad, _titleH, _radius, _bottom;
        int _hover = -1;

        public Action<string> OnPick;

        ResourceMenu(List<string[]> items)
        {
            _items = items;
            _pad = Ui.Px(12);          // 外框到内容的留白
            _rowH = Ui.Px(38);         // 每行高度（含行间距）
            _titleH = Ui.Px(42);       // 标题栏高度
            _radius = Ui.Px(12);
            _bottom = Ui.Px(10);

            FormBorderStyle = FormBorderStyle.None;
            ShowInTaskbar = false;
            StartPosition = FormStartPosition.Manual;
            TopMost = true;
            BackColor = Color.White;
            KeyPreview = true;
            Ui.EnableDoubleBuffer(this);
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);

            int w = Ui.Px(285);
            using (var g = CreateGraphics())
            {
                int tw = (int)g.MeasureString("配套教学视频资源", Ui.F(10f, true)).Width + _pad * 2 + Ui.Px(12);
                if (tw > w) w = tw;
                foreach (var it in items)
                {
                    int rw = (int)g.MeasureString(it[0], Ui.F(9.5f)).Width + Ui.Px(70);
                    if (rw > w) w = rw;
                }
            }
            if (w > Ui.Px(470)) w = Ui.Px(470);
            ClientSize = new Size(w, _titleH + items.Count * _rowH + _bottom);
            Region = new Region(Ui.Round(new Rectangle(0, 0, Width, Height), _radius));
        }

        /// <summary>仅供离屏截图自检使用</summary>
        public static ResourceMenu CreateForTest(List<string[]> items) { return new ResourceMenu(items); }

        /// <summary>在 anchor 控件下方弹出菜单（空间不够时自动向上翻）</summary>
        public static void ShowAt(Control anchor, List<string[]> items, Action<string> onPick)
        {
            if (items == null || items.Count == 0) return;
            CloseOpen();

            var m = new ResourceMenu(items);
            m.OnPick = onPick;
            var form = anchor.FindForm();
            if (form != null) m.Owner = form;

            Point p = anchor.PointToScreen(new Point(0, anchor.Height + Ui.Px(4)));
            Rectangle scr = Screen.FromControl(anchor).WorkingArea;
            if (p.X + m.Width > scr.Right) p.X = scr.Right - m.Width - Ui.Px(4);
            if (p.Y + m.Height > scr.Bottom)
                p.Y = anchor.PointToScreen(Point.Empty).Y - m.Height - Ui.Px(4);
            if (p.X < scr.Left) p.X = scr.Left + Ui.Px(4);
            if (p.Y < scr.Top) p.Y = scr.Top + Ui.Px(4);
            m.Location = p;

            _open = m;
            m.Show();
            m.Activate();
        }

        public static void CloseOpen()
        {
            if (_open != null && !_open.IsDisposed)
            {
                var o = _open; _open = null;
                try { o.Close(); } catch { }
            }
        }

        protected override void OnDeactivate(EventArgs e)
        {
            base.OnDeactivate(e);
            Close();
        }

        protected override void OnKeyDown(KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Escape) Close();
            base.OnKeyDown(e);
        }

        int RowAt(Point p)
        {
            if (p.Y < _titleH) return -1;
            int i = (p.Y - _titleH) / _rowH;
            return (i >= 0 && i < _items.Count) ? i : -1;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            int i = RowAt(e.Location);
            if (i != _hover) { _hover = i; Invalidate(); }
            base.OnMouseMove(e);
        }

        protected override void OnMouseLeave(EventArgs e) { _hover = -1; Invalidate(); base.OnMouseLeave(e); }

        protected override void OnMouseUp(MouseEventArgs e)
        {
            int i = RowAt(e.Location);
            if (i >= 0)
            {
                string url = _items[i][1];
                Close();
                if (OnPick != null) OnPick(url);
                return;
            }
            base.OnMouseUp(e);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;

            var full = new Rectangle(0, 0, Width, Height);
            using (var b = new SolidBrush(Color.White)) g.FillRectangle(b, full);

            // 标题
            Ui.Text(g, "配套教学视频资源", Ui.F(10f, true), Ui.Ink, _pad + Ui.Px(4), Ui.Px(13));
            using (var pen = new Pen(Ui.Line, 1))
                g.DrawLine(pen, _pad, _titleH - Ui.Px(9), Width - _pad, _titleH - Ui.Px(9));

            // 条目
            for (int i = 0; i < _items.Count; i++)
            {
                var r = new Rectangle(Ui.Px(6), _titleH + i * _rowH, Width - Ui.Px(12), _rowH - Ui.Px(6));
                bool hov = (i == _hover);
                if (hov) Ui.FillRound(g, r, Ui.Px(8), Ui.AccentSoft);

                var f = Ui.F(9.5f);
                var txt = new Rectangle(r.X + Ui.Px(14), r.Y, r.Width - Ui.Px(50), r.Height);
                Ui.TextVC(g, _items[i][0], f, hov ? Ui.Accent : ColorTranslator.FromHtml("#3C4A60"), txt);

                // 右侧箭头提示
                Ui.TextVC(g, "›", Ui.F(11f, true), hov ? Ui.Accent : ColorTranslator.FromHtml("#C3CCDA"),
                    new Rectangle(r.Right - Ui.Px(32), r.Y, Ui.Px(18), r.Height));
            }

            // 外框
            using (var pen = new Pen(ColorTranslator.FromHtml("#D8DEE9"), 1))
            using (var path = Ui.Round(new Rectangle(0, 0, Width - 1, Height - 1), _radius))
                g.DrawPath(pen, path);
        }
    }
}
