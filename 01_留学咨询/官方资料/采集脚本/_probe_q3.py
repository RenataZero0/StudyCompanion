# -*- coding: utf-8 -*-
import json, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

P = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = json.load(open(P, encoding='utf-8'))
print("total records:", len(data))

EEE_KW = ['electrical', 'electronic', 'mechatron', 'robotic', 'renewable', 'photovoltaic', 'power']
CS_KW  = ['computer science', 'software', 'computing', 'data science', 'artificial intelligence', 'cyber']

TARGETS = ['Keele', 'Robert Gordon', 'Salford', 'Portsmouth', 'Aston', 'Queen Mary',
           'Newcastle', 'Sussex', 'Brunel', 'Southampton', 'Manchester', 'Glasgow',
           'Sheffield', 'Leeds', 'Birmingham', 'Cardiff', 'Belfast', 'York',
           'Lancaster', 'Liverpool', 'Heriot', 'Swansea', 'Surrey', 'Exeter',
           'Reading', 'Otago', 'Auckland', 'QUT', 'RMIT', 'Swinburne', 'Durham',
           'Alberta', 'Western Australia', 'New South Wales']

def eap_str(e):
    if not isinstance(e, dict):
        return str(e)
    return "O=%s L=%s R=%s S=%s W=%s" % (e.get('Overall'), e.get('Listening'),
                                          e.get('Reading'), e.get('Speaking'), e.get('Writing'))

for t in TARGETS:
    rows = [r for r in data if t.lower() in (r.get('uni_name') or '').lower()]
    if not rows:
        print("\n### %s : NOT FOUND IN COURSE FINDER" % t)
        continue
    print("\n### %s  (%d IFY courses)" % (rows[0].get('uni_name'), len(rows)))
    hits, fnd = [], []
    for r in rows:
        p = (r.get('programme') or '') + ' | ' + (r.get('subject') or '')
        pl = p.lower()
        if any(k in pl for k in EEE_KW) or any(k in pl for k in CS_KW):
            hits.append(r)
        if re.search(r'foundation|integrated|extended|year zero', (p + (r.get('notes') or '')).lower()):
            fnd.append(r)
    seen = set()
    for r in hits + fnd:
        key = (r.get('programme'), r.get('ify_grades'))
        if key in seen:
            continue
        seen.add(key)
        pl = ((r.get('programme') or '') + (r.get('subject') or '')).lower()
        tag = 'EEE' if any(k in pl for k in EEE_KW) else ('CS ' if any(k in pl for k in CS_KW) else '   ')
        if r in fnd:
            tag = 'FND'
        print("  %s %-70s | %-14s | EAP %s" % (tag, (r.get('programme') or '')[:70],
                                                r.get('ify_grades'), eap_str(r.get('eap'))))
        nt = (r.get('notes') or '').strip()
        if nt:
            print("        notes: %s" % nt[:230])
