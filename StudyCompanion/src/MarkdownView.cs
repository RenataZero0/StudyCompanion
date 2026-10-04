using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 轻量 Markdown 阅读控件（自绘 + 自管滚动）。
    /// 支持：# / ## / ### 标题、- 项目符号、&gt; 引用、**粗体**、`代码`、``` 代码块，
    /// 以及 **Markdown 表格**（带表头底色、行分隔线、列宽自适应）。
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

            // ---- 表格 ----
            public bool Para;                 // 普通段落（续行要接到它后面）
            public string Marker;             // 有序列表的序号（无序列表留 null，画圆点）
            public bool Table;
            public List<string[]> Rows;       // Rows[0] 是表头
            public int[] ColW;                // 每列宽度（含内边距）
            public int[] Align;               // 0 左 1 中 2 右
            public List<int> RowH = new List<int>();
            public List<List<List<string>>> CellLines;   // 每格换行后的文本
        }

        readonly List<Block> _blocks = new List<Block>();
        readonly VScrollBar _bar = new VScrollBar();
        int _contentH;
        int Pad { get { return Ui.Px(22); } }

        // 表格样式
        int CellPadX { get { return Ui.Px(10); } }
        int CellPadY { get { return Ui.Px(7); } }
        static readonly Color HeadBg = ColorTranslator.FromHtml("#F3F5F9");
        static readonly Color BodyInk = ColorTranslator.FromHtml("#3C4A60");

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

            for (int i = 0; i < lines.Length; i++)
            {
                string raw = lines[i];
                string line = raw.TrimEnd();
                if (line.TrimStart().StartsWith("```")) { inCode = !inCode; continue; }

                if (inCode)
                {
                    Add(line, Ui.F(9f), ColorTranslator.FromHtml("#3F6B52"), 0, Ui.Px(10), false, true);
                    continue;
                }

                // ---------------- 表格 ----------------
                if (line.TrimStart().StartsWith("|"))
                {
                    var rows = new List<string[]>();
                    var aligns = new List<int>();
                    int j = i;
                    bool sepSeen = false;
                    while (j < lines.Length && lines[j].TrimStart().StartsWith("|"))
                    {
                        string t = lines[j].Trim();
                        if (IsTableSeparator(t))
                        {
                            sepSeen = true;
                            aligns = ParseAligns(t);
                        }
                        else if (t.Length > 1)
                        {
                            rows.Add(SplitRow(t));
                        }
                        j++;
                    }
                    // 至少要有一行内容，并且有 --- 分隔行才算表格；否则当普通文本
                    if (rows.Count > 0 && sepSeen)
                    {
                        int n = 0;
                        foreach (var r in rows) if (r.Length > n) n = r.Length;
                        for (int k = 0; k < rows.Count; k++)
                        {
                            var r = rows[k];
                            if (r.Length < n)
                            {
                                var bigger = new string[n];
                                Array.Copy(r, bigger, r.Length);
                                for (int q = r.Length; q < n; q++) bigger[q] = "";
                                rows[k] = bigger;
                            }
                        }
                        while (aligns.Count < n) aligns.Add(0);
                        _blocks.Add(new Block { Table = true, Rows = rows, Align = aligns.ToArray(), Font = Ui.F(9.5f) });
                        i = j - 1;
                        first = false;
                        continue;
                    }
                }

                int level = 0;
                string t2 = line;
                while (t2.StartsWith("#")) { level++; t2 = t2.Substring(1); }
                t2 = t2.TrimStart();

                if (level == 1)
                {
                    Add(Plain(t2), Ui.F(17f, true), Ui.Ink, first ? 0 : Ui.Px(10), 0, false, false);
                    first = false;
                    continue;
                }
                if (level == 2)
                {
                    Add(Plain(t2), Ui.F(13.5f, true), Ui.Accent, Ui.Px(26), 0, false, false);
                    first = false;
                    continue;
                }
                if (level >= 3)
                {
                    Add(Plain(t2), Ui.F(11.5f, true), Ui.Ink, Ui.Px(16), 0, false, false);
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
                    Add(Plain(l.Substring(2)), Ui.F(10f), BodyInk, Ui.Px(3), Ui.Px(12), true, false);
                    continue;
                }
                // 有序列表 1. 2. 3. —— 不识别的话后面的项会被当成续行吸收掉
                var om = System.Text.RegularExpressions.Regex.Match(l, @"^(\d{1,3})\.\s+(.*)$");
                if (om.Success)
                {
                    Add(Plain(om.Groups[2].Value), Ui.F(10f), BodyInk, Ui.Px(3), Ui.Px(12), true, false);
                    _blocks[_blocks.Count - 1].Marker = om.Groups[1].Value + ".";
                    continue;
                }
                if (l.StartsWith("> "))
                {
                    Add(Plain(l.Substring(2)), Ui.F(9.5f), Ui.Sub, Ui.Px(4), Ui.Px(12), false, false);
                    continue;
                }
                if (l.Length == 0) { Add("", Ui.F(6f), Ui.Sub, Ui.Px(4), 0, false, false); continue; }

                // lazy continuation：没有块标记的行接到上一段 / 列表项后面
                var prev = _blocks.Count > 0 ? _blocks[_blocks.Count - 1] : null;
                if (prev != null && (prev.Para || prev.Bullet))
                {
                    prev.Text = Join(prev.Text, Plain(l));
                    continue;
                }
                Add(Plain(l), Ui.F(10f), BodyInk, Ui.Px(2), 0, false, false);
                _blocks[_blocks.Count - 1].Para = true;
                first = false;
            }
            Measure();
        }

        static string Plain(string s)
        {
            string t = s.Replace("**", "").Replace("`", "");
            // 过滤控制字符（制表符除外）—— 源文件里混进退格符时，
            // 渲染出来像是少了个字母，很难查。
            var sb = new StringBuilder(t.Length);
            foreach (char c in t) if (c >= 32 || c == '\t') sb.Append(c);
            return sb.ToString();
        }

        /// <summary>
        /// 把续行接到上一段后面。中文之间直接接，英文/数字之间补一个空格。
        /// Markdown 里这叫 lazy continuation —— 列表项在源码中换行时，
        /// 第二行没有 "- " 前缀，但仍然是同一个列表项。
        /// </summary>
        static string Join(string a, string b)
        {
            if (string.IsNullOrEmpty(a)) return b;
            if (string.IsNullOrEmpty(b)) return a;
            char x = a[a.Length - 1], y = b[0];
            bool ax = x < 128, by = y < 128;
            if (ax && by && x != ' ' && y != ' ') return a + " " + b;
            return a + b;
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

        // ------------------------------------------------------------------ 表格小工具
        /// <summary>| --- | :--: | ---: | 这种行</summary>
        static bool IsTableSeparator(string t)
        {
            if (!t.StartsWith("|")) return false;
            bool hasDash = false;
            foreach (char ch in t)
            {
                if (ch == '-') { hasDash = true; continue; }
                if (ch == '|' || ch == ':' || ch == ' ') continue;
                return false;
            }
            return hasDash;
        }

        static List<int> ParseAligns(string sep)
        {
            var list = new List<int>();
            var cells = SplitRow(sep);
            foreach (var c in cells)
            {
                string s = c.Trim();
                bool l = s.StartsWith(":");
                bool r = s.EndsWith(":");
                if (l && r) list.Add(1);
                else if (r) list.Add(2);
                else list.Add(0);
            }
            return list;
        }

        static string[] SplitRow(string t)
        {
            string s = t.Trim();
            if (s.StartsWith("|")) s = s.Substring(1);
            if (s.EndsWith("|")) s = s.Substring(0, s.Length - 1);
            var parts = s.Split('|');
            for (int i = 0; i < parts.Length; i++) parts[i] = Plain(parts[i].Trim());
            return parts;
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

        void MeasureTable(Graphics g, Block b, int avail)
        {
            int n = b.Rows[0].Length;
            var natural = new int[n];
            for (int c = 0; c < n; c++) natural[c] = Ui.Px(46);      // 每列至少这么宽

            // 表头用粗体量，正文用普通体
            using (var hf = new Font(b.Font, FontStyle.Bold))
            {
                foreach (var row in b.Rows)
                {
                    Font f = (row == b.Rows[0]) ? hf : b.Font;
                    for (int c = 0; c < n && c < row.Length; c++)
                    {
                        int wNeed = (int)Math.Ceiling(g.MeasureString(row[c], f).Width) + CellPadX * 2;
                        if (wNeed > natural[c]) natural[c] = wNeed;
                    }
                }
            }

            int sep = 1;
            int total = 0;
            foreach (int v in natural) total += v;
            total += sep * (n - 1);

            b.ColW = new int[n];
            if (total <= avail)
            {
                // 有余量就把多出来的按比例分给各列，别让表格缩在一边
                int extra = avail - total;
                for (int c = 0; c < n; c++)
                    b.ColW[c] = natural[c] + extra * natural[c] / Math.Max(1, total);
                int used = 0;
                foreach (int v in b.ColW) used += v;
                b.ColW[n - 1] += avail - used - sep * (n - 1);
            }
            else
            {
                int budget = avail - sep * (n - 1);
                int minW = Ui.Px(54);
                int sum = 0;
                for (int c = 0; c < n; c++) { b.ColW[c] = Math.Max(minW, natural[c] * budget / Math.Max(1, total)); sum += b.ColW[c]; }
                // 还超就削最宽的那列
                int guard = 0;
                while (sum > budget && guard++ < 200)
                {
                    int widest = 0;
                    for (int c = 1; c < n; c++) if (b.ColW[c] > b.ColW[widest]) widest = c;
                    if (b.ColW[widest] <= minW) break;
                    b.ColW[widest] -= Ui.Px(4);
                    sum -= Ui.Px(4);
                }
            }

            // 每个格子的换行结果 + 行高
            b.CellLines = new List<List<List<string>>>();
            b.RowH.Clear();
            using (var hf = new Font(b.Font, FontStyle.Bold))
            {
                foreach (var row in b.Rows)
                {
                    bool head = (row == b.Rows[0]);
                    Font f = head ? hf : b.Font;
                    int lh = (int)Math.Ceiling(f.GetHeight(g) * 1.35f);
                    var rowCells = new List<List<string>>();
                    int maxLines = 1;
                    for (int c = 0; c < n; c++)
                    {
                        var ls = Wrap(g, c < row.Length ? row[c] : "", f, b.ColW[c] - CellPadX * 2);
                        rowCells.Add(ls);
                        if (ls.Count > maxLines) maxLines = ls.Count;
                    }
                    b.CellLines.Add(rowCells);
                    b.RowH.Add(maxLines * lh + CellPadY * 2);
                }
            }

            int h = 0;
            foreach (int v in b.RowH) h += v;
            b.Height = h + b.Gap;
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
                    if (b.Table)
                    {
                        b.Gap = Ui.Px(16);
                        MeasureTable(g, b, w);
                        b.Y = y;
                        y += b.Height;
                        continue;
                    }
                    int ind = b.Indent + (b.Bullet ? (b.Marker == null ? Ui.Px(16) : Ui.Px(26)) : 0);
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

                if (blk.Table) { DrawTable(g, blk, top, w); continue; }

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
                    if (blk.Marker == null)
                    {
                        using (var b = new SolidBrush(ColorTranslator.FromHtml("#9AA6B8")))
                            g.FillEllipse(b, x + Ui.Px(3), top + lh / 2 - Ui.Px(2), Ui.Px(4), Ui.Px(4));
                        x += Ui.Px(16);
                    }
                    else
                    {
                        using (var b = new SolidBrush(ColorTranslator.FromHtml("#9AA6B8")))
                        using (var mf = new Font(blk.Font, FontStyle.Bold))
                            g.DrawString(blk.Marker, mf, b, x, top);
                        x += Ui.Px(26);
                    }
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

        /// <summary>画一个 Markdown 表格</summary>
        void DrawTable(Graphics g, Block b, int top, int w)
        {
            int n = b.ColW.Length;
            int height = b.Height - b.Gap;
            var outer = new Rectangle(Pad, top, w, height);
            int radius = Ui.Px(8);

            // 底：白色圆角块 + 细边框
            Ui.FillRound(g, outer, radius, Color.White);
            Ui.StrokeRound(g, outer, radius, Ui.Line, 1f);

            using (var bold = new Font(b.Font, FontStyle.Bold))
            {
                // 表头底色（裁剪成圆角，免得四角露出来）
                using (var path = Ui.Round(new Rectangle(Pad, top, w, b.RowH[0] + radius), radius))
                {
                    var old = g.Clip;
                    g.SetClip(path);
                    using (var hb = new SolidBrush(HeadBg))
                        g.FillRectangle(hb, Pad, top, w, b.RowH[0]);
                    g.Clip = old;
                }

                int y = top;
                for (int r = 0; r < b.Rows.Count; r++)
                {
                    bool head = (r == 0);
                    Font f = head ? bold : b.Font;
                    int rh = b.RowH[r];
                    int x = Pad;

                    for (int c = 0; c < n; c++)
                    {
                        int cw = b.ColW[c];

                        // 列分隔线（表头那一行不画，避免和底色叠一起显脏）
                        if (c > 0 && !head)
                        {
                            using (var pen = new Pen(ColorTranslator.FromHtml("#EEF1F6"), 1))
                                g.DrawLine(pen, x, y + Ui.Px(4), x, y + rh - Ui.Px(4));
                        }

                        var ls = b.CellLines[r][c];
                        int lh = (int)Math.Ceiling(f.GetHeight(g) * 1.35f);
                        int textH = ls.Count * lh;
                        int ty = y + (rh - textH) / 2;

                        using (var brush = new SolidBrush(head ? Ui.Ink : BodyInk))
                        {
                            for (int k = 0; k < ls.Count; k++)
                            {
                                float tw = g.MeasureString(ls[k], f).Width;
                                float tx = x + CellPadX;
                                if (b.Align[c] == 1) tx = x + (cw - tw) / 2f;
                                else if (b.Align[c] == 2) tx = x + cw - CellPadX - tw;
                                g.DrawString(ls[k], f, brush, tx, ty + k * lh);
                            }
                        }
                        x += cw;
                    }

                    // 行分隔线
                    y += rh;
                    if (r < b.Rows.Count - 1)
                    {
                        using (var pen = new Pen(Ui.Line, 1))
                            g.DrawLine(pen, Pad + 1, y, Pad + w - 1, y);
                    }
                }
            }
        }
    }
}
