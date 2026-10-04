# -*- coding: utf-8 -*-
import json, sys
sys.path.insert(0, r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料")
from make_report import recs, bucket, QS, grade_int
from collections import defaultdict
by_uni = defaultdict(list)
for r in recs:
    b = bucket(r.get("subject",""))
    if b and (r.get("ify_grades") or "").strip():
        by_uni[(r.get("uni_name",""), QS.get(r.get("uni_name",""), 9999))].append((b, r))
targets = sorted([k for k in by_uni if k[1] < 9999], key=lambda k: k[1])[:22]
for u, q in targets:
    for label, want in (("CS/IT","CS/IT"), ("EEE","EEE/硬件")):
        g = [r for b, r in by_uni[(u,q)] if b == want]
        seen, rows = set(), []
        for r in sorted(g, key=lambda x: (grade_int(x.get("ify_grades","")) if grade_int(x.get("ify_grades","")) is not None else 99)):
            k = (r.get("subject",""), r.get("ify_grades",""))
            if k in seen: continue
            seen.add(k)
            rows.append("%s = %s" % (r.get("subject",""), r.get("ify_grades","")))
        if rows:
            print("### %s (QS #%d) %s" % (u, q, label))
            for x in rows[:10]:
                print("  " + x)
