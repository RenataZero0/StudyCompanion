using System;
using System.Net;
using System.Text;

namespace StudyCompanion
{
    /// <summary>
    /// 桌面端 GitHub 链路的命令行自检 —— 用的就是主程序里那份代码。
    ///
    ///   csc /target:exe /main:GhTest /out:ghtest.exe src\*.cs tools\GhTest.cs
    ///   ghtest.exe            # 只测 TLS + 设备码请求（不需要登录）
    ///   ghtest.exe --full     # 再用已登录的令牌测私有仓库读 / Release
    /// </summary>
    public static class GhTest
    {
        static int fail = 0;

        static void Ok(string s) { Console.WriteLine("  [OK]   " + s); }
        static void Bad(string s) { fail++; Console.WriteLine("  [FAIL] " + s); }

        public static int Main(string[] args)
        {
            Console.OutputEncoding = Encoding.UTF8;
            Console.WriteLine("=== StudyCompanion 桌面端 GitHub 自检 ===");
            GitHub.Init();     // 必须最先调用（const 成员不会触发静态构造函数）
            Console.WriteLine("TLS 协议 = " + ServicePointManager.SecurityProtocol);
            Console.WriteLine("系统代理 = " + (WebRequest.DefaultWebProxy == null ? "(无)" : WebRequest.DefaultWebProxy.GetProxy(new Uri("https://github.com")).ToString()));
            Console.WriteLine("ClientID = " + GitHub.ClientId);
            Console.WriteLine("VersionTag = " + GitHub.VersionTag);
            Console.WriteLine();

            // --- 1. TLS 是否可用 ---
            Console.WriteLine("[1] HTTPS 连通性 / TLS 握手");
            try
            {
                var r = (HttpWebRequest)WebRequest.Create("https://api.github.com/rate_limit");
                r.UserAgent = "StudyCompanion-Selftest";
                r.Timeout = 20000;
                using (var resp = (HttpWebResponse)r.GetResponse())
                using (var s = resp.GetResponseStream())
                using (var sr = new System.IO.StreamReader(s))
                {
                    string body = sr.ReadToEnd();
                    Ok("HTTP " + (int)resp.StatusCode + "，收到 " + body.Length + " 字节");
                }
            }
            catch (Exception e) { Bad("HTTPS 请求失败：" + e.Message); }

            // --- 2. 设备码请求（这条就是用户点「登录 GitHub」时走的路）---
            Console.WriteLine();
            Console.WriteLine("[2] 设备码请求（等价于点「登录 GitHub」的第一步）");
            GitHub.DeviceCode dc = null;
            try
            {
                dc = GitHub.DeviceStart();
                Ok("拿到 user_code = " + dc.UserCode);
                Ok("verification_uri = " + dc.VerifyUrl);
                Ok("有效期 " + dc.ExpiresIn + " 秒，轮询间隔 " + dc.Interval + " 秒");
            }
            catch (Exception e) { Bad("设备码请求失败：" + e.Message); }

            // --- 3. 用令牌测私有仓库 ---
            Console.WriteLine();
            if (GitHub.LoggedIn)
            {
                Console.WriteLine("[3] 私有仓库读（已登录：" + GitHub.User + "）");
                try
                {
                    var rel = GitHub.LatestRelease();
                    Ok("最新 Release = " + rel.Tag);
                    Ok("APK 资产 url 就绪 = " + (rel.ApkUrl.Length > 0) + "，EXE = " + (rel.ExeUrl.Length > 0));

                    string remote = GitHub.ReadFile(GitHub.SyncPath);
                    Ok("读取 " + GitHub.SyncPath + " -> " + (remote == null ? "(云端还没有这个文件，正常)" : remote.Length + " 字符"));

                    string missing = GitHub.ReadFile("sync/definitely-not-here.txt");
                    if (missing == null) Ok("读取不存在的文件返回 null（符合预期）");
                    else Bad("不存在的文件竟返回了内容");
                }
                catch (Exception e) { Bad("仓库读取失败：" + e.Message); }
            }
            else
            {
                Console.WriteLine("[3] 跳过（本机还没登录；需要测就用主程序登录一次）");
            }

            Console.WriteLine();
            Console.WriteLine(fail == 0 ? "=== 全部通过 ===" : "=== 有 " + fail + " 项失败 ===");
            return fail == 0 ? 0 : 1;
        }
    }
}
