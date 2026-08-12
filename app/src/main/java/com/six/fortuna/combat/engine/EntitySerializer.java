package com.six.fortuna.combat.engine;

import com.six.fortuna.StatsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Entity <-> JSON 互转，只挑战斗快照需要的字段存，省得战斗中断后状态全丢。
 * current_intent不存（它包含Java方法引用，序列化没意义），恢复时靠调用方重新计算意图。
 */
public class EntitySerializer {
    public static JSONObject toJson(Entity e) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", e.name == null ? "" : e.name);
            o.put("hp", e.hp);
            o.put("max_hp", e.max_hp);
            o.put("outside_max_hp", e.outside_max_hp);
            o.put("block", e.block);
            o.put("energy", e.energy);
            o.put("max_energy", e.max_energy);
            o.put("gain_energy", e.gain_energy);
            o.put("strength", e.strength);
            o.put("swift", e.swift);
            o.put("burn_term", e.burn_term);
            o.put("burn_strength", e.burn_strength);
            o.put("rapture_strength", e.rapture_strength);
            o.put("rapture_term", e.rapture_term);
            o.put("bleed_strength", e.bleed_strength);
            o.put("bleed_term", e.bleed_term);
            o.put("sinking_term", e.sinking_term);
            o.put("sinking_strength", e.sinking_strength);
            o.put("tremor_term", e.tremor_term);
            o.put("tremor_strength", e.tremor_strength);
            o.put("tremor_conversion", e.tremor_conversion);
            o.put("poise_term", e.poise_term);
            o.put("poise_strength", e.poise_strength);
            o.put("charge_term", e.charge_term);
            o.put("charge_strength", e.charge_strength);
            o.put("charge_consume", e.charge_consume);
            o.put("sanity", e.sanity);
            o.put("this_turn_strength", e.this_turn_strength);
            o.put("blade", e.blade);
            o.put("panic", e.panic);
            o.put("stagger_panic_term", e.stagger_panic_term);
            o.put("stagger_count", e.stagger_count);
            o.put("health", e.health);
            o.put("restrictions", e.restrictions);
            o.put("grace", e.grace);
            o.put("krama", e.krama);
            o.put("difficulty", e.difficulty);
            o.put("tireness", e.tireness);
            o.put("countA", e.countA);
            o.put("countB", e.countB);
            o.put("countC", e.countC);
            o.put("shin", e.shin);
            o.put("enemyId", e.EntityId);
            o.put("buff", e.buff.toJson());
            o.put("ammo", e.ammo);
            o.put("ammotype", e.ammoType);
            o.put("totalammo", e.totalAmmo);
            // staggerLine 数组
            JSONArray sl = new JSONArray();
            for (double d : e.staggerLine) sl.put(d);
            o.put("staggerLine", sl);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return o;
    }

    public static Entity fromJson(JSONObject o, Mechanics m, StatsActivity s) {
        Entity e = new Entity();
        e.name = o.optString("name", "");
        e.hp = o.optInt("hp", 0);
        e.max_hp = o.optInt("max_hp", 0);
        e.outside_max_hp = o.optInt("outside_max_hp", 0);
        e.block = o.optInt("block", 0);
        e.energy = o.optInt("energy", 0);
        e.max_energy = o.optInt("max_energy", 0);
        e.gain_energy = o.optInt("gain_energy", 0);
        e.strength = o.optInt("strength", 0);
        e.swift = o.optInt("swift", 0);
        e.burn_term = o.optInt("burn_term", 0);
        e.burn_strength = o.optInt("burn_strength", 0);
        e.rapture_strength = o.optInt("rapture_strength", 0);
        e.rapture_term = o.optInt("rapture_term", 0);
        e.bleed_strength = o.optInt("bleed_strength", 0);
        e.bleed_term = o.optInt("bleed_term", 0);
        e.sinking_term = o.optInt("sinking_term", 0);
        e.sinking_strength = o.optInt("sinking_strength", 0);
        e.tremor_term = o.optInt("tremor_term", 0);
        e.tremor_strength = o.optInt("tremor_strength", 0);
        e.tremor_conversion = o.optInt("tremor_conversion", 0);
        e.poise_term = o.optInt("poise_term", 0);
        e.poise_strength = o.optInt("poise_strength", 0);
        e.charge_term = o.optInt("charge_term", 0);
        e.charge_strength = o.optInt("charge_strength", 1);
        e.charge_consume = o.optInt("charge_consume", 0);
        e.sanity = o.optInt("sanity", 0);
        e.this_turn_strength = o.optInt("this_turn_strength", 0);
        e.blade = o.optInt("blade", 0);
        e.panic = o.optInt("panic", 0);
        e.stagger_panic_term = o.optInt("stagger_panic_term", 0);
        e.stagger_count = o.optInt("stagger_count", 0);
        e.health = o.optInt("health", 0);
        e.restrictions = o.optInt("restrictions", 0);
        e.grace = o.optInt("grace", 0);
        e.krama = o.optInt("krama", 0);
        e.difficulty = o.optInt("difficulty", 0);
        e.tireness = o.optInt("tireness", 1);
        e.countA = o.optInt("countA", 0);
        e.countB = o.optInt("countB", 0);
        e.countC = o.optInt("countC", 0);
        e.shin = o.optInt("shin", 0);
        e.EntityId = o.optInt("enemyId", 0);
        e.ammo = o.optInt("ammo", 0);
        e.ammoType = o.optInt("ammotype", 0);
        e.totalAmmo = o.optInt("totalammo", 6);
        JSONObject buffJson = o.optJSONObject("buff");
        if (buffJson != null) {
            e.buff = Buff.fromJson(buffJson, m, s, e);   // 用静态工厂重建 buff
        }
        JSONArray sl = o.optJSONArray("staggerLine");
        if (sl != null) {
            double[] arr = new double[sl.length()];
            for (int i = 0; i < sl.length(); i++) arr[i] = sl.optDouble(i, 0);
            e.staggerLine = arr;
        }
        return e;
    }
}