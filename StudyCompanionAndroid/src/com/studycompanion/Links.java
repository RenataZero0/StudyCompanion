package com.studycompanion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 小节 → 配套教学资源 / 课本名 的映射（与桌面版 Links.cs 同一张表） */
public class Links {

    static class LinkSet {
        String yt = "", bl = "";
        List<String[]> res = new ArrayList<String[]>();
        void add(String label, String url) { res.add(new String[]{label, url}); }
    }

    private static final Map<String, LinkSet> T = new HashMap<String, LinkSet>();

    private static final String IDX9709 = "https://www.math1234567.com/article/video9709";
    private static final String IDX9231 = "https://www.math1234567.com/article/video9231";
    private static final String TLM = "https://www.tlmaths.com/";
    private static final String EXAM = "https://www.examsolutions.net/";
    private static final String PHYON = "https://www.alevelphysicsonline.com/";
    private static final String PHY_PRAC = "https://www.alevelphysicsonline.com/practical-skills";
    private static final String PHY_UNC = "https://www.alevelphysicsonline.com/uncertainty";
    private static final String MIT801 = "https://ocw.mit.edu/courses/8-01sc-classical-mechanics-fall-2016/";
    private static final String MIT802 = "https://ocw.mit.edu/courses/8-02-physics-ii-electricity-and-magnetism-spring-2007/";
    private static final String MIT803 = "https://ocw.mit.edu/courses/8-03sc-physics-iii-vibrations-and-waves-fall-2016/pages/lecture-videos/";
    private static final String MIT804 = "https://ocw.mit.edu/courses/8-04-quantum-physics-i-spring-2013/";
    private static final String MIT1803 = "https://ocw.mit.edu/courses/18-03-differential-equations-spring-2010/video_galleries/video-lectures/";
    private static final String MIT1806 = "https://ocw.mit.edu/courses/18-06-linear-algebra-spring-2010/";
    private static final String JBSTAT = "https://www.jbstatistics.com/category/hypothesis-testing/";
    private static final String STATQ = "https://www.youtube.com/@statquest";
    private static final String EAPF = "https://www.eapfoundation.com/";
    private static final String PHRASE = "https://www.phrasebank.manchester.ac.uk/";
    private static final String YT_QUAD = "https://www.youtube.com/playlist?list=PLQc2VtOvtyekSWz1tcMcfXQ1GXxdBhwNc";
    private static final String YT_COORD = "https://www.youtube.com/playlist?list=PLQc2VtOvtyemrhRfCRuE0-sT0SDp6ukAj";
    private static final String YT_TRIG = "https://www.youtube.com/playlist?list=PLquyU_6YLv6cxWXtiOo9dBXS46DM4wCrp";
    private static final String YT_HACK = "https://www.youtube.com/playlist?list=PLaQBxdrnmtNxi_Ne7a2ZBWJ7ZI39EoZil";
    private static final String YT_LOGS = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4uT0xxFSToPgn_3YDYcaxAu";
    private static final String YT_BICEN = "https://www.youtube.com/playlist?list=PL0SSkmc4r_BYZf-UyGHM1eMt00sILgRIS";
    private static final String YT_POLY = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4sbk3csZn6i1skwvnqjo3Xc";
    private static final String YT_NUM = "https://www.youtube.com/playlist?list=PLg2tfDG3Ww4uHZWDIOiK3V7iyz4sw1HCP";
    private static final String YT_3B1B = "https://www.youtube.com/playlist?list=PLZHQObOWTQDPD3MizzM2xVFitgF8hE_ab";
    private static final String YT_WELCH = "https://www.classcentral.com/course/youtube-imaginary-numbers-are-real-45679";
    private static final String YT_ARC = "https://www.examsolutions.net/tutorials/arcs-sectors-segments/";
    private static final String YT_PARTS = "https://www.examsolutions.net/tutorials/integration-by-parts/";
    private static final String YT_HARM = "https://www.examsolutions.net/tutorials/harmonic-identities/?board=CIE&level=International&module=P2&topic=1435";
    private static final String YT_SUVAT = "https://www.examsolutions.net/tutorials/motion-straight-line-constant-acceleration-suvat/";
    private static final String YT_MECH = "https://www.examsolutions.net/exammodule/m2-edexcel/";
    private static final String YT_MOM = "https://www.examsolutions.net/tutorials/two-particles-colliding-separating/";
    private static final String YT_STATS = "https://www.examsolutions.net/exammodule/statistics-a-level-mei/";
    private static final String YT_MACL = "https://www.examsolutions.net/tutorials/maclaurins-series/";
    private static final String YT_WEP = "https://www.flippingphysics.com/apc-work-energy-power-review.html";
    private static final String YT_JBDISC = "https://www.jbstatistics.com/category/discrete-probability-distributions/";
    private static final String BL_9702 = "https://www.bilibili.com/cheese/play/ss37608";
    private static final String BL_KHAN = "https://open.163.com/newview/movie/courseintro?newurl=MDAPTVFE8";

    private static void A(String key, String yt, String bl, String[]... res) {
        LinkSet ls = new LinkSet();
        ls.yt = yt; ls.bl = bl;
        for (String[] r : res) ls.res.add(r);
        T.put(key, ls);
    }

    private static String[] R(String a, String b) { return new String[]{a, b}; }

    static {
        // ===== Pure Mathematics 1 =====
        A("P1 Ch1", "CIE 9709 P1 quadratics completing the square discriminant", "A Level 一元二次方程 判别式 配方",
                R("The Maths Prof · 二次方程 (CIE P1)", YT_QUAD), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch2", "CIE 9709 P1 functions domain range inverse composite", "A Level 函数 定义域 值域 反函数",
                R("CIE 9709 章节索引", IDX9709), R("TLMaths 全站索引", TLM), R("可汗学院·函数", BL_KHAN));
        A("P1 Ch3", "CIE 9709 P1 coordinate geometry circle equation", "A Level 坐标几何 圆的方程",
                R("The Maths Prof · 坐标几何 (CIE P1)", YT_COORD), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch4", "CIE 9709 P1 circular measure arc length sector area", "A Level 弧度制 弧长 扇形面积",
                R("ExamSolutions · 弧长与扇形", YT_ARC), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch5", "CIE 9709 P1 trigonometry identities equations graphs", "A Level 三角函数 图像 恒等式",
                R("CIE P1 三角恒等式与方程", YT_TRIG), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch6", "CIE 9709 P1 binomial expansion arithmetic geometric series", "A Level 二项式 等差 等比数列",
                R("CIE AS P1 真题精讲", YT_HACK), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch7", "CIE 9709 P1 differentiation chain rule tangent normal", "A Level 导数 链式法则 切线",
                R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch8", "CIE 9709 P1 stationary points rates of change", "A Level 驻点 极大值 极小值 变化率",
                R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
        A("P1 Ch9", "CIE 9709 P1 integration area volume trapezium rule", "A Level 定积分 面积 旋转体体积",
                R("ExamSolutions · 积分技巧", YT_PARTS), R("CIE 9709 章节索引", IDX9709));

        // ===== Pure Mathematics 2 & 3 =====
        A("P2/3 Ch1", "CIE 9709 P2 modulus polynomial division factor remainder theorem", "A Level 绝对值 多项式除法 余数定理",
                R("TLMaths · 多项式与部分分式", YT_POLY), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch2", "CIE 9709 P2 logarithms exponential natural log", "A Level 对数 指数方程 自然对数",
                R("TLMaths · 对数与指数", YT_LOGS), R("Bicen Maths · 指数与对数", YT_BICEN));
        A("P2/3 Ch3", "CIE 9709 P3 sec cosec cot compound double angle", "A Level 三角恒等式 和角 倍角 辅助角",
                R("ExamSolutions · R sin(θ±α)", YT_HARM), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch4", "CIE 9709 P3 product quotient implicit parametric", "A Level 乘积法则 商法则 隐函数求导",
                R("TLMaths 全站索引", TLM), R("ExamSolutions", EXAM), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch5", "CIE 9709 P3 integration exponential trig trapezium", "A Level 积分 指数 三角 梯形法则",
                R("ExamSolutions · 积分技巧", YT_PARTS), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch6", "CIE 9709 P3 numerical solution iteration Newton-Raphson", "A Level 数值解法 迭代法 牛顿法",
                R("TLMaths · 迭代法与牛顿法", YT_NUM), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch7", "CIE 9709 P3 partial fractions binomial expansion", "A Level 部分分式 二项式展开",
                R("TLMaths · 多项式与部分分式", YT_POLY), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch8", "CIE 9709 P3 integration by substitution by parts", "A Level 换元积分 分部积分",
                R("ExamSolutions · 分部积分", YT_PARTS), R("TLMaths 全站索引", TLM));
        A("P2/3 Ch9", "CIE 9709 P3 vectors scalar product planes", "A Level 向量 数量积 平面",
                R("3Blue1Brown · 线性代数本质", YT_3B1B), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch10", "CIE 9709 P3 differential equations separating variables", "A Level 微分方程 分离变量",
                R("MIT 18.03 微分方程", MIT1803), R("CIE 9709 章节索引", IDX9709));
        A("P2/3 Ch11", "CIE 9709 P3 complex numbers Argand de Moivre", "A Level 复数 复平面 棣莫弗定理",
                R("Welch Labs · 虚数是什么", YT_WELCH), R("CIE 9709 章节索引", IDX9709));

        // ===== Mechanics 1 =====
        A("M1 Ch1", "CIE 9709 Mechanics kinematics suvat velocity time graph", "A Level 力学 运动学 匀加速",
                R("ExamSolutions · SUVAT", YT_SUVAT), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch2", "CIE 9709 Mechanics forces Newton laws", "A Level 力学 牛顿定律 受力分析",
                R("ExamSolutions · 力与力矩", YT_MECH), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch3", "CIE 9709 Mechanics resolving forces equilibrium Lami", "A Level 力学 力的分解 平衡",
                R("ExamSolutions · 力与力矩", YT_MECH), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch4", "CIE 9709 Mechanics friction limiting equilibrium", "A Level 力学 摩擦力 摩擦极限",
                R("ExamSolutions · 力学", YT_MECH), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch5", "CIE 9709 Mechanics connected particles pulleys", "A Level 力学 连接体 滑轮",
                R("ExamSolutions · 力学", YT_MECH), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch6", "CIE 9709 Mechanics variable acceleration calculus", "A Level 力学 变加速 积分",
                R("ExamSolutions · 力学", YT_MECH), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch7", "CIE 9709 Mechanics momentum collisions conservation", "A Level 力学 动量 碰撞",
                R("ExamSolutions · 碰撞与动量", YT_MOM), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch8", "CIE 9709 Mechanics work energy kinetic potential", "A Level 力学 功 动能 势能",
                R("Flipping Physics · 功与能", YT_WEP), R("MIT 8.01 经典力学", MIT801));
        A("M1 Ch9", "CIE 9709 Mechanics work-energy principle power", "A Level 力学 功能原理 功率",
                R("Flipping Physics · 功与能", YT_WEP), R("MIT 8.01 经典力学", MIT801));

        // ===== Statistics 1 =====
        A("S1 Ch1", "CIE 9709 Statistics stem leaf histogram cumulative frequency", "A Level 统计 茎叶图 直方图",
                R("ExamSolutions · 统计模块", YT_STATS), R("StatQuest", STATQ));
        A("S1 Ch2", "CIE 9709 Statistics mean median mode", "A Level 统计 平均数 中位数 众数",
                R("ExamSolutions · 统计模块", YT_STATS), R("StatQuest", STATQ));
        A("S1 Ch3", "CIE 9709 Statistics variance standard deviation quartiles", "A Level 统计 方差 标准差",
                R("ExamSolutions · 统计模块", YT_STATS), R("StatQuest", STATQ));
        A("S1 Ch4", "CIE 9709 Statistics probability conditional independent", "A Level 统计 概率 条件概率",
                R("StatQuest · 条件概率", "https://youtu.be/_IgyaD7vOOA"), R("jbstatistics", JBSTAT));
        A("S1 Ch5", "CIE 9709 Statistics permutations combinations", "A Level 统计 排列 组合",
                R("ExamSolutions · 统计模块", YT_STATS), R("StatQuest", STATQ));
        A("S1 Ch6", "CIE 9709 Statistics discrete random variable expectation", "A Level 统计 离散随机变量 期望",
                R("jbstatistics · 离散分布", YT_JBDISC), R("StatQuest", STATQ));
        A("S1 Ch7", "CIE 9709 Statistics binomial geometric distribution", "A Level 统计 二项分布 几何分布",
                R("jbstatistics · 离散分布", YT_JBDISC), R("StatQuest", STATQ));
        A("S1 Ch8", "CIE 9709 Statistics normal distribution standardisation", "A Level 统计 正态分布",
                R("jbstatistics · 假设检验与分布", JBSTAT), R("StatQuest", STATQ));

        // ===== Further Mathematics =====
        String[][] fm = {
                {"Ch1", "roots of polynomial equations cubic quartic", "多项式方程的根 三次 四次"},
                {"Ch2", "rational functions asymptotes oblique", "有理函数 渐近线"},
                {"Ch3", "summation of series sigma r", "级数求和 Σr"},
                {"Ch4", "matrices inverse determinant transformation", "矩阵 逆矩阵 行列式"},
                {"Ch5", "polar coordinates area", "极坐标 极坐标面积"},
                {"Ch6", "vector product cross product planes", "向量积 叉积 平面"},
                {"Ch7", "proof by induction divisibility", "数学归纳法 整除"},
                {"Ch8", "continuous random variable pdf cdf", "连续随机变量 概率密度"},
                {"Ch9", "t distribution hypothesis test confidence interval", "t分布 假设检验 置信区间"},
                {"Ch10", "chi squared test goodness of fit contingency", "卡方检验 拟合优度"},
                {"Ch11", "non-parametric Wilcoxon sign rank test", "非参数检验 威尔科克森"},
                {"Ch12", "probability generating function", "概率母函数"},
                {"Ch13", "projectiles trajectory", "抛体运动 轨迹方程"},
                {"Ch14", "equilibrium rigid body centre of mass", "刚体平衡 重心 力矩"},
                {"Ch15", "circular motion vertical circle", "圆周运动 竖直圆"},
                {"Ch16", "Hooke law elastic potential energy", "胡克定律 弹性势能"},
                {"Ch17", "linear motion variable force", "变力作用 直线运动"},
                {"Ch18", "momentum impulse oblique collision", "动量 冲量 斜碰撞"},
                {"Ch19", "hyperbolic functions inverse", "双曲函数 反双曲函数"},
                {"Ch20", "eigenvalues eigenvectors diagonalisation", "特征值 特征向量 对角化"},
                {"Ch21", "implicit parametric Maclaurin series", "隐函数 参数方程 麦克劳林级数"},
                {"Ch22", "reduction formula arc length surface area", "递推公式 弧长 旋转曲面"},
                {"Ch23", "de Moivre roots of unity complex summation", "棣莫弗定理 单位根 复数求和"},
                {"Ch24", "second order differential equations", "二阶微分方程"},
        };
        for (String[] row : fm) {
            List<String[]> res = new ArrayList<String[]>();
            res.add(R("CIE 9231 章节索引", IDX9231));
            if ("Ch4".equals(row[0]) || "Ch20".equals(row[0])) res.add(R("MIT 18.06 线性代数", MIT1806));
            if ("Ch21".equals(row[0])) res.add(R("ExamSolutions · 麦克劳林级数", YT_MACL));
            if ("Ch22".equals(row[0]) || "Ch24".equals(row[0])) res.add(R("MIT 18.03 微分方程", MIT1803));
            A("FM " + row[0], "CIE 9231 Further Maths " + row[1], "A Level 进阶数学 " + row[2],
                    res.toArray(new String[res.size()][]));
        }

        // ===== Physics =====
        String[][] phy = {
                {"Ch1", "kinematics speed velocity vectors", "运动学 速率 速度 矢量"},
                {"Ch2", "accelerated motion projectiles suvat", "加速运动 匀加速 抛体"},
                {"Ch3", "dynamics force mass Newton laws", "动力学 力 牛顿定律"},
                {"Ch4", "forces moments centre of gravity", "力 力矩 重心"},
                {"Ch5", "work energy power", "功 能量 功率"},
                {"Ch6", "momentum collisions explosions", "动量 碰撞 爆炸"},
                {"Ch7", "density pressure Archimedes Young modulus", "密度 压强 杨氏模量"},
                {"Ch8", "electric current voltage resistance power", "电流 电压 电阻 电功率"},
                {"Ch9", "Kirchhoff laws resistor combinations", "基尔霍夫定律 电阻组合"},
                {"Ch10", "resistance resistivity Ohm law I-V", "电阻 电阻率 欧姆定律"},
                {"Ch11", "internal resistance potential divider sensors", "内阻 分压器 传感器"},
                {"Ch12", "waves Doppler electromagnetic polarisation", "波 多普勒 电磁波 偏振"},
                {"Ch13", "superposition diffraction interference grating", "叠加 衍射 干涉 光栅"},
                {"Ch14", "stationary waves nodes antinodes", "驻波 波节 波腹"},
                {"Ch15", "atomic structure radioactivity particles", "原子结构 放射性 粒子物理"},
                {"Ch16", "circular motion centripetal force", "圆周运动 向心力"},
                {"Ch17", "gravitational field potential orbits", "引力场 引力势 轨道"},
                {"Ch18", "oscillations simple harmonic resonance", "振动 简谐运动 共振"},
                {"Ch19", "thermal physics specific heat latent", "热学 比热容 潜热"},
                {"Ch20", "ideal gas kinetic theory Boyle", "理想气体 分子运动论"},
                {"Ch21", "uniform electric field Coulomb", "匀强电场 电场强度 库仑"},
        };
        for (String[] row : phy) {
            List<String[]> res = new ArrayList<String[]>();
            res.add(R("A Level Physics Online", PHYON));
            int n = Integer.parseInt(row[0].substring(2));
            if (n <= 6) res.add(R("MIT 8.01 经典力学", MIT801));
            else if (n <= 11) res.add(R("MIT 8.02 电磁学", MIT802));
            else if (n <= 18) res.add(R("MIT 8.03 振动与波", MIT803));
            else res.add(R("MIT 8.04 量子物理", MIT804));
            res.add(R("Bilibili · CIE 9702 直击考点", BL_9702));
            A("Phy " + row[0], "CIE 9702 A Level Physics " + row[1], "A Level 物理 " + row[2],
                    res.toArray(new String[res.size()][]));
        }
        A("Phy P1", "CIE 9702 practical skills uncertainty error", "A Level 物理 实验 不确定度",
                R("A Level Physics Online · 实验技能", PHY_PRAC), R("A Level Physics Online · 不确定度", PHY_UNC));

        // ===== EAP =====
        A("EAP", "EAP academic writing essay structure referencing", "学术英语 学术写作 参考文献",
                R("EAP Foundation · 学术写作", EAPF + "writing/"),
                R("EAP Foundation · 听讲座与笔记", EAPF + "listening/lectures/"),
                R("Manchester Academic Phrasebank", PHRASE),
                R("Purdue OWL 引用规范", "https://owl.purdue.edu/owl/research_and_citation/resources.html"));
    }

    // ------------------------------------------------------------------
    private static final Pattern BOOK_RX = Pattern.compile("(P2/3|P1|M1|S1|FM|Phy)\\s+(Ch\\d+|P1)\\b");
    private static final Pattern PAPER_RX = Pattern.compile("真题精练\\s*(\\d{4})\\s*([A-Za-z0-9/]+)");
    private static final Pattern SEC_RX = Pattern.compile("§(\\d{1,2}\\.\\d{1,2})");

    public static String bookCodeFor(ScheduleData.Slot s) {
        if (s == null) return null;
        String t = s.title();
        if (s.subject.contains("复习")) return null;
        Matcher m = BOOK_RX.matcher(t);
        if (m.find()) return m.group(1);
        Matcher pm = PAPER_RX.matcher(t);
        if (pm.find()) {
            String paper = pm.group(1), sub = pm.group(2).toUpperCase();
            if ("9702".equals(paper)) return "Phy";
            if ("9231".equals(paper)) return "FM";
            if ("9709".equals(paper)) {
                if (sub.startsWith("P1")) return "P1";
                if (sub.startsWith("P3")) return "P2/3";
                if (sub.startsWith("M1") || sub.startsWith("P4")) return "M1";
                if (sub.startsWith("S1") || sub.startsWith("P6")) return "S1";
            }
        }
        return null;
    }

    public static final Map<String, String> BOOK_NAMES = new HashMap<String, String>();
    static {
        BOOK_NAMES.put("P1", "Pure Mathematics 1 - Coursebook");
        BOOK_NAMES.put("P2/3", "Pure Mathematics 2 & 3 - Coursebook");
        BOOK_NAMES.put("M1", "Mechanics - Coursebook");
        BOOK_NAMES.put("S1", "Probability & Statistics 1 - Coursebook");
        BOOK_NAMES.put("FM", "Further Mathematics - Coursebook");
        BOOK_NAMES.put("Phy", "Physics - Coursebook");
    }

    public static String bookTitleForSlot(ScheduleData.Slot s) {
        String code = bookCodeFor(s);
        if (code == null) return "";
        String n = BOOK_NAMES.get(code);
        return n == null ? "" : n;
    }

    /** 某个时段可用的资源链接 {标题, URL} */
    public static List<String[]> forSlot(ScheduleData.Slot s) {
        List<String[]> out = new ArrayList<String[]>();
        if (s == null) return out;
        String key = null;
        Matcher m = BOOK_RX.matcher(s.title());
        if (m.find()) key = m.group(1) + " " + m.group(2);

        LinkSet ls = key == null ? null : T.get(key);
        if (ls == null) {
            String t = s.title() + " " + s.subject;
            if (t.contains("真题")) ls = T.get("P1 Ch1");
            if (ls == null && (t.contains("EAP") || t.contains("学术英语"))) ls = T.get("EAP");
            if (ls == null && t.contains("复习")) ls = T.get("P1 Ch1");
            if (ls == null && t.contains("物理")) ls = T.get("Phy Ch1");
            if (ls == null && t.contains("纯数")) ls = T.get("P1 Ch1");
            if (ls == null && t.contains("应用")) ls = T.get("M1 Ch1");
            if (ls == null && t.contains("进阶")) ls = T.get("FM Ch1");
            if (ls == null) ls = T.get("P1 Ch1");
        }
        if (ls == null) return out;
        out.addAll(ls.res);
        if (ls.yt.length() > 0)
            out.add(R("▶ YouTube 搜索", "https://www.youtube.com/results?search_query=" + enc(ls.yt)));
        if (ls.bl.length() > 0)
            out.add(R("▶ Bilibili 搜索（免翻墙）", "https://search.bilibili.com/all?keyword=" + enc(ls.bl)));
        return out;
    }

    /** 该时段第一个小节号（用于显示，比如 §1.5） */
    public static String firstSection(ScheduleData.Slot s) {
        if (s == null) return "";
        Matcher m = SEC_RX.matcher(s.title());
        return m.find() ? m.group(1) : "";
    }

    private static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
