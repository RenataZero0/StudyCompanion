# -*- coding: utf-8 -*-
"""
在桌面上验证 DailyPlan.java 的真实逻辑。

做法：写最小的 android.* 桩类 + 一个 ScheduleData.Slot 桩，
把**真实的 DailyPlan.java**原样拷进来编译运行 ——
测的是真代码，不是另写一份复制品。
"""
import io, os, sys, shutil, subprocess, json
sys.stdout.reconfigure(encoding="utf-8")

WS = r"D:\UsrFiles\Documents\NCUK IFY Self Study\StudyCompanion"
T = os.path.join(os.environ["TEMP"], "plantest")
if os.path.isdir(T):
    shutil.rmtree(T, ignore_errors=True)
os.makedirs(os.path.join(T, "android", "content", "res"))
os.makedirs(os.path.join(T, "com", "studycompanion"))
os.makedirs(os.path.join(T, "assets"))

# 资产：真实的三张表。源在仓库根的 assets/plan/，放进台架时摊平到 assets/ 根目录
# （和 APK 里一致）
for f in ("exercises.tsv", "checks.tsv", "books.tsv"):
    shutil.copy(os.path.join(WS, "assets", "plan", f),
                os.path.join(T, "assets", f))

def w(rel, text):
    p = os.path.join(T, rel)
    io.open(p, "w", encoding="utf-8", newline="").write(text)

# ---------------- android 桩 ----------------
w("android/content/res/AssetManager.java", """package android.content.res;
import java.io.*;
public class AssetManager {
    public static String base = ".";
    public InputStream open(String name) throws IOException {
        return new FileInputStream(new File(base, name));
    }
}
""")

w("android/content/SharedPreferences.java", """package android.content;
import java.util.*;
public interface SharedPreferences {
    boolean getBoolean(String k, boolean d);
    Editor edit();
    public interface Editor {
        Editor putBoolean(String k, boolean v);
        Editor remove(String k);
        void apply();
    }
    public static class Mem implements SharedPreferences {
        public final Map<String, Boolean> m = new HashMap<String, Boolean>();
        public boolean getBoolean(String k, boolean d) { Boolean v = m.get(k); return v == null ? d : v; }
        public Editor edit() {
            return new Editor() {
                public Editor putBoolean(String k, boolean v) { m.put(k, v); return this; }
                public Editor remove(String k) { m.remove(k); return this; }
                public void apply() { }
            };
        }
    }
}
""")

w("android/content/Context.java", """package android.content;
import android.content.res.AssetManager;
public class Context {
    public final SharedPreferences.Mem m = new SharedPreferences.Mem();
    public AssetManager getAssets() { return new AssetManager(); }
    public SharedPreferences getSharedPreferences(String n, int mode) { return m; }
    public static final int MODE_PRIVATE = 0;
}
""")

# ---------------- ScheduleData.Slot 桩（字段与真身一致） ----------------
w("com/studycompanion/ScheduleData.java", """package com.studycompanion;
import java.util.*;
public class ScheduleData {
    public static class Slot {
        public String start = "", end = "", subject = "";
        public final List<String> body = new ArrayList<String>();
        public String title() { return body.isEmpty() ? "" : body.get(0); }
        public String key() { return start + "-" + end; }
        public int startMin() { return toMin(start); }
        public int endMin() { return toMin(end); }
        public int duration() { return Math.max(0, endMin() - startMin()); }
        static int toMin(String hhmm) {
            try {
                String[] p = hhmm.split(":");
                return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
            } catch (Exception e) { return 0; }
        }
    }
}
""")

# 真实的 DailyPlan.java
shutil.copy(os.path.join(WS, "StudyCompanionAndroid", "src", "com", "studycompanion", "DailyPlan.java"),
            os.path.join(T, "com", "studycompanion", "DailyPlan.java"))

# ---------------- 测试驱动 ----------------
w("Test.java", """import android.content.Context;
import android.content.res.AssetManager;
import com.studycompanion.DailyPlan;
import com.studycompanion.ScheduleData;
import java.util.*;

public class Test {
    public static void main(String[] a) throws Exception {
        AssetManager.base = a[0];
        Context c = new Context();
        // 从标准输入读时段：一行一个，字段用 \\t 分，第一个是 body[0]（标题），后面是明细
        Scanner sc = new Scanner(System.in, "UTF-8");
        while (sc.hasNextLine()) {
            String ln = sc.nextLine();
            if (ln.trim().length() == 0) continue;
            String[] f = ln.split("\\t", -1);
            ScheduleData.Slot s = new ScheduleData.Slot();
            s.start = f[0]; s.end = f[1]; s.subject = f[2];
            for (int i = 3; i < f.length; i++) s.body.add(f[i]);
            List<DailyPlan.Step> st = DailyPlan.steps(c, s);
            System.out.println("### " + s.start + "-" + s.end + "  " + s.subject);
            for (DailyPlan.Step x : st) {
                if (x.head) { System.out.println("  [小节] " + x.text); continue; }
                if (x.untimed) { System.out.println("  \\u2014\\u2014       " + x.text); continue; }
                System.out.println("  " + x.time + " (" + x.minutes + "\\u2032) " + x.text);
                if (x.note.length() > 0)
                    for (String n : x.note.split("\\n")) System.out.println("        . " + n);
            }
            System.out.println();
        }
    }
}
""")

JDK = r"D:\Program Files\Java\jdk-21"
srcs = []
for root, dirs, files in os.walk(T):
    for f in files:
        if f.endswith(".java"):
            srcs.append(os.path.join(root, f))
out = os.path.join(T, "out")
os.makedirs(out)
r = subprocess.run([os.path.join(JDK, "bin", "javac.exe"), "-encoding", "UTF-8", "-d", out] + srcs,
                   capture_output=True, text=True)
print("javac:", r.returncode)
if r.returncode != 0:
    print(r.stdout); print(r.stderr); sys.exit(1)
print("桩类 + 真实 DailyPlan.java 编译通过 ->", out)
io.open(os.path.join(T, "compile_ok.txt"), "w", encoding="utf-8").write(out)
