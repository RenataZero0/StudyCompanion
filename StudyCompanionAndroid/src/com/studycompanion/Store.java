package com.studycompanion;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 完成记录与设置（SharedPreferences，等价于桌面版的 data\progress.tsv + settings.ini） */
public class Store {

    private static SharedPreferences sp;

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("study", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------ 打卡
    public static Set<String> done() {
        return new HashSet<String>(sp.getStringSet("done", new HashSet<String>()));
    }

    private static void save(Set<String> s) {
        sp.edit().putStringSet("done", new HashSet<String>(s)).apply();
    }

    public static String key(String iso, ScheduleData.Slot s) { return iso + "|" + s.key(); }

    public static boolean isDone(String iso, ScheduleData.Slot s) {
        return sp.getStringSet("done", new HashSet<String>()).contains(key(iso, s));
    }

    public static void toggle(String iso, ScheduleData.Slot s) {
        Set<String> s2 = done();
        String k = key(iso, s);
        if (!s2.remove(k)) s2.add(k);
        save(s2);
    }

    public static int doneCount(String iso) {
        ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
        if (dp == null) return 0;
        Set<String> s = sp.getStringSet("done", new HashSet<String>());
        int n = 0;
        for (ScheduleData.Slot sl : dp.slots) if (s.contains(key(iso, sl))) n++;
        return n;
    }

    public static boolean isPerfectDay(String iso) {
        ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
        if (dp == null || dp.slots.isEmpty()) return false;
        return doneCount(iso) >= dp.slots.size();
    }

    public static List<String> doneDates() {
        Set<String> set = new HashSet<String>();
        for (String k : sp.getStringSet("done", new HashSet<String>()))
            if (k.length() >= 10) set.add(k.substring(0, 10));
        List<String> out = new ArrayList<String>(set);
        Collections.sort(out);
        return out;
    }

    /** 连续打卡天数（当天有 ≥1 个时段完成即算打卡；今天还没开始则从昨天往前数） */
    public static int streak() {
        Set<String> d = new HashSet<String>(doneDates());
        String cur = ScheduleData.todayIso();
        if (!d.contains(cur)) cur = ScheduleData.shiftIso(cur, -1);
        int n = 0;
        while (d.contains(cur)) { n++; cur = ScheduleData.shiftIso(cur, -1); }
        return n;
    }

    public static int bestStreak() {
        List<String> ds = doneDates();
        if (ds.isEmpty()) return 0;
        Set<String> d = new HashSet<String>(ds);
        int best = 0, run = 0;
        String cur = ds.get(0), last = ds.get(ds.size() - 1);
        while (cur.compareTo(last) <= 0) {
            if (d.contains(cur)) { run++; if (run > best) best = run; }
            else run = 0;
            cur = ScheduleData.shiftIso(cur, 1);
        }
        return best;
    }

    public static double totalHours() {
        double min = 0;
        for (String k : sp.getStringSet("done", new HashSet<String>())) {
            int i = k.indexOf('|');
            if (i < 10) continue;
            String iso = k.substring(0, 10), slotKey = k.substring(i + 1);
            ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
            if (dp == null) continue;
            for (ScheduleData.Slot s : dp.slots)
                if (s.key().equals(slotKey)) { min += s.duration(); break; }
        }
        return min / 60.0;
    }

    public static int totalDone() { return sp.getStringSet("done", new HashSet<String>()).size(); }

    /** 本周（周一–周日）完成 / 总数 */
    public static int[] weekProgress() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int dow = c.get(java.util.Calendar.DAY_OF_WEEK);         // 周日=1
        int back = (dow + 5) % 7;                                 // 到周一的偏移
        String monday = ScheduleData.shiftIso(ScheduleData.todayIso(), -back);
        int done = 0, total = 0;
        for (int i = 0; i < 7; i++) {
            String iso = ScheduleData.shiftIso(monday, i);
            ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
            if (dp == null) continue;
            total += dp.slots.size();
            done += doneCount(iso);
        }
        return new int[]{done, total};
    }

    public static int perfectDaysThisMonth() {
        String today = ScheduleData.todayIso();
        int y = Integer.parseInt(today.substring(0, 4));
        int m = Integer.parseInt(today.substring(5, 7));
        int day = Integer.parseInt(today.substring(8, 10));
        int n = 0;
        for (int d = 1; d <= day; d++)
            if (isPerfectDay(String.format("%04d-%02d-%02d", y, m, d))) n++;
        return n;
    }

    // ------------------------------------------------------------------ 设置
    public static boolean autoRemind() { return sp.getBoolean("autoRemind", true); }
    public static void setAutoRemind(boolean v) { sp.edit().putBoolean("autoRemind", v).apply(); }

    /** CSV 导入：只导入「已完成」的行，按 日期+时段 匹配 */
    public static int[] importCsv(String content) {
        int added = 0, existed = 0, bad = 0;
        Set<String> s = done();
        for (String raw : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            if (raw.trim().length() == 0) continue;
            List<String> f = splitCsv(raw);
            if (f.size() < 3) { bad++; continue; }
            String date = f.get(0).trim();
            if ("日期".equals(date)) continue;
            String slot = f.get(2).trim();
            String state = f.size() > 5 ? f.get(5).trim() : "已完成";
            if (date.length() < 10 || slot.indexOf(':') < 0) { bad++; continue; }
            if (state.contains("未完成")) continue;
            String k = date + "|" + slot;
            if (s.contains(k)) existed++;
            else { s.add(k); added++; }
        }
        save(s);
        return new int[]{added, existed, bad};
    }

    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else q = false;
                } else sb.append(c);
            } else {
                if (c == '"') q = true;
                else if (c == ',') { out.add(sb.toString()); sb.setLength(0); }
                else sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    public static String exportCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("日期,星期,时段,科目,内容,状态\n");
        String[] wd = {"日", "一", "二", "三", "四", "五", "六"};
        for (String iso : ScheduleData.allDates()) {
            ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
            if (dp == null) continue;
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                    Integer.parseInt(iso.substring(8, 10)));
            String w = "周" + wd[c.get(java.util.Calendar.DAY_OF_WEEK) - 1];
            for (ScheduleData.Slot s : dp.slots) {
                sb.append(iso).append(',').append(w).append(',').append(s.key()).append(',')
                        .append(q(s.subject)).append(',').append(q(s.title())).append(',')
                        .append(isDone(iso, s) ? "已完成" : "未完成").append('\n');
            }
        }
        return sb.toString();
    }

    static String q(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
