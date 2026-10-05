# -*- coding: utf-8 -*-
"""修正从 chapter_goals.py 抄过来的错字（不动那个 agent 的源文件）"""
import io, sys, os
sys.stdout.reconfigure(encoding="utf-8")

ROOT = r"D:\UsrFiles\Documents\NCUK IFY Self Study"

# (路径, 错误, 正确)
TARGETS = [
    (os.path.join(ROOT, "StudyCompanion", "assets", "plan", "checks.tsv"),
     "xx²+bx+c", "ax²+bx+c"),
    (os.path.join(ROOT, "02_学习与教材", "每日执行清单_2026-10至2027-08.md"),
     "xx²+bx+c", "ax²+bx+c"),
]

for path, bad, good in TARGETS:
    if not os.path.exists(path):
        print("跳过（不存在）:", path)
        continue
    s = io.open(path, encoding="utf-8").read()
    n = s.count(bad)
    if n:
        s = s.replace(bad, good)
        io.open(path, "w", encoding="utf-8", newline="").write(s)
    print("替换 %d 处 -> %s" % (n, path))
