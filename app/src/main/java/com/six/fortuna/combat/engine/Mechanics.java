package com.six.fortuna.combat.engine;

import android.content.res.Resources;

import com.six.fortuna.R;

import java.util.Random;

/**
 * 一比一对应 machanics.c 里的函数。
 * 数值逻辑（乘区、判定顺序）完全保留，只是把 printf 换成 log 回调，
 * C里的 target->hp -= xxx 直接翻译成 Java 一样的写法。
 */
public class Mechanics {

    public interface Logger {
        void log(String msg);
    }

    public final Logger logger;
    private final Resources res;
    private final Random random = new Random();

    public Mechanics(Logger logger, Resources res) {
        this.logger = logger;
        this.res = res;
    }

    private void log(String msg) {
        if (logger != null) logger.log(msg);
    }

    // --- randint(min, max) ---
    public int randint(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    // --- AmplitudeConversion(target, getitself, conversetype) ---
    // Java端不做交互式输入，直接指定 conversetype（0无 1灼热 2俱亡 3回声）
    private String amplitudeName(int conversetype) {
        switch (conversetype) {
            case 1:
                return res.getString(R.string.mechanic_amplitude_hot);
            case 2:
                return res.getString(R.string.mechanic_amplitude_sinking);
            case 3:
                return res.getString(R.string.mechanic_amplitude_weak);
            default:
                return res.getString(R.string.mechanic_amplitude_unknown);
        }
    }

    public void amplitudeConversion(Entity target, int conversetype) {
        log(String.format(res.getString(R.string.mechanic_log_amplitude), target.name, amplitudeName(conversetype)));
        target.tremor_conversion = conversetype;
    }

    // --- deal_damage(amount, target, self) ---
    public boolean dealDamage(int amount, Entity target, Entity self) {
        // 1. 基础物理伤害与力量乘区
        int damage = amount + self.strength + self.this_turn_strength;
        if (target.buff.sidestep >= damage) {
            if (target.bleed_term > 0) {
                bleedActivate(target);
            }
            if (self.bleed_term > 0) {
                bleedActivate(self);
            }
            target.buff.sidestep--;
            log(String.format(res.getString(R.string.mechanic_log_dodge), target.name, damage, target.buff.sidestep));
            return false;
        } else if (target.buff.sidestep > 0) {
            if (target.buff.Precongization > 0) {
                if (target.buff.Precongization >= 20) {
                    target.buff.Precongization -= Math.min(target.buff.Precongization / 2, damage / 5);
                    return false;
                }
            } else {
                target.buff.sidestep = 0;
            }
            self.sanity += 2;
        }
        sanityReturn(self);
        damage *= 1 + 0.01 * ((self.sanity > 0) ? (self.sanity) : (-1 * self.sanity));

        if (damage <= 0) {
            damage = 0;
        } else {
            if (randint(1, 100) <= self.poise_strength * 5) {
                damage = (int) (damage * (1.5 + Math.min(1.0, 0.01 * self.poise_strength)));
                self.poise_term--;
            }
        }

        // 2. 触发破裂(Rapture)伤害增幅
        if (target.rapture_term > 0) {
            target.hp -= target.rapture_strength;
            target.rapture_term--;
            if (target.rapture_term == 0) {
                target.rapture_strength = 0;
            }
        }

        // 3. 触发沉沦(Sinking)精神判定伤害
        sinking(target);

        // 4. 触发流血(Bleed)结算
        if (self.bleed_term > 0) {
            bleedActivate(self);
        }

        if (self.ammoType == 3) {
            switch (self.ammo) {
                case 7:
                    damage *= 1.2;
                    target.health -= damage;
                    break;
                case 6:
                    damage *= 1.1;
                    target.stagger_panic_term++;
                    break;
                case 5:
                    damage *= 1.2;
                    target.burn_strength += 30;
                    target.this_turn_strength -= 10;
                    break;
                case 4:
                    damage *= 1.3;
                    target.health -= damage;
                    target.swift -= 12;
                    break;
                case 3:
                    damage *= 1.3;
                    target.burn_strength += 50;
                    target.strength -= 3;
                    target.swift -= 3;
                    break;
                case 2:
                    damage *= 0.7;
                    if (target.burn_strength > 7) {
                        if (target.burn_strength > 14) {
                            if (target.burn_strength > 21) {
                                for (int i = 0; i < 3; i++) target.block -= damage;
                            } else {
                                for (int i = 0; i < 2; i++) target.block -= damage;
                            }
                        } else {
                            target.block -= damage;
                        }
                    }
                    break;
                case 1:
                    damage *= 2;
                    damage *= 1 + 0.025 * ((target.max_hp - target.hp) / (target.max_hp / 100));
                    self.consumeAmmo(1);
                    dealDamage((int) (damage * 0.8), self, self);
                    self.reload();
                    break;
                case 0:
                    damage *= 1.3;
                    target.health -= damage;

            }
            if (self.ammo > 0) self.consumeAmmo(1);
        }

        if (target.buff.rainoftears >= 1) {
            if (target.sinking_term > 0) {
                damage *= 1 - 0.1 * ((target.sinking_term > 9) ? 9 : target.sinking_term);
                target.sinking_term++;
                target.sinking_strength++;
            }
            target.strength += 2;
        }

        if (target.buff.littlebird == 1) {
            target.strength += 3;
            target.max_hp *= 1.2;
        }

        // 5. 护盾抵扣伤害结算
        if (target.block >= damage) {
            target.block -= damage;
            log(String.format(res.getString(R.string.mechanic_log_shield_block), target.name, target.block));
        } else {
            damage -= target.block;
            target.block = 0;
            target.hp -= damage;
            target.health -= damage;
            log(String.format(res.getString(R.string.mechanic_log_damage), target.name, damage, target.hp, target.max_hp));
        }

        //6. 解放带来的增益
        if (self.buff.Unlock > 0) {
            self.sanity += 5;
            if (self.buff.Unlock > 1) {
                self.strength += 2;
                if (self.buff.Unlock > 2) {
                    self.grace += 1;
                }
            }
        }
        return true;
    }

    public void defend(int amount, Entity target) {
        amount += target.swift;
        if (amount <= 0) return;
        target.block += amount;
    }

    public void sidestep(int amount, Entity target) {
        amount += target.swift * 0.75;
        if (amount <= 0) return;
        target.buff.sidestep += amount;
    }

    // --- heal(amount, target) ---
    public void heal(int amount, Entity target) {
        target.hp += amount;
        if (target.hp > target.max_hp) target.hp = target.max_hp;
    }

    // --- burn(target) ---
    public void burn(Entity target) {
        if (target.burn_term > 0) {
            target.hp -= target.burn_strength;
            target.health -= target.burn_strength;
            log(String.format(res.getString(R.string.mechanic_log_burn), target.name, target.burn_strength));
            target.burn_term--;
            if (target.burn_term == 0) target.burn_strength = 0;
        }
    }

    // --- bleedActivate(target) ---
    public void bleedActivate(Entity target) {
        if (target.bleed_term > 0) {
            target.hp -= target.bleed_strength;
            target.health -= target.bleed_strength;
            log(String.format(res.getString(R.string.mechanic_log_bleed), target.name, target.bleed_strength));
            target.bleed_term--;
            if (target.bleed_term == 0) target.bleed_strength = 0;
        }
    }

    // --- sinking(target) ---
    public void sinking(Entity target) {
        if (target.sinking_term > 0) {
            target.sanity -= target.sinking_strength;
            target.sinking_term--;
            log(String.format(res.getString(R.string.mechanic_log_sinking),
                    target.name, target.sinking_strength, target.sanity));
            if (target.sinking_term <= 0) {
                target.sinking_strength = 0;
                target.sinking_term = 0;
            }
        }
    }

    // --- tremorBurst(target) ---
    public void tremorBurst(Entity target) {
        if (target.tremor_term > 0) {
            log(String.format(res.getString(R.string.mechanic_log_tremor), target.name));
            target.health -= target.tremor_strength;
            target.tremor_term--;
            if (target.tremor_term == 0) target.tremor_strength = 0;

            switch (target.tremor_conversion) {
                case 0:
                    break;
                case 1: // 震颤 - 灼热
                    target.burn_strength += (int) (target.tremor_strength * 0.01);
                    target.burn_term += (int) (target.tremor_term * 0.3);
                    burn(target);
                    break;
                case 2: // 震颤 - 俱亡
                    if (target.tremor_strength > 500) {
                        target.sinking_strength += 5;
                    } else {
                        target.sinking_strength += (int) (target.tremor_strength * 0.01);
                    }
                    target.sinking_term += 1;
                    sinking(target);
                    break;
                case 3: // 震颤 - 回声
                    target.this_turn_strength -= (int) (target.tremor_strength * 0.03 + 1);
                    break;
            }
        }
    }

    // --- sanityReturn(target) ---
    public void sanityReturn(Entity target) {
        if (target.sanity > 45) target.sanity = 45;
        if (target.sanity < -45) target.sanity = -45;
    }

    // --- sanityCheck(target) ---
    public int sanityCheck(Entity target) {
        sanityReturn(target);
        if (target.sanity == -45) return 2;
        else if (target.sanity <= -20) return 1;
        return 0;
    }

    // --- ifStaggered(target) ---
    public int ifStaggered(Entity target) {
        // 防护：C版里stagger_count理论不会超过3，但Java这边多加一层边界检查，避免数组越界崩溃
        if (target.stagger_count >= target.staggerLine.length) {
            return 0;
        }
        double staggerLine = target.staggerLine[target.stagger_count];
        int checkPoint = (int) (target.outside_max_hp * staggerLine);

        if (target.health < checkPoint && target.stagger_count < 3 && target.buff.UnlockedHealth > 0) {
            target.stagger_panic_term += 1;
            target.stagger_count++;
            log(String.format(res.getString(R.string.mechanic_log_stagger), target.name));
            target.block = 0;
            return 1;
        }
        return 0;
    }
}