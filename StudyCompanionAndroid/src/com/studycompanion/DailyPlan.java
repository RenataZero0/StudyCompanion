package com.studycompanion;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「这一段具体怎么做」——把课表里的一行（14:00-15:00 应用数学 / 读 §1.3 …）
 * 展开成带时间戳的一步一步。
 *
 * 数据来自随 APK 打包的三张小表（assets/*.tsv，必须放在 assets 根目录，
 * 详见 build.ps1 里的说明——aapt2 在 Windows 上会把子目录写成反斜杠），
 * **完全离线**——平板没有代理、连不上 GitHub 也照样能用。
 *
 *   exercises.tsv  BOOK \t section \t ex \t q \t page \t minutes
 *   checks.tsv     BOOK \t chapter \t check
 *   books.tsv      BOOK \t 中文名
 */
public class DailyPlan {

    public static class Step {
        public String time = "";      // "14:03"
        public int minutes;
        public String text = "";      // 主文本
        public String note = "";      // 缩进的小字说明（可空）
        public boolean head = false;  // 是不是「小节标题」这种非可勾选项
        public boolean fixed = false;   // 分钟数是课表原文里写死的，不许被 fitTail 改
        public boolean untimed = false; // 「合格线 / 说明」这种不定时的行：不显示分钟、时钟也不走
        public String raw = "";         // 原文（只有写了分钟数的行才填）——收尾后用它把正文里的数字改回来
    }

    static class Ex {
        String ex = "", q = "";
        int page, minutes;
    }

    static final Map<String, Ex> EX = new HashMap<String, Ex>();
    static final Map<String, String> CHECK = new HashMap<String, String>();
    static final Map<String, String> BOOK_CN = new HashMap<String, String>();
    static boolean loaded = false;

    static final Map<String, String> ALIAS = new HashMap<String, String>();
    static {
        ALIAS.put("PM1", "P1");
        ALIAS.put("PM23", "P2/3");
        ALIAS.put("MECH", "M1");
        ALIAS.put("PHY", "Phy");
    }

    static final Pattern TIME_RE = Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})\\s+(.+)$");
    static final Pattern NEW_RE = Pattern.compile("^新课\\s+([A-Za-z0-9/]+)\\s+(Ch\\d+)\\s+(.*?)\\s*(§.*)?$");
    static final Pattern PRAC_RE = Pattern.compile("^练习\\s+([A-Za-z0-9/]+)\\s+(Ch\\d+)\\s+(.*?)\\s*[（(](§.*?)[）)]\\s*$");
    static final Pattern SEC_RE = Pattern.compile("§\\s*(\\d+\\.\\d+)");
    static final Pattern READ_RE = Pattern.compile("^读\\s*§\\s*(\\d+\\.\\d+)\\s*([^：:]*)");
    static final Pattern DASH_RE = Pattern.compile("^[·•]\\s*(.*)$");
    static final Pattern RANGE_RE = Pattern.compile("^(\\d+)\\s*[–\\-~]\\s*(\\d+)$");
    static final Pattern DUR1 = Pattern.compile("(\\d+)\\s*[′'’]");
    static final Pattern DUR2 = Pattern.compile("(\\d+)\\s*分钟");

    // ------------------------------------------------------------------ 加载
    public static synchronized void load(Context c) {
        if (loaded) return;
        loaded = true;
        read(c, "exercises.tsv", new Row() {
            public void on(String[] f) {
                if (f.length < 6) return;
                Ex e = new Ex();
                e.ex = f[2];
                e.q = f[3];
                e.page = parseInt(f[4]);
                e.minutes = parseInt(f[5]);
                EX.put(f[0] + ":" + f[1], e);
            }
        });
        read(c, "checks.tsv", new Row() {
            public void on(String[] f) {
                if (f.length < 3) return;
                CHECK.put(f[0] + ":" + f[1], f[2]);
            }
        });
        read(c, "books.tsv", new Row() {
            public void on(String[] f) {
                if (f.length < 2) return;
                BOOK_CN.put(f[0], f[1]);
            }
        });
    }

    interface Row { void on(String[] f); }

    static void read(Context c, String asset, Row sink) {
        InputStream in = null;
        try {
            in = c.getAssets().open(asset);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.length() == 0 || line.startsWith("#")) continue;
                if (!line.contains("\t")) continue;
                sink.on(line.split("\t", -1));
            }
            r.close();
        } catch (Exception ignored) {
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
        }
    }

    static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    static String books(String b) {
        String cn = BOOK_CN.get(b);
        return cn == null ? b : cn;
    }

    static Ex exOf(String book, String sec) {
        String b = ALIAS.containsKey(book) ? ALIAS.get(book) : book;
        Ex e = EX.get(b + ":" + sec);
        if (e == null) e = EX.get("PHY:" + sec);
        return e;
    }

    static String sectionName(com.studycompanion.ScheduleData.Slot s, String sec) {
        for (int i = 1; i < s.body.size(); i++) {
            String ln = s.body.get(i).trim();
            if (ln.startsWith("·") || ln.startsWith("•")) ln = ln.substring(1).trim();
            Matcher m = READ_RE.matcher(ln);
            if (m.find() && m.group(1).equals(sec)) {
                String nm = m.group(2) == null ? "" : m.group(2).trim();
                nm = nm.replaceAll("[：:]$", "").trim();
                nm = nm.replaceAll("\\s*p\\.\\s*\\d+\\s*$", "").trim();
                return nm;
            }
        }
        return "";
    }

    static String hhmm(int total) {
        return String.format("%02d:%02d", (total / 60) % 24, total % 60);
    }

    /** 这一行正文里自己写了多少分钟（「（30 分钟）」「20′」）。没写就返回 0。 */
    public static int statedMinutes(String text) {
        if (text == null || text.length() == 0) return 0;
        Matcher m = DUR1.matcher(text);
        boolean hit = m.find();
        if (!hit) { m = DUR2.matcher(text); hit = m.find(); }
        return hit ? parseInt(m.group(1)) : 0;
    }

    /** 把正文里那个分钟数换掉（跟 statedMinutes 认的是同一处数字）。 */
    public static String withMinutes(String text, int mm) {
        if (text == null || text.length() == 0) return text;
        Matcher g = DUR1.matcher(text);
        boolean hit = g.find();
        if (!hit) { g = DUR2.matcher(text); hit = g.find(); }
        if (!hit) return text;
        int a = g.start(1), b = g.end(1);
        return text.substring(0, a) + mm + text.substring(b);
    }

    // ------------------------------------------------------------------ 展开
    public static List<Step> steps(Context c, com.studycompanion.ScheduleData.Slot s) {
        List<Step> out = stepsRaw(c, s);
        fitTail(out, s);
        // 收尾可能动过分钟数：凡是正文里写了时长的行，把正文里的数字同步成最终值，
        // 这样「正文写（30 分钟）」和「步骤显示 25′」永远不会同时出现。
        for (int i = 0; i < out.size(); i++) {
            Step st = out.get(i);
            if (!st.head && !st.untimed && st.raw.length() > 0)
                st.text = withMinutes(st.raw, st.minutes);
        }
        return out;
    }

    // 整数除法、「至少 N 分钟」的下限，都会让步骤总时长和时段对不上：
    // 少了最后一步提前收尾，多了最后一步被顶出时段外。
    // 统一收尾：多的补给最后一步，超的从最长的一步开始往下扣，谁也不许低于 MIN_MIN。
    static final int MIN_MIN = 3;
    // 收尾时的下限只保证「不是 0」：这里再用 3 分钟卡，
    // 就会出现「把所有步骤顶到 3 分钟、只能回头去砍课表原文写死的数字」的荒唐结果。
    static final int MIN_TAIL = 1;

    static void fitTail(List<Step> out, com.studycompanion.ScheduleData.Slot s) {
        if (s == null || out.isEmpty()) return;
        List<Integer> idx = new ArrayList<Integer>();   // 可以随便调的（时长是程序算出来的）
        List<Integer> all = new ArrayList<Integer>();   // 所有定时行（含课表原文里写死的）
        int sum = 0;
        for (int i = 0; i < out.size(); i++) {
            Step st = out.get(i);
            if (st.head || st.untimed) continue;        // 提示行 / 不定时行不参与
            if (st.minutes < MIN_TAIL) st.minutes = MIN_TAIL;
            sum += st.minutes;
            all.add(Integer.valueOf(i));
            if (!st.fixed) idx.add(Integer.valueOf(i));
        }
        if (all.isEmpty()) return;
        // 实在没有「程序算出来的」行时，才允许动课表原文里的数字，
        // 否则宁可让原文的数字保持原样。
        if (idx.isEmpty()) idx = all;

        int diff = s.duration() - sum;
        if (diff > 0) {
            int flex = 0;
            for (int i = 0; i < idx.size(); i++) {
                if (!out.get(idx.get(i).intValue()).fixed) flex++;
            }
            if (flex == 0) {
                // 一个「程序算出来的」行都没有：只能加在最后一步上
                int lastI = idx.get(idx.size() - 1).intValue();
                out.get(lastI).minutes += diff;
                return;
            }
            // 富余的时间平摊给「程序算出来的」那些行，免得全堆在最后一步上
            int each = diff / flex, rest = diff % flex;
            for (int i = 0; i < idx.size(); i++) {
                int k = idx.get(i).intValue();
                Step st = out.get(k);
                if (st.fixed) continue;
                st.minutes += each;
                if (rest > 0) { st.minutes += 1; rest--; }
            }
            return;
        }
        while (diff < 0) {
            int best = -1;
            // 先只从「程序算出来的」里挑最长的往下扣
            for (int i = 0; i < idx.size(); i++) {
                int k = idx.get(i).intValue();
                Step st = out.get(k);
                if (st.minutes <= MIN_TAIL || st.fixed) continue;
                if (best < 0 || st.minutes > out.get(best).minutes) best = k;
            }
            // 实在没有，才退而求其次去动课表原文里写死的
            if (best < 0) {
                for (int i = 0; i < idx.size(); i++) {
                    int k = idx.get(i).intValue();
                    if (out.get(k).minutes <= MIN_TAIL) continue;
                    if (best < 0 || out.get(k).minutes > out.get(best).minutes) best = k;
                }
            }
            if (best < 0) break;      // 已经全部到底，只能认了
            out.get(best).minutes--;
            diff++;
        }
    }

    static List<Step> stepsRaw(Context c, com.studycompanion.ScheduleData.Slot s) {
        load(c);
        List<Step> out = new ArrayList<Step>();
        if (s == null || s.body.isEmpty()) return out;

        String title = s.body.get(0).trim();
        int total = Math.max(1, s.duration());
        int t = s.startMin();
        int end = s.endMin();

        Matcher mn = NEW_RE.matcher(title);
        Matcher mp = PRAC_RE.matcher(title);

        // ---------------------------------------------- 新课
        if (mn.matches()) {
            String book = ALIAS.containsKey(mn.group(1)) ? ALIAS.get(mn.group(1)) : mn.group(1);
            String ch = mn.group(2), secs = mn.group(4) == null ? "" : mn.group(4);
            List<String> secList = secListOf(secs);

            out.add(info(books(book) + " " + ch + " " + mn.group(3) + "　" + secs));

            int openMin = 3, noteMin = 8, ankiMin = 5;
            int readTotal = Math.max(15, total - openMin - noteMin - ankiMin - 4);
            int per = Math.max(MIN_MIN, readTotal / Math.max(1, secList.size()));

            t += 0;
            out.add(step(t, openMin, "翻到课本先看本节 Learning outcomes，在纸上写下「这节我要学会哪几件事」", ""));
            t += openMin;

            for (int i = 0; i < secList.size(); i++) {
                String sec = secList.get(i);
                String nm = sectionName(s, sec);
                out.add(step(t, per, "读 §" + sec + (nm.length() > 0 ? " " + nm : "") + " —— 三遍法",
                        "① 通读一遍不求记住　② 遮住例题解答自己完整算一遍再对答案　③ 合上书默写本节公式/定义"));
                t += per;
            }
            out.add(step(t, noteMin, "把本节公式/定义手抄一页笔记", "抄的过程就是第一遍记忆"));
            t += noteMin;
            out.add(step(t, ankiMin, "今天新学的术语做成 Anki 卡", "正面术语，反面中英对照 + 一句例子"));
            t += ankiMin;

            int left = Math.max(3, end - t);
            String ck = CHECK.get(book + ":" + ch);
            out.add(step(t, left, "合上书自测",
                    ck != null ? ck : "能不能用自己的话把这两节讲一遍"));
            return out;
        }

        // ---------------------------------------------- 练习
        if (mp.matches()) {
            String book = ALIAS.containsKey(mp.group(1)) ? ALIAS.get(mp.group(1)) : mp.group(1);
            String ch = mp.group(2), secs = mp.group(4);
            List<String> secList = secListOf(secs);

            out.add(info(books(book) + " " + ch + " " + mp.group(3) + "　" + secs));

            int openMin = 3, fixMin = 15, ankiMin = 5;
            int budget = Math.max(20, total - openMin - fixMin - ankiMin);

            out.add(step(t, openMin, "先翻回课本把要考的公式/定义扫一眼，再开始做题", ""));
            t += openMin;

            List<Ex> exs = new ArrayList<Ex>();
            int est = 0;
            for (int i = 0; i < secList.size(); i++) {
                Ex e = exOf(book, secList.get(i));
                exs.add(e);
                if (e != null) est += e.minutes;
            }
            if (est <= 0) est = budget;
            double scale = Math.min(1.0, budget * 1.0 / est);

            boolean anyEx = false;
            for (int i = 0; i < secList.size(); i++) {
                Ex e = exs.get(i);
                if (e == null) continue;
                anyEx = true;
                String sec = secList.get(i);
                String nm = sectionName(s, sec);
                int mm = Math.max(8, (int) Math.round(e.minutes * scale));
                Matcher rm = RANGE_RE.matcher(e.q == null ? "" : e.q.trim());
                String qtxt = rm.matches()
                        ? ("题 " + rm.group(1) + "–" + rm.group(2))
                        : "全部题（做不完先做前一半，剩余顺延到下一个练习时段）";
                String label = e.ex;
                if (!rm.matches() && nm.length() > 0 && label.contains("节末"))
                    label = "§" + sec + " " + nm + " 的节末 Questions";
                out.add(step(t, mm, label + "　" + qtxt,
                        "位置：" + books(book) + " §" + sec + (nm.length() > 0 ? " " + nm : "")
                                + "，课本 p." + e.page + "\n卡住超过 3 分钟就跳过，做完统一看解析"));
                t += mm;
            }
            if (!anyEx) {
                List<String> ds = details(s);
                int step = Math.max(MIN_MIN, budget / Math.max(1, ds.size()));
                for (int i = 0; i < ds.size(); i++) {
                    out.add(step(t, step, ds.get(i), ""));
                    t += step;
                }
            }

            out.add(step(t, fixMin, "批改 + 错题",
                    "对答案册逐题打勾/画圈，做对的也看一眼解法是否更短\n"
                  + "每道错题写一行：题号 + 我为什么错 + 正确思路一句话"));
            t += fixMin;
            out.add(step(t, Math.max(3, end - t), "今天想不起来的术语/公式做成 Anki 卡", ""));
            return out;
        }

        // ---------------------------------------------- 其他（EAP / 复习 / 测试）
        List<String> ds = details(s);
        if (ds.isEmpty()) return out;
        out.add(info(s.subject));

        // 课表原文里自己写了「（25 分钟）」「20′」的，那个数字就是作者定的，
        // 必须原样照抄 —— 不能再平均分配、更不能被 fitTail 改掉，
        // 否则会出现「正文写着（30 分钟），步骤上却显示 25′」这种自相矛盾。
        int[] est = new int[ds.size()];
        boolean[] fromText = new boolean[ds.size()];
        int fixedSum = 0, blanks = 0;
        for (int i = 0; i < ds.size(); i++) {
            Matcher m = DUR1.matcher(ds.get(i));
            boolean hit = m.find();
            if (!hit) { m = DUR2.matcher(ds.get(i)); hit = m.find(); }
            est[i] = hit ? parseInt(m.group(1)) : 0;
            if (est[i] > 0) { fromText[i] = true; fixedSum += est[i]; }
            else blanks++;
        }
        int slack = total - fixedSum;
        // 原文写的时长加起来比时段本身还长（课表排得太满）：按比例缩小。
        // 用 floor 保证缩完不会又超出去；正文里的数字随后会跟着改成缩完的值。
        if (fixedSum > total) {
            double sc = total * 1.0 / fixedSum;
            for (int i = 0; i < est.length; i++)
                if (fromText[i]) est[i] = Math.max(1, (int) Math.floor(est[i] * sc));
            fixedSum = 0; blanks = 0;
            for (int i = 0; i < est.length; i++)
                if (est[i] > 0) fixedSum += est[i]; else blanks++;
            slack = total - fixedSum;
        }
        if (blanks > 0 && slack > 0) {
            // 剩下的时间平摊给没写时长的行。这些行的正文里本来就没有数字，
            // 给多少都不会自相矛盾；但富余如果连 1 分钟都不够分，
            // 就只能让排在后面的那几行改成不定时，不能超支。
            int per = Math.max(1, slack / blanks);
            int given = 0;
            for (int i = 0; i < est.length; i++) {
                if (est[i] != 0) continue;
                if (given + per > slack) continue;   // 余量不够了，这行改用不定时
                est[i] = per;
                given += per;
            }
        } else if (fixedSum == 0) {
            // 一行都没写时长：按行数平分
            int even = Math.max(MIN_MIN, total / Math.max(1, ds.size()));
            for (int i = 0; i < est.length; i++) est[i] = even;
        }
        // 剩下的情况：作者写的时长已经把这个时段占满了。
        // 那些没写时长的行是「合格线 / 本类口径」这类说明，本来就不占时间 ——
        // 不给分钟数、时刻也不显示，时钟停在最后一个定时步骤上。

        int cursor = t;
        for (int i = 0; i < ds.size(); i++) {
            if (est[i] > 0) {
                Step st = step(cursor, est[i], ds.get(i), "");
                if (fromText[i]) st.raw = ds.get(i);   // 正文里写了数字，收尾后要同步
                st.fixed = fromText[i];                // 只有课表原文写了数字的才锁死
                out.add(st);
                cursor += est[i];
            } else {
                Step st = step(cursor, 0, ds.get(i), "");
                st.time = "";
                st.untimed = true;
                out.add(st);
            }
        }
        return out;
    }

    static List<String> secListOf(String secs) {
        List<String> out = new ArrayList<String>();
        if (secs == null) return out;
        Matcher m = SEC_RE.matcher(secs);
        while (m.find()) if (!out.contains(m.group(1))) out.add(m.group(1));
        return out;
    }

    static List<String> details(com.studycompanion.ScheduleData.Slot s) {
        List<String> out = new ArrayList<String>();
        for (int i = 1; i < s.body.size(); i++) {
            Matcher m = DASH_RE.matcher(s.body.get(i).trim());
            if (m.matches() && m.group(1).trim().length() > 0) out.add(m.group(1).trim());
        }
        return out;
    }

    static Step step(int t, int mm, String text, String note) {
        Step s = new Step();
        s.time = hhmm(t);
        s.minutes = mm;
        s.text = text;
        s.note = note == null ? "" : note;
        return s;
    }

    static Step info(String text) {
        Step s = new Step();
        s.head = true;
        s.text = text;
        return s;
    }

    // ------------------------------------------------------------------ 打勾存档
    /** 某天某个时段的第 idx 步有没有做完 */
    public static boolean done(Context c, String iso, int slot, int idx) {
        return prefs(c).getBoolean(key(iso, slot) + "." + idx, false);
    }

    /** 这一步勾上/取消，返回勾完之后的进度 [已完成, 总数] */
    public static void setDone(Context c, String iso, int slot, int idx, boolean v) {
        prefs(c).edit().putBoolean(key(iso, slot) + "." + idx, v).apply();
    }

    /** 这个时段完成了多少步 */
    public static int doneCount(Context c, String iso, int slot, int total) {
        int n = 0;
        for (int i = 0; i < total; i++) if (done(c, iso, slot, i)) n++;
        return n;
    }

    public static void clearSlot(Context c, String iso, int slot, int total) {
        SharedPreferences.Editor e = prefs(c).edit();
        for (int i = 0; i < total; i++) e.remove(key(iso, slot) + "." + i);
        e.apply();
    }

    static String key(String iso, int slot) {
        return "plan." + iso + "." + slot;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("plan", Context.MODE_PRIVATE);
    }
}
