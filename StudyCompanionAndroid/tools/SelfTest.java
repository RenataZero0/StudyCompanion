import com.studycompanion.Links;
import com.studycompanion.ScheduleData;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * 电脑端自检：直接跑 APK 里同一份解析代码，验证课表读得对、链接映射对。
 * 编译时需要 android.jar 只是为了解析 import，实际执行的都是纯 Java 代码。
 *
 *   javac -cp android.jar -d classes src/**\/*.java tools/SelfTest.java
 *   java  -cp "android.jar;classes" SelfTest <Schedule.xlsx> [yyyy-MM-dd]
 */
public class SelfTest {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "assets/Schedule.xlsx";
        String day = args.length > 1 ? args[1] : ScheduleData.todayIso();

        byte[] data = Files.readAllBytes(new File(path).toPath());
        ScheduleData.loadFromBytes(data);

        StringBuilder sb = new StringBuilder();
        sb.append("=== 课表解析 ===\n");
        sb.append("  错误      : ").append(ScheduleData.lastError.isEmpty() ? "无" : ScheduleData.lastError).append('\n');
        sb.append("  天数      : ").append(ScheduleData.dayCount()).append('\n');
        sb.append("  范围      : ").append(ScheduleData.firstDate()).append(" ~ ").append(ScheduleData.lastDate()).append('\n');

        sb.append("\n=== ").append(day).append(" ===\n");
        ScheduleData.DayPlan dp = ScheduleData.getDay(day);
        if (dp == null) {
            sb.append("  没有安排\n");
        } else {
            sb.append("  ").append(dp.header).append("  [").append(dp.dayKind()).append("]\n");
            for (ScheduleData.Slot s : dp.slots) {
                sb.append("  [").append(s.key()).append("] ").append(s.subject).append(" -> ").append(s.title()).append('\n');
                String book = Links.bookTitleForSlot(s);
                sb.append("      BOOK  ").append(book.isEmpty() ? "（无需课本）" : book).append('\n');
                for (String[] l : Links.forSlot(s))
                    sb.append("      LINK  ").append(l[0]).append("  =>  ").append(l[1]).append('\n');
            }
        }

        sb.append("\n=== 抽查若干日期 ===\n");
        String[] probe = {"2026-10-01", "2026-10-08", "2026-10-10", "2026-10-17", "2026-10-18",
                "2027-02-08", "2027-03-29", "2027-08-30"};
        for (String p : probe) {
            ScheduleData.DayPlan d = ScheduleData.getDay(p);
            if (d == null) { sb.append("  ").append(p).append("  -> 无\n"); continue; }
            StringBuilder t = new StringBuilder();
            for (ScheduleData.Slot s : d.slots) t.append(s.start).append("-").append(s.end).append(' ').append(s.subject).append(" / ");
            sb.append("  ").append(p).append("  [").append(d.dayKind()).append("]  ")
              .append(d.slots.size()).append(" 段  ").append(t).append('\n');
        }

        sb.append("\n=== 链接表抽查 ===\n");
        String[] keys = {"P1 Ch1", "P2/3 Ch11", "M1 Ch4", "S1 Ch8", "FM Ch15", "Phy Ch16", "Phy Ch21", "Phy P1", "EAP"};
        for (String k : keys) {
            ScheduleData.Slot fake = new ScheduleData.Slot();
            fake.subject = "纯数";
            fake.body.add(k.replace(" ", " ") + " 测试");
            List<String[]> l = Links.forSlot(fake);
            sb.append("  ").append(k).append(" -> ").append(l.size()).append(" 条链接");
            if (!l.isEmpty()) sb.append("  例：").append(l.get(0)[0]);
            sb.append('\n');
        }

        String out = sb.toString();
        System.out.print(out);
        Files.write(new File("selftest.txt").toPath(), out.getBytes("UTF-8"));
    }
}
