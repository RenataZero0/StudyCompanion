package com.studycompanion;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** 到点提醒：用 AlarmManager 精确排程 + 通知栏提醒 */
public class Reminder {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String CHANNEL_ID = "study_reminder";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "学习提醒", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("到学习时间提醒你开始，以及结束前 5 分钟的提示");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    /** 把未来 7 天所有时段的「开始」和「结束前 5 分钟」排进闹钟 */
    public static void scheduleAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        ensureChannel(ctx);

        // 先清掉旧的（requestCode 由天数+时段序号决定，直接覆盖即可）
        String today = ScheduleData.todayIso();
        Calendar now = Calendar.getInstance();

        for (int d = 0; d < 7; d++) {
            String iso = ScheduleData.shiftIso(today, d);
            ScheduleData.DayPlan dp = ScheduleData.getDay(iso);
            if (dp == null) continue;
            for (int i = 0; i < dp.slots.size(); i++) {
                ScheduleData.Slot s = dp.slots.get(i);
                Calendar start = at(iso, s.startMin());
                Calendar end = at(iso, s.endMin());
                if (start.after(now)) {
                    set(am, ctx, start.getTimeInMillis(), d * 100 + i * 2,
                            "该学习啦！　" + s.start + "–" + s.end,
                            s.subject + "　" + s.title() + "　点开看看今天要做什么。");
                }
                if (end.after(now)) {
                    set(am, ctx, end.getTimeInMillis() - 5 * 60 * 1000, d * 100 + i * 2 + 1,
                            "还有 5 分钟：" + s.subject,
                            "本时段 " + s.end + " 结束，完成后记得打卡。");
                }
            }
        }
    }

    private static Calendar at(String iso, int minutes) {
        Calendar c = Calendar.getInstance();
        c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                Integer.parseInt(iso.substring(8, 10)), minutes / 60, minutes % 60, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static void set(AlarmManager am, Context ctx, long when, int code, String title, String body) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_BODY, body);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i, flags);
        try {
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (SecurityException e) {
            // 没拿到「精确闹钟」权限时退回不精确的，晚几分钟总比不提醒好
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int code = 0; code < 700; code++) {
            Intent i = new Intent(ctx, ReminderReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i, flags);
            am.cancel(pi);
        }
    }

    public static void notifyNow(Context ctx, String title, String body) {
        ensureChannel(ctx);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) b = new Notification.Builder(ctx, CHANNEL_ID);
        else b = new Notification.Builder(ctx);

        b.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setPriority(Notification.PRIORITY_HIGH);
            b.setDefaults(Notification.DEFAULT_ALL);
        }
        nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
    }
}
