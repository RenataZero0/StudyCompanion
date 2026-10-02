# -*- coding: utf-8 -*-
"""用 pages.tsv（真实 PDF 页）修正 curriculum.py 中 PM2&3 的印刷页码"""
import re, sys, os, collections
sys.stdout.reconfigure(encoding="utf-8")

DATA = r"D:\UsrFiles\Documents\NCUK IFY Self Study\StudyCompanion\data"
CUR = r"D:\UsrFiles\Documents\NCUK IFY Self Study\_plan\curriculum.py"
OFFSET = 11  # PM2&3: 印刷页 + 11 = PDF 页

idx = {}
for line in open(os.path.join(DATA, "pages.tsv"), encoding="utf-8"):
    if line.startswith("#") or not line.strip():
        continue
    b, s, p = line.rstrip("\n").split("\t")
    if b == "P2/3":
        idx[s] = int(p) - OFFSET

# 对缺失的小节做线性插值，避免出现空洞
import re as _re
src0 = open(CUR, encoding="utf-8").read()
s0 = src0.index("PM23 = (")
e0 = src0.index("# ------------------------------------------------------------------- Mechanics")
all_secs = [(int(m.group(1)), int(m.group(2)))
            for m in _re.finditer(r'\("(\d+)\.(\d+)",', src0[s0:e0])]
for a in sorted({x[0] for x in all_secs}):
    bs = sorted(b for aa, b in all_secs if aa == a)
    known = [(b, idx[f"{a}.{b}"]) for b in bs if f"{a}.{b}" in idx]
    if not known:
        continue
    for b in bs:
        if f"{a}.{b}" in idx:
            continue
        lo = [x for x in known if x[0] < b]
        hi = [x for x in known if x[0] > b]
        if lo and hi:
            b1, p1 = lo[-1]
            b2, p2 = hi[0]
            v = round(p1 + (p2 - p1) * (b - b1) / (b2 - b1))
        elif lo:
            v = lo[-1][1]
        else:
            v = hi[0][1]
        idx[f"{a}.{b}"] = int(v)

print("PM2&3 修正后印刷页码：")
for s in sorted(idx, key=lambda k: [int(x) for x in k.split(".")]):
    print(f"  {s:5s} -> {idx[s]}")

src = open(CUR, encoding="utf-8").read()
start = src.index("PM23 = (")
end = src.index("# ------------------------------------------------------------------- Mechanics")
block = src[start:end]

changed = [0]
def repl(m):
    sec, mid, old = m.group(1), m.group(2), m.group(3)
    if sec in idx:
        new = idx[sec]
        if str(new) != old:
            changed[0] += 1
        return f'("{sec}",{mid}{new}),'
    return m.group(0)

newblock = re.sub(r'\("(\d+\.\d+)",(\s*"[^"]*",\s*)(\d+)\),', repl, block)
src = src[:start] + newblock + src[end:]
open(CUR, "w", encoding="utf-8").write(src)
print(f"\n已修正 {changed[0]} 处页码 -> {CUR}")
