package com.six.fortuna.combat.engine;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.res.Resources;
import android.widget.EditText;
import android.widget.Toast;

import com.six.fortuna.R;
import com.six.fortuna.StatsActivity;
import com.six.fortuna.combat.engine.CardTypes.Card;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

import java.util.Arrays;

/**
 * 原样搬运 cards.c 里目前完整可见的卡牌效果函数。
 * cards.c 里还有一批被截断没看到全文的效果（RendSpace/TripleSlash/heavyArmor/firebath/
 * Disposal/ignite/curse_slime/curse_heaviness/curse_NoSelf/GuGuGaGa/Reqiuem等），
 * 那些等你发我完整代码或者要用到的时候再补，没看到的我不瞎编。
 */
public class FortunaCards {

    private final Mechanics m;
    private final StatsActivity s;
    private final Resources res;
    private static final int MAX_LOOP = 50;

    public void chargeCycle(Entity self, int cycleLevel) {
        while (self.charge_consume >= cycleLevel) {
            self.charge_consume -= cycleLevel;
            self.charge_strength++;
        }
    }

    public FortunaCards(Mechanics mechanics, StatsActivity s) {
        this.m = mechanics;
        this.s = s;
        this.res = s.getResources();
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
            Toast.makeText(s, res.getString(R.string.toast_yan_trigger_2light), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(s, res.getString(R.string.toast_yan_trigger_1light), LENGTH_SHORT).show();
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

    public void effectJishixingle(Entity player, Entity enemy) {
        if (player.charge_term >= 4) {
            player.charge_consume += 4;
            player.charge_term -= 4;
            chargeCycle(player, 10);
        }
        m.dealDamage(m.randint(1, 12), enemy, player);
        enemy.stagger_panic_term++;
    }

    public void effectAfterImageStep(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        m.sidestep(8, player);
        if (enemy.blade >= 60) {
            enemy.blade -= 8;
            deck.addCard(afterImageStepCard(deck, logger), logger);
        }
    }

    public void effect_RendSpace(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        enemy.blade += 12 + player.poise_strength / 10 + enemy.blade / 10;
        if (enemy.blade >= 70) {
            deck.addCard(RendSpaceYuanCard(deck, logger), logger);
            if (player.poise_strength < 20) {
                player.poise_strength = 20;
                if (player.poise_term <= 0) {
                    player.poise_term = 2;
                }
            }
        }
    }

    public void effect_RendSpace_Yuan(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        m.dealDamage((int) (enemy.max_hp * 0.01 * enemy.blade), enemy, player);
        if (enemy.hp <= 0) {
            enemy.hp = 1;
            enemy.burn_term += 10;
            enemy.burn_strength += 100;
        }
        for (int i = 0; i < 8; i++) {
            curse_NoSelf(player, enemy);
        }
    }

    public void effect_EGO_RedBeauty(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        enemy.burn_term += 2;
        enemy.burn_strength += 3 * player.energy;
        for (int i = 0; i < player.energy; i++) {
            enemy.burn_term += 2;
            enemy.burn_strength *= 1.1;
            m.burn(enemy);
        }
        if (player.energy < 4) {
            for (int i = 0; i < 4 - player.energy; i++) {
                deck.addCard(burningCurseCard(), logger);
            }
            if (player.energy == 0) {
                player.hp *= 0.75;
            }
        }
        player.energy = 0;
    }

    public void effect_WordOfPower_Death(Entity player, Entity enemy) {
        m.dealDamage((int) ((enemy.block + enemy.max_hp) * 1.5), enemy, player);
        enemy.hp = -1;
    }

    public void effect_comboshoot(Entity player, Entity enemy) {
        player.poise_term++;
        if (player.ammo > 0 && player.ammoType != 3) {
            player.ammo--;
            m.dealDamage(8, enemy, player);
            effect_comboshoot(player, enemy);
            player.poise_strength += 3;
            player.poise_term++;
        }
        if (player.ammoType == 3) {
            for (int i = 0; i < 7; i++) {
                m.dealDamage(16, enemy, player);
            }
        }
    }

    public void curse_NoSelf(Entity player, Entity enemy) {
        player.max_hp *= 0.9;
    }

    public void effect_disposal(Entity player, Entity enemy) {
        effect_comboshoot(player, enemy);
        player.totalAmmo = 10;
        player.ammoType = 1;
        player.reload();
        player.consumeAmmo(10);
        m.amplitudeConversion(enemy, 1); //震颤：灼热
        for (int i = 0; i < 5; i++) {
            m.dealDamage(5, enemy, player);
            enemy.tremor_strength += 7;
            enemy.tremor_term += 4;
            enemy.burn_strength += 3;
            enemy.burn_term += 4;
            player.reload();
            player.consumeAmmo(10);
        }
        for (int i = 0; i < 5; i++) m.tremorBurst(enemy);
    }

    public void randomStrength(Entity player, Entity enemy) {
        switch (m.randint(1, 6)) {
            case 1:
                enemy.burn_strength += 3;
                break;
            case 2:
                enemy.rapture_strength += 3;
                break;
            case 3:
                enemy.sinking_strength += 3;
                break;
            case 4:
                enemy.tremor_strength += 3;
                break;
            case 5:
                enemy.bleed_strength += 3;
                break;
            case 6:
                player.poise_strength += 3;
                break;
        }
    }

    public void randomTerm(Entity player, Entity enemy) {
        switch (m.randint(1, 6)) {
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

    public void randomTrigger(Entity enemy) {
        switch (m.randint(1, 5)) {
            case 1:
                m.burn(enemy);
                break;
            case 2:
                enemy.hp -= enemy.rapture_strength;
                enemy.rapture_term--;
                break;
            case 3:
                m.sinking(enemy);
                break;
            case 4:
                m.amplitudeConversion(enemy, m.randint(1, 3));
                m.tremorBurst(enemy);
                break;
            case 5:
                m.bleedActivate(enemy);
                break;
        }
    }

    public void TermforTooMush(Entity enemy, Entity player, int loop) {
        player.poise_term += 2 * loop / 6;
        enemy.rapture_term += 2 * loop / 6;
        enemy.sinking_term += 2 * loop / 6;
        enemy.tremor_term += 2 * loop / 6;
        enemy.burn_term += 2 * loop / 6;
        enemy.bleed_term += 2 * loop / 6;
    }

    public void StrengthforTooMush(Entity enemy, Entity player, int loop) {
        player.poise_strength += 3 * loop / 6;
        enemy.rapture_strength += 3 * loop / 6;
        enemy.sinking_strength += 3 * loop / 6;
        enemy.tremor_strength += 3 * loop / 6;
        enemy.burn_strength += 3 * loop / 6;
        enemy.bleed_strength += 3 * loop / 6;
    }

    public void TriggerforTooMuch(Entity enemy, Entity player, int loop) {
        int temp = enemy.buff.sidestep;
        int tempStrength = enemy.poise_strength;
        int tempTerm = enemy.poise_term;
        enemy.poise_strength = 0;
        enemy.poise_term = 0;
        enemy.buff.sidestep = 0;
        m.dealDamage(enemy.rapture_strength * loop / 5 - enemy.strength, enemy, enemy);
        enemy.rapture_term -= loop / 5;
        enemy.sanity -= enemy.sinking_strength * loop / 5;
        if (enemy.sanity < -45) enemy.hp -= (enemy.sanity + 45) * -1;
        m.sanityReturn(enemy);
        enemy.sinking_term -= loop / 5;
        m.amplitudeConversion(enemy, 1);
        enemy.burn_strength += (int) (enemy.tremor_strength * 0.01) / (loop / 15);
        enemy.burn_term += (int) (enemy.tremor_term * 0.3) / (loop / 15);
        m.dealDamage(enemy.burn_strength * loop / 15 - enemy.strength, enemy, enemy);
        enemy.burn_term -= loop / 15;
        m.amplitudeConversion(enemy, 2);
        enemy.sinking_term++;
        if (enemy.tremor_strength > 500) {
            enemy.sinking_strength += 5 * (loop / 15);
        } else {
            enemy.sinking_strength += (int) (enemy.tremor_strength * 0.01) * (loop / 15);
        }
        enemy.sanity -= enemy.sinking_strength * (loop / 15);
        if (enemy.sanity < -45) enemy.hp -= (enemy.sanity + 45) * -1;
        m.sanityReturn(enemy);
        m.amplitudeConversion(enemy, 3);
        enemy.this_turn_strength -= (int) (enemy.tremor_strength * 0.03 + 1) * (loop / 15);
        enemy.tremor_term -= loop / 5;
        m.dealDamage(enemy.burn_strength * loop / 5 - enemy.strength, enemy, enemy);
        enemy.burn_term -= loop / 5;
        m.dealDamage(enemy.bleed_strength * loop / 5 - enemy.strength, enemy, enemy);
        enemy.bleed_term -= loop / 5;
        enemy.buff.sidestep = temp;
        enemy.poise_strength = tempStrength;
        enemy.poise_term = tempTerm;
    }

    public void effect_ProliferatingG(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        //G公司来的
        if (player.energy <= 0) {
            m.dealDamage(8, enemy, player);
            m.defend(8, player);
            return;
        }
        player.energy--;
        for (int i = 0; i < 4 + (deck.drawPile.size() + deck.discardPile.size()) / 75; i++) {
            deck.discardPile.add(PoliferatingG_Card(deck, logger));
        }

        for (int i = 0; i < 7 + (deck.drawPile.size() + deck.discardPile.size()) / 10; i++) {
            if (7 + (deck.drawPile.size() + deck.discardPile.size()) / 10 > MAX_LOOP) {
                TermforTooMush(enemy, player, 7 + (deck.drawPile.size() + deck.discardPile.size()) / 10);
                break;
            }
            randomTerm(player, enemy);
        }
        for (int i = 0; i < 12 + (deck.drawPile.size() + deck.discardPile.size()) / 7; i++) {
            if (12 + (deck.drawPile.size() + deck.discardPile.size()) / 7 > MAX_LOOP) {
                StrengthforTooMush(enemy, player, 12 + (deck.drawPile.size() + deck.discardPile.size()) / 7);
                break;
            }
            randomStrength(player, enemy);
        }
        for (int i = 0; i < 3 + (deck.discardPile.size() + deck.drawPile.size()) / 150; i++) {
            if (3 + (deck.discardPile.size() + deck.drawPile.size()) / 150 > MAX_LOOP) {
                TriggerforTooMuch(enemy, player, 3 + (deck.discardPile.size() + deck.drawPile.size()) / 150);
                break;
            }
            randomTrigger(enemy);
        }
        m.dealDamage(8 + (deck.drawPile.size() + deck.discardPile.size()) / 33, enemy, player);
        m.defend(8 + (deck.drawPile.size() + deck.discardPile.size()) / 33, player);
        deck.drawCard(player, logger);
    }

    public void effect_AnswerMeAllHeiShouPack(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        enemy.rapture_strength += 5;
        enemy.rapture_term += 2;
        m.dealDamage(10, enemy, player);
        player.poise_term += 2;
        new Builder(s)
                .setTitle(res.getString(R.string.dialog_heishou_title))
                .setMessage(res.getString(R.string.dialog_heishou_message))
                .setPositiveButton(res.getString(R.string.dialog_heishou_positive), (dialog, which) -> {
                    switch (m.randint(1, 2)) {
                        case 1:
                            deck.addCard(budaijiezou(deck, logger, 0), logger);
                            break;
                        case 2:
                            Card sCard = Si();
                            sCard.cost = 0;
                            sCard.consumption = 1;
                            deck.addCard(sCard, logger);
                            break;
                        default:
                            deck.addCard(budaijiezou(deck, logger, 0), logger);
                    }
                    deck.addCard(kaidao_Card(), logger);
                    s.runOnUiThread(() -> {
                        s.updateSpinner();
                        s.refreshAllUI();
                    });
                })
                .setNegativeButton(res.getString(R.string.dialog_heishou_negative), ((dialog, which) -> {
                    deck.addCard(shankailaozizijilai(), logger);
                    s.runOnUiThread(() -> {
                        s.updateSpinner();
                        s.refreshAllUI();
                    });
                }))
                .setCancelable(false)
                .show();
    }

    public void effect_kaidao(Entity player, Entity enemy) {
        for (int i = 0; i < 2; i++) {
            enemy.rapture_term += 2;
            enemy.rapture_strength += 3;
            player.poise_strength += 4;
            player.poise_term++;
            m.dealDamage(8, enemy, player);
        }
    }

    public void effect_shankailaozizijilai(Entity player, Entity enemy) {
        double k = 0.7;
        for (int i = 0; i < 4; i++) {
            enemy.rapture_strength += 21;
            enemy.rapture_term += 10;
            player.poise_term += 3;
            player.poise_strength += 5;
            if (player.hp <= player.max_hp * k) {
                k -= 0.2;
                i--;
            }
            m.dealDamage(18, enemy, player);
        }
    }

    public void effect_budaijiezou(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        for (int i = 0; i < (player.swift >= 10 ? 4 : 3); i++) {
            player.this_turn_strength += 3;
            enemy.rapture_term += 4;
            enemy.rapture_strength += 1 + player.swift / 5 + player.this_turn_strength / 3 + player.tianjiustarblade / 10;
        }
        player.swift += 5;
        if (player.swift >= 10) {
            player.tianjiustarblade += 10 + player.swift / 15;
        }
        if (player.tianjiustarblade >= 75) {
            player.tianjiustarblade -= 75;
            deck.addCard(xiangxinwuhunzhenshen(), logger);
        }
    }

    public void effect_wudile(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        for (int i = 0; i < 2; i++) m.defend(1, player);
        Toast.makeText(s, res.getString(R.string.toast_wudile), LENGTH_SHORT).show();
        if (player.tianjiustarblade >= 75) {
            player.tianjiustarblade -= 75;
            deck.addCard(xiangxinwuhunzhenshen(), logger);
        } else {
            player.swift += 5;
            player.tianjiustarblade += 2;
        }
    }

    public void effect_xiangxinwuhunzhenshen(Entity player, Entity enemy) {
        Toast.makeText(s, res.getString(R.string.toast_xiangxin), LENGTH_SHORT).show();
        player.tianjiustarblade += 75;
        player.this_turn_strength += player.swift / 1.5;
        enemy.rapture_strength += 5 + player.swift / 2 + player.this_turn_strength / 3 + player.tianjiustarblade / 7;
        enemy.rapture_term += 3 + player.tianjiustarblade / 5;
        player.this_turn_strength += enemy.rapture_term;
        enemy.hp -= enemy.rapture_strength * (enemy.rapture_term - 1);
        enemy.rapture_term = enemy.rapture_strength;
        enemy.rapture_strength = player.tianjiustarblade;
        player.tianjiustarblade = 0;
    }

    public void effect_Purify(Entity player, Entity enemy) {
        for (int i = 0; i < 3; i++) {
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
        if (player.swift < 0) player.swift = 0;
        if (player.strength < 0) player.strength = 0;
        player.restrictions = 0;
        player.buff.reset_Debuff();
        if (enemy.swift > 0) enemy.swift = 0;
        if (enemy.strength > 0) enemy.strength = 0;
        enemy.poise_strength = 0;
        enemy.poise_term = 0;
        enemy.charge_term = 0;
        enemy.charge_strength = 1;
        enemy.buff.reset_Buff();
        new android.app.AlertDialog.Builder(s)
                .setTitle(res.getString(R.string.dialog_lock_title))
                .setMessage(res.getString(R.string.dialog_lock_message))
                .setCancelable(false)
                .setPositiveButton(res.getString(R.string.dialog_lock_positive), (dialog, which) -> {
                    enemy.buff.lockedHealth = 0;
                    Toast.makeText(s, res.getString(R.string.toast_lock_yes), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(res.getString(R.string.dialog_lock_negative), (dialog, which) -> {
                    Toast.makeText(s, res.getString(R.string.toast_lock_no), LENGTH_SHORT).show();
                })
                .show();
    }

    public void SeedofLight(Entity player, Entity enemy) {
        //光之种！
        player.buff.SeedofLight += 1;
        while (player.energy >= 2) {
            player.buff.SeedofLight++;
            player.energy -= 2;
        }
    }

    public void CollectLight(Entity player, Entity enemy) {
        player.energy += 2;
        if (player.buff.SeedofLight > 0) {
            player.energy++;
        }
        //消耗，获得2光芒
    }

    public void effect_Tiphereth(Entity player, Entity enemy) {
        //存在意义的憧憬
        player.buff.Tiphereth++;
        new AlertDialog.Builder(s)
                .setMessage(res.getString(R.string.dialog_tiphereth_message))
                .setPositiveButton(res.getString(R.string.dialog_tiphereth_button), null)
                .setNegativeButton(res.getString(R.string.dialog_tiphereth_button), null)
                .setNeutralButton(res.getString(R.string.dialog_tiphereth_button), null)
                .show();
    }

    public void effect_setDanger(Entity player, Entity enemy) {
        final EditText input = new EditText(s);
        input.setHint(res.getString(R.string.dialog_setdanger_message));
        new android.app.AlertDialog.Builder(s)
                .setTitle(res.getString(R.string.dialog_setdanger_title))
                .setMessage(res.getString(R.string.dialog_setdanger_message))
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(res.getString(R.string.dialog_setdanger_positive), (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    try {
                        player.difficulty = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        Toast.makeText(s, res.getString(R.string.toast_setdanger_error), Toast.LENGTH_SHORT).show();
                    }
                    s.updateSpinner();
                    s.refreshAllUI();
                }).show();
    }

    public void effect_cogito(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        Card i = createByKey(ALL_CARD_KEYS[m.randint(0, ALL_CARD_KEYS.length - 1)], deck, logger);
        i.cost = 0;
        i.consumption = 1;
        deck.addCard(i, logger);
        deck.drawCard(player, logger);
    }

    public void effect_300lunacy(Entity player, Entity enemy) {
        enemy.current_intent = new EnemyAction(res.getString(R.string.card_name_heishou), null); // 临时借用
        Toast.makeText(s, res.getString(R.string.toast_300lunacy), Toast.LENGTH_LONG).show();
    }

    public void effect_ququ(Entity player, Entity enemy) {
        //自我吟诵
        player.buff.cibei++;
    }

    public void effect_zhuizhui(Entity player, Entity enemy) {
        if (m.dealDamage(8, enemy, player)) {
            enemy.bleed_term++;
            enemy.bleed_strength += 2;
            enemy.tremor_strength += 2;
            enemy.tremor_term++;
            m.tremorBurst(enemy);
        }
    }

    public void effect_Thunder(Entity player, Entity enemy) {
        for (int i = 0; i < 2; i++) player.buff.addChargeBalls(1, enemy);
    }

    public void effect_CurrentGeneration(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        int k = player.buff.currentgeneration;
        for (int i = 0; i < k; i++) {
            if (k >= 50) {
                for (int j = 0; j < player.buff.chargeBallSize; j++) player.buff.addChargeBalls(1, enemy);
                player.buff.ballDamage((k - player.buff.chargeBallSize) * (18 + 3 * (player.buff.concentration) - player.buff.concentration), player, enemy);
                player.buff.currentgeneration += k;
                s.appendLog(String.format(res.getString(R.string.log_currentgeneration), player.buff.currentgeneration));
                break;
            }
            player.buff.addChargeBalls(1, enemy);
        }
        Card a = CurrentGeneration(deck, logger);
        if (player.buff.currentgeneration < 50) {
            a.name = res.getString(R.string.card_name_currentgeneration) + " (等我启动)";
            deck.discardPile.add(a);
        } else {
            a.name = res.getString(R.string.card_name_currentgeneration) + " (我已启动)";
            deck.discardPile.add(a);
        }
    }

    public void effect_calmBrain(Entity player, Entity enemy) {
        player.buff.concentration += 5;
        player.buff.addChargeBalls(2, enemy);
        player.buff.nextTurnLight++;
    }

    public void effect_rainbow(Entity player, Entity enemy) {
        for (int i = 1; i <= 3; i++) player.buff.addChargeBalls(i, enemy);
        player.buff.concentration += 5;
    }

    public void effect_charge(Entity player, Entity enemy) {
        if (player.charge_strength <= 3) {
            player.charge_term += 15;
            player.charge_consume += player.charge_term;
            player.charge_term = 0;
            chargeCycle(player, 8);
        } else if (player.charge_strength <= 5) {
            player.charge_term += 7;
            player.charge_consume += player.charge_term;
            player.charge_term = 0;
            chargeCycle(player, 8);
            player.energy += 2;
        } else {
            player.buff.addChargeBalls(3, enemy);
        }
    }

    public void effect_sixthSense(Entity player, Entity enemy) {
        player.buff.sidestep += 12 + player.swift;
        if (player.swift < 0) player.swift = 0;
        player.swift += (player.max_hp - player.hp) / (player.max_hp / 20);
    }

    public void effect_EGOMagicBullet(Entity player, Entity enemy) {
        if (player.ammoType != 3) {
            player.ammoType = 3;
            player.totalAmmo = 7;
            player.reload();
        }
    }

    public void effect_Si(Entity player, Entity enemy) {
        int k = 100;
        int j = 5;
        for (int i = 0; i < 3; i++) {
            if (m.randint(1, k) < player.poise_strength * 5) {
                i--;
                k += j;
                j *= j;
            }
            player.poise_term++;
            if (m.dealDamage(9, enemy, player)) {
                enemy.rapture_strength += 5;
                enemy.rapture_term += 3;
                enemy.buff.poison += enemy.rapture_strength + enemy.rapture_term * 2;
            }
        }
    }

    public void effect_littleBird(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        Toast.makeText(s, res.getString(R.string.toast_littlebird_intro), LENGTH_SHORT).show();
        if (player.buff.bigbird == 1 && player.buff.tallbird == 1) {
            switch (m.randint(0, 2)) {
                case 0:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_1), LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_2), LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_3), LENGTH_SHORT).show();
            }
            deck.addCard(BigMonster(deck, logger), logger);
            player.buff.tallbird = 0;
            player.buff.bigbird = 0;
            player.buff.Apocalypse_Bird++;
        } else {
            player.buff.littlebird = 1;
        }
    }

    public void effect_tallBird(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        Toast.makeText(s, res.getString(R.string.toast_tallbird_intro), LENGTH_SHORT).show();
        if (player.buff.littlebird == 1 && player.buff.bigbird == 1) {
            switch (m.randint(0, 2)) {
                case 0:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_1), LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_2), LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_3), LENGTH_SHORT).show();
            }
            deck.addCard(BigMonster(deck, logger), logger);
            player.buff.littlebird = 0;
            player.buff.bigbird = 0;
            player.buff.Apocalypse_Bird++;
        } else {
            enemy.buff.reset_Buff();
            player.buff.tallbird = 1;
        }
    }

    public void effect_bigBird(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        Toast.makeText(s, res.getString(R.string.toast_bigbird_intro), LENGTH_SHORT).show();
        if (player.buff.littlebird == 1 && player.buff.tallbird == 1) {
            switch (m.randint(0, 2)) {
                case 0:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_1), LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_2), LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(s, res.getString(R.string.toast_threebirds_3), LENGTH_SHORT).show();
            }
            deck.addCard(BigMonster(deck, logger), logger);
            player.buff.littlebird = 0;
            player.buff.tallbird = 0;
            player.buff.Apocalypse_Bird++;
        } else {
            player.staggerLine = new double[]{0.0, 0.0, 0.0};
            player.buff.bigbird = 1;
        }
    }

    public void effect_BigMonster(Entity player, Entity enemy, CardDeck deck, Mechanics.Logger logger) {
        deck.drawCard(player, logger);
        Card BigMonster = BigMonster(deck, logger);
        deck.drawPile.add(BigMonster);
        if (m.dealDamage(600, enemy, player)) {
            for (int i = 0; i < 12 + 2 * player.buff.Apocalypse_Bird; i++) {
                if (12 + 2 * player.buff.Apocalypse_Bird > MAX_LOOP) {
                    TermforTooMush(enemy, player, 12 + 2 * player.buff.Apocalypse_Bird);
                    break;
                }
                randomTerm(player, enemy);
            }
            for (int i = 0; i < 12 + 2 * player.buff.Apocalypse_Bird; i++) {
                if (12 + 2 * player.buff.Apocalypse_Bird > MAX_LOOP) {
                    StrengthforTooMush(enemy, player, 12 + 2 * player.buff.Apocalypse_Bird);
                    break;
                }
                randomStrength(player, enemy);
            }
            for (int i = 0; i < 12 + 2 * player.buff.Apocalypse_Bird; i++) {
                if (12 + 2 * player.buff.Apocalypse_Bird > MAX_LOOP) {
                    TriggerforTooMuch(enemy, player, 12 + 2 * player.buff.Apocalypse_Bird);
                    break;
                }
                randomTrigger(enemy);
            }
            player.buff.Apocalypse_Bird++;
        }
    }

    public void effect_BloodstainedTears(Entity player, Entity enemy) {
        player.buff.lockedHealth++;
        player.buff.bloodstainedTears++;
    }

    public void effect_BloodstainedTears_Finale(Entity player, Entity enemy) {
        int a = 0;
        int d = player.buff.bloodstainedTears;
        player.this_turn_strength += 20;
        enemy.buff.sidestep = 0;
        enemy.block = 0;
        for (int i = 0; i < d; i++) {
            a += a + 1;
            player.buff.bloodstainedTears++;
            if (m.dealDamage(80, enemy, player)) {
                enemy.rapture_strength += Math.min((player.this_turn_strength + player.strength) * (player.buff.bloodstainedTears - 2), 150);
                enemy.rapture_term += 2 * player.buff.bloodstainedTears;
            }
        }
        if (m.dealDamage(200, enemy, player)) {
            enemy.hp -= enemy.rapture_strength * (enemy.rapture_term - 5);
            enemy.rapture_term = 5;
        }
        player.buff.bloodstainedTears = 3;
    }


    // --- 构造出实际的Card对象（对应 cards.c 底部的全局卡牌定义）---
    public Card BloodstainedTears() {
        return new Card("bloodstainedtears", res.getString(R.string.card_name_bloodstainedtears_start), 0, this::effect_BloodstainedTears, 3, 1);
    }

    public Card BloodstainedTears_finale() {
        return new Card("bloodstainedtearsfinale", res.getString(R.string.card_name_bloodstainedtears_finale), 0, this::effect_BloodstainedTears_Finale, 3, 1);
    }

    public Card xiangxinwuhunzhenshen() {
        return new Card("xiangxinwuhunzhenshen", res.getString(R.string.card_name_xiangxin), 0, this::effect_xiangxinwuhunzhenshen, 3, 1);
    }

    public Card littleBird(CardDeck deck, Mechanics.Logger logger) {
        return new Card("littleBird", res.getString(R.string.card_name_littlebird), 1, (player, enemy) -> effect_littleBird(player, enemy, deck, logger), 3, 1);
    }

    public Card tallBird(CardDeck deck, Mechanics.Logger logger) {
        return new Card("tallBird", res.getString(R.string.card_name_tallbird), 2, (player, enemy) -> effect_tallBird(player, enemy, deck, logger), 3, 1);
    }

    public Card bigBird(CardDeck deck, Mechanics.Logger logger) {
        return new Card("bigBird", res.getString(R.string.card_name_bigbird), 3, (player, enemy) -> effect_bigBird(player, enemy, deck, logger), 3, 1);
    }

    public Card BigMonster(CardDeck deck, Mechanics.Logger logger) {
        return new Card("BigMonster", res.getString(R.string.card_name_bigmonster), 7, (player, enemy) -> effect_BigMonster(player, enemy, deck, logger), 3, 1);
    }

    public Card Si() {
        return new Card("Si", res.getString(R.string.card_name_si), 2, this::effect_Si, 3, 0);
    }

    public Card EGOMagicBullet() {
        return new Card("EGOMagicBullet", res.getString(R.string.card_name_egomagicbullet), 0, this::effect_EGOMagicBullet, 3, 1);
    }

    public Card sixthSense() {
        return new Card("sixthSense", res.getString(R.string.card_name_sixthsense), 1, this::effect_sixthSense, 2, 0);
    }

    public Card charge() {
        return new Card("charge", res.getString(R.string.card_name_charge), 1, this::effect_charge, 2, 0);
    }

    public Card rainbow() {
        return new Card("rainbow", res.getString(R.string.card_name_rainbow), 3, this::effect_rainbow, 2, 1);
    }

    public Card CalmBrain() {
        return new Card("CalmBrain", res.getString(R.string.card_name_calmbrain), 2, this::effect_calmBrain, 2, 0);
    }

    public Card CurrentGeneration(CardDeck deck, Mechanics.Logger logger) {
        return new Card("CurrentGeneration", res.getString(R.string.card_name_currentgeneration), 3, (player, enemy) -> effect_CurrentGeneration(player, enemy, deck, logger), 3, 1);
    }

    public Card Thunder() {
        return new Card("Thunder", res.getString(R.string.card_name_thunder), 1, this::effect_Thunder, 1, 0);
    }

    public Card Elanzhuizhui() {
        return new Card("zhuizhui", res.getString(R.string.card_name_zhuizhui), 0, (player, enemy) -> effect_zhuizhui(enemy, player), 1, 1);
    }

    public Card ququ() {
        return new Card("ququ", res.getString(R.string.card_name_ququ), 1, this::effect_ququ, 3, 0);
    }

    public Card zhuizhui() {
        return new Card("zhuizhui", res.getString(R.string.card_name_zhuizhui), 0, this::effect_zhuizhui, 1, 1);
    }

    public Card _300Lunacy() {
        return new Card("300Lunacy", res.getString(R.string.card_name_300lunacy), 1, this::effect_300lunacy, 2, 1);
    }

    public Card Cogito(CardDeck deck, Mechanics.Logger logger) {
        return new Card("cogito", res.getString(R.string.card_name_cogito), 1, (player, enemy) -> effect_cogito(player, enemy, deck, logger), 3, 0);
    }

    public Card setDanger() {
        return new Card("setDanger", res.getString(R.string.card_name_setdanger), -2, this::effect_setDanger, 4, 1);
    }

    public Card Tiphereth() {
        return new Card("Tiphereth", res.getString(R.string.card_name_tiphereth), 0, this::effect_Tiphereth, 3, 1);
    }

    public Card SeedofLightCard() {
        return new Card("SeedofLight", res.getString(R.string.card_name_seedoflight), 3, this::SeedofLight, 3, 1);
    }

    public Card CollectLight() {
        return new Card("CollectLight", res.getString(R.string.card_name_collectlight), 0, this::CollectLight, 1, 1);
    }

    public Card wudile(CardDeck deck, Mechanics.Logger logger) {
        return new Card("wudile", res.getString(R.string.card_name_wudile), 1, (player, enemy) -> effect_wudile(player, enemy, deck, logger), 2, 0);
    }

    public Card Purify() {
        return new Card("Purify", res.getString(R.string.card_name_purify), 3, this::effect_Purify, 3, 1);
    }

    public Card kaidao_Card() {
        return new Card("kaidao", res.getString(R.string.card_name_kaidao), 0, this::effect_kaidao, 3, 1);
    }

    public Card shankailaozizijilai() {
        return new Card("shankailaozizijilai", res.getString(R.string.card_name_shankai), 0, this::effect_shankailaozizijilai, 3, 1);
    }

    public Card AnswerMeAllHeiShouPack_Card(CardDeck deck, Mechanics.Logger logger) {
        return new Card("@AllHeiShou", res.getString(R.string.card_name_heishou), 3,
                (player, enemy) -> effect_AnswerMeAllHeiShouPack(player, enemy, deck, logger), 3, 0);
    }

    public Card budaijiezou(CardDeck deck, Mechanics.Logger logger, int cost) {
        return new Card("budaijiezoufree", res.getString(R.string.card_name_budaijiezou), cost, (player, enemy) -> effect_budaijiezou(player, enemy, deck, logger), 3, 1);
    }

    public Card budaijiezou(CardDeck deck, Mechanics.Logger logger) {
        return new Card("budaijiezou", res.getString(R.string.card_name_budaijiezou), 2, (player, enemy) -> effect_budaijiezou(player, enemy, deck, logger), 3, 0);
    }

    public Card PoliferatingG_Card(CardDeck deck, Mechanics.Logger logger) {
        return new Card("ProliferatingG", res.getString(R.string.card_name_proliferatingg), 0,
                (player, enemy) -> effect_ProliferatingG(player, enemy, deck, logger), 3, 1);
    }

    public Card comboshoot() {
        return new Card("ComboShoot", res.getString(R.string.card_name_comboshoot), 2, this::effect_comboshoot, 2, 0);
    }

    public Card disposal() {
        return new Card("Disposal", res.getString(R.string.card_name_disposal), 3, this::effect_disposal, 3, 0);
    }

    public Card EGO_RedBeauty_Card(CardDeck deck, Mechanics.Logger logger) {
        return new Card("EGO_RedBeauty", res.getString(R.string.card_name_ego_redbeauty), 0,
                (player, enemy) -> effect_EGO_RedBeauty(player, enemy, deck, logger), 3, 0);
    }

    public Card WordPower_Death_Card() {
        return new Card("WordPower_Death", res.getString(R.string.card_name_wordpower_death), -1, this::effect_WordOfPower_Death, 4, 1);
    }

    public Card RendSpaceCard(CardDeck deck, Mechanics.Logger logger) {
        return new Card("RendSpace", res.getString(R.string.card_name_rendspace), 1,
                (player, enemy) -> effect_RendSpace(player, enemy, deck, logger), 3, 0);
    }

    public Card RendSpaceYuanCard(CardDeck deck, Mechanics.Logger logger) {
        return new Card("RendSpaceYuan", res.getString(R.string.card_name_rendspace_yuan), 3,
                (player, enemy) -> effect_RendSpace_Yuan(player, enemy, deck, logger), 0, 1);
    }

    public Card NoselfCard() {
        return new Card("Noself", res.getString(R.string.card_name_noself), 0, this::curse_NoSelf, 0, 1);
    }

    public Card afterImageStepCard(CardDeck deck, Mechanics.Logger logger) {
        return new Card("afterimage_step", res.getString(R.string.card_name_afterimage_step), 0,
                (player, enemy) -> effectAfterImageStep(player, enemy, deck, logger), 3, 1);
    }

    public Card strikeCard() {
        return new Card("strike", res.getString(R.string.card_name_strike), 1, this::effectStrike, 1, 0);
    }

    public Card defendCard() {
        return new Card("defend", res.getString(R.string.card_name_defend), 1, this::effectDefend, 1, 0);
    }

    public Card burningCurseCard() {
        return new Card("burning_curse", res.getString(R.string.card_name_burning_curse), 0, this::curseBurning, -1, 1);
    }

    public Card jishixingleCard() {
        return new Card("jishixingle", res.getString(R.string.card_name_jishixingle), 2, this::effectJishixingle, 2, 0);
    }

    public Card swordFlashingCard() {
        return new Card("swordflashing", res.getString(R.string.card_name_swordflashing), 2, this::effectSwordflashing, 3, 0);
    }

    public Card beheadingCard() {
        return new Card("beheading", res.getString(R.string.card_name_beheading), 1, this::effectBeheading, 1, 0);
    }

    public Card memorialProcessionCard() {
        return new Card("memorial_procession", res.getString(R.string.card_name_memorial_procession), 2, this::effectMemorialProcession, 2, 0);
    }

    public Card inevitableSlashCard(CardDeck deck, Mechanics.Logger logger) {
        return new Card("inevitable_slash", res.getString(R.string.card_name_inevitable_slash), 1,
                (player, enemy) -> effectInevitableSlash(player, enemy, deck, logger), 2, 0);
    }

    /** 所有卡牌的key列表，局外养成界面展示用 */
    public static final String[] ALL_CARD_KEYS = {
            "strike", "defend", "burning_curse", "jishixingle", "afterimage_step", "RendSpace",
            "swordflashing", "beheading", "memorial_procession", "inevitable_slash", "RendSpaceYuan",
            "Noself", "WordPower_Death", "EGO_RedBeauty", "ComboShoot", "Disposal", "ProliferatingG",
            "budaijiezou", "budaijiezoufree", "@AllHeiShou", "shankailaozizijilai", "Purify", "wudile",
            "SeedofLight", "CollectLight", "Tiphereth", "setDanger", "cogito", "300Lunacy", "ququ", "Thunder",
            "CurrentGeneration", "CalmBrain", "rainbow", "charge", "sixthSense", "EGOMagicBullet", "Si",
            "littleBird", "tallBird", "bigBird", "BigMonster", "xiangxinwuhunzhenshen", "bloodstainedtears",
            "bloodstainedtearsfinale"
    };

    /** 战斗掉落奖励池：诅咒牌不算奖励，排除在外 */
    public static final String[] REWARD_POOL_KEYS = {
            "strike", "defend", "jishixingle",
            "swordflashing", "beheading", "memorial_procession", "inevitable_slash", "afterimage_step",
            "RendSpace", "EGO_RedBeauty", "ComboShoot", "Disposal", "ProliferatingG", "budaijiezou",
            "@AllHeiShou", "beheading", "Purify", "wudile", "SeedofLight", "CollectLight", "Tiphereth",
            "cogito", "300Lunacy", "ququ", "Thunder", "CurrentGeneration", "CalmBrain", "rainbow", "charge",
            "sixthSense", "EGOMagicBullet", "Si", "littleBird", "tallBird", "bigBird", "bloodstainedtears"
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
            case "bloodstainedtears":
            case "bloodstainedtearsfinale":
            case "SeedofLight":
            case "littleBird":
            case "tallBird":
            case "xiangxinwuhunzhenshen":
            case "bigBird":
            case "BigMonster":
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
            case "strike":
                return strikeCard();
            case "cogito":
                return Cogito(deck, logger);
            case "defend":
                return defendCard();
            case "burning_curse":
                return burningCurseCard();
            case "jishixingle":
                return jishixingleCard();
            case "swordflashing":
                return swordFlashingCard();
            case "setDanger":
                return setDanger();
            case "beheading":
                return beheadingCard();
            case "Purify":
                return Purify();
            case "SeedofLight":
                return SeedofLightCard();
            case "memorial_procession":
                return memorialProcessionCard();
            case "inevitable_slash":
                return inevitableSlashCard(deck, logger);
            case "afterimage_step":
                return afterImageStepCard(deck, logger);
            case "RendSpace":
                return RendSpaceCard(deck, logger);
            case "WordPower_Death":
                return WordPower_Death_Card();
            case "EGO_RedBeauty":
                return EGO_RedBeauty_Card(deck, logger);
            case "ComboShoot":
                return comboshoot();
            case "Disposal":
                return disposal();
            case "ProliferatingG":
                return PoliferatingG_Card(deck, logger);
            case "budaijiezou":
                return budaijiezou(deck, logger, 0);
            case "budaijiezoufree":
                return budaijiezou(deck, logger, 0);
            case "@AllHeiShou":
                return AnswerMeAllHeiShouPack_Card(deck, logger);
            case "kaidao":
                return kaidao_Card();
            case "shankailaozizijilai":
                return shankailaozizijilai();
            case "wudile":
                return wudile(deck, logger);
            case "CollectLight":
                return CollectLight();
            case "Tiphereth":
                return Tiphereth();
            case "300Lunacy":
                return _300Lunacy();
            case "zhuizhui":
                return zhuizhui();
            case "ququ":
                return ququ();
            case "CurrentGeneration":
                return CurrentGeneration(deck, logger);
            case "Thunder":
                return Thunder();
            case "CalmBrain":
                return CalmBrain();
            case "rainbow":
                return rainbow();
            case "charge":
                return charge();
            case "sixthSense":
                return sixthSense();
            case "EGOMagicBullet":
                return EGOMagicBullet();
            case "Si":
                return Si();
            case "littleBird":
                return littleBird(deck, logger);
            case "tallBird":
                return tallBird(deck, logger);
            case "bigBird":
                return bigBird(deck, logger);
            case "BigMonster":
                return BigMonster(deck, logger);
            case "bloodstainedtears":
                return BloodstainedTears();
            case "bloodstainedtearsfinale":
                return BloodstainedTears_finale();
            default:
                return strikeCard();
        }
    }

    /**
     * 卡牌中文展示名（多语言），局外养成界面使用。
     * 改为实例方法，通过 res 获取。
     */
    public static String displayName(String key, Resources res) {
        // 直接通过 key 获取资源 ID，返回对应字符串
        switch (key) {
            case "strike":
                return res.getString(R.string.card_name_strike);
            case "defend":
                return res.getString(R.string.card_name_defend);
            case "burning_curse":
                return res.getString(R.string.card_name_burning_curse);
            case "jishixingle":
                return res.getString(R.string.card_name_jishixingle);
            case "swordflashing":
                return res.getString(R.string.card_name_swordflashing);
            case "beheading":
                return res.getString(R.string.card_name_beheading);
            case "memorial_procession":
                return res.getString(R.string.card_name_memorial_procession);
            case "inevitable_slash":
                return res.getString(R.string.card_name_inevitable_slash);
            case "afterimage_step":
                return res.getString(R.string.card_name_afterimage_step);
            case "RendSpace":
                return res.getString(R.string.card_name_rendspace);
            case "RendSpaceYuan":
                return res.getString(R.string.card_name_rendspace_yuan);
            case "Noself":
                return res.getString(R.string.card_name_noself);
            case "WordPower_Death":
                return res.getString(R.string.card_name_wordpower_death);
            case "EGO_RedBeauty":
                return res.getString(R.string.card_name_ego_redbeauty);
            case "ComboShoot":
                return res.getString(R.string.card_name_comboshoot);
            case "Disposal":
                return res.getString(R.string.card_name_disposal);
            case "ProliferatingG":
                return res.getString(R.string.card_name_proliferatingg);
            case "budaijiezou":
            case "budaijiezoufree":
                return res.getString(R.string.card_name_budaijiezou);
            case "kaidao":
                return res.getString(R.string.card_name_kaidao);
            case "CollectLight":
                return res.getString(R.string.card_name_collectlight);
            case "wudile":
                return res.getString(R.string.card_name_wudile);
            case "@AllHeiShou":
                return res.getString(R.string.card_name_heishou);
            case "Purify":
                return res.getString(R.string.card_name_purify);
            case "cogito":
                return res.getString(R.string.card_name_cogito);
            case "Tiphereth":
                return res.getString(R.string.card_name_tiphereth);
            case "SeedofLight":
                return res.getString(R.string.card_name_seedoflight);
            case "setDanger":
                return res.getString(R.string.card_name_setdanger);
            case "300Lunacy":
                return res.getString(R.string.card_name_300lunacy);
            case "zhuizhui":
                return res.getString(R.string.card_name_zhuizhui);
            case "ququ":
                return res.getString(R.string.card_name_ququ);
            case "Thunder":
                return res.getString(R.string.card_name_thunder);
            case "CalmBrain":
                return res.getString(R.string.card_name_calmbrain);
            case "CurrentGeneration":
                return res.getString(R.string.card_name_currentgeneration);
            case "rainbow":
                return res.getString(R.string.card_name_rainbow);
            case "charge":
                return res.getString(R.string.card_name_charge);
            case "sixthSense":
                return res.getString(R.string.card_name_sixthsense);
            case "EGOMagicBullet":
                return res.getString(R.string.card_name_egomagicbullet);
            case "Si":
                return res.getString(R.string.card_name_si);
            case "littleBird":
                return res.getString(R.string.card_name_littlebird);
            case "tallBird":
                return res.getString(R.string.card_name_tallbird);
            case "bigBird":
                return res.getString(R.string.card_name_bigbird);
            case "BigMonster":
                return res.getString(R.string.card_name_bigmonster);
            case "xiangxinwuhunzhenshen":
                return res.getString(R.string.card_name_xiangxin);
            case "bloodstainedtears":
                return res.getString(R.string.card_name_bloodstainedtears_start);
            case "bloodstainedtearsfinale":
                return res.getString(R.string.card_name_bloodstainedtears_finale);
            default:
                return key; // fallback
        }
    }

    /**
     * 判断卡牌 key 是否存在（用于加载存档时校验）
     */
    public static boolean ifExists(String key) {
        return Arrays.asList(ALL_CARD_KEYS).contains(key);
    }
}