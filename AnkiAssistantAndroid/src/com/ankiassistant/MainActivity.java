package com.ankiassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 入口 + 导航框架。
 *
 * 手机（最短边 < 600dp）：底部横栏三个页签。
 * 平板（最短边 >= 600dp）：左边一个可收起的选择栏（需求里的"左边拉一个选择框"）。
 * 旋转屏幕不重建 Activity（manifest 里声明了 configChanges），只重排容器，
 * 因此编辑到一半的卡片不会因为转屏而丢失。
 */
public class MainActivity extends Activity {

    public static final int TAB_CREATE = 0;
    public static final int TAB_BROWSE = 1;
    public static final int TAB_SETTINGS = 2;

    public Store store;
    private CreateView createView;
    private BrowseView browseView;
    private SettingsView settingsView;

    private LinearLayout root, topBar, rail, bottomBar;
    private FrameLayout content;
    private NavItem[] railItems = new NavItem[3];
    private NavItem[] barItems = new NavItem[3];
    private boolean tablet;
    private int current = TAB_CREATE;
    private boolean built;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashHandler.install(this);
        store = new Store(this);
        applySystemBars();
        buildUi();
        show(current);
        maybeIntro();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        buildUi();
        show(current);
    }

    /** 状态栏/导航栏配色（浅色背景 + 深色图标） */
    private void applySystemBars() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Ui.BG);
            getWindow().setNavigationBarColor(Ui.BG);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            View dec = getWindow().getDecorView();
            dec.setSystemUiVisibility(dec.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    // ------------------------------------------------------------------ 布局

    private void buildUi() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        tablet = Math.min(dm.widthPixels, dm.heightPixels) / dm.density >= 600f;

        if (createView == null) {
            createView = new CreateView(this);
            browseView = new BrowseView(this);
            settingsView = new SettingsView(this);
        }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);

        // ---- 顶栏 ----
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Ui.WHITE);
        topBar.setPadding(Ui.dp(14), 0, Ui.dp(14), 0);
        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(52)));

        if (tablet) {
            final TextView menu = new TextView(this);
            menu.setText("☰");
            menu.setTextSize(20);
            menu.setTextColor(Ui.INK);
            menu.setGravity(android.view.Gravity.CENTER);
            menu.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(40), ViewGroup.LayoutParams.MATCH_PARENT));
            menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { toggleRail(); }
            });
            topBar.addView(menu);
        }

        TextView title = new TextView(this);
        Ui.title(title, "Anki 助手");
        title.setTextSize(18);
        title.setPadding(tablet ? Ui.dp(6) : 0, 0, 0, 0);
        topBar.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView ver = new TextView(this);
        ver.setText(Version.VERSION_TAG);
        ver.setTextColor(Ui.TEXT_DIM);
        ver.setTextSize(12);
        ver.setGravity(android.view.Gravity.CENTER_VERTICAL);
        topBar.addView(ver, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- 主体 ----
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(tablet ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        body.setBackgroundColor(Ui.BG);
        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (tablet) {
            rail = buildRail();
            body.addView(rail, new LinearLayout.LayoutParams(Ui.dp(176), ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            rail = null;
        }

        content = new FrameLayout(this);
        if (tablet) {
            body.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        } else {
            body.addView(content, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        if (!tablet) {
            bottomBar = buildBottomBar();
            body.addView(bottomBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(62)));
        } else {
            bottomBar = null;
        }
        built = true;
    }

    private LinearLayout buildRail() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(Ui.WHITE);
        r.setPadding(Ui.dp(8), Ui.dp(10), Ui.dp(8), Ui.dp(10));
        String[] labels = {"制卡", "浏览", "设置"};
        int[] icons = {IconDrawable.ADD, IconDrawable.CARDS, IconDrawable.SLIDERS};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            NavItem item = new NavItem(this, false, labels[i], icons[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { show(idx); }
            });
            railItems[i] = item;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(48));
            lp.bottomMargin = Ui.dp(6);
            r.addView(item, lp);
        }
        // 底部留白，视觉上更像"抽屉"
        View spacer = new View(this);
        r.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView tip = new TextView(this);
        tip.setText("点 ☰ 可收起\n本栏只在平板上出现");
        tip.setTextColor(Ui.TEXT_DIM);
        tip.setTextSize(11);
        tip.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(8));
        r.addView(tip);
        return r;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        bar.setBackground(Ui.roundStroke(Ui.WHITE, Ui.LINE, 0));
        String[] labels = {"制卡", "浏览", "设置"};
        int[] icons = {IconDrawable.ADD, IconDrawable.CARDS, IconDrawable.SLIDERS};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            NavItem item = new NavItem(this, true, labels[i], icons[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { show(idx); }
            });
            barItems[i] = item;
            bar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        return bar;
    }

    private void toggleRail() {
        if (rail == null) return;
        if (rail.getVisibility() == View.GONE) {
            rail.setVisibility(View.VISIBLE);
            rail.setTranslationX(-Ui.dp(176));
            rail.animate().translationX(0).setDuration(160).start();
        } else {
            rail.animate().translationX(-Ui.dp(176)).setDuration(160)
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            if (rail != null) rail.setVisibility(View.GONE);
                        }
                    }).start();
        }
    }

    // ------------------------------------------------------------------ 切页

    public void show(int index) {
        if (content == null) return;
        current = index;
        View v = index == TAB_BROWSE ? browseView
                : index == TAB_SETTINGS ? settingsView : createView;
        if (v.getParent() instanceof ViewGroup) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        content.removeAllViews();
        content.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        for (int i = 0; i < 3; i++) {
            boolean on = (i == index);
            if (railItems[i] != null) railItems[i].setActive(on);
            if (barItems[i] != null) barItems[i].setActive(on);
        }
        if (index == TAB_CREATE) createView.onShown();
        if (index == TAB_BROWSE) browseView.onShown();
        if (index == TAB_SETTINGS) settingsView.onShown();
    }

    @Override
    public void onBackPressed() {
        // 不退出应用：任何页面按返回都先回到制卡页（防误触丢卡片）
        if (current != TAB_CREATE) show(TAB_CREATE);
    }

    // ------------------------------------------------------------------ 首次引导

    private void maybeIntro() {
        if (store.introShown()) return;
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("三步开始使用")
                .setMessage("1. 电脑上的 Anki 安装 AnkiConnect 插件（ID 2055492159）\n\n"
                        + "2. 设置里填电脑的局域网 IP，点「测试连接」\n\n"
                        + "3. 制卡页输入单词 → AI 填充 → 保存到 Anki（自动同步到 AnkiWeb 云端）\n\n"
                        + "详细步骤见「设置 → Anki 连接」。")
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        store.setIntroShown(true);
                        show(TAB_SETTINGS);
                    }
                })
                .setNegativeButton("直接开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        store.setIntroShown(true);
                    }
                })
                .create();
        d.show();
    }

    // ------------------------------------------------------------------ 导航项

    /** 底部栏（竖向）或左侧栏（横向）里的一个页签 */
    static class NavItem extends LinearLayout {
        private final ImageView icon;
        private final TextView label;
        private final boolean horizontal;
        private final int iconType;
        private boolean active;

        NavItem(android.content.Context ctx, boolean horizontalMode, String text, int type) {
            super(ctx);
            horizontal = horizontalMode;
            iconType = type;
            setOrientation(horizontal ? HORIZONTAL : VERTICAL);
            setGravity(android.view.Gravity.CENTER);

            icon = new ImageView(ctx);
            int sz = Ui.dp(horizontal ? 22 : 21);
            LayoutParams ilp = horizontal
                    ? new LayoutParams(sz, sz)
                    : new LayoutParams(sz, sz);
            if (horizontal) {
                ilp = new LayoutParams(sz, sz);
                ilp.rightMargin = Ui.dp(10);
            }
            icon.setLayoutParams(ilp);
            addView(icon);

            label = new TextView(ctx);
            label.setText(text);
            label.setTextSize(horizontal ? 15 : 11);
            label.setGravity(android.view.Gravity.CENTER);
            addView(label, horizontal
                    ? new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    : new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            if (!horizontal) {
                LayoutParams lp = (LayoutParams) label.getLayoutParams();
                lp.topMargin = Ui.dp(3);
            }
            setActive(false);
        }

        void setActive(boolean on) {
            active = on;
            int color = on ? Ui.ACCENT : Ui.SUB;
            icon.setImageDrawable(new IconDrawable(iconType, color));
            label.setTextColor(color);
            setBackground(on ? Ui.round(Ui.ACCENT_SOFT, horizontal ? 10 : 12)
                            : Ui.round(0x00000000, horizontal ? 10 : 12));
            if (horizontal) setPadding(Ui.dp(10), 0, Ui.dp(6), 0);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
        }

        boolean isActive() { return active; }
    }
}
