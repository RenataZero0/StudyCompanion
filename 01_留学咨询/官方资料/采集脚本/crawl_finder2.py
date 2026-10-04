# -*- coding: utf-8 -*-
"""Threaded crawl of the NCUK Course Finder admin-ajax endpoint."""
import urllib.request, urllib.parse, re, html, json, os, time, sys
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

sys.path.insert(0, r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料")
from crawl_finder import UNIS, AJAX, HDRS, parse_cards

OUT_DIR = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料"
lock = threading.Lock()
done = [0]

def post(uni_id, page, subject="", prog="ify"):
    body = urllib.parse.urlencode({
        "action": "coursefinder", "page": str(page), "subject": subject,
        "universities[]": str(uni_id), "qualifications[]": prog,
    })
    req = urllib.request.Request(AJAX, data=body.encode("utf-8"), headers=HDRS)
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8", "replace")

def crawl_uni(uid, name):
    recs, page, total = [], 1, None
    while True:
        c = None
        for attempt in range(4):
            try:
                c = post(uid, page); break
            except Exception as e:
                if attempt == 3:
                    print("  FAIL", name, page, e, flush=True); return recs
                time.sleep(1.5 + attempt)
        m = re.search(r"returned <strong>(\d+)</strong>", c)
        if total is None:
            total = int(m.group(1)) if m else 0
        cards = parse_cards(c)
        if not cards:
            break
        for r_ in cards:
            r_["uni_id"], r_["uni_name"], r_["page"] = uid, name, page
        recs.extend(cards)
        if total <= page * 20:
            break
        page += 1
        time.sleep(0.12)
    return recs

all_recs = []
with ThreadPoolExecutor(max_workers=6) as ex:
    futs = {ex.submit(crawl_uni, uid, UNIS[uid]): uid for uid in sorted(UNIS.keys())}
    for f in as_completed(futs):
        uid = futs[f]
        try:
            recs = f.result()
        except Exception as e:
            print("  ERR", UNIS[uid], e, flush=True); recs = []
        all_recs.extend(recs)
        with lock:
            done[0] += 1
            print("[%d/81] %-55s -> %d courses" % (done[0], UNIS[uid], len(recs)), flush=True)

with open(os.path.join(OUT_DIR, "course_finder_all.json"), "w", encoding="utf-8") as f:
    json.dump(all_recs, f, ensure_ascii=False, indent=1)
print("TOTAL RECORDS:", len(all_recs))
