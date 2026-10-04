import re, html, sys, json, io, os

BASE = r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料\dom"

def load(p):
    with open(p, "r", encoding="utf-8-sig", errors="replace") as f:
        return f.read()

def to_text(t):
    t = re.sub(r"(?s)<script.*?</script>", " ", t)
    t = re.sub(r"(?s)<style.*?</style>", " ", t)
    t = re.sub(r"<[^>]+>", "\n", t)
    t = html.unescape(t)
    t = re.sub(r"[ \t\r\f\v]+", " ", t)
    t = re.sub(r"\n\s*\n+", "\n", t)
    return t

# ---------- 1. university id map ----------
t = load(os.path.join(BASE, "unsw_ify.html"))
ids = re.findall(r'name="universities\[\]"[^>]*?value="(\d+)"[^>]*?>', t)
print("universities[] checkbox values found:", len(ids))

# fallback: any value= + following label
pairs = re.findall(r'<input[^>]*name="universities\[\]"[^>]*value="(\d+)"[^>]*>\s*<label[^>]*>(.*?)</label>', t, re.S)
print("pairs with labels:", len(pairs))
for v, lab in pairs[:200]:
    print(v, html.unescape(lab).strip())
