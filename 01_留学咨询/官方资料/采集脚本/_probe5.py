# -*- coding: utf-8 -*-
import json, io, re
p = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = json.load(io.open(p, encoding="utf-8"))
recs = data if isinstance(data, list) else data.get("records", data)
eap = [r for r in recs if str(r.get("eap","")).strip()]
print("records with eap:", len(eap))
import collections
c = collections.Counter(str(r.get("eap",""))[:120] for r in eap)
for k,v in c.most_common(12): print("  %4d  %s" % (v,k))
print()
print("sample programmes field:", json.dumps(recs[0].get("programme"), ensure_ascii=False)[:300])
print("sample all_programmes:", json.dumps(recs[0].get("all_programmes"), ensure_ascii=False)[:300])
print()
# QS top-200 unis, EEE+CS reach table
import sys
sys.path.insert(0, r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料")
from make_report import QS, bucket, grade_int
rows = []
for r in recs:
    u = r.get("uni_name",""); q = QS.get(u)
    if not q or q > 200: continue
    b = bucket(r.get("subject",""))
    if b in ("CS/IT","EEE/硬件"):
        g = (r.get("ify_grades") or "").strip()
        gi = grade_int(g)
        if gi is not None and gi <= 136:
            rows.append((q, u, b, r.get("subject"), g, gi))
rows.sort(key=lambda x: (x[5], x[0]))
print("=== QS<=200, CS/EEE with IFY requirement <= AAB(136):", len(rows))
seen=set()
for q,u,b,s,g,gi in rows:
    k=(u,s,g)
    if k in seen: continue
    seen.add(k)
    print("  %-4s QS#%-4d %-6s %-62s %s" % ("", q, b, s[:62], g))
