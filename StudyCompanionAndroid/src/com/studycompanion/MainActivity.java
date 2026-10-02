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

    // ================================================================== CSV
    @Override
    public void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            toast("打不开这个链接：" + url);
        }
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
        if (!GitHub.loggedIn(this)) { toast("请先登录 GitHub"); return; }
        busy = new AlertDialog.Builder(this)
                .setTitle("正在检查更新…").setMessage("读取私有仓库的最新 Release")
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
        if (rel.tag.equalsIgnoreCase(GitHub.VERSION_TAG)) {
            simple("已是最新版本", "当前：" + GitHub.VERSION_TAG + "\n最新：" + rel.tag);
            return;
        }
        if (rel.apkUrl.length() == 0) {
            toast("这个 Release 里没有 APK 附件");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + rel.tag)
                .setMessage("当前：" + GitHub.VERSION_TAG
                        + "\n大小：" + (rel.apkSize / 1024) + " KB\n\n" + head(rel.notes))
                .setPositiveButton("下载并安装", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { downloadApk(rel); }
                })
                .setNeutralButton("打开 Release 页", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { openUrl(rel.pageUrl); }
                })
                .setNegativeButton("取消", null).show();
    }

    private void onUpdateError(Exception e) {
        dismissBusy();
        simple("检查更新失败", String.valueOf(e.getMessage()));
    }

    static String head(String s) {
        if (s == null) return "";
        s = s.replace("\r", "");
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
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
                    GitHub.downloadAsset(MainActivity.this, rel.apkUrl, out, new GitHub.Progress() {
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
