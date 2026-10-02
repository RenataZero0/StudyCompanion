# -*- coding: utf-8 -*-
"""
NCUK IFY 自学计划生成器
生成 Schedule.xlsx：2026-10 ~ 2027-08 共 11 个月度日历 Sheet（YYYYMM）
"""
import datetime as dt
import calendar
import sys, os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from curriculum import TRACKS

# --------------------------------------------------------------------- 时间模板
T_WEEKDAY = "21:00-22:00"
# 假期 / 周末的时段：上午块已整体挪到下午（先新课、后练习的顺序不变）
T_PM1, T_PM2 = "14:00-15:00", "15:15-16:15"
T_SAT2, T_SUN2 = "20:00-21:00", "20:00-21:00"

SUBJ_CN = {
    "PURE": "纯数", "PHY": "物理", "APPLIED": "应用数学",
    "FURTHER": "进阶数学", "EAP": "学术英语", "REVIEW": "复习/测试",
}

# ------------------------------------------------------------------ 假期表格
# (开始, 结束, 名称, 类型)  类型: 'PH' 法定假期 / 'VAC' 寒暑假
HOLIDAYS = [
    ("2026-10-01", "2026-10-07", "国庆假期", "PH"),
    ("2026-12-24", "2027-01-03", "圣诞·元旦假期", "VAC"),
    ("2027-01-23", "2027-02-21", "寒假", "VAC"),
    ("2027-04-03", "2027-04-05", "清明节假期", "PH"),
    ("2027-05-01", "2027-05-05", "劳动节假期", "PH"),
    ("2027-06-09", "2027-06-09", "端午节假期", "PH"),
    ("2027-06-28", "2027-08-31", "暑假", "VAC"),
]
# 调休补课日（按正常工作日上课，但仍按工作日 1 小时安排）
MAKEUP_DAYS = ["2026-10-10", "2027-05-08"]

# 阶段测试 / 里程碑
MILESTONES = {
    "2026-11-29": "11月测试：P1 Ch1-3｜物理 Ch1-2",
    "2026-12-27": "12月测试：P1 Ch1-5｜物理 Ch1-4",
    "2027-01-24": "寒假前测试：P1 Ch1-6｜物理 Ch1-5",
    "2027-02-28": "2月测试：P1 全部｜物理 Ch1-7｜M1 全书",
    "2027-03-28": "3月测试：P2/3 Ch1｜物理 Ch1-9｜S1 Ch1-3",
    "2027-04-25": "期中测试：P2/3 Ch1-2｜物理 Ch1-11｜S1 Ch1-5",
    "2027-05-30": "5月测试：P2/3 Ch1-4｜物理 Ch1-12｜S1 全书",
    "2027-06-27": "学年末 AS 全科模拟（真题限时）",
    "2027-07-25": "暑期测试：P2/3 Ch6-8｜物理 Ch13-16",
    "2027-08-29": "暑期总测试：P2/3 全书｜物理 Ch1-18｜FM 全书",
}

# ------------------------------------------------------------------- EAP 大纲
EAP = [
    "EAP 导论：学术英语与日常英语的差异",
    "学术阅读①：略读与扫读（skimming）",
    "学术词汇①：AWL Sublist 1-2 + Anki 制卡",
    "段落写作：主题句/支撑句/结论句",
    "学术阅读②：识别论点、论据与论证结构",
    "学术词汇②：AWL Sublist 3-4",
    "摘要写作：压缩与转述学术文本（paraphrase）",
    "引用规范：Harvard 格式入门与学术诚信",
    "学术听力①：康奈尔笔记法与讲座笔记符号",
    "学术写作①：说明文（explanatory essay）结构",
    "学术词汇③：AWL Sublist 5-6",
    "图表描述：数据、趋势与比较（Task 1）",
    "学术听力②：TED-Ed / BBC 学术播客精听",
    "学术写作②：议论文与反驳段",
    "学术词汇④：AWL Sublist 7-10",
    "学术讨论：seminar 发言、提问与回应",
    "学术写作③：报告与实验报告结构",
    "学术听力③：大学公开课精听（MIT OCW）",
    "学术写作④：essay 自我修改与同伴互评",
    "扩展论文：选题、文献检索与提纲",
    "学术写作⑤：引言与结论的写法",
    "数据与公式的英文表达（数理语境）",
    "学术演讲①：presentation 结构与开场",
    "学术演讲②：幻灯片设计与视觉辅助",
    "数学与物理术语英汉对照复习（配 Anki）",
    "学术写作⑥：因果链与逻辑连接词",
    "学术听力④：英音/美音/多口音训练",
    "学术写作⑦：定义、分类与举例",
    "学术演讲③：presentation 演练与录像复盘",
    "EAP 模拟①：限时 essay（40 分钟 / 400 词）",
    "学术阅读③：长篇文献速读与结构化笔记",
    "学术写作⑧：批判性写作",
    "EAP 模拟②：听力 + 笔记 + 口头复述",
    "学术写作⑨：中式英语错误与学术语域",
    "EAP 模拟③：小组讨论与口头答辩",
    "学术写作⑩：EAP 真题 essay 题目精练",
    "综合复习：EAP 读听说写技能串联",
    "EAP 模拟④：读写综合限时任务",
    "个人陈述①：素材梳理与初稿",
    "个人陈述②：修改、润色与定稿",
]


# ============================================================== 内容队列构建
def _page(p):
    return f" p.{p}" if p else ""


def build_queue(books):
    """把课本目录转成 1 小时一节的课时队列。每章：讲2节→练1节→…→章末复习"""
    q = []
    for book in books:
        code, cn, en, chapters = book
        for chcode, chcn, chen, secs, eoc in chapters:
            i = 0
            groups = [secs[j:j + 2] for j in range(0, len(secs), 2)]
            for gi, grp in enumerate(groups):
                # 讲课
                nums = "、".join(f"§{s[0]}" for s in grp)
                title = f"新课 {code} {chcode} {chcn} {nums}"
                det = []
                for s in grp:
                    det.append(f"· §{s[0]} {s[1]}{_page(s[2])}")
                det.append("· 精读课本+做例题，整理笔记")
                q.append({"track_title": title, "details": det,
                          "tag": ("NOTE", code, chcode)})
                # 练习（每组讲完即练，最后一组留到章末）
                if gi < len(groups) - 1:
                    pr = []
                    for s in grp:
                        pr.append(f"· 做 §{s[0]} 课后练习并批改")
                    pr.append("· 错题本整理 + Anki 5′")
                    q.append({"track_title": f"练习 {code} {chcode} {chcn}（{nums}）",
                              "details": pr, "tag": ("PRAC", code, chcode)})
            # 章末复习
            ed = [f"· 章末复习题 End-of-chapter review{_page(eoc)}"]
            if eoc:
                ed.append("· 限时完成并批改")
            ed.append("· 本章公式/术语卡入 Anki")
            q.append({"track_title": f"章末复习 {code} {chcode} {chcn}",
                      "details": ed, "tag": ("EOC", code, chcode)})
    return q


QUEUES = {
    "PURE": build_queue(TRACKS["PURE"]),
    "PHY": build_queue(TRACKS["PHY"]),
    "APPLIED": build_queue(TRACKS["APPLIED"]),
    "FURTHER": build_queue(TRACKS["FURTHER"]),
}
EAP_QUEUE = [{"track_title": f"EAP {i+1:02d}", "details": [f"· {t}", "· 词汇/术语入 Anki"],
              "tag": ("EAP",)} for i, t in enumerate(EAP)]


# ------------------------------------------------ 课本学完后的真题强化队列
def _paper(subject, code, year, paper, note):
    return {"track_title": f"真题精练 {subject} {code} {year} {paper}",
            "details": [f"· {note}", "· 限时 55 分钟作答 + 即时对答案",
                        "· 错题归档，次日二刷错题"],
            "tag": ("EXAM", code)}


TAILS = {
    "PURE": [_paper("9709", "P1" if i % 2 == 0 else "P3", 2019 + i // 2,
                    "Paper " + ("1" if i % 2 == 0 else "3"), "纯数真题卷，限时 1 小时 50 分")
             for i in range(12)],
    "PHY": [_paper("9702", "AS", 2019 + i // 2,
                   "Paper " + ("1" if i % 2 == 0 else "2"), "物理 AS 真题（选择题/结构题）")
            for i in range(12)],
    "APPLIED": [_paper("9709", "M1" if i % 2 == 0 else "S1", 2019 + i // 2,
                       "Paper " + ("4" if i % 2 == 0 else "6"), "力学/概率统计真题卷")
                for i in range(10)],
    "FURTHER": [_paper("9231", c, 2019 + i // 4, "Paper " + str(i % 4 + 1), "进阶数学真题卷")
                for i, c in enumerate(["FP1", "FP2", "FS", "FM"] * 3)],
    "EAP": [{"track_title": f"EAP 强化 {i+1:02d}", "details": [f"· {t}", "· 限时完成后复盘"],
             "tag": ("EXAM", "EAP")} for i, t in enumerate([
        "限时 essay（40 分钟 / 450 词）并自评",
        "听力讲座 + 康奈尔笔记 + 口头复述",
        "学术阅读长文速读 + 摘要写作",
        "seminar 讨论录音复盘（流利度/学术语域）",
        "presentation 限时演练与同伴反馈",
        "EAP 真题套卷（读+写）限时",
        "EAP 真题套卷（听+说）限时",
        "学术词汇综合自测（AWL 全表）",
        "个人陈述 + 面试问答演练",
        "学年总结：错题/弱项清单与下阶段目标",
    ])],
}
for _k, _t in TAILS.items():
    (EAP_QUEUE if _k == "EAP" else QUEUES[_k]).extend(_t)

# 队列耗尽后的溢出顺序（各科完成后转入进阶数学）
FALLBACK = {"PURE": ["PURE", "FURTHER"], "PHY": ["PHY"],
            "APPLIED": ["APPLIED", "FURTHER"], "FURTHER": ["FURTHER"],
            "EAP": ["EAP", "FURTHER"]}


# ============================================================== 日期类型判定
def d(s):
    return dt.date.fromisoformat(s)


HOL_MAP = {}
for a, b, name, kind in HOLIDAYS:
    cur = d(a)
    while cur <= d(b):
        HOL_MAP[cur] = (name, kind)
        cur += dt.timedelta(days=1)

MAKEUP = {d(x) for x in MAKEUP_DAYS}


def day_type(dt_):
    if dt_ in MAKEUP:
        return ("school", None, None)
    if dt_ in HOL_MAP:
        name, kind = HOL_MAP[dt_]
        if dt_.weekday() >= 5:
            return ("weekend", name, kind)
        return ("vacation", name, kind)
    if dt_.weekday() >= 5:
        return ("weekend", None, None)
    return ("school", None, None)


# ============================================================== 计划生成
WD = ["一", "二", "三", "四", "五", "六", "日"]


class Plan:
    def __init__(self):
        self.cursor = {k: 0 for k in QUEUES}
        self.cursor["EAP"] = 0
        self.week_log = {k: [] for k in ["PURE", "PHY", "APPLIED", "FURTHER", "EAP"]}
        self.used = {k: 0 for k in QUEUES}
        self.used["EAP"] = 0
        self.rows = []
    def take(self, track):
        for qkey in FALLBACK[track]:
            q = EAP_QUEUE if qkey == "EAP" else QUEUES[qkey]
            i = self.cursor[qkey]
            if i < len(q):
                self.cursor[qkey] = i + 1
                self.used[qkey] += 1
                return q[i], qkey
        return ({"track_title": "本阶段内容已全部完成",
                 "details": ["· 自由复习 / 自主拓展"], "tag": ("DONE",)}, track)

    # ---- 每个日期产出 cells[weekday] = list of block dicts
    def blocks_for(self, dt_):
        kind, hname, hkind = day_type(dt_)
        wd = dt_.weekday()          # 0=Mon
        out = []
        if kind == "school":
            if wd <= 4:
                track = ["PURE", "PHY", "PURE", "PHY", "EAP"][wd]
                out.append((T_WEEKDAY, track))
            elif wd == 5:
                out += [(T_PM1, "APPLIED"), (T_SAT2, "APPLIED")]
            else:
                out += [(T_PM1, "REVIEW"), (T_SUN2, "FURTHER")]
        elif kind == "vacation":
            if wd <= 4:
                track = ["PURE", "PHY", "PURE", "PHY", "EAP"][wd]
                out += [(T_PM1, track), (T_PM2, track)]
            elif wd == 5:
                out += [(T_PM1, "APPLIED"), (T_SAT2, "APPLIED")]
            else:
                out += [(T_PM1, "REVIEW"), (T_SUN2, "FURTHER")]
        else:  # weekend（含周末假期）
            if wd == 5:
                out += [(T_PM1, "APPLIED"), (T_SAT2, "APPLIED")]
            else:
                out += [(T_PM1, "REVIEW"), (T_SUN2, "FURTHER")]
        return kind, hname, hkind, out

    def render_day(self, dt_):
        kind, hname, hkind, blocks = self.blocks_for(dt_)
        lines = []
        hdr = f"{dt_.month}月{dt_.day}日 周{WD[dt_.weekday()]}"
        if hname:
            hdr += f" ◆{hname}"
        lines.append(hdr)
        lines.append("─" * 12)

        week_items = []

        def lesson_line(slot_time, track):
            if track == "REVIEW":
                return None
            it, qkey = self.take(track)
            ln = [f"{slot_time} {SUBJ_CN[qkey]}"]
            ln.append(it["track_title"])
            ln += it["details"][:2]
            if it["tag"][0] in ("NOTE", "PRAC", "EOC"):
                self.week_log[qkey].append(it["track_title"])
            return ln

        for slot_time, track in blocks:
            if track == "REVIEW":
                lines.append(f"{slot_time} 复习/测试")
                key = dt_.isoformat()
                if key in MILESTONES:
                    lines.append(f"★ {MILESTONES[key]}")
                    lines.append("· 限时作答后逐题订正，记录失分点")
                else:
                    import re
                    wk = []
                    for t in ("PURE", "PHY", "APPLIED", "FURTHER"):
                        codes = []
                        for x in self.week_log[t]:
                            m = re.search(r"([A-Za-z0-9/]+ Ch\d+)", x)
                            if m and m.group(1) not in codes:
                                codes.append(m.group(1))
                        if codes:
                            wk.append(codes[-1])
                    lines.append("· 回顾：" + (" ｜ ".join(wk) if wk else "整理本周笔记"))
                    lines.append("· Anki 术语卡 15′ + 错题本整理")
                    lines.append("· 预习下周内容，制定周计划")
            else:
                ln = lesson_line(slot_time, track)
                if ln:
                    lines += ln
        return kind, hname, hkind, lines


# ============================================================== Excel 输出
def main():
    from openpyxl import Workbook
    from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
    from openpyxl.utils import get_column_letter

    plan = Plan()
    start, end = d("2026-10-01"), d("2027-08-31")

    # 先生成整年数据
    day_cells = {}
    cur = start
    while cur <= end:
        if cur.weekday() == 0:
            plan.week_log = {k: [] for k in plan.week_log}
        day_cells[cur] = plan.render_day(cur)
        cur += dt.timedelta(days=1)

    print("=== 各轨道消耗（小时）===")
    for k in ["PURE", "PHY", "APPLIED", "FURTHER", "EAP"]:
        total = len(QUEUES[k]) if k != "EAP" else len(EAP_QUEUE)
        print(f"{k:8s} 已排 {plan.used[k]:3d} / 共 {total:3d} 节")

    # ---- 写 Excel
    wb = Workbook()
    wb.remove(wb.active)

    thin = Side(style="thin", color="FF9C9C9C")
    med = Side(style="medium", color="FF000000")
    border_hdr = Border(left=thin, right=thin, top=med, bottom=thin)
    border_hdr_l = Border(left=med, right=thin, top=med, bottom=thin)
    border_day = Border(left=thin, right=thin, top=thin, bottom=thin)

    FILL_RED = PatternFill("solid", fgColor="FFFBD5D5")     # 浅红（单数月）
    FILL_GREEN = PatternFill("solid", fgColor="FFD6EFD6")   # 浅绿（双数月）
    FILL_HDR = PatternFill("solid", fgColor="FFFFFF00")

    months = [(2026, 10), (2026, 11), (2026, 12)] + [(2027, m) for m in range(1, 9)]

    # ---------- 先还原用户原有的 202306 格式模板页 ----------
    from openpyxl.styles import Color
    tpl = wb.create_sheet("202306")
    for ci, name in enumerate(["Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"], 1):
        c = tpl.cell(row=1, column=ci, value=name)
        c.fill = FILL_HDR
        c.font = Font(name="等线", size=16, bold=True)
        c.alignment = Alignment(horizontal="center", vertical="center")
        c.border = border_hdr_l if ci == 1 else border_hdr
    tpl.row_dimensions[1].height = 38.25
    for col, w in {"A": 35.75, "B": 36.6583333333333, "C": 34.3333333333333,
                   "D": 34.25, "E": 34.9166666666667, "F": 35.4166666666667,
                   "G": 37.3333333333333, "H": 34.0}.items():
        tpl.column_dimensions[col].width = w
    for r in range(2, 7):
        tpl.row_dimensions[r].height = 150
    tpl_fill = PatternFill("solid", fgColor="FFE4E9F0")
    tpl_fill.fgColor = Color(theme=9, tint=0.799981688894314)
    tpl_font = Font(name="等线", size=16, bold=True, color=Color(theme=1))
    tpl_align = Alignment(horizontal="center", vertical="top", wrap_text=True)
    _tpl_vals = {
        "D2": "1\n", "E2": "2\n", "F2": "3\n", "G2": "4\n\n",
        "A3": "5\n", "B3": "6\n", "C3": "7\n", "D3": "8\n", "E3": "9\n",
        "F3": "10\n", "G3": "11\n\n\n",
        "A4": "12\n", "B4": "13\n", "C4": "14\n", "D4": "15\n", "E4": "16\n",
        "F4": "17\n", "G4": "18\n",
        "A5": "19\n", "B5": "20\n", "C5": "21\n", "D5": "22\n", "E5": "23\n",
        "F5": "24\n\n", "G5": "25\n\n\n\n\n\n",
        "A6": "26\n\n", "B6": "27\n", "C6": "28\n", "D6": "29\n", "E6": "30\n",
        "F6": 31,
    }
    for r in range(2, 7):
        for ci in range(1, 8):
            c = tpl.cell(row=r, column=ci)
            c.fill = tpl_fill
            c.font = tpl_font
            c.alignment = tpl_align
            c.border = border_day
    for coord, v in _tpl_vals.items():
        tpl[coord] = v
    tpl.freeze_panes = "A2"
    tpl.sheet_view.zoomScale = 55

    for (yy, mm) in months:
        ws = wb.create_sheet(f"{yy}{mm:02d}")
        fill = FILL_RED if mm % 2 == 1 else FILL_GREEN

        for ci, name in enumerate(["Mon", "Tues", "Wed", "Thurs", "Fri", "Sat", "Sun"], 1):
            c = ws.cell(row=1, column=ci, value=name)
            c.fill = FILL_HDR
            c.font = Font(name="等线", size=16, bold=True)
            c.alignment = Alignment(horizontal="center", vertical="center")
            c.border = border_hdr_l if ci == 1 else border_hdr
        ws.row_dimensions[1].height = 38.25
        for ci in range(1, 8):
            ws.column_dimensions[get_column_letter(ci)].width = 38

        cal = calendar.Calendar(firstweekday=0)
        weeks = cal.monthdatescalendar(yy, mm)
        for wi, week in enumerate(weeks):
            r = wi + 2
            ws.row_dimensions[r].height = 178
            for ci, day in enumerate(week, 1):
                c = ws.cell(row=r, column=ci)
                c.border = border_day
                if day.month != mm:
                    c.fill = PatternFill("solid", fgColor="FFFAFAFA")
                    continue
                c.fill = fill
                c.font = Font(name="等线", size=9)
                c.alignment = Alignment(horizontal="left", vertical="top", wrap_text=True)
                kind, hname, hkind, lines = day_cells[day]
                c.value = "\n".join(lines)
        ws.freeze_panes = "A2"
        ws.sheet_view.zoomScale = 55

    # 把原模板页放到最后，新的月度计划排在最前
    idx = wb.sheetnames.index("202306")
    wb.move_sheet("202306", offset=len(wb.sheetnames) - 1 - idx)

    default = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "Schedule.xlsx")
    out = sys.argv[1] if len(sys.argv) > 1 else default
    try:
        wb.save(out)
    except PermissionError:
        out = out.replace(".xlsx", "_new.xlsx")
        wb.save(out)
        print("[!] 原文件被占用，已另存为:", out)
    print("saved:", out)


if __name__ == "__main__":
    main()
