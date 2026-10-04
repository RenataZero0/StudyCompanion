# -*- coding: utf-8 -*-
import json, io, sys
sys.path.insert(0, r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料")
from make_report import QS, bucket, grade_int
recs = json.load(io.open(r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json", encoding="utf-8"))
print("--- UWA engineering full notes ---")
for r in recs:
    if r.get("uni_name")=="The University of Western Australia" and ("ngineer" in r.get("subject","")):
        e=r.get("eap") or {}
        print(" * %s | grade=%r | EAP=%s | code=%s\n   %s" % (r.get("subject"), r.get("ify_grades"), e.get("Overall"), r.get("code"), r.get("notes","")))
print()
print("--- QS<=250, EEE/硬件 or CS/IT, grade<=ABB, ranked by QS ---")
rows=[]
for r in recs:
    u=r.get("uni_name",""); q=QS.get(u)
    if not q or q>250: continue
    b=bucket(r.get("subject",""))
    if b not in ("CS/IT","EEE/硬件"): continue
    g=(r.get("ify_grades") or "").strip(); gi=grade_int(g)
    if gi is None or gi>128: continue
    e=r.get("eap") or {}
    rows.append((q,u,b,r.get("subject"),g,e.get("Overall","-"),"/".join(str(e.get(k,"-")) for k in ("Listening","Reading","Speaking","Writing"))))
rows.sort(key=lambda x:(x[0],x[3]))
seen=set()
for q,u,b,s,g,eo,es in rows:
    k=(u,s,g)
    if k in seen: continue
    seen.add(k)
    print("#%-4d %-5s %-58s %-6s EAP %s [%s]" % (q, b, s[:58], g, eo, es))
print()
print("--- QS map check ---")
for n in ["University of Surrey","University of Sussex","University of Surrey","Aston University","Heriot-Watt University","Swansea University","University of Reading","Newcastle University","Queen's University Belfast","Cardiff University","University of York","Lancaster University","University of Liverpool","Queen Mary University of London","The University of Auckland","The University of Western Australia","UNSW Sydney","RMIT University","University of Otago","University of Exeter","University of Sheffield","Durham University","The University of Manchester","University of Bristol","University of Birmingham","University of Leeds","University of Glasgow","University of Alberta","University of Southampton Malaysia","University of Southampton"]:
    print("  %-45s %s" % (n, QS.get(n)))
