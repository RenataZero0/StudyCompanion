# -*- coding: utf-8 -*-
"""对 --plantest 生成的 plandump.txt 做穷尽检查，找「时间与安排不符」的所有形态。

输出写到仓库里的 plancheck.txt（本机控制台按 ANSI 解码，直接 print 一定是乱码）。
"""
import io
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
APP = os.path.dirname(HERE)          # StudyCompanion\StudyCompanion\
DUMP = os.path.join(APP, "plandump.txt")
OUT = os.path.join(APP, "plancheck.txt")

HDR = re.compile(r"^###\s+(\S+)\s+(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})\s+(\S+)\s+时长\s+(\d+)′\s+合计\s+(\d+)′\s+(\S+)")
STEP = re.compile(r"^\s+(\d{1,2}):(\d{2})\s+\((\d+)[′']\)\s*(.*)$")
UNTIMED = re.compile(r"^\s+——\s+(.*)$")
RANGE = re.compile(r"(\d+)\s*[–\-~]\s*(\d+)\s*分钟")
SAID = re.compile(r"(\d+)\s*(?:分钟|[′'])")
PER = re.compile(r"(?:每节|各|每个|每小块)\s*(\d+)\s*分钟")
TOTAL = re.compile(r"共\s*(\d+)\s*分钟")
SEC = re.compile(r"§\s*\d+(?:\.\d+)?")

probs = []
slots = 0
minutes_total = 0
dates = set()
untimed_n = 0


def add(kind, where, msg):
    probs.append((kind, where, msg))


def flush(where, start, dur, expect, steps, prev_end):
    global minutes_total
    if start is None:
        return
    minutes_total += dur
    if expect != start + dur:
        add("未收尾", where, "收在 %02d:%02d，应收到 %02d:%02d（差 %+d 分钟）"
            % (expect // 60, expect % 60, (start + dur) // 60, (start + dur) % 60,
               expect - (start + dur)))
    if not steps:
        add("空时段", where, "一个步骤都没有")
        return
    first = int(steps[0][1]) * 60 + int(steps[0][2])
    if first != start:
        add("起点不对", where, "第一步在 %02d:%02d，时段却从 %02d:%02d 开始"
            % (first // 60, first % 60, start // 60, start % 60))


def sa(minutes):
    return "%02d:%02d" % (minutes // 60 % 24, minutes % 60)


def main():
    global slots, untimed_n
    lines = io.open(DUMP, encoding="utf-8").read().splitlines()
    where = None
    start = dur = expect = None
    steps = []
    for ln in lines:
        h = HDR.match(ln)
        if h:
            flush(where, start, dur, expect, steps, None)
            iso = h.group(1)
            dates.add(iso)
            slots += 1
            where = "%s %s-%s %s" % (iso, h.group(2) + ":" + h.group(3),
                                     h.group(4) + ":" + h.group(5), h.group(6))
            start = int(h.group(2)) * 60 + int(h.group(3))
            dur = (int(h.group(4)) * 60 + int(h.group(5))) - start
            expect = start
            steps = []
            stated_dur = int(h.group(7))
            stated_sum = int(h.group(8))
            if stated_dur != dur:
                add("表头时长", where, "表头写 %d′，起止时间算出 %d′" % (stated_dur, dur))
            if stated_sum != dur:
                add("表头合计", where, "表头合计 %d′ ≠ 时长 %d′" % (stated_sum, dur))
            if h.group(9) != "OK" and stated_sum == stated_dur:
                add("表头判定", where, "合计等于时长却标了 %s" % h.group(9))
            continue
        if where is None:
            continue
        if UNTIMED.match(ln):
            untimed_n += 1
            continue
        # 「  标题：…」和「  [科目]」是分段信息，不是步骤
        if ln.strip().startswith("标题：") or (ln.strip().startswith("[") and ln.strip().endswith("]")):
            continue
        s = STEP.match(ln)
        if not s:
            if ln.strip():
                add("无法解析", where, "这行看不懂：" + ln.strip()[:60])
            continue
        t = int(s.group(1)) * 60 + int(s.group(2))
        badge = int(s.group(3))
        body = s.group(4)
        steps.append((ln, s.group(1), s.group(2), badge, body))
        if t != expect:
            add("时钟错位", where, "该 %s 却写 %s（差 %+d 分钟）"
                % (sa(expect), sa(t), t - expect))
            expect = t
        if badge < 1:
            add("零分钟", where, "「%s」的分钟数是 %d" % (body[:40], badge))
        if not body.strip():
            add("空正文", where, "有 %d′ 却没有正文" % badge)
        # 正文里写的数必须等于徽标（取值优先级同 statedMinutes）
        tot = TOTAL.search(body)
        per = PER.search(body)
        if tot:
            said = int(tot.group(1))
        elif per:
            n = len(set(SEC.findall(body)))
            said = int(per.group(1)) * n if n >= 1 else int(per.group(1))
        else:
            g = SAID.search(body)
            said = int(g.group(1)) if g else badge
        if said != badge:
            add("正文打架", where, "徽标 %d′，正文写 %d′ —— %s" % (badge, said, body[:56]))
        # 「每节 N 分钟」旁边必须给出「共 M 分钟」，否则读者算不出来
        if per and not tot:
            add("缺共字", where, "写了「每节 %s 分钟」却没写「共 M 分钟」—— %s"
                % (per.group(1), body[:56]))
        if per and tot and int(per.group(1)) * max(1, len(set(SEC.findall(body)))) != int(tot.group(1)):
            add("共字不符", where, "「每节 %s 分钟」×%d 节 ≠ 「共 %s 分钟」"
                % (per.group(1), len(set(SEC.findall(body))), tot.group(1)))
        # 「a–b 分钟」这类区间：徽标落在区间内才算合理
        for r in RANGE.finditer(body):
            lo, hi = int(r.group(1)), int(r.group(2))
            if not (lo <= badge <= hi):
                add("区间不符", where, "「%s」区间 %d–%d 不含徽标 %d′"
                    % (r.group(0), lo, hi, badge))
        expect += badge
    flush(where, start, dur, expect, steps, None)

    kinds = {}
    for k, w, m in probs:
        kinds[k] = kinds.get(k, 0) + 1
    w = io.open(OUT, "w", encoding="utf-8", newline="")
    w.write("plandump：%s\n" % DUMP)
    w.write("时段 %d 个，覆盖 %d 天，总时长 %.2f 小时，不定时行 %d 条\n"
            % (slots, len(dates), minutes_total / 60.0, untimed_n))
    w.write("发现问题 %d 处\n" % len(probs))
    for k in sorted(kinds):
        w.write("  %-8s %d\n" % (k, kinds[k]))
    w.write("\n")
    for k in sorted(kinds):
        w.write("== %s ==\n" % k)
        for kk, ww, mm in probs:
            if kk == k:
                w.write("  %s：%s\n" % (ww, mm))
        w.write("\n")
    w.close()
    print("OK -> %s  (%d problems, %d kinds)" % (OUT, len(probs), len(kinds)))
    for k in sorted(kinds):
        print("   %-8s %d" % (k, kinds[k]))


main()
