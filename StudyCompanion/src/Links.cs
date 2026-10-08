using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace StudyCompanion
{
    public class LinkSet
    {
        public string Yt = "";                                  // YouTube 搜索词
        public string Bl = "";                                  // Bilibili 搜索词
        public List<string[]> Res = new List<string[]>();       // 精确资源 {标题, URL}
        public void Add(string label, string url) { Res.Add(new[] { label, url }); }
    }

    public class BookInfo
    {
        public string Code = "";
        public string Name = "";
        public string Path = "";
        public int Offset = 0;     // PDF 物理页 = 印张页 + Offset
    }

    /// <summary>把一个学习时段的标题映射到配套教学视频资源</summary>
    public static class VideoLinks
    {
        // ---- 常用资源 ----
        const string IDX9709 = "https://www.math1234567.com/article/video9709";
        const string IDX9231 = "https://www.math1234567.com/article/video9231";
        const string TLM = "https://www.tlmaths.com/";
        const string EXAM = "https://www.examsolutions.net/";
        const string BICEN = "https://www.youtube.com/@BicenMaths/playlists";
        const string PHYON = "https://www.alevelphysicsonline.com/";
        const string PHY_PRAC = "https://www.alevelphysicsonline.com/practical-skills";
        const string PHY_UNC = "https://www.alevelphysicsonline.com/uncertainty";
        const string MIT801 = "https://ocw.mit.edu/courses/8-01sc-classical-mechanics-fall-2016/";
        const string MIT802 = "https://ocw.mit.edu/courses/8-02-physics-ii-electricity-and-magnetism-spring-2007/";
        const string MIT803 = "https://ocw.mit.edu/courses/8-03sc-physics-iii-vibrations-and-waves-fall-2016/pages/lecture-videos/";
        const string MIT804 = "https://ocw.mit.edu/courses/8-04-quantum-physics-i-spring-2013/";
        const string MIT1803 = "https://ocw.mit.edu/courses/18-03-differential-equations-spring-2010/video_galleries/video-lectures/";
        const string MIT1806 = "https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/";
        const string JBSTAT = "https://www.jbstatistics.com/category/hypothesis-testing/";
        const string STATQ = "https://www.youtube.com/@statquest";
        const string EAPF = "https://www.eapfoundation.com/";
        const string PHRASE = "https://www.phrasebank.manchester.ac.uk/";

        const string YT_MATHS_PROF_QUAD = "https://www.youtube.com/playlist?list=PLQc2VtOvtyekSWz1tcMcfXQ1GXxdBhwNc";
        const string YT_MATHS_PROF_COORD = "https://www.youtube.com/playlist?list=PLQc2VtOvtyemrhRfCRuE0-sT0SDp6ukAj";
        const string YT_CIE_P1_TRIG = "https://www.youtube.com/playlist?list=PLquyU_6YLv6cxWXtiOo9dBXS46DM4wCrp";
        const string YT_CIE_P1_HACK = "https://www.youtube.com/playlist?list=PLaQBxdrnmtNxi_Ne7a2ZBWJ7ZI39EoZil";
        const string YT_TLM_LOGS = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4uT0xxFSToPgn_3YDYcaxAu";
        const string YT_BICEN_LOG = "https://www.youtube.com/playlist?list=PL0SSkmc4r_BYZf-UyGHM1eMt00sILgRIS";
        const string YT_TLM_POLY = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4sbk3csZn6i1skwvnqjo3Xc";
        const string YT_TLM_NUM = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4uHZWDIOiK3V7iyz4sw1HCP";
        const string YT_3B1B_LA = "https://www.youtube.com/playlist?list=PLZHQObOWTQDPD3MizzM2xVFitgF8hE_ab";
        const string YT_3B1B_DE = "https://www.youtube.com/playlist?list=PLZHQObOWTQDNPOjrT6KVlfJuKtYTftqH6";
        const string YT_WELCH = "https://www.classcentral.com/course/youtube-imaginary-numbers-are-real-45679";
        const string YT_EXAM_ARC = "https://www.examsolutions.net/tutorials/arcs-sectors-segments/";
        const string YT_EXAM_PARTS = "https://www.examsolutions.net/tutorials/integration-by-parts/";
        const string YT_EXAM_HARM = "https://www.examsolutions.net/tutorials/harmonic-identities/?board=CIE&level=International&module=P2&topic=1435";
        const string YT_EXAM_SUVAT = "https://www.examsolutions.net/tutorials/motion-straight-line-constant-acceleration-suvat/";
        const string YT_EXAM_MECH = "https://www.examsolutions.net/exammodule/m2-edexcel/";
        const string YT_EXAM_MOM = "https://www.examsolutions.net/tutorials/two-particles-colliding-separating/";
        const string YT_EXAM_STATS = "https://www.examsolutions.net/exammodule/statistics-a-level-mei/";
        const string YT_EXAM_MACL = "https://www.examsolutions.net/tutorials/maclaurins-series/";
        const string YT_FLIP_WEP = "https://www.flippingphysics.com/apc-work-energy-power-review.html";
        const string YT_JB_DISC = "https://www.jbstatistics.com/category/discrete-probability-distributions/";
        const string BL_CIE9702 = "https://www.bilibili.com/cheese/play/ss37608";
        const string BL_KHAN_FUNC = "https://open.163.com/newview/movie/courseintro?newurl=MDAPTVFE8";
        const string BL_MIT_EM = "http://open.163.com/newview/movie/courseintro?newurl=M72UIB0K0";
        const string BL_SONGHAO = "https://www.bilibili.com/list/ml3311577248?oid=31944511&bvid=BV1UW411k7Jv";

        static readonly Dictionary<string, LinkSet> T = new Dictionary<string, LinkSet>();

        static void A(string key, string yt, string bl, params string[][] res)
        {
            var ls = new LinkSet();
            ls.Yt = yt; ls.Bl = bl;
            if (res != null) foreach (var r in res) ls.Res.Add(r);
            T[key] = ls;
        }
        static string[] R(string label, string url) { return new[] { label, url }; }

        static VideoLinks()
        {
            // ==================== Pure Mathematics 1 ====================
            A("P1 Ch1", "CIE 9709 P1 quadratics completing the square discriminant",
              "A Level 一元二次方程 判别式 配方",
              R("The Maths Prof · 二次方程 (CIE P1)", YT_MATHS_PROF_QUAD), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch2", "CIE 9709 P1 functions domain range inverse composite transformations",
              "A Level 函数 定义域 值域 反函数 复合函数",
              R("CIE 9709 章节索引", IDX9709), R("TLMaths 全站索引", TLM), R("可汗学院·函数（免翻墙）", BL_KHAN_FUNC));
            A("P1 Ch3", "CIE 9709 P1 coordinate geometry circle equation",
              "A Level 坐标几何 圆的方程 直线",
              R("The Maths Prof · 坐标几何 (CIE P1)", YT_MATHS_PROF_COORD), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch4", "CIE 9709 P1 circular measure arc length sector area radians",
              "A Level 弧度制 弧长 扇形面积",
              R("ExamSolutions · 弧长与扇形", YT_EXAM_ARC), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch5", "CIE 9709 P1 trigonometry identities equations graphs",
              "A Level 三角函数 图像 恒等式 方程",
              R("CIE P1 三角恒等式与方程", YT_CIE_P1_TRIG), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch6", "CIE 9709 P1 binomial expansion arithmetic geometric series",
              "A Level 二项式展开 等差数列 等比数列",
              R("CIE AS P1 真题精讲", YT_CIE_P1_HACK), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch7", "CIE 9709 P1 differentiation chain rule tangent normal",
              "A Level 导数 链式法则 切线 法线",
              R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch8", "CIE 9709 P1 stationary points rates of change maxima minima",
              "A Level 驻点 极大值 极小值 变化率",
              R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
            A("P1 Ch9", "CIE 9709 P1 integration area under curve volume of revolution trapezium rule",
              "A Level 定积分 面积 旋转体体积 梯形法则",
              R("ExamSolutions · 分部积分/积分技巧", YT_EXAM_PARTS), R("CIE 9709 章节索引", IDX9709));

            // ==================== Pure Mathematics 2 & 3 ====================
            A("P2/3 Ch1", "CIE 9709 P2 modulus function polynomial division factor remainder theorem",
              "A Level 绝对值函数 多项式除法 因式定理 余数定理",
              R("TLMaths · 多项式与部分分式", YT_TLM_POLY), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch2", "CIE 9709 P2 logarithms exponential equations natural log",
              "A Level 对数 指数方程 自然对数",
              R("TLMaths · 对数与指数", YT_TLM_LOGS), R("Bicen Maths · 指数与对数", YT_BICEN_LOG));
            A("P2/3 Ch3", "CIE 9709 P3 sec cosec cot compound double angle R sin(theta+alpha)",
              "A Level 三角恒等式 和角 倍角 辅助角公式",
              R("ExamSolutions · R sin(θ±α) (CIE P2)", YT_EXAM_HARM), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch4", "CIE 9709 P3 product quotient rule implicit parametric differentiation",
              "A Level 乘积法则 商法则 隐函数求导 参数方程求导",
              R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch5", "CIE 9709 P3 integration exponential trig trapezium rule",
              "A Level 积分 指数函数 三角函数 梯形法则",
              R("ExamSolutions · 积分技巧", YT_EXAM_PARTS), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch6", "CIE 9709 P3 numerical solution iteration Newton-Raphson",
              "A Level 数值解法 迭代法 牛顿迭代法",
              R("TLMaths · 迭代法与牛顿法", YT_TLM_NUM), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch7", "CIE 9709 P3 partial fractions binomial expansion non-integer n",
              "A Level 部分分式 二项式展开 非整数次幂",
              R("TLMaths · 多项式与部分分式", YT_TLM_POLY), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch8", "CIE 9709 P3 integration by substitution by parts tan inverse",
              "A Level 换元积分 分部积分 反三角积分",
              R("ExamSolutions · 分部积分", YT_EXAM_PARTS), R("TLMaths 全站索引", TLM));
            A("P2/3 Ch9", "CIE 9709 P3 vectors scalar product line equation planes",
              "A Level 向量 数量积 直线方程 平面",
              R("3Blue1Brown · 线性代数本质", YT_3B1B_LA), R("CIE 9709 章节索引", IDX9709));
            A("P2/3 Ch10", "CIE 9709 P3 differential equations separating variables",
              "A Level 微分方程 分离变量",
              R("MIT 18.03 微分方程", MIT1803), R("3Blue1Brown · 微分方程", YT_3B1B_DE));
            A("P2/3 Ch11", "CIE 9709 P3 complex numbers Argand diagram de Moivre loci",
              "A Level 复数 复平面 棣莫弗定理 轨迹",
              R("Welch Labs · 虚数是什么", YT_WELCH), R("CIE 9709 章节索引", IDX9709));

            // ==================== Mechanics 1 ====================
            A("M1 Ch1", "CIE 9709 Mechanics 1 kinematics suvat velocity time graph",
              "A Level 力学 运动学 匀加速 位移时间图像",
              R("ExamSolutions · SUVAT", YT_EXAM_SUVAT), R("MIT 8.01 经典力学（免翻墙）", MIT801));
            A("M1 Ch2", "CIE 9709 Mechanics 1 forces Newton laws motion in a line",
              "A Level 力学 牛顿定律 受力分析",
              R("ExamSolutions · 力与力矩", YT_EXAM_MECH), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch3", "CIE 9709 Mechanics 1 resolving forces equilibrium Lami theorem",
              "A Level 力学 力的分解 平衡 拉密定理",
              R("ExamSolutions · 力与力矩", YT_EXAM_MECH), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch4", "CIE 9709 Mechanics 1 friction limiting equilibrium",
              "A Level 力学 摩擦力 摩擦极限",
              R("ExamSolutions · 力学", YT_EXAM_MECH), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch5", "CIE 9709 Mechanics 1 connected particles pulleys Newton third law",
              "A Level 力学 连接体 滑轮 牛顿第三定律",
              R("ExamSolutions · 力学", YT_EXAM_MECH), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch6", "CIE 9709 Mechanics 1 variable acceleration calculus",
              "A Level 力学 变加速 积分 导数",
              R("ExamSolutions · 力学", YT_EXAM_MECH), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch7", "CIE 9709 Mechanics 1 momentum collisions conservation",
              "A Level 力学 动量 碰撞 动量守恒",
              R("ExamSolutions · 碰撞与动量", YT_EXAM_MOM), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch8", "CIE 9709 Mechanics 1 work energy kinetic gravitational potential",
              "A Level 力学 功 动能 重力势能",
              R("Flipping Physics · 功与能", YT_FLIP_WEP), R("MIT 8.01 经典力学", MIT801));
            A("M1 Ch9", "CIE 9709 Mechanics 1 work-energy principle power",
              "A Level 力学 功能原理 功率",
              R("Flipping Physics · 功与能", YT_FLIP_WEP), R("MIT 8.01 经典力学", MIT801));

            // ==================== Statistics 1 ====================
            A("S1 Ch1", "CIE 9709 Statistics 1 stem and leaf histogram cumulative frequency",
              "A Level 统计 茎叶图 直方图 累积频率",
              R("ExamSolutions · 统计模块", YT_EXAM_STATS), R("StatQuest", STATQ));
            A("S1 Ch2", "CIE 9709 Statistics 1 mean median mode",
              "A Level 统计 平均数 中位数 众数",
              R("ExamSolutions · 统计模块", YT_EXAM_STATS), R("StatQuest", STATQ));
            A("S1 Ch3", "CIE 9709 Statistics 1 variance standard deviation quartiles",
              "A Level 统计 方差 标准差 四分位数",
              R("ExamSolutions · 统计模块", YT_EXAM_STATS), R("StatQuest", STATQ));
            A("S1 Ch4", "CIE 9709 Statistics 1 probability conditional independent events",
              "A Level 统计 概率 条件概率 独立事件",
              R("StatQuest · 条件概率", "https://youtu.be/_IgyaD7vOOA"), R("jbstatistics 假设检验/概率", JBSTAT));
            A("S1 Ch5", "CIE 9709 Statistics 1 permutations combinations",
              "A Level 统计 排列 组合",
              R("ExamSolutions · 统计模块", YT_EXAM_STATS), R("StatQuest", STATQ));
            A("S1 Ch6", "CIE 9709 Statistics 1 discrete random variable expectation variance",
              "A Level 统计 离散随机变量 期望 方差",
              R("jbstatistics · 离散分布", YT_JB_DISC), R("StatQuest", STATQ));
            A("S1 Ch7", "CIE 9709 Statistics 1 binomial geometric distribution",
              "A Level 统计 二项分布 几何分布",
              R("jbstatistics · 离散分布", YT_JB_DISC), R("StatQuest", STATQ));
            A("S1 Ch8", "CIE 9709 Statistics 1 normal distribution standardisation",
              "A Level 统计 正态分布 标准化 正态近似",
              R("jbstatistics · 假设检验与分布", JBSTAT), R("StatQuest", STATQ));

            // ==================== Further Mathematics ====================
            string[] fmCh = {
                "Ch1|roots of polynomial equations cubic quartic|多项式方程的根 三次 四次",
                "Ch2|rational functions asymptotes oblique|有理函数 渐近线",
                "Ch3|summation of series sigma r r2 r3|级数求和 Σr Σr² Σr³",
                "Ch4|matrices inverse determinant transformation|矩阵 逆矩阵 行列式 变换",
                "Ch5|polar coordinates area|极坐标 极坐标面积",
                "Ch6|vector product cross product planes|向量积 叉积 平面",
                "Ch7|proof by induction divisibility|数学归纳法 整除",
                "Ch8|continuous random variable pdf cdf|连续随机变量 概率密度函数",
                "Ch9|t distribution hypothesis test confidence interval|t分布 假设检验 置信区间",
                "Ch10|chi squared test goodness of fit contingency|卡方检验 拟合优度 列联表",
                "Ch11|non-parametric Wilcoxon sign rank test|非参数检验 威尔科克森 符号秩",
                "Ch12|probability generating function|概率母函数",
                "Ch13|projectiles trajectory|抛体运动 轨迹方程",
                "Ch14|equilibrium rigid body centre of mass|刚体平衡 重心 力矩",
                "Ch15|circular motion vertical circle|圆周运动 竖直圆",
                "Ch16|Hooke law elastic potential energy|胡克定律 弹性势能",
                "Ch17|linear motion variable force|变力作用 直线运动",
                "Ch18|momentum impulse oblique collision|动量 冲量 斜碰撞",
                "Ch19|hyperbolic functions inverse|双曲函数 反双曲函数",
                "Ch20|eigenvalues eigenvectors diagonalisation|特征值 特征向量 对角化",
                "Ch21|implicit parametric Maclaurin series|隐函数 参数方程 麦克劳林级数",
                "Ch22|reduction formula arc length surface area|递推公式 弧长 旋转曲面面积",
                "Ch23|de Moivre roots of unity complex summation|棣莫弗定理 单位根 复数求和",
                "Ch24|second order differential equations|二阶微分方程 齐次 非齐次",
            };
            foreach (var row in fmCh)
            {
                var p = row.Split('|');
                var res = new List<string[]>();
                res.Add(R("CIE 9231 章节索引（进阶纯数全专题）", IDX9231));
                if (p[0] == "Ch4" || p[0] == "Ch20") res.Add(R("MIT 18.06 线性代数", MIT1806));
                if (p[0] == "Ch21") res.Add(R("ExamSolutions · 麦克劳林级数", YT_EXAM_MACL));
                if (p[0] == "Ch22" || p[0] == "Ch24") res.Add(R("MIT 18.03 微分方程", MIT1803));
                if (p[0] == "Ch5") res.Add(R("MIT 18.01 单变量微积分", "https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/"));
                A("FM " + p[0], "CIE 9231 Further Maths " + p[1], "A Level 进阶数学 " + p[2], res.ToArray());
            }

            // ==================== Physics ====================
            string[] phyCh = {
                "Ch1|kinematics speed velocity displacement vectors|运动学 速率 速度 位移 矢量",
                "Ch2|accelerated motion projectiles suvat|加速运动 匀加速 抛体运动",
                "Ch3|dynamics force mass Newton laws|动力学 力 质量 牛顿定律",
                "Ch4|forces moments centre of gravity|力 力矩 重心 力偶",
                "Ch5|work energy power|功 能量 功率",
                "Ch6|momentum collisions explosions|动量 碰撞 爆炸",
                "Ch7|density pressure Archimedes Young modulus|密度 压强 阿基米德 杨氏模量",
                "Ch8|electric current voltage resistance power|电流 电压 电阻 电功率",
                "Ch9|Kirchhoff laws resistor combinations|基尔霍夫定律 电阻组合",
                "Ch10|resistance resistivity Ohm law I-V characteristic|电阻 电阻率 欧姆定律 I-V特性",
                "Ch11|internal resistance potential divider sensors potentiometer|内阻 分压器 传感器 电位器",
                "Ch12|waves Doppler effect electromagnetic polarisation|波 多普勒效应 电磁波 偏振",
                "Ch13|superposition diffraction interference diffraction grating|叠加 衍射 干涉 衍射光栅",
                "Ch14|stationary waves nodes antinodes|驻波 波节 波腹",
                "Ch15|atomic structure radioactivity particles|原子结构 放射性 粒子物理",
                "Ch16|circular motion centripetal force|圆周运动 向心力 角速度",
                "Ch17|gravitational field potential orbits|引力场 引力势 轨道",
                "Ch18|oscillations simple harmonic motion resonance|振动 简谐运动 共振 阻尼",
                "Ch19|thermal physics specific heat latent heat|热学 比热容 潜热 内能",
                "Ch20|ideal gas kinetic theory Boyle law|理想气体 分子运动论 玻意耳定律",
                "Ch21|uniform electric field Coulomb|匀强电场 电场强度 库仑定律",
            };
            foreach (var row in phyCh)
            {
                var p = row.Split('|');
                var res = new List<string[]>();
                res.Add(R("A Level Physics Online", PHYON));
                int n = int.Parse(p[0].Substring(2));
                if (n <= 6) res.Add(R("MIT 8.01 经典力学", MIT801));
                else if (n <= 11) res.Add(R("MIT 8.02 电磁学", MIT802));
                else if (n <= 14) res.Add(R("MIT 8.03 振动与波", MIT803));
                else if (n >= 16 && n <= 18) res.Add(R("MIT 8.03 振动与波", MIT803));
                else res.Add(R("MIT 8.04 量子物理", MIT804));
                res.Add(R("Bilibili · CIE 9702 直击考点 AS", BL_CIE9702));
                A("Phy " + p[0], "CIE 9702 A Level Physics " + p[1], "A Level 物理 " + p[2], res.ToArray());
            }
            A("Phy P1", "CIE 9702 practical skills uncertainty error analysis",
              "A Level 物理 实验 不确定度 误差分析",
              R("A Level Physics Online · 实验技能", PHY_PRAC), R("A Level Physics Online · 不确定度", PHY_UNC));

            // ==================== EAP ====================
            A("EAP", "EAP academic writing essay structure referencing",
              "学术英语 学术写作 论文结构 参考文献",
              R("EAP Foundation · 学术写作", EAPF + "writing/"), R("EAP Foundation · 听讲座与笔记", EAPF + "listening/lectures/"),
              R("Manchester Academic Phrasebank", PHRASE), R("Purdue OWL 引用规范", "https://owl.purdue.edu/owl/research_and_citation/resources.html"));
        }

        // ------------------------------------------------------------------
        static readonly Regex BookRx = new Regex(@"(P2/3|P1|M1|S1|FM|Phy)\s+(Ch\d+|P1)\b", RegexOptions.Compiled);
        static readonly Regex PageRx = new Regex(@"p\.(\d+)", RegexOptions.Compiled);
        static readonly Regex PaperRx = new Regex(@"真题精练\s*(\d{4})\s*([A-Za-z0-9/]+)", RegexOptions.Compiled);

        /// <summary>判断这个时段要用哪本课本，返回课本代号（找不到返回 null）</summary>
        public static string BookCodeFor(Slot s)
        {
            if (s == null) return null;
            string t = s.Title ?? "";

            // 复习/测试时段横跨多科，标一本课本反而会误导，不显示
            if ((s.Subject ?? "").Contains("复习")) return null;

            var m = BookRx.Match(t);
            if (m.Success) return m.Groups[1].Value;

            // 真题精练：9709 P1 / 9709 P3 / 9709 M1 / 9709 S1 / 9702 AS / 9231 FP1 …
            var pm = PaperRx.Match(t);
            if (pm.Success)
            {
                string paper = pm.Groups[1].Value;
                string sub = pm.Groups[2].Value.ToUpperInvariant();
                if (paper == "9702") return "Phy";
                if (paper == "9231") return "FM";
                if (paper == "9709")
                {
                    if (sub.StartsWith("P1")) return "P1";
                    if (sub.StartsWith("P3")) return "P2/3";
                    if (sub.StartsWith("M1") || sub.StartsWith("P4")) return "M1";
                    if (sub.StartsWith("S1") || sub.StartsWith("P6")) return "S1";
                }
            }
            return null;
        }

        /// <summary>这个时段要用到的课本标题，例如 "Pure Mathematics 1 - Coursebook"</summary>
        public static string BookTitleForSlot(Slot s)
        {
            string code = BookCodeFor(s);
            if (code == null) return "";
            BookInfo b = Books.FirstOrDefault(x => x.Code == code);
            return b == null ? "" : b.Name;
        }

        /// <summary>为某个时段生成视频链接列表 {标题, URL}</summary>
        public static List<string[]> ForSlot(Slot s)
        {
            var outp = new List<string[]>();
            if (s == null) return outp;

            string key = null;
            string title = s.Title ?? "";
            var m = BookRx.Match(title);
            if (m.Success) key = m.Groups[1].Value + " " + m.Groups[2].Value;

            LinkSet ls = null;
            if (key != null && T.TryGetValue(key, out ls)) { }
            else
            {
                // 回退：按科目关键词
                string t = title + " " + s.Subject;
                if (t.Contains("真题")) T.TryGetValue("P1 Ch1", out ls);
                if (ls == null && (t.Contains("EAP") || t.Contains("学术英语"))) T.TryGetValue("EAP", out ls);
                if (ls == null && t.Contains("复习")) T.TryGetValue("P1 Ch1", out ls);
                if (ls == null && t.Contains("物理")) T.TryGetValue("Phy Ch1", out ls);
                if (ls == null && t.Contains("纯数")) T.TryGetValue("P1 Ch1", out ls);
                if (ls == null && t.Contains("应用")) T.TryGetValue("M1 Ch1", out ls);
                if (ls == null && t.Contains("进阶")) T.TryGetValue("FM Ch1", out ls);
                if (ls == null) T.TryGetValue("P1 Ch1", out ls);
            }
            if (ls == null) return outp;

            foreach (var r in ls.Res) outp.Add(r);
            if (!string.IsNullOrEmpty(ls.Yt))
                outp.Add(R("▶ YouTube 搜索", "https://www.youtube.com/results?search_query=" + Uri.EscapeDataString(ls.Yt)));
            if (!string.IsNullOrEmpty(ls.Bl))
                outp.Add(R("▶ Bilibili 搜索（免翻墙）", "https://search.bilibili.com/all?keyword=" + Uri.EscapeDataString(ls.Bl)));
            return outp;
        }

        // ------------------------------------------------------------------ 课本 PDF
        static List<BookInfo> _books;
        public static List<BookInfo> Books
        {
            get
            {
                if (_books != null) return _books;
                _books = new List<BookInfo>();
                string dir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data");
                string f = Path.Combine(dir, "books.tsv");
                if (File.Exists(f))
                {
                    foreach (var line in File.ReadAllLines(f, Encoding.UTF8))
                    {
                        if (line.Trim().Length == 0 || line.StartsWith("#")) continue;
                        var p = line.Split('\t');
                        if (p.Length < 4) continue;
                        var b = new BookInfo();
                        b.Code = p[0].Trim(); b.Name = p[1].Trim(); b.Path = p[2].Trim();
                        int off; int.TryParse(p[3].Trim(), out off); b.Offset = off;
                        b.Path = ResolvePath(b.Path);
                        _books.Add(b);
                    }
                }
                return _books;
            }
        }

        /// <summary>课本 PDF 可能被移动到别处：在原位置找不到时，到常见目录里按文件名搜一次</summary>
        static readonly Dictionary<string, string> _resolved = new Dictionary<string, string>();
        static string ResolvePath(string stored)
        {
            if (!string.IsNullOrEmpty(stored) && File.Exists(stored)) return stored;
            string name = Path.GetFileName(stored);
            if (string.IsNullOrEmpty(name)) return stored;
            string hit;
            if (_resolved.TryGetValue(name, out hit) && File.Exists(hit)) return hit;

            var roots = new List<string>();
            string exeDir = AppDomain.CurrentDomain.BaseDirectory;
            string parent = Path.GetDirectoryName(exeDir.TrimEnd('\\'));
            if (!string.IsNullOrEmpty(parent))
            {
                roots.Add(Path.Combine(parent, "Textbook"));
                roots.Add(parent);
            }
            roots.Add(@"D:\BaiduNetdiskDownload");
            roots.Add(@"D:\UsrFiles\Documents");

            foreach (var r in roots)
            {
                try
                {
                    if (!Directory.Exists(r)) continue;
                    var found = Directory.GetFiles(r, name, SearchOption.AllDirectories);
                    if (found.Length > 0) { _resolved[name] = found[0]; return found[0]; }
                }
                catch { }
            }
            return stored;
        }

        /// <summary>返回 {标题, URL} —— 打开当天用到的课本对应页</summary>
        public static List<string[]> PdfLinksForSlot(Slot s)
        {
            var res = new List<string[]>();
            if (s == null) return res;
            string title = s.Title ?? "";
            string code = BookCodeFor(s);
            if (code == null) return res;
            BookInfo book = Books.FirstOrDefault(b => b.Code == code);
            if (book == null || !File.Exists(book.Path)) return res;

            // 1) 优先用「小节 -> PDF 页」索引（最准）
            int pdfPage = 0;
            string secLabel = "";
            foreach (Match sm in Regex.Matches(title, @"§(\d{1,2}\.\d{1,2})"))
            {
                string key = code + "|" + sm.Groups[1].Value;
                int pg;
                if (SectionIndex.TryGetValue(key, out pg)) { pdfPage = pg; secLabel = sm.Groups[1].Value; break; }
            }

            // 2) 回退：印刷页码 + 偏移
            if (pdfPage == 0)
            {
                int page = 0;
                foreach (var line in s.Body)
                {
                    var pm = PageRx.Match(line);
                    if (pm.Success) { int v; if (int.TryParse(pm.Groups[1].Value, out v)) { page = v; break; } }
                }
                if (page > 0 && book.Offset > 0) { pdfPage = page + book.Offset; secLabel = "p." + page; }
            }

            string url = FileUrl(book.Path);
            if (pdfPage > 0)
            {
                url += "#page=" + pdfPage;
                res.Add(R("打开课本 " + code + "（" + (secLabel.StartsWith("p.") ? secLabel : "§" + secLabel) + "）", url));
            }
            else res.Add(R("打开课本 " + code, url));
            return res;
        }

        /// <summary>
        /// 本地路径 -> 可以交给 ShellExecute 的 file:// URL。
        /// ⚠ 不能用 new Uri(path).AbsoluteUri：它会把路径里的非 ASCII 字符百分号编码
        ///   （…/02_%E5%AD%A6%E4%B9%A0%E4%B8%8E%E6%95%99%E6%9D%90/…），WPS 与 Windows shell
        ///   拿到这种 URL 会直接报「系统找不到指定的文件」（2026-10-08 实测；纯 ASCII 路径正常）。
        ///   实测：只把空格转成 %20、中文原样保留，WPS 能正常打开并跳到 #page=N。
        /// </summary>
        public static string FileUrl(string path)
        {
            string p = path.Replace('\\', '/');
            p = p.StartsWith("//") ? "file:" + p : "file:///" + p.TrimStart('/');
            // 先把 % 自身编码，避免路径里出现字面 % 时被当成转义序列
            return p.Replace("%", "%25").Replace(" ", "%20").Replace("#", "%23");
        }

        // ------------------------------------------------------------------ 小节 -> PDF 页索引
        static Dictionary<string, int> _secIdx;
        public static Dictionary<string, int> SectionIndex
        {
            get
            {
                if (_secIdx != null) return _secIdx;
                _secIdx = new Dictionary<string, int>();
                try
                {
                    string f = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "data", "pages.tsv");
                    if (File.Exists(f))
                        foreach (var line in File.ReadAllLines(f, Encoding.UTF8))
                        {
                            if (line.StartsWith("#") || line.Trim().Length == 0) continue;
                            var p = line.Split('\t');
                            if (p.Length < 3) continue;
                            int pg;
                            if (int.TryParse(p[2].Trim(), out pg))
                                _secIdx[p[0].Trim() + "|" + p[1].Trim()] = pg;
                        }
                }
                catch { }
                return _secIdx;
            }
        }
    }
}
