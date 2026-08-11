package com.six.fortuna.combat.engine;

import android.widget.Toast;

import com.six.fortuna.StatsActivity;

/**
 * 原样搬运 enemies.c 里 panicCheck / restriction / brain_hajimi 这一整条链路（哈基米敌人AI）。
 * 其他敌人（Albinah / YunanRu / Rien / Calisto / Sora / GuGuGaGa / slimeElite / thieve / min）
 * enemies.c里都有，但这条消息篇幅有限先只搬一个跑通，你要哪个我接着搬哪个，逻辑保证按你C版来。
 */
public class FortunaEnemies {

    private final Mechanics m;
    private final StatsActivity s;

    public FortunaEnemies(Mechanics mechanics, StatsActivity s) {
        this.m = mechanics;
        this.s = s;
    }

    private static final int LOW_MORALE = 1;
    private static final int PANIC = 2;

    public boolean staggerCheck(Entity self){
        if (self.stagger_panic_term > 0){
            return true;
        }
        return false;
    }

    public void chargeCycle(Entity self, int level){
        if(self.charge_consume >= level){
            self.charge_strength += self.charge_strength / level;
            self.charge_consume -= (self.charge_consume / level)*level;
        }
    }

    // --- panicCheck(self) ---
    public int panicCheck(Entity self) {
        if (self.panic == 2) {
            self.sanity = 0;
            return 2;
        } else if (self.panic == 1) {
            if (m.sanityCheck(self) == 2) {
                self.sanity = 0;
                return 2;
            }
            return 1;
        }
        int check = m.sanityCheck(self);
        if (check == 2) {
            self.sanity = 0;
            return 2;
        } else if (check == 1) {
            return 1;
        } else {
            return 0;
        }
    }

    // --- restriction(self, player)：制约机制，敌人变强、玩家血线被压低 ---
    public void restriction(Entity self, Entity player) {
        self.strength += (int) (0.7 * player.restrictions);
        self.buff.concentration += (int)(0.4 * player.restrictions);
        self.max_hp = (int) (self.max_hp * (1 + player.restrictions * 0.03));
        self.hp = self.max_hp;
        if (player.hp >= player.outside_max_hp * (1 - 0.01 * player.restrictions) && player.restrictions <= 50) {
            player.hp = (int) ((1 - 0.01 * player.restrictions) * player.max_hp);
            if (player.hp < 0.5 * player.max_hp){
                player.hp = (int) (0.5 * player.max_hp);
            }
        } else if (player.restrictions >= 50) {
            if (player.hp > player.outside_max_hp * 0.5) {
                player.hp = (int) (player.outside_max_hp * 0.5);
            }
        }
        self.health = self.max_hp;
        while (true) {
            m.sanityCheck(player);
            if (m.ifStaggered(player) == 1) continue;
            break;
        }
        player.swift -= (int) (player.restrictions * 0.5);
        player.stagger_panic_term = 0;
    }

    // ===== 哈基米 (Hajimi) 招式 =====

    // action_ha
    public void actionHa(Entity self, Entity player) {
        self.countA++;
        self.strength += 2;
        m.dealDamage(5, player, self);
        player.bleed_term += 5;
    }

    // action_zhamao
    public void actionZhamao(Entity self, Entity player) {
        self.countA++;
        self.strength++;
        self.swift++;
        self.block += self.swift + 3;
    }

    // action_enhancedbaseattack
    public void actionEnhancedBaseAttack(Entity self, Entity player) {
        self.countA = 0;
        m.dealDamage((int) (self.strength * 1.5) + 10, player, self);
        player.bleed_strength += self.strength;
        self.poise_strength += self.strength;
        self.poise_term += self.strength * 2;
        self.strength = (int) (self.strength * 0.75);
    }

    // brain_hajimi(self, player)
    public EnemyAction brainHajimi(Entity self, Entity player) {
        EnemyAction haqi = new EnemyAction("这只猫看起来很生气……", this::actionHa);
        EnemyAction zhamao = new EnemyAction("猫毛炸起来了，像针一样……", this::actionZhamao);
        EnemyAction enhanced = new EnemyAction("猫的爪子开始蠢蠢欲动", this::actionEnhancedBaseAttack);
        EnemyAction nullAction = new EnemyAction("混乱中，不会行动", null);

        self.panic = panicCheck(self);
        if (self.panic == PANIC) {
            self.strength += 4;
            self.stagger_panic_term++;
            self.panic = 0;
            return nullAction;
        } else if (self.panic == LOW_MORALE) {
            self.strength += 2;
            player.this_turn_strength += 4;
        }
        self.panic = 0;

        if(staggerCheck(self)){
            return nullAction;
        }

        if (self.countC != 1) {
            restriction(self, player);
            self.countC = 1;
        }

        int i = m.randint(1, 100);
        if (i <= self.countA * 10) {
            return enhanced;
        }
        if (i <= 60) {
            return haqi;
        } else {
            return zhamao;
        }
    }

    public void normalhit(Entity self, Entity player){
        if(!(m.dealDamage(4, player, self))){
            self.hp -= self.max_hp * 0.34;
        }else{
            self.hp += self.max_hp * 0.1;
        }
        self.charge_term++;
    }

    public void strongmove(Entity self, Entity player){
        self.strength += 12;
        player.strength += 4;
    }

    public EnemyAction hearts(Entity self, Entity player){
        EnemyAction strongmove = new EnemyAction("剧↑烈↓搏↑动↓", this::strongmove);
        EnemyAction normalHit = new EnemyAction("被本攻击打中有奇效~", this::normalhit);
        EnemyAction stagger = new EnemyAction("混乱了呢☺️❀", null);
        if(self.countC != 1){
            self.countC = 1;
            self.charge_strength = 2;
            restriction(self, player);
            self.charge_term = 1;
        }
        if(self.charge_term >= self.charge_strength){
            self.charge_term = 0;
            return strongmove;
        }else{
            return normalHit;
        }
    }

    public void storm(Entity self, Entity player){
        self.charge_term += 10;
        chargeCycle(self, 10);
        self.swift += self.charge_strength;
        self.buff.concentration += self.charge_strength;
        self.buff.chargeBallSize++;
    }

    public void thundering(Entity self, Entity player){
        self.charge_consume += self.charge_term;
        self.charge_term = 1;
        chargeCycle(self, 10);
        for(int i = 0; i < self.buff.chargeBallSize; i++) self.buff.addChargeBalls(1, player);
        self.buff.currentgeneration *= 1.5;
        int k = 0;
        for(int i = 0; i < self.buff.chargeBallSize; i++) if(self.buff.chargeBalls.get(i).equals(1)) k++;
        if(m.dealDamage((self.buff.currentgeneration + self.buff.concentration) * k, player, self)){
            player.max_energy -= 2;
        }
    }

    public void flashing(Entity self, Entity player){
        int k = 0;
        for(int i = 0; i < self.buff.chargeBallSize; i++) if(self.buff.chargeBalls.get(i).equals(1)) k++;
        player.strength -= self.charge_strength;
        player.strength -= k;
    }

    public void sound(Entity self, Entity player){
        int k = 0;
        for(int i = 0; i < self.buff.chargeBallSize; i++) if(self.buff.chargeBalls.get(i).equals(1)) k++;
        player.swift -= self.charge_strength;
        player.swift -= k;
    }

    public EnemyAction brainThunderSpirit(Entity self, Entity player){
        EnemyAction storm = new EnemyAction("墨云卷地碾青山", this::storm);
        EnemyAction thundering = new EnemyAction("雷动于九天之上", this::thundering);
        EnemyAction flashing = new EnemyAction("雷车动地电火明", this::thundering);
        EnemyAction sound = new EnemyAction("雷声催雨千峰暗", this::sound);
        EnemyAction staggerAction = new EnemyAction("纷纷云散心安在，隐隐雷收气未平", null);

        self.block += (self.buff.currentgeneration > 50) ? 100 : self.buff.currentgeneration * 2;
        self.buff.concentration += self.buff.currentgeneration / 10;
        self.charge_term += (self.buff.currentgeneration > 30) ? 30 : self.buff.currentgeneration;
        if(staggerCheck(self)){
            return staggerAction;
        }
        if(self.countC != 1){
            self.charge_term = 5;
            self.charge_strength = 1;
            self.swift += 3;
            self.countC = 1;
            self.countB = 0;
        }
        self.countB++;
        switch(self.countB){
            case 1:
                return storm;
            case 2:
                return thundering;
            case 3:
                return flashing;
            case 4:
                self.countB = 0;
                return sound;
        }
        return staggerAction;
    }

    public void grasp(Entity self, Entity player){
        m.defend(30, self);
        self.charge_term += 2;
    }

    public void enhancedAtk(Entity self, Entity player){
        for (int i = 0; i < self.charge_term; i++){
            m.dealDamage(40, player, self);
        }
        self.charge_term = 0;
    }

    public EnemyAction barinForgottenKiller(Entity self, Entity player){
        //被遗弃的丢人魔(划掉)被遗弃的杀人魔
        EnemyAction grasp = new EnemyAction("俺...俺喘不上气了...", this::grasp);
        EnemyAction enhancedAttack = new EnemyAction("放，放俺出去！", this::enhancedAtk);
        if (self.sinking_term > 0 || self.rapture_term > 0 || self.bleed_term > 0 || self.tremor_term > 0 || self.burn_term > 0 || self.this_turn_strength < 0){
            self.this_turn_strength -= 32;
            self.swift -= 28;
            self.buff.expireSwift -= 28;
        }else{
            self.this_turn_strength += 32;
            self.swift += 30;
            self.buff.expireSwift += 30;
        }

        if (self.countC != 1){
            restriction(self, player);
            self.charge_term = 1;
            self.charge_strength = 3;
            self.countC = 1;
        }

        if(self.charge_strength != 3){
            self.charge_strength = 3;
        }

        if(self.charge_term >= self.charge_strength){
            return enhancedAttack;
        }else{
            return grasp;
        }
    }

    public void DepressedFlesh(Entity self, Entity player){
        player.bleed_strength += 3;
        player.bleed_term += 4;
        if(self.charge_term >= 20){
            self.charge_consume += 20;
            self.charge_term -= 20;
        }else if(self.charge_term > 0){
            self.charge_consume += self.charge_term;
            self.charge_term = 0;
        }
        while(self.charge_consume >= 10){
            self.charge_consume -= 10;
            self.charge_strength++;
        }
        m.dealDamage((int) ((20 + self.strength) * (1 + 0.1 * self.charge_strength)), player, self);
    }

    public void DepressedFleshPremium(Entity self, Entity player){
        player.bleed_strength += 4;
        player.bleed_term += 5;
        if(self.charge_term >= 40){
            self.charge_consume += 40;
            self.charge_term -= 40;
        }else if(self.charge_term > 0){
            self.charge_consume += self.charge_term;
            self.charge_term = 0;
        }
        while(self.charge_consume >= 10){
            self.charge_consume -= 10;
            self.charge_strength++;
        }
        m.dealDamage((int)((30 + self.strength) * (1 + 0.2 * self.charge_strength) - self.strength), player, self);
    }

    public void FasciaHungers(Entity self, Entity player){
        player.bleed_strength += 3;
        player.bleed_term += 1;
        if(self.charge_term >= 8){
            self.charge_consume += 8;
            self.charge_term -= 8;
            self.this_turn_strength += 2;
            player.hp -= player.max_hp * 0.001 * self.charge_strength;
        }else if(self.charge_term > 0){
            self.charge_consume += self.charge_term;
            self.charge_term = 0;
        }
        while(self.charge_consume >= 10){
            self.charge_consume -= 10;
            self.charge_strength++;
        }
        int a = player.hp;
        m.dealDamage(3, player, self);
        if(a > player.hp){
            self.charge_term += 4;
        }
    }

    public void TimeForYourMeal(Entity self, Entity player){
        player.bleed_strength += 1;
        player.bleed_term += 6;
        if(self.charge_term >= 8){
            self.charge_consume += 8;
            self.charge_term -= 8;
            self.this_turn_strength += 2;
            player.hp -= player.max_hp * 0.0015 * self.charge_strength;
        }else if(self.charge_term > 0){
            self.charge_consume += self.charge_term;
            self.charge_term = 0;
        }
        while(self.charge_consume >= 10){
            self.charge_consume -= 10;
            self.charge_strength++;
        }
        int a = player.hp;
        m.dealDamage(6, player, self);
        if(a > player.hp){
            self.charge_term += 6;
        }
    }

    public EnemyAction brainAlbina(Entity self, Entity player) {
        EnemyAction FasciaHungers = new EnemyAction("法西娅正在挨饿呢~", this::FasciaHungers);
        EnemyAction TimeForYourMeal = new EnemyAction("我会好好利用你身上的血管的", this::TimeForYourMeal);
        EnemyAction DepressedFlesh = new EnemyAction("受压肉体的解放！", this::DepressedFlesh);
        EnemyAction DepressedFlesh_Premium = new EnemyAction("我已启动", this::DepressedFleshPremium);
        EnemyAction nullAction = new EnemyAction("混乱中，不会行动", null);

        self.panic = panicCheck(self);
        if (self.panic == PANIC) {
            self.strength += 4;
            self.stagger_panic_term++;
            self.panic = 0;
            return nullAction;
        } else if (self.panic == LOW_MORALE) {
            self.strength += 2;
            player.this_turn_strength += 4;
        }
        self.panic = 0;

        if(staggerCheck(self)){
            return nullAction;
        }

        if (self.countC != 1) {
            self.countB = 0;
            restriction(self, player);
            self.countC = 1;
        }

        self.this_turn_strength += self.charge_strength;

        self.charge_term += 8;
        //正常回合开始则获得8充能 -- 血肉材料

        if (self.charge_strength >= 5 || self.hp <= self.outside_max_hp * 0.6){
            self.countB = 1;
            if(self.charge_strength >= 5){
                self.charge_strength += 2;
                self.strength += 2;
            }
        }

        if (m.randint(1, 40) < self.charge_term){
            if(self.countB != 0){
                return DepressedFlesh_Premium;
            }
            return DepressedFlesh;
        }else{
            if (self.countB != 0){
                return TimeForYourMeal;
            }
            return FasciaHungers;
        }
    }

    public void questionedornot(Entity self, Entity player){
        if(m.dealDamage(10, player, self)) {
            player.tremor_term += 5;
            player.tremor_strength += 10;
        }
    }

    public void waw(Entity self, Entity player){
        self.this_turn_strength = -999;
        player.this_turn_strength = -999;
    }

    public void niule(Entity self, Entity player){
        self.buff.cibei = 3;
        self.name = "因为变区而哭嚎的艾兰虫";
        if(self.shin < self.buff.cibei){
            self.shin = self.buff.cibei;
        }
    }

    public void fault1(Entity self, Entity player){
        if(player.name.equals("小金") || player.name.equals("金智勋") || player.name.equals("张施娜")){
            self.buff.cibei += 30;
            self.shin+=30;
            self.strength += 30;
        }else{
            self.buff.cibei += 1;
            self.shin++;
        }
    }

    public void fault2(Entity self, Entity player){
        if(player.name.equals("小金") || player.name.equals("金智勋") || player.name.equals("张施娜")){
            self.buff.cibei += 30;
            self.shin += 30;
            self.strength += 30;
        }else{
            self.buff.cibei += 1;
            self.shin++;
        }
    }

    public EnemyAction brainQuQu(Entity self, Entity player){
        EnemyAction stater = new EnemyAction("如果就这么让加强离开，那么我一辈子都会变成区吧...", null);
        EnemyAction atk = new EnemyAction("孩子们，你们到底问了没？", this::questionedornot);
        EnemyAction waw = new EnemyAction("同是瓦夜人格，怎会。。。", this::waw);
        EnemyAction qwq = new EnemyAction("。。。原来，我生来就只是区么，卡门女士。。。", this::niule);
        EnemyAction stk = new EnemyAction("创造出了我又不给出强度，小金，都是你的错！", this::fault1);
        EnemyAction ttk = new EnemyAction("强度不行还不给EGO，金智勋，还是你的错！", this::fault2);
        EnemyAction ftk = new EnemyAction("出五破了都不给我加强，张施娜，又是你的错！", this::fault1);
        self.shin++;

        if(self.countA != 1){
            self.countA = 1;
            self.countB = 1;
            self.countC = 0;
            restriction(self, player);
            return stater;
        }

        if(self.shin < self.buff.cibei){
            self.shin = self.buff.cibei;
        }
        if(self.buff.cibei < self.shin){
            self.buff.cibei = self.shin;
        }

        if(self.countB == 1){
            if(self.hp <= self.max_hp * 0.95){
                self.countB = 2;
                return qwq;
            }
            switch (m.randint(1, 2)){
                case 1:
                    return atk;
                case 2:
                    return waw;
            }
        }else{
            self.countC++;
            switch(self.countC){
                case 1:
                    return stk;
                case 2:
                    return ttk;
                case 3:
                    self.countC = 0;
                    return ftk;
            }
        }
        return stater;
    }


    public void indexAttack(int amount, Entity target, Entity self){
        int k = target.hp;
        m.dealDamage(amount, target, self);
        if(k <= target.hp){
            if(self.buff.Unlock >= 3) return;
            self.krama += 5;
        }else if(self.buff.Unlock < 3){
            self.buff.Unlock++;
            switch (self.buff.Unlock){
                case 1:
                    Toast.makeText(s, "无我梦中", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(s, "阿鼻叫唤", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(s, "支离灭裂", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public void atkSlash(Entity self, Entity player){
        indexAttack(5, player, self);
        self.strength++;
        player.sinking_term += 3;
        self.this_turn_strength += 2;
        player.rapture_strength += 2;
        player.rapture_term += 5;
    }

    public void atkBlunt(Entity self, Entity player){
        indexAttack(8, player, self);
        m.amplitudeConversion(player, 1);
        player.tremor_term += 10;
        player.sinking_strength += 2;
        player.tremor_strength += 3;
        self.this_turn_strength += 3;
        for(int i = 0; i < 3; i++) m.tremorBurst(player);
    }

    public void atkThrust(Entity self, Entity player){
        indexAttack(10, player, self);
        player.hp -= 3;
        player.sinking_term += 2;
        player.bleed_strength += 8;
        player.bleed_term += 3;
    }

    public void normalAtk(Entity self, Entity player){
        switch(m.randint(1,3)){
            case 1:
                atkBlunt(self, player);
                break;
            case 2:
                atkSlash(self, player);
                break;
            case 3:
                atkThrust(self, player);
                break;

        }
        self.grace++;
    }

    public void FuriosoReplica(Entity self, Entity player){
        for(int i = 0; i < 9; i++){
            player.sinking_strength += 1;
            switch(m.randint(1,3)){
                case 1:
                    atkBlunt(self, player);
                    break;
                case 2:
                    atkSlash(self, player);
                    break;
                case 3:
                    atkThrust(self, player);
                    break;

            }
            self.strength *= 0.8 + 0.1 * self.buff.Unlock;
        }
    }

    public void FuriosoReplicaCresendo(Entity self, Entity player){
        player.sinking_term++;
        for(int i = 0; i < 9; i++){
            player.sinking_strength += 2;
            switch(m.randint(1,3)){
                case 1:
                    atkBlunt(self, player);
                    break;
                case 2:
                    atkSlash(self, player);
                    break;
                case 3:
                    atkThrust(self, player);
                    break;

            }
            self.strength *= 0.9 + 0.1 * self.buff.Unlock;
            player.sinking_term++;
        }
    }

    public void FuriosoReplicaCresendoReqiuemMass(Entity self, Entity player){
        player.sinking_term++;
        for(int i = 0; i < 9; i++){
            player.sinking_strength += 3;
            switch(m.randint(1,3)){
                case 1:
                    atkBlunt(self, player);
                    break;
                case 2:
                    atkSlash(self, player);
                    break;
                case 3:
                    atkThrust(self, player);
                    break;

            }
            self.this_turn_strength += 2;
            self.strength *= 1 + 0.1 * self.buff.Unlock;
            player.sinking_strength++;
            player.sinking_term++;
        }
    }

    public void eStaggerAction(Entity self, Entity player){
        normalAtk(self, player);
        player.sinking_strength = 0;
        player.sinking_term = 0;
        player.stagger_panic_term = 0;
        player.sanity = 45;
    }

    public EnemyAction brainRein(Entity self, Entity player){
        //这个做的是真特喵用心
        EnemyAction announce_Rein = new EnemyAction("如果就这样让你离开，那么你永远都不会回来了吧...", null);
        EnemyAction atkI = new EnemyAction("一", this::normalAtk);
        EnemyAction atkII = new EnemyAction("二", this::normalAtk);
        EnemyAction atkIII = new EnemyAction("三(trois)", this::normalAtk);
        EnemyAction atkIV = new EnemyAction("四", this::normalAtk);
        EnemyAction atkV = new EnemyAction("五", this::normalAtk);
        EnemyAction atkVI = new EnemyAction("六(six)", this::normalAtk);
        EnemyAction atkVII = new EnemyAction("七，还剩二", this::normalAtk);
        EnemyAction atkVIII = new EnemyAction("八，还剩一", this::normalAtk);
        EnemyAction atkIX = new EnemyAction("九(neuf)，完成", this::normalAtk);
        EnemyAction nullAction = new EnemyAction("哈哈哈，挺厉害的嘛", null);
        EnemyAction eStaggerAction = new EnemyAction("就连那副模样，也依然如此美丽么？", this::eStaggerAction);
        EnemyAction FuriosoReplicaFirst = new EnemyAction("啊，结果……还是到了这一天啊。", this::FuriosoReplica);
        EnemyAction FuriosoReplica = new EnemyAction("女儿。你知道的吧？预测是没有意义的。", this::FuriosoReplica);
        EnemyAction FuriosoReplica_CresendoFst = new EnemyAction("无需踏上旅途，这里就是你该待的地狱不是么？", this::FuriosoReplicaCresendo);
        EnemyAction FuriosoReplica_CresendoI = new EnemyAction("这奈落就是你该待的地方。", this::FuriosoReplicaCresendo);
        EnemyAction FuriosoReplica_CresendoII = new EnemyAction("活在被定制的人生中的愚蠢之人。那正是你。也是我啊。", this::FuriosoReplicaCresendo);
        EnemyAction FuriosoReplica_CresendoIII = new EnemyAction("事到如今你也无法自由地活下去了。就算你逃跑，神的兵卒也会来追捕你。", this::FuriosoReplicaCresendo);
        EnemyAction Furioso_Cresendo_ReqiuemMassI = new EnemyAction("这是不能被斩断的，女儿。……你不能擅自斩断它。", this::FuriosoReplicaCresendoReqiuemMass);
        EnemyAction Furioso_Cresendo_ReqiuemMassII = new EnemyAction("女儿。别怨恨我。一码归一码。", this::FuriosoReplicaCresendoReqiuemMass);
        EnemyAction Furioso_Cresendo_ReqiuemMassIII = new EnemyAction("只有撕开天空……把那些想带走我女儿的东西统统清除。", this::FuriosoReplicaCresendoReqiuemMass);

        self.panic = panicCheck(self);
        if (self.panic == PANIC) {
            player.sinking_term = 0;
            player.sinking_strength = 0;
            player.sanity = 45;
        } else if (self.panic == LOW_MORALE) {
            player.sinking_term = 0;
            player.sinking_strength = 0;
            player.this_turn_strength += 4;
        }
        self.panic = 0;

        if(self.hp <= self.max_hp*0.6 && self.countC == 1){
            self.countC = 2;
            if(self.buff.UnlockedHealth > 0){
                self.buff.UnlockedHealth = 0;
            }
            //失去第一次锁血
            self.buff.lockedHealth--;
            self.stagger_panic_term = 0;
            if(self.hp < self.max_hp){
                self.hp = (int) (0.6*self.max_hp);
            }
//            if(self.hp <= self.max_hp*0.3){
//                self.countB++;
//                self.countC = 3;
//                switch(m.randint(1, 3)){
//                    case 1:
//                        return Furioso_Cresendo_ReqiuemMassI;
//                    case 2:
//                        return Furioso_Cresendo_ReqiuemMassII;
//                    case 3:
//                        return Furioso_Cresendo_ReqiuemMassIII;
//                }
//            }
            return FuriosoReplica_CresendoFst;
        }

        if(self.hp <= self.max_hp*0.3 && self.countC == 2){
            self.countC = 3;
            self.hp = (int) (self.max_hp * 0.3);
            if(self.buff.UnlockedHealth > 0) self.buff.UnlockedHealth = 0;
            self.buff.lockedHealth--;
            self.stagger_panic_term = 0;
            self.stagger_count = 4;
            //至此，再无锁血
            switch(m.randint(1, 3)){
                case 1:
                    return Furioso_Cresendo_ReqiuemMassI;
                case 2:
                    return Furioso_Cresendo_ReqiuemMassII;
                case 3:
                    return Furioso_Cresendo_ReqiuemMassIII;
            }
        }

        if(staggerCheck(self)){
            return nullAction;
        }

        if(player.sanity <= -45 || player.stagger_panic_term > 0){
            return eStaggerAction;
        }

        if (self.countC == 0) {
            self.countB = 0; //灼烧着的伤口
            self.grace = 0; //？！区区？！赫尔墨斯
            self.buff.lockedHealth = 2;
            restriction(self, player);
            self.countC = 1;
            return announce_Rein;
        }

        if(player.sinking_strength <= 5) player.sinking_strength = 5;

        if(self.countB > 0){
            switch(self.countB){
                case 1:
                    self.hp *= 0.99;
                    self.strength++;
                    self.countB++;
                    break;
                default:
                    self.hp *= 1-0.01 * self.countB;
                    self.strength++;
                    self.strength += self.countB * 0.1;
            }
        }



        if (self.grace >= 9){
            self.grace = 0;
            switch(self.countC){
                case 0:
                    //这行不通
                    return nullAction;
                case 1:
                    if(self.countA != 1){
                        self.countA = 1;
                        return FuriosoReplicaFirst;
                    }
                    return FuriosoReplica;
                    //九(neuf), 完成
                case 2:
                    switch(m.randint(1, 3)){
                        case 1:
                            return FuriosoReplica_CresendoI;
                        case 2:
                            return FuriosoReplica_CresendoII;
                        case 3:
                            return FuriosoReplica_CresendoIII;
                    }
                case 3:
                    switch(m.randint(1, 3)){
                        case 1:
                            return Furioso_Cresendo_ReqiuemMassI;
                        case 2:
                            return Furioso_Cresendo_ReqiuemMassII;
                        case 3:
                            return Furioso_Cresendo_ReqiuemMassIII;
                    }
                    //这招式名字越来越长了
            }
        }
        switch (self.grace){
            case 0:
                return atkI;
            case 1:
                return atkII;
            case 2:
                return atkIII;
            case 3:
                return atkIV;
            case 4:
                return atkV;
            case 5:
                return atkVI;
            case 6:
                return atkVII;
            case 7:
                return atkVIII;
            case 8:
                return atkIX;
                //neuf,完成
        }
        return nullAction;
    }

    public void Disposal_Valencina(Entity self, Entity player){
        if(player.block > 0){
            self.this_turn_strength += 40;
        }
        m.amplitudeConversion(player, 1);
        for(int i = 0; i < 4; i++){
            if(self.consumeAmmo(3)){
                self.buff.Precongization++;
                self.swift += 2;
                self.strength *= 1.125;
                self.this_turn_strength *= 1.5;
            }else{
                jielu(self, player);
            }
            self.this_turn_strength += (self.poise_strength > 40 ? 10 : (self.poise_strength / 4));
            if(m.dealDamage(7, player, self)){
                player.tremor_strength += 15;
                player.burn_strength += 9;
                player.burn_term += 2;
                player.tremor_term += 6;
            }
        }

        if(m.dealDamage(10, player, self)){
            for (int i = 0; i < 5; i++) m.tremorBurst(player);
            if(player.stagger_panic_term > 1){
                self.countB = 1;
            }
        }
        if(self.buff.Precongization < 10) self.buff.Precongization = 10;
    }

    public void jielu(Entity self, Entity player){
        self.swift += 3;
        while(self.consumeAmmo(1)){
            self.buff.sidestep += 2;
            self.this_turn_strength += 2;
        }
        self.reload();
        for(int i = 0; i < 3; i++){
            if(m.dealDamage(3, player, self)){
                m.tremorBurst(player);
            }
        }
    }

    public EnemyAction brainValencina(Entity self, Entity player){
        EnemyAction Disposal = new EnemyAction("烦死了烦死了，你和券券全都烦死了！", this::Disposal_Valencina);
        EnemyAction jielu = new EnemyAction("哪个*拇指粗口*说这招叫解开就🦌的？！", this::jielu);
        EnemyAction nullAction = new EnemyAction("由于对拇指妈可能过于难以启动，故而让其静坐一回合---开发者", null);
        EnemyAction stagger = new EnemyAction("混乱了呢☺️❀", null);

        if(self.stagger_panic_term > 0) {
            self.buff.sidestep = 0;
            return stagger;
        }

        if(self.countA != 1){
            self.ammoType = 1; //加速弹
            self.totalAmmo = 20;
            self.reload();
            restriction(self, player);
            self.buff.Precongization = 20;
            self.countA = 1;
        }
        self.buff.sidestep = 0;
        self.buff.sidestep += self.swift + self.buff.Precongization;
        int a = (self.poise_strength + self.poise_term)/10;
        self.this_turn_strength += a;
        self.strength += a / 10;
        self.countC += self.buff.Precongization;
        if(m.randint(1, 100) < self.countC) {
            self.countC = 0;
            self.countB = 1;
        }
        if(self.buff.Precongization <= 1 || self.countB == 1){
            self.countB = 0;
            return Disposal;
        }else{
            return jielu;
        }
    }

    public void painted(Entity self, Entity player){
        //涂·抹
        self.buff.noSelf += 3;
        if(self.max_hp < self.hp){
            m.defend(self.hp - self.max_hp, self);
            self.hp = self.max_hp;
        }
        player.poise_strength = 0;
        player.poise_term = 0;
        if(m.dealDamage((int) (player.hp * (0.05 + 0.001 * (self.strength > 300 ? 300 : self.strength))), player, self)) player.max_hp = player.hp;
        if(player.hp <= 1){
            player.stagger_panic_term += 2;
            player.hp = 1;
        }
        self.strength += ((self.buff.noSelf > 10 )? 5 : self.buff.noSelf / 2);
    }

    public void duanYuan(Entity self, Entity player){
        player.blade += self.buff.noSelf;
        for(int i = 0; i < player.blade; i++){
            player.max_hp *= 0.999;
        }
        painted(self, player);
    }

    public void clearyouclearme(Entity self, Entity player){
        self.buff.noSelf = 999;
        player.block = 0;
        player.buff.sidestep = 0;
        m.dealDamage((int)(player.max_hp * (0.5 * 0.01 * (Math.min((self.buff.noSelf + self.strength), 150)))), player, self);
        player.max_hp = player.hp;
        if(player.hp < 0){
            player.max_hp = 0;
            player.hp = 0;
        }
    }

    public EnemyAction brainYoshide(Entity self, Entity player){
        EnemyAction painted = new EnemyAction("涂抹", this::painted);
        EnemyAction clear = new EnemyAction("将口抹去，也将口口抹口", this::clearyouclearme);
        EnemyAction pinted = new EnemyAction("口抹", this::painted);
        EnemyAction pante = new EnemyAction("涂口", this::painted);
        EnemyAction ptd = new EnemyAction("口口", this::painted);
        EnemyAction duan = new EnemyAction("断·口", this::duanYuan);
        EnemyAction yuan = new EnemyAction("口·缘", this::duanYuan);
        EnemyAction nothing = new EnemyAction("口·口", this::duanYuan);
        EnemyAction stagger = new EnemyAction("陷·混", null);
        self.sanity = -44;
        if(self.countC != 1){
            restriction(self, player);
            self.countC = 1;
            self.buff.noSelf += 0.1 * player.restrictions > 100 ? 100 : (int) (0.1 * player.restrictions);
            self.countB = 0;
            self.buff.lockedHealth += 2;
        }
        if(self.max_hp < self.hp){
            self.stagger_panic_term = 0;
            self.stagger_count = 4;
            m.defend(self.hp - self.max_hp, self);
            self.hp = self.max_hp;
        }
        if(self.buff.UnlockedHealth > 0){
            self.buff.UnlockedHealth = 0;
            self.buff.lockedHealth--;
            self.stagger_panic_term = 0;
            self.stagger_count = 4;
            self.max_hp = (int) (self.outside_max_hp * 0.3 * (1 + player.restrictions * 0.03));
            self.hp = self.max_hp;
            self.buff.noSelf /= 2;
        }
        if(self.stagger_panic_term > 0 && self.buff.lockedHealth == 0) return stagger;
        if(player.max_hp < player.outside_max_hp * 0.10 || player.max_hp < 100 || self.buff.noSelf >= 300){
            return clear;
        }
        if(self.buff.noSelf < 10) {
            return painted;
        }else if(self.buff.noSelf < 30){
            switch (m.randint(1, 2)){
                case 1:
                    return pinted;
                case 2:
                    return pante;
            }
            return pinted;
        }else if(self.buff.noSelf < 50){
            return ptd;
        }else if(self.buff.noSelf < 70){
            switch (m.randint(1, 2)){
                case 1:
                    return duan;
                case 2:
                    return yuan;
            }
        }else{
            return nothing;
        }
        return painted;
    }

    public void Purify(Entity self, Entity player){
        int k = 3;
        for(int i = 0; i < k; i++){
            if(m.dealDamage(18, player, self)){
                player.bleed_strength += 9;
                player.bleed_term += 4;
            }
            if(player.bleed_strength > i * 18){
                k++;
            }
        }
        if(k >= 5){
            player.stagger_panic_term += 2;
        }
    }

    public void Whistle(Entity self, Entity player){
        self.strength += 8;
        m.heal((int) (self.max_hp * 0.4), self);
        player.strength -= 4;
    }

    public void hold(Entity self, Entity player){
        if(m.dealDamage(9, player, self)){
            player.bleed_term += 6;
            player.strength -= 2;
        }
    }

    public void GreatPurify(Entity self, Entity player){
        self.strength += Math.min(player.bleed_strength/5, 10);
        int k = 50;
        for(int i = 0; i < 1; i++){
            if(player.bleed_strength > k){
                k += 50;
                i--;
            }
            if(m.dealDamage(60, player, self)){
                player.bleed_strength += 13;
                player.bleed_term += 4;
            }
        }
    }

    public void loyalfulHold(Entity self, Entity player){
        self.burn_strength = ((self.burn_strength - 3 > 0) ? (self.burn_strength - 3) : (1));
        self.burn_term += 4;
        if(m.dealDamage(22, player, self)){
            player.bleed_strength += 1;
            player.bleed_term += 7;
            player.swift -= 3;
        }
    }

    public EnemyAction brainKromo(Entity self, Entity player){
        EnemyAction Purify = new EnemyAction("净↑化↓", this::Purify);
        EnemyAction Whistle = new EnemyAction("辛克莱哟~(神秘の哨音)", this::Whistle);
        EnemyAction Hold = new EnemyAction("就由我来执握", this::hold);
        EnemyAction GreatPurify = new EnemyAction("崇高的净↑化↓", this::GreatPurify);
        EnemyAction loyalfulHold = new EnemyAction("荣耀の执握", this::loyalfulHold);

        if(self.countC == 0){
            self.countC = 1;
            restriction(self, player);
            self.buff.lockedHealth += 1;
        }
        if(self.burn_term > 0 && self.burn_strength > 0 || self.bleed_term > 0 && self.bleed_strength > 0){
            self.strength += 3;
            m.heal((int) (self.max_hp * 0.3), self);
        }
        switch (self.countC){
            case 1:
                if(self.buff.UnlockedHealth > 0){
                    self.max_hp *= 1.4;
                    self.hp = self.max_hp;
                    self.stagger_panic_term = 0;
                    self.stagger_count = 4;
                    self.buff.UnlockedHealth = 0;
                    self.countC = 2;
                    self.name = "欲成原初的克罗默";
                    self.buff.lockedHealth--;
                    return GreatPurify;
                    //进二阶段了
                }
                switch (m.randint(1, 2)){
                    case 1:
                        if(m.randint(1, 100) < 24){
                            return Purify;
                        }else{
                            return Whistle;
                        }
                    case 2:
                        return Hold;
                }
                break;
            case 2:
                if(m.randint(1, 4) == 4){
                    return GreatPurify;
                }else{
                    return loyalfulHold;
                }
            default:
                break;
        }
        return Hold;
    }

    public void hajia(Entity self, Entity player){
        self.sinking_term += 3;
        self.sinking_strength += 4 + self.shin;
        if(m.dealDamage(14, player, self)) {
            player.rapture_term += 2;
            player.rapture_strength += 8 + self.shin;
            if (self.sanity < 0) {
                m.tremorBurst(player);
            }
        }
    }

    public void kai(Entity self, Entity player){
        self.sinking_term += 3;
        self.sinking_strength += 6 + self.shin;
        if(m.dealDamage(20, player, self)){
            player.rapture_strength += 18 + self.shin;
        }
    }

    public void EGO_CorrideBandageKing(Entity self, Entity player){
        Toast.makeText(s, "这些绳子既是绷带，也是枷锁……到底何时才能让我解放!!", Toast.LENGTH_SHORT);
        s.appendLog("那一天的伞夫，侵蚀起来~");
        self.swift -= 8;
        self.strength += 5;
        self.this_turn_strength += self.swift * -1;
        player.swift -= 3;
        int k = 1;
        int j = 8;
        int a = 2;
        if(self.swift < -10){
            k++;
            if(self.swift < -30){
                k++;
            }if(self.swift < -75){
                k++;
            }
        }
        for(int i = 0; i < k; i++){
            if(m.dealDamage(28, player, self)){
                m.heal((int) (self.max_hp * 0.01 * a), self);
                a *= 2;
                player.sanity -= j;
                j /= 2;
                player.tremor_strength += 14;
                player.tremor_term += 4;
                m.tremorBurst(player);
            }
        }
        self.sinking_term = 0;
        self.sinking_strength = 0;
        self.shin++;
        self.sanity = 0;
        self.name = "伞神";
    }

    public EnemyAction brainGOD(Entity self, Entity player){
        EnemyAction Hajia = new EnemyAction("哈加！", this::hajia);
        EnemyAction kai = new EnemyAction("开！", this::kai);
        EnemyAction Corride_BandageKing = new EnemyAction("这些绳子既是绷带，也是枷锁……到底何时才能让我解放！！", this::EGO_CorrideBandageKing);

        if(self.sanity <= -45){
            m.sanityReturn(self);
            self.name = "被缚之王";
            self.swift -= 8;
            self.strength += 6;
            return Corride_BandageKing;
        }
        self.swift -= 3;
        self.strength += 2;

        while(self.sinking_term > 10){
            m.sinking(self);
        }

        if(self.buff.rainoftears != 1){
            //被动：泪雨
            self.buff.rainoftears = 1;
            self.buff.lockedHealth++;
            restriction(self, player);
        }

        if(self.buff.UnlockedHealth > 0){
            self.hp = (int) (0.4*self.max_hp);
            self.buff.lockedHealth--;
        }
        switch (m.randint(1, 3)){
            case 1:
                return kai;
            case 2:
            case 3:
            default:
                return Hajia;
        }
    }
}