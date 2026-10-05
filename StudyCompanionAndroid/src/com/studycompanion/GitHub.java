package com.studycompanion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * GitHub 集成：设备码登录 + REST API。
 *
 * ⚠️ CLIENT_ID 需要先在 GitHub 注册一个 OAuth App（勾选 Enable Device Flow），
 *    把拿到的 Client ID 填在下面。设备码流程不需要 client_secret，所以放在客户端是安全的。
 */
public class GitHub {

    /** OAuth App 的 Client ID（设备码流程不需要 client_secret） */
    public static final String CLIENT_ID = "Ov23limNvMWKQQ3qKGD1";

    public static final String OWNER = "RenataZero0";
    public static final String REPO = "StudyCompanion";
    /** repo 权限：读私有仓库内容 + 写同步文件 */
    public static final String SCOPE = "repo";

    /** 本 APK 对应的 Release 标签。每次发版时与 Release 一起改，用于判断有没有新版。 */
    public static final String VERSION_TAG = "v2.1.20";

    public static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    public static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    public static final String API = "https://api.github.com";
    public static final String VERIFY_URL = "https://github.com/login/device";

    public static final String SYNC_PATH = "sync/progress.txt";
    /** 打卡记录的 CSV 也会自动传到这里，不用在手机上手动导出 */
    public static final String CSV_PATH = "sync/StudyRecord.csv";

    private static final int TIMEOUT = 20000;

    public static boolean configured() {
        return CLIENT_ID != null && CLIENT_ID.length() > 10 && !CLIENT_ID.startsWith("PUT_");
    }

    // ================================================================== 令牌
    /**
     * 数值比较版本号：a &gt; b 返回 1，相等 0，a &lt; b 返回 -1。
     * 不能只判断「相不相等」—— 那样远端版本比本机旧时也会被当成有新版本。
     * 顺便也修掉 2.1.10 &lt; 2.1.9 这种字符串比较的坑。
     */
    public static int compareVersion(String a, String b) {
        int[] pa = parseVer(a), pb = parseVer(b);
        for (int i = 0; i < 3; i++) if (pa[i] != pb[i]) return pa[i] > pb[i] ? 1 : -1;
        return 0;
    }

    static int[] parseVer(String v) {
        int[] r = new int[3];
        if (v == null) return r;
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        String[] parts = v.split("\\.");
        for (int i = 0; i < 3 && i < parts.length; i++) {
            String d = "";
            for (int k = 0; k < parts[i].length(); k++) {
                char c = parts[i].charAt(k);
                if (c < '0' || c > '9') break;
                d += c;
            }
            try { r[i] = Integer.parseInt(d); } catch (Exception ignored) { }
        }
        return r;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("study", Context.MODE_PRIVATE);
    }

    public static String token(Context c) { return sp(c).getString("gh_token", ""); }
    public static String user(Context c) { return sp(c).getString("gh_user", ""); }
    public static boolean loggedIn(Context c) { return token(c).length() > 0; }

    public static void saveToken(Context c, String t, String u) {
        sp(c).edit().putString("gh_token", t).putString("gh_user", u).apply();
    }

    public static void logout(Context c) {
        sp(c).edit().remove("gh_token").remove("gh_user")
                .remove("gh_refresh").remove("gh_expires").apply();
    }

    // ---------------------------------------------------------------- 刷新令牌
    // GitHub 的 OAuth App 可以打开「Expire user access tokens」，那样 access token
    // 只有 8 小时寿命，必须用 refresh_token 换新的。
    // 之前只保存了 access_token，于是每天都要重新授权一次。

    /** 保存 refresh_token。rt 为空表示这个令牌长期有效，不需要续期。 */
    public static void saveRefresh(Context c, String rt, int expiresIn) {
        android.content.SharedPreferences.Editor e = sp(c).edit();
        if (rt == null || rt.length() == 0) {
            e.remove("gh_refresh").remove("gh_expires").apply();
            return;
        }
        // 提前 5 分钟续期，免得刚好卡在边界上失效
        long at = expiresIn > 0 ? System.currentTimeMillis() + (expiresIn - 300) * 1000L : 0L;
        e.putString("gh_refresh", rt).putLong("gh_expires", at).apply();
    }

    public static boolean canAutoRefresh(Context c) {
        return sp(c).getString("gh_refresh", "").length() > 0;
    }

    /**
     * 令牌快过期/已过期时用 refresh_token 换一个新的。换了返回 true。
     * Context 为 null 时只做判断不落盘（供不便传 Context 的地方调用）。
     */
    public static boolean ensureFreshToken(Context c) {
        if (c == null) return false;
        String rt = sp(c).getString("gh_refresh", "");
        if (rt.length() == 0) return false;                       // 长期令牌，不用管
        long at = sp(c).getLong("gh_expires", 0L);
        if (at <= 0) return false;
        if (System.currentTimeMillis() < at) return false;        // 还没到期

        try {
            String body = form("client_id", CLIENT_ID, "grant_type", "refresh_token",
                    "refresh_token", rt);
            JSONObject j = new JSONObject(postForm(TOKEN_URL, body));
            if (j.optString("error", "").length() > 0) return false;
            String tok = j.optString("access_token", "");
            if (tok.length() == 0) return false;

            saveToken(c, tok, sp(c).getString("gh_user", ""));
            String nrt = j.optString("refresh_token", "");
            saveRefresh(c, nrt.length() > 0 ? nrt : rt, j.optInt("expires_in", 0));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ================================================================== HTTP
    static String request(String method, String url, String body, String token, String accept) throws Exception {
        HttpURLConnection conn = open(url);
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        if (token != null && token.length() > 0)
            conn.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();
        }
        int code = conn.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = in == null ? "" : readAll(in);
        conn.disconnect();
        if (code < 200 || code >= 300)
            throw new Exception("HTTP " + code + (text.length() > 0 ? "  " + trim(text) : ""));
        return text;
    }

    static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    static String trim(String s) {
        s = s.replace("\n", " ").replace("\r", " ");
        return s.length() > 220 ? s.substring(0, 220) + "…" : s;
    }

    static String form(String... kv) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(kv[i], "UTF-8")).append('=').append(URLEncoder.encode(kv[i + 1], "UTF-8"));
        }
        return sb.toString();
    }

    // ================================================================== 设备码流程
    public static class DeviceCode {
        public String deviceCode = "", userCode = "", verifyUrl = VERIFY_URL;
        public int interval = 5, expiresIn = 900;
    }

    /** 第一步：向 GitHub 要一个用户码 */
    public static DeviceCode deviceStart() throws Exception {
        if (!configured()) throw new Exception("还没有填入 OAuth App 的 Client ID");
        String body = form("client_id", CLIENT_ID, "scope", SCOPE);
        HttpURLConnection conn = open(DEVICE_CODE_URL);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        int code = conn.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = in == null ? "" : readAll(in);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + "  " + trim(text));

        JSONObject j = new JSONObject(text);
        DeviceCode dc = new DeviceCode();
        dc.deviceCode = j.optString("device_code");
        dc.userCode = j.optString("user_code");
        dc.verifyUrl = j.optString("verification_uri", VERIFY_URL);
        dc.interval = j.optInt("interval", 5);
        dc.expiresIn = j.optInt("expires_in", 900);
        if (dc.deviceCode.length() == 0) throw new Exception("GitHub 没有返回 device_code：" + trim(text));
        return dc;
    }

    /** 轮询换 token。cancel 由外部置位；返回 access_token */
    public static String devicePoll(Context c, DeviceCode dc, Cancel cancel) throws Exception {
        long deadline = System.currentTimeMillis() + dc.expiresIn * 1000L;
        int interval = Math.max(5, dc.interval);
        while (System.currentTimeMillis() < deadline) {
            if (cancel != null && cancel.cancelled()) throw new Exception("已取消");
            sleep(interval * 1000L);
            if (cancel != null && cancel.cancelled()) throw new Exception("已取消");

            String body = form("client_id", CLIENT_ID, "device_code", dc.deviceCode,
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            String text;
            try {
                text = postForm(TOKEN_URL, body);
            } catch (Exception e) {
                continue;               // 网络抖动就再试
            }
            JSONObject j = new JSONObject(text);
            String err = j.optString("error", "");
            if ("authorization_pending".equals(err)) continue;
            if ("slow_down".equals(err)) { interval += 5; continue; }
            if ("expired_token".equals(err)) throw new Exception("代码已过期，请重新登录");
            if ("access_denied".equals(err)) throw new Exception("你在页面上点了取消");
            if (err.length() > 0) throw new Exception("GitHub 返回：" + err);
            String tok = j.optString("access_token", "");
            if (tok.length() > 0) {
                // GitHub 若开了令牌过期，会一起给 refresh_token 和 expires_in，
                // 必须存下来，否则过 8 小时就得重新授权。
                saveRefresh(c, j.optString("refresh_token", ""), j.optInt("expires_in", 0));
                return tok;
            }
        }
        throw new Exception("等待超时，请重新登录");
    }

    static String postForm(String url, String body) throws Exception {
        HttpURLConnection conn = open(url);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        int code = conn.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = in == null ? "" : readAll(in);
        conn.disconnect();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + "  " + trim(text));
        return text;
    }

    public interface Cancel { boolean cancelled(); }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    // ------------------------------------------------------------------ TLS
    // Android 5.0+ 默认就启用 TLS1.2，但少数老机型/定制 ROM 会把它关掉，
    // GitHub 只接受 TLS1.2+，所以这里显式指定一次（拿不到就退回系统默认）。
    private static javax.net.ssl.SSLSocketFactory TLS12;
    private static boolean TLS_TRIED;

    static void applyTls(HttpURLConnection conn) {
        if (!(conn instanceof HttpsURLConnection)) return;
        if (!TLS_TRIED) {
            TLS_TRIED = true;
            try {
                SSLContext c = SSLContext.getInstance("TLSv1.2");
                c.init(null, null, null);
                TLS12 = c.getSocketFactory();
            } catch (Exception e) { TLS12 = null; }
        }
        if (TLS12 != null) {
            try { ((HttpsURLConnection) conn).setSSLSocketFactory(TLS12); } catch (Exception ignored) { }
        }
    }

    // ------------------------------------------------------------------ 代理
    // 程序自己不能翻墙，但可以把你已经有的代理（Clash / v2ray 之类）填进来，
    // 之后所有请求都从那儿走。取值在 MainActivity 启动时和用户保存后各刷一次。
    private static java.net.Proxy PROXY;

    public static void refreshProxy(android.content.Context c) {
        PROXY = null;
        String p = Store.proxy();
        if (p == null || p.length() == 0) return;
        int i = p.lastIndexOf(':');
        if (i <= 0) return;
        try {
            PROXY = new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new java.net.InetSocketAddress(p.substring(0, i),
                            Integer.parseInt(p.substring(i + 1))));
        } catch (Exception ignored) { PROXY = null; }
    }

    /** 当前有没有在用代理（界面显示用） */
    public static boolean proxyOn() { return PROXY != null; }

    /** 统一开连接：套上代理、打开 TLS1.2 */
    static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) (PROXY == null
                ? new URL(url).openConnection()
                : new URL(url).openConnection(PROXY));
        applyTls(conn);
        return conn;
    }

    // ================================================================== API
    /**
     * 把底层报错翻译成用户看得懂的话 —— 说清「出了什么事」和「该怎么办」。
     *
     * 以前直接把 `HTTP 401  {"message":"Bad credentials",...}` 甩进弹窗，
     * 用户既看不懂也不知道下一步做什么。
     */
    public static String friendly(Exception e) {
        String m = e == null ? "" : String.valueOf(e.getMessage());
        if (m.length() == 0) return "发生了未知错误。";
        String low = m.toLowerCase(java.util.Locale.US);

        if (m.startsWith("HTTP 401"))
            return "登录已失效（HTTP 401 Bad credentials）\n\n"
                 + "本地保存的 GitHub 令牌过期或被撤销了。\n"
                 + "在「设置与工具 → GitHub 同步」里重新登录一次就好。";

        if (m.startsWith("HTTP 403")) {
            if (low.contains("rate limit") || low.contains("abuse"))
                return "触发 GitHub 访问频率限制（HTTP 403）\n\n"
                     + "短时间内请求太多次了，等几分钟再试。\n"
                     + "自动检查每天只运行一次；手动连续点也会触发。";
            return "GitHub 拒绝了这次请求（HTTP 403）\n\n"
                 + "通常是令牌权限不足，或者账号被限制。\n"
                 + "可以退出登录后重新授权一次。";
        }

        if (m.startsWith("HTTP 404"))
            return "找不到内容（HTTP 404）\n\n"
                 + "仓库不存在、被改名或已删除；\n"
                 + "也可能是没登录时访问了私有仓库。";

        if (m.startsWith("HTTP 409"))
            return "同步冲突（HTTP 409）\n\n"
                 + "远端文件被另一台设备改过了。再同步一次通常就好了。";

        if (m.startsWith("HTTP 422"))
            return "请求内容被 GitHub 拒绝（HTTP 422）\n\n"
                 + "一般出现在写入同步数据时，再试一次。";

        if (m.startsWith("HTTP 5"))
            return "GitHub 服务端出错（" + m.substring(0, Math.min(9, m.length())).trim()
                 + "）\n\n这是对方的问题，过一会儿再试。";

        if (low.contains("ssl") || m.contains("安全通道"))
            return "加密连接建立失败（TLS 握手失败）\n\n"
                 + "常见原因：系统时间不对、系统缺少 TLS 1.2，\n"
                 + "或者网络中间有设备在拦截 HTTPS。";

        if (m.contains("超时") || low.contains("timeout") || low.contains("timed out"))
            return "连接超时\n\n"
                 + "网络太慢或被挡住了。本仓库在 GitHub 上，\n"
                 + "国内使用通常需要先开代理。";

        if (m.contains("网络错误") || low.contains("unable to connect")
                || low.contains("name resolution") || low.contains("no such host")
                || low.contains("failed to connect") || low.contains("econnrefused"))
            return "连不上 GitHub\n\n"
                 + "检查一下网络；如果人在国内，通常需要开启代理再试。";

        // 认不出来就把原文给他，至少别丢信息
        return m;
    }

    /**
     * 公开接口不该被坏令牌连累。
     *
     * 本地可能存着已失效的令牌，带着它请求会 401 Bad credentials ——
     * 连查最新 Release 都失败，可仓库是公开的，本来不需要令牌。
     * 所以 401 时去掉令牌重试。
     *
     * ⚠️ 但**不能顺手 logout** —— 401 不代表令牌真的废了（网络抖动、代理插一脚、
     * 限流、GitHub 偶发抽风都会回 401）。以前这里无条件清掉令牌，
     * 结果偶发一次 401 就要重新授权。现在只有 /user 也回 401 才认定真的失效。
     */
    public static JSONObject api(Context c, String path) throws Exception {
        // 令牌可能快过期了，先用 refresh_token 续一下再发请求
        ensureFreshToken(c);
        String tok = token(c);
        try {
            return new JSONObject(request("GET", API + path, null, tok, "application/vnd.github+json"));
        } catch (Exception e) {
            if (tok == null || tok.length() == 0) throw e;
            if (!String.valueOf(e.getMessage()).startsWith("HTTP 401")) throw e;

            JSONObject j = new JSONObject(request("GET", API + path, null, null, "application/vnd.github+json"));
            if (tokenLooksDead(c, tok)) {
                try { logout(c); } catch (Exception ignored) { }
            }
            return j;
        }
    }

    /**
     * 令牌是不是真的废了。只有 /user 明确回 401 才算；
     * 网络错误、超时、5xx 一律当作「探不出来」，保留令牌别乱删。
     */
    static boolean tokenLooksDead(Context c, String tok) {
        if (tok == null || tok.length() == 0) return true;
        try {
            request("GET", API + "/user", null, tok, "application/vnd.github+json");
            return false;                       // 能读到用户信息 = 令牌好好的
        } catch (Exception e) {
            return String.valueOf(e.getMessage()).startsWith("HTTP 401");
        }
    }

    public static JSONObject apiPut(Context c, String path, String json) throws Exception {
        return new JSONObject(request("PUT", API + path, json, token(c), "application/vnd.github+json"));
    }

    /** 取当前登录用户，顺带验证 token 是否还有效 */
    public static String currentUser(Context c) throws Exception {
        return api(c, "/user").optString("login", "");
    }

    /** 用指定 token 取用户信息（刚登录、还没存下来时用） */
    public static String currentUserWith(Context c, String token) {
        try {
            return new JSONObject(request("GET", API + "/user", null, token,
                    "application/vnd.github+json")).optString("login", "");
        } catch (Exception e) { return ""; }
    }

    public static class Release {
        public String tag = "", name = "", notes = "", pageUrl = "";
        /** 走 API 的下载地址，要令牌（私有仓库 / 已登录时用） */
        public String apkUrl = "", exeUrl = "";
        /** 浏览器直链，公开仓库时不用登录也能下 */
        public String apkBrowser = "", exeBrowser = "";
        public long apkSize = 0, exeSize = 0;

        /** 挑一个能用的下载地址 */
        public String apkDownload(boolean loggedIn) {
            if (loggedIn && apkUrl.length() > 0) return apkUrl;
            return apkBrowser.length() > 0 ? apkBrowser : apkUrl;
        }
    }

    /** 最新 Release。仓库公开时不需要登录 */
    public static Release latestRelease(Context c) throws Exception {
        JSONObject j = api(c, "/repos/" + OWNER + "/" + REPO + "/releases/latest");
        Release r = new Release();
        r.tag = j.optString("tag_name", "");
        r.name = j.optString("name", "");
        r.notes = j.optString("body", "");
        r.pageUrl = j.optString("html_url", "");
        JSONArray arr = j.optJSONArray("assets");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject a = arr.optJSONObject(i);
                if (a == null) continue;
                String n = a.optString("name", "");
                String api = a.optString("url", "");
                String br = a.optString("browser_download_url", "");
                if (n.endsWith(".apk")) {
                    r.apkUrl = api; r.apkBrowser = br; r.apkSize = a.optLong("size", 0);
                } else if (n.endsWith(".exe")) {
                    r.exeUrl = api; r.exeBrowser = br; r.exeSize = a.optLong("size", 0);
                }
            }
        }
        return r;
    }

    /** 读取仓库里的文本文件，返回 null 表示文件不存在 */
    public static String readFile(Context c, String path) throws Exception {
        try {
            JSONObject j = api(c, "/repos/" + OWNER + "/" + REPO + "/contents/" + path);
            String b64 = j.optString("content", "").replace("\n", "").replace("\r", "");
            if (b64.length() == 0) return "";
            byte[] raw = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            return new String(raw, "UTF-8");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("HTTP 404")) return null;
            throw e;
        }
    }

    /** 读取文件的 sha（更新时需要带上），不存在返回 null */
    public static String fileSha(Context c, String path) {
        try {
            JSONObject j = api(c, "/repos/" + OWNER + "/" + REPO + "/contents/" + path);
            return j.optString("sha", null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeFile(Context c, String path, String content, String message) throws Exception {
        String sha = fileSha(c, path);
        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("content", android.util.Base64.encodeToString(content.getBytes("UTF-8"), android.util.Base64.NO_WRAP));
        if (sha != null) body.put("sha", sha);
        apiPut(c, "/repos/" + OWNER + "/" + REPO + "/contents/" + path, body.toString());
    }

    /**
     * 下载 release 资源，自动跟随重定向。
     *
     * 注意：API 资源地址（api.github.com/repos/.../releases/assets/xxx）**必须**带令牌，
     * 匿名访问会 404；浏览器直链（github.com/.../releases/download/...）则不需要令牌。
     * 仓库公开时不登录也走后者，所以这里只对 api.github.com 加 Authorization 头。
     */
    public static void downloadAsset(Context c, String url, java.io.File out,
                                     Progress p) throws Exception {
        boolean needsAuth = url != null && url.contains("api.github.com");
        HttpURLConnection conn = open(url);
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Accept", "application/octet-stream");
        if (needsAuth) conn.setRequestProperty("Authorization", "Bearer " + token(c));
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        int code = conn.getResponseCode();
        if (code == 302 || code == 301) {
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            conn = open(loc);
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
            code = conn.getResponseCode();
        }
        if (code < 200 || code >= 300) throw new Exception("下载失败 HTTP " + code);
        long total = conn.getContentLength();
        InputStream in = conn.getInputStream();
        java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
        byte[] buf = new byte[16384];
        long got = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            fos.write(buf, 0, n);
            got += n;
            if (p != null) p.onProgress(got, total);
        }
        fos.close();
        in.close();
        conn.disconnect();
    }

    public interface Progress { void onProgress(long got, long total); }

    /** 从 tag（如 v1.9）里取出版本号，用于比较 */
    public static int versionCode(String tag) {
        if (tag == null) return 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tag.length(); i++) {
            char ch = tag.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append(ch);
            else if (sb.length() > 0) break;
        }
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return 0; }
    }
}
