# -*- coding: utf-8 -*-
import json, re, sys, io, collections
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
P = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\course_finder_all.json"
data = [r for r in json.load(open(P, encoding='utf-8'))
        if r.get('programme') == 'International Foundation Year']

# 1) any record whose subject mentions foundation (any uni)
fnd = [r for r in data if re.search(r'foundation|extended degree|year 0|year zero',
                                    (r.get('subject') or '').lower())]
print("records with 'foundation' in subject:", len(fnd))
for r in fnd[:20]:
    print("   ", r.get('university'), '|', r.get('subject'), '|', r.get('ify_grades'))
print()

# 2) Manchester grade distribution
ma = [r for r in data if 'Manchester' in (r.get('university') or '')]
print("Manchester IFY records:", len(ma))
c = collections.Counter(re.sub(r'\s*See Course Facts.*$', '', (r.get('ify_grades') or ''),
                               flags=re.S).strip() for r in ma)
for g, n in c.most_common():
    print("   %-10s %d" % (g, n))
print()
print("--- Manchester entries at BBB or below ---")
for r in ma:
    g = re.sub(r'\s*See Course Facts.*$', '', (r.get('ify_grades') or ''), flags=re.S).strip()
    if g in ('BBB', 'BBC', 'BCC', 'CCC', 'CCD', 'CDD', 'DDD', 'BB', 'BC'):
        e = r.get('eap') or {}
        print("  %-62s | %-6s | EAP %s/%s%s%s%s | %s" % (
            (r.get('subject') or '')[:62], g, e.get('Overall'), e.get('Listening'),
            e.get('Reading'), e.get('Speaking'), e.get('Writing'),
            (r.get('notes') or '')[:110].replace('\n', ' ')))
