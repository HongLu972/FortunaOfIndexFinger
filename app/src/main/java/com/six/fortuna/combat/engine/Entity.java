package com.six.fortuna.combat.engine;

import com.six.fortuna.StatsActivity;

/**
 * 原样对应 C 版 types.h 里的 struct Entity_t。
 * 字段名和C版保持一致，方便对照 mechanics.c / cards.c / enemies.c 里的逻辑来核对。
 * 没有照搬卡组的链表实现（draw_stack_top等），Java这边直接用 List<Card> 代替，
 * 语义等价：draw_stack_top -> drawPile，discard_stack_top -> discardPile。
 */
public class Entity {
    public String name = "";
    public int gold;
    public int hp;
    public int max_hp;
    public int block;
    public int energy;
    public int max_energy;
    public Mechanics m;
    public int strength;
    public int swift;

    public int burn_term;
    public int burn_strength;

    public int rapture_strength;
    public int rapture_term;

    public int bleed_strength;
    public int bleed_term;

    public int sinking_term;
    public int sinking_strength;

    public int tremor_term;
    public int tremor_strength;
    public int tremor_conversion; // 0无 1灼热 2俱亡 3回声

    public int poise_term;
    public int poise_strength;

    public int charge_term;
    public int charge_strength;
    public int charge_consume;

    public int sanity;
    public int this_turn_strength;
    public int blade;
    public int EntityId;

    public double[] staggerLine = new double[]{0.75, 0.5, 0.25};
    public int outside_max_hp;
    public int panic;
    public int stagger_panic_term;
    public int stagger_count;
    public int health; // 混乱判定专用的"另一条血线"，与hp分开结算
    public int restrictions;
    public int grace;
    public int krama;
    public int gain_energy;
    public int difficulty;
    public int ammo; //弹药
    public int totalAmmo;
    public int tireness = 1;
    public int outside_MaxEnergy;
    public int ammoType;

    // 敌人AI用的计数器（C版里countA/countB/countC/shin是万能状态槽）
    public int countA, countB, countC, shin;
    public int tianjiustarblade;
    public StatsActivity s;

    public Buff buff = new Buff(m, s, this);

    // 当前意图（敌方专用，回合开始时brain函数算出来）
    public EnemyAction current_intent;

    public void initialization(){
        totalAmmo = 6;
        ammo = 6;
        ammoType = 0;
    }
    public Entity(){
        initialization();
    }

    public Entity(Mechanics m, StatsActivity s){
        this.s = s;
        this.m = m;
        this.buff = new Buff(m, s, this);
        initialization();
    }

    public void reload(){
        ammo = totalAmmo;
    }

    public boolean consumeAmmo(int amount){
        for(int i = 0; i < amount; i++){
            if(ammo < amount) return false;
            switch(ammoType){
                case 0:
                    break;
                case 1:
                    poise_term++;
                    poise_strength += 2;
                    swift++;
                    break;
                case 2://蝶
                    sanity += 6;
                    break;
                case 3:
                    if(totalAmmo != 7 || ammo > 7 || ammo < 0){
                        totalAmmo = 7;
                        reload();
                    }
            }
            ammo--;
        }
        return true;
    }



    private String getAmmoName(){
        switch (ammoType){
            case 0:
                return "🧨弹药";
            case 1:
                return "🧨加速弹";
            case 2:
                return "🦋活蝶/死蝶";
            case 3:
                String i = "";
                switch (ammo){
                    case 7:
                        i = "第一弹-贯穿";
                        break;
                    case 6:
                        i = "第二弹-精准";
                        break;
                    case 5:
                        i = "第三弹-摄魂";
                        break;
                    case 4:
                        i = "第四弹-残酷";
                        break;
                    case 3:
                        i = "第五弹-噤声";
                        break;
                    case 2:
                        i = "第六弹-倾泻";
                        break;
                    case 1:
                        i = "第七弹-绝望";
                        break;
                }
                return "🪄魔弹: "+i+"  ";
            default:
                return "🧨弹药";
        }
    }
    public String getAmmo(){
        if(ammoType == 3) return getAmmoName();
        return getAmmoName() + ":" + ammo + "/" + totalAmmo;
    }
}