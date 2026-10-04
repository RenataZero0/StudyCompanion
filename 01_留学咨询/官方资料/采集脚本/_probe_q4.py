# -*- coding: utf-8 -*-
import json, re, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
P = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = [r for r in json.load(open(P, encoding='utf-8'))
        if r.get('programme') == 'International Foundation Year']
print("IFY records:", len(data))

EEE_KW = ['electrical', 'electronic', 'mechatron', 'robotic', 'renewable', 'photovoltaic']
CS_KW  = ['computer science', 'software engineering', 'computing', 'data science',
          'artificial intelligence', 'cyber']

TARGETS = ['Keele', 'Robert Gordon', 'Salford', 'Portsmouth', 'Aston', 'Queen Mary',
           'Newcastle', 'Sussex', 'Brunel', 'Southampton', 'Manchester', 'Glasgow',
           'Sheffield', 'Leeds', 'Birmingham', 'Cardiff', 'Belfast', 'York',
           'Lancaster', 'Liverpool', 'Heriot', 'Swansea', 'Surrey', 'Exeter',
           'Reading', 'Otago', 'Auckland', 'QUT', 'RMIT', 'Swinburne', 'Durham',
           'Alberta', 'Western Australia', 'New South Wales']

def eap(e):
    if not isinstance(e, dict):
        return str(e)
    return "%s/%s%s%s%s" % (e.get('Overall'), e.get('Listening'), e.get('Reading'),
                            e.get('Speaking'), e.get('Writing'))

for t in TARGETS:
    rows = [r for r in data if t.lower() in (r.get('university') or '').lower()]
    if not rows:
        print("\n### %-26s : NOT IN COURSE FINDER" % t)
        continue
    print("\n### %-26s (%d IFY courses)" % (rows[0].get('university'), len(rows)))
    out = []
    for r in rows:
        s = (r.get('subject') or '').lower()
        if any(k in s for k in EEE_KW):
            tag = 'EEE'
        elif any(k in s for k in CS_KW):
            tag = 'CS '
        elif re.search(r'foundation year|integrated', s + (r.get('notes') or '').lower()):
            tag = 'FND'
        else:
            continue
        g = re.sub(r'\s*See Course Facts.*$', '', (r.get('ify_grades') or ''), flags=re.S).strip()
        out.append((tag, r.get('subject'), g, eap(r.get('eap')), (r.get('notes') or '')[:150]))
    seen = set()
    for tag, subj, g, e, n in out:
        k = (subj, g)
        if k in seen:
            continue
        seen.add(k)
        print("  %s %-64s | %-8s | EAP %-14s" % (tag, subj[:64], g, e))
        if n:
            print("        %s" % n.replace('\n', ' '))
