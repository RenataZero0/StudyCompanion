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

want_days = ["2026-10-03", "2026-10-17", "2026-10-12"]
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
