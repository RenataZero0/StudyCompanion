using System;
using System.Drawing;
using System.Windows.Forms;

namespace StudyCompanion
{
    /// <summary>
    /// 发现新版本时的提示窗口。
    ///
    /// 以前是直接弹 MessageBox，把 Release 说明**原样塞进去** ——
    /// `## 标题`、`**加粗**`、``` 代码围栏全露在外面，又长又难读。
    /// 现在改成正经的对话框：左边信息、中间用 MarkdownView 渲染说明（可滚动），
    /// 底下三个按钮永远看得见。
    /// </summary>
    public class UpdateDialog : Form
    {
        public enum Choice { Later, Download, ReleasePage }

        public Choice Result = Choice.Later;

        public UpdateDialog(GitHub.Release rel)
        {
            Text = "发现新版本 " + rel.Tag;
            BackColor = Ui.Bg;
            StartPosition = FormStartPosition.CenterParent;
            ClientSize = new Size(Ui.Px(680), Ui.Px(560));
            MinimumSize = new Size(Ui.Px(460), Ui.Px(360));
            Font = Ui.F(9f);
            AutoScaleMode = AutoScaleMode.None;
            ShowInTaskbar = false;
            MaximizeBox = false;
            MinimizeBox = false;
            try { Icon = AppIcon.Get(32); } catch { }

            // ---------------- 顶栏 ----------------
            var head = new Panel();
            head.Dock = DockStyle.Top;
            head.Height = Ui.Px(84);
            head.BackColor = Color.White;
            head.Paint += delegate (object s, PaintEventArgs e)
            {
                using (var p = new Pen(Ui.Line, 1))
                    e.Graphics.DrawLine(p, 0, head.Height - 1, head.Width, head.Height - 1);
            };
            Controls.Add(head);

            var title = new Label();
            title.Text = "发现新版本 " + rel.Tag;
            title.Font = Ui.F(13f, true);
            title.ForeColor = Ui.Ink;
            title.AutoSize = false;
            title.TextAlign = ContentAlignment.MiddleLeft;
            title.BackColor = Color.White;
            title.SetBounds(Ui.Px(22), Ui.Px(14), head.Width - Ui.Px(44), Ui.Px(28));
            head.Controls.Add(title);

            // 三行信息拆开写成小字，比原来挤在 MessageBox 里清楚
            var info = new Label();
            info.Text = "当前版本 " + GitHub.VersionTag
                      + "　→　最新版本 " + rel.Tag
                      + "　　·　　安装包 " + (rel.ExeSize / 1024) + " KB";
            info.ForeColor = Ui.Sub;
            info.Font = Ui.F(8.5f);
            info.AutoSize = false;
            info.TextAlign = ContentAlignment.MiddleLeft;
            info.BackColor = Color.White;
            info.SetBounds(Ui.Px(24), Ui.Px(44), head.Width - Ui.Px(48), Ui.Px(24));
            head.Controls.Add(info);

            // ---------------- 底部按钮（先加，保证不被内容挤掉）----------------
            var foot = new Panel();
            foot.Dock = DockStyle.Bottom;
            foot.Height = Ui.Px(58);
            foot.BackColor = Color.White;
            foot.Paint += delegate (object s, PaintEventArgs e)
            {
                using (var p = new Pen(Ui.Line, 1))
                    e.Graphics.DrawLine(p, 0, 0, foot.Width, 0);
            };
            Controls.Add(foot);

            var later = new Pill();
            later.Text = "以后再说";
            later.Bounds = new Rectangle(Ui.Px(22), Ui.Px(15), Ui.Px(88), Ui.Px(28));
            later.Click += delegate { Result = Choice.Later; Close(); };
            foot.Controls.Add(later);

            var rel2 = new Pill();
            rel2.Text = "Release 页";
            rel2.Bounds = new Rectangle(Ui.Px(118), Ui.Px(15), Ui.Px(96), Ui.Px(28));
            rel2.Click += delegate { Result = Choice.ReleasePage; Close(); };
            foot.Controls.Add(rel2);

            var go = new Pill();
            go.Text = "下载并安装";
            go.Primary = true;
            go.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            go.Bounds = new Rectangle(foot.Width - Ui.Px(22) - Ui.Px(112), Ui.Px(15), Ui.Px(112), Ui.Px(28));
            go.Click += delegate { Result = Choice.Download; Close(); };
            foot.Controls.Add(go);

            // ---------------- 改动说明 ----------------
            var host = new Panel();
            host.Dock = DockStyle.Fill;
            host.Padding = new Padding(Ui.Px(16), Ui.Px(12), Ui.Px(16), Ui.Px(12));
            host.BackColor = Ui.Bg;
            Controls.Add(host);
            host.BringToFront();

            var card = new Card();
            card.Dock = DockStyle.Fill;
            card.Radius = Ui.Px(14);
            card.Padding = new Padding(Ui.Px(4));
            host.Controls.Add(card);

            var view = new MarkdownView();
            view.Dock = DockStyle.Fill;
            card.Controls.Add(view);

            string notes = rel.Notes == null ? "" : rel.Notes.Trim();
            view.SetMarkdown(notes.Length == 0 ? "（这个版本没有写改动说明）" : notes);
        }
    }
}
