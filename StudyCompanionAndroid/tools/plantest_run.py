# -*- coding: utf-8 -*-
"""从 Schedule.xlsx 抽几个真实时段，喂给桌面上编译好的真 DailyPlan 跑一遍"""
import io, os, re, sys, subprocess
sys.stdout.reconfigure(encoding="utf-8")
import openpyxl

WS = r"D:\UsrFiles\Documents\NCUK IFY Self Study\StudyCompanion"
T = os.path.join(os.environ["TEMP"], "plantest")
XLSX = r"D:\UsrFiles\Documents\NCUK IFY Self Study\02_学习与教材\Schedule.xlsx"

DAY_RE = re.compile(r"^(\d{1,2})月(\d{1,2})日\s*周([一二三四五六日])")
TIME_RE = re.compile(r"^(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})\s+(.+)$")
DASH_RE = re.compile(r"^[·•]\s*(.*)$")

wb = openpyxl.load_workbook(XLSX, data_only=True)
want_months = {"202610"}
slots = []          # (iso, start, end, subject, [body...])
for name in wb.sheetnames:
    if name not in want_months:
        continue
    sh = wb[name]
    for row in sh.iter_rows(values_only=True):
        for cell in row:
            if not isinstance(cell, str) or "月" not in cell:
                continue
            lines = [l.strip() for l in cell.split("\n")]
            m = DAY_RE.match(lines[0]) if lines else None
            if not m:
                continue
            mon, day = int(m.group(1)), int(m.group(2))
            iso = "%s-%02d-%02d" % (name[:4], mon, day)
            cur = None
            for ln in lines[1:]:
                if not ln or set(ln) <= set("─—-"):
                    continue
                tm = TIME_RE.match(ln)
                if tm:
                    if cur:
                        slots.append(cur)
                    cur = [iso, tm.group(1), tm.group(2), tm.group(3), []]
                    continue
                if cur is None:
                    continue
                cur[4].append(ln)
            if cur:
                slots.append(cur)
                cur = None

want_days = ["2026-10-03", "2026-10-17", "2026-10-12",
             "2026-10-09", "2026-10-13", "2026-11-04"]
sel = [s for s in slots if s[0] in want_days]

# 写测试输入：start \t end \t subject \t body0 \t body1 ...
inp = []
for s in sel:
    body = s[4]
    if not body:
        continue
    inp.append("\t".join([s[1], s[2], s[3]] + body))

infile = os.path.join(T, "slots.tsv")
io.open(infile, "w", encoding="utf-8", newline="").write("\n".join(inp) + "\n")
print("抽到 %d 个时段（%s）" % (len(inp), "、".join(want_days)))

JDK = r"D:\Program Files\Java\jdk-21"
out = os.path.join(T, "out")
assets = os.path.join(T, "assets")
r = subprocess.run([os.path.join(JDK, "bin", "java.exe"),
                    "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                    "-cp", out, "Test", assets],
                   stdin=io.open(infile, "rb"), capture_output=True)
txt = r.stdout.decode("utf-8", "replace")
io.open(os.path.join(T, "steps.txt"), "w", encoding="utf-8", newline="").write(txt)
print("java exit =", r.returncode)
if r.stderr:
    print(r.stderr.decode("utf-8", "replace")[:800])
print("输出 ->", os.path.join(T, "steps.txt"), " 共", len(txt.splitlines()), "行")

# ---- 时间轴自检：每个时段里所有步骤的分钟数必须正好等于时段长度 ----
HDR = re.compile(r"^###\s+(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})\s+(.*)$")
MIN = re.compile(r"^\d{1,2}:\d{2}\s+\((\d+)[′']\)")
dur = None
tot = 0
bad = 0
slots_seen = 0
for line in txt.splitlines():
    h = HDR.match(line)
    if h:
        if dur is not None:
            slots_seen += 1
            if tot != dur:
                bad += 1
                print("  ! 时间轴不匹配：%s 应 %d′ 实 %d′" % (cur, dur, tot))
        dur = (int(h.group(3)) * 60 + int(h.group(4))) - (int(h.group(1)) * 60 + int(h.group(2)))
        cur = h.group(5)
        tot = 0
        continue
    m = MIN.match(line.strip())
    if m and dur is not None:
        tot += int(m.group(1))
if dur is not None:
    slots_seen += 1
    if tot != dur:
        bad += 1
        print("  ! 时间轴不匹配：%s 应 %d′ 实 %d′" % (cur, dur, tot))
print("时间轴自检：时段 %d 个，不匹配 %d 个" % (slots_seen, bad))

# ---- 正文自检：步骤徽标上的分钟数不能和同一行正文里写的数字打架 ----
# 正文里写「（约 25 分钟，p.4）」而徽标显示 18′ —— 这正是 v2.1.20 要根除的毛病。
# 取值优先级要和 DailyPlan.statedMinutes 一致：
# 「共 M 分钟」→ 「每节 N 分钟」×节数 → 第一个「N 分钟 / N′」。
SAID = re.compile(r"(\d+)\s*(?:分钟|[′'])")
TOTAL = re.compile(r"共\s*(\d+)\s*分钟")
PER = re.compile(r"(?:每节|各|每个|每小块)\s*(\d+)\s*分钟")
SEC = re.compile(r"§\s*\d+\.\d+")
LABEL_END = "："


def label_seg(body):
    # 只看第一个中文冒号之前的「标签段」，和 DailyPlan.labelOf 一致。
    # 冒号后面的数字是描述（讲满 2 分钟、5–8 分钟讲座），不是这一步的时长。
    i = body.find(LABEL_END)
    return body[:i] if i > 0 else body


clash = 0
for line in txt.splitlines():
    m = MIN.match(line.strip())
    if not m:
        continue
    badge = int(m.group(1))
    body = line.strip()[m.end():]
    seg = label_seg(body)
    tot = TOTAL.search(seg)
    per = PER.search(seg)
    if tot:
        said = int(tot.group(1))
    elif per:
        n = len(set(SEC.findall(seg)))
        said = int(per.group(1)) * n if n >= 1 else int(per.group(1))
    else:
        g = SAID.search(seg)
        said = int(g.group(1)) if g else badge
    if said != badge:
        clash += 1
        if clash <= 5:
            print("  ! 正文与分钟数打架：徽标 %d′，正文写 %d′ —— %s"
                  % (badge, said, body[:70]))
print("正文与分钟数自检：打架 %d 处" % clash)

# ---- 时钟自检：每一步的开始时间必须等于上一步的结束时间，且正好收在时段末尾 ----
# fitTail 改过分钟数之后如果忘了重排时钟，就会出现「20:05 起、28 分钟」
# 的下一步却写着 20:32（该是 20:33）这种错位。
FOOT = re.compile(r"^\s*(\d{1,2}):(\d{2})\s+\((\d+)[′']\)")
clk_bad = tail_bad = per_bad = 0
start = dur = None
expect = 0
for line in txt.splitlines():
    h = HDR.match(line)
    if h:
        if start is not None and expect != start + dur:
            tail_bad += 1
            if tail_bad <= 5:
                print("  ! 没收到时段末尾：%s 收在 %02d:%02d，应 %02d:%02d"
                      % (cur, expect // 60, expect % 60, (start + dur) // 60, (start + dur) % 60))
        start = int(h.group(1)) * 60 + int(h.group(2))
        dur = (int(h.group(3)) * 60 + int(h.group(4))) - start
        expect = start
        cur = h.group(5)
        continue
    if start is None:
        continue
    f = FOOT.match(line)
    if not f:
        continue
    t = int(f.group(1)) * 60 + int(f.group(2))
    badge = int(f.group(3))
    if t != expect:
        clk_bad += 1
        if clk_bad <= 5:
            print("  ! 时钟错位：%s 该 %02d:%02d 却写 %02d:%02d"
                  % (cur, expect // 60, expect % 60, t // 60, t % 60))
        expect = t
    expect += badge
    # 「每节 N 分钟」× 节数：正文没写「共 M 分钟」时，徽标必须等于 N×节数
    body = line.strip()[f.end():]
    seg = label_seg(body)
    tot = TOTAL.search(seg)
    p = PER.search(seg)
    if p and not tot:
        cnt = len(set(SEC.findall(seg)))
        if cnt >= 1 and int(p.group(1)) * cnt != badge:
            per_bad += 1
            if per_bad <= 5:
                print("  ! 「每节 %s 分钟」×%d 节 ≠ 徽标 %d′ —— %s"
                      % (p.group(1), cnt, badge, body[:56]))
if start is not None and expect != start + dur:
    tail_bad += 1
    print("  ! 没收到时段末尾：%s 收在 %02d:%02d，应 %02d:%02d"
          % (cur, expect // 60, expect % 60, (start + dur) // 60, (start + dur) % 60))
print("时钟自检：错位 %d 处，未收尾 %d 处；「每节 N 分钟」不符 %d 处"
      % (clk_bad, tail_bad, per_bad))
