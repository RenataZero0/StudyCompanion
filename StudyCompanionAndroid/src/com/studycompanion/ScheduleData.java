package com.studycompanion;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 直接解析 assets/Schedule.xlsx（OOXML = zip + xml），不需要任何第三方库。
 * 解析逻辑与桌面版 Schedule.cs 保持一致。
 *
 * 只用 java.* 与 javax.xml.*，因此同样的代码可以在电脑上用普通 JVM 跑测试
 * （见 tools/SelfTest.java）——发布前先验证解析结果，而不是等装到手机上才发现问题。
 */
public class ScheduleData {

    public static class Slot {
        public String start = "", end = "", subject = "";
        public final List<String> body = new ArrayList<String>();

        public String title() { return body.isEmpty() ? "" : body.get(0); }
        public String key() { return start + "-" + end; }
        public int startMin() { return toMin(start); }
        public int endMin() { return toMin(end); }
        public int duration() { return Math.max(0, endMin() - startMin()); }

        static int toMin(String hhmm) {
            if (hhmm == null) return 0;
            String[] p = hhmm.split(":");
            if (p.length != 2) return 0;
            try { return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }
            catch (Exception e) { return 0; }
        }
    }

    public static class DayPlan {
        public String iso = "";
        public String header = "";
        public String holiday = "";
        public final List<Slot> slots = new ArrayList<Slot>();

        public boolean isHoliday() { return holiday.length() > 0; }
        public int totalMinutes() {
            int t = 0;
            for (Slot s : slots) t += s.duration();
            return t;
        }
        public String dayKind() {
            if (isHoliday()) return holiday;
            java.util.Calendar c = cal(iso);
            int d = c.get(java.util.Calendar.DAY_OF_WEEK);
            if (d == java.util.Calendar.SATURDAY || d == java.util.Calendar.SUNDAY) return "周末";
            return "上学日";
        }
    }

    public static String lastError = "";
    public static String loadedFrom = "";
    private static final TreeMap<String, String> TEXTS = new TreeMap<String, String>();
    private static final Map<String, DayPlan> PLANS = new HashMap<String, DayPlan>();

    private static final Pattern TIME_RX = Pattern.compile(
            "^\\s*(\\d{1,2}:\\d{2})\\s*[-–—]\\s*(\\d{1,2}:\\d{2})\\s*(.+?)\\s*$");
    private static final Pattern DAY_RX = Pattern.compile("^(\\d{1,2})月(\\d{1,2})日");

    public static int dayCount() { return TEXTS.size(); }
    public static List<String> allDates() { return new ArrayList<String>(TEXTS.keySet()); }
    public static String firstDate() { return TEXTS.isEmpty() ? "" : TEXTS.firstKey(); }
    public static String lastDate() { return TEXTS.isEmpty() ? "" : TEXTS.lastKey(); }

    // ------------------------------------------------------------------ 读取
    public static void load(Context ctx) {
        try {
            loadFromBytes(readAll(ctx.getAssets().open("Schedule.xlsx")));
            loadedFrom = "assets/Schedule.xlsx · " + TEXTS.size() + " 天";
        } catch (Exception e) {
            lastError = "读取课表失败：" + e;
        }
    }

    /** 纯 Java 入口：给定 xlsx 的字节内容解析（便于在电脑上跑测试） */
    public static void loadFromBytes(byte[] workbook) throws Exception {
        lastError = "";
        TEXTS.clear();
        PLANS.clear();

        Map<String, byte[]> zip = unzip(workbook);
        List<String> shared = readSharedStrings(zip.get("xl/sharedStrings.xml"));
        Map<String, String> rels = readRels(zip.get("xl/_rels/workbook.xml.rels"));
        for (String[] sh : readSheets(zip.get("xl/workbook.xml"), rels)) {
            String name = sh[0], target = sh[1];
            if (!name.matches("\\d{6}")) continue;
            int yy = Integer.parseInt(name.substring(0, 4));
            int mm = Integer.parseInt(name.substring(4, 6));
            readSheet(zip.get(target), yy, mm, shared);
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        byte[] b = drain(in);
        in.close();
        return b;
    }

    /** 把流读完但不关闭（ZipInputStream 还要继续读下一个 entry） */
    static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    static Map<String, byte[]> unzip(byte[] data) throws IOException {
        Map<String, byte[]> out = new HashMap<String, byte[]>();
        ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry e;
        while ((e = z.getNextEntry()) != null) {
            out.put(e.getName(), drain(z));      // 注意：不能关闭 z
        }
        z.close();
        return out;
    }

    static Document dom(byte[] b) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        try { f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); }
        catch (Exception ignored) { }
        DocumentBuilder db = f.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(b));
    }

    static List<String> readSharedStrings(byte[] b) throws Exception {
        List<String> list = new ArrayList<String>();
        if (b == null) return list;
        NodeList sis = dom(b).getElementsByTagName("si");
        for (int i = 0; i < sis.getLength(); i++) {
            NodeList ts = ((Element) sis.item(i)).getElementsByTagName("t");
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < ts.getLength(); j++) sb.append(ts.item(j).getTextContent());
            list.add(sb.toString());
        }
        return list;
    }

    static Map<String, String> readRels(byte[] b) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        if (b == null) return map;
        NodeList rs = dom(b).getElementsByTagName("Relationship");
        for (int i = 0; i < rs.getLength(); i++) {
            Element e = (Element) rs.item(i);
            String id = e.getAttribute("Id"), tg = e.getAttribute("Target");
            if (id.length() == 0 || tg.length() == 0) continue;
            tg = tg.replace("\\", "/");
            if (tg.startsWith("/")) tg = tg.substring(1);
            else if (!tg.startsWith("xl/")) tg = "xl/" + tg;
            map.put(id, tg);
        }
        return map;
    }

    static List<String[]> readSheets(byte[] b, Map<String, String> rels) throws Exception {
        List<String[]> out = new ArrayList<String[]>();
        if (b == null) return out;
        NodeList ss = dom(b).getElementsByTagName("sheet");
        for (int i = 0; i < ss.getLength(); i++) {
            Element e = (Element) ss.item(i);
            String name = e.getAttribute("name");
            String rid = e.getAttribute("r:id");
            if (rid.length() == 0) rid = e.getAttribute("id");
            if (name.length() > 0 && rels.containsKey(rid)) out.add(new String[]{name, rels.get(rid)});
        }
        return out;
    }

    static void readSheet(byte[] b, int yy, int mm, List<String> shared) throws Exception {
        if (b == null) return;
        NodeList cs = dom(b).getElementsByTagName("c");
        for (int i = 0; i < cs.getLength(); i++) {
            Element c = (Element) cs.item(i);
            String type = c.getAttribute("t");
            String text = null;
            if ("s".equals(type)) {
                NodeList vs = c.getElementsByTagName("v");
                if (vs.getLength() > 0) {
                    try {
                        int idx = Integer.parseInt(vs.item(0).getTextContent().trim());
                        if (idx >= 0 && idx < shared.size()) text = shared.get(idx);
                    } catch (Exception ignored) { }
                }
            } else if ("inlineStr".equals(type)) {
                NodeList ts = c.getElementsByTagName("t");
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < ts.getLength(); j++) sb.append(ts.item(j).getTextContent());
                text = sb.toString();
            } else {
                NodeList vs = c.getElementsByTagName("v");
                if (vs.getLength() > 0) text = vs.item(0).getTextContent();
            }
            if (text == null || text.length() == 0) continue;
            String first = text.replace("\r", "").split("\n", -1)[0];
            Matcher m = DAY_RX.matcher(first.trim());
            if (m.find()) {
                int d = Integer.parseInt(m.group(2));
                if (d >= 1 && d <= 31) TEXTS.put(String.format("%04d-%02d-%02d", yy, mm, d), text);
            }
        }
    }

    // ------------------------------------------------------------------ 单元格 → DayPlan
    public static DayPlan getDay(String iso) {
        DayPlan d = PLANS.get(iso);
        if (d != null) return d;
        String txt = TEXTS.get(iso);
        if (txt == null) return null;
        d = parseDay(iso, txt);
        PLANS.put(iso, d);
        return d;
    }

    static DayPlan parseDay(String iso, String cellText) {
        DayPlan dp = new DayPlan();
        dp.iso = iso;
        String[] lines = cellText.replace("\r", "").split("\n", -1);
        if (lines.length > 0) {
            dp.header = lines[0].trim();
            int i = dp.header.indexOf('◆');
            if (i >= 0) dp.holiday = dp.header.substring(i + 1).trim();
        }
        Slot cur = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) continue;
            if (line.replaceAll("[─\\-=_—]", "").length() == 0) continue;
            Matcher m = TIME_RX.matcher(line);
            if (m.matches()) {
                cur = new Slot();
                cur.start = norm(m.group(1));
                cur.end = norm(m.group(2));
                cur.subject = m.group(3).trim();
                dp.slots.add(cur);
            } else if (cur != null) {
                cur.body.add(line);
            }
        }
        return dp;
    }

    static String norm(String hhmm) {
        String[] p = hhmm.split(":");
        if (p.length != 2) return hhmm;
        while (p[0].length() < 2) p[0] = "0" + p[0];
        return p[0] + ":" + p[1];
    }

    // ------------------------------------------------------------------ 日期工具
    public static java.util.Calendar cal(String iso) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                Integer.parseInt(iso.substring(8, 10)));
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c;
    }

    public static String todayIso() { return iso(java.util.Calendar.getInstance()); }

    public static String iso(java.util.Calendar c) {
        return String.format("%04d-%02d-%02d", c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH));
    }

    public static String shiftIso(String iso, int days) {
        java.util.Calendar c = cal(iso);
        c.add(java.util.Calendar.DAY_OF_MONTH, days);
        return iso(c);
    }
}
