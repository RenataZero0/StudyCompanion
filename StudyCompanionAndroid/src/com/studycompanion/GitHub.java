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
 * GitHub 闆嗘垚锛氳澶囩爜鐧诲綍 + REST API銆?
 *
 * 鈿狅笍 CLIENT_ID 闇€瑕佸厛鍦?GitHub 娉ㄥ唽涓€涓?OAuth App锛堝嬀閫?Enable Device Flow锛夛紝
 *    鎶婃嬁鍒扮殑 Client ID 濉湪涓嬮潰銆傝澶囩爜娴佺▼涓嶉渶瑕?client_secret锛屾墍浠ユ斁鍦ㄥ鎴风鏄畨鍏ㄧ殑銆?
 */
public class GitHub {

    /** OAuth App 鐨?Client ID锛堣澶囩爜娴佺▼涓嶉渶瑕?client_secret锛?*/
    public static final String CLIENT_ID = "Ov23limNvMWKQQ3qKGD1";

    public static final String OWNER = "RenataZero0";
    public static final String REPO = "StudyCompanion";
    /** repo 鏉冮檺锛氳绉佹湁浠撳簱鍐呭 + 鍐欏悓姝ユ枃浠?*/
    public static final String SCOPE = "repo";

    /** 鏈?APK 瀵瑰簲鐨?Release 鏍囩銆傛瘡娆″彂鐗堟椂涓?Release 涓€璧锋敼锛岀敤浜庡垽鏂湁娌℃湁鏂扮増銆?*/
    public static final String VERSION_TAG = "v2.1.6";

    public static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    public static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    public static final String API = "https://api.github.com";
    public static final String VERIFY_URL = "https://github.com/login/device";

    public static final String SYNC_PATH = "sync/progress.txt";
    /** 鎵撳崱璁板綍鐨?CSV 涔熶細鑷姩浼犲埌杩欓噷锛屼笉鐢ㄥ湪鎵嬫満涓婃墜鍔ㄥ鍑?*/
    public static final String CSV_PATH = "sync/StudyRecord.csv";

    private static final int TIMEOUT = 20000;

    public static boolean configured() {
        return CLIENT_ID != null && CLIENT_ID.length() > 10 && !CLIENT_ID.startsWith("PUT_");
    }

    // ================================================================== 浠ょ墝
    /**
     * 鏁板€兼瘮杈冪増鏈彿锛歛 &gt; b 杩斿洖 1锛岀浉绛?0锛宎 &lt; b 杩斿洖 -1銆?
     * 涓嶈兘鍙垽鏂€岀浉涓嶇浉绛夈€嶁€斺€?閭ｆ牱杩滅鐗堟湰姣旀湰鏈烘棫鏃朵篃浼氳褰撴垚鏈夋柊鐗堟湰銆?
     * 椤轰究涔熶慨鎺?2.1.10 &lt; 2.1.9 杩欑瀛楃涓叉瘮杈冪殑鍧戙€?
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
        sp(c).edit().remove("gh_token").remove("gh_user").apply();
    }

    // ================================================================== HTTP
    static String request(String method, String url, String body, String token, String accept) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        applyTls(conn);
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
        return s.length() > 220 ? s.substring(0, 220) + "鈥? : s;
    }

    static String form(String... kv) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(kv[i], "UTF-8")).append('=').append(URLEncoder.encode(kv[i + 1], "UTF-8"));
        }
        return sb.toString();
    }

    // ================================================================== 璁惧鐮佹祦绋?
    public static class DeviceCode {
        public String deviceCode = "", userCode = "", verifyUrl = VERIFY_URL;
        public int interval = 5, expiresIn = 900;
    }

    /** 绗竴姝ワ細鍚?GitHub 瑕佷竴涓敤鎴风爜 */
    public static DeviceCode deviceStart() throws Exception {
        if (!configured()) throw new Exception("杩樻病鏈夊～鍏?OAuth App 鐨?Client ID");
        String body = form("client_id", CLIENT_ID, "scope", SCOPE);
        HttpURLConnection conn = (HttpURLConnection) new URL(DEVICE_CODE_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        applyTls(conn);
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
        if (dc.deviceCode.length() == 0) throw new Exception("GitHub 娌℃湁杩斿洖 device_code锛? + trim(text));
        return dc;
    }

    /** 杞鎹?token銆俢ancel 鐢卞閮ㄧ疆浣嶏紱杩斿洖 access_token */
    public static String devicePoll(DeviceCode dc, Cancel cancel) throws Exception {
        long deadline = System.currentTimeMillis() + dc.expiresIn * 1000L;
        int interval = Math.max(5, dc.interval);
        while (System.currentTimeMillis() < deadline) {
            if (cancel != null && cancel.cancelled()) throw new Exception("宸插彇娑?);
            sleep(interval * 1000L);
            if (cancel != null && cancel.cancelled()) throw new Exception("宸插彇娑?);

            String body = form("client_id", CLIENT_ID, "device_code", dc.deviceCode,
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            String text;
            try {
                text = postForm(TOKEN_URL, body);
            } catch (Exception e) {
                continue;               // 缃戠粶鎶栧姩灏卞啀璇?
            }
            JSONObject j = new JSONObject(text);
            String err = j.optString("error", "");
            if ("authorization_pending".equals(err)) continue;
            if ("slow_down".equals(err)) { interval += 5; continue; }
            if ("expired_token".equals(err)) throw new Exception("浠ｇ爜宸茶繃鏈燂紝璇烽噸鏂扮櫥褰?);
            if ("access_denied".equals(err)) throw new Exception("浣犲湪椤甸潰涓婄偣浜嗗彇娑?);
            if (err.length() > 0) throw new Exception("GitHub 杩斿洖锛? + err);
            String tok = j.optString("access_token", "");
            if (tok.length() > 0) return tok;
        }
        throw new Exception("绛夊緟瓒呮椂锛岃閲嶆柊鐧诲綍");
    }

    static String postForm(String url, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        applyTls(conn);
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
    // Android 5.0+ 榛樿灏卞惎鐢?TLS1.2锛屼絾灏戞暟鑰佹満鍨?瀹氬埗 ROM 浼氭妸瀹冨叧鎺夛紝
    // GitHub 鍙帴鍙?TLS1.2+锛屾墍浠ヨ繖閲屾樉寮忔寚瀹氫竴娆★紙鎷夸笉鍒板氨閫€鍥炵郴缁熼粯璁わ級銆?
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

    // ================================================================== API
    public static JSONObject api(Context c, String path) throws Exception {
        return new JSONObject(request("GET", API + path, null, token(c), "application/vnd.github+json"));
    }

    public static JSONObject apiPut(Context c, String path, String json) throws Exception {
        return new JSONObject(request("PUT", API + path, json, token(c), "application/vnd.github+json"));
    }

    /** 鍙栧綋鍓嶇櫥褰曠敤鎴凤紝椤哄甫楠岃瘉 token 鏄惁杩樻湁鏁?*/
    public static String currentUser(Context c) throws Exception {
        return api(c, "/user").optString("login", "");
    }

    /** 鐢ㄦ寚瀹?token 鍙栫敤鎴蜂俊鎭紙鍒氱櫥褰曘€佽繕娌″瓨涓嬫潵鏃剁敤锛?*/
    public static String currentUserWith(Context c, String token) {
        try {
            return new JSONObject(request("GET", API + "/user", null, token,
                    "application/vnd.github+json")).optString("login", "");
        } catch (Exception e) { return ""; }
    }

    public static class Release {
        public String tag = "", name = "", notes = "", pageUrl = "";
        /** 璧?API 鐨勪笅杞藉湴鍧€锛岃浠ょ墝锛堢鏈変粨搴?/ 宸茬櫥褰曟椂鐢級 */
        public String apkUrl = "", exeUrl = "";
        /** 娴忚鍣ㄧ洿閾撅紝鍏紑浠撳簱鏃朵笉鐢ㄧ櫥褰曚篃鑳戒笅 */
        public String apkBrowser = "", exeBrowser = "";
        public long apkSize = 0, exeSize = 0;

        /** 鎸戜竴涓兘鐢ㄧ殑涓嬭浇鍦板潃 */
        public String apkDownload(boolean loggedIn) {
            if (loggedIn && apkUrl.length() > 0) return apkUrl;
            return apkBrowser.length() > 0 ? apkBrowser : apkUrl;
        }
    }

    /** 鏈€鏂?Release銆備粨搴撳叕寮€鏃朵笉闇€瑕佺櫥褰?*/
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

    /** 璇诲彇浠撳簱閲岀殑鏂囨湰鏂囦欢锛岃繑鍥?null 琛ㄧず鏂囦欢涓嶅瓨鍦?*/
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

    /** 璇诲彇鏂囦欢鐨?sha锛堟洿鏂版椂闇€瑕佸甫涓婏級锛屼笉瀛樺湪杩斿洖 null */
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
     * 涓嬭浇 release 璧勬簮锛岃嚜鍔ㄨ窡闅忛噸瀹氬悜銆?
     *
     * 娉ㄦ剰锛欰PI 璧勬簮鍦板潃锛坅pi.github.com/repos/.../releases/assets/xxx锛?*蹇呴』**甯︿护鐗岋紝
     * 鍖垮悕璁块棶浼?404锛涙祻瑙堝櫒鐩撮摼锛坓ithub.com/.../releases/download/...锛夊垯涓嶉渶瑕佷护鐗屻€?
     * 浠撳簱鍏紑鏃朵笉鐧诲綍涔熻蛋鍚庤€咃紝鎵€浠ヨ繖閲屽彧瀵?api.github.com 鍔?Authorization 澶淬€?
     */
    public static void downloadAsset(Context c, String url, java.io.File out,
                                     Progress p) throws Exception {
        boolean needsAuth = url != null && url.contains("api.github.com");
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Accept", "application/octet-stream");
        if (needsAuth) conn.setRequestProperty("Authorization", "Bearer " + token(c));
        conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
        applyTls(conn);
        int code = conn.getResponseCode();
        if (code == 302 || code == 301) {
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(loc).openConnection();
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "StudyCompanion-Android");
            applyTls(conn);
            code = conn.getResponseCode();
        }
        if (code < 200 || code >= 300) throw new Exception("涓嬭浇澶辫触 HTTP " + code);
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

    /** 浠?tag锛堝 v1.9锛夐噷鍙栧嚭鐗堟湰鍙凤紝鐢ㄤ簬姣旇緝 */
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
