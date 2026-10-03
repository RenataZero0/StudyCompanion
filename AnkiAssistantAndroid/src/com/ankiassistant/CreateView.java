package com.ankiassistant;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 制卡页（主界面）：
 *   单词（正面） → AI 按 PRMOPT 第二节第 3 条的格式自动填充背面 → 富文本微调 → 保存到 Anki。
 *
 * 编辑与预览全部在 assets/editor.html 里完成（contenteditable + MathJax + 真正的卡片模板），
 * 这样输入与预览能力与 Anki 桌面端一致（加粗/斜体/颜色/列表/引用/代码/挖空/公式/HTML 源码）。
 */
public class CreateView extends LinearLayout {

    public interface FieldsCb { void onFields(JSONObject fields); }

    private final MainActivity act;
    private final Store store;

    private EditText wordInput, deckInput, tagInput;
    private TextView aiBadge;
    private Button aiBtn, saveBtn, draftBtn, clearBtn, pickBtn;
    private TextView statusLine;
    private WebView editor;
    private boolean editorReady;

    private static final String EDITOR_URL = "file:///android_asset/editor.html";

    public CreateView(MainActivity context) {
        super(context);
        act = context;
        store = context.store;
        setOrientation(VERTICAL);
        setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(8));
        build();
    }

    // ------------------------------------------------------------------ 静态部件

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(LinearLayout.VERTICAL);
        Ui.card(l);
        l.setPadding(Ui.dp(13), Ui.dp(11), Ui.dp(13), Ui.dp(11));
        return l;
    }

    private TextView smallLabel(String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextColor(Ui.SUB);
        t.setTextSize(12);
        return t;
    }

    private void build() {
        // ---- 正面：单词 ----
        LinearLayout wordCard = card();
        addView(wordCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        wordCard.addView(smallLabel("正面 · 单词（你输入的词就是卡片正面）"));
        wordInput = new EditText(getContext());
        wordInput.setSingleLine(true);
        wordInput.setTextSize(21);
        wordInput.setTextColor(Ui.INK);
        wordInput.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Ui.hint(wordInput, "例如 probability");
        wordInput.setPadding(0, Ui.dp(4), 0, Ui.dp(2));
        wordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                pushWord(s.toString());
            }
        });
        wordCard.addView(wordInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // AI 行
        LinearLayout aiRow = new LinearLayout(getContext());
        aiRow.setOrientation(LinearLayout.HORIZONTAL);
        aiRow.setGravity(Gravity.CENTER_VERTICAL);
        aiRow.setPadding(0, Ui.dp(6), 0, 0);

        aiBadge = new TextView(getContext());
        aiBadge.setTextSize(12);
        aiBadge.setTextColor(Ui.ACCENT);
        aiBadge.setPadding(0, 0, Ui.dp(8), 0);
        aiBadge.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { act.show(MainActivity.TAB_SETTINGS); }
        });
        aiRow.addView(aiBadge, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        clearBtn = new Button(getContext());
        clearBtn.setText("清空");
        Ui.secondary(clearBtn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { clearAll(); }
        });
        aiRow.addView(clearBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        aiBtn = new Button(getContext());
        aiBtn.setText("AI 填充");
        Ui.primary(aiBtn);
        aiBtn.setPadding(Ui.dp(18), aiBtn.getPaddingTop(), Ui.dp(18), aiBtn.getPaddingBottom());
        aiBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { aiFill(); }
        });
        LinearLayout.MarginLayoutParams mp = new LinearLayout.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.leftMargin = Ui.dp(8);
        aiRow.addView(aiBtn, mp);

        wordCard.addView(aiRow);

        // ---- 编辑器 ----
        LinearLayout editorWrap = card();
        editorWrap.setPadding(0, 0, 0, 0);
        editorWrap.setClipToOutline(false);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        elp.topMargin = Ui.dp(9);
        addView(editorWrap, elp);

        editor = new WebView(getContext());
        WebSettings s = editor.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // assets 里的 editor.html 与 mathjax 都要靠 file:// 才能互相引用
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        editor.setBackgroundColor(0x00000000);
        editor.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                editorReady = true;
                pushTemplates();
                pushWord(wordInput.getText().toString());
                if (store.introShown()) {
                    // 首次引导里用户可能已经改了设置，这里刷新一下徽标
                    refreshAiBadge();
                }
            }
        });
        editorWrap.addView(editor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 千万不要忘了这一句：之前漏了 loadUrl，编辑器区域整块空白
        editor.loadUrl(EDITOR_URL);

        // ---- 牌组 / 标签 ----
        LinearLayout metaCard = card();
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = Ui.dp(9);
        addView(metaCard, mlp);

        metaCard.addView(smallLabel("牌组"));
        LinearLayout deckRow = new LinearLayout(getContext());
        deckRow.setOrientation(LinearLayout.HORIZONTAL);
        deckRow.setGravity(Gravity.CENTER_VERTICAL);
        deckInput = new EditText(getContext());
        deckInput.setSingleLine(true);
        deckInput.setTextSize(15);
        deckInput.setTextColor(Ui.INK);
        Ui.hint(deckInput, "NCUK::专业术语");
        deckRow.addView(deckInput, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pickBtn = new Button(getContext());
        pickBtn.setText("▾ 从 Anki 选择");
        Ui.secondary(pickBtn);
        pickBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDeck(); }
        });
        deckRow.addView(pickBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        metaCard.addView(deckRow);

        metaCard.addView(smallLabel("标签（空格分隔）"));
        tagInput = new EditText(getContext());
        tagInput.setSingleLine(true);
        tagInput.setTextSize(15);
        tagInput.setTextColor(Ui.INK);
        Ui.hint(tagInput, "自建 数学");
        metaCard.addView(tagInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- 保存 ----
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, Ui.dp(9), 0, 0);

        saveBtn = new Button(getContext());
        saveBtn.setText("保存到 Anki");
        Ui.primary(saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(false); }
        });
        actions.addView(saveBtn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        draftBtn = new Button(getContext());
        draftBtn.setText("存草稿");
        Ui.secondary(draftBtn);
        draftBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(true); }
        });
        LinearLayout.MarginLayoutParams dpLp = new LinearLayout.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpLp.leftMargin = Ui.dp(8);
        actions.addView(draftBtn, dpLp);
        addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusLine = new TextView(getContext());
        statusLine.setTextSize(12.5f);
        statusLine.setTextColor(Ui.TEXT_DIM);
        statusLine.setPadding(0, Ui.dp(7), 0, 0);
        addView(statusLine, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        deckInput.setText(store.defaultDeck());
        tagInput.setText(store.defaultTags());
        refreshAiBadge();
    }

    // ------------------------------------------------------------------ 状态显示

    private void status(String msg, int color) {
        statusLine.setTextColor(color);
        statusLine.setText(msg);
    }

    public void refreshAiBadge() {
        String id = store.aiProvider();
        String label = AiClient.presetLabel(id);
        boolean keyless = !AiClient.needsKey(id) && store.aiApiKey().trim().length() == 0;
        aiBadge.setText("AI：" + label + (keyless ? "（免密钥）" : "") + " · 点此更换");
    }

    /** 切回本页时刷新（设置可能改过） */
    public void onShown() {
        refreshAiBadge();
        if (deckInput.getText().length() == 0) deckInput.setText(store.defaultDeck());
    }

    // ------------------------------------------------------------------ WebView 桥接

    private void js(String code) {
        if (!editorReady) return;
        try { editor.evaluateJavascript(code, null); } catch (Throwable ignored) { }
    }

    private void pushTemplates() {
        try {
            JSONObject t = new JSONObject();
            t.put("front", CardFormat.CARD_FRONT);
            t.put("back", CardFormat.CARD_BACK);
            t.put("css", CardFormat.CARD_CSS);
            js("setTemplates(" + t.toString() + ")");
        } catch (Exception ignored) { }
    }

    private void pushWord(String word) {
        js("setWord(" + JSONObject.quote(word == null ? "" : word) + ")");
    }

    private void pushFields(JSONObject fields) {
        if (fields == null) return;
        js("setFields(" + fields.toString() + ")");
    }

    /** 读回编辑器里的背面字段（异步） */
    public void readFields(final FieldsCb cb) {
        if (!editorReady) {
            cb.onFields(null);
            return;
        }
        try {
            editor.evaluateJavascript(
                    "(function(){try{return getFields();}catch(e){return null;}})()",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            JSONObject o = null;
                            try {
                                if (value != null && !"null".equals(value)) o = new JSONObject(value);
                            } catch (Exception ignored) { }
                            cb.onFields(o);
                        }
                    });
        } catch (Throwable t) {
            cb.onFields(null);
        }
    }

    private void clearEditor() {
        wordInput.setText("");
        js("clearFields()");
    }

    private void clearAll() {
        clearEditor();
        status("已清空", Ui.TEXT_DIM);
    }

    // ------------------------------------------------------------------ AI 填充

    private void aiFill() {
        final String word = wordInput.getText().toString().trim();
        if (word.length() == 0) {
            status("先在上面输入单词，再点 AI 填充", Ui.RED);
            wordInput.requestFocus();
            return;
        }
        if (store.aiBaseUrlEffective().length() == 0) {
            status("还没选 AI 服务商，请到「设置」里配置", Ui.RED);
            return;
        }
        status("AI 正在生成卡片内容…", Ui.SUB);
        aiBtn.setEnabled(false);
        clearBtn.setEnabled(false);

        Th.bg(new Runnable() {
            @Override
            public void run() {
                try {
                    AiClient ai = new AiClient();
                    String text = ai.chat(store.aiBaseUrlEffective(), store.aiApiKey(),
                            store.aiModelEffective(), CardFormat.SYSTEM,
                            CardFormat.buildPrompt(word, store.subject()));
                    final JSONObject parsed = CardFormat.parseAi(text);
                    final JSONObject fields = CardFormat.noteFields(word,
                            parsed != null ? parsed : CardFormat.fallbackFields(word, text));
                    final boolean ok = parsed != null;
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            aiBtn.setEnabled(true);
                            clearBtn.setEnabled(true);
                            pushFields(fields);
                            status(ok ? "AI 填充完成，可以逐项修改后保存"
                                      : "AI 的输出不是 JSON，已原样放进「定义」，请手动整理",
                                    ok ? Ui.GREEN : Ui.AMBER);
                        }
                    });
                } catch (final AiClient.AiException e) {
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            aiBtn.setEnabled(true);
                            clearBtn.setEnabled(true);
                            status("AI 调用失败：" + e.getMessage(), Ui.RED);
                        }
                    });
                }
            }
        });
    }

    // ------------------------------------------------------------------ 保存

    private void save(final boolean asDraft) {
        final String word = wordInput.getText().toString().trim();
        if (word.length() == 0) {
            status("先输入单词（卡片正面）", Ui.RED);
            wordInput.requestFocus();
            return;
        }
        final String deck = deckInput.getText().toString().trim().length() == 0
                ? store.defaultDeck() : deckInput.getText().toString().trim();
        final String tags = tagInput.getText().toString().trim();

        status(asDraft ? "正在存草稿…" : "正在读取编辑器内容…", Ui.SUB);
        readFields(new FieldsCb() {
            @Override
            public void onFields(JSONObject back) {
                final JSONObject note = CardFormat.mergeNote(word, back);
                if (asDraft) {
                    store.addDraft(word, note, deck, tags);
                    status("已存入本地草稿箱（浏览 → 本地草稿 可随时补发）", Ui.GREEN);
                    clearEditor();
                    return;
                }
                saveBtn.setEnabled(false);
                status("正在保存到 Anki…", Ui.SUB);
                Th.bg(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AnkiClient anki = new AnkiClient(store.ankiHost(), store.ankiPort(),
                                    store.ankiApiKey());
                            long id = anki.saveNote(deck, note, parseTags(tags), store.autoSync());
                            Th.ui(new Runnable() {
                                @Override
                                public void run() {
                                    saveBtn.setEnabled(true);
                                    clearEditor();
                                    status(store.autoSync()
                                            ? "已保存并同步到 AnkiWeb 云端 ✓"
                                            : "已保存到本地 Anki（自动同步已关闭）✓", Ui.GREEN);
                                }
                            });
                        } catch (final AnkiClient.AnkiException e) {
                            // Anki 连不上：自动转草稿，卡片不会丢
                            store.addDraft(word, note, deck, tags);
                            Th.ui(new Runnable() {
                                @Override
                                public void run() {
                                    saveBtn.setEnabled(true);
                                    status("Anki 没连上，已自动转存草稿箱。原因：" + e.getMessage(),
                                            Ui.AMBER);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    public static JSONArray parseTags(String tags) {
        JSONArray arr = new JSONArray();
        if (tags == null) return arr;
        String[] parts = tags.split("[,，\\s]+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().length() > 0) arr.put(parts[i].trim());
        }
        return arr;
    }

    // ------------------------------------------------------------------ 牌组选择

    private void pickDeck() {
        status("正在从 Anki 读取牌组…", Ui.SUB);
        Th.bg(new Runnable() {
            @Override
            public void run() {
                try {
                    AnkiClient anki = new AnkiClient(store.ankiHost(), store.ankiPort(), store.ankiApiKey());
                    final JSONArray names = anki.deckNames();
                    final String[] items = new String[names.length()];
                    for (int i = 0; i < items.length; i++) items[i] = names.optString(i, "");
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            status("共 " + items.length + " 个牌组", Ui.TEXT_DIM);
                            if (items.length == 0) return;
                            AlertDialog d = new AlertDialog.Builder(act)
                                    .setTitle("选择牌组")
                                    .setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            deckInput.setText(items[which]);
                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .create();
                            d.show();
                        }
                    });
                } catch (final AnkiClient.AnkiException e) {
                    Th.ui(new Runnable() {
                        @Override
                        public void run() {
                            status("读取牌组失败（可以手动输入）：" + e.getMessage(), Ui.RED);
                        }
                    });
                }
            }
        });
    }
}
