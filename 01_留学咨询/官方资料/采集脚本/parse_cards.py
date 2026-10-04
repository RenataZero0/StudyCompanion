import re, html, os, sys, json

BASE = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\dom"

def load(p):
    with open(p, "r", encoding="utf-8-sig", errors="replace") as f:
        return f.read()

def clean(s):
    s = re.sub(r"(?s)<script.*?</script>", " ", s)
    s = re.sub(r"<[^>]+>", " ", s)
    s = html.unescape(s)
    return re.sub(r"\s+", " ", s).strip()

def parse_cards(t):
    out = []
    # split into result cards
    parts = re.split(r'<div class="entry-directory-result">', t)[1:]
    for p in parts:
        p = p.split('<div class="entry-directory-result">')[0]
        m_sub = re.search(r"<h2>(.*?)</h2>", p, re.S)
        m_uni = re.search(r"<h3>(.*?)</h3>", p, re.S)
        m_prog = re.search(r"<h4>(.*?)</h4>", p, re.S)
        rec = {
            "subject": clean(m_sub.group(1)) if m_sub else "",
            "university": clean(m_uni.group(1)) if m_uni else "",
            "programme": clean(m_prog.group(1)) if m_prog else "",
        }
        txt = clean(p)
        m = re.search(r"IFY Grades Required:\s*(.+?)\s*(?:Notes:|EAP Requirements|$)", txt)
        rec["ify_grades"] = m.group(1).strip() if m else ""
        # EAP
        eap = {}
        m = re.search(r"EAP Requirements(.*?)(?:Overall Points/Grades Requirements|IFY Grades Required|Notes:|$)", txt)
        if m:
            seg = m.group(1)
            for k in ["Overall", "Listening", "Reading", "Speaking", "Writing"]:
                mm = re.search(k + r"\s*:?\s*([A-E*]+)", seg)
                if mm:
                    eap[k] = mm.group(1)
        rec["eap"] = eap
        m = re.search(r"Notes:\s*(.*)$", txt)
        rec["notes"] = (m.group(1)[:400] if m else "")
        out.append(rec)
    return out

if __name__ == "__main__":
    t = load(os.path.join(BASE, "unsw_ify.html"))
    total = re.search(r"Your search has returned <strong>(\d+)</strong>", t)
    print("TOTAL reported:", total.group(1) if total else "?")
    cards = parse_cards(t)
    print("cards parsed on this page:", len(cards))
    for c in cards[:15]:
        print("-", c["subject"], "|", c["programme"], "| IFY:", c["ify_grades"], "| EAP:", c["eap"])
