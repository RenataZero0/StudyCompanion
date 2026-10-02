using System;
using System.Drawing;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>内置的更新日志阅读器（自绘 Markdown，不依赖系统对 .md 的文件关联）</summary>
    public class DocViewer : Form
    {
        readonly MarkdownView _view = new MarkdownView();

        /// <summary>自检用：正文长度</summary>
        public int BodyLength { get { return _md == null ? 0 : _md.Length; } }
        string _md = "";

        public DocViewer(string title, string markdown)
        {
            _md = markdown ?? "";
            Text = title;
            BackColor = Ui.Bg;
            StartPosition = FormStartPosition.CenterParent;
            ClientSize = new Size(Ui.Px(760), Ui.Px(660));
            MinimumSize = new Size(Ui.Px(540), Ui.Px(420));
            Font = Ui.F(9f);
            AutoScaleMode = AutoScaleMode.None;
            ShowInTaskbar = false;
            try { Icon = AppIcon.Get(32); } catch { }

            var head = new Panel();
            head.Dock = DockStyle.Top;
            head.Height = Ui.Px(58);
            head.BackColor = Color.White;
            Controls.Add(head);

            var lab = new Label();
            lab.Text = title;
            lab.Font = Ui.F(12f, true);
            lab.ForeColor = Ui.Ink;
            lab.AutoSize = false;
            lab.Dock = DockStyle.Fill;
            lab.TextAlign = ContentAlignment.MiddleLeft;
            lab.Padding = new Padding(Ui.Px(22), 0, 0, 0);
            head.Controls.Add(lab);

            var close = new Pill();
            close.Text = "关闭";
            close.Primary = true;
            close.Click += delegate { Close(); };
            close.Bounds = new Rectangle(head.Width - Ui.Px(104), Ui.Px(15), Ui.Px(80), Ui.Px(28));
            close.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            head.Controls.Add(close);
            close.BringToFront();

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

            _view.SetMarkdown(_md);
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
