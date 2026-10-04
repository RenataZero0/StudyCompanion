# -*- coding: utf-8 -*-
"""Crawl the NCUK Course Finder via its admin-ajax endpoint (server-rendered HTML)."""
import urllib.request, urllib.parse, re, html, json, os, time, sys

OUT_DIR = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料"
AJAX = "https://www.ncuk.ac.uk/wp-admin/admin-ajax.php"
HDRS = {
    "X-Requested-With": "XMLHttpRequest",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36",
    "Referer": "https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/",
    "Origin": "https://www.ncuk.ac.uk",
    "Content-Type": "application/x-www-form-urlencoded",
}

UNIS = {
400:"University of Alberta",50:"Aston University",360:"The University of Auckland",
414:"Auckland University of Technology (AUT)",106:"Bangor University",44:"University of Birmingham",
453:"University of Birmingham Dubai",43:"University of Bradford",15:"University of Bristol",
357:"Brock University",60:"Brunel University of London",415:"University of Canterbury",
63:"Cardiff University",158:"University for the Creative Arts",161:"De Montfort University",
66:"University of Dundee",67:"Durham University",71:"University of Essex",72:"University of Exeter",
176:"Falmouth University",73:"University of Glasgow",189:"Harper Adams University",
75:"Heriot-Watt University",8:"University of Huddersfield",
597:"International College of Liberal Arts at Yamanashi Gakuin University",79:"Keele University",
9:"University of Kent",49:"Kingston University London",143:"University of Lancashire",
81:"Lancaster University",202:"The University of Law",10:"University of Leeds",
48:"Leeds Beckett University",83:"University of Leicester",412:"Lincoln University",
11:"University of Liverpool",210:"Liverpool Hope University",332:"Liverpool John Moores University",
84:"London Metropolitan University",87:"London South Bank University",12:"The University of Manchester",
47:"Manchester Metropolitan University",411:"Massey University",632:"Murdoch University Dubai",
607:"NABA - Nuova Accademia Di Belle Arti",425:"UNSW Sydney",91:"Newcastle University",
599:"Newcastle University Medicine Malaysia",342:"The University of Newcastle, Australia",
107:"Northumbria University, Newcastle",232:"Norwich University of the Arts",416:"University of Otago",
593:"University of Ottawa",95:"Oxford Brookes University",18:"University of Portsmouth",
46:"Queen Mary University of London",19:"Queen's University Belfast",
7:"Queensland University of Technology (QUT)",96:"University of Reading",
605:"University of Reading Malaysia",548:"University of Regina",337:"RMIT University",
252:"Robert Gordon University",97:"Royal Holloway, University of London",13:"University of Salford",
14:"University of Sheffield",45:"Sheffield Hallam University",418:"University of Southampton Malaysia",
443:"St. George's University",353:"State University of New York (SUNY) at Oswego",
99:"University of Surrey",17:"University of Sussex",105:"Swansea University",
362:"Swinburne University of Technology",551:"Toronto Metropolitan University",
417:"Victoria University of Wellington",413:"University of Waikato",
59:"University of the West of England - UWE Bristol",351:"The University of Western Australia",
103:"University of Westminster",104:"University of York",
}

def clean(s):
    s = re.sub(r"(?s)<script.*?</script>", " ", s)
    s = re.sub(r"<[^>]+>", " ", s)
    s = html.unescape(s)
    return re.sub(r"\s+", " ", s).strip()

def parse_cards(t):
    out = []
    parts = re.split(r'<div class="entry-directory-result">', t)[1:]
    for p in parts:
        m_sub = re.search(r"<h2>(.*?)</h2>", p, re.S)
        m_uni = re.search(r"<h3>(.*?)</h3>", p, re.S)
        m_prog = re.search(r"<h4>(.*?)</h4>", p, re.S)
        m_code = re.search(r'class="js-cf-favourite\s*"\s*id="(CC\d+)"', p)
        rec = {
            "subject": clean(m_sub.group(1)) if m_sub else "",
            "university": clean(m_uni.group(1)) if m_uni else "",
            "programme": clean(m_prog.group(1)) if m_prog else "",
            "code": m_code.group(1) if m_code else "",
        }
        txt = clean(p)
        m = re.search(r"IFY Grades Required:\s*(.+?)\s*(?:Notes:|EAP Requirements|Overall Points/Grades Requirements|$)", txt)
        rec["ify_grades"] = m.group(1).strip() if m else ""
        if not rec["ify_grades"]:
            m = re.search(r"(?:A-Level|Grades? Required)\s*:?\s*([A-E*]{2,4})", txt)
            rec["ify_grades"] = m.group(1) if m else ""
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
        rec["notes"] = m.group(1)[:600] if m else ""
        # any other programme blocks in the same card
        rec["all_programmes"] = re.findall(r"<h4>(.*?)</h4>", p, re.S)
        out.append(rec)
    return out

def post(uni_id, page, subject="", prog="ify"):
    body = urllib.parse.urlencode({
        "action": "coursefinder",
        "page": str(page),
        "subject": subject,
        "universities[]": str(uni_id),
        "qualifications[]": prog,
    }, doseq=False)
    req = urllib.request.Request(AJAX, data=body.encode("utf-8"), headers=HDRS)
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8", "replace")

def crawl_uni(uid, name):
    recs, page = [], 1
    total = None
    while True:
        for attempt in range(3):
            try:
                c = post(uid, page)
                break
            except Exception as e:
                if attempt == 2:
                    print("  FAIL", name, page, e, flush=True)
                    return recs
                time.sleep(2)
        m = re.search(r"returned <strong>(\d+)</strong>", c)
        if total is None:
            total = int(m.group(1)) if m else 0
        cards = parse_cards(c)
        if not cards:
            break
        for r_ in cards:
            r_["uni_id"] = uid
            r_["uni_name"] = name
            r_["page"] = page
        recs.extend(cards)
        if total <= page * 20:
            break
        page += 1
        time.sleep(0.25)
    return recs

if __name__ == "__main__":
    all_recs = []
    ids = sorted(UNIS.keys())
    for i, uid in enumerate(ids, 1):
        name = UNIS[uid]
        recs = crawl_uni(uid, name)
        all_recs.extend(recs)
        print("[%d/%d] %-55s -> %d courses" % (i, len(ids), name, len(recs)), flush=True)
        time.sleep(0.2)
    with open(os.path.join(OUT_DIR, "course_finder_all.json"), "w", encoding="utf-8") as f:
        json.dump(all_recs, f, ensure_ascii=False, indent=1)
    print("TOTAL RECORDS:", len(all_recs))
