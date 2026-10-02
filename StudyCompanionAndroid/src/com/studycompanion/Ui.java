package com.studycompanion;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

/** 配色与绘制工具，数值与桌面版一一对应 */
public class Ui {
    /** 屏幕密度（桌面版叫 Ui.S，1.0 = 96dpi）。这里用 dp 近似 */
    public static float S = 1f;

    public static int px(float v) { return Math.round(v * S); }
    public static float fpx(float v) { return v * S; }

    public static final int BG          = 0xFFF3F5F9;
    public static final int CARD        = 0xFFFFFFFF;
    public static final int INK         = 0xFF1B2432;
    public static final int SUB         = 0xFF71809A;
    public static final int LINE        = 0xFFE5E9F0;
    public static final int ACCENT      = 0xFF3568E8;
    public static final int ACCENT_SOFT = 0xFFE8EFFE;
    public static final int GREEN       = 0xFF21A366;
    public static final int GREEN_SOFT  = 0xFFE4F5EC;
    public static final int GREEN_LINE  = 0xFFB9E3CC;
    public static final int AMBER       = 0xFFDE9420;
    public static final int AMBER_SOFT  = 0xFFFDF2DF;
    public static final int RED         = 0xFFE0533F;
    public static final int TEXT_BODY   = 0xFF3C4A60;
    public static final int TEXT_DIM    = 0xFF9AA6B8;
    public static final int DONE_TEXT   = 0xFF4C7A63;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** 常用字号（对应桌面版的 pt → px 近似） */
    public static Paint font(float size, boolean bold, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(fpx(size));
        p.setColor(color);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        p.setSubpixelText(true);
        return p;
    }

    public static void roundRect(Canvas c, RectF r, float rad, int color) {
        P.reset();
        P.setAntiAlias(true);
        P.setColor(color);
        c.drawRoundRect(r, rad, rad, P);
    }

    public static void roundRect(Canvas c, float l, float t, float rr, float b, float rad, int color) {
        RectF r = new RectF(l, t, rr, b);
        roundRect(c, r, rad, color);
    }

    public static void roundStroke(Canvas c, RectF r, float rad, int color, float w) {
        P.reset();
        P.setAntiAlias(true);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(w);
        P.setColor(color);
        c.drawRoundRect(r, rad, rad, P);
    }

    public static void text(Canvas c, String s, float x, float y, Paint p) {
        c.drawText(s, x, y, p);
    }

    /** 垂直居中（y 用中心线） */
    public static void textV(Canvas c, String s, float x, float centerY, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2f;
        c.drawText(s, x, baseline, p);
    }

    /** 水平 + 垂直居中 */
    public static void textC(Canvas c, String s, RectF r, Paint p) {
        float w = p.measureText(s);
        float x = r.left + (r.width() - w) / 2f;
        float y = r.top + r.height() / 2f;
        textV(c, s, x, y, p);
    }

    /** 左对齐 + 垂直居中 + 超长省略号 */
    public static void textVC(Canvas c, String s, RectF r, Paint p) {
        String t = ellipsize(p, s, r.width());
        textV(c, t, r.left, r.top + r.height() / 2f, p);
    }

    public static String ellipsize(Paint p, String s, float maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && p.measureText(t + "…") > maxW) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    /** 按宽度换行（中英文混排，逐字符断行即可） */
    public static java.util.List<String> wrap(Paint p, String s, float maxW) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (s == null) { out.add(""); return out; }
        if (maxW < fpx(30)) maxW = fpx(30);
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n') { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(ch);
            if (p.measureText(cur.toString()) > maxW && cur.length() > 1) {
                cur.setLength(cur.length() - 1);
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
