# -*- coding: utf-8 -*-
"""按月份报告每条轨道实际推进到哪一章，用于校对规划文档"""
import sys, os, datetime as dt, collections, re
sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_schedule as B

p = B.Plan()
cur, end = B.d("2026-10-01"), B.d("2027-08-31")
month_items = collections.defaultdict(lambda: collections.defaultdict(list))

def take_month(month, f):
    before = {k: p.cursor[k] for k in list(B.QUEUES) + ["EAP"]}
    f()
    for k in before:
        if p.cursor[k] > before[k]:
            q = B.EAP_QUEUE if k == "EAP" else B.QUEUES[k]
            for i in range(before[k], p.cursor[k]):
                month_items[month][k].append(q[i]["track_title"])

while cur <= end:
    if cur.weekday() == 0:
        p.week_log = {k: [] for k in p.week_log}
    take_month((cur.year, cur.month), lambda c=cur: p.render_day(c))
    cur += dt.timedelta(days=1)

def span(month, key):
    its = month_items[month].get(key, [])
    if not its:
        return "-"
    def c(t):
        m = re.search(r"([A-Za-z0-9/]+ (?:Ch\d+|P1|P1\.\d+))", t)
        return m.group(1) if m else t
    a, b = c(its[0]), c(its[-1])
    return a if a == b else f"{a} → {b}"

for m in sorted(month_items):
    print(f"--- {m[0]}-{m[1]:02d} ---")
    for k in ["PURE", "PHY", "APPLIED", "FURTHER", "EAP"]:
        if month_items[m].get(k):
            print(f"   {k:8s} {len(month_items[m][k]):3d} 节  {span(m, k)}")
