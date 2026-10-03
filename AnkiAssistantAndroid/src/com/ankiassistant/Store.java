package com.ankiassistant;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 设置与本地草稿。全部存 SharedPreferences（键值都是字符串，草稿是 JSON 数组字符串）。
 * 断网时保存的卡片先进草稿箱，联网后可一键补发。
 */
public class Store {

    private final SharedPreferences sp;

    public Store(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences("anki_assistant", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------ 设置

    public String ankiHost() { return sp.getString("ankiHost", ""); }
    public void setAnkiHost(String v) { put("ankiHost", v); }

    public int ankiPort() {
        try { return Integer.parseInt(sp.getString("ankiPort", "8765").trim()); }
        catch (Exception e) { return 8765; }
    }
    public void setAnkiPort(String v) { put("ankiPort", v); }

    public String ankiApiKey() { return sp.getString("ankiApiKey", ""); }
    public void setAnkiApiKey(String v) { put("ankiApiKey", v); }

    public String aiProvider() { return sp.getString("aiProvider", AiClient.P_ZHIPU); }
    public void setAiProvider(String v) { put("aiProvider", v); }

    public String aiApiKey() { return sp.getString("aiApiKey", ""); }
    public void setAiApiKey(String v) { put("aiApiKey", v); }

    public String aiModel() { return sp.getString("aiModel", ""); }
    public void setAiModel(String v) { put("aiModel", v); }

    public String aiBaseUrl() { return sp.getString("aiBaseUrl", ""); }
    public void setAiBaseUrl(String v) { put("aiBaseUrl", v); }

    /** AI 填充时的学科背景（决定释义风格） */
    public String subject() { return sp.getString("subject", "CIE A-Level / NCUK IFY 数学、物理术语"); }
    public void setSubject(String v) { put("subject", v); }

    public String defaultDeck() { return sp.getString("defaultDeck", "NCUK::专业术语"); }
    public void setDefaultDeck(String v) { put("defaultDeck", v); }

    public String defaultTags() { return sp.getString("defaultTags", "自建"); }
    public void setDefaultTags(String v) { put("defaultTags", v); }

    public boolean autoSync() { return sp.getBoolean("autoSync", true); }
    public void setAutoSync(boolean v) { sp.edit().putBoolean("autoSync", v).apply(); }

    /** 当前生效的 AI 接口地址：自定义档直接读 aiBaseUrl，预设档按 id 推导（允许覆盖） */
    public String aiBaseUrlEffective() {
        String custom = aiBaseUrl().trim();
        if (custom.length() > 0) return custom;
        return AiClient.presetBaseUrl(aiProvider());
    }

    public String aiModelEffective() {
        String m = aiModel().trim();
        if (m.length() > 0) return m;
        return AiClient.presetModel(aiProvider());
    }

    private void put(String k, String v) {
        sp.edit().putString(k, v == null ? "" : v).apply();
    }

    // ------------------------------------------------------------------ 草稿

    public JSONArray drafts() {
        try {
            return new JSONArray(sp.getString("drafts", "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public int draftCount() { return drafts().length(); }

    public void addDraft(String word, JSONObject fields, String deck, String tags) {
        JSONArray arr = drafts();
        try {
            JSONObject o = new JSONObject();
            o.put("word", word == null ? "" : word);
            o.put("fields", fields == null ? new JSONObject() : fields);
            o.put("deck", deck == null ? "" : deck);
            o.put("tags", tags == null ? "" : tags);
            o.put("time", System.currentTimeMillis());
            arr.put(o);
            put("drafts", arr.toString());
        } catch (JSONException ignored) { }
    }

    public void removeDraft(int index) {
        JSONArray arr = drafts();
        if (index < 0 || index >= arr.length()) return;
        JSONArray next = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            if (i != index) next.put(arr.opt(i));
        }
        put("drafts", next.toString());
    }

    public void clearDrafts() { put("drafts", "[]"); }
}
