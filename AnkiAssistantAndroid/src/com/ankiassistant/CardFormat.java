package com.ankiassistant;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 卡片格式规范 —— 本应用的"唯一事实来源"。
 *
 * 需求（PRMOPT.md 第二节第 3 条）：
 *   正面：[输入的单词]
 *   背面：
 *     【音标】[此单词的音标]
 *     【词性】[此单词的词性]
 *     【定义】([词性]) [英文释义]; ([若有多个词性]) [此词性对应的释义]
 *     【关联公式/符号】[相关公式]（使用 Anki 内置 MathJax 格式）
 *     【易混】[相近单词]（列出读音与意义相近的词）
 *
 * 这里同时负责三件事：
 *   1) buildPrompt()  —— 生成喂给 AI 的提示词
 *   2) parseAi()      —— 把 AI 的返回（可能带 ```json 围栏、可能啰嗦）解析成字段
 *   3) noteFields()   —— 组装成 Anki 的笔记字段
 * 卡片模板 / 样式也定义在这里，保证「桌面端渲染」与「应用内预览」完全一致。
 *
 * 注意：这个类只依赖 org.json 与 java.*，所以能在电脑 JVM 上直接跑自检（tools/SelfTest.java）。
 */
public class CardFormat {

    public static final String MODEL_NAME = "专业术语卡";

    /** 笔段顺序（也是背面从上到下的顺序） */
    public static final String[] FIELDS = {
            "单词", "音标", "词性", "定义", "关联公式/符号", "易混"
    };

    public static final String SYSTEM =
            "你是资深的英汉词典编辑，同时熟悉 CIE A-Level / NCUK IFY 的数学与物理术语。"
            + "你只输出一个 JSON 对象，不输出任何解释、注释或 Markdown 代码块。";

    /** 卡片正面模板 */
    public static final String CARD_FRONT =
            "<div class=\"word\">{{单词}}</div>";

    /** 卡片背面模板 —— 用 {{#字段}} 条件块，空字段连标签一起隐藏 */
    public static final String CARD_BACK =
            "<div class=\"word\">{{单词}}</div>\n"
            + "<hr id=\"answer\">\n"
            + "<div class=\"body\">\n"
            + "{{#音标}}<div class=\"row\"><span class=\"lbl\">【音标】</span><span class=\"val\">{{音标}}</span></div>{{/音标}}\n"
            + "{{#词性}}<div class=\"row\"><span class=\"lbl\">【词性】</span><span class=\"val\">{{词性}}</span></div>{{/词性}}\n"
            + "{{#定义}}<div class=\"row\"><span class=\"lbl\">【定义】</span><span class=\"val\">{{定义}}</span></div>{{/定义}}\n"
            + "{{#关联公式/符号}}<div class=\"row\"><span class=\"lbl\">【关联公式/符号】</span><span class=\"val\">{{关联公式/符号}}</span></div>{{/关联公式/符号}}\n"
            + "{{#易混}}<div class=\"row\"><span class=\"lbl\">【易混】</span><span class=\"val\">{{易混}}</span></div>{{/易混}}\n"
            + "</div>";

    /** 模板样式（写进笔记类型，桌面端渲染用的是同一段 CSS） */
    public static final String CARD_CSS =
            ".card{font-family:-apple-system,'Segoe UI',Roboto,'Helvetica Neue',Arial,"
            + "'PingFang SC','Microsoft YaHei',sans-serif;font-size:20px;line-height:1.65;"
            + "text-align:left;color:#1b2432;background:#ffffff;padding:4px;}\n"
            + ".word{font-size:30px;font-weight:700;letter-spacing:.5px;}\n"
            + "hr#answer{border:none;border-top:2px solid #e5e9f0;margin:14px 0;}\n"
            + ".row{margin:9px 0;}\n"
            + ".lbl{color:#3568e8;font-weight:700;margin-right:8px;white-space:nowrap;}\n"
            + ".val{color:#1b2432;}\n"
            + "code{background:#f3f5f9;border-radius:4px;padding:1px 5px;}\n";

    // ---------------------------------------------------------------- 提示词

    /** 用户提示词：把格式要求原样告诉 AI */
    public static String buildPrompt(String word, String subject) {
        String subj = (subject == null || subject.trim().length() == 0)
                ? "数学、物理学科术语（若是学科术语请给出精确定义）" : subject.trim();
        return "请为单词「" + word.trim() + "」生成一张词卡。\n"
                + "学科背景：" + subj + "。\n"
                + "\n"
                + "只输出一个 JSON 对象，字段如下（值都是字符串，不要用 markdown）：\n"
                + "phonetic：英式音标，形如 /ˈprɒbəbəl/，多个读音用逗号分隔；不确定就留空字符串。\n"
                + "pos：词性缩写，如 n.  v.  adj.  adv.  prep.，多个用 \" ; \" 分隔。\n"
                + "definition：英文释义。严格按这个格式：([词性]) 英文释义; ([词性]) 另一个释义\n"
                + "        例如：(n.) the quality of being likely to happen; (adj.) likely to be the case\n"
                + "        若是数学/物理术语，用英文给出教材级别的精确定义。\n"
                + "formula：与该词相关的公式、符号或表达式，使用 Anki 内置 MathJax 格式，"
                + "行内公式用 \\( ... \\)，独立成行的公式用 \\[ ... \\]；没有就留空字符串。\n"
                + "confusables：2-3 个读音或意义相近、容易混淆的词，每个写成一行：\n"
                + "        词 /音标/ — 该词的意思（与原词的差别）\n"
                + "\n"
                + "输出示例：\n"
                + "{\"phonetic\":\"/ˈprɒbəbəl/\",\"pos\":\"adj. ; n.\","
                + "\"definition\":\"(adj.) likely to happen or be true; (n.) a person or thing that may be chosen or expected\","
                + "\"formula\":\"\\\\(P(A)=\\\\dfrac{n(A)}{n(S)}\\\\)\","
                + "\"confusables\":\"probable /ˈprɒbəbəl/ — 很可能的，几乎同义，语气稍弱\\n"
                + "possible /pəˈsɪbəl/ — 可能的，强调客观存在机会\\n"
                + "plausible /ˈplɔːzəbəl/ — 看似有道理的，侧重听上去合理\"}";
    }

    // ------------------------------------------------------------ AI 返回解析

    /**
     * 解析 AI 的原始返回。容忍：```json 围栏、前后缀废话、中文键名。
     * 解析不出来返回 null（调用方自行降级）。
     */
    public static JSONObject parseAi(String raw) {
        if (raw == null) return null;
        String s = stripFences(raw.trim());
        JSONObject o = tryObject(s);
        if (o == null) {
            int a = s.indexOf('{'), b = s.lastIndexOf('}');
            if (a >= 0 && b > a) o = tryObject(s.substring(a, b + 1));
        }
        if (o == null) return null;
        JSONObject out = new JSONObject();
        put(out, "phonetic", o, "phonetic", "音标", "pronunciation", "ipa");
        put(out, "pos", o, "pos", "词性", "partOfSpeech", "partsOfSpeech", "speech");
        put(out, "definition", o, "definition", "def", "释义", "定义", "meaning", "english");
        put(out, "formula", o, "formula", "math", "symbol", "公式", "关联公式", "关联公式/符号");
        put(out, "confusables", o, "confusables", "confusable", "similar", "易混", "易混词", "近义词");
        if (isNullAll(out)) return null;
        return out;
    }

    /** AI 完全没按格式输出时的降级：整段文字当成释义 */
    public static JSONObject fallbackFields(String word, String raw) {
        JSONObject o = new JSONObject();
        String text = raw == null ? "" : raw.trim();
        try {
            o.put("phonetic", "");
            o.put("pos", "");
            o.put("definition", text.replace('\n', ' ').replace('\r', ' '));
            o.put("formula", "");
            o.put("confusables", "");
        } catch (JSONException ignored) { }
        return o;
    }

    private static JSONObject tryObject(String s) {
        try {
            JSONObject o = new JSONObject(s);
            return o.length() == 0 ? null : o;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String stripFences(String s) {
        if (s.indexOf("```") < 0) return s;
        int start = s.indexOf("```");
        int end = s.lastIndexOf("```");
        if (end <= start) return s;
        String inner = s.substring(start + 3, end);
        int nl = inner.indexOf('\n');
        if (nl >= 0) {
            String first = inner.substring(0, nl).trim();
            if (first.length() == 0 || first.toLowerCase().matches("[a-z0-9_+-]+")) {
                inner = inner.substring(nl + 1);
            }
        }
        return inner.trim();
    }

    private static void put(JSONObject out, String key, JSONObject src, String... aliases) {
        try {
            for (int i = 0; i < aliases.length; i++) {
                if (src.has(aliases[i]) && !src.isNull(aliases[i])) {
                    String v = src.get(aliases[i]) instanceof String
                            ? src.getString(aliases[i])
                            : String.valueOf(src.get(aliases[i]));
                    if (v != null && v.trim().length() > 0) {
                        out.put(key, v.trim());
                        return;
                    }
                }
            }
            out.put(key, "");
        } catch (JSONException ignored) { }
    }

    private static boolean isNullAll(JSONObject o) {
        String[] keys = {"phonetic", "pos", "definition", "formula", "confusables"};
        for (int i = 0; i < keys.length; i++) {
            String v = o.optString(keys[i], "");
            if (v != null && v.trim().length() > 0) return false;
        }
        return true;
    }

    // ------------------------------------------------------------ 组装 Anki 字段

    /**
     * 把 AI 字段组装成 Anki 笔记字段（键名 = 笔记类型字段名）。
     * 新行转成 &lt;br&gt;，尖括号转义，防止 AI 输出的 HTML 破坏卡片。
     */
    public static JSONObject noteFields(String word, JSONObject ai) {
        JSONObject f = new JSONObject();
        try {
            f.put("单词", escape(word == null ? "" : word.trim()));
            f.put("音标", nl(ai == null ? "" : ai.optString("phonetic", "")));
            f.put("词性", nl(ai == null ? "" : ai.optString("pos", "")));
            f.put("定义", nl(ai == null ? "" : ai.optString("definition", "")));
            f.put("关联公式/符号", nl(ai == null ? "" : ai.optString("formula", "")));
            f.put("易混", nl(ai == null ? "" : ai.optString("confusables", "")));
        } catch (JSONException ignored) { }
        return f;
    }

    /** 供「手工填充」使用的空字段 */
    public static JSONObject emptyFields(String word) {
        return noteFields(word, null);
    }

    /**
     * 把编辑器里读回的背面字段 + 输入框里的单词，合成一张完整笔记的字段表。
     * 编辑器里的内容是用户手动改过的，以它为准（覆盖 AI 的原始输出）。
     */
    public static JSONObject mergeNote(String word, JSONObject back) {
        JSONObject f = new JSONObject();
        try {
            f.put("单词", escape(word == null ? "" : word.trim()));
            for (int i = 1; i < FIELDS.length; i++) {
                String name = FIELDS[i];
                f.put(name, back == null ? "" : back.optString(name, ""));
            }
        } catch (JSONException ignored) { }
        return f;
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 多行文本 -> Anki 字段（换行保留为 <br>） */
    private static String nl(String s) {
        if (s == null) return "";
        return escape(s).replace("\r\n", "\n").replace("\n", "<br>");
    }

    // ------------------------------------------------------------ 自检用

    /** 背面渲染出来的纯文本（不含标签），用于自检 */
    public static String plainBack(JSONObject fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(fields.optString("单词", "")).append('\n');
        String[] rest = {"音标", "词性", "定义", "关联公式/符号", "易混"};
        for (int i = 0; i < rest.length; i++) {
            String v = fields.optString(rest[i], "");
            if (v.length() > 0) sb.append("【").append(rest[i]).append("】").append(v.replace("<br>", "\n")).append('\n');
        }
        return sb.toString();
    }
}
