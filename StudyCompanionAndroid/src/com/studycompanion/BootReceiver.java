package com.studycompanion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机 / 应用更新后重新排提醒 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        if (!a.equals(Intent.ACTION_BOOT_COMPLETED) && !a.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) return;
        ScheduleData.load(ctx);
        Store.init(ctx);
        if (Store.autoRemind()) Reminder.scheduleAll(ctx);
    }
}
