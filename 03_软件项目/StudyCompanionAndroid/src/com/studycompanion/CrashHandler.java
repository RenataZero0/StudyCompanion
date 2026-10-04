package com.studycompanion;

import android.content.Context;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 崩溃日志捕获。
 *
 * 安卓端出问题时，光看「闪退」两个字没法定位 —— 所以装上这个：
 * 一旦未捕获异常，把完整堆栈写到文件；下次启动时弹出来，可以直接复制/发送。
 *
 * 文件位置：/sdcard/Android/data/com.studycompanion/files/crash.txt
 * （手机文件管理器或插数据线都能看到；卸载应用会一起删掉）
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String FILE = "crash.txt";
    private final Context app;
    private final Thread.UncaughtExceptionHandler prev;

    public CrashHandler(Context ctx, Thread.UncaughtExceptionHandler previous) {
        app = ctx.getApplicationContext();
        prev = previous;
    }

    /** 在 Application / 第一个 Activity 里尽早调用一次 */
    public static void install(Context ctx) {
        Thread.UncaughtExceptionHandler cur = Thread.getDefaultUncaughtExceptionHandler();
        if (cur instanceof CrashHandler) return;
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(ctx, cur));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try { write(app, e); } catch (Throwable ignored) { }
        if (prev != null) prev.uncaughtException(t, e);
    }

    static File file(Context c) {
        File dir = c.getExternalFilesDir(null);
        if (dir == null) dir = c.getFilesDir();
        return new File(dir, FILE);
    }

    static void write(Context c, Throwable e) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("时间 : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        pw.println("设备 : " + Build.MANUFACTURER + " " + Build.MODEL
                + "   Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        pw.println("版本 : " + GitHub.VERSION_TAG);
        pw.println();
        e.printStackTrace(pw);
        pw.flush();

        FileOutputStream fos = new FileOutputStream(file(c));
        fos.write(sw.toString().getBytes("UTF-8"));
        fos.close();
    }

    /** 上次崩溃留下的记录；没有返回 null */
    public static String last(Context c) {
        try {
            File f = file(c);
            if (!f.exists()) return null;
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            String s = new String(bos.toByteArray(), "UTF-8");
            return s.length() == 0 ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear(Context c) {
        try { File f = file(c); if (f.exists()) f.delete(); } catch (Exception ignored) { }
    }
}
