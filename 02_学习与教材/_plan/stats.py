# -*- coding: utf-8 -*-
"""从生成的计划中导出一份覆盖情况统计，用于撰写规划文档"""
import sys, os, datetime as dt, collections, re
sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_schedule as B

p = B.Plan()
cur, end = B.d("2026-10-01"), B.d("2027-08-31")
cells, blk = {}, {}
while cur <= end:
    if cur.weekday() == 0:
        p.week_log = {k: [] for k in p.week_log}
    cells[cur] = p.render_day(cur)
    blk[cur] = p.blocks_for(cur)
    cur += dt.timedelta(days=1)

# 每月的学习小时 + 每科小时
mh = collections.Counter()
msub = collections.defaultdict(collections.Counter)
cur = start = B.d("2026-10-01")
while cur <= end:
    for t, tr in blk[cur][3]:
        hrs = 1
        if tr == "REVIEW":
            msub[(cur.year, cur.month)]["REVIEW"] += hrs
            mh[(cur.year, cur.month)] += hrs
        else:
            msub[(cur.year, cur.month)][tr] += hrs
            mh[(cur.year, cur.month)] += hrs
    cur += dt.timedelta(days=1)

print("月份     总h  纯数 物理 应用 进阶 EAP 复习")
tot = collections.Counter()
for k in sorted(mh):
    s = msub[k]
    print(f"{k[0]}-{k[1]:02d}  {mh[k]:4d}  {s['PURE']:4d} {s['PHY']:4d} {s['APPLIED']:4d} "
          f"{s['FURTHER']:4d} {s['EAP']:3d} {s['REVIEW']:4d}")
    for kk, vv in s.items():
        tot[kk] += vv
print("合计    ", sum(tot.values()), dict(tot))

# 每科完成的教材章节（按日期）
print("\n=== 各科覆盖进度 ===")
for track in ["PURE", "PHY", "APPLIED", "FURTHER"]:
    q = B.QUEUES[track]
    n = p.cursor[track]
    if n == 0:
        continue
    def code_of(t):
        m = re.search(r"([A-Za-z0-9/]+) (Ch\d+|P1)", t)
        return m.group(0) if m else t
    print(f"{track}: {n}/{len(q)} 节")
    print("   首:", q[0]["track_title"])
    print("   末:", q[n - 1]["track_title"])
print("EAP:", p.used["EAP"], "/", len(B.EAP_QUEUE), "末:", B.EAP_QUEUE[p.cursor['EAP'] - 1]['track_title'])

# 里程碑
print("\n=== 里程碑 ===")
for k in sorted(B.MILESTONES):
    print(" ", k, B.MILESTONES[k])

# 假期段
print("\n=== 假期 ===")
for a, b, n, kd in B.HOLIDAYS:
    print(f"  {a} ~ {b}  {n} ({kd})")
