# -*- coding: utf-8 -*-
import json, io, re
p = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
recs = json.load(io.open(p, encoding="utf-8"))
SHORT = {
 "UNSW Sydney": ["Engineering (Electrical) (Honours)","Engineering (Computer) (Honours)","Engineering (Photovoltaics and Solar Energy) (Honours)","Engineering (Renewable Energy) (Honours)","Science (Computer Science)","Engineering (Software) (Honours)","Engineering (Robotics and Mechatronic) (Honours)","Engineering (Telecommunications) (Honours)","Engineering (Honours) / Master of Engineering (Electrical Engineering)"],
 "The University of Western Australia": ["Computer Science","Advanced Computer Science: Computing and Data Science","Data Science","Master of Professional Engineering (Direct Pathway)","Mechanical Engineering","Civil Engineering"],
 "The University of Auckland": ["Computer Science","Electrical and Electronic Engineering","Software Engineering"],
 "RMIT University": ["Bachelor of Computer Science","Bachelor of Engineering (Electrical Engineering) (Honours)","Bachelor of Engineering (Computer and Network Engineering) (Honours)","Bachelor of Software Engineering (Professional)"],
 "Queen Mary University of London": ["Electrical and Electronic Engineering","Computer Science"],
 "Cardiff University": ["Electrical and Electronic Engineering","Computer Science"],
 "Lancaster University": ["Electronic & Electrical Engineering","Computer Science"],
 "University of York": ["Electronic and Electrical Engineering","Computer Science"],
 "Queen's University Belfast": ["Electrical and Electronic Engineering","Computer Science","Computer Science including Professional Experience"],
 "University of Liverpool": ["Electrical and Electronic Engineering","Computer Science"],
 "University of Surrey": ["Computer Science","Electronic Engineering"],
 "University of Sussex": ["Computer Science","Electrical and Electronic Engineering"],
 "University of Reading": ["Computer Science"],
 "Aston University": ["Electrical and Electronic Engineering","Computer Science"],
 "Heriot-Watt University": ["Electrical and Electronic Engineering (4 years)","Computer Science"],
 "Swansea University": ["Electronic and Electrical Engineering","Computer Science"],
 "University of Exeter": ["Computer Science","Renewable Energy Engineering"],
 "Newcastle University": ["Electrical and Electronic Engineering","Computer Science"],
 "University of Sheffield": ["Electrical and Electronic Engineering","Computer Science"],
 "University of Southampton Malaysia": [],
 "University of Otago": ["Computer Science","Energy Science and Technology"],
}
def fmt(r):
    e = r.get("eap") or {}
    eaps = "EAP " + "/".join(str(e.get(k,"-")) for k in ("Overall","Listening","Reading","Speaking","Writing")) if e else "EAP (无要求/未列)"
    return "%s | %s | %s | %s | %s" % (r.get("subject"), r.get("ify_grades"), eaps, r.get("code",""), str(r.get("notes",""))[:220])
for u, subs in SHORT.items():
    hits = [r for r in recs if r.get("uni_name")==u and (not subs or r.get("subject") in subs)]
    if not hits: 
        print("=" * 90); print("### " + u + "  <<无匹配>>"); continue
    print("=" * 90); print("### " + u)
    seen=set()
    for r in hits:
        k=(r.get("subject"), r.get("ify_grades"))
        if k in seen: continue
        seen.add(k)
        print("  " + fmt(r))
