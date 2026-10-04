# Q7 — NCUK 官方大学目录深度排查

**任务日期范围（数据快照）**：本次抓取时 NCUK 官网显示 © Copyright 2026
**调查对象**：NCUK 自己的官方大学目录（Course Finder / Our Universities / 各大学合作页）
**学生背景（既定，不重复验证）**：NCUK IFY，三门学术模块 = Technical Maths、Further Mathematics、Physics，另加 EAP。等级上限 BBB(120)–ABB(128)。目标 EEE → 机械/土木/航空/材料。全程英国就读。不能化学。不读商科。
**本文件回答两个核心问题**：
1. NCUK Course Finder 能否取到课程级别的录取要求？（**问题一：南安普顿问题**）
2. "三门学术模块"如何计算，两个数学模块能否分别计算？（**问题二：模块计数问题**）

---

## 结论摘要（先看这里）

| # | 问题 | 结论 | 证据等级 |
|---|---|---|---|
| 1 | NCUK Course Finder 能否取到课程级录取要求？ | **不能。** 课程行是 JavaScript 注入，所有 GET 参数被服务端忽略。测试的 4 个查询串全部返回 HTTP 200 但内容完全相同、**零课程行** | 官方 |
| 2 | 英国南安普顿大学（Southampton 本校）是否是 NCUK 合作院校？ | **不是。** NCUK 官方目录中只有 **University of Southampton Malaysia**（马来西亚校区）。四处独立证据一致，且英国本校页 404 | 官方 |
| 3 | 英国 top-100 合作院校接受哪些 NCUK 项目？ | 见下表（8 所全部取到） | 官方 |
| 4 | "三门学术模块"是否允许两个数学模块分别计算？ | **NCUK 官方页面从未说明。** 官方只列出 12 个具名模块（其中 3 个是数学），并写明"study three academic subject modules"，既未允许也未禁止两个数学模块并列 | 未查到 |
| 5 | 各校数字级录取要求 / EAP 等级要求 | **NCUK 任何一个大学合作页面都未给出任何数字级要求或 EAP 等级要求。** 这些只存在于需人工下载的 PDF | 未查到 |

---

## (1) NCUK Course Finder 查询串测试 — 全部失败

**入口**：`https://www.ncuk.ac.uk/ncuk-entry-directory` → 302 重定向到 `https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/`（HTTP 200）
**页面标题（逐字）**：`Find Your Dream University Course | NCUK Course Finder`
**页面 H1（逐字）**：`University Course Finder`

### 测试结果表

| 测试 URL | HTTP | 是否返回课程数据 | 说明 |
|---|---|---|---|
| `https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/` | 200 | **否** | 仅有筛选控件，**零课程行** |
| `https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?university=the-university-of-manchester` | 200 | **否** | 与裸页面**逐字节相同**；参数被服务端忽略 |
| `https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?country=uk` | 200 | **否** | 国家下拉仍列出全部 10 个国家，未施加任何英国限定 |
| `https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?ed_uni_header=1&ed_prog_header=1` | 200 | **否** | 与裸页面相同；`ed_uni_header` / `ed_prog_header` 无任何效果 |

### 页面上实际存在的内容（逐字）

页面只有两个空的筛选下拉框：

**"By Country/Territory"（逐字，共 10 项）**：`United Kingdom`, `Australia`, `New Zealand`, `USA`, `Canada`, `Malaysia`, `Italy`, `UAE`, `Japan`, `Grenada`

**"By University"（逐字，完整 80+ 项字母序列表，本文件全文照录，因为它同时是南安普顿问题的证据）**：
`University of Alberta`, `Aston University`, `The University of Auckland`, `Auckland University of Technology (AUT)`, `Bangor University`, `University of Birmingham`, `University of Birmingham Dubai`, `University of Bradford`, `University of Bristol`, `Brock University`, `Brunel University of London`, `University of Canterbury`, `Cardiff University`, `University for the Creative Arts`, `De Montfort University`, `University of Dundee`, `Durham University`, `University of Essex`, `University of Exeter`, `Falmouth University`, `University of Glasgow`, `Harper Adams University`, `Heriot-Watt University`, `University of Huddersfield`, `International College of Liberal Arts at Yamanashi Gakuin University`, `Keele University`, `University of Kent`, `Kingston University London`, `University of Lancashire`, `Lancaster University`, `The University of Law`, `University of Leeds`, `Leeds Beckett University`, `University of Leicester`, `Lincoln University`, `University of Liverpool`, `Liverpool Hope University`, `Liverpool John Moores University`, `London Metropolitan University`, `London South Bank University`, `The University of Manchester`, `Manchester Metropolitan University`, `Massey University`, `Murdoch University Dubai`, `NABA – Nuova Accademia Di Belle Arti`, `UNSW Sydney`, `Newcastle University`, `Newcastle University Medicine Malaysia`, `The University of Newcastle, Australia`, `Northumbria University, Newcastle`, `Norwich University of the Arts`, `University of Otago`, `University of Ottawa`, `Oxford Brookes University`, `University of Portsmouth`, `Queen Mary University of London`, `Queen's University Belfast`, `Queensland University of Technology (QUT)`, `University of Reading`, `University of Reading Malaysia`, `University of Regina`, `RMIT University`, `Robert Gordon University`, `Royal Holloway, University of London`, `University of Salford`, `University of Sheffield`, `Sheffield Hallam University`, `University of Southampton Malaysia`, `St. George's University`, `State University of New York (SUNY) at Oswego`

> **证据等级**：官方
> **URL**：https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/
> **关键负面结论**：**Course Finder 的课程级录取要求无法通过 web_fetch 取得 —— 未查到。**

**技术说明（为什么失败）**：NCUK Course Finder 是一个纯 JavaScript 组件。服务端渲染出的 HTML 只包含筛选框的 `<select>` 元素和静态选项文本，课程表格由浏览器端脚本在用户选择筛选条件后异步注入。`?university=` / `?country=uk` 等 GET 参数不在服务端被解析（同样的参数在 `Our Universities` 归档页上也只是客户端筛选，见下）。

---

## (2) 南安普顿问题 — 英国南安普顿本校 NOT an NCUK partner

这是本任务最明确的发现，共有**四项互相独立的官方证据**。

### 证据 A：合作页 slug 穿透到马来西亚页

| URL | HTTP | 页面标题 | H1 |
|---|---|---|---|
| `https://www.ncuk.ac.uk/our-universities/university-of-southampton/` | 200 | `University of Southampton Malaysia - NCUK` | `University of Southampton Malaysia` |
| `https://www.ncuk.ac.uk/our-universities/university-of-southampton-malaysia/` | 200 | `University of Southampton Malaysia - NCUK` | `University of Southampton Malaysia` |

两个 URL 返回**逐字节相同的内容**。也就是说并不存在独立的英国南安普顿合作页；裸 slug `university-of-southampton` 直接落到马来西亚校区页面。

**页面正文逐字**：
> "The University of Southampton Malaysia is a fantastic choice for international students seeking quality higher education. Located in Iskandar Puteri, the university campus sits as a neighbour to the vibrant cities of Johor Bahru and Singapore."

> "This fantastic location offers students a unique multicultural experience, combining the best of East and West. Studying at NCUK provides a pathway to this prestigious university, enabling students to gain a globally recognised degree in a vibrant and dynamic environment."

**"Accepted NCUK programmes:"（逐字，仅一条）**：`International Foundation Year`

**其他逐字细节**：位置 "Located in Iskandar Puteri, one of Malaysia's fastest-growing cities"；"Its proximity to Singapore offers students an opportunity for even more diverse cultural experiences."；设施 "The university's advanced aeronautics and astronautics labs are particularly noteworthy."

> **证据等级**：官方
> **URL**：https://www.ncuk.ac.uk/our-universities/university-of-southampton-malaysia/

### 证据 B：Course Finder 的 "By University" 下拉框中唯一的一个 Southampton 条目

完整下拉框（上文 (1) 已全文照录）中，字母序上 `Sheffield Hallam University` 之后紧接的是 `University of Southampton Malaysia`。**列表中没有独立的 `University of Southampton` 条目。**

> **证据等级**：官方
> **URL**：https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/

### 证据 C：NCUK IFY 官方页的 80+ 所升学合作院校全名单

`https://www.ncuk.ac.uk/ncuk-programmes/international-foundation-year/` 页面正文的 University Progression 链接列出了全部合作院校（80+ 条）。该名单中只有 **`University of Southampton Malaysia → /our-universities/university-of-southampton-malaysia/`**，**没有**任何独立的英国南安普顿条目。

> **证据等级**：官方
> **URL**：https://www.ncuk.ac.uk/ncuk-programmes/international-foundation-year/

### 证据 D：NCUK 官网首页合作伙伴 logo 轮播 + 英国本校 slug 404

- NCUK 官网首页 `https://www.ncuk.ac.uk/` 的 "NCUK University Partners" logo 轮播是一个完整的合作院校 slug 索引。该轮播中包含 Manchester / Sheffield / Queen Mary / Bristol / Leeds / Glasgow / Durham / Birmingham 等，**完全没有 Southampton 的任何条目**。
- 直接测试英国本校 slug：`https://www.ncuk.ac.uk/our-universities/the-university-of-southampton/` → **HTTP 404**，页面标题 `Page not found - NCUK`，正文逐字 `# 404` / `## 404 - Page Not Found` / "The page you've requested either no longer exists or cannot be found."

> **证据等级**：官方
> **URL**：https://www.ncuk.ac.uk/ ；https://www.ncuk.ac.uk/our-universities/the-university-of-southampton/

### 结论（南安普顿）

**NCUK 官方目录中，"University of Southampton"（英国南安普顿本校）不作为独立合作院校存在；唯一出现的是 "University of Southampton Malaysia"（马来西亚伊斯干达公主城校区）。** 该校区可接受的 NCUK 项目仅 `International Foundation Year`，且页面未给出任何数字级成绩要求或 EAP 要求（**未查到**）。

**对学生的影响**：如果学生的目标是**在英国**读 EEE，南安普顿不能通过 NCUK 路径申请（就 NCUK 官方目录所能证明的范围而言）。若接受马来西亚校区，则升学路径存在，但需注意页面提到的 aeronautics/astronautics 实验室属于该校区。

**同类模式提醒**：NCUK 会为**特定校区单列合作页**（如 `University of Southampton Malaysia`、`Queen Mary University of London, Malta Campus`、`University of Reading Malaysia`、`Newcastle University Medicine Malaysia`、`University of Birmingham Dubai`）。因此看到一个大学名时要检查它到底挂的是本校还是海外校区。

---

## (3) 英国 top-100 合作院校的 "Accepted NCUK programmes" 列表

以下 8 所全部从 NCUK 官方合作页取到（**官方**）。每所学校的 `Accepted NCUK programmes:` 区块为逐字照录的 bullet 列表。

| 大学 | 合作页 URL | Accepted NCUK programmes（逐字） | 数字级成绩要求 | EAP 等级要求 |
|---|---|---|---|---|
| The University of Manchester | https://www.ncuk.ac.uk/our-universities/university-manchester/ | `International Foundation Year`, `Master's Preparation` | 未查到 | 未查到 |
| University of Birmingham | https://www.ncuk.ac.uk/our-universities/the-university-of-birmingham/ | `International Foundation Year`, `International Year One`, `Master's Preparation` | 未查到 | 未查到 |
| University of Leeds | https://www.ncuk.ac.uk/our-universities/university-of-leeds/ | `International Foundation Year`, `International Art & Design Foundation`, `International Year One`, `Master's Preparation` | 未查到 | 未查到 |
| University of Glasgow | https://www.ncuk.ac.uk/our-universities/university-of-glasgow/ | `International Foundation Year` | 未查到 | 未查到 |
| University of Sheffield | https://www.ncuk.ac.uk/our-universities/the-university-of-sheffield/ | `International Foundation Year`, `International Year One`, `Master's Preparation` | 未查到 | 未查到 |
| Durham University | https://www.ncuk.ac.uk/our-universities/durham-university/ | `International Foundation Year` | 未查到 | 未查到 |
| University of Bristol | https://www.ncuk.ac.uk/our-universities/the-university-of-bristol/ | `International Foundation Year`, `International Year One`, `Master's Preparation` | 未查到 | 未查到 |
| Queen Mary University of London | https://www.ncuk.ac.uk/our-universities/queen-mary-university-london/ | `International Foundation Year`, `International Year One`, `Master's Preparation` | 未查到 | 未查到 |

**另附两个非本校实体（同批抓取，官方）**：

| 实体 | 合作页 URL | Accepted NCUK programmes（逐字） |
|---|---|---|
| University of Southampton Malaysia | https://www.ncuk.ac.uk/our-universities/university-of-southampton-malaysia/ | `International Foundation Year` |
| Queen Mary University of London, Malta Campus | https://www.ncuk.ac.uk/our-universities/queen-mary-university-of-london-malta-campus/ | `International Foundation Year` |

> 注：`/our-universities/queen-mary-university-of-london/`（带 "of"）曾解析/重定向到 Malta 校区页；正确的伦敦本校 slug 是 `queen-mary-university-london`（无 "of"）。

### Manchester 的关键前置资格要求（逐字，本任务在 NCUK 找到的最重要一句）

> "_Please note: Applicants with NCUK programmes are highly valued at The University of Manchester. To ensure a competitive application, the university requires students to have completed the NCUK International Foundation Year (IFY) with a previous qualification deemed equivalent to an AS Level. Further details on these prerequisite qualifications can be [found here](https://www.ncuk.ac.uk/wp-content/uploads/2026/05/NCUK-International-Foundation-Year-The-University-of-Manchester-Prerequisite-Exclusion-2026.pdf)._"

**解读**：Manchester 要求申请者除了 IFY 之外，还必须持有一份被认定为**等同于 AS Level 的既有学历**。细则在需人工下载的 PDF 中。页面本身未给出任何数字级成绩要求（**未查到**）。
**证据等级**：官方 · **URL**：https://www.ncuk.ac.uk/our-universities/university-manchester/

### 各校简介逐字（辅助判断，均为官方）

- Manchester：页面 H1 `The University of Manchester`，标题 `Study Abroad At The University Of Manchester | NCUK`
- Birmingham：blurb "The University of Birmingham, is the original 'redbrick' university, part of the prestigious Russell Group with a rich history and strong global reputation."
- Leeds：blurb "Transform your future by joining the University of Leeds; a leading UK university established in 1904 and one of the largest universities in the UK. Leeds is part of the Russell Group…"
- Glasgow：blurb "The University of Glasgow has been changing the world since its foundation in 1451, consistently ranked as a world top 100 university. Member of the prestigious Russell Group…"
- Sheffield：blurb "The University of Sheffield, located in the vibrant city of Sheffield, is a research-intensive university with a global reputation for excellence and outstanding student experience." … "The University of Sheffield is a member of the Russell Group…"；页面标题 `Progress to the University of Sheffield from an NCUK Qualification`
- Durham：blurb "Durham University offers international students studying with or applying through NCUK an unparalleled study abroad experience…"
- Bristol：blurb "The University of Bristol is a prestigious institution with a powerful reputation for academic excellence and research impact. One of the most popular and successful universities in the UK and globally, Bristol offers more than 600 undergraduate and postgraduate degrees in a wide range of subjects."
- Queen Mary：blurb "Queen Mary University of London is a leading research-intensive university with a difference… The University has over 33,000 students in degree programmes and close to 4,500 members of staff. Queen Mary is a truly global university: over 170 nationalities are represented on its five campuses in London, and the University has a presence in Malta, Paris, Singapore and China."
- Southampton Malaysia：见上文 (2)
- QMUL Malta：blurb "offers you the best of both worlds – a GMC-recognised UK medical degree in sunny and tranquil Malta. A member of the Russell Group, Queen Mary is ranked joint 59th in the world for medicine (QS World University Rankings by Subject 2025)."（位于 Gozo 岛 Victoria，Gozo General Hospital 院内，约 300 名学生，五年制 MBBS；与工程无关）

### (3) 最重要的整体观察

**NCUK 的所有大学合作页面都只列"项目名称"（IFY / International Art & Design Foundation / International Year One / Master's Preparation）。没有任何一个 NCUK 合作页面给出数字级成绩要求、EAP 等级要求或学科模块组合限制。** 这些数字只存在于需人工下载的 PDF 升学指南中。因此本任务在"数字级要求"上一律**未查到**。

---

## (4) "three academic subject modules" 的定义与两个数学模块能否分别计算

### 官方原文（逐字）

来源：`https://www.ncuk.ac.uk/ncuk-programmes/international-foundation-year/`（HTTP 200，H1 `International Foundation Year`）
证据等级：**官方**

> "With the NCUK International Foundation Year, you will study **three academic subject modules** in addition to an **English for Academic Purposes module**. With **12 subject modules available\***, there are numerous combinations to choose from which will alter the types of degree courses and university progression options available to you, ensuring that you get started on the path that's right for you."

**12 个可选模块全名单（逐字）**：
> "Available modules for you to study include **Art & Design, Biology, Business Studies, Chemistry, Computer Science, Economics, Further Maths, Global Studies, Integrated Maths, Physics, Sociology and Technical Maths**."

> 注意：这 12 个具名模块中**有三个是数学** —— `Further Maths`、`Integrated Maths`、`Technical Maths`，三者是并列的独立具名条目。

**额外必修模块（逐字）**：
> "Additionally, you will study the **Skills for Success** module, a fully online module designed to build essential academic and transferable skills in preparation for university study."

> 即 IFY 结构 = **3 门学术学科模块 + EAP + Skills for Success（全在线）**。

**模块可得性脚注（逐字）**：
> "\*NCUK Study Centres vary on which International Foundation Year modules are available. Please contact your chosen Study Centre to enquire what modules are available so that you are best prepared for your studies."

**Ecctis 对标（逐字）**：
> "Ecctis has independently benchmarked the NCUK International Foundation Year as comparable to the following secondary education qualifications: GCE A Level standard (United Kingdom), Hong Kong Diploma of Secondary Education (HKDSE) standard, Advanced Placement standard (United States), Senior Secondary School Certificate of Education standard (Australia) and Alberta High School Diploma standard (Canada)."

**EAP 的关键官方声明（逐字，Key Benefits）**：
> "Benefit from our English for Academic Purposes module which improves your English language skills in reading, writing, speaking and listening and is **accepted by universities in lieu of IELTS**."

> 即 EAP 可**替代 IELTS**。页面上**没有**任何 EAP 的数字等级要求（**未查到**）。

**其他 Key Benefits（逐字）**：
> "Guaranteed\* entry to one of 80+ NCUK University Partners worldwide."
> "Complete the International Foundation Year in as little as 9 months!"

**IFY 入学要求（逐字，针对新生而非升学）**：
> "To be accepted onto the International Foundation Year, applicants must meet the minimum entry criteria shown below:
> #### Academic requirements:
> - Completion of local high school
> - Completion of IGCSE/O Levels/GCSE's with 4 modules at grade 4 or above (normally to include English and Maths)
> _NB: For entry to Study Centres in the UK, students will need to complete a UKVI IELTS test for visa purposes._
> #### English language requirements:
> Students must be able to show English language ability at IELTS 5.0 level or equivalent prior to entry."

**页面其他数字（逐字）**："International Foundation Year students have the option of progressing to one of 6,000+ degree courses at NCUK University Partners in the UK, Australia, New Zealand, the USA, Canada and more!"；"With 135+ Study Centres to choose from"；医学方向另有页面 `https://www.ncuk.ac.uk/study-medicine/`。

### 模块计数问题的答案

**核心负面结论：NCUK 官方 IFY 页面说明了"三门学术学科模块"，但完全没有说明两个数学模块能否分别计数。**

- 官方**没有**任何页面写"两个数学模块不能同时计入三门"。
- 官方也**没有**任何页面写"两个数学模块可以分别计入三门"。
- 唯一的官方相关事实是：`Further Maths`、`Integrated Maths`、`Technical Maths` 在 12 模块名单中是**三个独立的具名模块**，而规则是"study three academic subject modules"（学三门学术学科模块），**未附加任何"数-学模块互斥"的限定条件**。
- 官方脚注同时说明**各 Study Centre 提供的模块不同**，需向所在 Study Centre 确认 —— 这意味着最终解释权可能落在 Study Centre 层面，而官方网页上没有统一裁决。

**结论**：该问题在 NCUK 公开官网上 **未查到** 明确答案。学生模式（Technical Maths + Further Maths + Physics）在字面上是"三个具名学术模块"的组合，与官方文本不冲突；但**官网上没有任何一句可以引用为"两个数学模块可以分别计算"的权威依据**。要拿到确定答案，只能：(a) 读需人工下载的 IFY Programme Overview PDF；(b) 直接向 Study Centre / NCUK 询问。

**尝试过但无结果的外部检索**：`NCUK "three academic subject modules" two mathematics modules`、`site:ncuk.ac.uk` 相关查询 — 均未产出任何解释 NCUK 模块计数规则的来源。

---

## (5) PDF / 需人工下载 清单

以下 PDF **均无法用 web_fetch 读取**（`Error: unsupported content type "application/pdf"`），全部标记 **需人工下载**。

| # | PDF URL | NCUK 页面如何描述它 | 状态 |
|---|---|---|---|
| 1 | https://www.ncuk.ac.uk/wp-content/uploads/2026/09/University-Progression-Routes-September-2026.pdf | 官方 Our Universities 页逐字："Looking for information on NCUK programme acceptance at NCUK University Partners? Check out our easy-to-use [university progression guide here]." → **这是权威的逐课程/逐校录取要求表** | **需人工下载** |
| 2 | https://www.ncuk.ac.uk/wp-content/uploads/2026/05/NCUK-International-Foundation-Year-The-University-of-Manchester-Prerequisite-Exclusion-2026.pdf | Manchester 页面逐字："Further details on these prerequisite qualifications can be found here." 搜索结果标题："University of Manchester - Prerequisites for International Foundation Year (IFY)" → **Manchester AS-Level 前置资格细则** | **需人工下载** |
| 3 | https://www.ncuk.ac.uk/wp-content/uploads/2026/06/International-Foundation-Year-Programme-Overview-June-2026.pdf | IFY 页面称其包含 "information on the aims of the programme, modules offered, course structures, assessment methods and more" → **可能包含模块计数规则** | **需人工下载** |
| 4 | https://www.ncuk.ac.uk/wp-content/uploads/2026/09/NCUK-International-Foundation-Year-Entry-Requirements-September-2026.pdf | IFY 页面称其为 "our full list of country-specific entry requirements" | **需人工下载** |
| 5 | https://www.ncuk.ac.uk/wp-content/uploads/2020/09/NCUK-and-The-University-of-Sheffield-Leaflet-2021-Portrait.pdf | 搜索结果描述 "Teaching and research excellence." | **需人工下载** |

---

## 结论

1. **NCUK Course Finder（`/ncuk-entry-directory`）无法提供课程级录取要求。** 目录入口 302 到 `/ncuk-programmes/ncuk-entry-directory/`，HTTP 200，但页面只有两个筛选下拉框、**零课程行**；课程表由 JavaScript 注入。测试的 `?university=`、`?country=uk`、`?ed_uni_header=`、`?ed_prog_header=` 四个查询串**全部被服务端忽略**，返回的 HTML 与裸页面相同。**课程级要求 = 未查到**，只能通过需人工下载的升学指南 PDF 获取。

2. **南安普顿问题已有定论：英国南安普顿大学不是 NCUK 的独立合作院校。** 四项独立官方证据一致 —— (a) `/our-universities/university-of-southampton/` 返回与马来西亚校区页**逐字节相同**的内容；(b) Course Finder 大学下拉框中唯一条目是 `University of Southampton Malaysia`；(c) IFY 官方页 80+ 所升学合作院校名单中只有 `University of Southampton Malaysia`；(d) 首页合作伙伴 logo 轮播中完全没有 Southampton，且 `/our-universities/the-university-of-southampton/` **HTTP 404**。马来西亚校区接受的 NCUK 项目仅为 `International Foundation Year`。**对全程英国就读的学生而言，南安普顿不能作为 NCUK 升学目标。**

3. **八所英国 top-100 合作院校的 "Accepted NCUK programmes" 已全部取到**（Manchester、Birmingham、Leeds、Glasgow、Sheffield、Durham、Bristol、Queen Mary）。**共同点是只列项目名称，无任何数字级成绩或 EAP 要求。** 其中最值得注意的是 **Manchester 的额外要求**：除 IFY 外还须持有一份被认定为等同于 **AS Level** 的既有学历。

4. **两门数学模块能否分别计算：未查到。** NCUK 官方仅说明"study three academic subject modules"，并列出 12 个具名模块（含三个并列的数学模块 `Further Maths` / `Integrated Maths` / `Technical Maths`），**既未允许也未禁止**两个数学模块并列计数。官方同时声明各 Study Centre 开设模块不同，须向 Study Centre 确认。确定答案需读 IFY Programme Overview PDF 或直接询问 NCUK/Study Centre。

5. **EAP 的确定信息只有一条（官方）**：EAP 被大学"**accepted by universities in lieu of IELTS**"（替代雅思）。**EAP 的具体等级要求：未查到。**

6. **NCUK 官方合作页的价值边界**：它们能可靠回答"某校是否接受 NCUK 以及接受哪些项目"，但**不能**回答"要多少分"。所有数字级答案都指向那份需人工下载的升学指南 PDF。

---

## 未查到与失败来源

### 未查到（已穷尽官方渠道仍无数字）

| 项 | 状态 | 说明 |
|---|---|---|
| NCUK Course Finder 的课程级录取要求 | **未查到** | JS 注入，GET 参数被忽略 |
| Manchester / Birmingham / Leeds / Glasgow / Sheffield / Durham / Bristol / Queen Mary 的数字级成绩要求 | **未查到** | 官方合作页均未给出 |
| 上述各校的 EAP 等级要求 | **未查到** | 官方合作页均未给出；仅有"EAP 替代雅思"的总声明 |
| "两个数学模块能否分别计算"的官方规则 | **未查到** | NCUK 官网无此说明 |
| IFY 的 EAP 数字等级要求 | **未查到** | 页面仅声称 EAP 可替代 IELTS |
| 南安普顿马来西亚校区的数字级成绩要求 | **未查到** | 合作页未给出 |

### 失败来源清单

**PDF（需人工下载，web_fetch 返回 `Error: unsupported content type "application/pdf"`）**
1. https://www.ncuk.ac.uk/wp-content/uploads/2026/09/University-Progression-Routes-September-2026.pdf — 最权威的逐校/逐课程录取要求表
2. https://www.ncuk.ac.uk/wp-content/uploads/2026/05/NCUK-International-Foundation-Year-The-University-of-Manchester-Prerequisite-Exclusion-2026.pdf
3. https://www.ncuk.ac.uk/wp-content/uploads/2026/06/International-Foundation-Year-Programme-Overview-June-2026.pdf
4. https://www.ncuk.ac.uk/wp-content/uploads/2026/09/NCUK-International-Foundation-Year-Entry-Requirements-September-2026.pdf
5. https://www.ncuk.ac.uk/wp-content/uploads/2020/09/NCUK-and-The-University-of-Sheffield-Leaflet-2021-Portrait.pdf

**JS-only / 参数无效（HTTP 200 但零数据）**
6. https://www.ncuk.ac.uk/ncuk-entry-directory — 302 到下方地址
7. https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/ — 只有筛选框，零课程行
8. https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?university=the-university-of-manchester — 参数被忽略
9. https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?country=uk — 参数被忽略
10. https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/?ed_uni_header=1&ed_prog_header=1 — 参数被忽略

**404（NCUK 官网不存在的 slug，记录了错误的 slug 猜测）**
11. https://www.ncuk.ac.uk/our-universities/the-university-of-manchester/ — 404（正确：`university-manchester`）
12. https://www.ncuk.ac.uk/our-universities/university-of-manchester/ — 404
13. https://www.ncuk.ac.uk/our-universities/university-of-sheffield/ — 404（正确：`the-university-of-sheffield`）
14. https://www.ncuk.ac.uk/our-universities/the-university-of-southampton/ — 404（**这是南安普顿问题的证据之一**）

**重定向 / slug 歧义（非 404，但需注意）**
15. https://www.ncuk.ac.uk/our-universities/queen-mary-university-of-london/ — 解析/重定向到 Malta 校区页；伦敦本校正确 slug 为 `queen-mary-university-london`
16. https://www.ncuk.ac.uk/our-universities/?country=uk — HTTP 200 但 `?country=` **不做服务端筛选**，页面仍列出 University of Alberta（加拿大）、The University of Auckland（新西兰）、AUT（新西兰）、University of Birmingham Dubai（UAE）等非英国院校

**官网搜索页不返回结果列表**
17. https://www.ncuk.ac.uk/?s=Sheffield — HTTP 200，标题 `You searched for Sheffield - NCUK`，但**没有任何搜索结果列表**，只渲染了首页模板（该模板的 logo 轮播反而是最有用的 slug 索引，见证据 D）

**搜索引擎查询无有效来源**
18. `site:ncuk.ac.uk our-universities Manchester` — 无有效结果
19. `site:ncuk.ac.uk our-universities Sheffield` — 无有效结果
20. `site:ncuk.ac.uk our-universities Queen Mary` — 无有效结果
21. `NCUK "three academic subject modules" two mathematics modules` — **无任何解释 NCUK 模块计数规则的来源**

**第三方/中介来源（已发现但未采用，非官方）**
22. https://www.basilpaterson.co.uk/wp-content/uploads/2024/02/ncuk-international-foundation-year-brochure.pdf — 中介
23. https://oxbridge.gitbook.io/otc/academic-programs/ncuk-international-foundation-year — 中介
24. https://edubridge-education.com/ncuk-ify/ — 中介

### 附：本任务抓取到的 NCUK 页面其他可用信息

**首页统计块（官方，但为营销口径、非约束性）**："Progress to one of 80+ universities – including 11 in the QS World Top 100"；"…university partners, with 22 ranked in the QS World Top 200"。
**首页计数器缺陷（注意勿引用）**：首页四个 "Why study with NCUK?" 计数器渲染为 `0`（JS 未填充），逐字呈现为 "##### 0 **students** progressed to world-leading universities"、"##### 0 **Study Centres** in **40+ countries**"、"##### 0 **nationalities**…"、"##### 0 **of students** achieve a **2:1 or higher** at university"、"##### 0 **university partners**, with **22 ranked in the QS World Top 200**"。这是页面缺陷，不代表真实数字。
**学生证言（官方，逐字）**：Lionel Correia, University of Bristol, `BEng Aerospace Engineering` — "I am really excited to start studying in a new country, especially in a beautiful city like Bristol…"；Agga Kaung Myat, The University of Manchester, `BSc (Hons) Biomedical Sciences` — "NCUK was the best choice and a new Study Centre had just happened to open in Yangon city."
**NCUK 联系方式（官方，逐字）**：NCUK, Spaces Peter House, Oxford Street, Manchester, M1 5AN, United Kingdom；`NORTHERN CONSORTIUM UK LIMITED` Trading as NCUK，Company number: 04842064。社交：facebook.com/NCUKofficial、instagram.com/NCUKofficial、linkedin.com/school/940252、x.com/NCUKTogether、weibo.com/u/2644765313、youtube.com/channel/UC6jeGYzOHriBxdFxGMLGPFg、tiktok.com/@ncukofficial。另有 NCUK 中国网站 http://www.ncuk.cn/ 、Global Hub、Our Policies、Contact Us。

### 附：NCUK 官方合作院校 slug 权威索引（从 IFY 页 University Progression 链接逐字提取，80+ 条）

University of Alabama at Birmingham → `the-university-of-alabama-at-birmingham`；University of Alberta → `university-of-alberta`；Aston University → `aston-university`；The University of Auckland → `the-university-of-auckland`；Auckland University of Technology (AUT) → `auckland-university-of-technology-aut`；University of Birmingham → `the-university-of-birmingham`；University of Birmingham Dubai → `university-of-birmingham-dubai`；University of Bradford → `the-university-of-bradford`；University of Bristol → `the-university-of-bristol`；Brock University → `brock-university`；Brunel University of London → `brunel-university-london`；University of Canterbury → `university-of-canterbury`；Cardiff University → `cardiff-university`；University for the Creative Arts → `university-for-the-creative-arts`；De Montfort University → `de-montfort-university`；Drew University → `drew-university`；University of Dundee → `university-of-dundee`；Durham University → `durham-university`；University of Essex → `university-of-essex`；University of Exeter → `university-of-exeter`；Falmouth University → `falmouth-university`；George Mason University → `george-mason-university`；University of Glasgow → `university-of-glasgow`；Harper Adams University → `harper-adams-university`；Heriot-Watt University → `heriot-watt-university`；University of Huddersfield → `the-university-of-huddersfield`；Illinois State University → `illinois-state-university`；International College of Liberal Arts at Yamanashi Gakuin University → `international-college-of-liberal-arts-at-yamanashi-gakuin-university`；Keele University → `keele-university`；University of Kent → `university-of-kent`；Kingston University London → `kingston-university`；University of Lancashire → `university-of-lancashire`；Lancaster University → `lancaster-university`；The University of Law → `the-university-of-law`；University of Leeds → `university-of-leeds`；Leeds Beckett University → `leeds-beckett-university`；University of Leicester → `university-of-leicester`；Lincoln University → `lincoln-university`；University of Liverpool → `university-of-liverpool`；Liverpool Hope University → `liverpool-hope-university`；Liverpool John Moores University → `liverpool-john-moores-university`；The University of Manchester → `university-manchester`；Manchester Metropolitan University → `manchester-metropolitan-university`；Massey University → `massey-university`；NABA – Nuova Accademia Di Belle Arti → `naba-nuova-accademia-di-belle-arti`；Newcastle University → `newcastle-university`；Newcastle University Medicine Malaysia → `newcastle-university-medicine-malaysia`；The University of Newcastle, Australia → `the-university-of-newcastle-australia`；Northumbria University, Newcastle → `northumbria-university-newcastle`；UNSW Sydney → `university-of-new-south-wales`；Oregon State University → `oregon-state-university`；University of Otago → `university-of-otago`；University of Ottawa → `university-of-ottawa`；Oxford Brookes University → `oxford-brookes-university`；University of Portsmouth → `university-of-portsmouth`；Queen Mary University of London → `queen-mary-university-london`；Queen Mary University of London, Malta Campus → `queen-mary-university-of-london-malta-campus`；Queen's University Belfast → `queens-university-belfast`；Queensland University of Technology (QUT) → `queensland-university-of-technology-qut`；University of Reading → `university-of-reading`；University of Reading Malaysia → `university-of-reading-malaysia`；University of Regina → `university-of-regina`；RMIT University → `rmit-university`；Robert Gordon University → `robert-gordon-university`；University of Salford → `the-university-of-salford`；University of Sheffield → `the-university-of-sheffield`；Sheffield Hallam University → `sheffield-hallam-university`；**University of Southampton Malaysia → `university-of-southampton-malaysia`**；State University of New York (SUNY) at Oswego → `state-university-of-new-york-suny-oswego`；St. George's University, Grenada → `st-georges-university`；Suffolk University → `suffolk-university`；University of Sussex → `university-of-sussex`；Swansea University → `swansea-university`；Swinburne University of Technology → `swinburne-university-of-technology`；Toronto Metropolitan University → `toronto-metropolitan-university`；Victoria University of Wellington → `victoria-university-of-wellington`；University of Waikato → `university-of-waikato`；University of the West of England – UWE Bristol → `university-of-the-west-of-england-uwe-bristol`；The University of Western Australia → `the-university-of-western-australia`；University of Westminster → `university-of-westminster`；University of York → `university-of-york`。

> 在这些官方链接中**没有**独立的 "University of Southampton" —— 只有 `university-of-southampton-malaysia`。

### 附：NCUK 官方 "Our Universities" 归档页原文（官方）

URL：https://www.ncuk.ac.uk/our-universities/?country=uk · 标题 `Universities Archive - NCUK` · HTTP 200

> "## NCUK University Partners"

> "NCUK's University Partners span the globe, offering students access to a world-class education with a commitment to excellence and innovation. With 80+ universities to choose from, you have the opportunity to create your own unique study abroad path in destinations such as the UK, Australia, Canada, New Zealand, the USA, and more!"

> "Looking for information on NCUK programme acceptance at NCUK University Partners? Check out our easy-to-use [university progression guide here](https://www.ncuk.ac.uk/wp-content/uploads/2026/09/University-Progression-Routes-September-2026.pdf)."

**该页筛选 facets（逐字）**：NCUK Programmes = International Foundation Year、International Art & Design Foundation、International Year One、Accounting and Finance、Business Management、Computer Science、**Electrical and Electronic Engineering**、Events Management、Law、International Year Two in Business Management、Master's Preparation。
> 注意：`Electrical and Electronic Engineering` 作为可筛选的 NCUK 项目/路径名称出现 —— 与学生目标方向一致。

**该页收录的卡片条目（逐字摘录）**：University of Alberta — Canada — "Ranked 4th in Canada and 96th in the world (QS World University Rankings 2027)"；Aston University — UK — "Ranked 21st in the UK for graduate earnings (Longitudinal Education Outcomes 2026)"；The University of Auckland — New Zealand — "Ranked 1st in New Zealand and 67th in the world (QS World University Rankings 2027)"；Auckland University of Technology (AUT) — New Zealand — "Ranked 1st in New Zealand and in the Top 100 in the world for international outlook (Times Higher Education World University Rankings 2024)"；Bangor University — UK — "5 Star rating for Employability (QS Star Rating System)"；University of Birmingham — UK — "Ranked 11th in the UK and joint 68th in the world (QS World University Rankings 2027)" → `/our-universities/the-university-of-birmingham/`；University of Bradford — UK — "Distance Learning MBA ranked 1st in the world for value for money and teaching related to ethics and climate solutions (Financial Times Online MBA Rankings 2024)"；University of Bristol — UK — "Ranked 8th in the UK and 57th in the world (QS World University Rankings 2027)" → `/our-universities/the-university-of-bristol/`。
