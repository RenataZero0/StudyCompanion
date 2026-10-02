package com.studycompanion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Store.autoRemind()) return;
        ScheduleData.load(ctx);
        Store.init(ctx);
        String title = intent.getStringExtra(Reminder.EXTRA_TITLE);
        String body = intent.getStringExtra(Reminder.EXTRA_BODY);
        if (title == null) title = "该学习啦！";
        if (body == null) body = "打开学习助手看看今天要做什么。";
        Reminder.notifyNow(ctx, title, body);
        Reminder.scheduleAll(ctx);        // 顺手把窗口往后推
    }
}
