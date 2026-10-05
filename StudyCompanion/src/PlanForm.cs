using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 「今天这几步怎么做」窗口：把某一天某一个时段展开成带时间戳的一步一步，
    /// 每步都能打勾，勾选记在 progress.tsv 里（会跟着 GitHub 一起同步）。
    /// </summary>
    public class PlanForm : Form
    {
        readonly Slot _slot;
        readonly DateTime _date;
        readonly int _slotIndex;
        readonly Canvas _canvas;
        readonly Panel _scroll = new Panel();
        readonly Label _progress = new Label();

        public PlanForm(DateTime date, Slot slot, int slotIndex)
        {
            _date = date; _slot = slot; _slotIndex = slotIndex;

            Text = "今天这几步怎么做";
            FormBorderStyle = FormBorderStyle.Sizable;
            StartPosition = FormStartPosition.CenterParent;
            MinimizeBox = false;
            ShowInTaskbar = false;
            MinimumSize = new Size(Ui.Px(520), Ui.Px(420));
            ClientSize = new Size(Ui.Px(680), Ui.Px(620));
            BackColor = Color.White;
            Font = Ui.F(9.5f);
            try { Icon = AppIcon.Get(32); } catch { }

            var title = new Label();
            title.Text = "今天这几步怎么做";
            title.Font = Ui.F(14f, true);
            title.ForeColor = Ui.Ink;
            title.SetBounds(Ui.Px(22), Ui.Px(16), Ui.Px(500), Ui.Px(28));
            Controls.Add(title);

            _progress.Font = Ui.F(10f, true);
            _progress.ForeColor = Ui.Green;
            _progress.TextAlign = ContentAlignment.MiddleRight;
            _progress.SetBounds(ClientSize.Width - Ui.Px(22) - Ui.Px(200), Ui.Px(20), Ui.Px(200), Ui.Px(24));
            Controls.Add(_progress);

            var sub = new Label();
            sub.Text = date.ToString("yyyy-MM-dd") + "　" + slot.Start + " – " + slot.End +
                       "　" + slot.Subject + "　" + slot.Title;
            sub.Font = Ui.F(9f);
            sub.ForeColor = Ui.Sub;
            sub.AutoEllipsis = true;
            sub.SetBounds(Ui.Px(22), Ui.Px(46), ClientSize.Width - Ui.Px(44), Ui.Px(22));
            Controls.Add(sub);

            var line = new Panel();
            line.BackColor = Ui.Line;
            line.SetBounds(Ui.Px(22), Ui.Px(74), ClientSize.Width - Ui.Px(44), 1);
            Controls.Add(line);

            var steps = DailyPlan.Steps(slot);

            _canvas = new Canvas();
            _canvas.Steps = steps;
            _canvas.Iso = date.ToString("yyyy-MM-dd");
            _canvas.SlotIndex = slotIndex;
            _canvas.Changed += delegate { UpdateProgress(); };

            _scroll.SetBounds(Ui.Px(12), Ui.Px(84), ClientSize.Width - Ui.Px(24), ClientSize.Height - Ui.Px(84) - Ui.Px(60));
            _scroll.BackColor = Color.White;
            _scroll.AutoScroll = true;
            _scroll.Controls.Add(_canvas);
            _canvas.SetBounds(0, 0, Math.Max(Ui.Px(240), _scroll.ClientSize.Width), Ui.Px(400));
            Controls.Add(_scroll);

            var footer = new Panel();
            footer.BackColor = Color.White;
            footer.SetBounds(0, ClientSize.Height - Ui.Px(56), ClientSize.Width, Ui.Px(56));
            Controls.Add(footer);

            var close = new Pill();
            close.Text = "关闭";
            close.Primary = true;
            close.SetBounds(ClientSize.Width - Ui.Px(22) - Ui.Px(96), Ui.Px(12), Ui.Px(96), Ui.Px(32));
            close.Click += delegate { Close(); };
            footer.Controls.Add(close);

            var hint = new Label();
            hint.Text = "点一行就能打勾；勾选会自动同步到手机。";
            hint.Font = Ui.F(8.5f);
            hint.ForeColor = Ui.Sub;
            hint.SetBounds(Ui.Px(22), Ui.Px(18), ClientSize.Width - Ui.Px(160), Ui.Px(20));
            footer.Controls.Add(hint);

            UpdateProgress();
        }

        protected override void OnResize(EventArgs e)
        {
            base.OnResize(e);
            if (_scroll == null || _canvas == null) return;
            _scroll.SetBounds(Ui.Px(12), Ui.Px(84),
                ClientSize.Width - Ui.Px(24), ClientSize.Height - Ui.Px(84) - Ui.Px(60));
            _canvas.Width = Math.Max(Ui.Px(240), _scroll.ClientSize.Width);
            _canvas.Relayout();
        }

        void UpdateProgress()
        {
            int total = 0, done = 0;
            foreach (var s in _canvas.Steps) { if (s.Head) continue; total++; }
            done = DailyPlan.DoneCount(_date.ToString("yyyy-MM-dd"), _slotIndex, _canvas.Steps.Count);
            _progress.Text = total == 0 ? "" : ("已完成 " + done + " / " + total + " 步");
            _progress.ForeColor = (total > 0 && done >= total) ? Ui.Green : Ui.Sub;
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            base.OnFormClosed(e);
            MainForm.NotifyDataChanged();
        }

        // ==================================================================== 步骤列表
        class Canvas : Control
        {
            public List<DailyPlan.Step> Steps = new List<DailyPlan.Step>();
            public string Iso = "";
            public int SlotIndex;
            public event EventHandler Changed;

            // 每次布局算出来的：每一步占的整行矩形（头部提示行没有勾选框，不可点）
            readonly List<RectangleF> _rows = new List<RectangleF>();
            readonly List<bool> _rowClickable = new List<bool>();

            int _contentH;
            int _hover = -1;

            public Canvas()
            {
                SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                         ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
                BackColor = Color.White;
            }

            protected override void OnHandleCreated(EventArgs e)
            {
                base.OnHandleCreated(e);
                Relayout();
            }

            public void Relayout()
            {
                using (var g = CreateGraphics())
                    Build(g, Math.Max(Ui.Px(240), ClientSize.Width));
                if (Height != _contentH) Height = _contentH;
                Invalidate();
            }

            /// <summary>算出来每一步的位置，顺便把总高度写进 _contentH</summary>
            void Build(Graphics g, int w)
            {
                _rows.Clear(); _rowClickable.Clear();
                int y = Ui.Px(6);
                int pad = Ui.Px(14);
                int box = Ui.Px(20);
                int timeW = Ui.Px(74);
                int gap = Ui.Px(12);
                int xTime = pad + box + gap;
                int xText = xTime + timeW + gap;
                int textW = Math.Max(Ui.Px(120), w - xText - pad);

                var fText = Ui.F(10.5f, true);
                var fNote = Ui.F(9f);
                var fHead = Ui.F(10f, true);

                foreach (var s in Steps)
                {
                    float h;
                    if (s.Head)
                    {
                        h = Ui.Px(30);
                        _rows.Add(new RectangleF(pad, y, w - pad * 2, h));
                        _rowClickable.Add(false);
                        y += (int)h + Ui.Px(2);
                        continue;
                    }
                    float th = g.MeasureString(s.Text, fText, textW).Height;
                    float nh = s.Note.Length > 0 ? g.MeasureString(s.Note, fNote, textW).Height + Ui.Px(3) : 0;
                    h = Math.Max(Ui.Px(36), th + nh + Ui.Px(12));
                    _rows.Add(new RectangleF(pad, y, w - pad * 2, h));
                    _rowClickable.Add(true);
                    y += (int)h;
                }
                _contentH = y + Ui.Px(10);
            }

            protected override void OnPaint(PaintEventArgs e)
            {
                var g = e.Graphics;
                g.SmoothingMode = SmoothingMode.AntiAlias;
                g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
                g.Clear(Color.White);

                int w = Math.Max(Ui.Px(240), ClientSize.Width);
                Build(g, w);

                int pad = Ui.Px(14);
                int box = Ui.Px(20);
                int timeW = Ui.Px(74);
                int gap = Ui.Px(12);
                int xTime = pad + box + gap;
                int xText = xTime + timeW + gap;
                int textW = Math.Max(Ui.Px(120), w - xText - pad);

                var fText = Ui.F(10.5f, true);
                var fNote = Ui.F(9f);
                var fHead = Ui.F(10f, true);
                var fTime = Ui.F(10f, true);
                var fMin = Ui.F(8.5f);

                for (int i = 0; i < Steps.Count && i < _rows.Count; i++)
                {
                    var s = Steps[i];
                    var r = _rows[i];

                    if (s.Head)
                    {
                        var band = new Rectangle((int)r.X - Ui.Px(4), (int)r.Y, (int)r.Width + Ui.Px(8), (int)r.Height);
                        Ui.FillRound(g, band, Ui.Px(8), Ui.AccentSoft);
                        Ui.TextVC(g, s.Text, fHead, Ui.Accent,
                            new Rectangle(band.X + Ui.Px(10), band.Y, band.Width - Ui.Px(14), band.Height));
                        continue;
                    }

                    bool done = DailyPlan.Done(Iso, SlotIndex, i);

                    // 悬停底色
                    if (i == _hover)
                    {
                        var hl = new Rectangle((int)r.X - Ui.Px(6), (int)r.Y + 1,
                                               (int)r.Width + Ui.Px(12), (int)r.Height - 2);
                        Ui.FillRound(g, hl, Ui.Px(9), ColorTranslator.FromHtml("#F5F8FE"));
                    }

                    // 勾选框
                    var bx = new Rectangle(pad, (int)r.Y + Ui.Px(6), box, box);
                    if (done)
                    {
                        Ui.FillRound(g, bx, Ui.Px(6), Ui.Green);
                        using (var p = new Pen(Color.White, Math.Max(1.6f, Ui.S * 1.8f)))
                        {
                            p.StartCap = LineCap.Round; p.EndCap = LineCap.Round;
                            g.DrawLines(p, new PointF[]
                            {
                                new PointF(bx.X + bx.Width * 0.24f, bx.Y + bx.Height * 0.52f),
                                new PointF(bx.X + bx.Width * 0.43f, bx.Y + bx.Height * 0.71f),
                                new PointF(bx.X + bx.Width * 0.77f, bx.Y + bx.Height * 0.30f),
                            });
                        }
                    }
                    else
                    {
                        Ui.StrokeRound(g, bx, Ui.Px(6), ColorTranslator.FromHtml("#C7D0DE"), 1.4f);
                    }

                    // 时间 + 时长（不定时的「合格线 / 说明」行不显示时间，只留一个破折号占位）
                    int ty = (int)r.Y + Ui.Px(3);
                    if (s.Untimed)
                    {
                        Ui.Text(g, "——", fTime, ColorTranslator.FromHtml("#C7D0DE"), xTime, ty);
                    }
                    else
                    {
                        Ui.Text(g, s.Time, fTime, done ? Ui.Sub : Ui.Ink, xTime, ty);
                        Ui.Text(g, s.Minutes + "′", fMin, Ui.Sub,
                            xTime + (int)g.MeasureString(s.Time, fTime).Width + Ui.Px(4), ty + Ui.Px(4));
                    }

                    // 正文
                    Color tc = done ? ColorTranslator.FromHtml("#8A9BAE") : Ui.Ink;
                    using (var b = new SolidBrush(tc))
                        g.DrawString(s.Text, fText, b, new RectangleF(xText, r.Y + Ui.Px(3), textW, r.Height));
                    if (s.Note.Length > 0)
                    {
                        float th = g.MeasureString(s.Text, fText, textW).Height;
                        using (var b = new SolidBrush(done ? ColorTranslator.FromHtml("#AEBBC9") : Ui.Sub))
                            g.DrawString(s.Note, fNote, b,
                                new RectangleF(xText, r.Y + Ui.Px(3) + th + Ui.Px(3), textW, r.Height));
                    }
                }

                // 最浅的分隔线
                using (var p = new Pen(ColorTranslator.FromHtml("#F0F3F8")))
                    for (int i = 0; i + 1 < _rows.Count; i++)
                        if (!Steps[i].Head && !Steps[i + 1].Head)
                        {
                            float yy = _rows[i].Bottom;
                            g.DrawLine(p, xTime, yy, w - pad, yy);
                        }
            }

            protected override void OnMouseMove(MouseEventArgs e)
            {
                base.OnMouseMove(e);
                int hit = RowAt(e.Y);
                if (hit != _hover) { _hover = hit; Invalidate(); }
                Cursor = (hit >= 0 && _rowClickable[hit]) ? Cursors.Hand : Cursors.Default;
            }

            protected override void OnMouseLeave(EventArgs e)
            {
                base.OnMouseLeave(e);
                if (_hover != -1) { _hover = -1; Invalidate(); }
            }

            int RowAt(int y)
            {
                for (int i = 0; i < _rows.Count; i++)
                    if (y >= _rows[i].Y && y < _rows[i].Bottom) return i;
                return -1;
            }

            protected override void OnMouseClick(MouseEventArgs e)
            {
                base.OnMouseClick(e);
                int i = RowAt(e.Y);
                if (i < 0 || !_rowClickable[i]) return;
                if (i >= Steps.Count) return;
                string iso = Iso;
                bool now = !DailyPlan.Done(iso, SlotIndex, i);
                DailyPlan.SetDone(iso, SlotIndex, i, now);
                Invalidate();
                if (Changed != null) Changed(this, EventArgs.Empty);
            }
        }
    }
}
