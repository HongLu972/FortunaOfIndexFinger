package com.six.fortuna.combat.engine;

import android.content.res.Resources;
import android.widget.Toast;

import com.six.fortuna.R;
import com.six.fortuna.StatsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Buff {
    public Mechanics m;
    public StatsActivity s;
    public Resources res;
    Entity self;

    public Buff(Mechanics mechanics, StatsActivity s, Entity self, Resources res) {
        this.m = mechanics;
        this.s = s;
        this.self = self;
        this.res = res;
        chargeBallSize = 3;
        chargeBallCount = 0;
    }

    //这里定义了所有效果的参数
    public int poison; //剧毒
    public int Precongization;
    public int cibei;
    public int armored;
    public int SeedofLight;
    public int currentgeneration;
    public int concentration;
    public int Unlock;
    public int sidestep; //闪避
    public int nextTurnLight; //下回合能量
    public int lockedHealth;
    public int Tiphereth;
    public int expireSwift;
    public int noSelf; //无我
    public int Apocalypse_Bird;
    public int rainoftears;
    public ArrayList chargeBalls = new ArrayList();
    public int chargeBallSize = 3;
    private int chargeBallCount = 0;
    public int UnlockedHealth;
    public int bloodstainedTears;
    public int littlebird;
    public int tallbird;
    public int bigbird;

    public void turnEndActivate() {
        m.defend(armored, self);
    }

    public void addChargeBalls(int ballType, Entity target) {
        chargeBalls.add(0, ballType);
        if (ballType == 1) currentgeneration++;
        chargeBallCount++;
        if (chargeBallCount > chargeBallSize) {
            onDestoryChargeBalls((int) chargeBalls.get(chargeBallCount - 1), target);
            chargeBalls.remove(chargeBallSize - 1);
            chargeBallCount--;
        }
    }

    public void ballDefense(int amount, Entity self) {
        amount += self.buff.concentration;
        if (amount >= 0) {
            self.block += amount;
            s.appendLog(String.format(res.getString(R.string.buff_log_shield), self.name, amount));
        }
    }

    public boolean ballDamage(int amount, Entity self, Entity target) {
        amount += self.buff.concentration;
        if (target.buff.sidestep > 0) {
            if (target.buff.sidestep >= amount) {
                target.buff.sidestep--;
                s.appendLog(String.format(res.getString(R.string.buff_log_dodge_all), amount, target.name));
                return false;
            } else {
                target.buff.sidestep = 0;
                amount *= 1.25;
            }
        }

        if (target.block > 0) {
            if (target.block > amount) {
                target.block -= amount;
                s.appendLog(String.format(res.getString(R.string.buff_log_shield_block), amount, target.name, target.block));
            } else {
                amount -= target.block;
            }
        }
        target.hp -= amount;
        s.appendLog(String.format(res.getString(R.string.buff_log_damage), amount, target.name, target.hp));
        return true;
    }

    public void onDestoryChargeBalls(int ballType, Entity target) {
        switch (ballType) {
            case 1:
                for (int i = 0; i < 3; i++) activateChargeBalls(1, target);
                break;
            case 2:
                for (int i = 0; i < 4; i++) activateChargeBalls(2, target);
                target.swift -= 3;
                break;
            case 3:
                self.energy += 3;
                break;
        }
    }

    public void activateChargeBalls(int ballType, Entity target) {
        switch (ballType) {
            case 1://闪电充能球
                ballDamage(6, self, target);
                break;
            case 2://冰霜充能球
                ballDefense(8, self);
                break;
            case 3://离子充能球
                self.energy++;
                break;
        }
    }

    public String Tiphereth_words() {
        switch (m.randint(1, 13)) {
            case 1:
                return res.getString(R.string.tiphereth_1);
            case 2:
                return res.getString(R.string.tiphereth_2);
            case 3:
                return res.getString(R.string.tiphereth_3);
            case 4:
                return res.getString(R.string.tiphereth_4);
            case 5:
                return res.getString(R.string.tiphereth_5);
            case 6:
                return res.getString(R.string.tiphereth_6);
            case 7:
                return res.getString(R.string.tiphereth_7);
            case 8:
                return res.getString(R.string.tiphereth_8);
            case 9:
                return res.getString(R.string.tiphereth_9);
            case 10:
                return res.getString(R.string.tiphereth_10);
            case 11:
                return res.getString(R.string.tiphereth_11);
            case 12:
                return res.getString(R.string.tiphereth_12);
            case 13:
                return res.getString(R.string.tiphereth_13);
            default:
                return res.getString(R.string.tiphereth_13);
        }
    }

    public void turnStartActivate(Entity target) {
        if (bloodstainedTears > 0) {
            if (bloodstainedTears < 5) {
                bloodstainedTears++;
            }
            self.hp -= self.max_hp * 0.05 * Math.min(bloodstainedTears, 4);
        }
        self.swift -= expireSwift;
        expireSwift = 0;
        self.hp -= poison;
        poison /= 2;      //每回合结束对自身造成层数的伤害并减半
        armored--;
        if (SeedofLight > 0) {
            SeedofLight++;
            if (Tiphereth >= 6) {
                SeedofLight++;
            }
            self.max_energy = SeedofLight + (7 > SeedofLight ? SeedofLight : 7);
            self.gain_energy = SeedofLight - 1;
            if (self.stagger_panic_term > 0) {
                self.stagger_panic_term--;
            }
        }
        self.energy += nextTurnLight;
        nextTurnLight = 0;
        if (Precongization > 0) {
            sidestep += 1;
            Precongization--;
        } else {
            sidestep = 0;
        }
        if (Tiphereth >= 6) {
            self.reload();
        }
        if (noSelf > 0) {
            noSelf *= 1.15;
            self.max_hp *= Math.pow(0.9993, noSelf);
        }
        if (Tiphereth > 0 && Tiphereth < 6) {
            Toast.makeText(s, Tiphereth_words(), Toast.LENGTH_LONG).show();
            Tiphereth++;
            if (m.randint(1, 3) == 1) {
                self.stagger_panic_term++;
                Tiphereth++;
            }
        }
        for (int i = 0; i < chargeBallCount; i++) {
            activateChargeBalls((Integer) chargeBalls.get(i), target);
        }
        if (self.EntityId != 8) cibei = 0;
        if (Apocalypse_Bird >= 1) {
            self.energy = self.max_energy;
            self.max_hp *= 1.2;
            self.strength *= 1.05;
            self.strength += 2;
        }
    }

    public void reset() {
        reset_Buff();
        reset_Debuff();
    }

    public void reset_Buff() {
        SeedofLight = 0;
        sidestep = 0;
        cibei = 0;
        nextTurnLight = 0;
        Unlock = 0;
        armored = 0;
        Tiphereth = 0;
        chargeBalls.clear();
        chargeBallCount = chargeBalls.size();
        chargeBallSize = 3;
        rainoftears = 0;
        bigbird = 0;
        tallbird = 0;
        littlebird = 0;
        bloodstainedTears = 0;
        Apocalypse_Bird = 0;
    }

    public void reset_Debuff() {
        expireSwift = 0;
        poison = 0;
        noSelf = 0;
    }

    public String translation_chargeball(int chargeBallType) {
        switch (chargeBallType) {
            case 1:
                return res.getString(R.string.chargeball_lightning);
            case 2:
                return res.getString(R.string.chargeball_frost);
            case 3:
                return res.getString(R.string.chargeball_ion);
        }
        return "";
    }

    public ArrayList<String> getString() {
        ArrayList<String> output = new ArrayList<>();
        if (SeedofLight > 0) output.add(String.format(res.getString(R.string.buff_seed_of_light), SeedofLight));
        if (cibei > 0) output.add(String.format(res.getString(R.string.buff_cibei), cibei));
        if (noSelf > 0) output.add(String.format(res.getString(R.string.buff_no_self), noSelf));
        if (bloodstainedTears > 0) output.add(String.format(res.getString(R.string.buff_bloodstained_tears), bloodstainedTears));
        if (sidestep > 0) output.add(String.format(res.getString(R.string.buff_sidestep), sidestep));
        if (Unlock > 0) output.add(String.format(res.getString(R.string.buff_unlock), Unlock));
        if (armored > 0) output.add(String.format(res.getString(R.string.buff_armored), armored));
        if (poison > 0) output.add(String.format(res.getString(R.string.buff_poison), poison));
        if (nextTurnLight > 0) output.add(String.format(res.getString(R.string.buff_next_turn_light), nextTurnLight));
        if (Precongization > 0) output.add(String.format(res.getString(R.string.buff_precognition), Precongization));
        if (expireSwift > 0) output.add(String.format(res.getString(R.string.buff_expire_swift), expireSwift));
        if (Tiphereth > 0) {
            int remain = 6 - Tiphereth;
            if (Tiphereth < 6) output.add(String.format(res.getString(R.string.buff_tiphereth_in_progress), remain));
            else output.add(res.getString(R.string.buff_tiphereth_done));
        }
        if (concentration > 0) output.add(String.format(res.getString(R.string.buff_concentration), concentration));
        String chargeBall = "";
        for (int i = 0; i < chargeBallCount; i++) {
            chargeBall += translation_chargeball((Integer) chargeBalls.get(i));
        }
        output.add(String.format(res.getString(R.string.buff_charge_balls), chargeBall));
        return output;
    }

    /**
     * 把 Buff 的数据状态导出为 JSONObject
     */
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            //负面效果
            o.put("poison", poison);
            o.put("expireswift", expireSwift);
            o.put("noself", noSelf);
            //正面效果
            o.put("littlebird", littlebird);
            o.put("tallbird", tallbird);
            o.put("bigbird", bigbird);
            o.put("bigmonster", Apocalypse_Bird);
            o.put("unlockedhealth", UnlockedHealth);
            o.put("lockedhealth", lockedHealth);
            o.put("nextturnlight", nextTurnLight);
            o.put("armored", armored);
            o.put("unlock", Unlock);
            o.put("seedOfLight", SeedofLight);
            o.put("?!追追?!", cibei);
            o.put("recongization", Precongization);
            o.put("tiphereth", Tiphereth);
            o.put("chargeballsize", chargeBallSize);
            o.put("chargeballcount", chargeBallCount);
            for (int i = 1; i <= chargeBallCount; i++) {
                o.put("chargeball" + i, chargeBalls.get(i - 1));
            }
            o.put("concentration", concentration);
            o.put("currentgeneration", currentgeneration);
            o.put("rainoftears", rainoftears);
            o.put("bloodstainedTears", bloodstainedTears);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }

    /**
     * 从 JSONObject 恢复 Buff 的数据状态（需要传入 Mechanics 和宿主 Entity）
     */
    public static Buff fromJson(JSONObject o, Mechanics m, StatsActivity s, Entity self) {
        Buff b = new Buff(m, s, self, s.getResources());
        //负面效果
        b.poison = o.optInt("poison", 0);
        b.expireSwift = o.optInt("expireswift", 0);
        b.noSelf = o.optInt("noself", 0);
        b.UnlockedHealth = o.optInt("unlockedhealth", 0);
        //正面效果
        b.littlebird = o.optInt("littlebird", 0);
        b.tallbird = o.optInt("tallbird", 0);
        b.bigbird = o.optInt("bigbird", 0);
        b.Apocalypse_Bird = o.optInt("bigmonster", 0);
        b.lockedHealth = o.optInt("lockedhealth", 0);
        b.nextTurnLight = o.optInt("nextturnlight", 0);
        b.armored = o.optInt("armored", 0);
        b.Unlock = o.optInt("unlock", 0);
        b.SeedofLight = o.optInt("seedOfLight", 0);
        b.cibei = o.optInt("?!追追?!", 0);
        b.Precongization = o.optInt("recongization", 0);
        b.Tiphereth = o.optInt("tiphereth", 0);
        b.chargeBallSize = o.optInt("chargeballsize", 3);
        b.chargeBallCount = o.optInt("chargeballcount", 0);
        for (int i = 1; i <= b.chargeBallCount; i++) {
            b.chargeBalls.add(o.optInt("chargeball" + i, 0));
        }
        b.concentration = o.optInt("concentration", 0);
        b.currentgeneration = o.optInt("currentgeneration", 0);
        b.rainoftears = o.optInt("rainoftears", 0);
        b.bloodstainedTears = o.optInt("bloodstainedTears", 0);
        return b;
    }
}