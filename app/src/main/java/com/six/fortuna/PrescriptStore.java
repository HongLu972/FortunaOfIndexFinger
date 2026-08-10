package com.six.fortuna;

import android.content.Context;
import android.content.SharedPreferences;

import com.six.fortuna.IndexLevel.IndexFingerLevel_CN;
import com.six.fortuna.Prescripts.Prescripts_finished;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责把职阶数据（业/护/rank/level）和挂起中的指令卡片
 * （含绝对截止时间戳deadline）持久化到 SharedPreferences。
 *
 * 关键点：deadline是 System.currentTimeMillis() 基准的绝对时间戳，
 * 不依赖任何内存中的Timer/Handler，所以App被系统杀掉、手机重启，
 * 只要SharedPreferences没被清，倒计时进度都还在。
 */
public class PrescriptStore {

    private static final String PREFS_NAME = "fortuna_prefs";
    private static final String KEY_STATS = "stats";
    private static final String KEY_CARDS = "pending_cards";
    private static final String KEY_COOLDOWN_DEADLINE = "cooldown_deadline";

    // ---------------- 战斗 / 局外养成相关（新增） ----------------
    private static final String KEY_EYES = "eyes";                    // 局外货币"眼"，以枚为单位存，显示时自己除万
    private static final String KEY_BATTLE_DIFFICULTY = "battle_difficulty";
    private static final String KEY_OWNED_CARD_KEYS = "owned_card_keys"; // 出战牌组构成（会被带进战斗的卡）
    private static final String KEY_CARD_COLLECTION = "card_collection";  // 大牌库：所有获得过但还没装备进牌组的卡
    private static final String KEY_BATTLE_SNAPSHOT = "battle_snapshot";  // 战斗中途快照，null表示当前没有进行中的战斗
    private static final String KEY_BONUS_MAX_HP = "bonus_max_hp";       // 局外养成：永久加成的血量上限

    public void saveEyes(long eyes) {
        prefs.edit().putLong(KEY_EYES, eyes).apply();
    }

    public long loadEyes() {
        return prefs.getLong(KEY_EYES, 0L);
    }

    public void saveBattleDifficulty(int difficulty) {
        prefs.edit().putInt(KEY_BATTLE_DIFFICULTY, difficulty).apply();
    }

    public int loadBattleDifficulty() {
        return prefs.getInt(KEY_BATTLE_DIFFICULTY, 0);
    }

    public void saveBonusMaxHp(int bonus) {
        prefs.edit().putInt(KEY_BONUS_MAX_HP, bonus).apply();
    }

    public int loadBonusMaxHp() {
        return prefs.getInt(KEY_BONUS_MAX_HP, 0);
    }

    /** 局外持久卡组：出战前从这里构建牌堆，卡从大牌库"装备"进来才会出现在这 */
    public void saveOwnedCardKeys(List<String> cardKeys) {
        try {
            JSONArray arr = new JSONArray();
            for (String k : cardKeys) arr.put(k);
            prefs.edit().putString(KEY_OWNED_CARD_KEYS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 返回null表示从没存过（用于判断是不是第一次进入，要发初始牌组） */
    public List<String> loadOwnedCardKeys() {
        String raw = prefs.getString(KEY_OWNED_CARD_KEYS, null);
        if (raw == null) return null;
        List<String> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) result.add(arr.getString(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** 大牌库：所有获得过、但还没装备进出战牌组的卡（战斗掉落先进这里，不直接进牌组） */
    public void saveCardCollection(List<String> cardKeys) {
        try {
            JSONArray arr = new JSONArray();
            for (String k : cardKeys) arr.put(k);
            prefs.edit().putString(KEY_CARD_COLLECTION, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> loadCardCollection() {
        String raw = prefs.getString(KEY_CARD_COLLECTION, null);
        List<String> result = new ArrayList<>();
        if (raw == null) return result;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) result.add(arr.getString(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** 战斗中途快照，原始JSON字符串直接透传，格式由StatsActivity的BattleSnapshot类决定 */
    public void saveBattleSnapshot(String json) {
        prefs.edit().putString(KEY_BATTLE_SNAPSHOT, json).apply();
    }

    /** null表示当前没有进行中的战斗（战斗结束/从未开始过都会是null） */
    public String loadBattleSnapshot() {
        return prefs.getString(KEY_BATTLE_SNAPSHOT, null);
    }

    public void clearBattleSnapshot() {
        prefs.edit().remove(KEY_BATTLE_SNAPSHOT).apply();
    }

    private final SharedPreferences prefs;

    private static final String KEY_CODENAME = "codename";

    public void saveCodename(String name){
        prefs.edit().putString(KEY_CODENAME, name).apply();
    }

    /** 返回 null 表示还没起过代号（用于首次询问的判断依据） */
    public String loadCodename(){
        return prefs.getString(KEY_CODENAME, null);
    }

    /** "将我抹去，也将你抹去" 按钮用：清空全部存档 */
    public void clearAll(){
        prefs.edit().clear().apply();
    }

    public PrescriptStore(Context context){
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ---------------- 职阶数据 ----------------

    public void saveStats(IndexFingerLevel_CN lvl){
        try {
            JSONObject o = new JSONObject();
            o.put("grace", lvl.grace);
            o.put("krama", lvl.krama);
            o.put("rank", lvl.rank);
            o.put("level", lvl.level);
            prefs.edit().putString(KEY_STATS, o.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 把存储的数据写回传入的 IndexFingerLevel_CN 实例。
     * 如果之前没存过任何数据，实例保持默认值（全0）不变。
     */
    public void loadStats(IndexFingerLevel_CN lvl){
        String raw = prefs.getString(KEY_STATS, null);
        if(raw == null) return;
        try {
            JSONObject o = new JSONObject(raw);
            lvl.grace = o.optInt("grace", 0);
            lvl.krama = o.optInt("krama", 0);
            lvl.rank = o.optInt("rank", 0);
            lvl.level = o.optInt("level", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ---------------- 获取指令的冷却时间 ----------------

    public void saveCooldownDeadline(long deadline){
        prefs.edit().putLong(KEY_COOLDOWN_DEADLINE, deadline).apply();
    }

    /**
     * 返回0表示没有正在进行的冷却（或从未冷却过）。
     */
    public long loadCooldownDeadline(){
        return prefs.getLong(KEY_COOLDOWN_DEADLINE, 0L);
    }

    // ---------------- 挂起中的指令卡片 ----------------

    public void saveCards(List<Prescripts_finished> cards){
        try {
            JSONArray arr = new JSONArray();
            for (Prescripts_finished c : cards) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("text", c.prescripts);
                o.put("time_taken", c.time_taken);
                o.put("deadline", c.deadline);
                arr.put(o);
            }
            prefs.edit().putString(KEY_CARDS, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<Prescripts_finished> loadCards(){
        List<Prescripts_finished> result = new ArrayList<>();
        String raw = prefs.getString(KEY_CARDS, null);
        if(raw == null) return result;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Prescripts_finished c = new Prescripts_finished(
                        o.optInt("time_taken", 0),
                        o.optString("text", "")
                );
                c.id = o.optString("id", c.id);
                c.deadline = o.optLong("deadline", 0L);
                result.add(c);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 增量写入/更新单张卡片会比较麻烦（要读全量、改一条、再写全量），
     * 索性提供一个"整体重写"的方法，MainActivity每次卡片列表变化后直接调用它。
     */
    public void replaceAllCards(List<Prescripts_finished> cards){
        saveCards(cards);
    }
}