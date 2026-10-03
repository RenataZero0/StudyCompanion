package com.ankiassistant;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 设置页：Anki 连接、AI 服务商、默认值、关于。
 */
public class SettingsView extends LinearLayout {

    private final MainActivity act;
    private final Store store;

    private EditText hostInput, portInput, ankiKeyInput;
    private EditText aiKeyInput, modelInput, urlInput;
    private EditText deckInput, tagInput, subjectInput;
    private CheckBox autoSyncBox;
    private Button providerBtn, testAnkiBtn, testAiBtn, saveBtn;
    private TextView ankiStatus, aiStatus;

    public SettingsView(MainActivity context) {
        super(context);
        act = context;
        store = context.store;
        setOrientation(VERTICAL);
        setPadding(0, 0, 0, 0);
        build();
    }

    // ------------------------------------------------------------------ 小部件

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.VERTICAL);
        Ui.card(l);
        l.setPadding(Ui.dp(14), Ui.dp(13), Ui.dp(14), Ui.dp(13));
        return l;
    }

    private TextView heading(String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextColor(Ui.INK);
        t.setTextSize(16);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView label(String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextColor(Ui.SUB);
        t.setTextSize(12);
        t.setPadding(0, Ui.dp(9), 0, Ui.dp(2));
        return t;
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(getContext());
        e.setSingleLine(true);
        e.setTextSize(14.5f);
        e.setTextColor(Ui.INK);
        Ui.hint(e, hint);
        if (password) e.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setBackground(Ui.roundStroke(Ui.WHITE, Ui.LINE, 9));
        e.setPadding(Ui.dp(10), Ui.dp(7), Ui.dp(10), Ui.dp(7));
        return e;
    }

    private TextView status() {
        TextView t = new TextView(getContext());
        t.setTextSize(12.5f);
        t.setTextColor(Ui.TEXT_DIM);
        t.setPadding(0, Ui.dp(7), 0, 0);
        return t;
    }

    private void addTo(LinearLayout card, String label, EditText input) {
        card.addView(label(label));
        card.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ------------------------------------------------------------------ 构建

    private void build() {
        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(14));
        scroll.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ================= Anki 连接 =================
        LinearLayout anki = card();
        anki.addView(heading("Anki 连接"));
        TextView ankiTip = new TextView(getContext());
        ankiTip.setText("填电脑的局域网 IP（电脑和本机连同一个 Wi-Fi）。Anki 要开着。");
        ankiTip.setTextColor(Ui.TEXT_DIM);
        ankiTip.setTextSize(12.5f);
        ankiTip.setPadding(0, Ui.dp(3), 0, 0);
        anki.addView(ankiTip);

        hostInput = input("例如 192.168.1.7", false);
        portInput = input("8765", false);
        ankiKeyInput = input("AnkiConnect 的 apiKey（没设就留空）", true);
        addTo(anki, "电脑的 IP 或主机名", hostInput);
        addTo(anki, "端口", portInput);
        addTo(anki, "API Key", ankiKeyInput);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, Ui.dp(10), 0, 0);
        testAnkiBtn = new Button(getContext());
        testAnkiBtn.setText("测试连接");
        Ui.primary(testAnkiBtn);
        testAnkiBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { testAnki(); }
        });
        row.addView(testAnkiBtn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        anki.addView(row);

        ankiStatus = status();
        anki.addView(ankiStatus);

        anki.addView(heading("怎么配置（电脑上只做一次）"));
        TextView guide = new TextView(getContext());
        guide.setText("1. 电脑打开 Anki → 工具 → 插件 → 获取插件，输入 2055492159（AnkiConnect）→ 重启 Anki\n\n"
                + "2. 插件列表里选中 AnkiConnect → 配置，把 webBindAddress 改成 \"0.0.0.0\"（允许局域网访问），"
                + "保存后再重启 Anki；想加密码就在配置里加 \"apiKey\": \"一串字符\"\n\n"
                + "3. 电脑上 Win+R 输入 ipconfig 看 IPv4 地址，填到上面的「电脑的 IP」\n\n"
                + "4. 点「测试连接」：第一次会在电脑上的 Anki 弹出允许提示，点允许即可\n\n"
                + "5. 保存卡片时会自动调用 sync()，推送到你的 AnkiWeb 云端账号");
        guide.setTextColor(Ui.TEXT_BODY);
        guide.setTextSize(13);
        guide.setLineSpacing(0, 1.15f);
        guide.setPadding(0, Ui.dp(6), 0, 0);
        anki.addView(guide);

        LinearLayout.LayoutParams ankiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ankiLp.bottomMargin = Ui.dp(11);
        col.addView(anki, ankiLp);

        // ================= AI =================
        LinearLayout ai = card();
        ai.addView(heading("AI 自动填充"));
        TextView aiTip = new TextView(getContext());
        aiTip.setText("选一个服务商，填好 Key（免密钥的不用填），点「测试」。");
        aiTip.setTextColor(Ui.TEXT_DIM);
        aiTip.setTextSize(12.5f);
        aiTip.setPadding(0, Ui.dp(3), 0, 0);
        ai.addView(aiTip);

        providerBtn = new Button(getContext());
        providerBtn.setText("选择服务商");
        Ui.secondary(providerBtn);
        providerBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickProvider(); }
        });
        ai.addView(label("服务商"));
        ai.addView(providerBtn);

        aiKeyInput = input("API Key（Bearer）", true);
        modelInput = input("模型名", false);
        urlInput = input("接口地址", false);
        addTo(ai, "API Key", aiKeyInput);
        addTo(ai, "模型", modelInput);
        addTo(ai, "接口地址（一般不用改）", urlInput);

        LinearLayout aiRow = new LinearLayout(getContext());
        aiRow.setOrientation(LinearLayout.HORIZONTAL);
        aiRow.setPadding(0, Ui.dp(10), 0, 0);
        testAiBtn = new Button(getContext());
        testAiBtn.setText("测试 AI");
        Ui.primary(testAiBtn);
        testAiBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { testAi(); }
        });
        aiRow.addView(testAiBtn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ai.addView(aiRow);
        aiStatus = status();
        ai.addView(aiStatus);

        LinearLayout.LayoutParams aiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aiLp.bottomMargin = Ui.dp(11);
        col.addView(ai, aiLp);

        // ================= 默认值 =================
        LinearLayout def = card();
        def.addView(heading("默认值"));
        deckInput = input("NCUK::专业术语", false);
        tagInput = input("自建 数学", false);
        subjectInput = input("给 AI 的学科背景", false);
        addTo(def, "默认牌组", deckInput);
        addTo(def, "默认标签", tagInput);
        addTo(def, "学科背景（影响 AI 释义风格）", subjectInput);

        autoSyncBox = new CheckBox(getContext());
        autoSyncBox.setText("保存后自动同步到 AnkiWeb 云端");
        autoSyncBox.setTextColor(Ui.TEXT_BODY);
        autoSyncBox.setTextSize(14);
        autoSyncBox.setPadding(0, Ui.dp(8), 0, 0);
        def.addView(autoSyncBox);

        saveBtn = new Button(getContext());
        saveBtn.setText("保存设置");
        Ui.primary(saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.topMargin = Ui.dp(10);
        def.addView(saveBtn, svLp);

        LinearLayout.LayoutParams defLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        defLp.bottomMargin = Ui.dp(11);
        col.addView(def, defLp);

        // ================= 关于 =================
        LinearLayout about = card();
        about.addView(heading("关于"));
        TextView ver = new TextView(getContext());
        ver.setText("Anki 助手 " + Version.VERSION_TAG
                + "\n与 StudyCompanion 同一套设计语言；卡片格式见 PRMOPT.md 第二节第 3 条。");
        ver.setTextColor(Ui.TEXT_BODY);
        ver.setTextSize(13);
        ver.setLineSpacing(0, 1.2f);
        about.addView(ver);

        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, Ui.dp(9), 0, 0);

        Button exportBtn = new Button(getContext());
        exportBtn.setText("导出草稿");
        Ui.secondary(exportBtn);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportDrafts(); }
        });
        btnRow.addView(exportBtn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button crashBtn = new Button(getContext());
        crashBtn.setText("崩溃日志");
        Ui.secondary(crashBtn);
        crashBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCrashLog(); }
        });
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.leftMargin = Ui.dp(8);
        crashBtn.setLayoutParams(cbLp);
        btnRow.addView(crashBtn);
        about.addView(btnRow);

        col.addView(about);

        loadValues();
    }

    // ------------------------------------------------------------------ 读写

    private void loadValues() {
        hostInput.setText(store.ankiHost());
        portInput.setText(String.valueOf(store.ankiPort()));
        ankiKeyInput.setText(store.ankiApiKey());
        aiKeyInput.setText(store.aiApiKey());
        modelInput.setText(store.aiModel());
        urlInput.setText(store.aiBaseUrl());
        deckInput.setText(store.defaultDeck());
        tagInput.setText(store.defaultTags());
        subjectInput.setText(store.subject());
        autoSyncBox.setChecked(store.autoSync());
        refreshProviderBtn();
    }

    public void onShown() {
        // 从别的页面回来时，输入框里可能已被改过，重新读一次（用户没保存就不覆盖已有输入）
        if (hostInput.getText().length() == 0) loadValues();
        refreshProviderBtn();
    }

    private void save() {
        store.setAnkiHost(hostInput.getText().toString());
        store.setAnkiPort(portInput.getText().toString().trim().length() == 0
                ? "8765" : portInput.getText().toString().trim());
        store.setAnkiApiKey(ankiKeyInput.getText().toString());
        store.setAiApiKey(aiKeyInput.getText().toString());
        store.setAiModel(modelInput.getText().toString());
        store.setAiBaseUrl(urlInput.getText().toString());
        store.setDefaultDeck(deckInput.getText().toString());
        store.setDefaultTags(tagInput.getText().toString());
        store.setSubject(subjectInput.getText().toString());
        store.setAutoSync(autoSyncBox.isChecked());
        ankiStatus.setText("设置已保存 ✓");
        ankiStatus.setTextColor(Ui.GREEN);
    }

    private void refreshProviderBtn() {
        providerBtn.setText("当前：" + AiClient.presetLabel(store.aiProvider()) + "  ▾");
    }

    // ------------------------------------------------------------------ 动作

    private void pickProvider() {
        final String[] ids = {AiClient.P_DEEPSEEK, AiClient.P_DOUBAO, AiClient.P_ZHIPU,
                AiClient.P_POLLINATIONS, AiClient.P_CUSTOM};
        String[] labels = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            labels[i] = AiClient.presetLabel(ids[i])
                    + (AiClient.needsKey(ids[i]) ? "" : "（无需 Key）");
        }
        new AlertDialog.Builder(act)
                .setTitle("选择 AI 服务商")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = ids[which];
                        store.setAiProvider(id);
                        if (!AiClient.P_CUSTOM.equals(id)) {
                            // 用预设地址/模型覆盖（用户仍可手动改，改完记得保存）
                            urlInput.setText(AiClient.presetBaseUrl(id));
                            modelInput.setText(AiClient.presetModel(id));
                            store.setAiBaseUrl(urlInput.getText().toString());
                            store.setAiModel(modelInput.getText().toString());
                        }
                        refreshProviderBtn();
                        aiStatus.setText("已切换到 " + AiClient.presetLabel(id)
                                + (AiClient.needsKey(id) ? "，请填 API Key 后点「测试 AI」" : "，可以直接测试"));
                        aiStatus.setTextColor(Ui.AMBER);
                    }
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    private void testAnki() {
        save();
        ankiStatus.setText("正在连接（第一次可能要在电脑上点允许）…");
        ankiStatus.setTextColor(Ui.SUB);
        testAnkiBtn.setEnabled(false);
        Th.bg(new Runnable() {
            @Override
            public void run() {
                try {
                    AnkiClient anki = new AnkiClient(store.ankiHost(), store.ankiPort(), store.ankiApiKey());
                    anki.requestPermission();
                    final String ver = anki.versionString();
                    final JSONArray decks = anki.deckNames();
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            testAnkiBtn.setEnabled(true);
                            ankiStatus.setText("连接成功 ✓ AnkiConnect v" + ver
                                    + "，共 " + decks.length() + " 个牌组");
                            ankiStatus.setTextColor(Ui.GREEN);
                        }
                    });
                } catch (final AnkiClient.AnkiException e) {
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            testAnkiBtn.setEnabled(true);
                            ankiStatus.setText("连接失败：" + e.getMessage());
                            ankiStatus.setTextColor(Ui.RED);
                        }
                    });
                }
            }
        });
    }

    private void testAi() {
        save();
        aiStatus.setText("正在调用 AI…");
        aiStatus.setTextColor(Ui.SUB);
        testAiBtn.setEnabled(false);
        Th.bg(new Runnable() {
            @Override
            public void run() {
                try {
                    AiClient ai = new AiClient();
                    String r = ai.chat(store.aiBaseUrlEffective(), store.aiApiKey(),
                            store.aiModelEffective(), CardFormat.SYSTEM, "只回复两个字：可用");
                    final String out = r.trim();
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            testAiBtn.setEnabled(true);
                            aiStatus.setText("AI 调用成功 ✓ 模型回复：" + out);
                            aiStatus.setTextColor(Ui.GREEN);
                        }
                    });
                } catch (final AiClient.AiException e) {
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            testAiBtn.setEnabled(true);
                            aiStatus.setText("AI 调用失败：" + e.getMessage());
                            aiStatus.setTextColor(Ui.RED);
                        }
                    });
                }
            }
        });
    }

    private void exportDrafts() {
        JSONArray arr = store.drafts();
        if (arr.length() == 0) {
            ankiStatus.setText("没有草稿可导出");
            ankiStatus.setTextColor(Ui.AMBER);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("单词\t音标\t词性\t定义\t关联公式/符号\t易混\t牌组\t标签\n");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject d = arr.optJSONObject(i);
            if (d == null) continue;
            JSONObject f = d.optJSONObject("fields");
            if (f == null) f = new JSONObject();
            sb.append(tsv(f.optString("单词", d.optString("word", "")))).append('\t');
            sb.append(tsv(f.optString("音标", ""))).append('\t');
            sb.append(tsv(f.optString("词性", ""))).append('\t');
            sb.append(tsv(f.optString("定义", ""))).append('\t');
            sb.append(tsv(f.optString("关联公式/符号", ""))).append('\t');
            sb.append(tsv(f.optString("易混", ""))).append('\t');
            sb.append(tsv(d.optString("deck", ""))).append('\t');
            sb.append(tsv(d.optString("tags", ""))).append('\n');
        }
        Intent it = new Intent(Intent.ACTION_SEND);
        it.setType("text/plain");
        it.putExtra(Intent.EXTRA_SUBJECT, "Anki 助手草稿.tsv");
        it.putExtra(Intent.EXTRA_TEXT, sb.toString());
        act.startActivity(Intent.createChooser(it, "导出草稿（可导入 Anki）"));
    }

    private static String tsv(String s) {
        if (s == null) return "";
        return s.replace("\t", " ").replace("\r\n", " ").replace("\n", " ")
                .replace("<br>", " ").replaceAll("<[^>]+>", "");
    }

    private void showCrashLog() {
        String log = CrashHandler.last(getContext());
        if (log == null) {
            ankiStatus.setText("没有崩溃记录 ✓");
            ankiStatus.setTextColor(Ui.GREEN);
            return;
        }
        new AlertDialog.Builder(act)
                .setTitle("上次崩溃日志")
                .setMessage(log)
                .setPositiveButton("复制", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager cm = (ClipboardManager)
                                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("crash", CrashHandler.last(getContext())));
                    }
                })
                .setNeutralButton("清除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CrashHandler.clear(getContext());
                    }
                })
                .setNegativeButton("关闭", null)
                .create().show();
    }
}
