# -*- coding: utf-8 -*-
"""为每本课本建立 小节号 -> PDF 物理页 索引，生成 data/pages.tsv 与 data/books.tsv"""
import os, re, sys, subprocess
sys.stdout.reconfigure(encoding="utf-8")

BASE = r"D:\UsrFiles\Documents\NCUK IFY Self Study\StudyCompanion\data"
TXT = os.path.join(BASE, "_pdftext")
os.makedirs(TXT, exist_ok=True)

BOOKS = {
    "P1":   (r"D:\BaiduNetdiskDownload\Mathematics\Pure Mathematics\Mathematics-Pure Mathematics 1-Coursebook.pdf", "Pure Mathematics 1"),
    "P2/3": (r"D:\BaiduNetdiskDownload\Mathematics\Pure Mathematics\Mathematics-Pure Mathematics 2 & 3-Coursebook.pdf", "Pure Mathematics 2 & 3"),
    "M1":   (r"D:\BaiduNetdiskDownload\Mathematics\Mechanics\Mathematics-Mechanics-Coursebook.pdf", "Mechanics"),
    "S1":   (r"D:\BaiduNetdiskDownload\Mathematics\Probability & Statistics\Mathematics-Probability&Statistics 1-Coursebook.pdf", "Probability & Statistics 1"),
    "FM":   (r"D:\BaiduNetdiskDownload\Mathematics\Further Mathematics\Further Mathematics-Coursebook.pdf", "Further Mathematics"),
    "Phy":  (r"D:\BaiduNetdiskDownload\Physics\Cambridge International AS & A Level Physics 3rd Edition Coursebook.pdf", "Physics"),
}
SCAN_OFFSET = {"M1": 13, "FM": 13}   # 扫描版：PDF 页 = 印刷页 + 偏移
FRONT = 12                            # 前 12 页一律视为封面/目录

SEC = re.compile(r"^(\d{1,2})\.(\d{1,2})\s+(.*)$")
MAXCH = {"P1": 9, "P2/3": 11, "S1": 8, "Phy": 31}


def heading_text(line):
    """取小节号后面的标题文字（遇到 3 个以上连续空格即截断），用于判断是否真是标题行"""
    m = SEC.match(line)
    if not m:
        return None
    tail = m.group(3)
    tail = re.split(r"\s{3,}", tail)[0].strip()
    return tail


def cached_text(code, path):
    c = os.path.join(TXT, code.replace("/", "_") + ".txt")
    if not os.path.exists(c) or os.path.getsize(c) < 1000:
        subprocess.run(["pdftotext", "-enc", "UTF-8", "-layout", path, c],
                       check=False, capture_output=True)
    return open(c, encoding="utf-8", errors="replace").read().split("\f")


rows = []
for code, (path, name) in BOOKS.items():
    if code in SCAN_OFFSET:
        continue
    pages = cached_text(code, path)
    found = {}
    last_page = 0
    max_chapter = 0
    last_b = {}
    maxch = MAXCH.get(code, 40)
    for i, pg in enumerate(pages):
        pno = i + 1
        if pno <= FRONT or pno < last_page:
            continue
        for line in pg.split("\n"):
            s = line.strip()
            m = SEC.match(s)
            if not m:
                continue
            tail = heading_text(s)
            if tail is None or len(tail) < 2 or len(tail) > 60:
                continue
            a, b = int(m.group(1)), int(m.group(2))
            if a > maxch or b < 1 or b > 40 or a < max_chapter or a < 1:
                continue
            if a == max_chapter and b <= last_b.get(a, -1):
                continue
            key = f"{a}.{b}"
            if key in found:
                continue
            if a > max_chapter:
                max_chapter = a
            last_b[a] = b
            found[key] = pno
            last_page = max(last_page, pno)
    # 同章节内页码必须单调递增，倒退的视为误匹配丢弃
    clean = {}
    for a in sorted({int(k.split(".")[0]) for k in found}):
        items = sorted((int(k.split(".")[1]), v) for k, v in found.items()
                       if int(k.split(".")[0]) == a)
        prev = -1
        for b, p in items:
            if p < prev:
                continue
            clean[f"{a}.{b}"] = p
            prev = p
    found = clean

    for k, p in sorted(found.items(), key=lambda kv: [int(x) for x in kv[0].split(".")]):
        rows.append((code, k, p))
    print(f"{code:5s} 索引 {len(found):3d} 条  样例: " +
          ", ".join(f"{k}->p{p}" for k, p in list(sorted(found.items(), key=lambda kv: [int(x) for x in kv[0].split('.')]))[:5]))

with open(os.path.join(BASE, "pages.tsv"), "w", encoding="utf-8") as f:
    f.write("# book\tsection\tpdf_page\n")
    for r in rows:
        f.write(f"{r[0]}\t{r[1]}\t{r[2]}\n")

with open(os.path.join(BASE, "books.tsv"), "w", encoding="utf-8") as f:
    f.write("# code\tname\tpdf_path\toffset\n")
    for code, (path, name) in BOOKS.items():
        f.write(f"{code}\t{name}\t{path}\t{SCAN_OFFSET.get(code,0)}\n")

print("written pages.tsv:", len(rows), "rows;  books.tsv: 6 rows")
