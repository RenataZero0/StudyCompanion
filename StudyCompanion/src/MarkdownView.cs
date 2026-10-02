using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 轻量 Markdown 阅读控件（自绘 + 自管滚动）。
    /// 支持：# / ## / ### 标题、- 项目符号、&gt; 引用、行内 **粗体** 与 `代码`、``` 代码块。
    /// </summary>
    public class MarkdownView : Control
    {
        class Block
        {
            public string Text = "";
            public Font Font;
            public Color Color;
            public int Gap;
            public int Indent;
            public bool Bullet;
            public bool Code;
            public bool Divider;
            public List<string> Lines = new List<string>();
            public int Y;
            public int Height;
        }

        readonly List<Block> _blocks = new List<Block>();
        readonly VScrollBar _bar = new VScrollBar();
        int _contentH;
        int Pad { get { return Ui.Px(22); } }

        public MarkdownView()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint |
                     ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
            BackColor = Color.White;
            _bar.Dock = DockStyle.Right;
            _bar.Width = Ui.Px(12);
            _bar.SmallChange = Ui.Px(30);
            _bar.LargeChange = Ui.Px(120);
            _bar.Scroll += delegate { Invalidate(); };
            Controls.Add(_bar);
        }

        // ------------------------------------------------------------------ 解析
        void Add(string text, Font f, Color c, int gap, int indent, bool bullet, bool code)
        {
            if (text.Length == 0 && !bullet) { _blocks.Add(new Block { Text = "", Font = f, Color = c, Gap = Ui.Px(6) }); return; }
            _blocks.Add(new Block { Text = text, Font = f, Color = c, Gap = gap, Indent = indent, Bullet = bullet, Code = code });
        }

        public void SetMarkdown(string md)
        {
            _blocks.Clear();
            if (string.IsNullOrEmpty(md)) { Measure(); return; }

            var lines = md.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n');
            bool inCode = false;
            bool first = true;

            foreach (var raw in lines)
            {
                string line = raw.TrimEnd();
                if (line.TrimStart().StartsWith("```")) { inCode = !inCode; continue; }

                if (inCode)
                {
                    Add(line, Ui.F(9f), ColorTranslator.FromHtml("#3F6B52"), 0, Ui.Px(10), false, true);
                    continue;
                }

                int level = 0;
                string t = line;
                while (t.StartsWith("#")) { level++; t = t.Substring(1); }
                t = t.TrimStart();

                if (level == 1)
                {
                    Add(Plain(t), Ui.F(17f, true), Ui.Ink, first ? 0 : Ui.Px(10), 0, false, false);
                    first = false;
                    continue;
                }
                if (level == 2)
                {
                    Add(Plain(t), Ui.F(13.5f, true), Ui.Accent, Ui.Px(26), 0, false, false);
                    first = false;
                    continue;
                }
                if (level >= 3)
                {
                    Add(Plain(t), Ui.F(11.5f, true), Ui.Ink, Ui.Px(16), 0, false, false);
                    first = false;
                    continue;
                }

                string l = line.TrimStart();
                if (IsRule(l))
                {
                    _blocks.Add(new Block { Divider = true, Font = Ui.F(4f), Gap = Ui.Px(18), Height = Ui.Px(22), Lines = new List<string> { "" } });
                    continue;
                }
                if (l.StartsWith("- ") || l.StartsWith("* "))
                {
                    Add(Plain(l.Substring(2)), Ui.F(10f), ColorTranslator.FromHtml("#3C4A60"), Ui.Px(3), Ui.Px(12), true, false);
                    continue;
                }
                if (l.StartsWith("> "))
                {
                    Add(Plain(l.Substring(2)), Ui.F(9.5f), Ui.Sub, Ui.Px(4), Ui.Px(12), false, false);
                    continue;
                }
                if (l.StartsWith("|"))
                {
                    Add(Plain(l), Ui.F(8.5f), Ui.Sub, 0, Ui.Px(6), false, false);
                    continue;
                }
                if (l.Length == 0) { Add("", Ui.F(6f), Ui.Sub, Ui.Px(4), 0, false, false); continue; }

                Add(Plain(l), Ui.F(10f), ColorTranslator.FromHtml("#3C4A60"), Ui.Px(2), 0, false, false);
                first = false;
            }
            Measure();
        }

        static string Plain(string s)
        {
            return s.Replace("**", "").Replace("`", "");
        }

        /// <summary>是不是 --- / *** / ___ 这种分隔线</summary>
        static bool IsRule(string l)
        {
            string t = l.Trim();
            if (t.Length < 3) return false;
            char c = t[0];
            if (c != '-' && c != '*' && c != '_') return false;
            foreach (char ch in t) if (ch != c && ch != ' ') return false;
            return true;
        }

        // ------------------------------------------------------------------ 排版测量
        static List<string> Wrap(Graphics g, string text, Font f, int maxW)
        {
            var res = new List<string>();
            if (maxW < Ui.Px(40)) maxW = Ui.Px(40);
            if (text.Length == 0) { res.Add(""); return res; }
            string cur = "";
            foreach (char ch in text)
            {
                string next = cur + ch;
                if (g.MeasureString(next, f).Width > maxW && cur.Length > 0)
                {
                    res.Add(cur);
                    cur = ch.ToString();
                }
                else cur = next;
            }
            res.Add(cur);
            return res;
        }

        void Measure()
        {
            int w = ClientSize.Width - Pad * 2 - _bar.Width;
            if (w < Ui.Px(80)) w = Ui.Px(80);
            using (var g = CreateGraphics())
            {
                int y = Pad;
                foreach (var b in _blocks)
                {
                    int ind = b.Indent + (b.Bullet ? Ui.Px(16) : 0);
                    b.Lines = Wrap(g, b.Text, b.Font, w - ind);
                    int lh = (int)Math.Ceiling(b.Font.GetHeight(g) * (b.Code ? 1.15f : 1.42f));
                    b.Height = b.Lines.Count * lh + b.Gap;
                    b.Y = y;
                    y += b.Height;
                }
                _contentH = y + Pad;
            }
            UpdateBar();
        }

        void UpdateBar()
        {
            int view = ClientSize.Height;
            int max = Math.Max(0, _contentH - view);
            _bar.Minimum = 0;
            _bar.Maximum = Math.Max(0, max + _bar.LargeChange - 1);
            _bar.Visible = max > 0;
            if (_bar.Value > max) _bar.Value = max;
        }

        protected override void OnResize(EventArgs e)
        {
            base.OnResize(e);
            Measure();
        }

        protected override void OnMouseWheel(MouseEventArgs e)
        {
            int max = Math.Max(0, _contentH - ClientSize.Height);
            int v = _bar.Value - e.Delta / 120 * Ui.Px(60);
            _bar.Value = Math.Max(0, Math.Min(max, v));
            Invalidate();
            base.OnMouseWheel(e);
        }

        int ScrollY
        {
            get
            {
                int max = Math.Max(0, _contentH - ClientSize.Height);
                return Math.Max(0, Math.Min(max, _bar.Value));
            }
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.ClearTypeGridFit;
            using (var b = new SolidBrush(Color.White)) g.FillRectangle(b, ClientRectangle);

            int sy = ScrollY;
            int w = ClientSize.Width - Pad * 2 - _bar.Width;

            foreach (var blk in _blocks)
            {
                int top = blk.Y - sy;
                if (top + blk.Height < 0 || top > ClientSize.Height) continue;

                int lh = blk.Lines.Count > 0 ? blk.Height / blk.Lines.Count : Ui.Px(16);
                int x = Pad + blk.Indent;

                if (blk.Divider)
                {
                    using (var pen = new Pen(Ui.Line, 1))
                        g.DrawLine(pen, Pad, top + blk.Height / 2, Pad + w, top + blk.Height / 2);
                    continue;
                }
                if (blk.Bullet)
                {
                    using (var b = new SolidBrush(ColorTranslator.FromHtml("#9AA6B8")))
                        g.FillEllipse(b, x + Ui.Px(3), top + lh / 2 - Ui.Px(2), Ui.Px(4), Ui.Px(4));
                    x += Ui.Px(16);
                }
                if (blk.Code)
                {
                    var r = new Rectangle(Pad, top - Ui.Px(2), w, blk.Height - Ui.Px(2));
                    Ui.FillRound(g, r, Ui.Px(6), ColorTranslator.FromHtml("#F3F7F4"));
                }

                using (var b = new SolidBrush(blk.Color))
                    for (int i = 0; i < blk.Lines.Count; i++)
                        g.DrawString(blk.Lines[i], blk.Font, b, x, top + i * lh);
            }
        }
    }
}
