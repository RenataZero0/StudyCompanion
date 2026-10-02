package com.studycompanion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新日志阅读器（自绘 Markdown）。
 *
 * 打开时会自动从 GitHub 拉一次最新的 CHANGELOG.md：拉到了就用最新的（同时写缓存），
 * 拉不到就显示缓存 / APK 内置副本，并在标题下说明原因。右上角「刷新」可手动再拉。
 *
 * 支持：# / ## / ### 标题、- 项目符号、> 引用、**粗体**、`代码`、``` 代码块，以及表格。
 */
public class DocActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        DocView v = new DocView(this);
        setContentView(v);
        v.load();
    }

    // ==================================================================
    static class DocView extends View {

        // ---- 间距体系：标题靠「上方留白」拉开层次，正文之间只留小空 ----
        static final int GAP_H1_TOP = 8,  GAP_H1_BOT = 12;
        static final int GAP_H2_TOP = 30, GAP_H2_BOT = 10;
        static final int GAP_H3_TOP = 18, GAP_H3_BOT = 8;
        static final int GAP_PARA   = 9;
        static final int GAP_BLANK  = 9;
        static final int GAP_BULLET = 6;
        static final int GAP_DIV    = 14;

        static final int HEAD_BG = 0xFFF3F5F9;
        static final int CELL_SEP = 0xFFEEF1F6;

        static final int TONE_OK = 0, TONE_NEW = 1, TONE_LOCAL = 2, TONE_ERR = 3;

        /** 项目符号占的横向空间（画的时候也是这个值） */
        static final int BULLET_INDENT = 14;

        private final List<Blk> blocks = new ArrayList<Blk>();
        private float scroll = 0, contentH = 0;
        private final OverScroller scroller;
        private float downY, downScroll;
        private boolean dragging;
        private final int slop;

        private int cardX;      // 内容卡片的左右外边距
        private int textPad;    // 卡片内部文字的左右内边距
        private float headerH;

        String md = "";
        String status = "";
        int statusTone = TONE_OK;
        boolean busy;
        RectF closeRect = new RectF();
        RectF refreshRect = new RectF();

        static class Blk {
            List<String> lines = new ArrayList<String>();
            String text = "";
            Paint paint;
            int gapTop, gapBot, indent;
            boolean bullet, code, divider, spacer, ruleBar;

            float y, h;

            // ---- 表格 ----
            boolean table;
            String[][] rows;
            int[] colW;
            int[] align;
            int[] rowH;
            List<List<List<String>>> cellLines;
            Paint headPaint;
        }

        DocView(Context ctx) {
            super(ctx);
            scroller = new OverScroller(ctx);
            slop = (int) (8 * getResources().getDisplayMetrics().density);
            headerH = Ui.px(80);
            cardX = Ui.px(12);
            textPad = Ui.px(18);
        }

        // ---------------------------------------------------------- 拉取
        void load() {
            md = Changelog.local(getContext());
            parse(md);
            updateStatusText();
            if (getWidth() > 0) measure();
            fetchAsync();
        }

        void updateStatusText() {
            String latest = Changelog.latestVersion(md);
            String local = GitHub.VERSION_TAG.startsWith("v")
                    ? GitHub.VERSION_TAG.substring(1) : GitHub.VERSION_TAG;
            boolean cached = Changelog.hasCache(getContext());

            if (latest.length() == 0) {
                status = "内置副本";
                statusTone = TONE_LOCAL;
            } else if (latest.equalsIgnoreCase(local)) {
                status = cached ? ("已是最新 " + local) : "内置副本 · 未联网";
                statusTone = cached ? TONE_OK : TONE_LOCAL;
            } else {
                status = "日志已到 v" + latest + "（本机 v" + local + "）";
                statusTone = TONE_NEW;
            }
        }

        void fetchAsync() {
            if (busy) return;
            busy = true;
            status = "正在从 GitHub 拉取…";
            statusTone = TONE_LOCAL;
            invalidate();

            new Thread(new Runnable() {
                public void run() {
                    final String[] err = new String[1];
                    final boolean ok = Changelog.fetch(getContext(), err);
                    post(new Runnable() {
                        public void run() {
                            busy = false;
                            if (ok) {
                                md = Changelog.local(getContext());
                                parse(md);
                                if (getWidth() > 0) measure();
                                updateStatusText();
                                String t = Changelog.cacheTime(getContext());
                                if (t.length() > 0) status += " · " + t + " 更新";
                            } else {
                                updateStatusText();
                                status = "拉取失败，显示本地副本";
                                statusTone = TONE_ERR;
                            }
                            invalidate();
                        }
                    });
                }
            }).start();
        }

        // ---------------------------------------------------------- 解析
        static String plain(String s) { return s.replace("**", "").replace("`", ""); }

        Blk mk(Paint p, int gapTop, int gapBot, int indent) {
            Blk b = new Blk();
            b.paint = p;
            b.gapTop = gapTop;
            b.gapBot = gapBot;
            b.indent = indent;
            return b;
        }

        void add(String text, Paint p, int gapTop, int gapBot, int indent) {
            Blk b = mk(p, gapTop, gapBot, indent);
            b.text = text;
            blocks.add(b);
        }

        void parse(String src) {
            blocks.clear();
            if (src == null) return;

            String[] lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n");
            boolean inCode = false;
            boolean first = true;

            for (int li = 0; li < lines.length; li++) {
                String line = lines[li];
                String trimmed = line.trim();

                if (trimmed.startsWith("```")) { inCode = !inCode; continue; }
                if (inCode) {
                    Blk b = mk(Ui.font(11.5f, false, 0xFF3F6B52), 0, 2, Ui.px(10));
                    b.text = line; b.code = true;
                    blocks.add(b);
                    continue;
                }

                // ---------------- 表格 ----------------
                if (trimmed.startsWith("|")) {
                    List<String[]> rows = new ArrayList<String[]>();
                    List<Integer> aligns = new ArrayList<Integer>();
                    int lj = li;
                    boolean sepSeen = false;
                    while (lj < lines.length && lines[lj].trim().startsWith("|")) {
                        String tt = lines[lj].trim();
                        if (isTableSeparator(tt)) { sepSeen = true; aligns = parseAligns(tt); }
                        else if (tt.length() > 1) rows.add(splitRow(tt));
                        lj++;
                    }
                    if (rows.size() > 0 && sepSeen) {
                        int n = 0;
                        for (String[] r : rows) if (r.length > n) n = r.length;
                        for (int k = 0; k < rows.size(); k++) {
                            String[] r = rows.get(k);
                            if (r.length < n) {
                                String[] bigger = new String[n];
                                System.arraycopy(r, 0, bigger, 0, r.length);
                                for (int q = r.length; q < n; q++) bigger[q] = "";
                                rows.set(k, bigger);
                            }
                        }
                        while (aligns.size() < n) aligns.add(0);
                        Blk b = new Blk();
                        b.table = true;
                        b.rows = rows.toArray(new String[0][]);
                        b.align = new int[n];
                        for (int q = 0; q < n; q++) b.align[q] = aligns.get(q);
                        b.paint = Ui.font(11.5f, false, Ui.TEXT_BODY);
                        b.headPaint = Ui.font(11.5f, true, Ui.INK);
                        b.gapTop = Ui.px(8);
                        b.gapBot = Ui.px(14);
                        blocks.add(b);
                        li = lj - 1;
                        first = false;
                        continue;
                    }
                }

                // ---------------- 标题 ----------------
                int level = 0;
                String t = line;
                while (t.startsWith("#")) { level++; t = t.substring(1); }
                t = t.trim();

                if (level == 1) {
                    add(plain(t), Ui.font(20, true, Ui.INK), first ? 0 : Ui.px(GAP_H1_TOP), Ui.px(GAP_H1_BOT), 0);
                    first = false; continue;
                }
                if (level == 2) {
                    add(plain(t), Ui.font(15.5f, true, Ui.ACCENT), Ui.px(GAP_H2_TOP), Ui.px(GAP_H2_BOT), 0);
                    first = false; continue;
                }
                if (level >= 3) {
                    add(plain(t), Ui.font(13, true, Ui.INK), Ui.px(GAP_H3_TOP), Ui.px(GAP_H3_BOT), 0);
                    first = false; continue;
                }

                String l = trimmed;
                if (isRule(l)) {
                    Blk b = new Blk();
                    b.divider = true;
                    b.h = 1;
                    b.gapTop = Ui.px(GAP_DIV);
                    b.gapBot = Ui.px(GAP_DIV);
                    blocks.add(b);
                    continue;
                }
                if (l.startsWith("- ") || l.startsWith("* ")) {
                    Blk b = mk(Ui.font(12.5f, false, Ui.TEXT_BODY), 0, Ui.px(GAP_BULLET), Ui.px(6));
                    b.text = plain(l.substring(2)); b.bullet = true;
                    blocks.add(b);
                    continue;
                }
                if (l.startsWith("> ")) {
                    Blk b = mk(Ui.font(12, false, Ui.SUB), 2, Ui.px(GAP_PARA), Ui.px(10));
                    b.text = plain(l.substring(2)); b.ruleBar = true;
                    blocks.add(b);
                    continue;
                }
                if (l.length() == 0) {
                    // 连续空行合并成一个，别把间距叠起来
                    if (!blocks.isEmpty() && blocks.get(blocks.size() - 1).spacer) continue;
                    Blk b = new Blk();
                    b.spacer = true;
                    b.h = 0;
                    b.gapTop = 0;
                    b.gapBot = Ui.px(GAP_BLANK);
                    blocks.add(b);
                    continue;
                }
                add(plain(l), Ui.font(12.5f, false, Ui.TEXT_BODY), 0, Ui.px(GAP_PARA), 0);
                first = false;
            }
        }

        static boolean isTableSeparator(String t) {
            if (!t.startsWith("|")) return false;
            boolean hasDash = false;
            for (int i = 0; i < t.length(); i++) {
                char ch = t.charAt(i);
                if (ch == '-') { hasDash = true; continue; }
                if (ch == '|' || ch == ':' || ch == ' ') continue;
                return false;
            }
            return hasDash;
        }

        static List<Integer> parseAligns(String sep) {
            List<Integer> list = new ArrayList<Integer>();
            for (String c : splitRow(sep)) {
                String t = c.trim();
                boolean l = t.startsWith(":");
                boolean r = t.endsWith(":");
                list.add((l && r) ? 1 : (r ? 2 : 0));
            }
            return list;
        }

        static String[] splitRow(String t) {
            String s = t.trim();
            if (s.startsWith("|")) s = s.substring(1);
            if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
            String[] parts = s.split("\\|");
            for (int i = 0; i < parts.length; i++) parts[i] = plain(parts[i].trim());
            return parts;
        }

        static boolean isRule(String l) {
            String t = l.trim();
            if (t.length() < 3) return false;
            char c = t.charAt(0);
            if (c != '-' && c != '*' && c != '_') return false;
            for (int i = 0; i < t.length(); i++) if (t.charAt(i) != c && t.charAt(i) != ' ') return false;
            return true;
        }

        // ---------------------------------------------------------- 测量
        static int cellPadX() { return Ui.px(10); }
        static int cellPadY() { return Ui.px(8); }

        float textLeft() { return cardX + textPad; }
        float textWidth() { return getWidth() - (cardX + textPad) * 2; }

        void measure() {
            float w = textWidth();
            if (w < Ui.px(80)) w = Ui.px(80);
            float y = headerH + Ui.px(18);

            for (Blk b : blocks) {
                if (b.table) {
                    measureTable(b, w);
                    b.y = y + b.gapTop;
                    y = b.y + (b.h - b.gapTop - b.gapBot) + b.gapBot;
                    continue;
                }
                if (b.spacer) {
                    y += b.gapBot;
                    b.y = y;
                    continue;
                }
                if (b.divider) {
                    b.y = y + b.gapTop;
                    y = b.y + b.h + b.gapBot;
                    continue;
                }
                // 列表项的文字是从项目符号右边开始的，换行宽度必须把那段缩进也扣掉，
                // 否则续行会比可用宽度还长，看起来像是另一段
                float extra = b.bullet ? BULLET_INDENT : 0;
                b.lines = Ui.wrap(b.paint, b.text, w - b.indent - extra);
                Paint.FontMetrics fm = b.paint.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * (b.code ? 1.2f : 1.52f);
                b.h = b.lines.size() * lh;
                b.y = y + b.gapTop;
                y = b.y + b.h + b.gapBot;
            }
            contentH = y + Ui.px(28);
        }

        /** 列宽先按内容自然宽度；装不下就按比例压，再装不下就削最宽的列。 */
        void measureTable(Blk b, float avail) {
            int n = b.rows[0].length;
            int[] natural = new int[n];
            for (int c = 0; c < n; c++) natural[c] = Ui.px(46);

            for (int r = 0; r < b.rows.length; r++) {
                Paint f = (r == 0) ? b.headPaint : b.paint;
                for (int c = 0; c < n && c < b.rows[r].length; c++) {
                    int need = (int) Math.ceil(f.measureText(b.rows[r][c])) + cellPadX() * 2;
                    if (need > natural[c]) natural[c] = need;
                }
            }

            int sep = 1, total = 0;
            for (int v : natural) total += v;
            total += sep * (n - 1);

            b.colW = new int[n];
            if (total <= avail) {
                int extra = (int) (avail - total);
                for (int c = 0; c < n; c++) b.colW[c] = natural[c] + extra * natural[c] / Math.max(1, total);
                int used = 0;
                for (int v : b.colW) used += v;
                b.colW[n - 1] += (int) (avail - used) - sep * (n - 1);
            } else {
                int budget = (int) (avail - sep * (n - 1));
                int minW = Ui.px(56), sum = 0;
                for (int c = 0; c < n; c++) {
                    b.colW[c] = Math.max(minW, natural[c] * budget / Math.max(1, total));
                    sum += b.colW[c];
                }
                int guard = 0;
                while (sum > budget && guard++ < 200) {
                    int widest = 0;
                    for (int c = 1; c < n; c++) if (b.colW[c] > b.colW[widest]) widest = c;
                    if (b.colW[widest] <= minW) break;
                    b.colW[widest] -= Ui.px(4);
                    sum -= Ui.px(4);
                }
            }

            b.cellLines = new ArrayList<List<List<String>>>();
            b.rowH = new int[b.rows.length];
            for (int r = 0; r < b.rows.length; r++) {
                Paint f = (r == 0) ? b.headPaint : b.paint;
                Paint.FontMetrics fm = f.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * 1.4f;
                List<List<String>> rowCells = new ArrayList<List<String>>();
                int maxLines = 1;
                for (int c = 0; c < n; c++) {
                    String cell = (c < b.rows[r].length) ? b.rows[r][c] : "";
                    List<String> ls = Ui.wrap(f, cell, b.colW[c] - cellPadX() * 2);
                    rowCells.add(ls);
                    if (ls.size() > maxLines) maxLines = ls.size();
                }
                b.cellLines.add(rowCells);
                b.rowH[r] = (int) Math.ceil(maxLines * lh) + cellPadY() * 2;
            }

            int hh = 0;
            for (int v : b.rowH) hh += v;
            b.h = hh + b.gapTop + b.gapBot;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            measure();
        }

        float maxScroll() { return Math.max(0, contentH - getHeight()); }

        // ---------------------------------------------------------- 绘制
        @Override
        protected void onDraw(Canvas c) {
            c.drawColor(Ui.BG);
            float w = getWidth();

            float cardTop = headerH + Ui.px(10);
            float cardBot = getHeight() - Ui.px(8);
            Ui.roundRect(c, cardX, cardTop, w - cardX, cardBot, Ui.px(16), Ui.CARD);

            drawHeader(c, w);

            c.save();
            c.clipRect(cardX, cardTop, w - cardX, cardBot);
            c.translate(0, -scroll);
            for (Blk b : blocks) {
                float top = b.y;
                if (top + b.h + b.gapBot < scroll - Ui.px(80)
                        || top > scroll + getHeight() + Ui.px(80)) continue;

                if (b.table) { drawTable(c, b, top, textWidth()); continue; }
                if (b.divider) {
                    Ui.roundRect(c, textLeft(), top, textLeft() + textWidth(), top + 1, 0, Ui.LINE);
                    continue;
                }
                if (b.spacer) continue;

                float x = textLeft() + b.indent;
                Paint.FontMetrics fm = b.paint.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * (b.code ? 1.2f : 1.52f);

                if (b.ruleBar) {
                    Ui.roundRect(c, x - Ui.px(10), top, x - Ui.px(6), top + b.h, Ui.px(2), Ui.LINE);
                }
                if (b.bullet) {
                    Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
                    dot.setColor(Ui.TEXT_DIM);
                    c.drawCircle(x + Ui.px(3), top + lh * 0.55f, Ui.px(2.6f), dot);
                    x += Ui.px(BULLET_INDENT);
                }
                if (b.code) {
                    Ui.roundRect(c, textLeft(), top - Ui.px(3), textLeft() + textWidth(),
                            top + b.h + Ui.px(3), Ui.px(8), 0xFFF3F7F4);
                }
                for (int i = 0; i < b.lines.size(); i++) {
                    Ui.text(c, b.lines.get(i), x, top + i * lh - fm.ascent, b.paint);
                }
            }
            c.restore();
        }

        void drawHeader(Canvas c, float w) {
            Ui.roundRect(c, 0, 0, w, headerH, 0, 0xFFFFFFFF);
            Ui.roundRect(c, 0, headerH - 1, w, headerH, 0, Ui.LINE);

            // 第一行：标题 + 按钮
            Ui.text(c, "更新日志", Ui.px(20), Ui.px(37), Ui.font(17, true, Ui.INK));

            float bh = Ui.px(34);
            float by = Ui.px(20);
            float bw = Ui.px(70);
            closeRect = new RectF(w - Ui.px(16) - bw, by, w - Ui.px(16), by + bh);
            Ui.roundRect(c, closeRect, bh / 2, Ui.ACCENT);
            Ui.textC(c, "关闭", closeRect, Ui.font(13, true, 0xFFFFFFFF));

            float rw = Ui.px(66);
            refreshRect = new RectF(closeRect.left - Ui.px(8) - rw, by, closeRect.left - Ui.px(8), by + bh);
            Ui.roundRect(c, refreshRect, bh / 2, busy ? 0x14000000 : Ui.ACCENT_SOFT);
            Ui.textC(c, busy ? "拉取中" : "刷新", refreshRect, Ui.font(13, true, Ui.ACCENT));

            // 第二行：状态（小圆点 + 文字，按可用宽度截断，不会压到按钮）
            int tone = busy ? Ui.ACCENT
                    : statusTone == TONE_OK ? Ui.GREEN
                    : statusTone == TONE_NEW ? Ui.AMBER
                    : statusTone == TONE_ERR ? Ui.RED : Ui.TEXT_DIM;
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(tone);
            c.drawCircle(Ui.px(25), Ui.px(62), Ui.px(3.4f), dot);

            Paint sp = Ui.font(11.5f, false, Ui.SUB);
            String shown = Ui.ellipsize(sp, status, w - Ui.px(40) - Ui.px(20));
            Ui.text(c, shown, Ui.px(35), Ui.px(66), sp);
        }

        void drawTable(Canvas c, Blk b, float top, float w) {
            int n = b.colW.length;
            float height = b.h - b.gapTop - b.gapBot;
            float rad = Ui.px(9);
            float x0 = textLeft();
            RectF outer = new RectF(x0, top, x0 + w, top + height);

            Ui.roundRect(c, outer, rad, Ui.CARD);
            Ui.roundStroke(c, outer, rad, Ui.LINE, 1f);

            Path clip = new Path();
            clip.addRoundRect(outer, rad, rad, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            Ui.roundRect(c, x0, top, x0 + w, top + b.rowH[0], 0, HEAD_BG);
            c.restore();

            Paint hair = new Paint();
            hair.setStrokeWidth(1);

            float y = top;
            for (int r = 0; r < b.rows.length; r++) {
                boolean head = (r == 0);
                Paint f = head ? b.headPaint : b.paint;
                float rh = b.rowH[r];
                float x = x0;

                for (int ci = 0; ci < n; ci++) {
                    float cw = b.colW[ci];
                    if (ci > 0 && !head) {
                        hair.setColor(CELL_SEP);
                        c.drawLine(x, y + Ui.px(5), x, y + rh - Ui.px(5), hair);
                    }
                    List<String> ls = b.cellLines.get(r).get(ci);
                    Paint.FontMetrics fm = f.getFontMetrics();
                    float lh = (fm.descent - fm.ascent) * 1.4f;
                    float textH = ls.size() * lh;
                    float ty = y + (rh - textH) / 2;

                    for (int k = 0; k < ls.size(); k++) {
                        String line = ls.get(k);
                        float tw = f.measureText(line);
                        float tx = x + cellPadX();
                        if (b.align[ci] == 1) tx = x + (cw - tw) / 2f;
                        else if (b.align[ci] == 2) tx = x + cw - cellPadX() - tw;
                        Ui.text(c, line, tx, ty + k * lh - fm.ascent, f);
                    }
                    x += cw;
                }
                y += rh;
                if (r < b.rows.length - 1) {
                    hair.setColor(Ui.LINE);
                    c.drawLine(x0 + 1, y, x0 + w - 1, y, hair);
                }
            }
        }

        // ---------------------------------------------------------- 触摸
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = e.getY(); downScroll = scroll; dragging = false;
                    scroller.forceFinished(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getY() - downY) > slop) dragging = true;
                    if (dragging) {
                        scroll = downScroll - (e.getY() - downY);
                        clamp();
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging) {
                        float x = e.getX(), y = e.getY();
                        if (refreshRect.contains(x, y)) { fetchAsync(); return true; }
                        if (closeRect.contains(x, y) || y < headerH) {
                            ((Activity) getContext()).finish();
                            return true;
                        }
                    }
                    return true;
            }
            return super.onTouchEvent(e);
        }

        void clamp() {
            if (scroll < 0) scroll = 0;
            if (scroll > maxScroll()) scroll = maxScroll();
        }

        @Override
        public void computeScroll() {
            if (scroller.computeScrollOffset()) {
                scroll = scroller.getCurrY();
                clamp();
                invalidate();
            }
        }
    }
}
