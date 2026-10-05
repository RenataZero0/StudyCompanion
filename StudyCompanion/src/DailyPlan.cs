using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

namespace StudyCompanion
{
    /// <summary>
    /// 「这一段具体怎么做」——把课表里的一行（14:00-15:00 应用数学 / 新课 M1 Ch1 … §1.3 §1.4）
    /// 展开成带时间戳的一步一步。
    ///
    /// 数据来自随程序打包的三张小表（data\plan\*.tsv），**完全离线**，
    /// 断网 / 没有代理也照样能用。
    ///
    ///   exercises.tsv  BOOK \t section \t ex \t q \t page \t minutes
    ///   checks.tsv     BOOK \t chapter \t check
    ///   books.tsv      BOOK \t 中文名
    /// </summary>
    public static class DailyPlan
    {
        public class Step
        {
            public string Time = "";      // "14:03"
            public int Minutes;
            public string Text = "";      // 主文本
            public string Note = "";      // 缩进的小字说明（可空，可含 \n）
            public bool Head;             // 「小节标题」这种不可勾选的提示行
        }

        class Ex
        {
            public string Name = "", Q = "";
            public int Page, Minutes;
        }

        static readonly Dictionary<string, Ex> EX = new Dictionary<string, Ex>();
        static readonly Dictionary<string, string> CHECK = new Dictionary<string, string>();
        static readonly Dictionary<string, string> BOOK_CN = new Dictionary<string, string>();
        static bool _loaded;
        static string _loadNote = "";

        /// <summary>加载时遇到的问题（用于自检输出）</summary>
        public static string LoadNote { get { return _loadNote; } }

        static readonly Dictionary<string, string> ALIAS = new Dictionary<string, string>
        {
            { "PM1", "P1" }, { "PM23", "P2/3" }, { "MECH", "M1" }, { "PHY", "Phy" },
        };

        static readonly Regex TIME_RE = new Regex(@"^(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})\s+(.+)$", RegexOptions.Compiled);
        static readonly Regex NEW_RE = new Regex(@"^新课\s+([A-Za-z0-9/]+)\s+(Ch\d+)\s+(.*?)\s*(§.*)?$", RegexOptions.Compiled);
        static readonly Regex PRAC_RE = new Regex(@"^练习\s+([A-Za-z0-9/]+)\s+(Ch\d+)\s+(.*?)\s*[（(](§.*?)[）)]\s*$", RegexOptions.Compiled);
        static readonly Regex SEC_RE = new Regex(@"§\s*(\d+\.\d+)", RegexOptions.Compiled);
        static readonly Regex READ_RE = new Regex(@"^读\s*§\s*(\d+\.\d+)\s*([^：:]*)", RegexOptions.Compiled);
        static readonly Regex DASH_RE = new Regex(@"^[·•]\s*(.*)$", RegexOptions.Compiled);
        static readonly Regex RANGE_RE = new Regex(@"^(\d+)\s*[–\-~]\s*(\d+)$", RegexOptions.Compiled);
        static readonly Regex MIN_RE = new Regex(@"(\d+)\s*[′'’]", RegexOptions.Compiled);
        static readonly Regex MIN2_RE = new Regex(@"(\d+)\s*分钟", RegexOptions.Compiled);

        // ------------------------------------------------------------------ 加载
        /// <summary>三张小表的候选目录：程序自带的那份优先，用户目录那份可以覆盖</summary>
        static List<string> AssetDirs()
        {
            var list = new List<string>();
            list.Add(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data", "plan"));
            try { list.Add(Path.Combine(Store.DataDir, "plan")); } catch { }
            return list;
        }

        public static void Load()
        {
            if (_loaded) return;
            _loaded = true;
            EX.Clear(); CHECK.Clear(); BOOK_CN.Clear();
            var found = new List<string>();

            Read("exercises.tsv", found, delegate (string[] f)
            {
                if (f.Length < 6) return;
                var e = new Ex();
                e.Name = f[2];
                e.Q = f[3];
                e.Page = ParseInt(f[4]);
                e.Minutes = ParseInt(f[5]);
                EX[f[0] + ":" + f[1]] = e;
            });
            Read("checks.tsv", found, delegate (string[] f)
            {
                if (f.Length < 3) return;
                CHECK[f[0] + ":" + f[1]] = f[2];
            });
            Read("books.tsv", found, delegate (string[] f)
            {
                if (f.Length < 2) return;
                BOOK_CN[f[0]] = f[1];
            });

            _loadNote = string.Format("exercises={0} checks={1} books={2} 来源={3}",
                EX.Count, CHECK.Count, BOOK_CN.Count,
                found.Count == 0 ? "(未找到 data\\plan)" : string.Join("; ", found.ToArray()));
        }

        static void Read(string name, List<string> found, Action<string[]> sink)
        {
            foreach (var dir in AssetDirs())
            {
                string p;
                try { p = Path.Combine(dir, name); } catch { continue; }
                try
                {
                    if (!File.Exists(p)) continue;
                    if (!found.Contains(dir)) found.Add(dir);
                    foreach (var line in File.ReadAllLines(p, Encoding.UTF8))
                    {
                        if (line.Length == 0 || line[0] == '#') continue;
                        if (line.IndexOf('\t') < 0) continue;
                        sink(line.Split('\t'));
                    }
                    return;     // 第一份找到的就够了
                }
                catch { }
            }
        }

        static int ParseInt(string s)
        {
            int v;
            return int.TryParse((s ?? "").Trim(), out v) ? v : 0;
        }

        static string Books(string b)
        {
            string cn;
            return BOOK_CN.TryGetValue(b, out cn) ? cn : b;
        }

        static string Alias(string b) { return ALIAS.ContainsKey(b) ? ALIAS[b] : b; }

        static Ex ExOf(string book, string sec)
        {
            string b = Alias(book);
            Ex e;
            if (EX.TryGetValue(b + ":" + sec, out e)) return e;
            if (EX.TryGetValue("PHY:" + sec, out e)) return e;
            return null;
        }

        static string SectionName(Slot s, string sec)
        {
            for (int i = 1; i < s.Body.Count; i++)
            {
                string ln = s.Body[i].Trim();
                if (ln.StartsWith("·") || ln.StartsWith("•")) ln = ln.Substring(1).Trim();
                var m = READ_RE.Match(ln);
                if (m.Success && m.Groups[1].Value == sec)
                {
                    string nm = m.Groups[2].Value.Trim();
                    nm = Regex.Replace(nm, "[：:]$", "").Trim();
                    nm = Regex.Replace(nm, @"\s*p\.\s*\d+\s*$", "").Trim();
                    return nm;
                }
            }
            return "";
        }

        static string Hhmm(int total)
        {
            return ((total / 60) % 24).ToString("00") + ":" + (total % 60).ToString("00");
        }

        // ------------------------------------------------------------------ 展开
        public static List<Step> Steps(Slot s)
        {
            var outp = StepsRaw(s);
            FitTail(outp, s);
            return outp;
        }

        /// <summary>
        /// 整数除法会把余数丢掉，导致最后一步比时段结束早几分钟收尾。
        /// 这里把差额补给最后一步，保证时间轴正好铺满整个时段。
        /// </summary>
        static void FitTail(List<Step> outp, Slot s)
        {
            if (s == null || outp.Count == 0) return;
            int sum = 0, last = -1;
            for (int i = 0; i < outp.Count; i++)
            {
                if (outp[i].Head) continue;
                if (outp[i].Minutes < 3) outp[i].Minutes = 3;   // 和绘制时的下限保持一致
                sum += outp[i].Minutes;
                last = i;
            }
            if (last < 0) return;
            int delta = s.DurationMinutes - sum;
            int want = outp[last].Minutes + delta;
            if (want < 3) want = 3;
            outp[last].Minutes = want;
        }

        static List<Step> StepsRaw(Slot s)
        {
            Load();
            var outp = new List<Step>();
            if (s == null || s.Body.Count == 0) return outp;

            string title = s.Body[0].Trim();
            int total = Math.Max(1, s.DurationMinutes);
            int t = s.StartMinutes;
            int end = s.EndMinutes;

            var mn = NEW_RE.Match(title);
            var mp = PRAC_RE.Match(title);

            // ---------------------------------------------- 新课
            if (mn.Success)
            {
                string book = Alias(mn.Groups[1].Value);
                string ch = mn.Groups[2].Value;
                string secs = mn.Groups[4].Success ? mn.Groups[4].Value : "";
                var secList = SecListOf(secs);

                outp.Add(Info(Books(book) + " " + ch + " " + mn.Groups[3].Value + "　" + secs));

                int openMin = 3, noteMin = 8, ankiMin = 5;
                int readTotal = Math.Max(15, total - openMin - noteMin - ankiMin - 4);
                int per = Math.Max(10, readTotal / Math.Max(1, secList.Count));

                outp.Add(StepAt(t, openMin, "翻到课本先看本节 Learning outcomes，在纸上写下「这节我要学会哪几件事」", ""));
                t += openMin;

                for (int i = 0; i < secList.Count; i++)
                {
                    string sec = secList[i];
                    string nm = SectionName(s, sec);
                    outp.Add(StepAt(t, per, "读 §" + sec + (nm.Length > 0 ? " " + nm : "") + " —— 三遍法",
                            "① 通读一遍不求记住　② 遮住例题解答自己完整算一遍再对答案　③ 合上书默写本节公式/定义"));
                    t += per;
                }
                outp.Add(StepAt(t, noteMin, "把本节公式/定义手抄一页笔记", "抄的过程就是第一遍记忆"));
                t += noteMin;
                outp.Add(StepAt(t, ankiMin, "今天新学的术语做成 Anki 卡", "正面术语，反面中英对照 + 一句例子"));
                t += ankiMin;

                int left = Math.Max(3, end - t);
                string ck;
                outp.Add(StepAt(t, left, "合上书自测",
                        CHECK.TryGetValue(book + ":" + ch, out ck) ? ck : "能不能用自己的话把这两节讲一遍"));
                return outp;
            }

            // ---------------------------------------------- 练习
            if (mp.Success)
            {
                string book = Alias(mp.Groups[1].Value);
                string ch = mp.Groups[2].Value;
                string secs = mp.Groups[4].Value;
                var secList = SecListOf(secs);

                outp.Add(Info(Books(book) + " " + ch + " " + mp.Groups[3].Value + "　" + secs));

                int openMin = 3, fixMin = 15, ankiMin = 5;
                int budget = Math.Max(20, total - openMin - fixMin - ankiMin);

                outp.Add(StepAt(t, openMin, "先翻回课本把要考的公式/定义扫一眼，再开始做题", ""));
                t += openMin;

                var exs = new List<Ex>();
                int est = 0;
                for (int i = 0; i < secList.Count; i++)
                {
                    var e = ExOf(book, secList[i]);
                    exs.Add(e);
                    if (e != null) est += e.Minutes;
                }
                if (est <= 0) est = budget;
                double scale = Math.Min(1.0, budget * 1.0 / est);

                bool anyEx = false;
                for (int i = 0; i < secList.Count; i++)
                {
                    var e = exs[i];
                    if (e == null) continue;
                    anyEx = true;
                    string sec = secList[i];
                    string nm = SectionName(s, sec);
                    int mm = Math.Max(8, (int)Math.Round(e.Minutes * scale));
                    bool rng = RANGE_RE.IsMatch((e.Q ?? "").Trim());
                    var rm = RANGE_RE.Match((e.Q ?? "").Trim());
                    string qtxt = rng
                            ? ("题 " + rm.Groups[1].Value + "–" + rm.Groups[2].Value)
                            : "全部题（做不完先做前一半，剩余顺延到下一个练习时段）";
                    string label = e.Name;
                    if (!rng && nm.Length > 0 && label.Contains("节末"))
                        label = "§" + sec + " " + nm + " 的节末 Questions";
                    outp.Add(StepAt(t, mm, label + "　" + qtxt,
                            "位置：" + Books(book) + " §" + sec + (nm.Length > 0 ? " " + nm : "")
                                    + "，课本 p." + e.Page + "\n卡住超过 3 分钟就跳过，做完统一看解析"));
                    t += mm;
                }
                if (!anyEx)
                {
                    var ds = Details(s);
                    int stp = Math.Max(10, budget / Math.Max(1, ds.Count));
                    for (int i = 0; i < ds.Count; i++)
                    {
                        outp.Add(StepAt(t, stp, ds[i], ""));
                        t += stp;
                    }
                }

                outp.Add(StepAt(t, fixMin, "批改 + 错题",
                        "对答案册逐题打勾/画圈，做对的也看一眼解法是否更短\n"
                      + "每道错题写一行：题号 + 我为什么错 + 正确思路一句话"));
                t += fixMin;
                outp.Add(StepAt(t, Math.Max(3, end - t), "今天想不起来的术语/公式做成 Anki 卡", ""));
                return outp;
            }

            // ---------------------------------------------- 其他（EAP / 复习 / 测试）
            var det = Details(s);
            if (det.Count == 0) return outp;
            outp.Add(Info(s.Subject));

            var est2 = new int[det.Count];
            int used = 0, blanks = 0;
            for (int i = 0; i < det.Count; i++)
            {
                // 先找「20′」这种写法，找不到再找「20分钟」。
                // 注意：不能对同一个 Matcher 连着调两次 find()——第一次已经越过了那个数字。
                var m = MIN_RE.Match(det[i]);
                if (!m.Success) m = MIN2_RE.Match(det[i]);
                est2[i] = m.Success ? ParseInt(m.Groups[1].Value) : 0;
                if (est2[i] > 0) used += est2[i]; else blanks++;
            }
            if (blanks > 0)
            {
                int per = Math.Max(5, (total - used) / blanks);
                for (int i = 0; i < est2.Length; i++) if (est2[i] == 0) est2[i] = per;
            }
            else if (used == 0)
            {
                int even = Math.Max(5, total / Math.Max(1, det.Count));
                for (int i = 0; i < est2.Length; i++) est2[i] = even;
            }
            for (int i = 0; i < det.Count; i++)
            {
                int mm = Math.Max(3, est2[i]);
                outp.Add(StepAt(t, mm, det[i], ""));
                t += mm;
            }
            return outp;
        }

        static List<string> SecListOf(string secs)
        {
            var outp = new List<string>();
            if (secs == null) return outp;
            foreach (Match m in SEC_RE.Matches(secs))
                if (!outp.Contains(m.Groups[1].Value)) outp.Add(m.Groups[1].Value);
            return outp;
        }

        static List<string> Details(Slot s)
        {
            var outp = new List<string>();
            for (int i = 1; i < s.Body.Count; i++)
            {
                var m = DASH_RE.Match(s.Body[i].Trim());
                if (m.Success && m.Groups[1].Value.Trim().Length > 0) outp.Add(m.Groups[1].Value.Trim());
            }
            return outp;
        }

        static Step StepAt(int t, int mm, string text, string note)
        {
            var s = new Step();
            s.Time = Hhmm(t);
            s.Minutes = mm;
            s.Text = text;
            s.Note = note == null ? "" : note;
            return s;
        }

        static Step Info(string text)
        {
            var s = new Step();
            s.Head = true;
            s.Text = text;
            return s;
        }

        // ------------------------------------------------------------------ 打勾存档
        // 键存在和打卡记录同一个集合里（progress.tsv），所以会跟着 GitHub 一起同步
        public static bool Done(string iso, int slot, int idx) { return Store.PlanStepDone(iso, slot, idx); }
        public static void SetDone(string iso, int slot, int idx, bool v) { Store.SetPlanStep(iso, slot, idx, v); }

        public static int DoneCount(string iso, int slot, int total)
        {
            int n = 0;
            for (int i = 0; i < total; i++) if (Done(iso, slot, i)) n++;
            return n;
        }

        /// <summary>把这一步的勾去掉/勾上，返回勾完之后的「已完成步数」</summary>
        public static int Toggle(string iso, int slot, int idx, int total, bool on)
        {
            SetDone(iso, slot, idx, on);
            return DoneCount(iso, slot, total);
        }
    }
}
