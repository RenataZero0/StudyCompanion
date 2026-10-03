import com.ankiassistant.AiClient;
import com.ankiassistant.AnkiClient;
import com.ankiassistant.CardFormat;

import org.json.JSONObject;

/**
 * 电脑端自检：脱离安卓直接跑「纯逻辑」部分。
 * 覆盖 PRMOPT.md 第二节第 3 条的卡片格式、AI 提示词/解析、AnkiConnect 请求与响应解析。
 * 运行：powershell -ExecutionPolicy Bypass -File selftest.ps1
 */
public class SelfTest {

    static int pass = 0, fail = 0;

    static void ok(String name, boolean cond, String detail) {
        if (cond) { pass++; System.out.println("  [ok] " + name); }
        else { fail++; System.out.println("  [FAIL] " + name + "  -> " + detail); }
    }

    static void eq(String name, Object got, Object expect) {
        boolean same = (got == null) ? expect == null : got.equals(expect);
        ok(name, same, "got=" + got + " expect=" + expect);
    }

    public static void main(String[] args) throws Exception {
        format();
        prompt();
        parseAi();
        fields();
        anki();
        ai();
        templates();
        System.out.println();
        System.out.println("pass=" + pass + "  fail=" + fail);
        if (fail > 0) System.exit(1);
    }

    // ------------------------------------------------------------ 卡片格式

    static void format() {
        System.out.println("== 卡片格式（PRMOPT 第二节第 3 条） ==");
        eq("字段数=6", CardFormat.FIELDS.length, 6);
        eq("第一个字段是正面", CardFormat.FIELDS[0], "单词");
        String back = CardFormat.CARD_BACK;
        ok("背面含【音标】", back.indexOf("【音标】") >= 0, back);
        ok("背面含【词性】", back.indexOf("【词性】") >= 0, back);
        ok("背面含【定义】", back.indexOf("【定义】") >= 0, back);
        ok("背面含【关联公式/符号】", back.indexOf("【关联公式/符号】") >= 0, back);
        ok("背面含【易混】", back.indexOf("【易混】") >= 0, back);
        ok("正面就是单词", CardFormat.CARD_FRONT.indexOf("{{单词}}") >= 0, CardFormat.CARD_FRONT);
        for (int i = 1; i < CardFormat.FIELDS.length; i++) {
            String f = CardFormat.FIELDS[i];
            ok("模板条件块包裹 " + f,
                    back.indexOf("{{#" + f + "}}") >= 0 && back.indexOf("{{/" + f + "}}") >= 0, f);
        }
        ok("样式含 MathJax 友好字体栈", CardFormat.CARD_CSS.indexOf("font-family") >= 0, "");
    }

    static void prompt() {
        System.out.println("== AI 提示词 ==");
        String p = CardFormat.buildPrompt("probability", "数学");
        ok("包含单词", p.indexOf("probability") >= 0, p);
        ok("要求 JSON", p.indexOf("JSON") >= 0, p);
        ok("包含 phonetic", p.indexOf("phonetic") >= 0, p);
        ok("包含 pos", p.indexOf("\"pos\"") >= 0 || p.indexOf("pos：") >= 0, p);
        ok("包含 definition 格式说明", p.indexOf("([词性])") >= 0, p);
        ok("包含 MathJax 行内定界符", p.indexOf("\\(") >= 0, p);
        ok("包含 MathJax 独行定界符", p.indexOf("\\[") >= 0, p);
        ok("包含 confusables", p.indexOf("confusables") >= 0, p);
        ok("学科背景进了提示词", p.indexOf("数学") >= 0, p);
        String p2 = CardFormat.buildPrompt("vector", null);
        ok("学科留空也有默认背景", p2.indexOf("学科背景") >= 0 && p2.indexOf("物理") >= 0, p2);
    }

    // ------------------------------------------------------------ AI 解析

    static void parseAi() {
        System.out.println("== AI 返回解析 ==");
        String clean = "{\"phonetic\":\"/ˈpəʊl/\",\"pos\":\"n.\","
                + "\"definition\":\"(n.) a measurement of how far something moves\","
                + "\"formula\":\"\\\\( s = \\\\dfrac{d}{t} \\\\)\","
                + "\"confusables\":\"pool /puːl/ — 水池\"}";
        JSONObject o = CardFormat.parseAi(clean);
        ok("干净 JSON 能解析", o != null, String.valueOf(o));
        if (o != null) {
            eq("音标", o.optString("phonetic"), "/ˈpəʊl/");
            eq("词性", o.optString("pos"), "n.");
            ok("公式保留反斜杠", o.optString("formula").indexOf("\\(") >= 0, o.optString("formula"));
        }

        String fenced = "好的，这是生成的卡片：\n```json\n" + clean + "\n```\n希望有帮助！";
        JSONObject o2 = CardFormat.parseAi(fenced);
        ok("围栏代码块能解析", o2 != null, fenced);
        if (o2 != null) eq("围栏-释义", o2.optString("pos"), "n.");

        String noisy = "以下是结果 {\"phonetic\":\"/iːt/\",\"definition\":\"(v.) to take food\"} 完毕";
        JSONObject o3 = CardFormat.parseAi(noisy);
        ok("JSON 前后有废话也能解析", o3 != null, noisy);

        String zh = "{\"音标\":\"/tuː/\",\"词性\":\"num.\",\"定义\":\"(num.) the number 2\"}";
        JSONObject o4 = CardFormat.parseAi(zh);
        ok("中文键也能识别", o4 != null, zh);
        if (o4 != null) eq("中文键-音标", o4.optString("phonetic"), "/tuː/");

        ok("纯废话返回 null", CardFormat.parseAi("我觉得这个词不太好") == null, "should be null");
        ok("null 返回 null", CardFormat.parseAi(null) == null, "should be null");
        ok("空 JSON 返回 null", CardFormat.parseAi("{}") == null, "should be null");

        JSONObject fb = CardFormat.fallbackFields("x", "some raw text");
        ok("降级时整段进定义", fb.optString("definition").indexOf("some raw text") >= 0, fb.toString());
    }

    static void fields() {
        System.out.println("== 组装 Anki 字段 ==");
        JSONObject ai = new JSONObject();
        ai.put("phonetic", "/ˈeθ/");
        ai.put("pos", "n.");
        ai.put("definition", "(n.) the 5th letter");
        ai.put("formula", "\\( \\varepsilon \\)");
        ai.put("confusables", "eta /ˈiːtə/\nepsilon /ˈɛpsɪləʊn/");
        JSONObject f = CardFormat.noteFields("epsilon", ai);
        for (int i = 0; i < CardFormat.FIELDS.length; i++) {
            ok("字段存在 " + CardFormat.FIELDS[i], f.has(CardFormat.FIELDS[i]), CardFormat.FIELDS[i]);
        }
        eq("正面=单词", f.optString("单词"), "epsilon");
        eq("换行转成 <br>", f.optString("易混").indexOf("<br>"), f.optString("易混").indexOf("\n"));
        ok("换行确实变成了 <br>", f.optString("易混").indexOf("<br>") >= 0, f.optString("易混"));

        JSONObject ai2 = new JSONObject();
        ai2.put("definition", "a<b>c");
        JSONObject f2 = CardFormat.noteFields("<tag>", ai2);
        eq("正面尖括号转义", f2.optString("单词"), "&lt;tag&gt;");
        eq("释义尖括号转义", f2.optString("定义"), "a&lt;b&gt;c");

        JSONObject merged = CardFormat.mergeNote("word<1>", f);
        eq("merge 保留单词转义", merged.optString("单词"), "word&lt;1&gt;");
        eq("merge 保留释义", merged.optString("定义"), f.optString("定义"));
        eq("merge 多余键被丢掉(不会带 AI 的键名)", merged.optString("phonetic", ""), "");

        String plain = CardFormat.plainBack(f);
        ok("纯文本背面含标签", plain.indexOf("【音标】") >= 0, plain);
        ok("纯文本背面含正面词", plain.startsWith("epsilon"), plain);

        JSONObject empty = CardFormat.noteFields("w", null);
        ok("AI 为 null 时字段仍然齐全", empty.length() == CardFormat.FIELDS.length, empty.toString());
    }

    // ------------------------------------------------------------ AnkiConnect

    static void anki() throws Exception {
        System.out.println("== AnkiConnect ==");
        String req = AnkiClient.buildRequest("deckNames", null);
        JSONObject r = new JSONObject(req);
        eq("请求含 action", r.optString("action"), "deckNames");
        eq("请求含 version", r.optInt("version", -1), 6);
        ok("无 params 时不塞空对象", !r.has("params"), req);

        JSONObject p = new JSONObject();
        p.put("query", "deck:*");
        JSONObject r2 = new JSONObject(AnkiClient.buildRequest("findNotes", p));
        eq("params 透传", r2.getJSONObject("params").optString("query"), "deck:*");

        eq("正常响应返回 result", AnkiClient.parseResponse("{\"result\":[1,2],\"error\":null}").toString(),
                new org.json.JSONArray("[1,2]").toString());
        try {
            AnkiClient.parseResponse("{\"result\":null,\"error\":\"collection is not available\"}");
            ok("error 抛异常", false, "no exception");
        } catch (AnkiClient.AnkiException e) {
            ok("error 抛异常", "collection is not available".equals(e.getMessage()), e.getMessage());
        }
        try {
            AnkiClient.parseResponse("<html>502</html>");
            ok("非 JSON 抛异常", false, "no exception");
        } catch (AnkiClient.AnkiException e) {
            ok("非 JSON 抛异常", true, e.getMessage());
        }
        try {
            AnkiClient.parseResponse("");
            ok("空响应抛异常", false, "no exception");
        } catch (AnkiClient.AnkiException e) {
            ok("空响应抛异常", true, e.getMessage());
        }

        eq("补默认端口", AnkiClient.normalizeBase("192.168.1.7", 8765), "http://192.168.1.7:8765");
        eq("保留自定义端口", AnkiClient.normalizeBase("192.168.1.7:9999", 8765), "http://192.168.1.7:9999");
        eq("保留 http 协议", AnkiClient.normalizeBase("http://192.168.1.7", 8765), "http://192.168.1.7:8765");
        eq("去掉尾部斜杠与路径", AnkiClient.normalizeBase("http://192.168.1.7:8765/", 8765), "http://192.168.1.7:8765");
        eq("空值回退本机", AnkiClient.normalizeBase("", 8765), "http://127.0.0.1:8765");
        eq("https 不被改写", AnkiClient.normalizeBase("https://anki.example.com", 8765), "https://anki.example.com:8765");
    }

    // ------------------------------------------------------------ AI 请求

    static void ai() throws Exception {
        System.out.println("== AI 客户端 ==");
        String body = AiClient.buildRequestBody("deepseek-chat", "系统提示", "用户提示");
        JSONObject o = new JSONObject(body);
        eq("model", o.optString("model"), "deepseek-chat");
        eq("两条消息", o.getJSONArray("messages").length(), 2);
        eq("system 角色", o.getJSONArray("messages").getJSONObject(0).optString("role"), "system");
        eq("user 内容", o.getJSONArray("messages").getJSONObject(1).optString("content"), "用户提示");

        String resp = "{\"id\":\"x\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"pos\\\":\\\"n.\\\"}\"}}]}";
        eq("取 content", AiClient.parseContent(resp), "{\"pos\":\"n.\"}");

        try {
            AiClient.parseContent("{\"error\":{\"message\":\"Invalid API key\"}}");
            ok("error.message 被带上", false, "no exception");
        } catch (AiClient.AiException e) {
            ok("error.message 被带上", e.getMessage().indexOf("Invalid API key") >= 0, e.getMessage());
        }
        try {
            AiClient.parseContent("{\"choices\":[]}");
            ok("空 choices 报错", false, "no exception");
        } catch (AiClient.AiException e) {
            ok("空 choices 报错", true, e.getMessage());
        }
        try {
            AiClient.parseContent("not json");
            ok("非 JSON 报错", false, "no exception");
        } catch (AiClient.AiException e) {
            ok("非 JSON 报错", true, e.getMessage());
        }

        ok("DeepSeek 预设有地址", AiClient.presetBaseUrl(AiClient.P_DEEPSEEK).startsWith("https://"), "");
        ok("豆包预设有地址", AiClient.presetBaseUrl(AiClient.P_DOUBAO).indexOf("volces") > 0, "");
        ok("智谱预设是免费模型", AiClient.presetModel(AiClient.P_ZHIPU).indexOf("glm") >= 0, "");
        ok("Pollinations 不需要 Key", !AiClient.needsKey(AiClient.P_POLLINATIONS), "");
        ok("DeepSeek 需要 Key", AiClient.needsKey(AiClient.P_DEEPSEEK), "");
        ok("自定义档没有预设地址", AiClient.presetBaseUrl(AiClient.P_CUSTOM).length() == 0, "");
    }

    static void templates() {
        System.out.println("== 模板/格式兜底 ==");
        ok("笔记类型名非空", CardFormat.MODEL_NAME.length() > 0, "");
        String back = CardFormat.CARD_BACK;
        int opens = count(back, "{{#"), closes = count(back, "{{/");
        eq("条件块成对", opens, closes);
        eq("条件块数量=5", opens, 5);
        ok("模板里没有未替换的占位残渣", back.indexOf("{{单词}}") >= 0, back);
    }

    static int count(String s, String sub) {
        int n = 0, i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) { n++; i += sub.length(); }
        return n;
    }
}
