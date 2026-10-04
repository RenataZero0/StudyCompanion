# NCUK Course Finder 实抓结果 —— CS / EEE 逐校录取要求（最终版）

**数据来源**：NCUK 官方 University Course Finder 后台接口，2026 年官方数据
**抓取范围**：81 所 NCUK 伙伴大学，**9,152 条 IFY 课程记录**（全部拿到，无遗漏）
**原始数据**：`NCUK官方资料\course_finder_all.json`、`NCUK_CourseFinder_全量.csv`（1.78 MB）
**抓取方式**：headless Chrome 定位接口 → `POST /wp-admin/admin-ajax.php`，`action=coursefinder`（此前的二手资料与中介转述可全部作废）

---

## 一、两套评分制 —— 全篇最关键的一件事

### 1. 学科模块（Physics / Technical Maths / Further Maths 等）

| 等级 | 百分制 | NCUK Points |
|---|---|---|
| A* | ≥80% | 56 |
| A | 70–79% | 48 |
| **B** | **60–69%** | **40** |
| C | 50–59% | 32 |
| D | 40–49% | 24 |
| E | 35–39% | 16 |

出处：`NCUK官方资料\ify_overview_2026.txt:134-140`

### 2. 英语模块（EAP）—— **另一套更严的线**

| 等级 | 百分制 | 对应雅思 |
|---|---|---|
| A* | 90%+ | 7.5 |
| A | 80–89% | 7.0 |
| **B** | **70–79%** | **6.5** |
| C | 60–69% | 6.0 |
| D | 50–59% | 5.5 |
| E | 40–49% | 5.0 |

出处：`NCUK官方资料\entry_req_2026.txt:1102-1147`

### 3. 由此得出的三条结论

1. **BBB = 120 points**（3 门 × 40）。学校说的"蒙纳士要 BBB / 60%"和官方一致。
2. ⚠️ **学科拿 B 只要 60–69%，但 EAP 拿 B 要 70–79%** —— **同样叫 "B"，英语比学科难一档**。
3. ⚠️ **学校告诉你"EAP 65% 就行"= 英语 C 档（60–69%）= 雅思 6.0。但下面大量学校要求 EAP B（70%+）= 雅思 6.5。** **这是你目前规划里最大的隐藏缺口。**

---

## 二、EEE / 硬件工程方向（QS ≤ 200，按门槛排序）

| QS | 学校 | 专业 | 学科要求 | EAP 要求 | 先修备注 |
|---|---|---|---|---|---|
| **19** | **UNSW Sydney** | Engineering (Electrical) (Honours) | **BBB** | **B**（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Photovoltaics and Solar Energy) (Honours) | **BBB** | B（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Renewable Energy) (Honours) | **BBB** | B（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Robotics and Mechatronic) (Honours) | **BBB** | B（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Telecommunications) (Honours) | **BBB** | B（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Honours) / MEng (Electrical) | ABB | B（四项 C） | IFY 数学 **和** 物理 |
| 67 | Auckland | Electrical and Electronic Engineering | ABB | B（四项 C） | 逐案评估，可能要求更高 |
| 67 | Auckland | Mechatronics Engineering | ABB | B（四项 C） | 同上 |
| 82 | Sheffield | Electrical and Electronic Engineering | **AAB** | B（四项 C） | 含数学 |
| 68 | **Birmingham** | Electronic and Electrical Engineering | **AAB** | **C**（四项 D） | 数学、物理同等 |
| 68 | Birmingham | Mechatronic and Robotic Engineering | AAB | **C**（四项 D） | 同上 |
| **103** | **QMUL** | Electrical and Electronic Engineering | **ABB** | **C**（四项 D） | **Technical 或 Further Mathematics required** |
| 103 | QMUL | Robotics Engineering | AAB | C（四项 D） | 同上 |
| 119 | RMIT | BEng (Electrical Engineering) (Honours) | **BCC** | B（四项 C） | 数学（科学或工程）最低 C |
| 139 | Liverpool | Electrical and Electronic Engineering | ABB | **C/C/C/C**（四项都要 C） | 数学 + 一门理科 |
| 139 | Liverpool | Mechatronics and Robotic Systems | ABB | C/C/C/C | 同上 |
| 149 | Newcastle | Electrical and Electronic Engineering | AAB（备注写 ABB） | **C**（四项 D） | 数学或进阶数学 |
| **158** | **York** | Electronic and Electrical Engineering | **ABB** | **C**（四项 D） | **含数学（Integrated Maths 或 Technical Maths）** |
| 164 | Lancaster | Electronic & Electrical Engineering | ABB | B（四项 D） | IFY 数学 + 一门理科（物理/化学/CS/**进阶数学**） |
| 174 | **QUB** | Electrical and Electronic Engineering | **ABB** | **C**（四项 D） | 数学 + 一门（物理优先/生物/化学/**进阶数学**） |
| 179 | Cardiff | Electrical and Electronic Engineering | ABB | B（四项 D） | **数学须 B** |
| 111 | Southampton Malaysia | Electrical and Electronic Engineering | **A\*AA** | B（四项 C） | 数学 A + 物理 A |
| 281 | Heriot-Watt | Electrical and Electronic Engineering (4年) | **BBC** | **C**（四项 D） | **物理 + 数学须 B** |
| **416** | **Aston** | Electrical and Electronic Engineering | **BBC** | **C**（四项 D） | **数学 B + （物理 或 进阶数学）B** |
| 436 | Swansea | Electronic and Electrical Engineering | **BBB** | C/C/C/C | 含工程数学 |
| 273 | Sussex | Electrical and Electronic Engineering | ABB | B（四项 C） | — |
| — | Surrey | Electronic Engineering | ABB | B（四项 C） | 数学 + 另一门 STEM |
| 77 | Leeds | Electronic and Electrical Engineering | AAA | C（四项 D） | — |
| 80 | Glasgow | Electronics and Electrical Engineering | AAA | **不接受 EAP**，须雅思 | **Physics +（Technical Maths 或 Further Maths）** |
| 85 | Durham | Engineering (Electrical) | A\*AA | B（四项 C） | — |
| 40 | Manchester | Electrical and Electronic Engineering | A\*AA | A（B/B/B/A） | Technical Maths +（物理/化学/进阶数学） |
| 57 | Bristol | Electrical and Electronic Engineering | AAA | B（四项 C） | — |
| 96 | Alberta | Electrical Engineering | AAA | B（四项 C） | — |

**UWA（#77）的工程是个例外**：没有独立 EEE 条目，工程走 `Master of Professional Engineering (Direct Pathway)` 五年制（BSc + MEng），**BBC + EAP B**，但备注要求 **Chemistry IFY 或大一修一门化学单元**；Civil Engineering 更直接写明"必须化学 + 物理 + 技术数学"。**你没有化学 → UWA 工程基本走不通**（CS 不受影响）。

---

## 三、CS / 计算机方向（QS ≤ 200）

| QS | 学校 | 专业 | 学科要求 | EAP | 备注 |
|---|---|---|---|---|---|
| **19** | **UNSW Sydney** | Science (Computer Science) | **BBB** | B（四项 C） | 可与双学位组合，按较高者录取 |
| 19 | UNSW Sydney | Engineering (Computer) (Honours) | **BBB** | B（四项 C） | 无 |
| 19 | UNSW Sydney | Engineering (Software) (Honours) | **BBB** | B（四项 C） | **需 IFY 数学** |
| 19 | UNSW Sydney | Data Science and Decisions | ABB | B（四项 C） | — |
| 19 | UNSW Sydney | Diploma in Computer Science | DDE | C（四项 D） | 文凭课程，非学位 |
| **67** | **Auckland** | Computer Science | **BCC** | **C**（四项 D） | 逐案评估，可能要求更高 |
| 67 | Auckland | Data Science | BCC | C（四项 D） | 同上 |
| 77 | UWA | Computer Science | **BBC** | B（四项 C） | 含数学 |
| 77 | UWA | Advanced Computer Science: Computing and Data Science | **BBB** | B（四项 C） | — |
| 77 | UWA | Data Science | BBC | B（四项 C） | 含数学 |
| 82 | Sheffield | Computer Science | A\*AA | B（四项 C） | 含数学（任何大纲） |
| 68 | **Birmingham** | Computer Science | A\*AA | **C**（四项 D） | — |
| 68 | Birmingham | Computer Engineering | AAB | **C**（四项 D） | — |
| **103** | **QMUL** | Computer Systems Engineering | **ABB** | **C**（四项 D） | Technical 或 Further Maths |
| 103 | QMUL | Computer Science | AAA | C（四项 D） | — |
| 119 | RMIT | Bachelor of Computer Science | **BCC** | B（四项 C） | 数学（工程/科学）最低 C |
| 119 | RMIT | Bachelor of Information Technology | **BCC** | B（四项 C） | 同上 |
| 119 | RMIT | Bachelor of Software Engineering (Professional) | **BCC** | B（四项 C） | 同上 |
| 139 | Liverpool | Computer Science and Electronic Engineering | **ABB** | C/C/C/C | — |
| 149 | Newcastle | Computer Science | AAB | C（四项 D） | 有 IFY Computing 者获优待 |
| 158 | York | Computer Science | AAA | B（四项 C） | 含数学（Integrated 或 Technical） |
| 164 | Lancaster | Computer Science | AAB | C（四项 D） | 鼓励 IFY Computing 背景 |
| 164 | Lancaster | Management and Information Technology | ABB | C（四项 D） | — |
| 174 | **QUB** | Computer Science including Professional Experience | **ABB** | **C**（四项 D） | 至少一门优选 IFY 科目 + GCSE 数学 |
| 174 | QUB | Software Engineering With Placement | ABB | C（四项 D） | — |
| 174 | QUB | Computer Engineering | ABB | C（四项 D） | — |
| 179 | Cardiff | Computer Science | ABB | B（四项 D） | GCSE 数学 B/6 |
| 179 | Cardiff | Applied Software Engineering | ABB | B（四项 D） | — |
| 136 | Exeter | Computer Science | AAA | B（四项 D） | AAA–AAB；可用 IFY 数学或进阶数学 |
| 273 | Sussex | Computer Science | ABB | B（四项 C） | — |
| 196 | Reading | Computer Science | ABB | B（四项 C） | — |
| — | Surrey | Computer Science | ABB | B（四项 C） | 需数学 |
| **416** | **Aston** | Computer Science | **BBB** | **C**（四项 D） | — |
| 281 | Heriot-Watt | Computer Science | **BBC** | **C**（四项 D） | 须含数学 |
| 436 | Swansea | Computer Science | **BBB** | C/C/C/C | 含数学 |
| **198** | **Otago** | Computer Science | **CCC** | **C**（四项 D） | 无 |
| 240 | QUT | Bachelor of Information Technology | **CCD** | — | — |
| 240 | QUT | BEng (Honours) (Computer and Software Systems) | **CCD** | — | — |
| 291 | Swinburne | Bachelor of Computer Science | **CCC** | — | — |

---

## 四、⚠️ 必须纠正的错误（我之前给你的结论）

### 错误 1：Southampton 门槛最低、有余量 —— **错了**

我之前引用二手资料说"Southampton 官方 96 NCUK points（=CCC），是全部候选校里门槛最低的，有 10 分余量"。

**官方实抓数据**：

| 学校 | 专业 | 学科要求 | EAP |
|---|---|---|---|
| University of Southampton Malaysia | Electrical and Electronic Engineering | **A\*AA** | B（四项 C） |
| University of Southampton Malaysia | Computer Science | **A\*AA** | B（四项 C） |
| University of Southampton Malaysia | Mechanical Engineering | A\*AA | B（四项 C） |
| University of Southampton Malaysia | Aeronautics and Astronautics | A\*AA | B（四项 C） |

**⇒ Southampton 是全表门槛最高的学校之一，不是最低。BBB 差得很远。我之前那条"南安有 10 分余量、可作主推"的建议完全作废。**

（另：**英国本部 Southampton 根本不在 NCUK 伙伴名单里**，Course Finder 中只有马来西亚校区。本部是否接受 IFY 直录仍未确认。）

### 错误 2：把 EAP 65% 当成通用门槛 —— 不准确

65% 只够蒙纳士那一条。**UNSW / UWA / Cardiff / Lancaster / RMIT / Sussex / Surrey / Sheffield / Exeter 都要求 EAP B，即 70–79%。**

---

## 五、最大发现：UNSW Sydney #19，EEE 只要 BBB

这是我此前断言"公开渠道查不到 UNSW 分数线、只能邮件问"的直接答案 —— **查到了**：

```
Engineering (Electrical) (Honours)                     = BBB    EAP B（四项 C）  课程码 CC15991
Engineering (Photovoltaics and Solar Energy) (Honours) = BBB    EAP B（四项 C）  课程码 CC15997
Engineering (Renewable Energy) (Honours)               = BBB    EAP B（四项 C）  课程码 CC15999
Engineering (Robotics and Mechatronic) (Honours)       = BBB    EAP B（四项 C）  课程码 CC15995
Engineering (Telecommunications) (Honours)             = BBB    EAP B（四项 C）  课程码 CC16003
Engineering (Computer) (Honours)                       = BBB    EAP B（四项 C）  课程码 CC15990
Engineering (Software) (Honours)                       = BBB    EAP B（四项 C）  课程码 CC16002
Science (Computer Science)                             = BBB    EAP B（四项 C）  课程码 CC15939
```

**QS #19，学科只要 BBB（=120，与蒙纳士要求同级），唯一门槛是 EAP B。**

⚠️ 但这条要打个问号：**UAS 正常的 A-Level 要求是 AAB–AAA**，NCUK IFY 只要 BBB 显得相当宽松，且 NCUK 自己也声明"要求可能变动，以官网最新为准"。**必须写邮件向 UNSW 书面确认**（确认后就变成你的第一志愿）。

---

## 六、你的选课 × 先修要求核对

你的组合：**Technical Maths + Further Maths + Physics**（3 门学术 + EAP）

| 学校 | 先修要求原文 | 你是否满足 |
|---|---|---|
| UNSW | "Students require IFY Mathematics" | ✅ |
| QMUL EEE | "Technical or Further Mathematics required" | ✅ 完全命中 |
| York EEE | "Including Mathematics (Integrated Maths or Technical Maths)" | ✅ |
| Lancaster EEE | "IFY Mathematics and a Physical Science (Physics, Chemistry, CS, or **Further Mathematics**)" | ✅ 双保险 |
| QUB EEE | "Including Mathematics and at least one from Physics (preferred), Biology, Chemistry, **Further Mathematics**" | ✅ |
| Cardiff EEE | "Must include grade B in Maths" | ✅ |
| Exeter CS / Renewable Energy | "Candidates may offer **IFY Maths or Further Maths**" / "B in Maths or Further Maths and B in another Science" | ✅ |
| Aston EEE | "min B in Mathematics and min B in either: Physics, **Further Maths**" | ✅ |
| Heriot-Watt EEE | "Must include Physics and a Maths course at B" | ✅ |
| Glasgow EEE | "Physics AND (Technical Maths or Further Math)" | ✅ |
| Glasgow CS | "Required IFY modules are: **Further Maths**" | ✅ 只要一门 |
| RMIT | "Maths (Science or Engineering) IFY with a minimum grade of C" | ✅ 技术数学属工程数学 |
| UWA 工程 | 需 **Chemistry** IFY 或大一修化学单元 | ❌ **走不通** |
| Leeds / Manchester / Bristol / Durham EEE | AAA / A*AA / A*AA | ❌ 够不到 |

**⇒ 你的选课对 EEE/硬件线是"完美匹配"：物理 + 数学正好是标准先修。对 CS 线是"够格但不占优"（多校偏好有 IFY Computing/CS 背景的申请者，而你没有）。**

---

## 七、"Technical Maths + Further Maths 算不算两门"—— 证据状态更新

在全部 9,152 条记录的备注里检索"Technical + Further / only one / not both / as one / cannot be combined"，**没有任何一条明确说明两门数学是否算两门独立科目**。

但抓到了大量**间接证据**（都指向"大学把它们当作可分别列出的两个模块"）：

- **QMUL**：`"Technical or Further Mathematics required"` —— 两者并列、可互相替代
- **Glasgow EEE**：`"Required IFY modules are: Physics AND Technical Maths or Further Math"`
- **Glasgow CS**：`"Required IFY modules are: Further Maths"` —— 只要一门数学
- **Aston EEE**：`"min grade B in either: Physics, Further Maths"` —— **进阶数学可替代物理**
- **Lancaster EEE**：把 **Further Mathematics 列为可接受的"Physical Science"**
- **QUB EEE**：物理/生物/化学/**进阶数学** 四选一
- **Exeter CS**：`"Candidates may offer IFY Maths or Further Maths"`
- **Swansea CS**：`"To include Mathematics 1 2 3"`（暗示模块有编号，可分别计）

⇒ **没有任何一所大学表示两门数学会冲突或只能算一门。风险等级由"中"下调为"低"。** 但仍然**没有一所大学书面承诺"两门数学 = 两门有效学术科目"**，所以给 NCUK/蒙纳士的确认邮件**照发**（成本极低，且它是一票否决级问题）。

---

## 八、修正后的最终选校结论

**前提**：(a) 两门数学不被合并；(b) 学科达 BBB–ABB；(c) **EAP 达 C（60%+），冲刺 B（70%+）**

### 冲刺（学科门槛低、排名最高）

1. **UNSW Sydney #19 —— Engineering (Electrical) (Honours)，BBB + EAP B** ← **第一志愿**
   - 排名最高（#19），学科门槛与蒙纳士同级
   - 唯一硬门槛是 EAP B（70%+）
   - 备选同校：Photovoltaics and Solar Energy / Renewable Energy (Honours)（都是 BBB）

### 主推（学科 ABB、EAP 只要 C —— 最舒适的组合）

2. **QMUL #103 —— EEE，ABB + EAP C** ⭐ **性价比最高**
   - "Technical or Further Mathematics required" 正中你的选课
   - EAP 只要 C（四项 D），是所有高排名选项里英语门槛最松的
3. **York #158 —— Electronic and Electrical Engineering，ABB + EAP C**
   - 明确写 "Integrated Maths 或 Technical Maths"
4. **QUB #174 —— EEE，ABB + EAP C**
   - 数学 + （物理优先/生物/化学/进阶数学）
5. **Liverpool #139 —— EEE，ABB + EAP C（四项都须 C）**
   - 四项都要 C 比"总评 C + 单项 D"严，但比 EAP B 松

### 若能达到 AAB

6. **Birmingham #68 —— EEE，AAB + EAP C** ← 排名最高的 EAP-C 选项
7. **Sheffield #82 —— EEE，AAB + EAP B**

### 保底（务必申 1–2 所）

8. **Aston #416 —— EEE，BBC + EAP C**（数学 B + 物理 B）
9. **Heriot-Watt #281 —— EEE，BBC + EAP C**（物理 + 数学 B）
10. **Swansea #436 —— EEE，BBB + EAP C**
11. **Auckland #67 —— Computer Science，BCC + EAP C**（QS #67，CS 门槛极低，逐案评估）
12. **Otago #198 —— Computer Science，CCC + EAP C**
13. **RMIT #119 —— BEng (Electrical) (Honours) 或 Bachelor of CS，BCC + EAP B**

### 与蒙纳士的对照（三方比较）

| 学校 | QS | 学科要求 | EAP | NCUK 保证录取 |
|---|---|---|---|---|
| Monash Clayton C2000 IT | **#31** | 平均 **≥60%（BBB）** | **C（65%）** | ❌ 非伙伴，个案认可 |
| **UNSW Sydney EEE (Honours)** | **#19** | **BBB** | **B（70%+）** | ✅ 是伙伴 |
| **QMUL EEE** | #103 | **ABB** | **C** | ✅ 是伙伴 |

**结论：蒙纳士的学科门槛不比 UNSW 高（都是 BBB 级），学校排名也仅次于 UNSW —— 它真正的劣势只有一个：不是 NCUK 伙伴，不享保证录取、没有官方分数表背书。而 UNSW 的唯一障碍是 EAP B。所以"蒙纳士 vs UNSW"的取舍，本质上就是"你 EAP 能不能上 70%"这一个问题。**

---

## 九、待确认清单（按优先级）

| # | 事项 | 找谁 | 为什么关键 |
|---|---|---|---|
| 1 | UNSW EEE = BBB 是否属实、是否长期有效 | UNSW 招生办 + NCUK | 若属实，第一志愿成立 |
| 2 | Technical Maths + Further Maths 是否算两门独立科目 | 上海常青藤教务 / NCUK / Monash Ann PENG | 一票否决级 |
| 3 | EAP B 的具体判定（总评 70%+ 且单项不低于 C？） | NCUK 或目标校 | 决定英语要投入多少 |
| 4 | 蒙纳士 C2001 CS / C2002 SE(Honours) / C2003 CS Advanced 的 NCUK 分数 | Ann PENG | 决定是否值得放弃 C2000 |
| 5 | 英国本部 Southampton 是否接受 IFY 直录 | `ugadmissions@southampton.ac.uk` | 影响备选池 |
| 6 | 从中国 Year 11 进入 IFY 的学生，哪些大学限制升学 | 中心教务 / NCUK | 官方文件有此警示 |
| 7 | 中心能否开 Computer Science 模块（可选，用于补 CS 线竞争力） | 教务 / 院长 Jason Wu | 你已明确学校不开 |
| 8 | UWA 工程是否必须化学 | UWA 招生办 | 决定 UWA 是否可用 |

---

## 十、文件索引

| 文件 | 内容 |
|---|---|
| `NCUK官方资料\course_finder_all.json` | 9,152 条原始记录（含 EAP 明细、课程码、备注） |
| `NCUK官方资料\NCUK_CourseFinder_全量.csv` | 全量表格（1.78 MB） |
| `NCUK官方资料\NCUK_CS与EEE录取要求_实抓.md` | CS/EEE 自动生成报告（118 KB） |
| `NCUK官方资料\crawl_finder2.py` | 并发爬虫（可复跑刷新数据） |
| `NCUK官方资料\make_report.py` | 报告生成器（含 QS 映射与分类关键词） |
| `NCUK官方资料\dom\` | 页面 DOM 缓存 |
| 本文件 | CS/EEE 最终结论 |
