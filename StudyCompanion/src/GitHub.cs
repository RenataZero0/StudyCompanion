using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Web.Script.Serialization;

namespace StudyCompanion
{
    /// <summary>
    /// GitHub 集成（设备码登录 + REST API + 打卡记录同步 + 检查更新）。
    ///
    /// ⚠️ CLIENT_ID 需要在 GitHub 注册一个 OAuth App（勾选 Enable Device Flow）后填到这里。
    ///    设备码流程不需要 client_secret，所以写在客户端是安全的。
    /// </summary>
    public static class GitHub
    {
        // ============================================================== TLS
        // csc.exe 编译出来的程序，运行时默认被当成旧版 .NET 应用，
        // SecurityProtocol 只开了 SSL3 + TLS1.0，而 GitHub 只接受 TLS1.2+，
        // 于是连接会失败并报「未能创建 SSL/TLS 安全通道」。
        // 这里在类型第一次被使用时显式打开 TLS1.2 / TLS1.3。
        static GitHub()
        {
            try
            {
                // 只开 TLS1.2。用「赋值」而不是 |=，
                // 否则默认的 Ssl3/Tls1.0 会留在列表里，反而容易握手失败。
                ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            }
            catch { }
        }

        /// <summary>
        /// 程序启动时调用一次。
        ///
        /// 注意：ClientId / VersionTag 是 const，编译器会把它们的取值**内联**，
        /// 访问它们**不会**触发静态构造函数。所以必须在发任何网络请求之前，
        /// 显式调用这个方法来确保 TLS 设置已经生效。
        /// </summary>
        public static void Init() { }

        /// <summary>OAuth App 的 Client ID（设备码流程不需要 client_secret）</summary>
        public const string ClientId = "Ov23limNvMWKQQ3qKGD1";

        public const string Owner = "RenataZero0";
        public const string Repo = "StudyCompanion";
        public const string Scope = "repo";

        /// <summary>本 exe 对应的 Release 标签，用于判断有没有新版</summary>
        public const string VersionTag = "v2.1.5";

        public const string SyncPath = "sync/progress.txt";
        /// <summary>打卡记录的 CSV 也会自动传到这里，不用手动导出</summary>
        public const string CsvPath = "sync/StudyRecord.csv";

        const string DeviceCodeUrl = "https://github.com/login/device/code";
        const string TokenUrl = "https://github.com/login/oauth/access_token";
        const string ApiBase = "https://api.github.com";
        public const string VerifyUrl = "https://github.com/login/device";

        const int Timeout = 20000;

        static readonly JavaScriptSerializer Json = new JavaScriptSerializer();

        public static bool Configured
        {
            get { return !string.IsNullOrEmpty(ClientId) && ClientId.Length > 10 && !ClientId.StartsWith("PUT_"); }
        }

        // ============================================================== 令牌存取
        static string TokenFile { get { return Path.Combine(Store.DataDir, "github.dat"); } }

        static string _token;
        static bool _loaded;

        public static string Token
        {
            get
            {
                if (!_loaded) { _loaded = true; _token = LoadToken(); }
                return _token;
            }
        }

        public static bool LoggedIn { get { return !string.IsNullOrEmpty(Token); } }

        static string LoadToken()
        {
            try
            {
                if (!File.Exists(TokenFile)) return null;
                byte[] enc = File.ReadAllBytes(TokenFile);
                byte[] raw = ProtectedData.Unprotect(enc, null, DataProtectionScope.CurrentUser);
                return Encoding.UTF8.GetString(raw);
            }
            catch { return null; }
        }

        public static void SaveToken(string token, string user)
        {
            _token = token; _loaded = true;
            try
            {
                byte[] raw = Encoding.UTF8.GetBytes(token);
                byte[] enc = ProtectedData.Protect(raw, null, DataProtectionScope.CurrentUser);
                File.WriteAllBytes(TokenFile, enc);
                File.WriteAllText(TokenFile + ".user", user ?? "", Encoding.UTF8);
            }
            catch { }
        }

        public static void Logout()
        {
            _token = null; _loaded = true;
            try { if (File.Exists(TokenFile)) File.Delete(TokenFile); } catch { }
            try { if (File.Exists(TokenFile + ".user")) File.Delete(TokenFile + ".user"); } catch { }
        }

        public static string User
        {
            get
            {
                try
                {
                    string f = TokenFile + ".user";
                    return File.Exists(f) ? File.ReadAllText(f, Encoding.UTF8) : "";
                }
                catch { return ""; }
            }
        }

        // ============================================================== HTTP
        static HttpWebRequest Req(string method, string url, string token, string accept)
        {
            var r = (HttpWebRequest)WebRequest.Create(url);
            r.Method = method;
            r.Timeout = Timeout;
            r.ReadWriteTimeout = Timeout;
            r.UserAgent = "StudyCompanion-Windows";
            r.Accept = accept;
            r.AllowAutoRedirect = true;
            if (!string.IsNullOrEmpty(token)) r.Headers["Authorization"] = "Bearer " + token;
            return r;
        }

        static string ReadBody(HttpWebResponse resp)
        {
            using (var s = resp.GetResponseStream())
            using (var sr = new StreamReader(s, Encoding.UTF8))
                return sr.ReadToEnd();
        }

        static string Send(HttpWebRequest r, string body)
        {
            try
            {
                if (body != null)
                {
                    byte[] b = Encoding.UTF8.GetBytes(body);
                    r.ContentType = "application/x-www-form-urlencoded";
                    r.ContentLength = b.Length;
                    using (var s = r.GetRequestStream()) s.Write(b, 0, b.Length);
                }
                using (var resp = (HttpWebResponse)r.GetResponse())
                    return ReadBody(resp);
            }
            catch (WebException e)
            {
                var resp = e.Response as HttpWebResponse;
                if (resp != null)
                {
                    string t = "";
                    try { t = ReadBody(resp); } catch { }
                    throw new Exception("HTTP " + (int)resp.StatusCode + "  " + Brief(t));
                }
                throw new Exception("网络错误：" + e.Message);
            }
        }

        static string Brief(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            s = s.Replace("\r", " ").Replace("\n", " ");
            return s.Length > 200 ? s.Substring(0, 200) + "…" : s;
        }

        public static Dictionary<string, object> Api(string method, string path, string jsonBody)
        {
            string url = ApiBase + path;
            string accept = "application/vnd.github+json";
            var r = Req(method, url, Token, accept);
            if (jsonBody != null)
            {
                byte[] b = Encoding.UTF8.GetBytes(jsonBody);
                r.ContentType = "application/json";
                r.ContentLength = b.Length;
                using (var s = r.GetRequestStream()) s.Write(b, 0, b.Length);
            }
            string text;
            try
            {
                using (var resp = (HttpWebResponse)r.GetResponse()) text = ReadBody(resp);
            }
            catch (WebException e)
            {
                var resp = e.Response as HttpWebResponse;
                if (resp != null)
                {
                    string t = "";
                    try { t = ReadBody(resp); } catch { }
                    throw new Exception("HTTP " + (int)resp.StatusCode + "  " + Brief(t));
                }
                throw new Exception("网络错误：" + e.Message);
            }
            return Json.Deserialize<Dictionary<string, object>>(text);
        }

        // ============================================================== 设备码流程
        public class DeviceCode
        {
            public string Device, UserCode, VerifyUrl = GitHub.VerifyUrl;
            public int Interval = 5, ExpiresIn = 900;
        }

        public static DeviceCode DeviceStart()
        {
            if (!Configured) throw new Exception("还没有填入 OAuth App 的 Client ID");
            string body = "client_id=" + Uri.EscapeDataString(ClientId) + "&scope=" + Uri.EscapeDataString(Scope);
            var r = Req("POST", DeviceCodeUrl, null, "application/json");
            string text = Send(r, body);
            var d = Json.Deserialize<Dictionary<string, object>>(text);
            var dc = new DeviceCode();
            dc.Device = Str(d, "device_code");
            dc.UserCode = Str(d, "user_code");
            string v = Str(d, "verification_uri");
            if (!string.IsNullOrEmpty(v)) dc.VerifyUrl = v;
            dc.Interval = Math.Max(5, Int(d, "interval", 5));
            dc.ExpiresIn = Int(d, "expires_in", 900);
            if (string.IsNullOrEmpty(dc.Device)) throw new Exception("GitHub 没有返回 device_code");
            return dc;
        }

        /// <summary>轮询换 token。cancelled 返回 true 时中止。</summary>
        public static string DevicePoll(DeviceCode dc, Func<bool> cancelled)
        {
            DateTime deadline = DateTime.UtcNow.AddSeconds(dc.ExpiresIn);
            int interval = Math.Max(5, dc.Interval);
            while (DateTime.UtcNow < deadline)
            {
                if (cancelled != null && cancelled()) throw new Exception("已取消");
                System.Threading.Thread.Sleep(interval * 1000);
                if (cancelled != null && cancelled()) throw new Exception("已取消");

                string body = "client_id=" + Uri.EscapeDataString(ClientId)
                            + "&device_code=" + Uri.EscapeDataString(dc.Device)
                            + "&grant_type=" + Uri.EscapeDataString("urn:ietf:params:oauth:grant-type:device_code");
                string text;
                try
                {
                    var r = Req("POST", TokenUrl, null, "application/json");
                    text = Send(r, body);
                }
                catch { continue; }        // 网络抖动就再试

                var d = Json.Deserialize<Dictionary<string, object>>(text);
                string err = Str(d, "error");
                if (err == "authorization_pending") continue;
                if (err == "slow_down") { interval += 5; continue; }
                if (err == "expired_token") throw new Exception("代码已过期，请重新登录");
                if (err == "access_denied") throw new Exception("你在页面上点了取消");
                if (!string.IsNullOrEmpty(err)) throw new Exception("GitHub 返回：" + err);
                string tok = Str(d, "access_token");
                if (!string.IsNullOrEmpty(tok)) return tok;
            }
            throw new Exception("等待超时，请重新登录");
        }

        public static string CurrentUser() { return CurrentUser(Token); }

        /// <summary>用指定令牌取当前登录用户名（刚拿到令牌、还没存下来时用）</summary>
        public static string CurrentUser(string token)
        {
            try
            {
                if (string.IsNullOrEmpty(token)) return "";
                var r = Req("GET", ApiBase + "/user", token, "application/vnd.github+json");
                string text;
                using (var resp = (HttpWebResponse)r.GetResponse()) text = ReadBody(resp);
                return Str(Json.Deserialize<Dictionary<string, object>>(text), "login");
            }
            catch { return ""; }
        }

        // ============================================================== Release
        public class Release
        {
            public string Tag = "", Name = "", Notes = "", PageUrl = "";
            /// <summary>走 API 的下载地址，要令牌</summary>
            public string ApkUrl = "", ExeUrl = "";
            /// <summary>浏览器直链，公开仓库不登录也能下</summary>
            public string ApkBrowser = "", ExeBrowser = "";
            public long ApkSize, ExeSize;

            /// <summary>挑一个能用的下载地址</summary>
            public string ExeDownload(bool loggedIn)
            {
                if (loggedIn && ExeUrl.Length > 0) return ExeUrl;
                return ExeBrowser.Length > 0 ? ExeBrowser : ExeUrl;
            }
        }

        public static Release LatestRelease()
        {
            var d = Api("GET", "/repos/" + Owner + "/" + Repo + "/releases/latest", null);
            var rel = new Release();
            rel.Tag = Str(d, "tag_name");
            rel.Name = Str(d, "name");
            rel.Notes = Str(d, "body");
            rel.PageUrl = Str(d, "html_url");

            object assetsObj;
            if (d.TryGetValue("assets", out assetsObj) && assetsObj != null)
            {
                // 注意：JavaScriptSerializer 把嵌套数组反序列化成 ArrayList，**不是** object[]，
                // 所以这里按 IEnumerable 处理，两种类型都能吃。
                var arr = assetsObj as System.Collections.IEnumerable;
                if (arr != null && !(assetsObj is string))
                {
                    foreach (object a in arr)
                    {
                        var m = a as Dictionary<string, object>;
                        if (m == null) continue;
                        string n = Str(m, "name");
                        string api = Str(m, "url");
                        string br = Str(m, "browser_download_url");
                        if (n.EndsWith(".apk", StringComparison.OrdinalIgnoreCase))
                        { rel.ApkUrl = api; rel.ApkBrowser = br; rel.ApkSize = Long(m, "size"); }
                        else if (n.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
                        { rel.ExeUrl = api; rel.ExeBrowser = br; rel.ExeSize = Long(m, "size"); }
                    }
                }
            }
            return rel;
        }

        // ============================================================== 仓库文件
        /// <summary>读取文本文件；不存在返回 null</summary>
        public static string ReadFile(string path)
        {
            try
            {
                var d = Api("GET", "/repos/" + Owner + "/" + Repo + "/contents/" + path, null);
                string b64 = Str(d, "content").Replace("\n", "").Replace("\r", "");
                if (b64.Length == 0) return "";
                return Encoding.UTF8.GetString(Convert.FromBase64String(b64));
            }
            catch (Exception e)
            {
                if (e.Message.Contains("HTTP 404")) return null;
                throw;
            }
        }

        static string FileSha(string path)
        {
            try { return Str(Api("GET", "/repos/" + Owner + "/" + Repo + "/contents/" + path, null), "sha"); }
            catch { return null; }
        }

        public static void WriteFile(string path, string content, string message)
        {
            var body = new Dictionary<string, object>();
            body["message"] = message;
            body["content"] = Convert.ToBase64String(Encoding.UTF8.GetBytes(content));
            string sha = FileSha(path);
            if (!string.IsNullOrEmpty(sha)) body["sha"] = sha;
            Api("PUT", "/repos/" + Owner + "/" + Repo + "/contents/" + path, Json.Serialize(body));
        }

        // ============================================================== 下载
        /// <summary>下载 release 资源。onProgress(已下载, 总大小)，总大小未知时为 -1</summary>
        public static void Download(string url, string outPath, Action<long, long> onProgress)
        {
            // API 资源地址匿名访问会 404，只有它才需要带令牌；
            // 浏览器直链（github.com/.../releases/download/...）不需要
            bool needsAuth = url != null && url.Contains("api.github.com");
            var r = Req("GET", url, needsAuth ? Token : null, "application/octet-stream");
            r.AllowAutoRedirect = true;
            using (var resp = (HttpWebResponse)r.GetResponse())
            using (var s = resp.GetResponseStream())
            using (var f = File.Create(outPath))
            {
                long total = resp.ContentLength;
                byte[] buf = new byte[16384];
                long got = 0;
                int n;
                while ((n = s.Read(buf, 0, buf.Length)) > 0)
                {
                    f.Write(buf, 0, n);
                    got += n;
                    if (onProgress != null) onProgress(got, total);
                }
            }
        }

        // ============================================================== 打卡同步
        public class SyncResult
        {
            public int Pulled, Total;
            public bool Uploaded;
            public bool CsvUploaded;
        }

        public static SyncResult Sync()
        {
            var res = new SyncResult();
            string remoteText = ReadFile(SyncPath);
            var remote = ParseKeys(remoteText);

            var merged = new HashSet<string>(Store.DoneKeys(), StringComparer.Ordinal);
            foreach (var k in remote) if (merged.Add(k)) res.Pulled++;
            Store.MergeKeys(merged);
            res.Total = merged.Count;

            if (!remote.SetEquals(merged))
            {
                WriteFile(SyncPath, DumpKeys(merged), "同步学习记录");
                res.Uploaded = true;
            }

            // 顺便把 CSV 也传到云端 —— 这样设置里就不需要「导出 / 导入 CSV」按钮了，
            // 想要表格直接去仓库拿 sync/StudyRecord.csv
            try
            {
                string csv = Store.ExportCsv();
                string remoteCsv = ReadFile(CsvPath);
                if (remoteCsv == null || remoteCsv != csv)
                {
                    WriteFile(CsvPath, csv, "同步学习记录 CSV");
                    res.CsvUploaded = true;
                }
            }
            catch
            {
                // CSV 传失败不影响打卡记录同步
            }
            return res;
        }

        public static HashSet<string> ParseKeys(string text)
        {
            var set = new HashSet<string>(StringComparer.Ordinal);
            if (text == null) return set;
            foreach (var raw in text.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n'))
            {
                string k = raw.Trim();
                if (k.Length == 0 || k.StartsWith("#")) continue;
                set.Add(k);
            }
            return set;
        }

        public static string DumpKeys(IEnumerable<string> keys)
        {
            var list = new List<string>(keys);
            list.Sort(StringComparer.Ordinal);
            var sb = new StringBuilder();
            foreach (var k in list) sb.Append(k).Append('\n');
            return sb.ToString();
        }

        // ============================================================== JSON 取值小工具
        public static string Str(Dictionary<string, object> d, string key)
        {
            object v;
            if (d != null && d.TryGetValue(key, out v) && v != null) return v.ToString();
            return "";
        }

        static int Int(Dictionary<string, object> d, string key, int def)
        {
            object v;
            if (d != null && d.TryGetValue(key, out v) && v != null)
            {
                int n;
                if (int.TryParse(v.ToString(), out n)) return n;
            }
            return def;
        }

        static long Long(Dictionary<string, object> d, string key)
        {
            object v;
            if (d != null && d.TryGetValue(key, out v) && v != null)
            {
                long n;
                if (long.TryParse(v.ToString(), out n)) return n;
            }
            return 0;
        }
    }
}
