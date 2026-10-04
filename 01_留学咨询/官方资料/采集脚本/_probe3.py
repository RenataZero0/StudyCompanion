# -*- coding: utf-8 -*-
import json, io, sys
p = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = json.load(io.open(p, encoding="utf-8"))
recs = data if isinstance(data, list) else data.get("records", data)
WANT = {
 "UNSW Sydney": ["Engineering (Electrical) (Honours)", "Engineering (Computer) (Honours)", "Engineering (Software) (Honours)", "Science (Computer Science)", "Engineering (Photovoltaics and Solar Energy) (Honours)", "Engineering (Renewable Energy) (Honours)", "Engineering (Robotics and Mechatronic) (Honours)"],
 "University of Glasgow": ["Computing Science", "Electronics and Electrical Engineering"],
 "The University of Western Australia": [],
 "Cardiff University": ["Computer Science", "Electrical and Electronic Engineering"],
 "Queen Mary University of London": ["Computer Science", "Electrical and Electronic Engineering"],
 "University of York": ["Computer Science", "Electronic and Electrical Engineering"],
 "Lancaster University": ["Computer Science", "Electronic & Electrical Engineering"],
 "Queen's University Belfast": ["Computer Science", "Electrical and Electronic Engineering"],
 "RMIT University": ["Bachelor of Computer Science", "Bachelor of Engineering (Electrical Engineering) (Honours)"],
 "Heriot-Watt University": ["Computer Science", "Electrical and Electronic Engineering (4 years)"],
 "Aston University": ["Computer Science", "Electrical and Electronic Engineering"],
 "Swansea University": ["Computer Science", "Electronic and Electrical Engineering"],
 "The University of Auckland": ["Computer Science", "Electrical and Electronic Engineering"],
 "University of Liverpool": ["Computer Science", "Electrical and Electronic Engineering"],
 "University of Sheffield": ["Computer Science", "Electrical and Electronic Engineering"],
 "University of Exeter": ["Computer Science"],
 "The University of Manchester": ["Electrical and Electronic Engineering"],
}
def show(r):
    keys = ["uni_name","subject","prog","ify_grades","eap_overall","eap_listening","eap_reading","eap_speaking","eap_writing","course_code","notes"]
    return " | ".join("%s=%s" % (k, str(r.get(k,""))[:150]) for k in keys if str(r.get(k,"")).strip())
for u, subs in WANT.items():
    hits = [r for r in recs if r.get("uni_name")==u and (not subs or r.get("subject") in subs)]
    print("=" * 100); print("### " + u + "  (共 %d 条匹配)" % len(hits))
    for r in hits[:24]:
        print("  " + show(r))
    if u == "The University of Western Australia":
        for r in recs:
            if r.get("uni_name")==u and ("ngineer" in r.get("subject","") or "lectr" in r.get("subject","")):
                print("  " + show(r))
