package com.studycompanion;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 打卡记录云端同步。
 *
 * 仓库里存 sync/progress.txt，一行一个「日期|时段」。
 * 合并策略是**并集**：手机和电脑各自标完成，同步后两边的记录都保留 —— 不会互相覆盖。
 */
public class Sync {

    public static class Result {
        public int pulled = 0;      // 从云端拉下来的新记录
        public int total = 0;       // 合并后总数
        public boolean uploaded = false;
        public String time = "";
    }

    static Set<String> parse(String text) {
        Set<String> out = new HashSet<String>();
        if (text == null) return out;
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String k = line.trim();
            if (k.length() == 0 || k.startsWith("#")) continue;
            out.add(k);
        }
        return out;
    }

    static String dump(Set<String> keys) {
        List<String> list = new ArrayList<String>(keys);
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String k : list) sb.append(k).append('\n');
        return sb.toString();
    }

    public static Result sync(Context c) throws Exception {
        Result r = new Result();
        String remoteText = GitHub.readFile(c, GitHub.SYNC_PATH);
        Set<String> remote = parse(remoteText);

        Set<String> local = Store.done();
        Set<String> merged = new HashSet<String>(local);
        for (String k : remote) if (merged.add(k)) r.pulled++;

        Store.mergeKeys(merged);
        r.total = merged.size();

        // 只有内容真的不一样才写回去，避免无意义的提交
        if (!remote.equals(merged)) {
            GitHub.writeFile(c, GitHub.SYNC_PATH, dump(merged), "同步学习记录");
            r.uploaded = true;
        }
        r.time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        return r;
    }
}
