package com.studycompanion;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** 更新日志阅读器（自绘 Markdown，与桌面版 DocViewer 一致） */
public class DocActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String md = "";
        try {
            InputStream in = getAssets().open("CHANGELOG.md");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            md = new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            md = "没有找到 CHANGELOG.md：" + e;
        }
        DocView v = new DocView(this, md);
        setContentView(v);
    }

    // ==================================================================
    static class DocView extends android.view.View {
        private final java.util.List<Blk> blocks = new java.util.ArrayList<Blk>();
        private float scroll = 0, contentH = 0;
        private final android.widget.OverScroller scroller;
        private float downY, downScroll;
        private boolean dragging;
        private final int slop;
        private final int pad;
        private float headerH;
        android.graphics.RectF closeRect = new android.graphics.RectF();

        static class Blk {
            String text = "";
            android.graphics.Paint paint;
            int gap, indent;
            boolean bullet, code, divider, h1;
            java.util.List<String> lines = new java.util.ArrayList<String>();
            float y, h;
        }

        DocView(android.content.Context ctx, String md) {
            super(ctx);
            scroller = new android.widget.OverScroller(ctx);
            slop = (int) (8 * getResources().getDisplayMetrics().density);
            pad = Ui.px(20);
            headerH = Ui.px(56);
            parse(md);
        }

        static String plain(String s) { return s.replace("**", "").replace("`", ""); }

        void add(String text, android.graphics.Paint p, int gap, int indent, boolean bullet, boolean code, boolean div, boolean h1) {
            Blk b = new Blk();
            b.text = text; b.paint = p; b.gap = gap; b.indent = indent;
            b.bullet = bullet; b.code = code; b.divider = div; b.h1 = h1;
            blocks.add(b);
        }

        void parse(String md) {
            if (md == null) return;
            String[] lines = md.replace("\r\n", "\n").replace('\r', '\n').split("\n");
            boolean inCode = false;
            boolean first = true;
            for (String raw : lines) {
                String line = raw;
                String trimmed = line.trim();
                if (trimmed.startsWith("```")) { inCode = !inCode; continue; }
                if (inCode) { add(line, Ui.font(11, false, 0xFF3F6B52), 0, Ui.px(10), false, true, false, false); continue; }

                int level = 0;
                String t = line;
                while (t.startsWith("#")) { level++; t = t.substring(1); }
                t = t.trim();
                if (level == 1) { add(plain(t), Ui.font(21, true, Ui.INK), first ? 0 : Ui.px(10), 0, false, false, false, true); first = false; continue; }
                if (level == 2) { add(plain(t), Ui.font(17, true, Ui.ACCENT), Ui.px(26), 0, false, false, false, false); first = false; continue; }
                if (level >= 3) { add(plain(t), Ui.font(14, true, Ui.INK), Ui.px(16), 0, false, false, false, false); first = false; continue; }

                String l = line.trim();
                if (isRule(l)) {
                    Blk b = new Blk();
                    b.divider = true; b.h = Ui.px(24); b.gap = Ui.px(18);
                    blocks.add(b);
                    continue;
                }
                if (l.startsWith("- ") || l.startsWith("* ")) {
                    add(plain(l.substring(2)), Ui.font(12, false, Ui.TEXT_BODY), Ui.px(3), Ui.px(12), true, false, false, false);
                    continue;
                }
                if (l.startsWith("> ")) {
                    add(plain(l.substring(2)), Ui.font(12, false, Ui.SUB), Ui.px(4), Ui.px(12), false, false, false, false);
                    continue;
                }
                if (l.length() == 0) { add("", Ui.font(7, false, Ui.SUB), Ui.px(4), 0, false, false, false, false); continue; }
                add(plain(l), Ui.font(12, false, Ui.TEXT_BODY), Ui.px(2), 0, false, false, false, false);
                first = false;
            }
        }

        static boolean isRule(String l) {
            String t = l.trim();
            if (t.length() < 3) return false;
            char c = t.charAt(0);
            if (c != '-' && c != '*' && c != '_') return false;
            for (int i = 0; i < t.length(); i++) if (t.charAt(i) != c && t.charAt(i) != ' ') return false;
            return true;
        }

        void measure() {
            float w = getWidth() - pad * 2;
            if (w < Ui.px(80)) w = Ui.px(80);
            float y = headerH + Ui.px(16);
            for (Blk b : blocks) {
                if (b.divider) { b.y = y; y += b.h + b.gap; continue; }
                float ind = b.indent + (b.bullet ? Ui.px(16) : 0);
                b.lines = Ui.wrap(b.paint, b.text, w - ind);
                android.graphics.Paint.FontMetrics fm = b.paint.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * (b.code ? 1.15f : 1.42f);
                b.h = b.lines.size() * lh + b.gap;
                b.y = y;
                y += b.h;
            }
            contentH = y + Ui.px(24);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            measure();
        }

        float maxScroll() { return Math.max(0, contentH - getHeight()); }

        @Override
        protected void onDraw(android.graphics.Canvas c) {
            c.drawColor(Ui.BG);
            float w = getWidth();
            // 顶栏
            Ui.roundRect(c, 0, 0, w, headerH, 0, 0xFFFFFFFF);
            Ui.text(c, "更新日志 · StudyCompanion", Ui.px(16), Ui.px(36), Ui.font(16, true, Ui.INK));
            closeRect = new android.graphics.RectF(w - Ui.px(16) - Ui.px(76), Ui.px(14), w - Ui.px(16), Ui.px(42));
            Ui.roundRect(c, closeRect, Ui.px(14), Ui.ACCENT);
            Ui.textC(c, "关闭", closeRect, Ui.font(13, true, 0xFFFFFFFF));
            Ui.roundRect(c, 0, headerH - Ui.px(1), w, headerH, 0, Ui.LINE);

            Ui.roundRect(c, Ui.px(12), headerH + Ui.px(10), w - Ui.px(12), getHeight() - Ui.px(6), Ui.px(14), Ui.CARD);

            c.save();
            c.clipRect(Ui.px(12), headerH + Ui.px(10), w - Ui.px(12), getHeight() - Ui.px(6));
            c.translate(0, -scroll);
            for (Blk b : blocks) {
                float top = b.y;
                if (top + b.h < scroll - Ui.px(50) || top > scroll + getHeight() + Ui.px(50)) continue;
                if (b.divider) {
                    Ui.roundRect(c, pad, top + b.h / 2, w - pad, top + b.h / 2 + 1, 0, Ui.LINE);
                    continue;
                }
                float x = pad + b.indent;
                if (b.bullet) {
                    android.graphics.Paint dot = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    dot.setColor(Ui.TEXT_DIM);
                    c.drawCircle(x + Ui.px(4), top + b.h / 2, Ui.px(2.5f), dot);
                    x += Ui.px(16);
                }
                android.graphics.Paint.FontMetrics fm = b.paint.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * (b.code ? 1.15f : 1.42f);
                for (int i = 0; i < b.lines.size(); i++) {
                    Ui.text(c, b.lines.get(i), x, top + i * lh - fm.ascent, b.paint);
                }
            }
            c.restore();
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downY = e.getY(); downScroll = scroll; dragging = false;
                    scroller.forceFinished(true);
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getY() - downY) > slop) dragging = true;
                    if (dragging) {
                        scroll = downScroll - (e.getY() - downY);
                        clamp();
                        invalidate();
                    }
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                    if (!dragging && e.getY() < headerH) {
                        // 点顶栏（含「关闭」按钮）都退出
                        ((Activity) getContext()).finish();
                        return true;
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
