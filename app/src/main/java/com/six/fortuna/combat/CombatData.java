package com.six.fortuna.combat;

public class CombatData {
    //主要用作父类使用。。。大概
    public int hp;   //生命值，会被红色伤害削减
    public int max_hp;
    public int outside_max_hp; //提前搭建局内和局外数据，防止后面调整麻烦
    public int sp;   //精神值，会被白色伤害削减
    public int max_sp;
    public int outside_max_sp;  //和Hp一样定义外部和局内上限
    public int light; //光芒，约等于使用了enum的能量，打出所有书页都需要靠他！
    public int max_light;
    public int growth_light; //光芒不会直接在回合开始时回满，而是每回合固定回一些
    public int outside_max_light; //迟早会用到的
    public int[] resist = new int[4];
    //抗性中0->红色抗性; 1->白抗; 2->黑抗; 3->蓝抗
    public int grace;
    public int krama;

    public String lightString(){
        String output = "";
        for(int i = 0; i < light; i++){
            output += "🟡";
            //使用黄色球体来当作充满了光芒的槽位
        }
        for(int i = 0; i < max_light - light; i++){
            output += "⚪";
            //使用白色球体代表未充满光芒的槽位
            //光芒数量不可以高于槽位
        }
        return output;
    }

    public void turnStart(){
        //回合开始，获得光芒
        light += growth_light;
        //若光芒高于槽位，则去除
        if(light > max_light){
            light = max_light;
        }
    }

    public void recieve_red(int dmg){
        //红色伤害 - 降低生命值
        hp -= dmg * resist[0];
    }

    public void recieve_white(int dmg){
        //白色伤害 - 降低精神值
        sp -= dmg * resist[1];
    }

    public void recieve_black(int dmg){
        //黑色伤害 - 第三高贵的伤害 - 降低生命值和精神值
        hp -= dmg * resist[2];
        sp -= dmg * resist[2];
    }

    public void recieve_blue(int dmg){
        //蓝色伤害 - 第二高贵的伤害 - 降低生命值和精神值上限
        max_hp -= dmg * resist[3];
        max_sp -= dmg * resist[3];
    }

    public void recieve_gray(int dmg){
        //灰色伤害 - 本不应存在的伤害 - 最为高贵 - 因为这是百分比伤害而且无视抗性
        if(dmg * 0.01 * outside_max_hp > dmg){
            max_hp -= 0.01 * outside_max_hp * dmg;
        }else{
            max_hp -= dmg;
        }
        if(dmg * 0.01 *outside_max_sp > dmg){
            max_hp -= 0.01 * outside_max_sp * dmg;
        }else{
            max_sp -= dmg;
        }
    }
}
