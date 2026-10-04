# Q2 — University of Manchester 工程类本科对 NCUK IFY 学生的录取要求

- **调研对象**：The University of Manchester（manchester.ac.uk，QS 2027 #40）
- **学生背景**：NCUK IFY，模块 = Technical Maths + Further Mathematics + Physics + EAP；不能学化学
- **成绩上限换算**：A\*=56 / A=48 / B=40 / C=32 / D=24 → ceiling **BBB(120) ~ ABB(128)**；AAB(136) / AAA(144) 不可达
- **入学年份基准**：2026 entry（官网现行课程页）
- **证据标签**：官方 / 官方媒体 / 中介 / 网友
- **抓取限制**：web_fetch 不能读 PDF（返回 `Error: unsupported content type "application/pdf"`）→ 一律记「需人工下载」且未重试；shell 无网络，仅 web_search / web_fetch 可用

---

## 1. 结论速览：Manchester 直接入学（Year 1）工程 offer 全部 AAA 以上

| 专业 | 课程代码 | 典型 A-level offer | 原文（verbatim） | 证据 | URL |
|---|---|---|---|---|---|
| BEng Chemical Engineering | 03340 / H800 | **AAA** | `AAA including Mathematics and either Chemistry or Physics.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/03340/beng-chemical-engineering/ |
| BEng Civil Engineering | 03343 / H200 | **AAA** | `AAA including Mathematics, Physics, and one other subject.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/03343/beng-civil-engineering/ |
| BEng Aerospace Engineering | 03333 / H400 | **A\*AA** | `A*AA including Mathematics, Physics, and one other subject.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/03333/beng-aerospace-engineering/ |
| BEng Mechanical Engineering | 03389 / H300 | **A\*A\*A** | `A*A*A in Mathematics, Physics, and one other subject.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/03389/beng-mechanical-engineering/ |

补充（同上官方页）：
- Chemical BEng 上下文 offer `AAB including A in Mathematics and B or above in Chemistry or Physics.`；UK refugee/care `ABB including specific subjects.`；IB 36 分 6,6,6 HL。
- Chemical 页明确写出变通路径（verbatim）：`If you do not have the required grades or subjects you may want to consider our integrated foundation year.` URL: https://www.foundation.se.manchester.ac.uk/
- Civil：上下文 AAB；refugee/care **ABB**；IB 36 分 6,6,6 HL。
- Aerospace：上下文 AAA；refugee/care AAB；IB 37 分 7,6,6 HL。
- Mechanical：上下文 A\*AA；refugee/care ABB；IB 38 分 7,7,6 HL；并注明 `We are willing to consider applicants without Physics if they have studied Further Mathematics`。

> **对 NCUK IFY 学生的含义**：上述四条直接入学通道最低为 AAA(144)，高于 ceiling ABB(128)，**不可达**。refugee/care 的 ABB(128) 只对英国本土难民/照护经历申请者开放，国际生不适用。

---

## 2. 唯一可行的 ABB-or-below 工程入口：Faculty of Science and Engineering **Integrated Foundation Year**（官方）

官方总览页（官方，https://www.se.manchester.ac.uk/study/foundation-year/）列出 11 条通道，含 Aerospace、Chemical、Civil、Computer Science、Earth and Planetary Sciences、Electrical/Electronic & Mechatronic、Environmental Science、Materials Science、Mathematics、Mechanical、Physics，并写明：

> `Provided you achieve the specific progression criteria for your chosen degree, completion of the Integrated Foundation Year guarantees you a place on first year of your chosen degree.`（官方）

### 2.1 各基础年通道的 A-level offer（2026 entry，官方课程页）

| 基础年（含学位通道） | 代码 | UCAS | A-level 原文（verbatim） | 证据 | URL |
|---|---|---|---|---|---|
| **Chemical Engineering with an Integrated Foundation Year** | 12951 | H113 | `Grades BBC where a student has 3 relevant subjects` / `Grades BBB where a student has 2 relevant subjects` / `Grades ABB where a student has 1 relevant subject`；`The subjects considered to be relevant are Mathematics, Further Mathematics, Physics, Chemistry, Engineering.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/12951/beng-meng-chemical-engineering-with-an-integrated-foundation-year/ |
| **Electrical, Electronic & Mechatronic Engineering with an Integrated Foundation Year** | 12950 | H112 | `Grades BBC where a student has 3 relevant subjects` / `Grades BBB where a student has 2 relevant subjects` / `Grades ABB where a student has 1 relevant subject`；`The subjects considered to be relevant are Mathematics, Further mathematics, Physics, Chemistry, Computer Science. Electronics, Design & Technology, Engineering.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/12950/beng-meng-electrical-electronic-mechatronic-engineering-with-an-integrated-foundation-year/ |
| **Mechanical Engineering with an Integrated Foundation Year** | 12947 | H109 | `Grades BBB where a student has 3 relevant subjects` / `Grades ABB where a student has 2 relevant subjects` / `Grades AAB where a student has 1 relevant subject`；`The subjects considered to be relevant are Mathematics, Further Mathematics, Physics, Chemistry, Design & Technology, Engineering.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/12947/beng-meng-mechanical-engineering-with-an-integrated-foundation-year/ |
| **Materials Science with an Integrated Foundation Year** | 12959 | F013 | `BBC where a student has 3 relevant subjects` / `BBB where a student has 2 relevant subjects` / `ABB where a student has 1 relevant subject`；`The subjects considered to be relevant are Mathematics, Further Mathematics, Physics, Chemistry, Statistics, Computer Science.` | 官方 | https://www.manchester.ac.uk/study/undergraduate/courses/2026/12959/bsc-meng-materials-science-with-an-integrated-foundation-year/ |

其它已确认存在、本次未逐页抓取 A-level 原文的基础年：Aerospace 12948、Civil 12949、Computer Science 12952（官方「相关课程」互链）。→ 具体 offer 原文 **未查到**（未抓取，非不可达）。

### 2.2 学生（Maths + Further Maths + Physics + EAP，无化学）的匹配度

NCUK IFY 的 Technical Maths / Further Mathematics / Physics **三门都落在上面各表的 "relevant subjects" 清单内**：

- **Chemical 12951**：三门相关 → **BBC (120)** 即可满足最低档。但需注意：该基础年在读期间 `You will study mathematics, physics and chemistry.`（官方原文），且升读的化学工程学位要求化学背景；学生「不能学化学」为硬约束 → **实际不可选**。
- **EEE / Mechatronic 12950**：三门相关 → **BBC (120)**。
- **Mechanical 12947**：三门相关 → **BBB (120)**。
- **Materials Science 12959**：三门相关 → **BBC (120)**。

> **最低工程 A-level offer（本次实测，官方）**：**BBC = 120 UCAS 分**，出现在 Chemical 12951、EEE 12950、Materials 12959 三条 Integrated Foundation Year 通道；在排除化学要求的 Chemical 通道后，**EEE/Mechatronic 12950 与 Materials 12959 的 BBC(120)** 是学生实际可达的最低工程入口，恰好落在 ABB(128) 以下。直接入学工程课最低为 AAA(144)，不可达。

### 2.3 重要限制（官方原文）

- 基础年**不参与**上下文/refugee-care 降分（Chemical、EEE、Materials 三页均写明）：`This course is not eligible for a contextual offer. Contextual offers are only available for courses that have a standard entry requirements of ABB or higher.`
- Mechanical 12947 的基础年**可**获得降一档的 contextual / refugee-care offer（原文 `If you meet the criteria, you may be made a reduced offer. This will typically be at one grade below the standard offer.`），但该政策面向英国本土学生。
- 可能被要求参加学术测评（官方原文）：`applicants may be asked to attend an Academic Assessment`，形式为 `an online, invigilated, multiple-choice test`，考查数学与物理。
- 官方也声明按个案综合评估：`The following must therefore be viewed only as general guide.`

---

## 3. English / EAP 政策（工程类）

### 3.1 官方课程页明文（基础年与直接入学）

- 基础年通道（Mechanical 12947 原文，Chemical 12951 / EEE 12950 同义）：
  > `The minimum English Language requirement for this course is either: GCSE/IGCSE English Language grade 4/C; IELTS Academic/IELTS for UKVI (Academic) 6.0 overall with no sub-skill below 5.5; TOEFL iBT 80 overall with no less than 20 in speaking and 18 in all other subscores. We do not accept TOEFL iBT Special Home Edition.; An acceptable equivalent qualification`
  > `Please note international students would require an IELTS for UKVI (Academic) if the Foundation Year is taken as a stand-alone qualification.`
- Chemical 12951 / EEE 12950 段落版原文：`Overseas students are required to evidence an IELTS for UKVI with an overall score of 6.0, with no less than 5.5 in each component.`
- 大学总页（官方，https://www.manchester.ac.uk/study/international/admissions/language-requirements/ ）指出：`Academic English entry requirements vary by course, and can be found on individual course profiles`；`acceptable English language tests (such as IELTS, TOEFL, Pearson and Trinity ISE etc) are deemed to be valid for two years after the test date`；UKVI 要求 CEFR B2。
  → 该页 "Acceptable English language tests" 与 "Other English language qualifications" 两个折叠小节的内容**未渲染出来**（需 JS）→ **NCUK EAP 是否被列入「可接受等效资格」，本次 未查到**。这是本次调研最重要的未决缺口。

### 3.2 中介/汇总来源（间接提到 NCUK EAP 等级）

- **中介** https://abroad-sa-edu.com/programs/show/815?progression_id=73272 （Into Global 的 Manchester「International Foundation in Mechanical and Electrical Engineering」项目页）列出升读所需 NCUK 成绩，原文：
  > `Electrical and Electronic Engineering with Industrial Experience — English level: A / Final level: A*AA`（并注 `An excellent performance in local examinations before starting the NCUK Foundation Year, particularly in mathematics and physical science subjects, is also required. Your qualifications prior to the NCUK Foundation Year will be benchmarked against the entry requirements for The University of Manchester BEng (Hons) Electrical, Electronic and Mechatronic Engineering with an Integrated Foundation Year.`）
  > `Mechanical Engineering — English level: B / Final level: A*A*A`
  > `Mechanical Engineering with Management — English level: B / Final level: A*A*A, including Mathematics, Physics, and one other subject.`
  > `Mechatronic Engineering — English level: A / Final level: A*AA`
  - 该页 "English level" 显然指 NCUK EAP 等级；但来源为中介（非 manchester.ac.uk / ncuk.ac.uk），且与官方直接入学 A-level offer 数值对应，**不能作为官方 EAP 门槛依据**。
- **中介**（新东方前途出国，https://liuxue.xdf.cn/news/tianjin_8051464.shtml ）称 Manchester 热门专业实际约需 IFY 总成绩 60%–70%，并称人文方向 NCUK IFY 最低 AAA 且 EAP 须 A；同文称 Manchester 只对「入读 IFY 前学历被认定为相当于 AS Level」的申请者发 offer。

### 3.3 NCUK 官方对 Manchester 的说明（官方）

- NCUK Manchester 专属页（**官方**，https://www.ncuk.ac.uk/our-universities/university-manchester/ ）verbatim：
  > `Please note: Applicants with NCUK programmes are highly valued at The University of Manchester. To ensure a competitive application, the university requires students to have completed the NCUK International Foundation Year (IFY) with a previous qualification deemed equivalent to an AS Level. Further details on these prerequisite qualifications can be found here.`
  - 所链接文件 = https://www.ncuk.ac.uk/wp-content/uploads/2026/05/NCUK-International-Foundation-Year-The-University-of-Manchester-Prerequisite-Exclusion-2026.pdf → **PDF，web_fetch 不可读，需人工下载**（未重试）。
  - 同页 Accepted NCUK programmes 仅列 International Foundation Year 与 Master's Preparation；**页面未给出工程类具体分数** → 具体 IFY 分数 **未查到**。
- NCUK Course Finder（https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/ ）列出 By University 下拉（含 The University of Manchester），但结果由 JavaScript 渲染，HTML 内**不含任何分数数据**；加 `?university=the-university-of-manchester` 参数无效 → NCUK 目录数据 **未查到**。

---

## 4. 结论

**存在，但只有一条门：Manchester 的「Integrated Foundation Year」基础年通道；直接入学（Year 1）不存在 ABB-or-below 的工程门。**

1. **直接入学：无门。** Manchester 工程类本科（Mechanical A\*A\*A、Aerospace A\*AA、Civil AAA、Chemical AAA）的典型 A-level offer 全部 ≥ AAA(144)，高于 NCUK IFY 学生的分数上限 ABB(128)。少量 ABB(128) 出现在 refugee/care-experienced offer，仅限英国本土特定身份，国际生不适用。机械工程页甚至预留了「无物理但有进阶数学」的变通，但仍要求 A\*A\*A。
2. **基础年：有门，且门槛在 120–128 之间。** 官方 Integrated Foundation Year 通道对学生这三门模块（Technical Maths / Further Mathematics / Physics）全部计为 "relevant subjects"，因此适用最低档：
   - **EEE & Mechatronic（12950，H112）BBC(120)** —— 学生三门相关，**可达**（且 EEE 是学生目标序列里的第一顺位）。
   - **Materials Science（12959，F013）BBC(120)** —— **可达**。
   - **Mechanical（12947，H109）BBB(120)** —— **可达**（同分数档，但要求表述为 3 门相关时 BBB）。
   - Chemical（12951，H113）BBC(120) 可达但**必须学化学**，与硬约束冲突，排除。
3. **因此：本次实测的「最低工程 A-level offer」= BBC，120 UCAS 分**，位于 Manchester Integrated Foundation Year 通道 —— 它低于同时满足 ABB(128) 上限，构成 Manchester 真实存在的 ABB-or-below 工程入口。学士学位本体仍从 Year 1 起按各系 progression criteria 升读并受 guarantee 保护（官方：`completion of the Integrated Foundation Year guarantees you a place on first year of your chosen degree`）。
4. **EAP / 英语：官方硬门槛为 IELTS for UKVI 6.0（单项不低于 5.5）或等效资格。** Manchester **是否接受 NCUK EAP 作为等效英语资格，本次未能从官方页面取证 → 未查到**（语言要求页的可接受考试清单由 JS 渲染，未抓取到）。这是该门能否真正打开的关键未知项，务必人工核实（见 §5）。
5. **另一道前置门槛（NCUK 官方提示）**：Manchester 要求 NCUK IFY 申请者「入读 IFY 前的学历被认定为相当于 AS Level」（NCUK 原文见 §3.3），具体前置学历清单在不可读 PDF 中 → **需人工下载核实**；若学生前置学历（如 IGCSE/O Level/高二）被判定低于 AS Level，即使 IFY 成绩达标也可能拿不到 offer。中介（新东方）亦转述同一限制。

---

## 5. 未查到与失败来源

### 5.1 需人工下载（PDF，web_fetch 返回 `Error: unsupported content type "application/pdf"`，未重试）
1. https://www.ncuk.ac.uk/wp-content/uploads/2026/05/NCUK-International-Foundation-Year-The-University-of-Manchester-Prerequisite-Exclusion-2026.pdf —— Manchester 前置学历要求（NCUK 官方链接，**最关键**）
2. https://www.ncuk.ac.uk/wp-content/uploads/2024/04/The-University-of-Manchester-Pre-requisites.pdf —— 旧版同主题
3. https://www.ncuk.ac.uk/wp-content/uploads/2025/04/NCUK-International-Foundation-Year-Entry-Requirements-April-25.pdf —— 全量 IFY 录取要求
4. https://www.ncuk.ac.uk/wp-content/uploads/2026/09/University-Progression-Routes-September-2026.pdf —— 升学路径总表
5. documents.manchester.ac.uk/display.aspx?DocID=... —— Manchester 官方文档服务，仅提供 PDF，无法抓取（含课程页引用的 `DocID=19217` 附加费用政策）

### 5.2 未查到（未见数字，不编造）
- **Manchester 官方是否接受 NCUK EAP 及其等级要求**：语言要求页 https://www.manchester.ac.uk/study/international/admissions/language-requirements/ 的 "Acceptable English language tests" / "Other English language qualifications" 小节内容未渲染（需 JS）→ 未查到。（中介页出现 EAP A/B 等级，但不足为官方依据。）
- **Manchester 对 NCUK IFY 的具体分数要求（如「BBC」「60%」）**：NCUK 官方页与 Course Finder 均未给出工程类具体分数（Course Finder 由 JS 渲染）→ 未查到。目前只能沿用「IFY 结果对标 A-level offer」的间接推断。
- **Aerospace 12948 / Civil 12949 / Computer Science 12952 基础年的 A-level offer 原文**：本次未逐页抓取 → 未查到（页面本身存在且可抓，非不可达）。
- **Manchester 中国国别页**：`/study/international/country-specific-information/china/` 与 `.../china/entry-requirements/` 均返回 **404**；`/study/international/admissions/entry-requirements/` 亦 404。
- **Manchester 官方材料学院国别页** https://www.materials.manchester.ac.uk/study/international-students/country-specific-information/index.htm?audience=materials 、UCAS 化学工程课程页 https://www.ucas.com/explore/courses/2031b7a6-fe6a-6165-035d-d108db112c6d/course?studyYear=2026 未抓取。
- **未发现** Manchester 官方的 "General Engineering" / "Automotive Engineering" 本科课程（其工程学位按 Aerospace / Civil / Chemical / EEE / Mechatronic / Mechanical 分设）；"Biomedical Engineering" 未在本次搜到的 Science & Engineering 基础年通道列表中 → 未查到，未编造。

### 5.3 失败来源与工具限制（经验记录）
- **archive.org 不可用**：`https://web.archive.org/web/2024/...` 抓取报 `TypeError: fetch failed`；wayback availability API 报 `URL hostname "archive.org" resolves to a non-public IP address` → **勿重试**。
- **子代理不可用**：本会话尝试派 3 个子代理分担抓取，全部失败：`Error: subagent depth 3 exceeds maxDepth 1`（本会话为深一层派生子会话，不能再派子代理）。
- **可用 URL 模式（重要，便于人工复核）**：
  - 课程页直达 entry requirements：`https://www.manchester.ac.uk/study/undergraduate/courses/2026/course/?code=<课程代码>&pg=3`（实测对 12947、12950 有效）
  - 或 `https://www.manchester.ac.uk/study/undergraduate/courses/2026/<code>/<slug>/`
- **证据标签说明**：本文件所有官方条目均来自 `manchester.ac.uk` 域名；`ncuk.ac.uk/our-universities/university-manchester/` 亦标为官方（NCUK 官方页面）；abroad-sa-edu.com 与 liuxue.xdf.cn 标为中介。
