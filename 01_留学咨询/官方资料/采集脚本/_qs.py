# -*- coding: utf-8 -*-
import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.path.insert(0, r"D:\UsrFiles\Documents\NCUK IFY Self Study\NCUK官方资料")
import make_report as m
QS = getattr(m, "QS2027", None) or getattr(m, "QS", None)
for k in ["Keele","Robert Gordon","Salford","Portsmouth","Manchester Metropolitan","Aston","Queen Mary","Newcastle","Sussex","Brunel","UNSW","New South Wales","Auckland","Otago","Queensland","RMIT","Swinburne","Westminster","Huddersfield"]:
    hits = [ (n,v) for n,v in QS.items() if k.lower() in n.lower() ] if QS else []
    print(k, "->", hits)
