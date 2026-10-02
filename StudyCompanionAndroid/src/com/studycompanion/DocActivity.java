package com.studycompanion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
 * 打开时会自动从 GitHub 拉一次最新的 CHANGELOG.md：
 * 拉到了就用最新的（同时写缓存），拉不到就显示缓存/APK 内置副本，并说明原因。
 * 右上角有「刷新」按钮可以手动再拉。
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
        private final List<Blk> blocks = new ArrayList<Blk>();
        private float scroll = 0, contentH = 0;
        private final OverScroller scroller;
        private float downY, downScroll;
        private boolean dragging;
        private final int slop;
        private final int pad;
        private float headerH;

        String md = "";
        String status = "";
        boolean busy;
        RectF closeRect = new RectF();
        RectF refreshRect = new RectF();

        static class Blk {
            String text = "";
            Paint paint;
            int gap, indent;
            boolean bullet, code, divider, h1;
            List<String> lines = new ArrayList<String>();
            float y, h;
        }

        DocView(Context ctx) {
            super(ctx);
            scroller = new OverScroller(ctx);
            slop = (int) (8 * getResources().getDisplayMetrics().density);
            pad = Ui.px(20);
            headerH = Ui.px(72);
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
            String src = cached ? "GitHub" : "内置";

            if (latest.length() == 0) status = "来源：" + src;
            else if (latest.equalsIgnoreCase(local)) status = "当前 v" + local + "　已是最新　·　来源：" + src;
            else status = "当前 v" + local + "　日志已到 v" + latest + "　·　来源：" + src;
        }

        void fetchAsync() {
            if (busy) return;
            busy = true;
            status = "正在从 GitHub 拉取最新日志…";
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
                                if (t.length() > 0) status += "　·　" + t + " 更新";
                            } else {
                                updateStatusText();
                                status = "拉取失败，显示本地副本（" + brief(err[0]) + "）";
                            }
                            invalidate();
                        }
                    });
                }
            }).start();
        }

        static String brief(String s) {
            if (s == null) return "未知原因";
            return s.length() > 36 ? s.substring(0, 36) + "…" : s;
        }

        // ---------------------------------------------------------- 解析
        static String plain(String s) { return s.replace("**", "").replace("`", ""); }

        void add(String text, Paint p, int gap, int indent, boolean bullet, boolean code, boolean div, boolean h1) {
            Blk b = new Blk();
            b.text = text; b.paint = p; b.gap = gap; b.indent = indent;
            b.bullet = bullet; b.code = code; b.divider = div; b.h1 = h1;
            blocks.add(b);
        }

        void parse(String src) {
            blocks.clear();
            if (src == null) return;
            String[] lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n");
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
                Paint.FontMetrics fm = b.paint.getFontMetrics();
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
        protected void onDraw(Canvas c) {
            c.drawColor(Ui.BG);
            float w = getWidth();

            // 内容卡片
            Ui.roundRect(c, Ui.px(12), headerH + Ui.px(10), w - Ui.px(12),
                    getHeight() - Ui.px(6), Ui.px(14), Ui.CARD);

            // 顶栏
            Ui.roundRect(c, 0, 0, w, headerH, 0, 0xFFFFFFFF);
            Ui.text(c, "更新日志 · StudyCompanion", Ui.px(18), Ui.px(32), Ui.font(16, true, Ui.INK));
            Ui.text(c, status, Ui.px(18), Ui.px(56), Ui.font(11, false, busy ? Ui.ACCENT : Ui.SUB));

            closeRect = new RectF(w - Ui.px(16) - Ui.px(72), Ui.px(18), w - Ui.px(16), Ui.px(50));
            Ui.roundRect(c, closeRect, Ui.px(16), Ui.ACCENT);
            Ui.textC(c, "关闭", closeRect, Ui.font(13, true, 0xFFFFFFFF));

            float rw = Math.max(Ui.px(68), Ui.font(13, true, Ui.ACCENT).measureText("拉取中") + Ui.px(26));
            refreshRect = new RectF(closeRect.left - Ui.px(8) - rw, Ui.px(18), closeRect.left - Ui.px(8), Ui.px(50));
            Ui.roundRect(c, refreshRect, Ui.px(16), busy ? 0x14000000 : Ui.ACCENT_SOFT);
            Ui.textC(c, busy ? "拉取中" : "刷新", refreshRect, Ui.font(13, true, Ui.ACCENT));

            Ui.roundRect(c, 0, headerH - Ui.px(1), w, headerH, 0, Ui.LINE);

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
                    Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
                    dot.setColor(Ui.TEXT_DIM);
                    c.drawCircle(x + Ui.px(4), top + b.h / 2, Ui.px(2.5f), dot);
                    x += Ui.px(16);
                }
                Paint.FontMetrics fm = b.paint.getFontMetrics();
                float lh = (fm.descent - fm.ascent) * (b.code ? 1.15f : 1.42f);
                for (int i = 0; i < b.lines.size(); i++) {
                    Ui.text(c, b.lines.get(i), x, top + i * lh - fm.ascent, b.paint);
                }
            }
            c.restore();
        }

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
