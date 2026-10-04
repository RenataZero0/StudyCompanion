# -*- coding: utf-8 -*-
import json, io, re
p = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = json.load(io.open(p, encoding="utf-8"))
recs = data if isinstance(data, list) else data.get("records", data)
print("TOTAL", len(recs))
print("keys:", sorted(recs[0].keys()))
eap = [r for r in recs if str(r.get("eap_overall","")).strip()]
print("records with eap_overall:", len(eap))
for r in eap[:15]:
    print("   ", r.get("uni_name"), "|", r.get("subject"), "|", r.get("ify_grades"), "| EAP", r.get("eap_overall"), r.get("eap_listening"), r.get("eap_reading"), r.get("eap_speaking"), r.get("eap_writing"))
print()
pat = re.compile(r"(Technical\s*(and|&|\+)\s*Further|Further\s*(and|&|\+)\s*Technical|not both|only one|both count|as one|cannot be combined|do not count|counted as)", re.I)
hits = [r for r in recs if pat.search(str(r.get("notes","")))]
print("=== NOTES mentioning two-maths combining:", len(hits))
seen=set()
for r in hits:
    k=(r.get("uni_name"),r.get("subject"))
    if k in seen: continue
    seen.add(k)
    print("  [%s] %s (%s) :: %s" % (r.get("uni_name"), r.get("subject"), r.get("ify_grades"), str(r.get("notes",""))[:260]))
