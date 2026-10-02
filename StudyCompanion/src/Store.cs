using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using Microsoft.Win32;

namespace StudyCompanion
{
    /// <summary>完成状态持久化 + 统计 + 开机自启</summary>
    public static class Store
    {
        static readonly HashSet<string> _done = new HashSet<string>();
        const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
        const string AppName = "StudyCompanion";

        /// <summary>
        /// 数据根目录：%APPDATA%\StudyCompanion
        ///
        /// 以前是放在 exe 旁边的 data\ 里 —— 自己编译、绿色运行没问题，
        /// 但装到 Program Files 之后那个目录是只读的，写不进去。
        /// 所以要改成用户目录，并做一次从旧位置的搬迁。
        /// </summary>
        public static string Root
        {
            get
            {
                string d = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "StudyCompanion");
                Directory.CreateDirectory(d);
                MigrateOnce(d);
                return d;
            }
        }

        static bool _migrated;

        /// <summary>
        /// 第一次在用户目录下运行时，把老版本（exe 同级目录）里的数据搬过来，
        /// 这样升级成安装版不会丢打卡记录。
        /// </summary>
        static void MigrateOnce(string newRoot)
        {
            if (_migrated) return;
            _migrated = true;
            try
            {
                string oldRoot = AppDomain.CurrentDomain.BaseDirectory;
                // 已经是同一个位置就没什么可搬的
                if (string.Equals(Path.GetFullPath(oldRoot).TrimEnd('\\'),
                                  Path.GetFullPath(newRoot).TrimEnd('\\'), StringComparison.OrdinalIgnoreCase))
                    return;

                string oldData = Path.Combine(oldRoot, "data");
                string newData = Path.Combine(newRoot, "data");
                Directory.CreateDirectory(newData);

                if (Directory.Exists(oldData))
                {
                    foreach (var f in Directory.GetFiles(oldData))
                    {
                        string target = Path.Combine(newData, Path.GetFileName(f));
                        if (!File.Exists(target)) File.Copy(f, target, false);
                    }
                }

                // settings.ini 也搬过来
                string oldIni = Path.Combine(oldRoot, "settings.ini");
                string newIni = Path.Combine(newRoot, "settings.ini");
                if (File.Exists(oldIni) && !File.Exists(newIni)) File.Copy(oldIni, newIni, false);
            }
            catch { }
        }

        /// <summary>用户数据目录（打卡记录、课本索引、令牌等）</summary>
        public static string DataDir
        {
            get
            {
                string d = Path.Combine(Root, "data");
                Directory.CreateDirectory(d);
                return d;
            }
        }
        static string SettingsPath { get { return Path.Combine(Root, "settings.ini"); } }
        static string ProgressPath { get { return Path.Combine(DataDir, "progress.tsv"); } }

        public static void Load()
        {
            _done.Clear();
            try
            {
                if (File.Exists(ProgressPath))
                    foreach (var line in File.ReadAllLines(ProgressPath, Encoding.UTF8))
                    {
                        var s = line.Trim();
                        if (s.Length > 0 && !s.StartsWith("#")) _done.Add(s);
                    }
            }
            catch { }
        }

        public static void Save()
        {
            try
            {
                var sb = new StringBuilder();
                sb.AppendLine("# StudyCompanion 完成记录：日期|时段");
                foreach (var k in _done.OrderBy(x => x, StringComparer.Ordinal)) sb.AppendLine(k);
                File.WriteAllText(ProgressPath, sb.ToString(), new UTF8Encoding(false));
            }
            catch { }
        }

        public static string Key(DateTime d, Slot s) { return d.ToString("yyyy-MM-dd") + "|" + s.Key; }
        public static bool IsDone(DateTime d, Slot s) { return _done.Contains(Key(d, s)); }

        /// <summary>所有已完成的「日期|时段」键（云端同步用）</summary>
        public static IEnumerable<string> DoneKeys() { return _done.ToList(); }

        /// <summary>并入一批键（并集，不删除本地记录）。返回新增条数</summary>
        public static int MergeKeys(IEnumerable<string> keys)
        {
            int added = 0;
            foreach (var k in keys)
                if (!string.IsNullOrEmpty(k) && !k.StartsWith("#") && _done.Add(k)) added++;
            if (added > 0) Save();
            return added;
        }

        public static bool SetDone(DateTime d, Slot s, bool v)
        {
            string k = Key(d, s);
            bool changed = v ? _done.Add(k) : _done.Remove(k);
            if (changed) Save();
            return changed;
        }

        public static void Toggle(DateTime d, Slot s) { SetDone(d, s, !IsDone(d, s)); }

        public static int DoneCount(DateTime d)
        {
            var dp = ScheduleData.GetDay(d);
            if (dp == null) return 0;
            int n = 0;
            foreach (var s in dp.Slots) if (IsDone(d, s)) n++;
            return n;
        }

        public static bool IsPerfectDay(DateTime d)
        {
            var dp = ScheduleData.GetDay(d);
            if (dp == null || dp.Slots.Count == 0) return false;
            return DoneCount(d) >= dp.Slots.Count;
        }

        public static IEnumerable<DateTime> DoneDates
        {
            get
            {
                var set = new HashSet<DateTime>();
                foreach (var k in _done)
                {
                    if (k.Length < 10) continue;
                    DateTime dt;
                    if (DateTime.TryParseExact(k.Substring(0, 10), "yyyy-MM-dd",
                        CultureInfo.InvariantCulture, DateTimeStyles.None, out dt)) set.Add(dt);
                }
                return set.OrderBy(x => x);
            }
        }

        /// <summary>连续打卡天数（当天完成 ≥1 个时段即算打卡；今天还没开始则从昨天往前数）</summary>
        public static int Streak()
        {
            var doneSet = new HashSet<DateTime>(DoneDates);
            DateTime d = DateTime.Today;
            if (!doneSet.Contains(d)) d = d.AddDays(-1);
            int n = 0;
            var first = ScheduleData.FirstDate; var last = ScheduleData.LastDate;
            while (doneSet.Contains(d))
            {
                if (first != DateTime.MinValue && (d < first || d > last)) break;
                n++; d = d.AddDays(-1);
            }
            return n;
        }

        public static int BestStreak()
        {
            var doneSet = new HashSet<DateTime>(DoneDates);
            if (doneSet.Count == 0) return 0;
            int best = 0, cur = 0;
            DateTime d = doneSet.Min();
            var last = doneSet.Max();
            while (d <= last)
            {
                if (doneSet.Contains(d)) { cur++; if (cur > best) best = cur; }
                else cur = 0;
                d = d.AddDays(1);
            }
            return best;
        }

        public static double TotalHours()
        {
            double mins = 0;
            foreach (var k in _done)
            {
                int i = k.IndexOf('|');
                if (i < 10) continue;
                DateTime dt;
                if (!DateTime.TryParseExact(k.Substring(0, 10), "yyyy-MM-dd",
                    CultureInfo.InvariantCulture, DateTimeStyles.None, out dt)) continue;
                var dp = ScheduleData.GetDay(dt);
                if (dp == null) continue;
                string sk = k.Substring(i + 1);
                foreach (var s in dp.Slots) if (s.Key == sk) { mins += s.DurationMinutes; break; }
            }
            return mins / 60.0;
        }

        public static int TotalDoneSlots() { return _done.Count; }

        /// <summary>本周（周一–周日）已完成的时段数 / 总时段数</summary>
        public static void WeekProgress(out int done, out int total)
        {
            done = 0; total = 0;
            DateTime monday = DateTime.Today.AddDays(-(((int)DateTime.Today.DayOfWeek + 6) % 7));
            for (int i = 0; i < 7; i++)
            {
                var d = monday.AddDays(i);
                var dp = ScheduleData.GetDay(d);
                if (dp == null) continue;
                total += dp.Slots.Count;
                done += DoneCount(d);
            }
        }

        public static string ExportCsv()
        {
            string p = Path.Combine(DataDir, "学习记录_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".csv");
            var sb = new StringBuilder();
            sb.AppendLine("日期,星期,时段,科目,内容,状态");
            foreach (var d in ScheduleData.AllDates)
            {
                var dp = ScheduleData.GetDay(d);
                if (dp == null) continue;
                foreach (var s in dp.Slots)
                {
                    sb.Append(d.ToString("yyyy-MM-dd")).Append(',')
                      .Append("周" + "日一二三四五六"[(int)d.DayOfWeek]).Append(',')
                      .Append(s.Key).Append(',')
                      .Append(Csv(s.Subject)).Append(',')
                      .Append(Csv(s.Title)).Append(',')
                      .Append(IsDone(d, s) ? "已完成" : "未完成").AppendLine();
                }
            }
            File.WriteAllText(p, sb.ToString(), new UTF8Encoding(true));
            return p;
        }

        static string Csv(string v)
        {
            if (string.IsNullOrEmpty(v)) return "";
            v = v.Replace("\"", "\"\"");
            return "\"" + v + "\"";
        }

        /// <summary>从导出的 CSV 里恢复完成记录；返回 (新增条数, 已存在条数, 无法识别条数)</summary>
        public static void ImportCsv(string path, out int added, out int existed, out int bad)
        {
            added = existed = bad = 0;
            var lines = File.ReadAllLines(path, Encoding.UTF8);
            foreach (var raw in lines)
            {
                if (raw.Trim().Length == 0) continue;
                var f = SplitCsvLine(raw);
                if (f.Count < 3) { bad++; continue; }
                string date = f[0].Trim();
                if (date == "日期") continue;                       // 表头
                string slot = f.Count > 2 ? f[2].Trim() : "";
                string state = f.Count > 5 ? f[5].Trim() : "已完成";

                DateTime dt;
                if (!DateTime.TryParse(date, out dt) || slot.Length == 0 ||
                    slot.IndexOf(':') < 0) { bad++; continue; }

                if (state.Contains("未完成")) continue;             // 只导入已完成的
                string key = dt.ToString("yyyy-MM-dd") + "|" + slot;
                if (_done.Contains(key)) existed++;
                else { _done.Add(key); added++; }
            }
            if (added > 0) Save();
        }

        static List<string> SplitCsvLine(string line)
        {
            var res = new List<string>();
            var sb = new StringBuilder();
            bool q = false;
            for (int i = 0; i < line.Length; i++)
            {
                char c = line[i];
                if (q)
                {
                    if (c == '"')
                    {
                        if (i + 1 < line.Length && line[i + 1] == '"') { sb.Append('"'); i++; }
                        else q = false;
                    }
                    else sb.Append(c);
                }
                else
                {
                    if (c == '"') q = true;
                    else if (c == ',') { res.Add(sb.ToString()); sb.Length = 0; }
                    else sb.Append(c);
                }
            }
            res.Add(sb.ToString());
            return res;
        }

        // ------------------------------------------------------------------ 开机自启
        public static bool AutoStartEnabled
        {
            get
            {
                try
                {
                    using (var k = Registry.CurrentUser.OpenSubKey(RunKey, false))
                    {
                        if (k == null) return false;
                        var v = k.GetValue(AppName) as string;
                        return !string.IsNullOrEmpty(v);
                    }
                }
                catch { return false; }
            }
        }

        public static bool SetAutoStart(bool on)
        {
            try
            {
                using (var k = Registry.CurrentUser.OpenSubKey(RunKey, true))
                {
                    if (k == null) return false;
                    if (on)
                    {
                        string exe = System.Reflection.Assembly.GetEntryAssembly().Location;
                        k.SetValue(AppName, "\"" + exe + "\" --tray");
                    }
                    else k.DeleteValue(AppName, false);
                }
                return true;
            }
            catch { return false; }
        }

        // ------------------------------------------------------------------ 设置
        /// <summary>点右上角 X 的行为：true = 最小化到托盘（默认，保证提醒能触发），false = 直接退出</summary>
        public static bool CloseToTray
        {
            get { return GetSetting("CloseToTray", "1") != "0"; }
            set { SetSetting("CloseToTray", value ? "1" : "0"); }
        }

        public static string GetSetting(string key, string def)
        {
            try
            {
                string f = SettingsPath;
                if (!File.Exists(f)) return def;
                foreach (var line in File.ReadAllLines(f, Encoding.UTF8))
                {
                    var s = line.Trim();
                    if (s.StartsWith(key + "=", StringComparison.OrdinalIgnoreCase))
                        return s.Substring(key.Length + 1).Trim();
                }
            }
            catch { }
            return def;
        }

        public static void SetSetting(string key, string val)
        {
            try
            {
                string f = SettingsPath;
                var lines = File.Exists(f) ? File.ReadAllLines(f, Encoding.UTF8).ToList() : new List<string>();
                bool found = false;
                for (int i = 0; i < lines.Count; i++)
                    if (lines[i].TrimStart().StartsWith(key + "=", StringComparison.OrdinalIgnoreCase))
                    { lines[i] = key + "=" + val; found = true; }
                if (!found) lines.Add(key + "=" + val);
                File.WriteAllText(f, string.Join("\r\n", lines) + "\r\n", new UTF8Encoding(false));
            }
            catch { }
        }
    }
}
