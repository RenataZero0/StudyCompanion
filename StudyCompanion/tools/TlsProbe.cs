using System;
using System.Net;
using System.Text;

namespace StudyCompanion
{
    /// <summary>找出这台机器上真正能连上 GitHub 的 TLS 配置（诊断用）</summary>
    public static class TlsProbe
    {
        static string Try(string label, SecurityProtocolType tls, bool useProxy)
        {
            try { ServicePointManager.SecurityProtocol = tls; } catch (Exception e) { return label + " -> 设置失败 " + e.Message; }
            try
            {
                var r = (HttpWebRequest)WebRequest.Create("https://api.github.com/rate_limit");
                r.UserAgent = "probe";
                r.Timeout = 12000;
                r.Proxy = useProxy ? WebRequest.DefaultWebProxy : null;
                using (var resp = (HttpWebResponse)r.GetResponse())
                    return string.Format("{0,-46} 代理={1,-3} -> OK  HTTP {2}", label, useProxy ? "on" : "off", (int)resp.StatusCode);
            }
            catch (Exception e)
            {
                string m = e.Message;
                if (m.Length > 46) m = m.Substring(0, 46);
                return string.Format("{0,-46} 代理={1,-3} -> 失败 {2}", label, useProxy ? "on" : "off", m);
            }
        }

        public static int Main(string[] args)
        {
            Console.OutputEncoding = Encoding.UTF8;
            Console.WriteLine("操作系统: " + Environment.OSVersion);
            Console.WriteLine(".NET    : " + Environment.Version);
            Console.WriteLine("默认 SecurityProtocol = " + ServicePointManager.SecurityProtocol);
            Console.WriteLine("DefaultWebProxy   = " + (WebRequest.DefaultWebProxy == null ? "(null)"
                : WebRequest.DefaultWebProxy.GetProxy(new Uri("https://api.github.com")).ToString()));
            Console.WriteLine();

            var combos = new object[,] {
                { "Tls12（只开 1.2）",            SecurityProtocolType.Tls12 },
                { "Tls12|Tls11|Tls",              SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls },
                { "Tls12|Tls13(12288)",           SecurityProtocolType.Tls12 | (SecurityProtocolType)12288 },
                { "SystemDefault(0)",             (SecurityProtocolType)0 },
                { "原样 Ssl3|Tls（默认）",         SecurityProtocolType.Ssl3 | SecurityProtocolType.Tls },
            };

            for (int i = 0; i < combos.GetLength(0); i++)
            {
                var label = (string)combos[i, 0];
                var tls = (SecurityProtocolType)combos[i, 1];
                Console.WriteLine(Try(label + "  [默认代理]", tls, true));
            }
            Console.WriteLine();
            for (int i = 0; i < combos.GetLength(0); i++)
            {
                var label = (string)combos[i, 0];
                var tls = (SecurityProtocolType)combos[i, 1];
                Console.WriteLine(Try(label + "  [不用代理]", tls, false));
            }
            return 0;
        }
    }
}
