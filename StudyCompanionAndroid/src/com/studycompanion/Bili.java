package com.studycompanion;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B 站视频检索。
 *
 * 直接跳到 B 站的搜索页，结果里常混着一堆无关内容（高数、考研、科普……）。
 * 这里改成：**拿综合排序的前 20 条，在本机按「跟 A Level 沾不沾边」重新打分**，
 * 再把最好的几个列出来给用户点。
 *
 * 打分只看标题 —— 够用，而且不需要额外请求。
 * 注意：不要用 order=click（按播放量），那个端点风控很严，直接 412。
 */
public class Bili {

    /** 一条搜索结果 */
    public static class Video {
        public String bvid = "";
        public String title = "";
        public String author = "";
        public String duration = "";
        public long play;
        public int score;
        public int videos = 1;   // 分 P 数（搜索接口直接给）

        public String page() { return "https://www.bilibili.com/video/" + bvid; }

        /** 列表里显示的一行说明 */
        public String subtitle() {
            StringBuilder sb = new StringBuilder();
            if (author != null && author.length() > 0) sb.append(author);
            if (play > 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(playText(play)).append("播放");
            }
            if (duration != null && duration.length() > 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(duration);
            }
            return sb.toString();
        }
    }

    static String playText(long n) {
        if (n >= 100000000L) return (n / 10000000L / 10.0) + "亿";
        if (n >= 10000L) return (n / 1000L / 10.0) + "万";
        return String.valueOf(n);
    }

    /** 一个分 P */
    public static class Part {
        public int page = 1;      // 第几 P（从 1 开始）
        public String part = "";  // 分 P 标题
        public String duration = "";
        public long cid;
    }

    /** 取某个视频的分 P 列表（view 接口）。失败抛异常，由调用方决定退回整个视频。 */
    public static List<Part> parts(String bvid) throws Exception {
        HttpURLConnection c = null;
        try {
            URL u = new URL("https://api.bilibili.com/x/web-interface/view?bvid="
                    + URLEncoder.encode(bvid, "UTF-8"));
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/120.0 Mobile Safari/537.36");
            c.setRequestProperty("Referer", "https://www.bilibili.com/");
            GitHub.applyTls(c);

            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            JSONObject j = new JSONObject(read(c.getInputStream()));
            if (j.optInt("code", -1) != 0) throw new Exception("B 站返回 " + j.optInt("code"));

            JSONObject data = j.optJSONObject("data");
            JSONArray pages = data == null ? null : data.optJSONArray("pages");
            List<Part> out = new ArrayList<Part>();
            if (pages == null) return out;
            for (int i = 0; i < pages.length(); i++) {
                JSONObject p = pages.optJSONObject(i);
                if (p == null) continue;
                Part part = new Part();
                part.page = p.optInt("page", i + 1);
                part.part = p.optString("part", "");
                part.duration = p.optString("duration", "");
                part.cid = p.optLong("cid", 0);
                out.add(part);
            }
            return out;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Exception ignored) { }
        }
    }

    // ================================================================ 分 P 匹配
    /** 中文主题词 → 英文同义词（把当天任务和视频分 P 标题对上） */
    static final Map<String, String[]> GLOSSARY = buildGlossary();

    static Map<String, String[]> buildGlossary() {
        Map<String, String[]> g = new HashMap<String, String[]>();
        String[][] rows = {
            {"位移", "displacement"},
            {"速度", "velocity", "speed"},
            {"速率", "speed", "rate"},
            {"加速度", "acceleration"},
            {"标量", "scalar"},
            {"矢量", "vector"},
            {"距离", "distance"},
            {"时间", "time"},
            {"图像", "graph", "diagram"},
            {"力", "force"},
            {"牛顿", "newton"},
            {"动量", "momentum"},
            {"冲量", "impulse"},
            {"能量", "energy"},
            {"功", "work"},
            {"功率", "power"},
            {"摩擦", "friction"},
            {"平衡", "equilibrium", "balance"},
            {"滑轮", "pulley"},
            {"碰撞", "collision"},
            {"动能", "kinetic"},
            {"势能", "potential"},
            {"运动学", "kinematics"},
            {"动力学", "dynamics"},
            {"波", "wave"},
            {"电", "electric"},
            {"电流", "current"},
            {"电压", "voltage", "potential difference"},
            {"电阻", "resistance", "resistivity"},
            {"概率", "probability"},
            {"正态", "normal distribution", "gaussian"},
            {"二项", "binomial"},
            {"几何", "geometric"},
            {"排列", "permutation"},
            {"组合", "combination"},
            {"期望", "expectation", "expected value", "mean"},
            {"方差", "variance"},
            {"标准差", "standard deviation"},
            {"函数", "function"},
            {"导数", "differentiation", "derivative"},
            {"积分", "integration", "integral"},
            {"对数", "logarithm", "log"},
            {"指数", "exponential"},
            {"复数", "complex"},
            {"矩阵", "matrix"},
            {"坐标", "coordinate"},
            {"圆", "circle"},
            {"三角", "trigonometry", "trig"},
            {"弧度", "radian", "circular measure"},
            {"扇形", "sector", "arc"},
            {"数列", "sequence", "series"},
            {"级数", "series"},
            {"假设检验", "hypothesis test"},
            {"置信区间", "confidence interval"},
            {"回归", "regression"},
            {"相关", "correlation"},
        };
        for (String[] r : rows) {
            String[] en = new String[r.length - 1];
            System.arraycopy(r, 1, en, 0, en.length);
            g.put(r[0], en);
        }
        return g;
    }

    /**
     * 从当天任务主题里挑出最匹配的分 P 下标。
     * topics 形如 "位移与速度|加速度|速率"，拆成词，查中英词表，
     * 再和每个分 P 标题比对，返回得分最高的下标；没匹配返回 -1。
     */
    public static int bestPart(List<Part> parts, String topics) {
        if (topics == null || topics.trim().length() == 0 || parts == null || parts.isEmpty()) return -1;

        String[] toks = topics.split("[、,，/\\\\|()（）\\s]|与|和|及");
        List<String> keys = new ArrayList<String>();
        for (String raw : toks) {
            String t = raw.trim();
            if (t.length() == 0) continue;
            keys.add(t.toLowerCase());
            String[] en = GLOSSARY.get(t);
            if (en != null) for (String e : en) keys.add(e);
        }

        int best = -1, bestScore = 0;
        for (int i = 0; i < parts.size(); i++) {
            String pt = parts.get(i).part.toLowerCase();
            int sc = 0;
            for (String k : keys) {
                if (k.length() >= 2 && pt.contains(k)) sc += 2;
                else if (k.length() >= 2 && pt.indexOf(k) >= 0) sc += 1;
            }
            if (sc > bestScore) { bestScore = sc; best = i; }
        }
        return bestScore > 0 ? best : -1;
    }

    // 同一个关键词只查一次，避免重复请求触发风控
    static final Map<String, List<Video>> CACHE = new HashMap<String, List<Video>>();
    static long lastCall;

    static final String API =
            "https://api.bilibili.com/x/web-interface/search/type?search_type=video&page=1&keyword=";

    /** 取前 n 个（已经按相关性排好） */
    public static List<Video> top(String keyword, int n) throws Exception {
        List<Video> all = CACHE.get(keyword);
        if (all == null) {
            // 两次请求之间留点间隔，B 站对高频访问会返回 412
            long wait = lastCall + 1200 - System.currentTimeMillis();
            if (wait > 0) Thread.sleep(wait);
            all = fetch(keyword);
            lastCall = System.currentTimeMillis();
            CACHE.put(keyword, all);
        }
        return all.size() <= n ? all : new ArrayList<Video>(all.subList(0, n));
    }

    static List<Video> fetch(String keyword) throws Exception {
        HttpURLConnection c = null;
        try {
            URL u = new URL(API + URLEncoder.encode(keyword, "UTF-8"));
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(12000);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/120.0 Mobile Safari/537.36");
            c.setRequestProperty("Referer", "https://www.bilibili.com/");
            c.setRequestProperty("Accept", "application/json");
            GitHub.applyTls(c);

            int code = c.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);

            String text = read(c.getInputStream());
            JSONObject j = new JSONObject(text);
            int biz = j.optInt("code", -1);
            if (biz != 0) throw new Exception("B 站返回 " + biz + " " + j.optString("message", ""));

            JSONObject data = j.optJSONObject("data");
            JSONArray arr = data == null ? null : data.optJSONArray("result");
            List<Video> out = new ArrayList<Video>();
            if (arr == null) return out;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String bv = o.optString("bvid", "");
                if (bv.length() == 0) continue;

                Video v = new Video();
                v.bvid = bv;
                v.title = strip(o.optString("title", ""));
                v.author = o.optString("author", "");
                v.duration = o.optString("duration", "");
                v.play = o.optLong("play", 0);
                v.videos = o.optInt("videos", 1);
                v.score = score(v.title);
                out.add(v);
            }

            // 先看相关性打分，同分再比播放量
            Collections.sort(out, new Comparator<Video>() {
                public int compare(Video a, Video b) {
                    if (a.score != b.score) return b.score - a.score;
                    return Long.compare(b.play, a.play);
                }
            });
            return out;
        } finally {
            if (c != null) try { c.disconnect(); } catch (Exception ignored) { }
        }
    }

    /**
     * 标题跟 A Level / CIE 的沾边程度。纯本地字符串判断，不额外请求。
     */
    static int score(String title) {
        if (title == null) return 0;
        String t = title.toLowerCase();
        int s = 0;

        if (t.contains("alevel") || t.contains("a level") || t.contains("a-level")
                || t.contains("a水准") || t.contains("英高")) s += 40;
        if (t.contains("cie") || t.contains("caie") || t.contains("剑桥") || t.contains("爱德思")
                || t.contains("edexcel") || t.contains("国际高中") || t.contains("国际课程")) s += 18;

        String[] codes = { "9709", "9231", "9702", "9701", "9700",
                           "p1", "p2", "p3", "p4", "m1", "m2", "s1", "s2", "fp", "fm" };
        for (String k : codes) {
            if (t.contains(k)) { s += 9; break; }
        }
        String[] good = { "精讲", "合集", "全套", "速通", "考点", "真题", "章节", "教程", "讲解" };
        for (String k : good) {
            if (t.contains(k)) { s += 6; break; }
        }
        String[] bad = { "考研", "专升本", "小学", "初中", "趣味", "鬼畜", "游戏", "广告" };
        for (String k : bad) {
            if (t.contains(k)) { s -= 25; break; }
        }
        return s;
    }

    /** 搜索结果里标题带 <em class="keyword"> 高亮标签，去掉 */
    static String strip(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean in = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '<') { in = true; continue; }
            if (ch == '>') { in = false; continue; }
            if (!in) sb.append(ch);
        }
        return sb.toString().trim();
    }

    static String read(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }
}
