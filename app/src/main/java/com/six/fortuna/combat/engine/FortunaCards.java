package com.six.fortuna.combat.engine;

import static android.widget.Toast.LENGTH_SHORT;
import static androidx.core.content.ContextCompat.startActivity;

import android.widget.EditText;
import android.widget.Toast;

import com.six.fortuna.StatsActivity;
import com.six.fortuna.combat.engine.CardTypes.Card;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

/**
 * 原样搬运 cards.c 里目前完整可见的卡牌效果函数。
 * cards.c 里还有一批被截断没看到全文的效果（RendSpace/TripleSlash/heavyArmor/firebath/
 * Disposal/ignite/curse_slime/curse_heaviness/curse_NoSelf/GuGuGaGa/Reqiuem等），
 * 那些等你发我完整代码或者要用到的时候再补，没看到的我不瞎编。
 */
public class FortunaCards{

    private final Mechanics m;
    private final StatsActivity s;

    public void chargeCycle(Entity self, int cycleLevel){
        while(self.charge_consume >= cycleLevel){
            self.charge_consume -= cycleLevel;
            self.charge_strength++;
        }
    }

    public FortunaCards(Mechanics mechanics, StatsActivity s) {
        this.m = mechanics;
        this.s = s;
    }

    // --- effect_strike: deal_damage(7, enemy, player) ---
    public void effectStrike(Entity player, Entity enemy) {
        m.dealDamage(7, enemy, player);
    }

    // --- effect_defend: execute_gain_defense(7, player, player) ---
    public void effectDefend(Entity player, Entity enemy) {
        player.block += 7 + player.swift;
    }

    // --- curse_burning: 诅咒牌，抽到手里就自烧 ---
    public void curseBurning(Entity player, Entity enemy) {
        player.burn_term += 3;
        player.burn_strength += 2;
        m.burn(player);
    }

    // --- effect_swordflashing / effect_swordflashing_yan ---
    public void effectSwordflashingYan(Entity player, Entity enemy) {
        enemy.burn_term = (int) (enemy.burn_term * 1.5);
        player.energy += 1;
        for (int i = 0; i < 5; i++) m.burn(enemy);
        if (enemy.burn_strength * enemy.burn_term >= 1000) {
            player.poise_strength = (int) (player.poise_strength * 1.5);
            m.dealDamage((int) ((enemy.burn_strength * enemy.burn_term * 0.5) * (1 + 0.005 * player.poise_strength)), enemy, player);
            player.energy += 1;
            enemy.burn_term = (int) (enemy.burn_term / 1.25);
            Toast.makeText(s, "触发了森罗焱象，恢复2光芒", Toast.LENGTH_LONG).show();;
        }else{
            Toast.makeText(s, "触发了森罗炎象，恢复1光芒", LENGTH_SHORT).show();
        }
    }

    public void effectSwordflashing(Entity player, Entity enemy) {
        player.poise_strength += 5;
        player.poise_term += 10;
        if ((enemy.burn_term * enemy.burn_strength) >= 100 && enemy.burn_term >= 5) {
            effectSwordflashingYan(player, enemy);
        }
        m.dealDamage(15, enemy, player);
        player.block += 10;
    }

    // --- effect_Beheading ---
    public void effectBeheading(Entity player, Entity enemy) {
        enemy.sinking_strength += 3;
        enemy.sinking_term += 4;
        player.sanity += enemy.sinking_strength * enemy.sinking_term;
        m.sanityReturn(enemy);
        m.sanityReturn(player);
        player.poise_strength += enemy.sinking_strength;
        player.poise_term++;
        m.dealDamage((int) ((5 + player.strength) * (1 + 0.01 * player.sanity + 0.45) * (1 + 0.2 * player.shin)), enemy, player);
        if (player.shin >= 1) {
            player.sanity -= 15;
            if (player.sanity <= -45) player.shin = 0;
        }
        if (player.sanity >= 25) {
            player.sanity -= 10;
            player.shin++;
        }
    }

    // --- effect_MemorialProcession ---
    public void effectMemorialProcession(Entity player, Entity enemy) {
        enemy.sinking_strength += 6;
        enemy.sinking_term += 7;
        player.sanity += enemy.sinking_strength * enemy.sinking_term;
        m.sanityReturn(enemy);
        m.sanityReturn(player);
        player.poise_strength += enemy.sinking_strength;
        player.poise_term++;
        m.dealDamage((int) ((10 + player.strength) * (1 + 0.02 * player.sanity + 0.9) * (1 + 0.4 * player.shin)), enemy, player);
        if (player.shin >= 1) {
            player.sanity -= 15;
            if (player.sanity <= -45) player.shin = 0;
        }
        if (player.sanity >= 25) {
            player.sanity -= 10;
            player.shin++;
        }
    }

    // --- effect_InevitableSlash（需要CardDeck.drawCard配合，故留一个重载接drawCard调用方）---
    public void effectInevitableSlash(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        deck.drawCard(player, logger);
        enemy.blade += 5 + 0.02 * (enemy.blade + 0.6 * enemy.poise_strength);
        player.poise_term++;
        player.poise_strength += 5;
        player.buff.nextTurnLight++;
    }

    public void effectJishixingle(Entity player, Entity enemy){
        if(player.charge_term >= 4){
            player.charge_consume += 4;
            player.charge_term -= 4;
            chargeCycle(player, 10);
        }
        m.dealDamage(m.randint(1, 12), enemy, player);
        enemy.stagger_panic_term++;
    }

    public void effectAfterImageStep(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        m.sidestep(8, player);
        if (enemy.blade >= 60){
            enemy.blade -= 8;
            deck.addCard(afterImageStepCard(deck, logger), logger);
        }
    }

    public void effect_RendSpace(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        enemy.blade += 12 + player.poise_strength/10 + enemy.blade/10;
        if (enemy.blade >= 70){
            deck.addCard(RendSpaceYuanCard(deck, logger), logger);
            if (player.poise_strength < 20){
                player.poise_strength = 20;
                if(player.poise_term <= 0){
                    player.poise_term = 2;
                }
            }
        }
    }

    public void effect_RendSpace_Yuan(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        m.dealDamage((int) (enemy.max_hp * 0.01 * enemy.blade), enemy, player);
        if(enemy.hp <= 0){
            enemy.hp = 1;
            enemy.burn_term += 10;
            enemy.burn_strength += 100;
        }
        for (int i = 0; i < 8; i++){
            curse_NoSelf(player, enemy);
        }
    }

    public void effect_EGO_RedBeauty(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        enemy.burn_term += 2;
        enemy.burn_strength += 3 * player.energy;
        for(int i = 0; i < player.energy; i++){
            enemy.burn_term += 2;
            enemy.burn_strength *= 1.1;
            m.burn(enemy);
        }
        if(player.energy < 4){
            for(int i = 0; i < 4 - player.energy; i++){
                deck.addCard(burningCurseCard(), logger);
            }
            if(player.energy == 0){
                player.hp *= 0.75;
            }
        }
        player.energy = 0;
    }

    public void effect_WordOfPower_Death(Entity player, Entity enemy){
        m.dealDamage(enemy.max_hp * 2, enemy, player);
    }

    public void effect_comboshoot(Entity player, Entity enemy){
        player.poise_term++;
        if (player.ammo > 0 && player.ammoType != 3){
            player.ammo--;
            m.dealDamage(8, enemy, player);
            effect_comboshoot(player, enemy);
            player.poise_strength += 3;
            player.poise_term++;
        }
        if(player.ammoType == 3){
            for(int i = 0; i < 7; i++){
                m.dealDamage(16, enemy, player);
            }
        }
    }

    public void curse_NoSelf(Entity player, Entity enemy){
        player.max_hp *= 0.9;
    }

    public void effect_disposal(Entity player, Entity enemy){
        effect_comboshoot(player, enemy);
        player.totalAmmo = 10;
        player.ammoType = 1;
        player.reload();
        player.consumeAmmo(10);
        m.amplitudeConversion(enemy, 1); //震颤：灼热
        for (int i = 0; i < 5; i++){
            m.dealDamage(5, enemy, player);
            enemy.tremor_strength += 7;
            enemy.tremor_term += 4;
            enemy.burn_strength += 3;
            enemy.burn_term += 4;
            player.reload();
            player.consumeAmmo(10);
        }
        for(int i = 0; i < 5; i++) m.tremorBurst(enemy);
    }

    public void effect_ProliferatingG(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        //G公司来的
        if(player.energy <= 0){
            m.dealDamage(8, enemy, player);
            m.defend(8, player);
            return;
        }
        player.energy--;
        for(int i = 0; i < 4 + (deck.drawPile.size() + deck.discardPile.size())/75; i++){
            deck.discardPile.add(PoliferatingG_Card(deck, logger));
        }

        for(int i = 0; i < 7 + (deck.drawPile.size() + deck.discardPile.size()) / 10; i++){
            switch (m.randint(1,6)){
                case 1:
                    enemy.burn_strength += 2;
                    break;
                case 2:
                    enemy.rapture_strength += 2;
                    break;
                case 3:
                    enemy.sinking_strength += 2;
                    break;
                case 4:
                    enemy.tremor_strength += 2;
                    break;
                case 5:
                    enemy.bleed_strength += 2;
                    break;
                case 6:
                    player.poise_strength += 2;
                    break;
            }
        }
        for (int i = 0; i < 12 + (deck.drawPile.size() + deck.discardPile.size())/7; i++){
            switch (m.randint(1,6)){
                case 1:
                    enemy.burn_term += 2;
                    break;
                case 2:
                    enemy.rapture_term += 2;
                    break;
                case 3:
                    enemy.sinking_term += 2;
                    break;
                case 4:
                    enemy.tremor_term += 2;
                    break;
                case 5:
                    enemy.bleed_term += 2;
                    break;
                case 6:
                    player.poise_term += 2;
                    break;
            }
        }
        for(int i = 0; i < 3 + (deck.discardPile.size() + deck.drawPile.size())/150; i++){
            switch (m.randint(1, 5)){
                case 1:
                    m.burn(enemy);
                    break;
                case 2:
                    m.dealDamage(1, enemy, player);
                    break;
                case 3:
                    m.sinking(enemy);
                    break;
                case 4:
                    m.amplitudeConversion(enemy, m.randint(1, 3));
                    m.tremorBurst(enemy);
                case 5:
                    m.bleedActivate(enemy);
                    break;
            }
        }
        m.dealDamage(8+(deck.drawPile.size() + deck.discardPile.size())/33, enemy, player);
        m.defend(8+(deck.drawPile.size() + deck.discardPile.size())/33, player);
        deck.drawCard(player, logger);
    }

    public void effect_AnswerMeAllHeiShouPack(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        enemy.rapture_strength += 5;
        enemy.rapture_term += 2;
        m.dealDamage(10, enemy, player);
        player.poise_term+=2;
        new Builder(s)
                .setTitle("是否要召集黑兽？")
                .setMessage("还是说让她们闪开，自己来？")
                .setPositiveButton("@全体黑兽 ，收到扣三技能！", (dialog, which) -> {
                    switch (m.randint(1, 2)){
                        case 1:
                            deck.addCard(budaijiezou(0), logger);
                            break;
                        case 2:
                            Card s = Si();
                            s.cost = 0;
                            s.consumption = 1;
                            deck.addCard(s, logger);
                            break;
                        default:
                            deck.addCard(budaijiezou(0), logger);
                    }
                    deck.addCard(kaidao_Card(), logger);
                    s.runOnUiThread(() -> {
                        s.updateSpinner();
                        s.refreshAllUI();
                    });
                })
                .setNegativeButton("闪开，老子自己来！", ((dialog, which) -> {
                    deck.addCard(shankailaozizijilai(), logger);
                    s.runOnUiThread(() -> {
                        s.updateSpinner();
                        s.refreshAllUI();
                    });
                }))
                .setCancelable(false)
                .show();
    }

    public void effect_kaidao(Entity player, Entity enemy){
        for(int i = 0; i < 2; i++){
            enemy.rapture_term += 2;
            enemy.rapture_strength += 3;
            player.poise_strength += 4;
            player.poise_term++;
            m.dealDamage(8, enemy, player);
        }
    }

    public void effect_shankailaozizijilai(Entity player, Entity enemy){
        double k = 0.7;
        for (int i = 0; i < 4; i++){
            enemy.rapture_strength += 21;
            enemy.rapture_term += 10;
            player.poise_term += 3;
            player.poise_strength += 5;
            if(player.hp <= player.max_hp * k) {
                k -= 0.2;
                i--;
            }
            m.dealDamage(18, enemy, player);
        }
    }

    public void effect_budaijiezou(Entity player, Entity enemy){
        for(int i = 0; i < (player.swift >= 10 ? 4 : 3); i++){
            player.this_turn_strength += 3;
            enemy.rapture_term += 4;
            enemy.rapture_strength += 1 + player.swift / 5 + player.this_turn_strength / 3 + player.tianjiustarblade / 10;
        }
        player.swift += 5;
        if(player.swift >= 10){
            player.tianjiustarblade += 10 + player.swift / 15;
        }
        if(player.tianjiustarblade >= 75){
            effect_xiangxinwuhunzhenshen(player, enemy);
        }
    }

    public void effect_wudile(Entity player, Entity enemy){
        for (int i = 0; i < 2; i++) m.defend(1, player);
        Toast.makeText(s, "无敌了无敌了！", LENGTH_SHORT).show();
        if(player.tianjiustarblade >= 75){
            effect_xiangxinwuhunzhenshen(player, enemy);
        }else{
            player.swift += 5;
            player.tianjiustarblade += 2;
        }
    }

    public void effect_xiangxinwuhunzhenshen(Entity player, Entity enemy){
        Toast.makeText(s, "相信武魂真身！", LENGTH_SHORT).show();
        player.this_turn_strength += player.swift / 1.5;
        enemy.rapture_strength += 5 + player.swift / 2 + player.this_turn_strength / 3 + player.tianjiustarblade / 7;
        enemy.rapture_term += 3 + player.tianjiustarblade / 5;
        player.this_turn_strength += enemy.rapture_term;
        while(enemy.rapture_term > 1){
            m.dealDamage(0, enemy, player);
        }
        enemy.rapture_term = enemy.rapture_strength;
        enemy.rapture_strength = player.tianjiustarblade;
        player.tianjiustarblade = 0;
    }

    public void effect_Purify(Entity player, Entity enemy){
        for(int i = 0; i < 3; i++){
            m.dealDamage(8, enemy, player);
            enemy.bleed_strength += 9;
            enemy.bleed_term++;
        }
        player.bleed_strength = 0;
        player.bleed_term = 0;
        player.sanity = 45;
        player.rapture_strength = 0;
        player.rapture_term = 0;
        player.sinking_term = 0;
        player.sinking_strength = 0;
        player.tremor_conversion = 0;
        player.tremor_strength = 0;
        player.tremor_term = 0;
        if(player.swift < 0) player.swift = 0;
        if(player.strength < 0) player.strength = 0;
        player.restrictions = 0;
        player.buff.reset_Debuff();
        if(enemy.swift > 0) enemy.swift = 0;
        if(enemy.strength > 0) enemy.strength = 0;
        enemy.poise_strength = 0;
        enemy.poise_term = 0;
        enemy.buff.reset_Buff();
    }

    public void SeedofLight(Entity player, Entity enemy){
        //光之种！
        player.buff.SeedofLight += 1;
        while(player.energy >= 2){
            player.buff.SeedofLight++;
            player.energy -= 2;
        }
    }

    public void CollectLight(Entity player, Entity enemy){
        player.energy += 2;
        if(player.buff.SeedofLight > 0){
            player.energy++;
        }
        //消耗，获得2光芒
    }

    public void effect_Tiphereth(Entity player, Entity enemy){
        //存在意义的憧憬
        player.buff.Tiphereth++;
        new AlertDialog.Builder(s)
                .setMessage("警告：SEPHIRAH的核心崩溃导致逆卡巴拉能量实体化，需要立刻抑制Sephirah的核心")
                .setPositiveButton("知道了", null)
                .setNegativeButton("知道了", null)
                .setNeutralButton("知道了", null)
                .show();
    }

    public void effect_setDanger(Entity player, Entity enemy){
        final EditText input = new EditText(s);
        input.setHint("加护获得");
        new android.app.AlertDialog.Builder(s)
                .setTitle("调试卡·危险度设置")
                .setMessage("你希望危险度为？")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    try {
                        player.difficulty = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        Toast.makeText(s, "请输入合法数字", Toast.LENGTH_SHORT).show();
                    }
                    s.updateSpinner();
                    s.refreshAllUI();
                }).show();
    }

    public void effect_cogito(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        Card i = createByKey(ALL_CARD_KEYS[m.randint(0, ALL_CARD_KEYS.length-1)], deck, logger);
        i.cost = 0;
        i.consumption = 1;
        deck.addCard(i, logger);
        deck.drawCard(player, logger);
    }

    public void effect_300lunacy(Entity player, Entity enemy){
        enemy.current_intent = new EnemyAction("那还说什么呢？这回合给你了呗", null);
        Toast.makeText(s, "朋友费，你在哪？快到我邮箱里来啊！我能听见你的声音，但愿...\n---开发者", Toast.LENGTH_LONG);
    }

    public void effect_ququ(Entity player, Entity enemy){
        //自我吟诵
        player.buff.cibei++;
    }

    public void effect_zhuizhui(Entity player, Entity enemy) {
        if(m.dealDamage(8, enemy, player)) {
            enemy.bleed_term++;
            enemy.bleed_strength += 2;
            enemy.tremor_strength += 2;
            enemy.tremor_term++;
            m.tremorBurst(enemy);
        }
    }

    public void effect_Thunder(Entity player, Entity enemy){
        for (int i = 0; i < 2; i++)player.buff.addChargeBalls(1, enemy);
    }

    public void effect_CurrentGeneration(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger){
        int k = player.buff.currentgeneration;
        for (int i = 0; i < k; i++) {
            if(k >= 50){
                for(int j = 0; j < player.buff.chargeBallSize; j++)  player.buff.addChargeBalls(1, enemy);
                player.buff.ballDamage((k - player.buff.chargeBallSize) * (18+3 * (player.buff.concentration) - player.buff.concentration), player, enemy);
                player.buff.currentgeneration += k;
                s.appendLog("由于本次电流相生生成闪电充能球过多，会导致卡死等不太好的操作，故而统一结算，下次使用时，会生成闪电充能球数为"+player.buff.currentgeneration);
                break;
            }
            player.buff.addChargeBalls(1, enemy);
        }
        Card a = CurrentGeneration(deck, logger);
        if(player.buff.currentgeneration < 50) {
            a.name = "等我启动";
            deck.discardPile.add(a);
        }else{
            a.name = "我已启动";
            deck.discardPile.add(a);
        }
    }

    public void effect_calmBrain(Entity player, Entity enemy){
        player.buff.concentration += 5;
        player.buff.addChargeBalls(2, enemy);
        player.buff.nextTurnLight++;
    }

    public void effect_rainbow(Entity player, Entity enemy){
        for(int i = 1; i <= 3; i++) player.buff.addChargeBalls(i, enemy);
        player.buff.concentration += 5;
    }

    public void effect_charge(Entity player, Entity enemy){
        if(player.charge_strength <= 3){
            player.charge_term += 15;
            player.charge_consume += player.charge_term;
            player.charge_term = 0;
            chargeCycle(player, 8);
        }else if(player.charge_strength <= 5){
            player.charge_term += 7;
            player.charge_consume += player.charge_term;
            player.charge_term = 0;
            chargeCycle(player, 8);
            player.energy += 2;
        }else{
            player.buff.addChargeBalls(3, enemy);
        }
    }

    public void effect_sixthSense(Entity player, Entity enemy){
        player.buff.sidestep += 12 + player.swift;
        if(player.swift < 0) player.swift = 0;
        player.swift += (player.max_hp - player.hp) / (player.max_hp / 20);
    }

    public void effect_EGOMagicBullet(Entity player, Entity enemy){
        if(player.ammoType != 3) {
            player.ammoType = 3;
            player.totalAmmo = 7;
            player.reload();
        }
    }

    public void effect_Si(Entity player, Entity enemy){
        int k = 100;
        int j = 5;
        for(int i = 0; i < 3; i++){
            if(m.randint(1, k) < player.poise_strength * 5){
                i--;
                k += j;
                j *= j;
            }
            player.poise_term++;
            if(m.dealDamage(9, enemy, player)){
                enemy.rapture_strength += 5;
                enemy.rapture_term += 3;
                enemy.buff.poison += enemy.rapture_strength + enemy.rapture_term * 2;
            }
        }
    }

    // --- 构造出实际的Card对象（对应 cards.c 底部的全局卡牌定义）---
    public Card Si(){
        return new Card("Si", "疯狂の蛇(绝命巳乱)", 2, this::effect_Si, 3, 0);
    }
    public Card EGOMagicBullet(){
        return new Card("EGOMagicBullet", "脑叶公司E.G.O::魔弹", 0, this::effect_EGOMagicBullet, 3, 1);
    }
    public Card sixthSense(){
        return new Card("sixthSense", "第六感", 1, this::effect_sixthSense, 2, 0);
    }
    public Card charge(){
        return new Card("charge", "充电", 1, this::effect_charge, 2, 0);
    }
    public Card rainbow(){
        return new Card("rainbow", "虹彩", 3, this::effect_rainbow, 2, 1);
    }
    public Card CalmBrain(){
        return new Card("CalmBrain", "冷静头脑", 2, this::effect_calmBrain, 2, 0);
    }
    public Card CurrentGeneration(CardDeck deck, Mechanics.Logger logger){
        return new Card("CurrentGeneration", "电流相生", 3, (player, enemy) -> effect_CurrentGeneration(player, enemy, deck, logger), 3, 1);
    }
    public Card Thunder(){
        return new Card("Thunder", "雷动", 1, this::effect_Thunder, 1, 0);
    }
    public Card Elanzhuizhui(){
        return new Card("zhuizhui", "既见慈悲，为何不笑？", 0, (player, enemy) -> effect_zhuizhui(enemy, player), 1, 1);
    }
    public Card ququ(){
        return new Card("ququ", "艾兰市区", 1, this::effect_ququ, 3, 0);
    }
    public Card zhuizhui(){
        return new Card("zhuizhui", "既见慈悲，为何不笑？", 0, this::effect_zhuizhui, 1, 1);
    }
    public Card _300Lunacy(){
        return new Card("300Lunacy", "小金の朋友费", 1, this::effect_300lunacy, 2, 1);
    }
    public Card Cogito(CardDeck deck, Mechanics.Logger logger){
        return new Card("cogito", "Cogito", 1, (player, enemy) -> effect_cogito(player, enemy, deck, logger), 3, 0);
    }
    public Card setDanger(){
        return new Card("setDanger", "危险度设置", -2, this::effect_setDanger, 4, 1);
    }
    public Card Tiphereth(){
        return new Card("Tiphereth", "存在意义的憧憬", 0, this::effect_Tiphereth, 3, 1);
    }
    public Card SeedofLightCard(){
        return new Card("SeedofLight", "光之种计划", 3, this::SeedofLight, 3, 1);
    }
    public Card CollectLight(){
        return new Card("CollectLight", "收集光芒", 0, this::CollectLight, 1, 1);
    }
    public Card wudile(){
        return new Card("wudile", "无敌了无敌了！", 1, this::effect_wudile, 2, 0);
    }
    public Card Purify(){
        return new Card("Purify", "净化！", 3, this::effect_Purify, 3, 1);
    }
    public Card kaidao_Card(){
        return new Card("kaidao", "开·道(开辟君主之道吧！)", 0, this::effect_kaidao, 3, 1);
    }
    public Card shankailaozizijilai(){
        return new Card("shankailaozizijilai", "闪开，老子自己来", 0, this::effect_shankailaozizijilai, 3, 1);
    }
    public Card AnswerMeAllHeiShouPack_Card(CardDeck deck, Mechanics.Logger logger){
        return new Card("@AllHeiShou", "@全体黑兽 ，收到扣三技能", 3,
                (player, enemy) -> effect_AnswerMeAllHeiShouPack(player, enemy, deck, logger), 3, 0);
    }
    public Card budaijiezou(int cost){
        return new Card("budaijiezoufree", "不带节奏(目不能追，耳未可及)", cost, this::effect_budaijiezou, 3, 1);
    }
    public Card budaijiezou(){
        return new Card("budaijiezou", "不带节奏(目不能追，耳未可及)", 2, this::effect_budaijiezou, 3, 0);
    }
    public Card PoliferatingG_Card(CardDeck deck, Mechanics.Logger logger){
        return new Card("ProliferatingG", "增殖的G", 0,
                (player, enemy) -> effect_ProliferatingG(player, enemy, deck, logger), 3, 1);
    }
    public Card comboshoot(){
        return new Card("ComboShoot", "连续射击", 2, this::effect_comboshoot, 2, 0);
    }
    public Card disposal(){
        return new Card("Disposal", "处置", 3, this::effect_disposal, 3, 0);
    }
    public Card EGO_RedBeauty_Card(CardDeck deck, Mechanics.Logger logger){
        return new Card("EGO_RedBeauty", "劣化侵蚀E.G.O:红艳煞", 0,
                (player, enemy) -> effect_EGO_RedBeauty(player, enemy, deck, logger), 3, 0);
    }
    public Card WordPower_Death_Card(){
        return new Card("WordPower_Death", "言灵【死】[测试用]", -1, this::effect_WordOfPower_Death, 4, 1);
    }
    public Card RendSpaceCard(CardDeck deck, Mechanics.Logger logger){
        return new Card("RendSpace", "空间斩", 1,
                (player, enemy) -> effect_RendSpace(player, enemy, deck, logger), 3, 0);
    }
    public Card RendSpaceYuanCard(CardDeck deck, Mechanics.Logger logger){
        return new Card("RendSpaceYuan", "空间斩[缘]", 3,
                (player, enemy) -> effect_RendSpace_Yuan(player, enemy, deck, logger), 0, 1);
    }
    public Card NoselfCard(){
        return new Card("Noself", "无我", 0, this::curse_NoSelf, 0, 1);
    }
    public Card afterImageStepCard(CardDeck deck, Mechanics.Logger logger){
        return new Card("afterimage_step", "残像步", 0,
                (player, enemy) -> effectAfterImageStep(player, enemy, deck, logger), 3, 1);
    }
    public Card strikeCard() {
        return new Card("strike", "斩击", 1, this::effectStrike, 1, 0);
    }

    public Card defendCard() {
        return new Card("defend", "防御", 1, this::effectDefend, 1, 0);
    }

    public Card burningCurseCard() {
        return new Card("burning_curse", "灼烧(诅咒)", 0, this::curseBurning, -1, 1);
    }

    public Card jishixingleCard() {
        return new Card("jishixingle", "及…及时行乐", 2, this::effectJishixingle, 2, 0);
    }

    public Card swordFlashingCard() {
        return new Card("swordflashing", "森罗火象", 2, this::effectSwordflashing, 3, 0);
    }

    public Card beheadingCard() {
        return new Card("beheading", "斩首", 1, this::effectBeheading, 1, 0);
    }

    public Card memorialProcessionCard() {
        return new Card("memorial_procession", "追悼游行", 2, this::effectMemorialProcession, 2, 0);
    }

    public Card inevitableSlashCard(CardDeck deck, Mechanics.Logger logger) {
        return new Card("inevitable_slash", "必然杀", 1,
                (player, enemy) -> effectInevitableSlash(player, enemy, deck, logger), 2, 0);
    }

    /** 所有卡牌的key列表，局外养成界面展示用 */
    public static final String[] ALL_CARD_KEYS = {
            "strike", "defend", "burning_curse", "jishixingle", "afterimage_step", "RendSpace",
            "swordflashing", "beheading", "memorial_procession", "inevitable_slash", "RendSpaceYuan",
            "Noself", "WordPower_Death", "EGO_RedBeauty", "ComboShoot", "Disposal", "ProliferatingG",
            "budaijiezou", "budaijiezoufree", "@AllHeiShou", "shankailaozizijilai", "Purify", "wudile",
            "SeedofLight", "CollectLight", "Tiphereth", "setDanger", "cogito", "300Lunacy", "ququ", "Thunder",
            "CurrentGeneration", "CalmBrain", "rainbow", "charge", "sixthSense", "EGOMagicBullet", "Si"
    };

    /** 战斗掉落奖励池：诅咒牌不算奖励，排除在外 */
    public static final String[] REWARD_POOL_KEYS = {
            "strike", "defend", "jishixingle",
            "swordflashing", "beheading", "memorial_procession", "inevitable_slash", "afterimage_step"
            , "RendSpace", "EGO_RedBeauty", "ComboShoot", "Disposal", "ProliferatingG", "budaijiezou",
            "@AllHeiShou", "beheading", "Purify", "wudile", "SeedofLight", "CollectLight", "Tiphereth",
            "cogito", "300Lunacy", "ququ", "Thunder", "CurrentGeneration", "CalmBrain", "rainbow", "charge",
            "sixthSense", "EGOMagicBullet", "Si"
    };

    /** 卡牌稀有度查表（对应各Card工厂方法里设的rareness：1普通/2稀有/3金卡），用于按危险度加权掉落 */
    public static int rarenessOf(String key) {
        if (key == null) return 1;
        switch (key) {
            case "WordPower_Death":
            case "setDanger":
                return 4;
            case "Purify":
            case "cogito":
            case "SeedofLight":
            case "swordflashing":
            case "afterimage_step":
            case "kaidao":
            case "EGO_RedBeauty":
            case "RendSpace":
            case "EGOMagicBullet":
            case "ququ":
            case "@AllHeiShou":
            case "shankailaozizijilai":
            case "Tiphereth":
            case "CurrentGeneration":
            case "Disposal":
            case "ProliferatingG":
            case "Si":
            case "budaijiezou":
            case "budaijiezoufree":
                return 3;
            case "ComboShoot":
            case "inevitable_slash":
            case "jishixingle":
            case "sixthSense":
            case "rainbow":
            case "charge":
            case "wudile":
            case "CalmBrain":
            case "memorial_procession":
            case "300Lunacy":
                return 2;
            default:
                return 1;
        }
    }

    /** 按key还原出Card对象——存档恢复、随机奖励池抽卡都靠这个 */
    public Card createByKey(String key, CardDeck deck, Mechanics.Logger logger) {
        if (key == null) return strikeCard();
        switch (key) {
            case "strike": return strikeCard();
            case "cogito": return Cogito(deck, logger);
            case "defend": return defendCard();
            case "burning_curse": return burningCurseCard();
            case "jishixingle": return jishixingleCard();
            case "swordflashing": return swordFlashingCard();
            case "setDanger": return setDanger();
            case "beheading": return beheadingCard();
            case "Purify": return Purify();
            case "SeedofLight": return SeedofLightCard();
            case "memorial_procession": return memorialProcessionCard();
            case "inevitable_slash": return inevitableSlashCard(deck, logger);
            case "afterimage_step": return afterImageStepCard(deck, logger);
            case "RendSpace": return RendSpaceCard(deck, logger);
            case "WordPower_Death": return WordPower_Death_Card();
            case "EGO_RedBeauty": return EGO_RedBeauty_Card(deck, logger);
            case "ComboShoot": return comboshoot();
            case "Disposal": return disposal();
            case "ProliferatingG": return PoliferatingG_Card(deck, logger);
            case "budaijiezou": return budaijiezou();
            case "budaijiezoufree": return budaijiezou(0);
            case "@AllHeiShou": return AnswerMeAllHeiShouPack_Card(deck, logger);
            case "kaidao": return kaidao_Card();
            case "shankailaozizijilai": return shankailaozizijilai();
            case "wudile": return wudile();
            case "CollectLight": return CollectLight();
            case "Tiphereth": return Tiphereth();
            case "300Lunacy": return _300Lunacy();
            case "zhuizhui": return zhuizhui();
            case "ququ": return ququ();
            case "CurrentGeneration": return CurrentGeneration(deck, logger);
            case "Thunder": return Thunder();
            case "CalmBrain": return CalmBrain();
            case "rainbow": return rainbow();
            case "charge": return charge();
            case "sixthSense": return sixthSense();
            case "EGOMagicBullet": return EGOMagicBullet();
            case "Si": return Si();
            default: return strikeCard();
        }
    }

    /** 卡牌中文展示名，局外养成界面用，不用临时new一张卡再取name */
    public static String displayName(String key) {
        switch (key) {
            case "strike": return "斩击";
            case "defend": return "防御";
            case "burning_curse": return "灼烧诅咒";
            case "jishixingle": return "及时行乐";
            case "swordflashing": return "森罗火象";
            case "beheading": return "斩首";
            case "memorial_procession": return "追悼游行";
            case "inevitable_slash": return "必然杀";
            case "afterimage_step": return "残像步";
            case "RendSpace": return "空间斩";
            case "WordPower_Death": return "言灵【死】[测试用]";
            case "EGO_RedBeauty": return "劣化侵蚀E.G.O:红艳煞";
            case "ComboShoot": return "连续射击";
            case "Disposal": return "处置";
            case "ProliferatingG": return "增殖的G";
            case "budaijiezou":
            case "budaijiezoufree": return "不带节奏(目不能追，耳未可及)";
            case "kaidao": return "开·道(开辟君主之道吧！)";
            case "CollectLight": return "收集光芒";
            case "wudile": return "无敌了无敌了！";
            case "@AllHeiShou": return "@全体黑兽 ，收到扣三技能";
            case "Purify": return "净化!";
            case "cogito": return "Cogito";
            case "Tiphereth": return "存在意义的憧憬";
            case "SeedofLight": return "光之种计划";
            case "setDanger": return "设置危险度";
            case "300Lunacy": return "小金の朋友费";
            case "zhuizhui": return "既见慈悲，为何不笑？";
            case "ququ": return "艾兰市区";
            case "Thunder": return "雷动";
            case "CalmBrain": return "冷静头脑";
            case "CurrentGeneration": return "电流相生";
            case "rainbow": return "虹彩";
            case "charge": return "充电";
            case "sixthSense": return "第六感";
            case "EGOMagicBullet": return "脑叶公司E.G.O::魔弹";
            case "Si": return "疯狂の蛇(绝命巳乱)";
            default: return key;
        }
    }

    public static boolean ifExists(String key){
        if(displayName(key).equals(key)){
            return false;
        }
        return true;
    }
}