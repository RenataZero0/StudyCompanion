# -*- coding: utf-8 -*-
"""Turn the crawled Course Finder JSON into readable reports."""
import json, os, re, csv
from collections import defaultdict

D = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料"
recs = json.load(open(os.path.join(D, "course_finder_all.json"), encoding="utf-8"))

QS = {
 "UNSW Sydney":19, "The University of Manchester":40, "University of Bristol":57,
 "The University of Auckland":67, "University of Birmingham":68, "University of Leeds":77,
 "The University of Western Australia":77, "University of Glasgow":80, "University of Sheffield":82,
 "Durham University":85, "University of Alberta":96, "Queen Mary University of London":103,
 "University of Southampton Malaysia":111, "RMIT University":119, "University of Exeter":136,
 "University of Liverpool":139, "Newcastle University":149, "University of York":158,
 "Lancaster University":164, "Queen's University Belfast":174, "Cardiff University":179,
 "University of Reading":196, "University of Otago":198, "Queensland University of Technology (QUT)":240,
 "University of Surrey":262, "University of Sussex":273, "Heriot-Watt University":281,
 "Swinburne University of Technology":291, "University of Leicester":315, "University of Dundee":339,
 "Brunel University of London":353, "University of Essex":396, "Aston University":416,
 "Swansea University":436, "University of Kent":462, "Oxford Brookes University":473,
 "University of Portsmouth":501, "Bangor University":538, "Northumbria University, Newcastle":575,
 "Manchester Metropolitan University":601, "University of Huddersfield":651,
 "Sheffield Hallam University":701, "Leeds Beckett University":751,
 "Liverpool John Moores University":801, "University of Salford":851,
 "The University of Law":999, "London Metropolitan University":999, "London South Bank University":999,
}

CS_KW = ["computer", "computing", "software", "cyber", "informatics", "information technology",
         "artificial intelligence", "data science", "data analytics", "machine learning"]
EEE_KW = ["electrical", "electronic", "photonics", "renewable", "energy", "mechatronic",
          "robotic", "automation", "control", "microelectronic", "semiconductor", "telecommunication",
          "power engineering", "instrumentation"]
ENG_KW = ["engineering", "engineering science"]

def bucket(subj):
    s = subj.lower()
    if any(k in s for k in CS_KW):
        return "CS/IT"
    if any(k in s for k in EEE_KW):
        return "EEE/硬件"
    if any(k in s for k in ENG_KW):
        return "其他工程"
    return ""

# ---- 1. full CSV ----
rows = []
for r in recs:
    eap = r.get("eap") or {}
    rows.append([r.get("uni_name",""), r.get("subject",""), r.get("programme",""),
                 r.get("ify_grades",""), eap.get("Overall",""), eap.get("Listening",""),
                 eap.get("Reading",""), eap.get("Speaking",""), eap.get("Writing",""),
                 QS.get(r.get("uni_name",""), ""), r.get("code",""), (r.get("notes","") or "")[:200]])
with open(os.path.join(D, "NCUK_CourseFinder_全量.csv"), "w", encoding="utf-8-sig", newline="") as f:
    w = csv.writer(f)
    w.writerow(["大学","专业/课程","NCUK课程","IFY所需等级","EAP总评","EAP听力","EAP阅读","EAP口语","EAP写作","QS2027","课程码","备注"])
    w.writerows(sorted(rows, key=lambda x: (x[9] if isinstance(x[9], int) else 9999, x[0], x[1])))

# ---- 2. focused: CS + EEE for ranked partners ----
def grade_int(g):
    """rough ordering: count A* as 2, A as 1 -> lower is easier"""
    g = (g or "").strip().upper().replace(" ", "")
    if not re.fullmatch(r"[A-E*]+", g):
        return None
    return sum(1 for ch in g if ch == "*") * 2 + sum(1 for ch in g if ch == "A")

out = []
out.append("# NCUK Course Finder 实抓数据（International Foundation Year）\n")
out.append("来源：ncuk.ac.uk Course Finder 后台接口（admin-ajax `coursefinder`），%d 条记录。\n" % len(recs))

by_uni = defaultdict(list)
for r in recs:
    b = bucket(r.get("subject",""))
    if b and (r.get("ify_grades") or "").strip():
        by_uni[r.get("uni_name","")].append((b, r))

order = sorted(by_uni.keys(), key=lambda u: (QS.get(u, 9999), u))
for u in order:
    lst = by_uni[u]
    cs = [r for b, r in lst if b == "CS/IT"]
    ee = [r for b, r in lst if b == "EEE/硬件"]
    ot = [r for b, r in lst if b == "其他工程"]
    if not cs and not ee:
        continue
    out.append("\n## %s  (QS 2027 #%s)\n" % (u, QS.get(u, "?")))
    for label, group in (("CS / IT", cs), ("EEE / 硬件电子", ee), ("其他工程（参考）", ot)):
        if not group:
            continue
        out.append("\n**%s**\n" % label)
        out.append("| 课程 | IFY 所需等级 | EAP 总评 | EAP 单项最低 |")
        out.append("|---|---|---|---|")
        seen = set()
        for r in sorted(group, key=lambda x: (grade_int(x.get("ify_grades","")) if grade_int(x.get("ify_grades","")) is not None else 99, x.get("subject",""))):
            key = (r.get("subject",""), r.get("ify_grades",""))
            if key in seen:
                continue
            seen.add(key)
            eap = r.get("eap") or {}
            subs = "/".join(sorted(set(v for k, v in eap.items() if k != "Overall" and v)))
            out.append("| %s | %s | %s | %s |" % (r.get("subject",""), r.get("ify_grades",""),
                                                  eap.get("Overall",""), subs or "-"))
        out.append("")

with open(os.path.join(D, "NCUK_CS与EEE录取要求_实抓.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(out))

print("records:", len(recs))
print("CS/EEE report universities:", len([u for u in order if any(b in ("CS/IT","EEE/硬件") for b,_ in by_uni[u])]))
