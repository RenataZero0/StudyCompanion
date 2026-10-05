using System;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;

namespace StudyCompanion
{
    /// <summary>
    /// 更新日志的读取与自动更新。
    ///
    /// 内容优先级：
    ///   1. 从 GitHub 拉下来的缓存（%APPDATA%\StudyCompanion\CHANGELOG.md）
    ///   2. 程序自带的副本（和 exe 放一起，安装时一起装进去）
    ///
    /// 这样即使装的是旧版本，也能看到新版本改了什么；断网时也有东西可看。
    /// </summary>
    public static class Changelog
    {
        /// <summary>仓库根目录的 CHANGELOG.md（仓库是公开的，不需要登录）</summary>
        public const string Url =
            "https://raw.githubusercontent.com/RenataZero0/StudyCompanion/main/CHANGELOG.md";

        public static string CachePath { get { return Path.Combine(Store.Root, "CHANGELOG.md"); } }
        public static string BundledPath
        {
            get { return Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "CHANGELOG.md"); }
        }

        /// <summary>当前能读到的最新内容（缓存优先，其次内置）</summary>
        public static string Local()
        {
            try { if (File.Exists(CachePath)) return File.ReadAllText(CachePath, Encoding.UTF8); } catch { }
            try { if (File.Exists(BundledPath)) return File.ReadAllText(BundledPath, Encoding.UTF8); } catch { }
            return "";
        }

        public static bool HasCache { get { try { return File.Exists(CachePath); } catch { return false; } } }

        /// <summary>缓存文件的时间（没有则返回 null）</summary>
        public static DateTime? CacheTime
        {
            get { try { return File.Exists(CachePath) ? File.GetLastWriteTime(CachePath) : (DateTime?)null; } catch { return null; } }
        }

        /// <summary>
        /// 从 GitHub 拉最新的一份。成功返回 true 并把内容写进缓存。
        /// 失败不抛异常，返回 false 并给出原因 —— 更新日志拉不到不该打扰用户。
        /// </summary>
        public static bool Fetch(out string error)
        {
            error = null;
            try
            {
                var r = (HttpWebRequest)WebRequest.Create(Url);
                r.UserAgent = "StudyCompanion";
                r.Timeout = 15000;
                r.ReadWriteTimeout = 15000;
                r.CachePolicy = new System.Net.Cache.RequestCachePolicy(
                    System.Net.Cache.RequestCacheLevel.NoCacheNoStore);
                GitHub.ApplyProxy(r);

                string text;
                using (var resp = (HttpWebResponse)r.GetResponse())
                using (var s = resp.GetResponseStream())
                using (var sr = new StreamReader(s, Encoding.UTF8))
                    text = sr.ReadToEnd();

                if (string.IsNullOrEmpty(text) || text.Length < 50)
                {
                    error = "服务器返回的内容是空的";
                    return false;
                }

                File.WriteAllText(CachePath, text, new UTF8Encoding(false));
                return true;
            }
            catch (Exception ex)
            {
                error = ex.Message;
                return false;
            }
        }

        /// <summary>从内容里取出最新版本号（第一个 "## vX.Y.Z" 标题）</summary>
        public static string LatestVersionIn(string md)
        {
            if (string.IsNullOrEmpty(md)) return "";
            var m = Regex.Match(md, @"^##\s*v([0-9][0-9.]*)", RegexOptions.Multiline);
            return m.Success ? m.Groups[1].Value : "";
        }

        /// <summary>从内容里取出某个版本那一节的正文（用于"这一版改了什么"）</summary>
        public static string SectionOf(string md, string version)
        {
            if (string.IsNullOrEmpty(md) || string.IsNullOrEmpty(version)) return "";
            var all = Regex.Matches(md, @"^##\s+.*$", RegexOptions.Multiline);
            for (int i = 0; i < all.Count; i++)
            {
                string head = all[i].Value;
                if (head.IndexOf(version, StringComparison.OrdinalIgnoreCase) < 0) continue;
                int start = all[i].Index + head.Length;
                int end = (i + 1 < all.Count) ? all[i + 1].Index : md.Length;
                return md.Substring(start, end - start).Trim();
            }
            return "";
        }
    }
}
