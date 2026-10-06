using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace StudyCompanion
{
    static class Program
    {
        [DllImport("user32.dll")]
        static extern bool SetProcessDPIAware();

        [STAThread]
        static void Main(string[] args)
        {
            bool selftest = false, testtoast = false, tray = false, layoutTest = false, closeTest = false, planTest = false;
            string shot = null, shotDate = null, mdFile = null, dlgShot = null, dlgName = null;
            for (int i = 0; i < args.Length; i++)
            {
                var a = args[i];
                if (a == "--selftest") selftest = true;
                else if (a == "--testtoast") testtoast = true;
                else if (a == "--tray") tray = true;
                else if (a == "--layouttest") layoutTest = true;
                else if (a == "--closetest") closeTest = true;
                else if (a == "--plantest") planTest = true;
                else if (a == "--dlgshot" && i + 2 < args.Length) { dlgName = args[i + 1]; dlgShot = args[i + 2]; i += 2; }
                else if (a == "--shot" && i + 1 < args.Length) shot = args[i + 1];
                else if (a == "--date" && i + 1 < args.Length) shotDate = args[i + 1];
                else if (a == "--md" && i + 1 < args.Length) mdFile = args[i + 1];
            }

            try { SetProcessDPIAware(); } catch { }

            // 必须早于任何网络请求：打开 TLS1.2，否则连 GitHub 会报「未能创建 SSL/TLS 安全通道」
            try { GitHub.Init(); } catch { }

            try
            {
                using (var g = Graphics.FromHwnd(IntPtr.Zero))
                    Ui.S = g.DpiX / 96f;
            }
            catch { Ui.S = 1f; }

            if (selftest) { SelfTest(shotDate); return; }

            if (closeTest) { CloseTest(); return; }

            if (layoutTest) { LayoutTest(); return; }

            if (planTest) { PlanTest(shotDate); return; }

            if (dlgShot != null) { DialogShot(dlgName, dlgShot); return; }

            if (shot != null) { Snapshot(shot, shotDate, mdFile); return; }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            bool created;
            using (var mutex = new Mutex(true, "StudyCompanion_SingleInstance", out created))
            {
                if (!created)
                {
                    MessageBox.Show("学习助手已经在运行了（请查看右下角托盘图标）。", "学习助手",
                        MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }
                ScheduleData.Load(true);
                Store.Load();

                if (testtoast)
                {
                    var t = new ToastForm("该学习啦！　21:00–22:00",
                        "纯数　新课 P1 Ch7 微分 §7.1、§7.2\n点「开始学习」查看今天的任务和配套视频。",
                        25, "开始学习", delegate { }, false);
                    Application.Run(t);
                    return;
                }

                // 自动检查更新必须在这里起 —— 开机自启是 --tray，
                // 只建托盘图标、不建主窗口，放在 MainForm 里就永远不会执行
                var marshal = new Form();
                var h = marshal.Handle;          // 逼出句柄但不显示，只用来切回 UI 线程
                Updater.Start(marshal);

                Application.Run(new TrayApp(tray));
            }
        }

        // ------------------------------------------------------------------ 离屏截图（自检用，不显示窗口）
        static void Snapshot(string path, string dateArg, string mdFile)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            ScheduleData.Load(true);
            Store.Load();

            if (Path.GetFileName(path).ToLower().Contains("toast"))
            {
                var t = new ToastForm("已最小化到托盘",
                    "程序仍在后台运行，到点会提醒你。",
                    3, "", null, true);
                t.ShowInTaskbar = false;
                t.StartPosition = FormStartPosition.Manual;
                t.Location = new Point(-4000, -4000);
                t.Show();
                for (int i = 0; i < 40; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(25); }
                var tb = new Bitmap(t.Width, t.Height);
                t.DrawToBitmap(tb, new Rectangle(0, 0, t.Width, t.Height));
                tb.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                tb.Dispose();
                Console.WriteLine("toast snapshot -> " + path);
                return;
            }

            // 升级对话框：离屏渲染一下，方便检查排版
            if (Path.GetFileName(path).ToLower().Contains("update"))
            {
                var rel = new GitHub.Release();
                rel.Tag = "v9.9.9";
                rel.Name = "测试版本";
                rel.PageUrl = "https://github.com/RenataZero0/StudyCompanion/releases";
                rel.ExeSize = 211456;
                rel.Notes = Changelog.SectionOf(Changelog.Local(),
                    Changelog.LatestVersionIn(Changelog.Local()));
                if (string.IsNullOrEmpty(rel.Notes)) rel.Notes = "（没有改动说明）";

                var d = new UpdateDialog(rel);
                d.ShowInTaskbar = false;
                d.StartPosition = FormStartPosition.Manual;
                d.Location = new Point(-4000, -4000);
                d.Show();
                for (int i = 0; i < 40; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(20); }
                using (var b = new Bitmap(d.Width, d.Height))
                {
                    d.DrawToBitmap(b, new Rectangle(0, 0, d.Width, d.Height));
                    b.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                }
                d.Close();
                Console.WriteLine("update snapshot -> " + path + "  | 说明长度=" + rel.Notes.Length);
                return;
            }

            if (Path.GetFileName(path).ToLower().Contains("doc"))
            {
                // 有 --md 就直接渲染指定文件（用来单独测 Markdown 渲染，不联网）
                // 否则先同步拉一次，这样截图内容是确定的，也顺便验证了自动更新这条链路
                string err = "";
                bool ok;
                string md;
                if (!string.IsNullOrEmpty(mdFile) && File.Exists(mdFile))
                {
                    md = File.ReadAllText(mdFile, Encoding.UTF8);
                    ok = true;
                    err = "(用了 --md)";
                }
                else
                {
                    ok = Changelog.Fetch(out err);
                    md = Changelog.Local();
                }
                if (string.IsNullOrEmpty(md)) md = "（没有找到更新日志）";
                var v = new DocViewer("更新日志 · StudyCompanion", md, string.IsNullOrEmpty(mdFile));
                v.ShowInTaskbar = false;
                v.StartPosition = FormStartPosition.Manual;
                v.Location = new Point(-4000, -4000);
                v.Show();
                for (int i = 0; i < 40; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(20); }
                int blen = v.BodyLength;
                using (var b = v.Snapshot()) b.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                v.Close();
                Console.WriteLine("doc snapshot -> " + path
                    + "  | 拉取=" + (ok ? "成功" : "失败(" + err + ")")
                    + "  | 缓存=" + Changelog.HasCache
                    + "  | 最新版本=" + Changelog.LatestVersionIn(md)
                    + "  | 正文字符数=" + blen);
                
                return;
            }

            if (Path.GetFileName(path).ToLower().Contains("menu"))
            {
                var dp0 = ScheduleData.GetDay(DateTime.Today);
                var items = new List<string[]>();
                if (dp0 != null && dp0.Slots.Count > 0) items = VideoLinks.ForSlot(dp0.Slots[0]);
                if (items.Count == 0)
                    items = new List<string[]> {
                        new[]{"A Level Physics Online","https://www.alevelphysicsonline.com/"},
                        new[]{"MIT 8.01 经典力学","https://ocw.mit.edu/"},
                        new[]{"▶ YouTube 搜索","https://www.youtube.com/"},
                        new[]{"▶ Bilibili 搜索（免翻墙）","https://search.bilibili.com/"} };
                var m = ResourceMenu.CreateForTest(items);
                m.StartPosition = FormStartPosition.Manual;
                m.Location = new Point(-4000, -4000);
                m.Show();
                for (int i = 0; i < 30; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(20); }
                var mb = new Bitmap(m.Width, m.Height);
                m.DrawToBitmap(mb, new Rectangle(0, 0, m.Width, m.Height));
                mb.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                mb.Dispose();
                m.Close();
                Console.WriteLine("menu snapshot -> " + path);
                return;
            }

            var f = new MainForm(false);
            if (!string.IsNullOrEmpty(dateArg))
            {
                DateTime dd;
                if (DateTime.TryParse(dateArg, out dd)) f.SetViewDate(dd);
            }
            f.ShowInTaskbar = false;
            f.Opacity = 0;
            f.StartPosition = FormStartPosition.Manual;
            f.Location = new Point(-4000, -4000);
            f.Show();
            for (int i = 0; i < 40; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(25); }
            var bmp = new Bitmap(f.Width, f.Height);
            f.DrawToBitmap(bmp, new Rectangle(0, 0, f.Width, f.Height));
            bmp.Save(path, System.Drawing.Imaging.ImageFormat.Png);
            bmp.Dispose();
            f.ForceClose = true;
            f.Close();
            Console.WriteLine("snapshot -> " + path);
        }

        // ------------------------------------------------------------------ 关闭行为测试
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        static extern bool IsWindowVisible(IntPtr hWnd);
        const int WM_CLOSE = 0x0010;

        static void Pump(int n)
        {
            for (int i = 0; i < n; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(20); }
        }

        static void CloseTest()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            ScheduleData.Load(true);
            Store.Load();
            TrayApp.SuppressToast = true;          // 测试时不弹提示窗
            var sb = new StringBuilder();
            bool oldSetting = Store.CloseToTray;

            // ---- 场景 1：关闭时 = 最小化到托盘 ----
            Store.CloseToTray = true;
            var f1 = new MainForm(false);
            f1.ShowInTaskbar = false;
            f1.Opacity = 0;
            f1.StartPosition = FormStartPosition.Manual;
            f1.Location = new Point(-4000, -4000);
            f1.Show();
            Pump(20);
            IntPtr h1 = f1.Handle;
            f1.Close();   // 与点 X 同一条 WM_CLOSE(lParam=0) 路径
            Pump(30);
            bool v1 = f1.Visible, t1 = f1.ShowInTaskbar, w1 = IsWindowVisible(h1);
            bool ok1 = !v1 && !t1 && !w1 && !f1.IsDisposed;
            sb.AppendLine("【场景 1】设置 = 最小化到托盘");
            sb.AppendLine("  窗体 Visible      = " + v1 + "    期望 False");
            sb.AppendLine("  ShowInTaskbar     = " + t1 + "    期望 False");
            sb.AppendLine("  系统认为窗口可见  = " + w1 + "    期望 False");
            sb.AppendLine("  窗体已销毁        = " + f1.IsDisposed + "    期望 False（进程要留着提醒）");
            sb.AppendLine("  [诊断] CloseReason = " + f1.DiagCloseReason + "  OnFormClosing 次数 = " + f1.DiagCloseCount + "  HideToTray 执行 = " + f1.DiagHidToTray + "  CloseToTray=" + Store.CloseToTray);
            sb.AppendLine("  → " + (ok1 ? "✔ 正确隐藏到托盘（窗口没有残留在屏幕上）" : "✘ 仍有残留"));
            f1.ForceClose = true; f1.Close(); Pump(10);

            // ---- 场景 2：关闭时 = 直接退出 ----
            Store.CloseToTray = false;
            var f2 = new MainForm(false);
            f2.ShowInTaskbar = false;
            f2.Opacity = 0;
            f2.StartPosition = FormStartPosition.Manual;
            f2.Location = new Point(-4000, -4000);
            f2.Show();
            Pump(20);
            IntPtr h2 = f2.Handle;
            f2.Close();
            Pump(30);
            bool w2 = IsWindowVisible(h2);     // 句柄已销毁时返回 False
            bool ok2 = f2.IsDisposed && !w2;
            sb.AppendLine();
            sb.AppendLine("【场景 2】设置 = 直接退出");
            sb.AppendLine("  窗体已销毁        = " + f2.IsDisposed + "    期望 True");
            sb.AppendLine("  系统认为窗口可见  = " + w2 + "    期望 False");
            sb.AppendLine("  → " + (ok2 ? "✔ 正常退出" : "✘ 未正确退出"));

            Store.CloseToTray = oldSetting;
            TrayApp.SuppressToast = false;

            string p = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "closetest.txt");
            File.WriteAllText(p, sb.ToString(), new UTF8Encoding(true));
            Console.WriteLine("closetest -> " + p);
        }

        // ------------------------------------------------------------------ 布局稳定性测试
        static void LayoutTest()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            ScheduleData.Load(true);
            Store.Load();
            var f = new MainForm(false);
            f.ShowInTaskbar = false;
            f.Opacity = 0;
            f.StartPosition = FormStartPosition.Manual;
            f.Location = new Point(-4000, -4000);
            f.Show();
            for (int i = 0; i < 40; i++) { Application.DoEvents(); System.Threading.Thread.Sleep(20); }

            var log = new StringBuilder();
            var sep = new[] { " | " };
            string baseLine = f.LayoutProbe();
            log.AppendLine("[1] 初始布局：");
            log.AppendLine("    " + baseLine);

            int unstable = 0;
            for (int i = 0; i < 8; i++)
            {
                f.RefreshAll();
                Application.DoEvents();
                string now = f.LayoutProbe();
                if (now != baseLine) { unstable++; log.AppendLine("    !! 第 " + (i + 1) + " 次刷新后布局变化：" + now); }
            }
            log.AppendLine("[2] 连续刷新 8 次，布局发生变化次数 = " + unstable + "  (期望 0)");

            var d0 = DateTime.Today;
            DateTime[] days = { d0, d0.AddDays(1), d0.AddDays(2), d0.AddDays(-1), d0.AddDays(7), d0.AddDays(30) };
            var probes = new string[days.Length];
            log.AppendLine("[3] 切换日期，右侧卡片几何位置：");
            for (int i = 0; i < days.Length; i++)
            {
                f.SetViewDate(days[i]);
                Application.DoEvents();
                probes[i] = f.LayoutProbe();
                var parts = probes[i].Split(sep, StringSplitOptions.None);
                log.AppendLine("    " + days[i].ToString("MM-dd") + "  " + parts[0] + " | " + parts[1]
                               + " | " + parts[2] + " | " + parts[3] + "   卡片数=" + f.CardCount);
            }

            bool sameRight = true;
            for (int i = 1; i < days.Length; i++)
            {
                var a = probes[0].Split(sep, StringSplitOptions.None);
                var b = probes[i].Split(sep, StringSplitOptions.None);
                for (int k = 0; k < 4; k++) if (a[k] != b[k]) sameRight = false;
            }
            log.AppendLine("[4] 右侧三张卡位置在切换日期时始终保持一致 = " + sameRight + "  (期望 True)");

            f.SetViewDate(d0);
            Application.DoEvents();
            log.AppendLine("[5] 回到今天后布局：" + (f.LayoutProbe() == baseLine ? "与初始完全一致 ✓" : "发生变化 ✗"));

            File.WriteAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "layouttest.txt"),
                log.ToString(), new UTF8Encoding(true));
            f.ForceClose = true;
            f.Close();
            Console.WriteLine("layouttest done");
        }

        // --------------------------------------------------- 每日步骤清单自检
        /// <summary>
        /// 把「今天这几步怎么做」的生成结果全量跑一遍（不依赖界面），
        /// 校验每个时段的时间轴是否恰好填满，结果写到 plantest.txt。
        /// </summary>
        static void PlanTest(string dateArg)
        {
            DailyPlan.Load();
            ScheduleData.Load();
            var log = new StringBuilder();
            log.AppendLine("DailyPlan 自检");
            log.AppendLine("数据来源：" + DailyPlan.LoadNote);
            log.AppendLine();

            DateTime only = DateTime.MinValue;
            if (!string.IsNullOrEmpty(dateArg))
                DateTime.TryParse(dateArg, out only);

            int slots = 0, bad = 0, noPlan = 0, clash = 0, clkBad = 0, tailBad = 0, shortBy = 0, scaled = 0, overSlots = 0;
            string clashSample = "", clkSample = "", tailSample = "", scaledSample = "";
            var samples = new List<string>();
            var dump = new StringBuilder();

            foreach (var d in ScheduleData.AllDates)
            {
                if (only != DateTime.MinValue && d.Date != only.Date) continue;
                var dp = ScheduleData.GetDay(d);
                if (dp == null) continue;
                for (int si = 0; si < dp.Slots.Count; si++)
                {
                    var s = dp.Slots[si];
                    slots++;
                    var steps = DailyPlan.Steps(s);
                    if (steps.Count == 0 || !HasPlan(steps)) { noPlan++; continue; }

                    // 时间轴不能超出该时段（不定时的「合格线 / 说明」行不计）。
                    // 短一点是允许的：原文没给够时长时，宁可提前几分钟结束，
                    // 也不能去改作者写的数字。
                    int sum = 0;
                    foreach (var st in steps) if (!st.Head && !st.Untimed) sum += st.Minutes;
                    bool over = sum > s.DurationMinutes;
                    bool ok = sum == s.DurationMinutes;
                    if (over) bad++;
                    else if (!ok) shortBy++;

                    // 步骤上的分钟数不能和正文里写的数字打架
                    // （正文写「（30 分钟）」步骤却显示 25′ —— 这正是要根除的毛病）
                    bool shrunk = false;
                    foreach (var st in steps)
                    {
                        if (st.Head || st.Untimed) continue;
                        var mm = DailyPlan.StatedMinutes(st.Text);
                        if (mm > 0 && mm != st.Minutes) { clash++; if (clashSample.Length == 0)
                            clashSample = s.Start + "-" + s.End + " " + s.Subject + "：正文写 "
                                + mm + "′，步骤显示 " + st.Minutes + "′ —— " + st.Text; }
                        // 课表原文写的数字有没有被改掉（时段排不下时只能按比例缩）
                        if (st.Raw.Length > 0)
                        {
                            int orig = DailyPlan.StatedMinutes(st.Raw);
                            if (orig > 0 && orig != st.Minutes) { scaled++; shrunk = true; if (scaledSample.Length == 0)
                                scaledSample = s.Start + "-" + s.End + " " + s.Subject
                                    + "：原文 " + orig + "′ → 现在 " + st.Minutes + "′ —— " + st.Text; }
                        }
                    }
                    if (shrunk) overSlots++;

                    // 时钟必须首尾相接，并且正好收在时段结束那一刻。
                    // 收尾逻辑改过分钟数之后如果忘了重排时钟，就会出现
                    // 「20:05 起、28 分钟」的下一步却写着 20:32 这种错位。
                    int clk = s.StartMinutes;
                    foreach (var st in steps)
                    {
                        if (st.Head || st.Untimed) continue;
                        if (st.Time != DailyPlan.Hhmm(clk)) { clkBad++; if (clkSample.Length == 0)
                            clkSample = s.Start + "-" + s.End + " " + s.Subject + "：该 " + DailyPlan.Hhmm(clk)
                                + " 却写 " + st.Time + " —— " + st.Text; }
                        clk += st.Minutes;
                    }
                    // 时钟越过时段末尾才算错；提前结束是原文没写够时长，另算一类。
                    if (clk > s.EndMinutes) { tailBad++; if (tailSample.Length == 0)
                        tailSample = s.Start + "-" + s.End + " " + s.Subject + "：收在 "
                            + DailyPlan.Hhmm(clk) + "，超出了 " + DailyPlan.Hhmm(s.EndMinutes); }

                    // 全量转储：给外部检查脚本用（时间轴连续性、正文里的隐含时长等）
                    dump.AppendLine("### " + d.ToString("yyyy-MM-dd") + " " + s.Start + "-" + s.End
                        + "  " + s.Subject + "   时长 " + s.DurationMinutes + "′  合计 " + sum + "′  "
                        + (ok ? "OK" : (over ? "超出 ✗" : "提前 " + (s.DurationMinutes - sum) + "′")));
                    dump.AppendLine("  标题：" + (s.Title ?? ""));
                    foreach (var st in steps)
                    {
                        if (st.Head) dump.AppendLine("  [" + st.Text + "]");
                        else if (st.Untimed) dump.AppendLine("  ——       " + st.Text);
                        else dump.AppendLine("  " + st.Time + " (" + st.Minutes + "′) " + st.Text);
                    }

                    if (samples.Count < 3 || !ok)
                    {
                        var sb = new StringBuilder();
                        sb.AppendLine("### " + s.Start + "-" + s.End + "  " + s.Subject
                            + "   时长 " + s.DurationMinutes + "′  合计 " + sum + "′  "
                            + (ok ? "OK" : (over ? "超出 ✗" : "提前 " + (s.DurationMinutes - sum) + "′")));
                        foreach (var st in steps)
                        {
                            if (st.Head) sb.AppendLine("  [" + st.Text + "]");
                            else if (st.Untimed) sb.AppendLine("  ——       " + st.Text);
                            else sb.AppendLine("  " + st.Time + " (" + st.Minutes + "′) " + st.Text
                                + (st.Note.Length > 0 ? "\n        . " + st.Note : ""));
                        }
                        if (samples.Count < 3) samples.Add(sb.ToString());
                        else log.AppendLine(sb.ToString());
                    }
                }
            }

            foreach (var t in samples) log.AppendLine(t);
            log.AppendLine();
            log.AppendLine("时段总数 = " + slots + "，其中无步骤 = " + noPlan
                + "，时间轴超出时段 = " + bad);
            log.AppendLine("时间轴提前结束（原文没写够时长）= " + shortBy + " 个时段");
            log.AppendLine("分钟数与正文打架 = " + clash
                + (clash > 0 ? "　（例：" + clashSample + "）" : ""));
            log.AppendLine("时钟错位 = " + clkBad
                + (clkBad > 0 ? "　（例：" + clkSample + "）" : ""));
            log.AppendLine("时钟越过时段末尾 = " + tailBad
                + (tailBad > 0 ? "　（例：" + tailSample + "）" : ""));
            log.AppendLine("课表原文写死的分钟数被改掉 = " + scaled + " 处"
                + (scaled > 0 ? "，涉及 " + overSlots + " 个时段"
                    + "（原文写得比时段长，只能等比缩）" : ""));

            File.WriteAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "plantest.txt"),
                log.ToString(), new UTF8Encoding(true));
            File.WriteAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "plandump.txt"),
                dump.ToString(), new UTF8Encoding(true));
            Console.WriteLine("plantest -> " + Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "plantest.txt"));
        }

        static bool HasPlan(List<DailyPlan.Step> steps)
        {
            foreach (var s in steps) if (!s.Head) return true;
            return false;
        }

        // --------------------------------------------------- 对话框离屏截图
        /// <summary>
        /// 把某个对话框离屏渲染成 PNG，用来核对排版（不弹到用户屏幕上）。
        /// 用法：--dlgshot proxy|github|plan 输出.png
        /// </summary>
        static void DialogShot(string name, string path)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            Form f;
            if (name == "proxy") f = new ProxyForm();
            else if (name == "github") f = new GitHubForm();
            else if (name == "plan")
            {
                ScheduleData.Load();
                DateTime d = ScheduleData.FirstDate;
                var dp = ScheduleData.GetDay(d);
                if (dp == null || dp.Slots.Count == 0) { Console.WriteLine("没有找到当天安排"); return; }
                f = new PlanForm(d, dp.Slots[0], 0);
            }
            else { Console.WriteLine("未知对话框：" + name); return; }

            f.StartPosition = FormStartPosition.Manual;
            f.Location = new Point(-6000, -6000);
            f.ShowInTaskbar = false;
            f.Show();
            Application.DoEvents();
            // 按「整个窗口」的尺寸来截：DrawToBitmap 会把标题栏也画进去，
            // 只用 ClientSize 的话底部会被裁掉一角（看上去像排版出问题）。
            using (var bmp = new Bitmap(f.Width, f.Height))
            {
                f.DrawToBitmap(bmp, new Rectangle(0, 0, bmp.Width, bmp.Height));
                bmp.Save(path, System.Drawing.Imaging.ImageFormat.Png);
            }
            f.Close();
            Console.WriteLine("dlgshot -> " + path);
        }

        // ------------------------------------------------------------------ 自检
        static void SelfTest(string dateArg)
        {
            ScheduleData.Load(true);
            Store.Load();
            var sb = new StringBuilder();
            sb.AppendLine("DPI 缩放 = " + Ui.S.ToString("0.00"));
            sb.AppendLine("工作簿 = " + ScheduleData.WorkbookPath);
            sb.AppendLine("错误 = " + (ScheduleData.LastError == "" ? "无" : ScheduleData.LastError));
            sb.AppendLine("天数 = " + ScheduleData.DayCount);
            sb.AppendLine("小节索引 = " + VideoLinks.SectionIndex.Count + " 条，课本 = " + VideoLinks.Books.Count + " 本");
            sb.AppendLine("完成记录 = " + Store.TotalDoneSlots() + " 条，累计 " + Store.TotalHours().ToString("0.0") + " 小时");
            sb.AppendLine("连续打卡 = " + Store.Streak() + " 天 / 最长 " + Store.BestStreak() + " 天");
            sb.AppendLine("开机自启 = " + Store.AutoStartEnabled);
            sb.AppendLine();
            var d = DateTime.Today;
            if (!string.IsNullOrEmpty(dateArg)) { DateTime tmp; if (DateTime.TryParse(dateArg, out tmp)) d = tmp; }
            sb.AppendLine("=== 今日 " + d.ToString("yyyy-MM-dd") + " ===");
            var dp = ScheduleData.GetDay(d);
            if (dp != null)
            {
                sb.AppendLine(dp.Header + "  [" + dp.DayKind + "]");
                foreach (var s in dp.Slots)
                {
                    sb.AppendLine("[" + s.Key + "] " + s.Subject + " -> " + s.Title);
                    string bt = VideoLinks.BookTitleForSlot(s);
                    sb.AppendLine("    BOOK " + (bt == "" ? "（无需课本）" : bt));
                    foreach (var l in VideoLinks.ForSlot(s)) sb.AppendLine("    LINK " + l[0] + " => " + l[1]);
                    foreach (var l in VideoLinks.PdfLinksForSlot(s)) sb.AppendLine("    PDF  " + l[0] + " => " + l[1]);
                }
            }
            else sb.AppendLine("今天没有安排");
            sb.AppendLine();
            sb.AppendLine("=== 抽查课本跳转 ===");
            foreach (var t in new[] { "P1|1.1", "P2/3|1.1", "S1|1.2", "Phy|21.3", "P1|7.2", "Phy|16.5" })
            {
                int pg;
                sb.AppendLine("  " + t + " -> " + (VideoLinks.SectionIndex.TryGetValue(t, out pg) ? "PDF 第 " + pg + " 页" : "无索引（回退到印刷页）"));
            }
            sb.AppendLine();
            sb.AppendLine("=== CSV 导出/导入 往返测试 ===");
            string prog = Path.Combine(Store.DataDir, "progress.tsv");
            string bak = prog + ".selftestbak";
            bool had = File.Exists(prog);
            try
            {
                if (had) File.Copy(prog, bak, true);
                var probe = ScheduleData.GetDay(new DateTime(2026, 10, 12));
                var probe2 = ScheduleData.GetDay(new DateTime(2026, 10, 1));
                var keys = new List<string>();
                if (probe != null) foreach (var s in probe.Slots) keys.Add("2026-10-12|" + s.Key);
                if (probe2 != null) keys.Add("2026-10-01|" + probe2.Slots[0].Key);

                File.WriteAllText(prog, "# test\r\n" + string.Join("\r\n", keys.ToArray()) + "\r\n",
                    new UTF8Encoding(false));
                Store.Load();
                int before = Store.TotalDoneSlots();
                string csv = Store.ExportCsv();

                File.WriteAllText(prog, "# cleared\r\n", new UTF8Encoding(false));
                Store.Load();
                int cleared = Store.TotalDoneSlots();

                int add, ex, bad;
                Store.ImportCsv(csv, out add, out ex, out bad);
                int after = Store.TotalDoneSlots();

                int add2, ex2, bad2;
                Store.ImportCsv(csv, out add2, out ex2, out bad2);

                sb.AppendLine("原始记录 " + before + " 条，清空后 " + cleared + " 条");
                sb.AppendLine("首次导入：新增 " + add + " / 已存在 " + ex + " / 无法识别 " + bad + "  → 现有 " + after
                              + (after == before ? "  ✔ 一致" : "  ✘ 不一致"));
                sb.AppendLine("重复导入：新增 " + add2 + " / 已存在 " + ex2 + "  → " + (add2 == 0 ? "✔ 幂等" : "✘ 有重复"));
                sb.AppendLine("CSV 文件：" + csv);
            }
            catch (Exception ex) { sb.AppendLine("测试异常：" + ex.Message); }
            finally
            {
                if (had) File.Copy(bak, prog, true);
                else if (File.Exists(prog)) File.Delete(prog);
                if (File.Exists(bak)) File.Delete(bak);
                Store.Load();
            }
            sb.AppendLine("（测试后已恢复原有记录：" + Store.TotalDoneSlots() + " 条）");

            string p = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "selftest.txt");
            File.WriteAllText(p, sb.ToString(), new UTF8Encoding(true));
            Console.WriteLine("selftest -> " + p);
        }
    }
}
