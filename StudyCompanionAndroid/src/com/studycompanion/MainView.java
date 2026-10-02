package com.studycompanion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 整个界面都在这一个自定义 View 里绘制：与桌面版同一套配色、卡片、徽章、日历、下拉菜单。
 * 手机上把桌面版的左右两栏改成上下单栏排列，视觉元素保持一致。
 */
public class MainView extends View {

    public interface Listener {
        void openUrl(String url);
        void exportCsv();
        void importCsv();
        void showChangelog();
        void ghLogin();
        void ghLogout();
        void ghSync();
        void ghCheckUpdate();
    }

    static class Hit {
        final RectF r = new RectF();
        int action;
        String arg;
        int index;
        Hit(RectF src, int a, String s, int i) { r.set(src); action = a; arg = s; index = i; }
    }

    static final int A_URL = 1, A_TOGGLE = 2, A_DAY = 3, A_MENU = 4, A_EXPORT = 5,
            A_IMPORT = 6, A_LOG = 7, A_TODAY = 8, A_CLOSE_MENU = 9, A_REMIND = 10,
            A_GH_LOGIN = 11, A_GH_LOGOUT = 12, A_GH_SYNC = 13, A_GH_UPDATE = 14,
            A_OV_CAL = 21, A_OV_STATS = 22, A_OV_TOOLS = 23, A_OV_CLOSE = 24;

    private final Listener listener;
    private final List<Hit> hits = new ArrayList<Hit>();

    String viewIso = ScheduleData.todayIso();
    float scrollY = 0, contentH = 0, headerH;
    int menuOwner = -1;
    private List<String[]> menuItems = new ArrayList<String[]>();
    private float hitOffset = 0;

    private final OverScroller scroller;
    private VelocityTracker vt;
    private float downY, downScroll;
    private boolean dragging;
    private final int slop;

    // ---- 手机端：日历 / 统计 / 设置 放独立页面，不占主界面 ----
    int overlay = 0;                 // 0=主界面 1=日历 2=统计 3=设置
    float ovScroll = 0, ovContentH = 0;
    float bottomH = 0;               // 底部按钮栏高度（仅手机端）
    private float ovDownY, ovDownScroll;
    private boolean ovDragging;

    public MainView(Context ctx, Listener l) {
        super(ctx);
        listener = l;
        scroller = new OverScroller(ctx);
        slop = (int) (8 * getResources().getDisplayMetrics().density);
        setFocusable(true);
    }

    // ================================================================== 触摸
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (overlay != 0) return overlayTouch(e);
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = y; downScroll = scrollY; dragging = false;
                if (vt == null) vt = VelocityTracker.obtain(); else vt.clear();
                vt.addMovement(e);
                scroller.forceFinished(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (vt != null) vt.addMovement(e);
                if (Math.abs(y - downY) > slop) dragging = true;
                if (dragging) {
                    scrollY = downScroll - (y - downY);
                    clampScroll();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (vt != null) {
                    vt.addMovement(e);
                    vt.computeCurrentVelocity(1000);
                    int vy = (int) vt.getYVelocity();
                    if (dragging && Math.abs(vy) > 200) {
                        // 注意符号：getYVelocity() 是「手指」的速度，
                        // 往上滑手指 y 减小 -> vy 为负；而 fling 要的是「滚动坐标」的速度，
                        // 往上滑应该是 scrollY 增大，所以要取反。
                        // 之前没取反，每次滑完都往反方向弹回去 —— 就是那个「回滚」现象。
                        scroller.fling(0, (int) scrollY, 0, -vy, 0, 0, 0, (int) maxScroll());
                        invalidate();
                    }
                }
                if (!dragging) handleTap(x, y);
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (overlay != 0) { ovScroll = scroller.getCurrY(); clampOv(); }
            else { scrollY = scroller.getCurrY(); clampScroll(); }
            invalidate();
        }
    }

    private float maxScroll() {
        // 手机端要减去底部按钮栏的高度
        return Math.max(0, contentH - (getHeight() - headerH - bottomH));
    }

    private void clampScroll() {
        float m = maxScroll();
        if (scrollY < 0) scrollY = 0;
        if (scrollY > m) scrollY = m;
    }

    private float maxOv() {
        return Math.max(0, ovContentH - (getHeight() - Ui.px(56)));
    }

    private void clampOv() {
        float m = maxOv();
        if (ovScroll < 0) ovScroll = 0;
        if (ovScroll > m) ovScroll = m;
    }

    /** 供 Activity 的返回键调用：关掉独立页面 */
    public boolean closeOverlay() {
        if (overlay == 0) return false;
        overlay = 0; ovScroll = 0;
        invalidate();
        return true;
    }

    // ------------------------------------------------ 独立页面（日历 / 统计 / 设置）
    private boolean overlayTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                ovDownY = y; ovDownScroll = ovScroll; ovDragging = false;
                if (vt == null) vt = VelocityTracker.obtain(); else vt.clear();
                vt.addMovement(e);
                scroller.forceFinished(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (vt != null) vt.addMovement(e);
                if (Math.abs(y - ovDownY) > slop) ovDragging = true;
                if (ovDragging) {
                    ovScroll = ovDownScroll - (y - ovDownY);
                    clampOv();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (vt != null) {
                    vt.addMovement(e);
                    vt.computeCurrentVelocity(1000);
                    int vy = (int) vt.getYVelocity();
                    if (ovDragging && Math.abs(vy) > 200) {
                        scroller.fling(0, (int) ovScroll, 0, -vy, 0, 0, 0, (int) maxOv());
                        invalidate();
                    }
                }
                if (!ovDragging) {
                    for (int i = hits.size() - 1; i >= 0; i--) {
                        Hit h = hits.get(i);
                        if (h.r.contains(x, y)) { dispatch(h); return true; }
                    }
                }
                ovDragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                ovDragging = false;
                return true;
        }
        return true;
    }

    private void handleTap(float x, float y) {
        if (menuOwner >= 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.r.contains(x, y)) { dispatch(h); return; }
            }
            menuOwner = -1;
            invalidate();
            return;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.r.contains(x, y)) { dispatch(h); return; }
        }
    }

    private void dispatch(Hit h) {
        switch (h.action) {
            case A_URL:
                if (listener != null) listener.openUrl(h.arg);
                break;
            case A_TOGGLE: {
                ScheduleData.DayPlan dp = ScheduleData.getDay(viewIso);
                if (dp != null && h.index < dp.slots.size()) {
                    Store.toggle(viewIso, dp.slots.get(h.index));
                }
                break;
            }
            case A_DAY:
                // 可能是具体日期，也可能是 "2026-10#month"（日历左右翻月）
                setViewIso(h.arg);
                break;
            case A_MENU:
                menuOwner = h.index;
                menuItems = Links.forSlot(currentSlot(h.index));
                break;
            case A_CLOSE_MENU:
                menuOwner = -1;
                break;
            case A_EXPORT:
                if (listener != null) listener.exportCsv();
                break;
            case A_IMPORT:
                if (listener != null) listener.importCsv();
                break;
            case A_LOG:
                if (listener != null) listener.showChangelog();
                break;
            case A_TODAY:
                viewIso = ScheduleData.todayIso();
                scrollY = 0;
                break;
            case A_REMIND:
                Store.setAutoRemind(!Store.autoRemind());
                Reminder.scheduleAll(getContext());
                break;
            case A_GH_LOGIN:
                if (listener != null) listener.ghLogin();
                break;
            case A_GH_LOGOUT:
                if (listener != null) listener.ghLogout();
                break;
            case A_GH_SYNC:
                if (listener != null) listener.ghSync();
                break;
            case A_GH_UPDATE:
                if (listener != null) listener.ghCheckUpdate();
                break;
            case A_OV_CAL:
                overlay = 1; ovScroll = 0; break;
            case A_OV_STATS:
                overlay = 2; ovScroll = 0; break;
            case A_OV_TOOLS:
                overlay = 3; ovScroll = 0; break;
            case A_OV_CLOSE:
                overlay = 0; ovScroll = 0; break;
        }
        invalidate();
    }

    private ScheduleData.Slot currentSlot(int i) {
        ScheduleData.DayPlan dp = ScheduleData.getDay(viewIso);
        if (dp == null || i < 0 || i >= dp.slots.size()) return null;
        return dp.slots.get(i);
    }

    // ================================================================== 绘制
    private void hit(RectF r, int action, String arg, int index) {
        RectF t = new RectF(r.left, r.top + hitOffset, r.right, r.bottom + hitOffset);
        hits.add(new Hit(t, action, arg, index));
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(Ui.BG);
        hits.clear();

        float W = getWidth(), H = getHeight();
        headerH = Ui.px(108);
        float outer = Ui.px(14);
        float gap = Ui.px(14);

        // 内容整体居中并限制最大宽度：超宽屏 / 横屏时不会被拉成一条细长带
        float total = Math.min(W - outer * 2, Ui.px(1180));
        boxX0 = Math.round((W - total) / 2f);
        boxX1 = boxX0 + total;
        // 平板 / 横屏：左右两栏（与桌面版一致）；手机竖屏：单栏
        boolean twoCol = total >= Ui.px(720);

        // 手机端：日历 / 统计 / 设置 挪到独立页面，主界面只留今天的任务
        if (overlay != 0) { drawOverlay(c, W, H); return; }
        bottomH = twoCol ? 0 : Ui.px(54);

        float bodyTop = headerH;
        c.save();
        c.clipRect(0, bodyTop, W, H - bottomH);
        hitOffset = bodyTop - scrollY;
        c.translate(0, hitOffset);

        if (!twoCol) {
            float y = Ui.px(14);
            y = drawCards(c, boxX0, total, y);
            contentH = y + Ui.px(20);
        } else {
            float leftW = Math.round((total - gap) * 0.60f);
            float rightW = total - gap - leftW;
            float xr = boxX0 + leftW + gap;
            float yl = drawCards(c, boxX0, leftW, Ui.px(14));
            float yr = drawCalendar(c, xr, rightW, Ui.px(14));
            yr = drawStats(c, xr, rightW, yr + gap);
            yr = drawTools(c, xr, rightW, yr + gap);
            contentH = Math.max(yl, yr) + Ui.px(30);
        }
        c.restore();

        clampScroll();
        drawHeader(c, W);
        if (!twoCol) drawBottomBar(c, W, H);

        if (menuOwner >= 0) drawMenu(c, W, H);
    }

    // ---------------------------------------------------------------- 手机端底部按钮栏
    private void drawBottomBar(Canvas c, float W, float H) {
        // 底部栏画在屏幕坐标系里，不跟随内容滚动 ——
        // 命中矩形不能再叠加内容偏移，否则按钮会被挪到屏幕外，怎么点都没反应
        hitOffset = 0;
        float top = H - bottomH;
        Ui.roundRect(c, 0, top, W, H, 0, 0xFFFFFFFF);
        Ui.roundRect(c, 0, top, W, top + 1, 0, Ui.LINE);

        String[] labels = {"月历", "统计", "设置"};
        int[] actions = {A_OV_CAL, A_OV_STATS, A_OV_TOOLS};
        float cellW = W / 3f;
        float m = Ui.px(10);
        float ph = Ui.px(36);
        float py = top + (bottomH - ph) / 2f;
        Paint pl = Ui.font(12.5f, true, Ui.ACCENT);

        for (int i = 0; i < 3; i++) {
            RectF r = new RectF(i * cellW + m, py, (i + 1) * cellW - m, py + ph);
            Ui.roundRect(c, r, ph / 2, Ui.ACCENT_SOFT);

            float iconS = Ui.px(16);
            float gap = Ui.px(6);
            float tw = pl.measureText(labels[i]);
            float sx = r.centerX() - (iconS + gap + tw) / 2f;
            float iy = r.centerY() - iconS / 2f;
            if (i == 0) iconCalendar(c, sx, iy, iconS, Ui.ACCENT);
            else if (i == 1) iconChart(c, sx, iy, iconS, Ui.ACCENT);
            else iconSliders(c, sx, iy, iconS, Ui.ACCENT);

            Ui.textV(c, labels[i], sx + iconS + gap, r.centerY(), pl);
            hit(r, actions[i], null, 0);
        }
    }

    // 三个小图标都用 Canvas 画，省掉图片资源
    private void iconCalendar(Canvas c, float x, float y, float s, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, s * 0.12f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(color);
        c.drawRoundRect(new RectF(x + s * 0.06f, y + s * 0.20f, x + s * 0.94f, y + s * 0.94f),
                s * 0.18f, s * 0.18f, p);
        c.drawLine(x + s * 0.06f, y + s * 0.44f, x + s * 0.94f, y + s * 0.44f, p);
        c.drawLine(x + s * 0.30f, y + s * 0.04f, x + s * 0.30f, y + s * 0.24f, p);
        c.drawLine(x + s * 0.70f, y + s * 0.04f, x + s * 0.70f, y + s * 0.24f, p);
    }

    private void iconChart(Canvas c, float x, float y, float s, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        float w = s * 0.17f;
        float[][] bars = {{0.06f, 0.50f}, {0.41f, 0.26f}, {0.76f, 0.04f}};
        for (float[] b : bars) {
            RectF r = new RectF(x + s * b[0], y + s * b[1], x + s * b[0] + w, y + s * 0.96f);
            c.drawRoundRect(r, w / 2, w / 2, p);
        }
    }

    private void iconSliders(Canvas c, float x, float y, float s, int color) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(color);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(Math.max(1.5f, s * 0.12f));
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(color);
        float[] ys = {0.22f, 0.50f, 0.78f};
        float[] kx = {0.36f, 0.68f, 0.44f};
        for (int i = 0; i < 3; i++) {
            float ly = y + s * ys[i];
            c.drawLine(x + s * 0.06f, ly, x + s * 0.94f, ly, line);
            c.drawCircle(x + s * kx[i], ly, s * 0.13f, dot);
        }
    }

    // ---------------------------------------------------------------- 独立页面
    private void drawOverlay(Canvas c, float W, float H) {
        c.drawColor(Ui.BG);
        float barH = Ui.px(56);
        float pad = Ui.px(14);
        float w = W - pad * 2;

        c.save();
        c.clipRect(0, barH, W, H);
        hitOffset = barH - ovScroll;
        c.translate(0, hitOffset);
        float y = Ui.px(14);
        if (overlay == 1) y = drawCalendar(c, pad, w, y);
        else if (overlay == 2) y = drawStats(c, pad, w, y);
        else y = drawTools(c, pad, w, y);
        ovContentH = y + Ui.px(30);
        c.restore();

        clampOv();

        // 顶栏
        Ui.roundRect(c, 0, 0, W, barH, 0, 0xFFFFFFFF);
        String title = overlay == 1 ? "月历" : overlay == 2 ? "学习统计" : "设置与工具";
        Ui.text(c, title, Ui.px(20), Ui.px(36), Ui.font(17, true, Ui.INK));
        RectF close = new RectF(W - Ui.px(16) - Ui.px(78), Ui.px(13), W - Ui.px(16), Ui.px(43));
        Ui.roundRect(c, close, Ui.px(15), Ui.ACCENT);
        Ui.textC(c, "关闭", close, Ui.font(13, true, 0xFFFFFFFF));
        hitOffset = 0;
        hit(close, A_OV_CLOSE, null, 0);
        Ui.roundRect(c, 0, barH - Ui.px(1), W, barH, 0, Ui.LINE);
    }

    // ---------------------------------------------------------------- 顶部
    private void drawHeader(Canvas c, float W) {
        Ui.roundRect(c, 0, 0, W, headerH, 0, 0xFFFFFFFF);
        // 顶栏内容与下方卡片对齐（超宽屏时不会一个居中一个贴边）
        float padL = boxX0 + Ui.px(16);
        float padR = boxX1 - Ui.px(16);

        ScheduleData.DayPlan dp = ScheduleData.getDay(viewIso);
        Calendar cal = calOf(viewIso);
        String[] wd = {"日", "一", "二", "三", "四", "五", "六"};
        String title = (cal.get(Calendar.MONTH) + 1) + " 月 " + cal.get(Calendar.DAY_OF_MONTH) + " 日　周"
                + wd[cal.get(Calendar.DAY_OF_WEEK) - 1];

        Paint pTitle = Ui.font(20, true, Ui.INK);
        Ui.text(c, title, padL, Ui.px(30), pTitle);

        // 徽章
        String badge = dp == null ? "无课表" : dp.dayKind();
        int bfg = Ui.ACCENT, bbg = Ui.ACCENT_SOFT;
        if (dp != null && dp.isHoliday()) { bfg = Ui.AMBER; bbg = Ui.AMBER_SOFT; }
        else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            bfg = Ui.AMBER; bbg = Ui.AMBER_SOFT;
        }
        Paint pBadge = Ui.font(11, true, bfg);
        float bw = pBadge.measureText(badge) + Ui.px(20);
        float bx = padL + pTitle.measureText(title) + Ui.px(10);
        Ui.roundRect(c, bx, Ui.px(12), bx + bw, Ui.px(36), Ui.px(12), bbg);
        Ui.textC(c, badge, new RectF(bx, Ui.px(12), bx + bw, Ui.px(36)), pBadge);

        // 第二行
        int done = Store.doneCount(viewIso);
        int total = dp == null ? 0 : dp.slots.size();
        String sub = dp == null ? "这一天没有安排"
                : (total + " 个时段 · 共 " + trim(dp.totalMinutes() / 60.0) + " 小时");
        Ui.text(c, sub, padL, Ui.px(54), Ui.font(12, false, Ui.SUB));

        String ptxt = total == 0 ? "今日无安排" : ("今日进度 " + done + " / " + total);
        Paint pProg = Ui.font(12, true, (total > 0 && done >= total) ? Ui.GREEN : Ui.INK);
        Ui.text(c, ptxt, padR - pProg.measureText(ptxt), Ui.px(54), pProg);

        // 进度条
        float barY = Ui.px(64);
        Ui.roundRect(c, padL, barY, padR, barY + Ui.px(8), Ui.px(4), 0xFFE7EBF3);
        double ratio = total == 0 ? 0 : Math.min(1.0, (double) done / total);
        if (ratio > 0) {
            float w = (float) Math.max(Ui.px(8), (padR - padL) * ratio);
            Ui.roundRect(c, padL, barY, padL + w, barY + Ui.px(8), Ui.px(4),
                    (total > 0 && done >= total) ? Ui.GREEN : Ui.ACCENT);
        }

        String s2 = "连续打卡 " + Store.streak() + " 天 · 最长 " + Store.bestStreak()
                + " 天 · 累计 " + trim(Store.totalHours()) + " 小时";
        Ui.text(c, s2, padL, Ui.px(92), Ui.font(11, false, Ui.SUB));

        Ui.roundRect(c, 0, headerH - Ui.px(1), W, headerH, 0, Ui.LINE);
    }

    // ---------------------------------------------------------------- 任务卡片
    private float drawCards(Canvas c, float pad, float w, float y) {
        ScheduleData.DayPlan dp = ScheduleData.getDay(viewIso);
        Ui.text(c, viewIso.equals(ScheduleData.todayIso()) ? "今天要学的内容"
                        : (viewIso + " 的安排（查看模式）"),
                pad + Ui.px(2), y + Ui.px(16), Ui.font(15, true, Ui.INK));
        y += Ui.px(34);

        if (dp == null || dp.slots.isEmpty()) {
            Ui.roundRect(c, pad, y, pad + w, y + Ui.px(60), Ui.px(14), Ui.CARD);
            Ui.textVC(c, "这一天没有安排（计划范围 2026-10 ~ 2027-08）",
                    new RectF(pad + Ui.px(16), y, pad + w - Ui.px(16), y + Ui.px(60)),
                    Ui.font(12, false, Ui.SUB));
            return y + Ui.px(60);
        }
        for (int i = 0; i < dp.slots.size(); i++) {
            y = drawCard(c, pad, w, y, dp.slots.get(i), i);
            y += Ui.px(12);
        }
        return y;
    }

    private float drawCard(Canvas c, float pad, float w, float y, ScheduleData.Slot s, int idx) {
        boolean done = Store.isDone(viewIso, s);
        String book = Links.bookTitleForSlot(s);
        List<String[]> links = Links.forSlot(s);

        // 先量链接高度
        float pillY = 0;
        List<RectF> pillRects = new ArrayList<RectF>();
        Paint pPill = Ui.font(12, false, Ui.SUB);
        {
            float px = Ui.px(16), py = 0, rowH = Ui.px(26), gap = Ui.px(8);
            float maxX = w - Ui.px(16);
            for (String[] l : links) {
                float pw = pPill.measureText(l[0]) + Ui.px(20);
                if (px + pw > maxX && px > Ui.px(16)) { px = Ui.px(16); py += rowH + gap; }
                pillRects.add(new RectF(px, py, px + pw, py + Ui.px(24)));
                px += pw + gap;
            }
            pillY = py + rowH;
        }

        // 标题换行（最多 3 行）。以前是 ellipsize 成一行，稍长的标题就被截掉了。
        // 标题在完成按钮下面，所以第一行也可以占满整个宽度，不用避开按钮。
        Paint pTitle = Ui.font(14, true, done ? Ui.DONE_TEXT : Ui.INK);
        List<String> titleLines = new ArrayList<String>();
        if (!s.body.isEmpty() && s.body.get(0).length() > 0) {
            float titleW = w - Ui.px(32);
            titleLines = Ui.wrap(pTitle, s.body.get(0), titleW);
            if (titleLines.size() > 3) {
                List<String> cut = new ArrayList<String>(titleLines.subList(0, 3));
                cut.set(2, Ui.ellipsize(pTitle, cut.get(2) + "…", titleW));
                titleLines = cut;
            }
        }
        float titleH = titleLines.isEmpty() ? 0 : titleLines.size() * Ui.px(22);

        float h = Ui.px(14) + Ui.px(26);
        if (titleH > 0) h += Ui.px(12) + titleH;
        if (book.length() > 0) h += Ui.px(23);
        h += Math.max(0, s.body.size() - 1) * Ui.px(20);
        if (!links.isEmpty()) h += pillY + Ui.px(10);
        h += Ui.px(14);

        Ui.roundRect(c, pad, y, pad + w, y + h, Ui.px(14), done ? Ui.GREEN_SOFT : Ui.CARD);
        Ui.roundStroke(c, new RectF(pad, y, pad + w, y + h), Ui.px(14), done ? Ui.GREEN_LINE : Ui.LINE, 1f);

        // 左侧色条
        int bc = pillarColor(s.subject);
        Ui.roundRect(c, pad, y + Ui.px(14), pad + Ui.px(4), y + h - Ui.px(14), Ui.px(2), done ? Ui.GREEN : bc);

        float x = pad + Ui.px(16);

        // 完成按钮（先算宽度，占右上角）
        Paint pDone = Ui.font(12, true, done ? 0xFFFFFFFF : Ui.GREEN);
        String dtext = done ? "已完成 ✓" : "标记完成";
        float dw = Math.max(Ui.px(84), pDone.measureText(dtext) + Ui.px(24));
        RectF dr = new RectF(pad + w - Ui.px(16) - dw, y + Ui.px(14), pad + w - Ui.px(16), y + Ui.px(42));
        Ui.roundRect(c, dr, Ui.px(14), done ? Ui.GREEN : 0xFFFFFFFF);
        Ui.roundStroke(c, dr, Ui.px(14), Ui.GREEN, 1f);
        Ui.textC(c, dtext, dr, pDone);
        hit(dr, A_TOGGLE, null, idx);

        // 时间
        Paint pTime = Ui.font(16, true, done ? Ui.GREEN : Ui.INK);
        String t = s.start + " – " + s.end;
        Ui.text(c, t, x, y + Ui.px(30), pTime);

        // 科目徽章
        float sx = x + pTime.measureText(t) + Ui.px(10);
        Paint pSub = Ui.font(13, true, bc);
        float sw = pSub.measureText(s.subject) + Ui.px(16);
        if (sx + sw < dr.left - Ui.px(8)) {
            RectF srect = new RectF(sx, y + Ui.px(12), sx + sw, y + Ui.px(36));
            Ui.roundRect(c, srect, Ui.px(12), (bc & 0x00FFFFFF) | 0x1C000000);
            Ui.textC(c, s.subject, srect, pSub);
        }

        float cy = y + Ui.px(52);

        // 标题（可能多行）
        for (int li = 0; li < titleLines.size(); li++) {
            Ui.text(c, titleLines.get(li), x, cy + li * Ui.px(22), pTitle);
        }
        if (titleH > 0) cy += titleH + Ui.px(2);

        // 课本行
        if (book.length() > 0) {
            Paint pChip = Ui.font(11, true, done ? Ui.GREEN : Ui.ACCENT);
            float cw = pChip.measureText("课本") + Ui.px(14);
            RectF chip = new RectF(x, cy + Ui.px(2), x + cw, cy + Ui.px(19));
            Ui.roundRect(c, chip, Ui.px(9), done ? Ui.GREEN_SOFT : Ui.ACCENT_SOFT);
            Ui.textC(c, "课本", chip, pChip);
            Ui.textVC(c, book, new RectF(x + cw + Ui.px(8), cy, pad + w - Ui.px(16), cy + Ui.px(21)),
                    Ui.font(12, false, done ? Ui.DONE_TEXT : Ui.TEXT_BODY));
            cy += Ui.px(23);
        }

        // 明细
        Paint pD = Ui.font(12, false, Ui.SUB);
        for (int i = 1; i < s.body.size(); i++) {
            Ui.text(c, Ui.ellipsize(pD, s.body.get(i), w - Ui.px(34)), x, cy + Ui.px(14), pD);
            cy += Ui.px(20);
        }

        // 链接胶囊
        if (!links.isEmpty()) {
            float base = y + h - Ui.px(14) - pillY;
            for (int i = 0; i < links.size(); i++) {
                RectF pr = pillRects.get(i);
                // X 也要偏移到卡片左边 —— 只偏移 Y 的话，
                // 单栏时 pad≈0 看不出问题，平板两栏时标签会跑到卡片外面
                pr.offset(pad, base);
                boolean menu = menuOwner == idx && i == links.size() - 1;
                Ui.roundRect(c, pr, pr.height() / 2, menu ? Ui.ACCENT_SOFT : 0xFFFFFFFF);
                Ui.roundStroke(c, pr, pr.height() / 2, menu ? Ui.ACCENT : Ui.LINE, 1f);
                Paint pl = Ui.font(12, false, menu ? Ui.ACCENT : Ui.SUB);
                Ui.textC(c, links.get(i)[0], pr, pl);
                if (i == links.size() - 1) hit(pr, A_MENU, null, idx);
                else hit(pr, A_URL, links.get(i)[1], idx);
            }
        }
        return y + h;
    }

    // ---------------------------------------------------------------- 日历
    private float drawCalendar(Canvas c, float pad, float w, float y) {
        float h = Ui.px(330);
        Ui.roundRect(c, pad, y, pad + w, y + h, Ui.px(14), Ui.CARD);
        Ui.roundStroke(c, new RectF(pad, y, pad + w, y + h), Ui.px(14), Ui.LINE, 1f);

        // 注意：displayMonthIso 是 "2026-10"（7 个字符），calOf 要的是完整日期，必须补上日
        Calendar shown = calOf(displayMonthIso + "-01");
        Ui.text(c, shown.get(Calendar.YEAR) + " 年 " + (shown.get(Calendar.MONTH) + 1) + " 月",
                pad + Ui.px(16), y + Ui.px(32), Ui.font(14, true, Ui.INK));

        // 左右箭头（和标题同一行，右对齐）
        float ax = pad + w - Ui.px(16) - Ui.px(58);
        RectF prev = new RectF(ax, y + Ui.px(14), ax + Ui.px(26), y + Ui.px(38));
        RectF next = new RectF(ax + Ui.px(32), y + Ui.px(14), ax + Ui.px(58), y + Ui.px(38));
        Ui.roundRect(c, prev, Ui.px(8), 0x0F000000);
        Ui.roundRect(c, next, Ui.px(8), 0x0F000000);
        Ui.textC(c, "‹", prev, Ui.font(16, true, Ui.SUB));
        Ui.textC(c, "›", next, Ui.font(16, true, Ui.SUB));
        hit(prev, A_DAY, shiftMonth(displayMonthIso, -1) + "#month", 0);
        hit(next, A_DAY, shiftMonth(displayMonthIso, 1) + "#month", 0);

        // 星期表头和日期网格都要排在标题下面，否则会互相压在一起
        String[] wd = {"一", "二", "三", "四", "五", "六", "日"};
        float gx = pad + Ui.px(12);
        float gw = w - Ui.px(24);
        float cw = gw / 7f;
        float headTop = y + Ui.px(44);
        float gy = y + Ui.px(72);                       // 日期网格起点
        float ch = (h - Ui.px(80)) / 6f;                // 6 行自适应，别写死

        Ui.roundRect(c, gx, headTop + Ui.px(22), gx + gw, headTop + Ui.px(23), 0, Ui.LINE);
        for (int i = 0; i < 7; i++) {
            Ui.textC(c, wd[i], new RectF(gx + i * cw, headTop, gx + (i + 1) * cw, headTop + Ui.px(22)),
                    Ui.font(13, false, i >= 5 ? Ui.AMBER : Ui.SUB));
        }

        Calendar first = calOf(displayMonthIso + "-01");
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;     // 周一为第一列
        Calendar cur = (Calendar) first.clone();
        cur.add(Calendar.DAY_OF_MONTH, -offset);
        String today = ScheduleData.todayIso();

        for (int i = 0; i < 42; i++) {
            String iso = ScheduleData.iso(cur);
            int col = i % 7, row = i / 7;
            RectF r = new RectF(gx + col * cw + Ui.px(2), gy + row * ch + Ui.px(2),
                    gx + (col + 1) * cw - Ui.px(2), gy + (row + 1) * ch - Ui.px(2));
            boolean inMonth = iso.substring(0, 7).equals(displayMonthIso);
            ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
            boolean sched = dp != null;
            int dn = sched ? Store.doneCount(iso) : 0;
            boolean perfect = sched && dp.slots.size() > 0 && dn >= dp.slots.size();
            boolean isToday = iso.equals(today);
            boolean isSel = iso.equals(viewIso);

            if (perfect) Ui.roundRect(c, r, Ui.px(10), Ui.GREEN);
            else if (isSel) Ui.roundRect(c, r, Ui.px(10), Ui.ACCENT_SOFT);
            if (isToday && !perfect) Ui.roundStroke(c, r, Ui.px(10), Ui.ACCENT, Ui.px(1.5f));

            int day = cur.get(Calendar.DAY_OF_MONTH);
            int fg = !inMonth ? 0xFFC6CEDC : perfect ? 0xFFFFFFFF
                    : (col >= 5 ? Ui.AMBER : Ui.INK);
            Ui.textC(c, String.valueOf(day), r, Ui.font(14, isToday, fg));

            if (sched && inMonth && !perfect && dp.slots.size() > 0) {
                int dots = Math.min(dn, 3);
                Paint pd = new Paint(Paint.ANTI_ALIAS_FLAG);
                pd.setColor(dots > 0 ? Ui.GREEN : 0x3C788CB4);
                for (int k = 0; k < Math.max(1, dots); k++) {
                    float dx = r.centerX() - (Math.max(1, dots) - 1) * Ui.px(4) + k * Ui.px(8);
                    c.drawCircle(dx, r.bottom - Ui.px(5), Ui.px(2), pd);
                }
            }
            if (inMonth) hit(r, A_DAY, iso, 0);
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }
        return y + h;
    }

    String displayMonthIso = ScheduleData.todayIso().substring(0, 7);
    /** 内容区左右边界（超宽屏时整体居中） */
    private float boxX0 = 0, boxX1 = 0;

    // ---------------------------------------------------------------- 统计 / 工具
    private float drawStats(Canvas c, float pad, float w, float y) {
        String[] lines = statLines();
        float h = Ui.px(48) + lines.length * Ui.px(23) + Ui.px(16);
        Ui.roundRect(c, pad, y, pad + w, y + h, Ui.px(14), Ui.CARD);
        Ui.roundStroke(c, new RectF(pad, y, pad + w, y + h), Ui.px(14), Ui.LINE, 1f);
        Ui.text(c, "学习统计", pad + Ui.px(16), y + Ui.px(28), Ui.font(14, true, Ui.INK));
        Paint p = Ui.font(12, false, Ui.SUB);
        float ly = y + Ui.px(52);
        for (String l : lines) { Ui.text(c, l, pad + Ui.px(16), ly + Ui.px(14), p); ly += Ui.px(23); }
        return y + h;
    }

    String[] statLines() {
        int[] wk = Store.weekProgress();
        return new String[]{
                "本周完成：" + wk[0] + " / " + wk[1] + " 个时段",
                "累计完成：" + Store.totalDone() + " 个时段 · " + trim(Store.totalHours()) + " 小时",
                "连续打卡：" + Store.streak() + " 天（最长 " + Store.bestStreak() + " 天）",
                "本月全勤：" + Store.perfectDaysThisMonth() + " 天",
        };
    }

    /** 工具区的分组：[标题, 按钮..., 以 \u0001 开头的说明行(可选)] */
    private List<String[]> toolGroups() {
        List<String[]> gs = new ArrayList<String[]>();
        // 「导出/导入 CSV」已去掉 —— 打卡记录和 CSV 都会在同步时自动传到 GitHub，
        // 想要表格直接去仓库下载 sync/StudyRecord.csv
        // 「检查更新」放在这里而不是 GitHub 组里 —— 仓库是公开的，查更新不需要登录
        gs.add(new String[]{"其他", "查看更新日志", "检查更新", "回到今天"});
        gs.add(new String[]{"提醒", Store.autoRemind() ? "到点提醒：已开启" : "到点提醒：已关闭"});

        boolean cfg = GitHub.configured();
        boolean in = GitHub.loggedIn(getContext());
        if (!cfg) {
            gs.add(new String[]{"GitHub 同步", "登录 GitHub",
                    "\u0001还没填 OAuth App 的 Client ID，需要重新编译"});
        } else if (!in) {
            gs.add(new String[]{"GitHub 同步", "登录 GitHub",
                    "\u0001登录后会自动同步打卡记录，并把 CSV 传到云端"});
        } else {
            gs.add(new String[]{"GitHub 同步", "立即同步", "退出登录",
                    "\u0001已登录：" + GitHub.user(getContext())});
        }
        return gs;
    }

    /** 量一组按钮会占几行 */
    private int buttonRows(String[] labels, float avail, Paint p) {
        int rows = 1;
        float bx = 0;
        for (int i = 1; i < labels.length; i++) {
            String label = labels[i];
            if (label.startsWith("\u0001")) continue;
            float bw = Math.max(Ui.px(104), p.measureText(label) + Ui.px(24));
            if (bx + bw > avail && bx > 0) { rows++; bx = 0; }
            bx += bw + Ui.px(8);
        }
        return rows;
    }

    private float drawTools(Canvas c, float pad, float w, float y) {
        List<String[]> groups = toolGroups();
        float avail = w - Ui.px(32);
        Paint pBtn0 = Ui.font(12, true, Ui.SUB);
        Paint pNote = Ui.font(11, false, Ui.TEXT_DIM);

        // 先量高度：按钮会自动换行，高度必须按换行后的行数算，否则卡片会高度不足被裁掉
        float h = Ui.px(48);
        for (String[] g : groups) {
            h += Ui.px(22);
            h += buttonRows(g, avail, pBtn0) * (Ui.px(30) + Ui.px(8));
            for (int i = 1; i < g.length; i++)
                if (g[i].startsWith("\u0001")) h += Ui.px(18);
            h += Ui.px(10);
        }
        h += Ui.px(2);

        Ui.roundRect(c, pad, y, pad + w, y + h, Ui.px(14), Ui.CARD);
        Ui.roundStroke(c, new RectF(pad, y, pad + w, y + h), Ui.px(14), Ui.LINE, 1f);
        Ui.text(c, "设置与工具", pad + Ui.px(16), y + Ui.px(28), Ui.font(14, true, Ui.INK));

        float cy = y + Ui.px(48);
        Paint pLabel = Ui.font(11, true, Ui.TEXT_DIM);
        for (String[] g : groups) {
            Ui.text(c, g[0], pad + Ui.px(16), cy + Ui.px(14), pLabel);
            cy += Ui.px(22);

            // 先把按钮全部排完（会自动换行），再画说明行。
            // 之前是在同一个循环里边排按钮边画说明，说明行用的还是按钮那一行的 cy，
            // 结果说明文字直接压在按钮上。
            float bx = pad + Ui.px(16);
            float rowTop = cy;
            for (int i = 1; i < g.length; i++) {
                String label = g[i];
                if (label.startsWith("\u0001")) continue;
                boolean primary = "提醒".equals(g[0]) || "登录 GitHub".equals(label);
                Paint pBtn = Ui.font(12, true, primary ? 0xFFFFFFFF : Ui.SUB);
                float bw = Math.max(Ui.px(104), pBtn.measureText(label) + Ui.px(24));
                if (bx + bw > pad + w - Ui.px(16) && bx > pad + Ui.px(16)) { bx = pad + Ui.px(16); rowTop += Ui.px(38); }
                RectF r = new RectF(bx, rowTop, bx + bw, rowTop + Ui.px(30));
                Ui.roundRect(c, r, Ui.px(15), primary ? Ui.ACCENT : 0xFFFFFFFF);
                Ui.roundStroke(c, r, Ui.px(15), primary ? Ui.ACCENT : Ui.LINE, 1f);
                Ui.textC(c, label, r, pBtn);

                int action = A_LOG;
                if ("查看更新日志".equals(label)) action = A_LOG;
                else if ("回到今天".equals(label)) action = A_TODAY;
                else if (label.startsWith("到点提醒")) action = A_REMIND;
                else if (label.startsWith("登录 GitHub")) action = A_GH_LOGIN;
                else if ("立即同步".equals(label)) action = A_GH_SYNC;
                else if ("检查更新".equals(label)) action = A_GH_UPDATE;
                else if ("退出登录".equals(label)) action = A_GH_LOGOUT;
                hit(r, action, null, 0);
                bx += bw + Ui.px(8);
            }
            cy = rowTop + Ui.px(30);

            for (int i = 1; i < g.length; i++) {
                String label = g[i];
                if (!label.startsWith("\u0001")) continue;
                Ui.text(c, label.substring(1), pad + Ui.px(16), cy + Ui.px(13), pNote);
                cy += Ui.px(18);
            }
            cy += Ui.px(10);
        }
        return y + h;
    }

    // ---------------------------------------------------------------- 下拉菜单
    private void drawMenu(Canvas c, float W, float H) {
        if (menuItems.isEmpty()) { menuOwner = -1; return; }
        // 菜单画在屏幕坐标系里，不跟随内容滚动 —— 命中矩形也不能再叠加滚动偏移
        hitOffset = 0;
        Paint pItem = Ui.font(13, false, Ui.TEXT_BODY);
        float maxW = 0;
        for (String[] it : menuItems) maxW = Math.max(maxW, pItem.measureText(it[0]));
        float mw = Math.min(W - Ui.px(24), Math.max(Ui.px(285), maxW + Ui.px(70)));
        float rowH = Ui.px(44), titleH = Ui.px(44);
        float mh = titleH + menuItems.size() * rowH + Ui.px(10);

        // 定位到被点的那个胶囊下方
        float anchorTop = Ui.px(200), anchorLeft = Ui.px(12);
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).action == A_MENU && hits.get(i).index == menuOwner) {
                anchorTop = hits.get(i).r.bottom;
                anchorLeft = hits.get(i).r.left;
                break;
            }
        }
        float mx = Math.max(Ui.px(12), Math.min(anchorLeft, boxX1 - mw - Ui.px(12)));
        float my = anchorTop + Ui.px(6);
        if (my + mh > H) my = Math.max(Ui.px(12), anchorTop - mh - Ui.px(52));
        RectF mr = new RectF(mx, my, mx + mw, my + mh);

        Ui.roundRect(c, mr, Ui.px(12), 0xFFFFFFFF);
        Ui.roundStroke(c, mr, Ui.px(12), 0xFFD8DEE9, 1f);
        Ui.text(c, "配套教学视频资源", mx + Ui.px(16), my + Ui.px(28), Ui.font(13, true, Ui.INK));
        Ui.roundRect(c, mx + Ui.px(12), my + titleH - Ui.px(9), mx + mw - Ui.px(12), my + titleH - Ui.px(8), 0, Ui.LINE);

        for (int i = 0; i < menuItems.size(); i++) {
            RectF r = new RectF(mx + Ui.px(6), my + titleH + i * rowH,
                    mx + mw - Ui.px(6), my + titleH + (i + 1) * rowH - Ui.px(6));
            Ui.textVC(c, menuItems.get(i)[0],
                    new RectF(r.left + Ui.px(14), r.top, r.right - Ui.px(36), r.bottom), pItem);
            Ui.text(c, "›", r.right - Ui.px(26), r.centerY() + Ui.px(5), Ui.font(14, true, 0xFFC3CCDA));
            hit(r, A_URL, menuItems.get(i)[1], 0);
        }
        // 点菜单外任意处关闭
        RectF outside = new RectF(0, 0, W, my);
        hit(outside, A_CLOSE_MENU, null, 0);
    }

    // ---------------------------------------------------------------- 小工具
    /**
     * "yyyy-MM-dd" 或 "yyyy-MM" → Calendar。
     * 只给月的时候补成 1 号，避免 substring 越界（之前真机闪退就是这个）。
     */
    static Calendar calOf(String iso) {
        String s = iso == null ? "" : iso.trim();
        if (s.length() == 7) s = s + "-01";
        while (s.length() < 10) s = s + "-01";
        Calendar c = Calendar.getInstance();
        try {
            c.set(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)) - 1,
                    Integer.parseInt(s.substring(8, 10)));
        } catch (Exception e) {
            // 实在解析不了就用今天，总比崩了强
            c = Calendar.getInstance();
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    static String shiftMonth(String ym, int delta) {
        Calendar c = calOf(ym + "-01");
        c.add(Calendar.MONTH, delta);
        return String.format("%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    static String trim(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) return String.valueOf(Math.round(v));
        return String.format("%.1f", v);
    }

    static int pillarColor(String subject) {
        if (subject.contains("纯数")) return Ui.ACCENT;
        if (subject.contains("物理")) return 0xFF7A5AF8;
        if (subject.contains("应用")) return 0xFF0E9AA7;
        if (subject.contains("进阶")) return 0xFFD9548B;
        if (subject.contains("英语")) return Ui.AMBER;
        return Ui.SUB;
    }

    /** 供外部（点日历后的月份切换）使用 */
    public void setViewIso(String iso) {
        if (iso.endsWith("#month")) {
            displayMonthIso = iso.substring(0, iso.length() - 6);
        } else {
            viewIso = iso;
            displayMonthIso = iso.substring(0, 7);
            scrollY = 0;
        }
        invalidate();
    }
}
