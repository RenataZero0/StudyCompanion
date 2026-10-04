# -*- coding: utf-8 -*-
import json, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
P = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = json.load(open(P, encoding='utf-8'))
r = data[0]
print("KEYS:", list(r.keys()))
for k, v in r.items():
    s = json.dumps(v, ensure_ascii=False)
    print("  %-16s = %s" % (k, s[:180]))
print()
# find a UNSW electrical record
for r in data:
    if 'New South Wales' in (r.get('uni_name') or '') and 'Electrical' in json.dumps(r, ensure_ascii=False):
        for k, v in r.items():
            print("  %-16s = %s" % (k, json.dumps(v, ensure_ascii=False)[:200]))
        break
