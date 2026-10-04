# q6-alternatives-d：NCUK 合作大学中"工科真强"且 IFY 录取线 ≤ ABB(128) 的英国大学

> **任务**：找出 NCUK 合作英国大学中工科实力确实强、并接受 NCUK IFY 学生以 ABB(128 UCAS) 或更低成绩入学的院校。
> **指定批次**：Queen's University Belfast (QUB)、Cardiff University、Newcastle University、Queen Mary University of London (QMUL)。
> **附加核实**：中介声明"Brunel 土木 = BBB、Brunel 土木与环境 = AAB、Brunel 计算机系统工程 = ABB"。
> **学生档案（给定，未复核）**：Technical Maths / Further Mathematics / Physics + EAP；NCUK 换算 A\*=56、A=48、B=40、C=32、D=24；上限 BBB(120)–ABB(128)；首选 EEE，其次机械/土木/航空/材料；全英就读；不能学化学；不读商科。

**证据标签约定**：官方 = 大学官网 / NCUK 官网（含 NCUK 中国官网 ncuk.cn）；官方媒体 = 正规新闻媒体；中介 = 留学中介/机构博客；网友 = 论坛、社交平台；未查到 = 任何来源都未见到该数字。

**非常关键的总体发现（先说结论的事实基础）**：
- **本次未能在任何官方来源看到 QUB / Cardiff / Newcastle / QMUL 这四校"电气与电子工程(EEE)"专业对 NCUK IFY 的逐专业分数要求。** 四校官网要么 403（QUB、Cardiff、QMUL 全域），要么页面在"入学要求"处被截断（Newcastle）。唯一给出四校 EEE 具体 IFY 分数的是中介文章（QMUL = ABB；见 2.4）。
- NCUK 的权威逐课程分数表只存在于 PDF（本工具无法读取，见第 5 节），因此**四校 EEE 的官方 IFY 分数线一栏一律记为 未查到**，不做任何推算。

---

## 1. 背景：已核实的官方基准

### 1.1 NCUK 合作校身份（官方，ncuk.ac.uk，全部 HTTP 200）
- 大学总表：<https://www.ncuk.ac.uk/our-universities/> —— 列表在 HTML 中（筛选器为 JS GET 参数）。**Brunel University of London、Cardiff University、Newcastle University、Queen Mary University of London、Queen's University Belfast 均在册。**
- <https://www.ncuk.ac.uk/our-universities/queens-university-belfast/> —— "Accepted NCUK programmes:" **International Foundation Year；International Year One**。页面称 QUB 为 Russell Group 成员（页面在 Rankings/Courses 段之前被截断）。
- <https://www.ncuk.ac.uk/our-universities/cardiff-university/> —— "Accepted NCUK programmes:" **International Foundation Year；International Art & Design Foundation**。
- <https://www.ncuk.ac.uk/our-universities/newcastle-university/> —— "Accepted NCUK programmes:" **International Foundation Year（仅此一项）**。
- <https://www.ncuk.ac.uk/our-universities/queen-mary-university-of-london/> **重定向**至 <https://www.ncuk.ac.uk/our-universities/queen-mary-university-of-london-malta-campus/> —— 该 slug 是 **QMUL 马耳他校区**（医学，页面称 "joint 59th in the world for medicine (QS World University Rankings by Subject 2025)"），接受的 NCUK 课程为 International Foundation Year。**QMUL 伦敦本部（Mile End）的 NCUK 合作专页未找到**。
- 课程查找器 <https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/> —— 大学下拉框可渲染（JS），但**课程行由 JS 注入，无法取到任何逐课程分数**（GET 参数被忽略）。

### 1.2 NCUK 国际大一（IYOne）EEE —— 官方最低线（重要参照）
来源（官方）：<https://www.ncuk.ac.uk/ncuk-programmes/international-year-one/electrical-electronic-engineering/>
逐字转录：
> "Have achieved at least an NCUK EAP 'D' grade or hold an acceptable equivalent alternative English language qualification."
> "obtained a minimum of 48 NCUK foundation points with a D in at least 2 subjects following study of the NCUK International Foundation Year programme (students must have a grade 'D' in both Maths and Physics)."

该页列出的升学合作校为：Aston, Auckland, AUT, Birmingham, Bristol, Canterbury, Huddersfield, Kent, Lancaster, Leeds, Liverpool, Liverpool John Moores, Manchester Metropolitan, Massey, Northumbria, RMIT, Salford, Sheffield, Sheffield Hallam, Swansea, Victoria University of Wellington, Waikato, UWE Bristol。
**QUB、Cardiff、Newcastle、QMUL 均不在该 IYOne-EEE 升学名单内**（官方）。这是本批次四校与"低门槛 EEE 通道"之间最重要的结构性差异。

### 1.3 QS 2027 排名（官方，NCUK 中国官网）
来源（官方，NCUK 中国官网 ncuk.cn，2026年06月22日）：<https://www.ncuk.cn/ncukhezuodaxuezai2027nianqsshijiedaxuepaimingzhongbiaoxianliangyan/>
逐字转录：
> "NCUK热烈祝贺其合作大学在**2027年QS世界大学排名**中取得卓越成绩。"
> "10所合作大学跻身全球百强，21所进入全球前200名"
> "伦敦大学玛丽女王学院（第103位）"
> "纽卡斯尔大学（第149位）"
> "贝尔法斯特女王大学（并列第174位）"
> "卡迪夫大学（并列第179位）"
> "贝尔法斯特女王大学：上升25位至并列第174名"
> "伦敦布鲁内尔大学：上升32位至第353名"
> NCUK招生与升学转化总监 Ben Bilverstone："我们为合作大学在今年QS世界大学排名中取得的优异成绩感到无比自豪……"
> "目前，NCUK与全球80多所大学建立了合作关系"

交叉印证（中介，2027 QS 英国大学完整表）：<https://www.jiemo.net/news/h5-show-2783499.html>（芥末留学，2026-06-18）——贝尔法斯特女王大学 174（2026:199，↑25）；卡迪夫大学 179（181，↑2）；伦敦玛丽女王大学 103（110，↑7）；纽卡斯尔大学 149（137，↓12）；布鲁内尔大学 353（385，↑32）。
另一中介表 <https://www.lcig.net/ComplexNews/Detail/1887>（柳橙国际，2026-06-23）给出相同的 110→103、137→149、199→174 对照，卡迪夫行因截断未见校名（与 179 一致）。
**Newcastle 官网自述印证（官方）**：<https://www.ncl.ac.uk/international/country/china/> —— "Global Top 150 University QS World University Rankings 2027"。

---

## 2. 逐校核查

### 2.1 Newcastle University（QS 2027 = 149）

**(a) QS 2027**：149 —— 官方（ncuk.cn 上表）；Newcastle 官网亦自述 "Global Top 150 University QS World University Rankings 2027"（官方）。

**(e) NCUK 合作身份**：**是**，且官方明确表态。<https://www.ncl.ac.uk/international/country/china/>（官方）逐字：
> "Newcastle University is proud to be an NCUK University partner, welcoming students who have successfully completed the NCUK International Foundation year as a recognised pathway to our degree programmes."

同页中国本科生录取口径（官方，逐字）：
> "a **75% - 80% average** at **other institutions** as comparable to ABB at A level"
> "with ABB typically the minimum required"
（该页另给通用 IELTS 6.5；Russell Group。）

**(b) 具体工科学位与 IFY 要求**
- **Electrical and Electronic Engineering BEng Honours, UCAS H607（2026 入学）** <https://www.ncl.ac.uk/undergraduate/2026/degrees/h607/>
  逐字："Typical entry requirements A-Level: ABB / IB: 32 points"；"Our professionally accredited Electrical and Electronic Engineering BEng Honours degree will prepare you for a career as a professional engineer."；FT International £30,700。
  **NCUK IFY 具体分数：未查到**（页面在入学要求/其他资格段被截断，未见 IFY 分数字样）。
- **Engineering with Foundation Year BEng Honours, UCAS H101（2027 入学）** <https://www.ncl.ac.uk/undergraduate/degrees/h101/>
  逐字：A-Level ABB，IB 32；"Successful completion of the Foundation Year guarantees a place onto one of the following BEng degree programmes:" 其中包含 "Electrical and Electronic Engineering BEng Honours (H607)"。
- 中介补充（新东方，2026.09.01）<https://liuxue.xdf.cn/news/tianjin_8050882.shtml>：NCUK IFY 被接受，工程（含 foundation year）A-Level 等价 ABB，NCUK 要求"通常在AAA至BBB之间"，IELTS 6.5（5.5/6.0），逐案审核。**无 NCUK 分数字表 → 不足以作为逐专业依据**，但与官方 ABB 口径一致。
- INTO/Newcastle International Study Centre 通道（官方通道方，**非 NCUK**）<https://www.nclisc.com/en/courses/beng-hons-electrical-and-electronic-engineering-with-international-foundation-year>
  逐字："World top 200 for Electrical and Electronic Engineering (QS World University Rankings by Subject 2026)."；"Professionally accredited by the Institution of Engineering and Technology (IET)."；英语 "IELTS 5.5 with 5.5 in all skills or equivalent"；学术 "Completion of 12 years of schooling"。

**(c) EAP / 英语**：NCUK EAP 要求 —— **未查到**（官方与中介均未给出 H607 的 EAP 等级）。官方通用英语参考 IELTS 6.5；NCUK 端通用门槛见 1.3 之外的 2.4 中介口径（UKVI IELTS 5.0 / 4.5 为 IFY 入学门槛，不是大学录取门槛）。

**(d) 工科实力证据**
- 官方逐字："Our professionally accredited Electrical and Electronic Engineering BEng Honours degree…"（<https://www.ncl.ac.uk/undergraduate/2026/degrees/h607/>）。**未点名 IET** —— 点名 IET 的是 ISC 页面（"Professionally accredited by the Institution of Engineering and Technology (IET)"）。
- QS 学科（EEE）世界前 200（来源同上 ISC 页面，官方通道方口径）。
- REF 2021 具体结果：**未查到**。

**风险提示**：Newcastle 接受的 NCUK 课程**仅 International Foundation Year**；官方未公布 H607 的 IFY 分数线，需向校方确认。

---

### 2.2 Queen's University Belfast（QS 2027 = 174）

**(a) QS 2027**：并列 174 —— 官方（ncuk.cn 上表，且为该文点名的"上升25位"院校之一）。QUB 官网相关新闻页存在但 **403 CloudFront**：<https://www.qub.ac.uk/News/Allnews/2026/qs-rankings-2027.html>（搜索标题为 "Queen's climbs 25 places, securing its highest percentile ever in prestigious university rankings"）。

**(e) NCUK 合作身份**：**是**。官方专页 <https://www.ncuk.ac.uk/our-universities/queens-university-belfast/> 列 International Foundation Year 与 International Year One；页面称 Russell Group（官方）。
**注意**：官方 "International Year One in Electrical & Electronic Engineering" 升学名单中**没有 QUB**（见 1.2）。

**(b) 具体工科学位与 IFY 要求**
- **QUB 域（qub.ac.uk）在本会话全部 403**（CloudFront "Request blocked"），因此**电气与电子工程 BEng 的官方 NCUK IFY 分数：未查到**。
- 中介（新东方，2026.09.02）<https://liuxue.xdf.cn/news/tianjin_8051220.shtml>（另有同日同源文 <https://liuxue.xdf.cn/news/tianjin_8051235.shtml>）逐字/转述：
  > "NCUK官网Entry Directory显示，贝尔法斯特女王大学共有124个本科专业接受NCUK国际预科成绩申请"
  > Aerospace Engineering："EAP要求为总分C级，单项D级。IFY总分128分，等级要求为ABB，且必须包含数学以及物理、生物、化学或进阶数学中的至少一门"
  > Aerospace Engineering with a Year in Industry："总分144分，等级要求为AAA"
  > Actuarial Science and Risk Management："152分…AAA…EAP总分B、单项D"
  > Advanced Accounting with Placement："128分…AAB…EAP B/D"
  > Computing and Information Technology with Year in Industry："136分…AAB…EAP C/D"
  > 换算口径："A对应80%及以上（56分），A对应70%-79%（48分），B对应60%-69%（40分）"
  > NCUK IFY 入学语言门槛："UKVI雅思5.0，各单项不低于4.5"；需完成12年学制、GCSE 数学 4/C；最低年龄16
  > 学费（2026）：商科与人文社科方向国际预科 £13,000；工程方向国际大一 £20,550
  > "贝尔法斯特女王大学与NCUK的合作关系于2021年建立"
  **QUB 电子电气工程（EEE）的 IFY 分数：未查到**（该文未列 EEE 行）。
- **非 NCUK 的官方通道方数据（可作强度与低门槛旁证）**：INTO Queen's ISC <https://www.qubisc.com/en/courses/beng-electrical-and-electronic-engineering-with-international-year-one>（官方通道方 = 官方）
  逐字："Two 'D' grades from a UK A-level board, a recognised foundation, first year of an overseas university degree programme or equivalent with good grades. Students will be expected to have studied Mathematics and preferably Physics at this level."
  逐字："IELTS 5.5 (with a minimum of 5.5 in all subskills) or equivalent."
  逐字："Progress to a degree accredited by the Institution of Engineering and Technology at a university ranked in the world top 200 for Electrical and Electronic Engineering (QS World University Rankings by Subject 2026)."
  费用：学费 £23,750 + 注册费 £275；"3terms Pathway + 2years Degree"；年龄 16（9月入学须在当年12月31日前满17）。

**(c) EAP / 英语**：主校区的 EEE EAP 要求 **未查到**。旁证：中介称 QUB 航空航天 EAP 总分 C、单项 D（2.2 (b)）；官方 INTO 通道为 IELTS 5.5(5.5)。

**(d) 工科实力证据**
- **IET 认证（官方）**：INTO Queen's ISC 页面逐字称升读的学位 "accredited by the Institution of Engineering and Technology"。
- **QS 学科 EEE 世界前 200（官方）**：同上页面逐字 "ranked in the world top 200 for Electrical and Electronic Engineering (QS World University Rankings by Subject 2026)"。
- 学院研究平台 ECIT（电子、通信与信息技术研究所）、CSIT、SIVS 等（来源为中介博客 <https://liuxue.xdf.cn/blog/jinfuyu/blog/5191364.shtml>，**中介**，且该文实为 MPhil 介绍，仅可作弱旁证）。
- REF 2021 具体结果：**未查到**（QUB 官方 REF 页 <https://www.qub.ac.uk/Research/our-research/research-excellence-framework-ref/> 亦因域 403 不可读）。

---

### 2.3 Cardiff University（QS 2027 = 179）

**(a) QS 2027**：并列 179 —— 官方（ncuk.cn 上表）。
NCUK 中国官网 Cardiff 专页 <https://www.ncuk.cn/university/kadifudaxue/>（官方）逐字：
> "卡迪夫大学的研究影响力排名全英国第二，也是排名英国前30名，全世界前160名的大学之一。一旦获得卡迪夫大学的学位，世界大门就会向你打开。……96%的毕业生在毕业后就很快找到了工作或开始进修。"
（该页未给任何工程录取分数；排名表述明显为旧数据。）

**(e) NCUK 合作身份**：**是**。官方专页 <https://www.ncuk.ac.uk/our-universities/cardiff-university/>（International Foundation Year；International Art & Design Foundation）。ncuk.cn 的 Cardiff 卡片标签显示为"国际艺术与设计基础课程"（官方，ncuk.cn）。

**(b) 具体工科学位与 IFY 要求**：**未查到**。
- cardiff.ac.uk 全域在本会话被 Cloudflare 拦截。工程本科课程页 <https://www.cardiff.ac.uk/study/undergraduate/courses/2026/electrical-and-electronic-engineering-beng> 返回 403 "Just a moment..."；<https://www.cardiff.ac.uk/study/international/entry-requirements> 同样 403。**因此 Cardiff 工程专业的 NCUK IFY 分数与 EAP 要求均未查到。**
- 一篇标题为"NCUK预科成绩达标就能直通卡迪夫大学？2026入学要求与专业差异全解析"的中介文（新东方，2026.09.02）<https://liuxue.xdf.cn/news/tianjin_8051278.shtml>，正文实际描述的是**卡迪夫大学国际学习中心（Cardiff ISC）的预科通道，而非 NCUK 分数**，逐字：
  > "卡迪夫大学对NCUK预科申请者的学术标准为：高二完成且包括相关科目在内的平均成绩达到70%，或高三完成且包括相关科目在内的平均成绩达到65%。"
  > "标准预科方向的英语要求为UKVI学术类雅思总分5.0分，其中写作不低于5.0分，其他各单项不低于4.0分。"
  > 医学/牙医/药学/医学药理学/验光："UKVI雅思总分6.0分，写作不低于6.0分，其他各单项不低于5.5分…需参加UCAT情景判断测试"；新闻/传媒同为 6.0。
  > 加速预科（22周）："UKVI雅思总分5.5分，各单项不低于5.0分"。
  > "卡迪夫大学预科课程设四大衔接方向：人文法律与社会科学、商科金融与会计、工程物理与建筑、健康医学与生命科学。"
  > "从NCUK国际预科升入卡迪夫大学本科的国际学生，可申请NCUK国际奖学金，金额为2,000英镑。"
  → 即：该文**未给出任何工程专业的 NCUK IFY 分数**（"工程物理与建筑"方向的分数未列）。

**(c) EAP / 英语**：**未查到**（仅上述非 NCUK 通道的 IELTS 5.0/5.5/6.0 口径）。

**(d) 工科实力证据**：**未查到**可引用的逐字证据（无 IET/IMechE/ICE 认证原文、无 REF 2021 结果、无学科排名原文）。仅有 NCUK 中国官网"研究影响力全英第二"这一机构级、未标注年份的表述（官方，ncuk.cn）。
记录在案但不可读（PDF，需人工下载）：<https://www.cardiff.ac.uk/__data/assets/pdf_file/0008/908729/Engineering-UG-Programme-English.pdf>（Cardiff 工程本科英语要求文件）。

---

### 2.4 Queen Mary University of London（QS 2027 = 103）

**(a) QS 2027**：103 —— 官方（ncuk.cn 上表逐字 "伦敦大学玛丽女王学院（第103位）"）；中介表 110→103（↑7）。

**(e) NCUK 合作身份**：**部分确认**。
- ncuk.ac.uk 上的 QMUL slug **重定向到马耳他校区页**（<https://www.ncuk.ac.uk/our-universities/queen-mary-university-of-london-malta-campus/>），因此**伦敦本部校区的 NCUK 合作专页未查到**。
- 但 ncuk.cn 的 NCUK 大学列表中**确有"伦敦玛丽女王大学"**（官方，<https://www.ncuk.cn/ncukdaxue/>，专页 <https://www.ncuk.cn/university/lundunmalinuwangdaxue/>）。
- 中介（新东方，2026.09.01）逐字：> "该校是NCUK（英国北方大学联合会）的创始成员之一"。

**(b) 具体工科学位与 IFY 要求**
- **官方（QMUL 测试域名 www-test.qmul.ac.uk，内容与官网一致）** <https://www-test.qmul.ac.uk/undergraduate/coursefinder/courses/2026/electrical-and-electronic-engineering/>
  - H600 BEng (Hons) Electrical and Electronic Engineering（3年）逐字："A-Level | Grades ABB at A-Level. This must include A-Level Mathematics. A second science subject at A-Level, from Physics, Computer Science, Electronics, Biology, Chemistry, Physics or Further Mathematics."；IB "minimum of 32 points overall, including 6,5,5 from three Higher Level subjects. This must include Mathematics at Higher Level."；GCSE "Minimum five GCSE passes including English at grade C or 4."
  - 补录(Clearing)口径逐字："A-levels: BBB including B or above in Mathematics, and one of the following: B or above in Biology, Chemistry, Physics, Electronics, Computer Science, Computing or Further Mathematics."
  - HHX0 BEng Electronic Engineering with Foundation（4年）逐字："A-Level | Grades BCC at A-Level. This must include grade B in A-Level Mathematics."；IB "minimum of 26 points overall, including 5,4,4 from three Higher Level subjects. This must include 5 in Higher Level Mathematics."；学费 £25,500（H600 为 £32,950）。
  - 上下文录取（contextual）：标准 BBC（BB 数学 + 物理/电子/计算机之一），增强 BCC。
  - **该官方页面上没有出现任何 NCUK IFY 分数字样 → QMUL 官方 NCUK IFY 分数：未查到。**
- **中介（新东方，2026.09.01）** <https://liuxue.xdf.cn/news/tianjin_8050707.shtml> 逐字：
  > "工程方向（Engineering pathway）的录取要求为AAB"
  > "航空航天工程（Aerospace Engineering with Industrial Experience）要求IFY成绩达到ABB，EAP等级为B级。材料科学与工程要求AAB。数学方向要求ABB。化学工程方向要求BBC。"
  - 其他：法律 AAA；人文社科 CCC；国际关系 ABB EAP A；公共关系 CCC EAP B；生物科学 CCC；物理 CCC；NCUK IFY 入学门槛"UKVI雅思总分5.0分，各单项不低于4.5分"，须在9月1日前满17岁。
- **中介（新东方，2026.09.02）** <https://liuxue.xdf.cn/news/tianjin_8051538.shtml> 逐字：
  > "伦敦玛丽女王大学电子电气工程专业要求IFY成绩达到ABB，EAP等级需达到B级。"
  → **QMUL EEE = ABB(128) + EAP B** 是本次唯一看到的 QMUL EEE 的 NCUK 具体分数，来源为**中介**；同文另一处称"工程方向 AAB"，两处存在不一致（可能因为 pathway 与单个专业口径不同）。**官方 QMUL EEE 的 IFY 分数：未查到。**

**(c) EAP / 英语**：中介口径 **EAP B**（EEE，逐字见上）；工程 pathway 亦要求 AAB/EAP 未给。官方页面仅给 IELTS/A-Level，无 EAP（**未查到**）。

**(d) 工科实力证据（官方，逐字）**
- 认证：> "This programme will be reviewed for re-accreditation by the Institute of Engineering and Technology on behalf of the Engineering Council in December 2025. This review is expected to result in accreditation for the 2026 and 2027 intakes. Accreditation can only be granted after a successful review and is subject to approval by the Institute of Engineering and Technology Academic Accreditation Committee."
- 学科排名：> "We were recently ranked 16th in the world and 1st in the UK for Electrical and Electronic Engineering - US News World University Ranking 2024."
- 学生调查：> "Electrical and Electronic Engineering at Queen Mary was voted 2nd in the UK for Student Voice and 2nd in UK Teaching on my Course in the recent National Student Survey 2024."
- 学会会员：> "We also offer IET membership, so you can receive sector updates and access to networking."
- 内部替代通道：> "International students who may not have had the chance to study A-level-equivalent qualifications may qualify for our one year International Science and Engineering Foundation programme, commonly known as the ISEFP. On successfully completing the ISEFP, and subject to meeting the progression requirements, you are guaranteed a place on your chosen degree programme at Queen Mary."
- 注：认证书面措辞是"预计将于 2026 与 2027 入学获得认证"，即**尚在复审中**，不是既成认证。
- 来源标签：官方（QMUL 测试/预发布域名）。

---

### 2.5 Brunel University of London（QS 2027 = 353）—— 中介声明核实

**(a) QS 2027**：353 —— 官方（ncuk.cn 逐字 "伦敦布鲁内尔大学：上升32位至第353名"；中介表 385→353，↑32）。

**(e) NCUK 合作身份**：**是**（ncuk.ac.uk 总表；ncuk.cn 校页 <https://www.ncuk.cn/university/lundunbuluneierdaxue/>）。

**(b) 官方课程页（Brunel 官网，官方）**
- <https://www.brunel.ac.uk/study/courses/civil-engineering-beng> —— Civil Engineering BEng (Hons)，课程代码 H208（带实习 H209），3–4 年，2026/27 学费 UK £9,790 / International £21,795。录取："ABB - BBB (See subject requirements) (A-level)"，BTEC "DDD - DDM"，IB "31-30"。页面标识"We're ranked 3rd in London (Complete University Guide 2027)"。**页面上 BBB 是 A-level 要求，不是 NCUK 要求；该页未见 NCUK 字样 → NCUK 官方分数 未查到。**
- <https://www.brunel.ac.uk/study/courses/computer-systems-engineering-beng> —— Computer Systems Engineering BEng (Hons)，代码 GH57（带实习 GH6P）。录取："ABB - BBB (See specified subjects) (A-level)"，BTEC DDD-DDM，IB 31-30；逐字："**Standard Offer:** GCE A-level BBB including one of the following subjects; Maths, Further Maths, Physics, Chemistry, Biology, Computer Science, Electronics or Design and Technology"；逐字："Our BEng computer systems engineering course is accredited by the Institution of Engineering and Technology (IET)."

**(b') 中介三方对照（关键：来源互相矛盾）**
| 来源 | 日期 | 土木 Civil | 土木与环境 Civil & Environmental | 计算机系统工程 | EAP |
|---|---|---|---|---|---|
| 新东方 liuxue.xdf.cn/news/tianjin_8051538.shtml | 2026.09.02 | **BBB** | **AAB** | **ABB** | 未提（该文总体称多数院校工程方向 EAP 要求 B 或 C） |
| 新东方 liuxue.xdf.cn/news/tianjin_8049846.shtml | 2026.08.31 | **AAB** | **AAB** | 未提 | "各专业均要求Overall达到C等级" |
| 新东方 liuxue.xdf.cn/news/tianjin_8050703.shtml | 2026.09.01 | **AAB** | 未提 | 未提（另给"工程管理 ABB"） | "学术英语（EAP）模块的整体等级要求为C级，听、说、读、写四个单项均须达到D级" |

第三条逐字补充：
> "申请土木工程本科课程的学生，IFY学术科目成绩须达到AAB。此外，该专业对IFY单科成绩设有明确要求：申请者的IFY英语语言科目和数学科目均须达到C级或以上。"
> "申请工程管理本科课程的学生，IFY学术科目成绩须达到ABB……申请者的IFY数学科目须达到合格成绩，并需在物理、化学、生物学或计算机科学等指定科目中任选一门达到合格标准"
> "EAP成绩与雅思成绩存在对应参照关系：C级约等同于雅思6.0分的水平。伦敦布鲁内尔大学多数本科专业的雅思要求为总分6.0分且各单项不低于5.5分。"

**核实结论（对任务给定的中介声明）**
- "Brunel Civil Engineering = BBB"：**1 个中介来源支持（BBB），2 个中介来源直接矛盾（AAB）**。Brunel 官网的"BBB"是 A-level 最低档，不是 NCUK。→ **未能证实，且证据冲突；官方 NCUK 分数 未查到。**
- "Brunel Civil & Environmental = AAB"：**2 个中介来源一致（AAB）**，无官方确认。→ **中介层面一致，官方 未查到。**
- "Brunel Computer Systems Engineering = ABB"：**1 个中介来源支持（ABB）**，与 Brunel 官网 A-level "ABB – BBB" 区间自洽，无官方 NCUK 确认。→ **与官网 A-level 口径自洽，NCUK 具体分数 未查到。**

---

## 3. 结论

**（1）四所指定大学都是 NCUK 合作校，但本次没有一家能在官方来源上查到 EEE 专业的 NCUK IFY 分数。** Newcastle 官方只给 A-level ABB 并公开欢迎 NCUK IFY；QUB/Cardiff/QMUL 官网（qub.ac.uk、cardiff.ac.uk、qmul.ac.uk）在本会话全部被 403/Cloudflare 拦截，QMUL 仅能通过其测试域名读到与官网一致的课程页。**唯一给出 QMUL EEE 具体 IFY 分数的证据是中介文章：ABB + EAP B。**

**（2）就"EEE 方向 + BBB–ABB 上限"而言，本批次最匹配的 1–2 个选择：**

- **首选：Queen Mary University of London — Electronic/Electrical Engineering, BEng H600（QS 2027 #103）**
  理由：中介口径 QMUL 电子电气工程 IFY = **ABB(128) + EAP B**，正好落在学生上限；EEE 学科实力有官方硬证据（US News 2024 世界第16、全英第1；NSS 2024 全英第2）；IET/Engineering Council 认证处于"预计 2026 与 2027 入学获批"的复审阶段；官网另给尚在执行的 A-level ABB 与 Clearing BBB 口径。**风险/诚实提示：** QMUL 的"工程方向（pathway）"另一中介来源写作 AAB，两处不一致；且 EAP 需 B 级（高于多数院校的 C），需向校方书面确认 EEE 与 EAP 的确切要求。若拿不到 ABB，QMUL 另有官方 HHX0 "Electronic Engineering with Foundation"（A-level BCC / IB 26），学费更低（£25,500）。

- **次选：Newcastle University — Electrical and Electronic Engineering BEng H607（QS 2027 #149）**
  理由：官方明确"proud to be an NCUK University partner…国际预科是被认可的升学路径"，并把 ABB 写作"typically the minimum required"；EEE 学位官方自述为"professionally accredited"，ISC 页面点名 **IET**；另有官方 H101 "Engineering with Foundation Year"（A-level ABB）**保证**升入含 H607 EEE 的 BEng 名单。**风险提示：** Newcastle 官方未公布 H607 的 NCUK IFY 分数（未查到），且 Newcastle 仅接受 IFY（不接受 IYOne）；须邮件确认 IFY 对应分数与 EAP 等级。

**（3）QUB 与 Cardiff：工科强，但目前无法证实其 EEE 的 IFY 门槛，暂不能作为稳妥选择。**
- QUB（#174）：NCUK 合作（IFY + IYOne）、Russell Group、EEE 有 IET 认证与 QS 学科前 200 的官方旁证（均来自官方通道方 INTO Queen's ISC 页面）；中介称 NCUK 有 124 个本科专业开放，其中航空航天 = **128/ABB、EAP C(单项 D)**，即工程门槛大概率就在学生上限附近，但 **EEE 具体分数 未查到**。QUB 域全程 403 是本次最大的信息缺口。
- Cardiff（#179）：NCUK 合作身份与奖学金确认，但 **工程专业 NCUK 分数与 EAP 完全未查到**；且那篇标题带 NCUK 的中介文实际讲的是 Cardiff 国际学习中心通道（高二 70%/高三 65%、IELTS 5.0/5.5/6.0），不能当作 NCUK 分数使用。

**（4）更强的结构性备选（官方数据完整、门槛明确更低）**：NCUK 官方 IYOne in EEE 页面给出 **48 foundation points + Maths/Physics 双 D + EAP D** 的最低线，并列出 Aston、Bristol、Leeds、Sheffield、Liverpool、Lancaster、Birmingham、Swansea、Salford、Northumbria、Huddersfield、Kent、LJMU、Manchester Met、Sheffield Hallam、UWE Bristol、RMIT 等升学合作校——**这份名单里没有 QUB/Cardiff/Newcastle/QMUL**。对"工科强 + 门槛确定"这一组合，这批学校（尤其 Sheffield、Leeds、Bristol、Birmingham、Liverpool、Lancaster）在本任务的证据质量上明显优于本批次四校。（注意：IYOne 是国际大一，路径与 IFY 不同。）

**（5）需要人工完成的动作**：下载并阅读 NCUK 官方逐课程 PDF（第 5 节首条）以补全四校 EEE 的权威分数；另外 QUB/Cardiff/QMUL 的官网需要用普通浏览器（非本工具）打开核对。

---

## 4. 尚未解决的关键缺口（给后续研究者）

1. **QUB 电气与电子工程 BEng 的 NCUK IFY 分数与 EAP 等级** —— 未查到（qub.ac.uk 全域 403）。
2. **Cardiff 工程（含 EEE / 机械 / 土木）的 NCUK IFY 分数与 EAP 等级** —— 未查到（cardiff.ac.uk 全域 Cloudflare 403）。
3. **QMUL 官方 EEE 的 NCUK IFY 分数** —— 未查到；仅有中介 "ABB + EAP B"（另有"工程方向 AAB"的矛盾说法）。
4. **Newcastle H607 的 NCUK IFY 分数与 EAP 等级** —— 未查到（官网页面在入学要求段被截断）。
5. **四校的 REF 2021 具体结果**（各 UoA 的 4*/3* 比例或排名）—— 全部未查到。
6. **QMUL 伦敦本部的 NCUK 合作专页** —— 未找到（ncuk.ac.uk 的 QMUL slug 指向马耳他校区）。
7. NCUK Course Finder 是否存在可读的 JSON/API 端点 —— 未尝试成功（课程行为 JS 注入）。

---

## 5. 未查到与失败来源（全部记录，均不再重试）

### 5.1 PDF（web_fetch 返回 unsupported content type "application/pdf"，**需人工下载**）
- <https://www.ncuk.ac.uk/wp-content/uploads/2026/09/University-Progression-Routes-September-2026.pdf> —— **NCUK 权威逐课程分数表（最该人工下载的一份）**
- <https://britisheducation.org.uk/pics/ncuk/ncuk-specialties.pdf>
- <https://ncuk.malverninternational.com/wp-content/uploads/sites/3/2025/06/NCUK_IFY-Entry-Requirements-May-2025.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2025/05/NCUK-International-Year-One-Entry-Requirements-May-2025.pdf>
- <http://www.ncuk.ac.uk/wp-content/uploads/2024/08/International-Year-One-Entry-Requirements-August-2024.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2018/02/IFY-Entry-Requirements-2018.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2018/10/NCUK-IFY-Grade-Equivalency.pdf>
- <https://ncukglobalconnect.com/wp-content/uploads/2024/07/International-Foundation-Year-Entry-Requirements-June-2024.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2025/10/International-Foundation-Year-NCUK-Programme-Overview-October-2025.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2025/10/International-Year-One-in-Electrical-Electronic-Engineering-NCUK-Programme-Overview-October-2025.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2024/04/International-Year-One-Engineering-Programme-and-Module-Overview.pdf>
- <https://www.ncuk.ac.uk/wp-content/uploads/2022/08/UAP-Entry-Requirements-22-23-August-2022.pdf>
- <https://www.cardiff.ac.uk/__data/assets/pdf_file/0008/908729/Engineering-UG-Programme-English.pdf>（Cardiff 工程本科英语要求）
- <https://www.cardiff.ac.uk/__data/assets/pdf_file/0010/297109/International-Foundation-Programme-prospectus.pdf>
- <https://www.qub.ac.uk/china/filestore/Filetoupload,944908,en.pdf>（QUB 中国区 INTO 预科表）
- <https://www.engc.org.uk/umbraco/api/file/Get?courseId=14188> 与 <https://www.engc.org.uk/umbraco/api/file/Get?courseId=1437>（Engineering Council 认证记录）
- <https://d7.stg.timeshighereducation.com/sites/default/files/institution_downloads/international_student_guide_sheffield.pdf>（THE 谢菲尔德国际学生指南，模板下载）
- <http://beo.jp/shared/brochure/IFYapplication.pdf>
- <http://one-web.t4staging.qmul.ac.uk/governance-and-legal-services/media/arcs/docs/quality-assurance/academic-development/programme-specs/se/eecs/2022x2f23/PS_HHX0_BEng-Electronic-Engineering-w-Foundation.pdf>（QMUL HHX0 专业规格）

### 5.2 403 / 反爬拦截（域名级不可达）
- `qub.ac.uk` 全域：<https://www.qub.ac.uk/courses/undergraduate/2026/electrical-electronic-engineering-beng-h600/> → 403 CloudFront "Request blocked"；<https://www.qub.ac.uk/International/International-students/Your-Country/China/> → 403；<https://www.qub.ac.uk/News/Allnews/2026/qs-rankings-2027.html> → 403；<https://www.qub.ac.uk/sites/StaffGateway/News/NewsArchive/2026/qs-rankings-2027.html> → 403；<https://www.qub.ac.uk/directorates/AcademicStudentAffairs/AcademicAffairs/ProgrammeSpecifications/2024/courses/ElectricalandElectronicEngineeringwithInternationalYearOne-BEng-AcademicYear202425.html> → 403；<https://www.qub.ac.uk/Research/our-research/research-excellence-framework-ref/> → 403。
- `qmul.ac.uk` 全域：<https://www.qmul.ac.uk/international-students/pathway-programmes/ify/ify-entry-requirements/> → 403 CloudFront；<https://www.qmul.ac.uk/undergraduate/coursefinder/courses/2026/electrical-and-electronic-engineering/> → 403。
- `cardiff.ac.uk` 全域：<https://www.cardiff.ac.uk/study/international/entry-requirements> → 403 "Just a moment..."；<https://www.cardiff.ac.uk/study/undergraduate/courses/2026/electrical-and-electronic-engineering-beng> → 403 "Just a moment..."。
- `topuniversities.com`（QUB/Cardiff 页面）→ 403 "Just a moment..."（Cloudflare）。
- `ucas.com` / `digital.ucas.com` 课程页 → 403 "Managed Challenge / I'm Under Attack Mode"。
- 代理读取尝试 <https://r.jina.ai/…>（用于绕过 qub.ac.uk 与 cardiff.ac.uk）→ TypeError: fetch failed（不可用）。

### 5.3 JS 注入 / 非 HTML 数据源
- <https://www.ncuk.ac.uk/ncuk-programmes/ncuk-entry-directory/>（原 <https://www.ncuk.ac.uk/ncuk-entry-directory> 的重定向目标）—— 大学下拉框可渲染，**课程行由 JS 注入**，GET 参数被忽略，取不到逐课程分数。
- <https://www.ncuk.cn/ncukdaxue/university-course-finder/> —— 中文页仅指向英文站查找器，本身无数据。

### 5.4 抓取成功但内容被截断 / 无所需数据
- <https://www.ncuk.ac.uk/our-universities/queens-university-belfast/>、<https://www.ncuk.ac.uk/our-universities/cardiff-university/>、<https://www.ncuk.ac.uk/our-universities/newcastle-university/> —— 均在 Rankings/Courses 段之前截断（已取到"Accepted NCUK programmes"信息）。
- <https://www.ncl.ac.uk/undergraduate/2026/degrees/h607/> —— 在入学要求/其他资格段之前截断（已取到 A-level ABB / IB 32 / £30,700）。
- <https://www.ncl.ac.uk/undergraduate/degrees/h600/> 与 <https://www.ncl.ac.uk/undergraduate/degrees/electrical-electronic-engineering-beng-h600/> → "Page Not Found - 404"（URL 形状错误；正确形状为 `/undergraduate/2026/degrees/h607/` 与 `/undergraduate/degrees/h101/`）。
- <https://www.walesonline.co.uk/incoming/welsh-universities-slip-down-international-34225199>（官方媒体）→ HTTP 200，但只返回导航框架、无排名数字。
- <https://www.businesseye.co.uk/news/queens-climbs-higher-in-global-university-rankings/> → HTTP 200，但仅返回导航（无数字）。
- <https://www.hotcoursesabroad.com/india/course/uk/electrical-and-electronic-engineering-beng-hons/54956630/program.html>（中介）→ HTTP 200，仅导航框架。
- <https://liuxue.xdf.cn/news/tianjin_8051278.shtml>（中介）→ HTTP 200，但全文为 Cardiff ISC 通道，**无工程 NCUK 分数**（已全文核对两遍）。
- <https://liuxue.xdf.cn/blog/jinfuyu/blog/5191364.shtml>（中介）→ HTTP 200，但为 QUB MPhil 研究型硕士介绍，无本科录取分数。
