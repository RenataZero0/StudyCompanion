package com.studycompanion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * ⚠️ 本文件有两条必须遵守的约束（都是被 d8 逼出来的）：
 *
 * 1. 不用 lambda —— javac --release 8 把 lambda 编成 invokedynamic，d8 处理时容易出问题。
 * 2. 匿名内部类最多嵌套两层 —— build-tools 34 的 d8 在解析三层嵌套（形如
 *    MainActivity$1$1$1，EnclosingMethod = MainActivity$1$1.run）时会内部崩溃：
 *    java.lang.NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null
 *
 * 所以这里所有对话框和按钮监听器都写在**具名方法**里，后台线程只负责调方法。
 */
public class MainActivity extends Activity implements MainView.Listener {

    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;

    private MainView view;
    private AlertDialog busy;
    private boolean cancelLogin = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // 尽早装上：这样后面任何地方抛异常都会留下堆栈，而不是只看到一个「闪退」
        CrashHandler.install(this);
        Ui.S = getResources().getDisplayMetrics().density;
        ScheduleData.load(this);
        Store.init(this);
        Reminder.ensureChannel(this);

        FrameLayout root = new FrameLayout(this);
        view = new MainView(this, this);
        root.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        requestNotifPermission();
        if (Store.autoRemind()) Reminder.scheduleAll(this);

        showLastCrash();
        autoCheckUpdate();
    }

    /**
     * 启动时自动查一次有没有新版（仓库公开，不需要登录）。
     * 每天最多查一次，避免每次开都弹；查不到就静默失败，绝不打扰。
     */
    private void autoCheckUpdate() {
        final android.content.SharedPreferences sp =
                getSharedPreferences("study", MODE_PRIVATE);
        final String today = ScheduleData.todayIso();
        if (today.equals(sp.getString("lastUpdateCheck", ""))) return;

        new Thread(new Runnable() {
            public void run() {
                try {
                    final GitHub.Release rel = GitHub.latestRelease(MainActivity.this);
                    sp.edit().putString("lastUpdateCheck", today).apply();
                    if (rel.tag.length() == 0) return;
                    // 远端不比本机新就什么都不做（之前用 equals，降级也会被当成升级）
                    if (GitHub.compareVersion(rel.tag, GitHub.VERSION_TAG) <= 0) return;
                    if (rel.apkDownload(false).length() == 0) return;
                    runOnUiThread(new Runnable() {
                        public void run() { showUpdateDialog(rel); }
                    });
                } catch (Exception e) {
                    // 没网 / 被墙 / 限流都无所谓，静默跳过
                }
            }
        }).start();
    }

    /**
     * 发现新版本的提示。正文放进可滚动区域并且限制高度，
     * 否则改动说明一长，底下的按钮就被挤出屏幕了。
     */
    private void showUpdateDialog(final GitHub.Release rel) {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(pad, (int) (8 * d), pad, 0);

        // 版本 / 大小
        android.widget.TextView info = new android.widget.TextView(this);
        info.setText("当前版本：" + GitHub.VERSION_TAG
                + "\n最新版本：" + rel.tag
                + "\n安装包大小：" + (rel.apkSize / 1024) + " KB");
        info.setTextSize(13f);
        info.setTextColor(0xFF71809A);
        info.setLineSpacing(0, 1.2f);
        box.addView(info);

        // 改动说明（可滚动，最高约占屏幕 40%）
        String notes = mdToPlain(rel.notes);
        if (notes.length() > 0) {
            android.widget.TextView body = new android.widget.TextView(this);
            body.setText(notes);
            body.setTextSize(13.5f);
            body.setTextColor(0xFF3C4A60);
            body.setLineSpacing(0, 1.3f);
            body.setPadding(0, pad, 0, 0);

            int wSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                    getResources().getDisplayMetrics().widthPixels - pad * 2,
                    android.view.View.MeasureSpec.AT_MOST);
            body.measure(wSpec, android.view.View.MeasureSpec.UNSPECIFIED);
            int bodyH = body.getMeasuredHeight();
            int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.40f);
            int h = Math.min(bodyH, maxH);

            android.widget.ScrollView sv = new android.widget.ScrollView(this);
            sv.addView(body);
            box.addView(sv, new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, h));
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + rel.tag)
                .setView(box)
                .setPositiveButton("下载并安装", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d2, int w) { downloadApk(rel); }
                })
                .setNeutralButton("Release 页", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d2, int w) { openUrl(rel.pageUrl); }
                })
                .setNegativeButton("以后再说", null)
                .create();
        dlg.show();

        // Material 主题默认把对话框按钮文字转成全大写（"Release 页" -> "RELEASE 页"），关掉
        android.widget.Button b1 = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button b2 = dlg.getButton(AlertDialog.BUTTON_NEUTRAL);
        android.widget.Button b3 = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (b1 != null) b1.setAllCaps(false);
        if (b2 != null) b2.setAllCaps(false);
        if (b3 != null) b3.setAllCaps(false);
    }

    /** 上次崩溃过就把堆栈弹出来 —— 手机上没法看 logcat，这是唯一能拿到线索的办法 */
    private void showLastCrash()
    {
        final String log = CrashHandler.last(this);
        if (log == null) return;
        String shown = log.length() > 1600 ? log.substring(0, 1600) + "\n…（完整内容已存成文件）" : log;
        new AlertDialog.Builder(this)
                .setTitle("上次运行时崩溃了")
                .setMessage(shown
                        + "\n\n完整日志在：\n" + CrashHandler.file(this).getAbsolutePath())
                .setPositiveButton("复制", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        copyCrash(log);
                    }
                })
                .setNeutralButton("分享", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        shareCrash(log);
                    }
                })
                .setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        CrashHandler.clear(MainActivity.this);
                    }
                })
                .show();
    }

    private void copyCrash(String log)
    {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", log));
            toast("崩溃日志已复制");
        } catch (Exception e) { toast("复制失败：" + e); }
        CrashHandler.clear(this);
    }

    private void shareCrash(String log)
    {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "StudyCompanion 崩溃日志");
            i.putExtra(Intent.EXTRA_TEXT, log);
            startActivity(Intent.createChooser(i, "发送崩溃日志"));
        } catch (Exception e) { copyCrash(log); return; }
        CrashHandler.clear(this);
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (view != null) view.invalidate();
    }

    /** 独立页面（月历/统计/设置）打开时，返回键先关它，而不是直接退出应用 */
    @Override
    public void onBackPressed() {
        if (view != null && view.closeOverlay()) return;
        super.onBackPressed();
    }

    // ================================================================== CSV
    /** 包名 */
    static final String PKG_YOUTUBE = "com.google.android.youtube";
    static final String PKG_BILI = "tv.danmaku.bili";

    /**
     * 打开链接。装了对应客户端就优先用客户端 ——
     * YouTube / B 站的网页版在手机浏览器里体验很差，直接跳 App 更顺。
     * 客户端没装（或跳转失败）再退回系统默认浏览器。
     */
    @Override
    public void openUrl(String url) {
        if (url == null || url.length() == 0) return;

        // bilipick:<关键词>|<当天主题> —— 去 B 站挑相关视频；
        // 主题用于在多 P 合集里自动定位最相关的那一集
        if (url.startsWith("bilipick:")) {
            String rest = url.substring("bilipick:".length());
            int bar = rest.indexOf('|');
            String kw = bar < 0 ? rest : rest.substring(0, bar);
            String topics = bar < 0 ? "" : rest.substring(bar + 1);
            pickBiliVideo(kw, topics);
            return;
        }

        for (Intent candidate : appIntents(url)) {
            try {
                candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(candidate);
                return;
            } catch (Exception ignored) {
                // 换下一个候选（客户端 -> 网页）
            }
        }
        try {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(web);
        } catch (Exception e) {
            toast("打不开这个链接：" + url);
        }
    }

    // ================================================================== B 站挑视频
    /**
     * 去 B 站查一遍，按「跟 A Level / CIE 沾不沾边」打分排序，
     * 把最好的几个列出来。查不到就退回搜索页。
     */
    private void pickBiliVideo(final String keyword, final String topics) {
        busy = new AlertDialog.Builder(this)
                .setTitle("正在找视频…").setMessage(keyword)
                .setCancelable(false).show();

        new Thread(new Runnable() {
            public void run() {
                List<Bili.Video> list = null;
                String err = null;
                try {
                    list = Bili.top(keyword, 6);
                    if (list.isEmpty()) err = "没搜到";
                } catch (Exception e) {
                    err = String.valueOf(e.getMessage());
                }
                final List<Bili.Video> fl = list;
                final String fe = err;
                runOnUiThread(new Runnable() {
                    public void run() { showBiliPicker(keyword, topics, fl, fe); }
                });
            }
        }).start();
    }

    private void showBiliPicker(final String keyword, final String topics,
            final List<Bili.Video> list, String err) {
        dismissBusy();

        if (list == null || list.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("没找到合适的视频")
                    .setMessage(err == null ? keyword : (keyword + "\n\n" + err))
                    .setPositiveButton("在 B 站搜索", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { openBiliSearch(keyword); }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        float d = getResources().getDisplayMetrics().density;
        box.setPadding((int) (14 * d), (int) (4 * d), (int) (14 * d), (int) (4 * d));
        sv.addView(box);

        for (int i = 0; i < list.size(); i++) {
            box.addView(videoButton(list.get(i), i == 0, topics));
        }

        // 限高：按钮再多也不把对话框撑出屏幕
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.55f);
        box.measure(android.view.View.MeasureSpec.makeMeasureSpec(
                        getResources().getDisplayMetrics().widthPixels - (int) (48 * d),
                        android.view.View.MeasureSpec.AT_MOST),
                android.view.View.MeasureSpec.UNSPECIFIED);
        sv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                Math.min(box.getMeasuredHeight(), maxH)));

        new AlertDialog.Builder(this)
                .setTitle("点一下直接播放")
                .setView(sv)
                .setNeutralButton("更多搜索", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { openBiliSearch(keyword); }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 一个视频 = 一个圆角按钮：标题 + 说明 + 分P标记 */
    private android.widget.TextView videoButton(final Bili.Video v, boolean top, final String topics) {
        float d = getResources().getDisplayMetrics().density;
        android.widget.TextView tv = new android.widget.TextView(this);

        String sub = v.subtitle();
        if (v.videos > 1) sub = (sub.length() > 0 ? sub + " · " : "") + "共 " + v.videos + " 集";
        tv.setText((top ? "★ 推荐  " : "") + v.title + "\n" + sub);
        tv.setTextSize(14f);
        tv.setTextColor(top ? 0xFF1B2432 : 0xFF3C4A60);
        tv.setLineSpacing(0, 1.2f);
        tv.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
        if (top) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(0xFFFFFFFF);
        gd.setCornerRadius(12 * d);
        gd.setStroke((int) (1 * d), top ? 0xFF3568E8 : 0xFFE5E9F0);
        tv.setBackground(gd);

        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View w) {
                if (v.videos > 1) pickPart(v, topics);
                else openBiliPart(v, 0);
            }
        });
        return tv;
    }

    /** 多 P 视频：先读分 P 列表，按当天主题匹配出最相关那一集直接跳 */
    private void pickPart(final Bili.Video v, final String topics) {
        busy = new AlertDialog.Builder(this)
                .setTitle("正在定位分 P…").setMessage(v.title)
                .setCancelable(false).show();
        new Thread(new Runnable() {
            public void run() {
                List<Bili.Part> parts = null;
                String err = null;
                try { parts = Bili.parts(v.bvid); } catch (Exception e) { err = String.valueOf(e.getMessage()); }
                final List<Bili.Part> fp = parts;
                final String fe = err;
                final int bi = (fp == null || fp.isEmpty()) ? -1 : Bili.bestPart(fp, topics);
                runOnUiThread(new Runnable() {
                    public void run() { showPartsDialog(v, topics, fp, fe, bi); }
                });
            }
        }).start();
    }

    private void showPartsDialog(final Bili.Video v, String topics,
            List<Bili.Part> parts, String err, int bestIndex) {
        dismissBusy();
        // 读不到分 P 就整个视频打开，别卡住用户
        if (parts == null || parts.isEmpty()) { openBiliPart(v, 0); return; }

        // 匹配到了对应的那一集，直接跳过去（toast 说明挑了哪一集）
        if (bestIndex >= 0) {
            Bili.Part p = parts.get(bestIndex);
            String label = p.part.length() > 0 ? p.part : ("第 " + p.page + " P");
            toast("已定位到 P" + p.page + " · " + label);
            openBiliPart(v, p.page);
            return;
        }

        float d = getResources().getDisplayMetrics().density;
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding((int) (14 * d), (int) (4 * d), (int) (14 * d), (int) (4 * d));
        sv.addView(box);

        for (int i = 0; i < parts.size(); i++) {
            final Bili.Part p = parts.get(i);
            android.widget.TextView tv = new android.widget.TextView(this);
            String label = p.part.length() > 0 ? p.part : ("第 " + p.page + " P");
            String sub = "第 " + p.page + " P" + (p.duration.length() > 0 ? " · " + p.duration : "");
            tv.setText(label + "\n" + sub);
            tv.setTextSize(13.5f);
            tv.setTextColor(0xFF3C4A60);
            tv.setLineSpacing(0, 1.2f);
            tv.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0xFFFFFFFF);
            gd.setCornerRadius(12 * d);
            gd.setStroke((int) (1 * d), 0xFFE5E9F0);
            tv.setBackground(gd);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (8 * d);
            tv.setLayoutParams(lp);

            tv.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View w) { openBiliPart(v, p.page); }
            });
            box.addView(tv);
        }

        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        box.measure(android.view.View.MeasureSpec.makeMeasureSpec(
                        getResources().getDisplayMetrics().widthPixels - (int) (48 * d),
                        android.view.View.MeasureSpec.AT_MOST),
                android.view.View.MeasureSpec.UNSPECIFIED);
        sv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                Math.min(box.getMeasuredHeight(), maxH)));

        new AlertDialog.Builder(this)
                .setTitle("选择分 P")
                .setView(sv)
                .setNegativeButton("取消", null)
                .show();
    }

    /** 打开某个视频（可选指定第几 P，page 从 1 开始；0 = 不分 P）。装了客户端就跳客户端。 */
    private void openBiliPart(Bili.Video v, int page) {
        if (installed(PKG_BILI)) {
            try {
                String uri = "bilibili://video/" + v.bvid + (page > 0 ? "?p=" + page : "");
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                i.setPackage(PKG_BILI);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return;
            } catch (Exception ignored) { }
        }
        openUrl(v.page() + (page > 0 ? "?p=" + page : ""));
    }

    private void openBiliSearch(String keyword) {
        openUrl("https://search.bilibili.com/all?keyword=" + Uri.encode(keyword));
    }

    /** 按优先级列出候选 Intent：先客户端，再网页 */
    private java.util.List<Intent> appIntents(String url) {
        java.util.List<Intent> out = new java.util.ArrayList<Intent>();
        String low = url.toLowerCase(java.util.Locale.US);

        if (low.contains("youtube.com") || low.contains("youtu.be")) {
            if (installed(PKG_YOUTUBE)) {
                String scheme = youtubeScheme(url);
                if (scheme != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
                    i.setPackage(PKG_YOUTUBE);
                    out.add(i);
                }
                // 让 YouTube 自己处理网页链接（它也声明了 youtube.com）
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i2.setPackage(PKG_YOUTUBE);
                out.add(i2);
            }
        } else if (low.contains("bilibili.com") || low.contains("b23.tv")) {
            if (installed(PKG_BILI)) {
                String scheme = biliScheme(url);
                if (scheme != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
                    i.setPackage(PKG_BILI);
                    out.add(i);
                }
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i2.setPackage(PKG_BILI);
                out.add(i2);
            }
        }
        return out;
    }

    private boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * youtube.com/watch?v=ID      -> vnd.youtube:ID
     * youtu.be/ID                 -> vnd.youtube:ID
     * 其他（搜索、频道、播放列表）-> null，交给网页链接
     */
    private String youtubeScheme(String url) {
        try {
            Uri u = Uri.parse(url);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(java.util.Locale.US);
            if (host.contains("youtu.be")) {
                String id = u.getPath();
                if (id != null && id.length() > 1) return "vnd.youtube:" + id.substring(1);
            }
            String v = u.getQueryParameter("v");
            if (v != null && v.length() > 0) return "vnd.youtube:" + v;
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * search.bilibili.com/all?keyword=X  -> bilibili://search?keyword=X
     * www.bilibili.com/video/BVxxxx      -> bilibili://video/BVxxxx
     * 其他 -> null
     */
    private String biliScheme(String url) {
        try {
            Uri u = Uri.parse(url);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(java.util.Locale.US);
            String path = u.getPath() == null ? "" : u.getPath();

            if (host.startsWith("search.")) {
                String kw = u.getQueryParameter("keyword");
                if (kw != null && kw.length() > 0) {
                    return "bilibili://search?keyword=" + Uri.encode(kw);
                }
            }
            if (path.startsWith("/video/")) {
                String bv = path.substring("/video/".length());
                if (bv.length() > 0) return "bilibili://video/" + bv;
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Override
    public void exportCsv() {
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/csv");
            i.putExtra(Intent.EXTRA_TITLE, "学习记录_" + ScheduleData.todayIso().replace("-", "") + ".csv");
            startActivityForResult(i, REQ_EXPORT);
        } catch (Exception e) {
            toast("导出失败：" + e);
        }
    }

    @Override
    public void importCsv() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            toast("打不开文件选择器：" + e);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;

        if (req == REQ_EXPORT) { writeCsv(data.getData()); return; }
        if (req != REQ_IMPORT) return;

        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            final int[] r = Store.importCsv(new String(bos.toByteArray(), "UTF-8"));
            simple("导入完成", "新增打卡：" + r[0] + " 条\n已存在：" + r[1] + " 条"
                    + (r[2] > 0 ? "\n无法识别：" + r[2] + " 行" : ""));
            if (view != null) view.invalidate();
        } catch (Exception e) {
            simple("导入失败", String.valueOf(e.getMessage()));
        }
    }

    private void writeCsv(Uri target) {
        try {
            OutputStream out = getContentResolver().openOutputStream(target);
            out.write(Store.exportCsv().getBytes("UTF-8"));
            out.close();
            toast("已导出学习记录");
        } catch (Exception e) {
            toast("写入失败：" + e);
        }
    }

    @Override
    public void showChangelog() {
        try { startActivity(new Intent(this, DocActivity.class)); }
        catch (Exception e) { toast("打不开更新日志：" + e); }
    }

    // ================================================================== 通用弹窗
    private void simple(String title, String msg) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton("好", null).show();
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show();
    }

    private void dismissBusy() {
        if (busy != null && busy.isShowing()) busy.dismiss();
        busy = null;
    }

    // ================================================================== GitHub 登录
    @Override
    public void ghLogin() {
        if (!GitHub.configured()) {
            simple("还没有配置",
                    "需要在 GitHub 注册一个 OAuth App（勾选 Enable Device Flow），"
                            + "把拿到的 Client ID 填进 GitHub.java 的 CLIENT_ID，然后重新编译 APK。\n\n"
                            + "注册地址：\nhttps://github.com/settings/applications/new");
            return;
        }
        cancelLogin = false;
        busy = new AlertDialog.Builder(this)
                .setTitle("正在向 GitHub 申请登录码…").setMessage("请稍候")
                .setCancelable(false).show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    final GitHub.DeviceCode dc = GitHub.deviceStart();
                    runOnUiThread(new Runnable() {
                        public void run() { showAuthorizeDialog(dc); }
                    });
                    String token = GitHub.devicePoll(dc, new GitHub.Cancel() {
                        public boolean cancelled() { return cancelLogin; }
                    });
                    final String login = GitHub.currentUserWith(MainActivity.this, token);
                    GitHub.saveToken(MainActivity.this, token, login);
                    runOnUiThread(new Runnable() {
                        public void run() { onLoggedIn(login); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() { onLoginError(e); }
                    });
                }
            }
        }).start();
    }

    /** 在 UI 线程上展示用户码（监听器写在方法里，嵌套只有一层） */
    private void showAuthorizeDialog(final GitHub.DeviceCode dc) {
        busy.setTitle("在浏览器里完成授权");
        busy.setMessage("代码：" + dc.userCode
                + "\n\n已复制到剪贴板。请在打开的页面粘贴它，再点绿色的 Authorize。\n\n"
                + "没自动打开就手动访问：\n" + dc.verifyUrl);
        busy.setButton(AlertDialog.BUTTON_POSITIVE, "打开浏览器",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { openVerify(dc); }
                });
        busy.setButton(AlertDialog.BUTTON_NEGATIVE, "取消",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { cancelLogin = true; }
                });
        openVerify(dc);
    }

    private void openVerify(GitHub.DeviceCode dc) {
        copyToClipboard(dc.userCode);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dc.verifyUrl)));
        } catch (Exception e) {
            toast("请手动访问 " + dc.verifyUrl);
        }
    }

    private void copyToClipboard(String s) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("device code", s));
        } catch (Exception ignored) { }
    }

    private void onLoggedIn(String login) {
        dismissBusy();
        toast("已登录 GitHub：" + login);
        if (view != null) view.invalidate();
        new AlertDialog.Builder(this)
                .setTitle("登录成功")
                .setMessage("要现在同步一次打卡记录吗？")
                .setPositiveButton("立即同步", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { ghSync(); }
                })
                .setNegativeButton("以后再说", null).show();
    }

    private void onLoginError(Exception e) {
        dismissBusy();
        simple("登录失败", String.valueOf(e.getMessage()));
    }

    @Override
    public void ghLogout() {
        new AlertDialog.Builder(this)
                .setTitle("退出 GitHub 登录？")
                .setMessage("退出后无法同步打卡记录，也不能检查更新。已经同步过的记录不受影响。")
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { doLogout(); }
                })
                .setNegativeButton("取消", null).show();
    }

    private void doLogout() {
        GitHub.logout(this);
        toast("已退出登录");
        if (view != null) view.invalidate();
    }

    // ================================================================== 同步
    @Override
    public void ghSync() {
        if (!GitHub.loggedIn(this)) { toast("请先登录 GitHub"); return; }
        busy = new AlertDialog.Builder(this)
                .setTitle("正在同步…").setMessage("与私有仓库交换打卡记录")
                .setCancelable(false).show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    final Sync.Result r = Sync.sync(MainActivity.this);
                    runOnUiThread(new Runnable() {
                        public void run() { onSynced(r); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() { onSyncError(e); }
                    });
                }
            }
        }).start();
    }

    private void onSynced(Sync.Result r) {
        dismissBusy();
        simple("同步完成", "从云端新增：" + r.pulled + " 条\n合计打卡：" + r.total + " 条\n"
                + (r.uploaded ? "已把本地记录上传到仓库" : "云端已是最新，无需上传"));
        if (view != null) view.invalidate();
    }

    private void onSyncError(Exception e) {
        dismissBusy();
        simple("同步失败", String.valueOf(e.getMessage()));
    }

    // ================================================================== 更新
    @Override
    public void ghCheckUpdate() {
        // 仓库现在是公开的，没登录也能查更新
        busy = new AlertDialog.Builder(this)
                .setTitle("正在检查更新…").setMessage("读取最新 Release")
                .setCancelable(false).show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    final GitHub.Release rel = GitHub.latestRelease(MainActivity.this);
                    runOnUiThread(new Runnable() {
                        public void run() { onUpdateChecked(rel); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() { onUpdateError(e); }
                    });
                }
            }
        }).start();
    }

    private void onUpdateChecked(GitHub.Release rel) {
        dismissBusy();
        int cmp = GitHub.compareVersion(rel.tag, GitHub.VERSION_TAG);
        if (cmp == 0) {
            simple("已是最新版本", "当前：" + GitHub.VERSION_TAG + "\n线上：" + rel.tag);
            return;
        }
        if (cmp < 0) {
            simple("本机版本比线上还新", "本机：" + GitHub.VERSION_TAG + "\n线上：" + rel.tag);
            return;
        }
        if (rel.apkUrl.length() == 0) {
            toast("这个 Release 里没有 APK 附件");
            return;
        }
        // 手动检查和自动检查共用同一个弹窗
        showUpdateDialog(rel);
    }

    private void onUpdateError(Exception e) {
        dismissBusy();
        simple("检查更新失败", String.valueOf(e.getMessage()));
    }

    /**
     * 把 Release 说明里的 Markdown 转成能直接看的纯文本。
     *
     * 以前是原样截断 400 字塞进弹窗，结果 "## 标题"、"**加粗**"、``` 围栏
     * 全都露在外面，又长又乱。这里去掉标记，保留段落和列表结构。
     */
    static String mdToPlain(String s) {
        if (s == null) return "";
        s = s.replace("\r\n", "\n").replace('\r', '\n');

        StringBuilder out = new StringBuilder();
        boolean inFence = false;
        for (String raw : s.split("\n")) {
            String t = raw.trim();
            if (t.startsWith("```") || t.startsWith("~~~")) { inFence = !inFence; continue; }
            if (inFence) { out.append("    ").append(raw).append('\n'); continue; }

            // 标题：去掉开头的 #
            int h = 0;
            while (h < t.length() && t.charAt(h) == '#') h++;
            if (h > 0 && h < t.length() && t.charAt(h) == ' ') {
                if (out.length() > 0) out.append('\n');
                out.append(t.substring(h + 1).trim()).append('\n');
                continue;
            }
            if (t.startsWith("- ") || t.startsWith("* ")) {
                out.append("· ").append(t.substring(2)).append('\n');
                continue;
            }
            if (t.startsWith("> ")) { out.append("　").append(t.substring(2)).append('\n'); continue; }
            if (t.startsWith("|")) continue;     // 表格在弹窗里没意义，丢掉
            if (t.equals("---") || t.equals("***")) { out.append('\n'); continue; }
            out.append(t).append('\n');
        }

        String r = out.toString();
        r = r.replace("**", "").replace("`", "").replace("~~", "");
        while (r.contains("\n\n\n")) r = r.replace("\n\n\n", "\n\n");
        r = r.trim();
        // 太长了也没人看，留个头
        return r.length() > 2600 ? r.substring(0, 2600) + "\n\n…（还有更多，可打开 Release 页查看）" : r;
    }

    // ------------------------------------------------------------------ 下载 APK
    private TextView dlMsg;

    private void downloadApk(final GitHub.Release rel) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                    .setTitle("需要「安装未知应用」权限")
                    .setMessage("Android 8 起安装 APK 需要单独授权。请在接下来的设置页里允许本应用安装应用，"
                            + "然后重新点一次「检查更新」。")
                    .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { openInstallSettings(); }
                    })
                    .setNegativeButton("取消", null).show();
            return;
        }

        dlMsg = new TextView(this);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        dlMsg.setPadding(p * 2, p, p * 2, p);
        dlMsg.setText("准备下载…");
        busy = new AlertDialog.Builder(this)
                .setTitle("正在下载新版本").setView(dlMsg)
                .setCancelable(false).show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    final File out = ApkProvider.fileFor(MainActivity.this,
                            "StudyCompanion-" + rel.tag + ".apk");
                    GitHub.downloadAsset(MainActivity.this,
                            rel.apkDownload(GitHub.loggedIn(MainActivity.this)), out, new GitHub.Progress() {
                        public void onProgress(long got, long total) { updateDownloadText(got, total); }
                    });
                    runOnUiThread(new Runnable() {
                        public void run() { onDownloaded(out); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() { onDownloadError(e); }
                    });
                }
            }
        }).start();
    }

    private void openInstallSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) { toast("打不开设置页"); }
    }

    /** 可能从后台线程调用，所以内部再 post 回 UI 线程 */
    private void updateDownloadText(final long got, final long total) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (dlMsg == null) return;
                dlMsg.setText(total > 0
                        ? ("已下载 " + (got / 1024) + " / " + (total / 1024) + " KB")
                        : ("已下载 " + (got / 1024) + " KB"));
            }
        });
    }

    private void onDownloaded(File apk) {
        dismissBusy();
        installApk(apk);
    }

    private void onDownloadError(Exception e) {
        dismissBusy();
        simple("下载失败", String.valueOf(e.getMessage()));
    }

    private void installApk(File apk) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(ApkProvider.uriFor(apk.getName()),
                    "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            simple("安装失败", "文件已下载到：\n" + apk.getAbsolutePath() + "\n\n" + e);
        }
    }
}
