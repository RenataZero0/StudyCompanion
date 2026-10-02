package com.studycompanion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements MainView.Listener {

    private static final int REQ_IMPORT = 1001;
    private static final int REQ_EXPORT = 1002;
    private MainView view;

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

    // ---------------------------------------------------------------- 回调
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
            int[] r = Store.importCsv(new String(bos.toByteArray(), "UTF-8"));
            new AlertDialog.Builder(this)
                    .setTitle("导入完成")
                    .setMessage("新增打卡：" + r[0] + " 条\n已存在：" + r[1] + " 条"
                            + (r[2] > 0 ? "\n无法识别：" + r[2] + " 行" : ""))
                    .setPositiveButton("好", null)
                    .show();
            if (view != null) view.invalidate();
        } catch (Exception e) {
            toast("导入失败：" + e);
        }
    }

    @Override
    public void showChangelog() {
        try { startActivity(new Intent(this, DocActivity.class)); }
        catch (Exception e) { toast("打不开更新日志：" + e); }
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show();
    }
}
