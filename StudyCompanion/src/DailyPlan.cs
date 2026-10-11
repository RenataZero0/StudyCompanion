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
            public bool Fixed;            // 分钟数是课表原文里写死的，不许被 FitTail 改
            public bool Untimed;          // 「合格线 / 说明」这种不定时的行：不显示分钟、时钟也不走
            public string Raw = "";       // 原文（只有写了分钟数的行才填）——收尾后用它把正文里的数字改回来
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
        // 「每节 5 分钟」说的是**每小节**的时长，不是这一行的总时长。
        // 课表里写「速览 §1.1…§1.4（每节 5 分钟）」= 4 节 ×5 分 = 20 分钟。
        static readonly Regex PER_RE = new Regex(@"(?:每节|各|每个|每小块)\s*(\d+)\s*分钟", RegexOptions.Compiled);
        // 收尾后写回去的「共 N 分钟」——优先级最高，它就是这一行的总时长。
        static readonly Regex TOTAL_RE = new Regex(@"共\s*(\d+)\s*分钟", RegexOptions.Compiled);

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

        public static string Hhmm(int total)
        {
            return ((total / 60) % 24).ToString("00") + ":" + (total % 60).ToString("00");
        }

        /// <summary>
        /// 这一行正文里自己写了多少分钟（「（30 分钟）」「20′」「每节 5 分钟」）。没写就返回 0。
        /// 自检用它来确认「步骤上显示的分钟数」和「正文里写的」一致。
        /// </summary>
        /// <summary>
        /// 「标签段」：第一个中文冒号之前的部分。
        /// 作者写时长一律写在标签段里（`① 回收（10 分钟）：…`、`Ex 2A 题 1–16（约 34 分钟，p.37）`）；
        /// 冒号之后的正文里出现的数字只是描述，不是这一行的量 ——
        /// 比如 `✓ 合格线（自己判定）：复述能连续讲满 2 分钟…` 里的 2 分钟是要求，不是步骤时长；
        /// `③ 产出（30 分钟）：…听一段 5–8 分钟真讲座…` 里的 5–8 分钟是材料长度。
        /// 早先把整行都拿去搜数字，结果把 10/25/30/10 的 EAP 格子重算成 9/24/29/9，
        /// 正文里的数字也跟着被改 —— 看起来就是「时间和安排不符」。
        /// </summary>
        static string LabelOf(string text)
        {
            if (string.IsNullOrEmpty(text)) return "";
            int i = text.IndexOf('：');
            return i > 0 ? text.Substring(0, i) : text;
        }

        public static int StatedMinutes(string text)
        {
            if (string.IsNullOrEmpty(text)) return 0;
            string seg = LabelOf(text);
            var tot = TOTAL_RE.Match(seg);           // 已经写明了总数，以它为准
            if (tot.Success) return ParseInt(tot.Groups[1].Value);
            var per = PER_RE.Match(seg);             // 「每节 5 分钟」× 节数
            if (per.Success)
            {
                int n = ParseInt(per.Groups[1].Value);
                int cnt = SecListOf(seg).Count;
                if (cnt >= 1) return n * cnt;
            }
            var m = MIN_RE.Match(seg);
            if (!m.Success) m = MIN2_RE.Match(seg);
            return m.Success ? ParseInt(m.Groups[1].Value) : 0;
        }

        /// <summary>把正文里那个分钟数换掉（跟 StatedMinutes 认的是同一处数字）。</summary>
        public static string WithMinutes(string text, int mm)
        {
            if (string.IsNullOrEmpty(text)) return text;
            string seg = LabelOf(text);
            // 「每节 N 分钟」这种写法：N 是每节的量，不能直接改成总数。
            if (PER_RE.IsMatch(seg))
            {
                var tot = TOTAL_RE.Match(seg);
                if (tot.Success)
                {
                    var d0 = tot.Groups[1];
                    return text.Substring(0, d0.Index) + mm.ToString() + text.Substring(d0.Index + d0.Length);
                }
                var p = PER_RE.Match(seg);
                int n = ParseInt(p.Groups[1].Value);
                int cnt = SecListOf(seg).Count;
                if (cnt >= 1 && n * cnt != mm)
                {
                    // 时段排不下，已经按比例压缩过：这时「每节 5 分钟」和实际总量对不上，
                    // 与其留下「每节 5 分钟，共 16 分钟」这种自相矛盾，不如只留总量。
                    return text.Substring(0, p.Index) + "共 " + mm + " 分钟"
                        + text.Substring(p.Index + p.Value.Length);
                }
                return text.Substring(0, p.Index) + p.Value + "，共 " + mm + " 分钟"
                    + text.Substring(p.Index + p.Value.Length);
            }
            var g = MIN_RE.Match(seg);
            if (!g.Success) g = MIN2_RE.Match(seg);
            if (!g.Success) return text;
            var d = g.Groups[1];
            return text.Substring(0, d.Index) + mm.ToString() + text.Substring(d.Index + d.Length);
        }

        // ------------------------------------------------------------------ 展开
        public static List<Step> Steps(Slot s)
        {
            var outp = StepsRaw(s);
            FitTail(outp, s);
            // 收尾可能动过分钟数：凡是正文里写了时长的行，把正文里的数字同步成最终值，
            // 这样「正文写（30 分钟）」和「步骤显示 25′」永远不会同时出现。
            foreach (var st in outp)
                if (!st.Head && !st.Untimed && st.Raw.Length > 0)
                    st.Text = WithMinutes(st.Raw, st.Minutes);
            // FitTail 改过分钟数之后，时钟必须重排一遍 ——
            // 否则会看到「20:05 起、28 分钟」的下一步却写着 20:32（该是 20:33）。
            int cursor = s.StartMinutes;
            foreach (var st in outp)
            {
                if (st.Head || st.Untimed) continue;   // 不定时的行不走钟
                st.Time = Hhmm(cursor);
                cursor += st.Minutes;
            }
            return outp;
        }

        /// <summary>
        /// 整数除法、以及「至少 N 分钟」的下限，都会让步骤总时长和时段对不上：
        /// 少了几分钟 → 最后一步提前收尾；多了几分钟 → 最后一步被顶出时段外。
        /// 这里统一收尾：多的补给最后一步，超的从最长的一步开始往下扣，谁也不许低于 MinMin。
        /// </summary>
        const int MinMin = 3;
        // 收尾时的下限只保证「不是 0」：这里再用 3 分钟卡，
        // 就会出现「把所有步骤顶到 3 分钟、只能回头去砍课表原文写死的数字」的荒唐结果。
        const int MinTail = 1;
        // 没写时长的行至少分到这么多分钟才值得给时间，否则显示成不定时。
        const int MinUseful = 3;

        static void FitTail(List<Step> outp, Slot s)
        {
            if (s == null || outp.Count == 0) return;
            var idx = new List<int>();        // 可以随便调的（时长是程序算出来的）
            var all = new List<int>();        // 所有定时行（含课表原文里写死的）
            int sum = 0;
            for (int i = 0; i < outp.Count; i++)
            {
                if (outp[i].Head || outp[i].Untimed) continue;   // 提示行 / 不定时行不参与
                if (outp[i].Minutes < MinTail) outp[i].Minutes = MinTail;
                sum += outp[i].Minutes;
                all.Add(i);
                if (!outp[i].Fixed) idx.Add(i);
            }
            if (all.Count == 0) return;
            // 实在没有「程序算出来的」行时，才允许动课表原文里的数字，
            // 否则宁可让原文的数字保持原样。
            if (idx.Count == 0) idx = all;

            int diff = s.DurationMinutes - sum;
            if (diff > 0)
            {
                int flex = 0;
                for (int i = 0; i < idx.Count; i++) if (!outp[idx[i]].Fixed) flex++;
                if (flex == 0)
                {
                    // 全是课表原文写死的时长：宁可让这个时段提前几分钟结束，
                    // 也不能去改原文的数字 —— 改了就是「时间和安排不符」。
                    return;
                }
                // 富余的时间平摊给「程序算出来的」那些行，免得全堆在最后一步上
                int each = diff / flex, rest = diff % flex;
                for (int i = 0; i < idx.Count; i++)
                {
                    int k = idx[i];
                    if (outp[k].Fixed) continue;
                    outp[k].Minutes += each;
                    if (rest > 0) { outp[k].Minutes += 1; rest--; }
                }
                return;
            }
            while (diff < 0)
            {
                int best = -1;
                // 先只从「程序算出来的」里挑最长的往下扣
                for (int i = 0; i < idx.Count; i++)
                {
                    int k = idx[i];
                    if (outp[k].Minutes <= MinTail || outp[k].Fixed) continue;
                    if (best < 0 || outp[k].Minutes > outp[best].Minutes) best = k;
                }
                // 实在没有，才退而求其次去动课表原文里写死的
                if (best < 0)
                {
                    for (int i = 0; i < idx.Count; i++)
                    {
                        int k = idx[i];
                        if (outp[k].Minutes <= MinTail) continue;
                        if (best < 0 || outp[k].Minutes > outp[best].Minutes) best = k;
                    }
                }
                if (best < 0) break;      // 已经全部到底，只能认了
                outp[best].Minutes--;
                diff++;
            }
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
                    int stp = Math.Max(MinMin, budget / Math.Max(1, ds.Count));
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

            // 课表原文里自己写了「（25 分钟）」「20′」的，那个数字就是作者定的，
            // 必须原样照抄 —— 不能再平均分配、更不能被 FitTail 改掉，
            // 否则会出现「正文写着（30 分钟），步骤上却显示 25′」这种自相矛盾。
            var own = new int[det.Count];
            var fromText = new bool[det.Count];
            int fixedSum = 0, blanks = 0;
            for (int i = 0; i < det.Count; i++)
            {
                own[i] = StatedMinutes(det[i]);
                if (own[i] > 0) { fromText[i] = true; fixedSum += own[i]; }
                else blanks++;
            }

            // 原文写的时长加起来比时段本身还长 —— **不再按比例缩小**。
            // 2026-10-11 用户决定：保留课表原文的数字，宁可让时间轴溢出（后面几行会标出来），
            // 也不要在程序里偷偷把「30 分钟」改小成「25 分钟」——那等于篡改课表。
            // 这类格子已经在排课端修掉了（EAP 补到 75′、格式训练不再把两节塞进一格），
            // 这里保留检测只为万一以后又出现：把差额记下来，交给 FitTail 之后的标记去提示。
            int slack = total - fixedSum;   // 可能为负（原文比时段长）——下面几个分支都会自然跳过
            // 没写时长的行（「逐题批改」「做完对照答案」这类说明）只有在能分到
            // MinUseful 分钟以上时才给时间。只分到 1–2 分钟毫无意义 ——
            // 「逐题批改 (1′)」比「——（属于上一步）」更容易让人误解。
            if (blanks > 0 && slack / blanks >= MinUseful)
            {
                int per = slack / blanks;
                int given = 0;
                for (int i = 0; i < own.Length; i++)
                {
                    if (own[i] != 0) continue;
                    if (given + per > slack) continue;   // 余量不够了，这行改用不定时
                    own[i] = per;
                    given += per;
                }
            }
            else if (blanks > 0 && slack >= MinUseful)
            {
                // 平摊不够看（例如 3 行只摊到 5 分钟）：把这点余量整块给第一条说明行 ——
                // 它通常紧跟在习题后面，是「逐题批改」这种真要吃时间的活。
                // 这样时间轴照样收在时段末尾，又不会冒出 1–2 分钟的空步骤。
                for (int i = 0; i < own.Length; i++)
                    if (own[i] == 0) { own[i] = slack; break; }
            }
            else if (fixedSum == 0)
            {
                // 一行都没写时长：按行数平分
                int even = Math.Max(MinMin, total / Math.Max(1, det.Count));
                for (int i = 0; i < own.Length; i++) own[i] = even;
            }
            // 剩下的情况：作者写的时长已经把这个时段占满了。
            // 那些没写时长的行是「合格线 / 本类口径」这类说明，本来就不占时间 ——
            // 不给分钟数、时刻也不显示，时钟停在最后一个定时步骤上。

            int cursor = t;
            for (int i = 0; i < det.Count; i++)
            {
                if (own[i] > 0)
                {
                    var st = StepAt(cursor, own[i], det[i], "");
                    if (fromText[i]) st.Raw = det[i];   // 正文里写了数字，收尾后要同步
                    st.Fixed = fromText[i];             // 只有课表原文写了数字的才锁死
                    outp.Add(st);
                    cursor += own[i];
                }
                else
                {
                    var st = StepAt(cursor, 0, det[i], "");
                    st.Time = "";
                    st.Untimed = true;
                    outp.Add(st);
                }
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
