package com.six.fortuna.combat.engine;

import android.widget.Toast;

import com.six.fortuna.StatsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Buff {
    public Mechanics m;
    public StatsActivity s;
    Entity self;

    public Buff(Mechanics mechanics, StatsActivity s, Entity self) {
        this.m = mechanics;
        this.s = s;
        this.self = self;
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
    public int Tiphereth;
    public int expireSwift;
    public int noSelf; //无我
    public int rainoftears;
    public ArrayList chargeBalls = new ArrayList();
    public int chargeBallSize = 3;
    private int chargeBallCount = 0;
    public void turnEndActivate(){
        m.defend(armored, self);
    }

    public void addChargeBalls(int ballType, Entity target){
        chargeBalls.add(0, ballType);
        if(ballType == 1) currentgeneration++;
        chargeBallCount++;
        if(chargeBallCount > chargeBallSize){
            onDestoryChargeBalls((int)chargeBalls.get(chargeBallCount-1), target);
            chargeBalls.remove(chargeBallSize - 1);
            chargeBallCount--;
        }
    }

    public void ballDefense(int amount, Entity self){
        amount += self.buff.concentration;
        if(amount >= 0){
            self.block += amount;
            s.appendLog("充能球对"+self.name+"施加了"+amount+"的护盾。");
        }
    }

    public boolean ballDamage(int amount, Entity self, Entity target){
        amount += self.buff.concentration;
        if(target.buff.sidestep > 0) {
            if (target.buff.sidestep >= amount) {
                target.buff.sidestep--;
                s.appendLog("充能球对"+target.name+"造成了"+amount+"点伤害但全被闪掉了，怎么，打不中么?");
                return false;
            } else {
                target.buff.sidestep = 0;
                amount *= 1.25;
            }
        }

        if(target.block > 0){
            if(target.block > amount){
                target.block -= amount;
                s.appendLog("充能球对"+target.name+"造成了"+amount+"点伤害但全被护盾格挡了，其剩余护盾为"+target.block);
            }else{
                amount -= target.block;
            }
        }
        target.hp -= amount;
        s.appendLog("充能球对"+target.name+"造成了"+amount+"点伤害，其剩余生命为"+target.hp);
        return true;
    }

    public void onDestoryChargeBalls(int ballType, Entity target){
        switch (ballType){
            case 1:
                for(int i = 0; i < 3; i++) activateChargeBalls(1, target);
                break;
            case 2:
                for(int i = 0; i < 4; i++) activateChargeBalls(2, target);
                target.swift -= 3;
                break;
            case 3:
                self.energy += 3;
                break;
        }
    }

    public void activateChargeBalls(int ballType, Entity target){
        switch (ballType){
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

    public String Tiphereth_words(){
        switch(m.randint(1, 13)){
            case 1:
                return "哪怕只有我一个人，我也能管理好每个收容单元！";
            case 2:
                return "这首歌叫做\"Tiphereth的挽歌\", 为Tiphereth而写的...";
            case 3:
                return "愿这首歌...能安抚我们的灵魂......";
            case 4:
                return "我们是两个人，也是一个人，你知道那是什么意思吧？";
            case 5:
                return "中央本部太大了，每个人都忙得不可开支。";
            case 6:
                return "又...又要换一个了么？是时候去仓库了...";
            case 7:
                return "Tiphereth有得到他想要的东西吗？没有...他从一开始到底在期待着什么...";
            case 8:
                return "不要用那样的眼神看着我！你们已经...已经...被我抛弃了！！！";
            case 9:
                return "真想再一次和你手牵着手，一起在海边散步...海浪在我们身后发出欢快的声响...";
            case 10:
                return "如果你能够听到这首歌的话...";
            case 11:
                return "等真正的Tiphereth回来后，我要向他展现出我最成熟的一面...我想让他知道...知道...我...我...我真的做到了...";
            case 12:
                return "你告诉过我...\"一切都会好起来的\"...";
            case 13:
                return "这一切...都是值得的吗...";
            default:
                return "这一切...都是值得的吗...";
        }
    }
    public void turnStartActivate(Entity target){
        self.swift -= expireSwift;
        expireSwift = 0;
        self.hp -= poison;
        poison /= 2;      //每回合结束对自身造成层数的伤害并减半
        armored--;
        if(SeedofLight > 0){
            SeedofLight++;
            if(Tiphereth >= 6){
                SeedofLight++;
            }
            self.max_energy = SeedofLight + (7 > SeedofLight ? SeedofLight : 7);
            self.gain_energy = SeedofLight - 1;
            if(self.stagger_panic_term > 0){
                self.stagger_panic_term--;
            }
        }
        self.energy += nextTurnLight;
        nextTurnLight = 0;
        if(Precongization > 0){
            sidestep += 1;
            Precongization--;
        }else {
            sidestep = 0;
        }
        if(Tiphereth >= 6){
            self.reload();
        }
        if(noSelf > 0){
            noSelf *= 1.15;
            for(int i = 0; i < noSelf; i++) self.max_hp *= 0.9993;
        }
        if(Tiphereth > 0 && Tiphereth < 6){
            Toast.makeText(s, Tiphereth_words(), Toast.LENGTH_LONG).show();
            Tiphereth++;
            if(m.randint(1, 3) == 1) {
                self.stagger_panic_term++;
                Tiphereth++;
            }
        }
        for(int i = 0; i < chargeBallCount; i++){
            activateChargeBalls((Integer) chargeBalls.get(i), target);
        }
        if(self.EntityId != 8) cibei = 0;
    }

    public void reset(){
        reset_Buff();
        reset_Debuff();
    }

    public void reset_Buff(){
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
    }

    public void reset_Debuff(){
        expireSwift = 0;
        poison = 0;
    }

    public String translation_chargeball(int chargeBallType){
        switch (chargeBallType){
            case 1:
                return "⚡闪电充能球";
            case 2:
                return "🧊冰霜充能球";
            case 3:
                return "💡离子充能球";
        }
        return "";
    }

    public ArrayList<String> getString(){
        ArrayList<String> output = new ArrayList<>();
        if(SeedofLight > 0) output.add("🕯️光之种"+SeedofLight+"  ");
        if(cibei > 0) output.add("🐛?!艾兰?! "+cibei+"  ");
        if(noSelf > 0) output.add("◼️无我"+noSelf+"  ");
        if(sidestep > 0) output.add("✨闪避"+sidestep+"  ");
        if(Unlock > 0) output.add("🌸解放"+Unlock+"  ");
        if(armored > 0) output.add("🛡️覆甲"+armored+"  ");
        if(poison > 0) output.add("🍄剧毒"+poison+"  ");
        if(nextTurnLight > 0) output.add("☀️下回合光芒"+nextTurnLight+"  ");
        if(Precongization > 0) output.add("👁️‍🗨️预知"+ Precongization +"  ");
        if(expireSwift > 0) output.add("🐢即将到期的敏捷"+expireSwift+"  ");
        if(Tiphereth > 0){
            int remain = 6 - Tiphereth;
            if(Tiphereth < 6) output.add("Tiphereth核心抑制中"+remain+"  ");
            else output.add("Tiphereth核心抑制完毕  ");
        }
        if(concentration > 0) output.add("💠集中"+concentration+"  ");
        String chargeBall = "";
        for(int i = 0; i < chargeBallCount; i++){
            chargeBall += translation_chargeball((Integer) chargeBalls.get(i));
        }
        output.add("充能球["+chargeBall+"]  ");
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
            o.put("nextturnlight", nextTurnLight);
            o.put("armored", armored);
            o.put("unlock", Unlock);
            o.put("seedOfLight", SeedofLight);
            o.put("?!追追?!", cibei);
            o.put("recongization", Precongization);
            o.put("tiphereth", Tiphereth);
            o.put("chargeballsize", chargeBallSize);
            o.put("chargeballcount", chargeBallCount);
            for(int i = 1; i <= chargeBallCount; i++){
                o.put("chargeball"+i, chargeBalls.get(i-1));
            }
            o.put("concentration", concentration);
            o.put("currentgeneration", currentgeneration);
            o.put("rainoftears", rainoftears);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }

    /**
     * 从 JSONObject 恢复 Buff 的数据状态（需要传入 Mechanics 和宿主 Entity）
     */
    public static Buff fromJson(JSONObject o, Mechanics m, StatsActivity s, Entity self) {
        Buff b = new Buff(m, s, self);
        //负面效果
        b.poison = o.optInt("poison", 0);
        b.expireSwift = o.optInt("expireswift", 0);
        b.noSelf = o.optInt("noself", 0);
        //正面效果
        b.nextTurnLight = o.optInt("nextturnlight", 0);
        b.armored = o.optInt("armored", 0);
        b.Unlock = o.optInt("unlock", 0);
        b.SeedofLight = o.optInt("seedOfLight", 0);
        b.cibei = o.optInt("?!追追?!", 0);
        b.Precongization = o.optInt("recongization", 0);
        b.Tiphereth = o.optInt("tiphereth", 0);
        b.chargeBallSize = o.optInt("chargeballsize", 3);
        b.chargeBallCount = o.optInt("chargeballcount", 0);
        for(int i = 1; i <= b.chargeBallCount; i++){
            b.chargeBalls.add(o.optInt("chargeball"+i, 0));
        }
        b.concentration = o.optInt("concentration", 0);
        b.currentgeneration = o.optInt("currentgeneration", 0);
        b.rainoftears = o.optInt("rainoftears", 0);
        return b;
    }
}
