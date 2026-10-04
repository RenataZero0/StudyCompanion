# -*- coding: utf-8 -*-
import json, re, sys, io, collections
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
P = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = [r for r in json.load(open(P, encoding='utf-8'))
        if r.get('programme') == 'International Foundation Year']

names = sorted({r.get('university') for r in data})
print("UNIVERSITIES CONTAINING 'Manchester':")
for n in names:
    if 'anchester' in n:
        print("   ", n)
print()
print("UNIVERSITIES CONTAINING 'Southampton':")
for n in names:
    if 'outhampton' in n:
        print("   ", n)
print()

def dump(uni, keys, label):
    rows = [r for r in data if r.get('university') == uni]
    print("=== %s (%s) : %d IFY records ===" % (uni, label, len(rows)))
    seen = set()
    for r in rows:
        s = (r.get('subject') or '')
        sl = s.lower()
        if not any(k in sl for k in keys):
            continue
        if s in seen:
            continue
        seen.add(s)
        e = r.get('eap') or {}
        g = re.sub(r'\s*See Course Facts.*$', '', (r.get('ify_grades') or ''), flags=re.S).strip()
        print("  %-58s | %-7s | EAP %s/%s%s%s%s" % (s[:58], g, e.get('Overall'),
              e.get('Listening'), e.get('Reading'), e.get('Speaking'), e.get('Writing')))
        nt = (r.get('notes') or '').replace('\n', ' ').strip()
        if nt:
            print("        %s" % nt[:230])
    print()

K = ['electrical', 'electronic', 'mechatron', 'renewable', 'photovoltaic', 'robot',
     'computer science', 'software', 'computing', 'data science', 'cyber', 'artificial']
dump('The University of Manchester', K, 'QS 40')
dump('Manchester Metropolitan University', K, 'QS 601')
dump('University of Southampton, Malaysia', K, 'QS 111 branch')
