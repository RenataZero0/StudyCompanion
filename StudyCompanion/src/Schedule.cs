using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml.Linq;

namespace StudyCompanion
{
    /// <summary>一个学习时间段（对应日历单元格里的一段）</summary>
    public class Slot
    {
        public string Start = "";
        public string End = "";
        public string Subject = "";
        public List<string> Body = new List<string>();
        public string Summary = "";          // 人类可读摘要（用于统计/导出）

        public string Title { get { return Body.Count > 0 ? Body[0] : ""; } }
        public string Key { get { return Start + "-" + End; } }
        public int StartMinutes { get { return ToMin(Start); } }
        public int EndMinutes { get { return ToMin(End); } }
        public int DurationMinutes { get { return Math.Max(0, EndMinutes - StartMinutes); } }

        public static int ToMin(string hhmm)
        {
            if (string.IsNullOrEmpty(hhmm)) return 0;
            var p = hhmm.Split(':');
            int h, m;
            if (p.Length == 2 && int.TryParse(p[0], out h) && int.TryParse(p[1], out m)) return h * 60 + m;
            return 0;
        }
        public DateTime StartToday { get { return DateTime.Today.AddMinutes(StartMinutes); } }
        public DateTime EndToday { get { return DateTime.Today.AddMinutes(EndMinutes); } }
    }

    public class DayPlan
    {
        public DateTime Date;
        public string Header = "";
        public string Holiday = "";
        public List<Slot> Slots = new List<Slot>();
        public bool IsHoliday { get { return Holiday.Length > 0; } }

        public int TotalMinutes
        {
            get { int t = 0; foreach (var s in Slots) t += s.DurationMinutes; return t; }
        }
        public string DayKind
        {
            get
            {
                if (IsHoliday) return Holiday;
                if (Date.DayOfWeek == DayOfWeek.Saturday || Date.DayOfWeek == DayOfWeek.Sunday) return "周末";
                return "上学日";
            }
        }
    }

    /// <summary>直接读取 Schedule.xlsx（OOXML = zip + xml），不需要安装 Excel</summary>
    public static class ScheduleData
    {
        public static string WorkbookPath = "";
        public static string LastError = "";
        public static bool LoadedFromCache = false;
        public static DateTime LoadedAt = DateTime.MinValue;

        static readonly Dictionary<DateTime, string> _texts = new Dictionary<DateTime, string>();
        static readonly Dictionary<DateTime, DayPlan> _plans = new Dictionary<DateTime, DayPlan>();

        static readonly Regex TimeRx = new Regex(
            @"^\s*(\d{1,2}:\d{2})\s*[-–—]\s*(\d{1,2}:\d{2})\s*(.+?)\s*$", RegexOptions.Compiled);
        static readonly Regex DayRx = new Regex(
            @"^(\d{1,2})月(\d{1,2})日", RegexOptions.Compiled);

        static readonly char Sep1 = '\u0001';   // 字段分隔
        static readonly char Sep2 = '\u0002';   // 行分隔

        // ------------------------------------------------------------------ 定位工作簿
        public static string FindWorkbook()
        {
            var cands = new List<string>();
            string exeDir = AppDomain.CurrentDomain.BaseDirectory;
            string cfg = Path.Combine(exeDir, "settings.ini");
            if (File.Exists(cfg))
            {
                foreach (var line in File.ReadAllLines(cfg, Encoding.UTF8))
                {
                    var s = line.Trim();
                    if (s.StartsWith("SchedulePath=", StringComparison.OrdinalIgnoreCase))
                        cands.Add(s.Substring("SchedulePath=".Length).Trim().Trim('"'));
                }
            }
            // 安装版：课表放在用户数据目录里，程序更新不会覆盖，用户也能自己改
            cands.Add(Path.Combine(Store.Root, "Schedule.xlsx"));

            // 从 exe 所在目录往上一层层找：课表可能在 exe 旁边，也可能在
            // 上层的 02_学习与教材/ 里（目录结构调整过几次，写死某一层会失效）
            string dir = exeDir.TrimEnd('\\');
            for (int up = 0; up < 5 && !string.IsNullOrEmpty(dir); up++)
            {
                cands.Add(Path.Combine(dir, "Schedule.xlsx"));
                cands.Add(Path.Combine(dir, "02_学习与教材", "Schedule.xlsx"));
                var parent = Path.GetDirectoryName(dir);
                if (parent == null || parent == dir) break;
                dir = parent;
            }

            foreach (var c in cands)
                if (!string.IsNullOrEmpty(c) && File.Exists(c)) return c;
            return cands.Count > 0 ? cands[0] : "";
        }

        public static string CachePath
        {
            get
            {
                string dir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data");
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "daycache.tsv");
            }
        }

        // ------------------------------------------------------------------ 读取
        public static void Load(bool force = false)
        {
            if (!force && LoadedAt.Date == DateTime.Today && _plans.Count > 0) return;
            LastError = ""; LoadedFromCache = false;
            _texts.Clear(); _plans.Clear();
            if (string.IsNullOrEmpty(WorkbookPath)) WorkbookPath = FindWorkbook();

            bool ok = false;
            try
            {
                if (!string.IsNullOrEmpty(WorkbookPath) && File.Exists(WorkbookPath))
                {
                    var texts = ReadWorkbookTexts(WorkbookPath);
                    if (texts.Count > 0) { foreach (var kv in texts) _texts[kv.Key] = kv.Value; ok = true; }
                }
                else LastError = "找不到 Schedule.xlsx：" + WorkbookPath;
            }
            catch (Exception ex)
            {
                LastError = "读取课表失败：" + ex.Message;
            }

            if (ok)
            {
                try { SaveCache(); } catch { }
            }
            else
            {
                if (TryLoadCache()) { LoadedFromCache = true; }
            }
            LoadedAt = DateTime.Now;

            foreach (var kv in _texts) _plans[kv.Key] = ParseDay(kv.Key, kv.Value);
        }

        static void SaveCache()
        {
            var sb = new StringBuilder();
            foreach (var kv in _texts.OrderBy(k => k.Key))
                sb.Append(kv.Key.ToString("yyyy-MM-dd")).Append(Sep1)
                  .Append(kv.Value.Replace("\r", "").Replace("\n", Sep2.ToString()))
                  .Append('\n');
            File.WriteAllText(CachePath, sb.ToString(), new UTF8Encoding(false));
        }

        static bool TryLoadCache()
        {
            try
            {
                if (!File.Exists(CachePath)) return false;
                foreach (var line in File.ReadAllLines(CachePath, Encoding.UTF8))
                {
                    if (line.Length == 0) continue;
                    int i = line.IndexOf(Sep1);
                    if (i <= 0) continue;
                    DateTime dt;
                    if (!DateTime.TryParseExact(line.Substring(0, i), "yyyy-MM-dd",
                            CultureInfo.InvariantCulture, DateTimeStyles.None, out dt)) continue;
                    _texts[dt] = line.Substring(i + 1).Replace(Sep2.ToString(), "\n");
                }
                return _texts.Count > 0;
            }
            catch { return false; }
        }

        // ------------------------------------------------------------------ xlsx 解析
        static List<string> ReadSharedStrings(ZipArchive zip)
        {
            var list = new List<string>();
            var e = zip.GetEntry("xl/sharedStrings.xml");
            if (e == null) return list;
            using (var s = e.Open())
            {
                var doc = XDocument.Load(s);
                foreach (var si in doc.Descendants().Where(x => x.Name.LocalName == "si"))
                {
                    var sb = new StringBuilder();
                    foreach (var t in si.Descendants().Where(x => x.Name.LocalName == "t"))
                        sb.Append(t.Value);
                    list.Add(sb.ToString());
                }
            }
            return list;
        }

        static Dictionary<string, string> SheetTargets(ZipArchive zip)
        {
            var map = new Dictionary<string, string>();
            var e = zip.GetEntry("xl/_rels/workbook.xml.rels");
            if (e == null) return map;
            using (var s = e.Open())
            {
                var doc = XDocument.Load(s);
                foreach (var r in doc.Descendants().Where(x => x.Name.LocalName == "Relationship"))
                {
                    var id = (string)r.Attribute("Id");
                    var tg = (string)r.Attribute("Target");
                    if (id == null || tg == null) continue;
                    tg = tg.Replace("\\", "/");
                    if (tg.StartsWith("/")) tg = tg.TrimStart('/');
                    else if (!tg.StartsWith("xl/")) tg = "xl/" + tg;
                    map[id] = tg;
                }
            }
            return map;
        }

        /// <summary>返回 日期 -> 该日单元格文本</summary>
        public static Dictionary<DateTime, string> ReadWorkbookTexts(string path)
        {
            var result = new Dictionary<DateTime, string>();
            using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
            using (var zip = new ZipArchive(fs, ZipArchiveMode.Read))
            {
                var shared = ReadSharedStrings(zip);
                var targets = SheetTargets(zip);

                var wbEntry = zip.GetEntry("xl/workbook.xml");
                if (wbEntry == null) throw new Exception("不是有效的 xlsx（缺少 xl/workbook.xml）");
                var sheets = new List<KeyValuePair<string, string>>(); // name, target
                using (var s = wbEntry.Open())
                {
                    var doc = XDocument.Load(s);
                    foreach (var sh in doc.Descendants().Where(x => x.Name.LocalName == "sheet"))
                    {
                        string name = (string)sh.Attribute("name");
                        string rid = null;
                        foreach (var a in sh.Attributes())
                            if (a.Name.LocalName == "id") rid = a.Value;
                        if (name == null || rid == null) continue;
                        string tg;
                        if (targets.TryGetValue(rid, out tg)) sheets.Add(new KeyValuePair<string, string>(name, tg));
                    }
                }

                foreach (var sh in sheets)
                {
                    if (!Regex.IsMatch(sh.Key, @"^\d{6}$")) continue;
                    int yy = int.Parse(sh.Key.Substring(0, 4));
                    int mm = int.Parse(sh.Key.Substring(4, 2));
                    var entry = zip.GetEntry(sh.Value);
                    if (entry == null) continue;

                    using (var s = entry.Open())
                    {
                        var doc = XDocument.Load(s);
                        foreach (var c in doc.Descendants().Where(x => x.Name.LocalName == "c"))
                        {
                            string t = (string)c.Attribute("t");
                            string text = null;
                            if (t == "s")
                            {
                                var v = c.Elements().FirstOrDefault(x => x.Name.LocalName == "v");
                                if (v != null)
                                {
                                    int idx;
                                    if (int.TryParse(v.Value.Trim(), out idx) && idx >= 0 && idx < shared.Count)
                                        text = shared[idx];
                                }
                            }
                            else if (t == "inlineStr")
                            {
                                var isEl = c.Elements().FirstOrDefault(x => x.Name.LocalName == "is");
                                if (isEl != null)
                                {
                                    var sb = new StringBuilder();
                                    foreach (var tt in isEl.Descendants().Where(x => x.Name.LocalName == "t")) sb.Append(tt.Value);
                                    text = sb.ToString();
                                }
                            }
                            else
                            {
                                var v = c.Elements().FirstOrDefault(x => x.Name.LocalName == "v");
                                if (v != null) text = v.Value;
                            }
                            if (string.IsNullOrEmpty(text)) continue;

                            var firstLine = text.Replace("\r", "").Split('\n')[0];
                            var m = DayRx.Match(firstLine.Trim());
                            if (!m.Success) continue;
                            int d = int.Parse(m.Groups[2].Value);
                            DateTime dt;
                            try { dt = new DateTime(yy, mm, d); } catch { continue; }
                            result[dt] = text;
                        }
                    }
                }
            }
            return result;
        }

        // ------------------------------------------------------------------ 单元格 -> DayPlan
        public static DayPlan ParseDay(DateTime date, string cellText)
        {
            var dp = new DayPlan();
            dp.Date = date;
            if (string.IsNullOrEmpty(cellText)) return dp;

            var lines = cellText.Replace("\r", "").Split('\n');
            int idx = 0;

            // 第一行：日期 + 星期 (+ ◆假期名)
            if (lines.Length > 0) dp.Header = lines[0].Trim();
            int dpos = dp.Header.IndexOf('◆');
            if (dpos >= 0) dp.Holiday = dp.Header.Substring(dpos + 1).Trim();
            idx = 1;

            Slot cur = null;
            for (; idx < lines.Length; idx++)
            {
                var raw = lines[idx];
                var line = raw.TrimEnd();
                if (line.Trim().Length == 0) continue;
                if (line.Trim().Trim('─', '-', '—', '=', '_').Length == 0) continue;  // 分隔线

                var m = TimeRx.Match(line);
                if (m.Success)
                {
                    cur = new Slot();
                    cur.Start = NormalizeTime(m.Groups[1].Value);
                    cur.End = NormalizeTime(m.Groups[2].Value);
                    cur.Subject = m.Groups[3].Value.Trim();
                    dp.Slots.Add(cur);
                }
                else if (cur != null)
                {
                    cur.Body.Add(line.Trim());
                }
            }

            foreach (var s in dp.Slots)
                s.Summary = (s.Subject + " " + s.Title).Trim();
            return dp;
        }

        static string NormalizeTime(string hhmm)
        {
            var p = hhmm.Split(':');
            if (p.Length != 2) return hhmm;
            return p[0].PadLeft(2, '0') + ":" + p[1];
        }

        // ------------------------------------------------------------------ 对外接口
        public static DayPlan GetDay(DateTime date)
        {
            DayPlan dp;
            if (_plans.TryGetValue(date.Date, out dp)) return dp;
            string txt;
            if (_texts.TryGetValue(date.Date, out txt))
            {
                dp = ParseDay(date.Date, txt);
                _plans[date.Date] = dp;
                return dp;
            }
            return null;
        }

        public static IEnumerable<DateTime> AllDates { get { return _texts.Keys.OrderBy(k => k); } }
        public static int DayCount { get { return _texts.Count; } }
        public static DateTime FirstDate { get { return _texts.Count == 0 ? DateTime.MinValue : _texts.Keys.Min(); } }
        public static DateTime LastDate { get { return _texts.Count == 0 ? DateTime.MinValue : _texts.Keys.Max(); } }
    }
}
