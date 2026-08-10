package com.six.fortuna.IndexLevel;
import static com.six.fortuna.MainActivity.addPrescriptGlobally;

import java.util.*;
import java.util.zip.Deflater;
import com.six.fortuna.*;
import com.six.fortuna.Prescripts.Prescripts_finished;

public class IndexFingerLevel_CN {
    Random r = new Random();
    public int grace;
    public int krama;
    public int rank;
    public int level;
    public boolean done;

    public IndexFingerLevel_CN(){

    }

    public IndexFingerLevel_CN(int grace, int krama, int rank, int level){
        this.grace = grace;
        this.krama = krama;
        this.rank = rank;
        this.level = level;
    }

    // 标记本次checkLevel()是否触发了“业过高被处决”的全部归零。
    // 这是一个纯数据标记，不涉及任何UI，UI层（MainActivity）读到true后
    // 负责弹窗，弹完记得把它重置回false，避免重复弹。
    public boolean justExecuted = false;

    public void checkLevel(){
        if(krama >= grace * 0.3 && krama >= 5){
            rank = 0;
            level = 1;
            grace = 0;
            krama = 0;
            justExecuted = true;
        }
        switch (rank){
            case 0:
                //编外成员晋升要求
                if(grace >= 50){
                    if(r.nextInt(101) <= (grace - 50) * 2){
                        rank++;
                        addPrescriptGlobally(new Prescripts_finished(-99, "你晋升了，现在是苦行者"));
                        if(grace >= 100){
                            grace -= 100;
                        }else{
                            grace = 0;
                        }
                        krama = 0;
                    }
                }
                break;
            case 1:
                if(grace >= 20){
                    if(level >= 9){
                        if(grace >= 40){
                            rank++;
                            addPrescriptGlobally(new Prescripts_finished(-99, "你晋升了，现在是代行者"));
                            level = 0;
                            grace -= 40;
                            krama = 0;
                        }
                    }else{
                        grace -= 20;
                        level++;
                    }
                }
                break;
            case 2:
                if(grace >= 30){
                    if(level >= 9){
                        if(grace >= 40){
                            addPrescriptGlobally(new Prescripts_finished(-99, "你晋升了，现在是传令员"));
                            rank++;
                            level = 0;
                            grace -= 40;
                            krama = 0;
                        }
                    }else{
                        grace -= 30;
                        level++;
                    }
                }
                break;
            case 3:
                if(grace >= 30){
                    if(level >= 9){
                        if(grace >= 50){
                            rank++;

                            addPrescriptGlobally(new Prescripts_finished(-99, "你晋升了，现在是神谕代行者"));
                            level = 0;
                            grace -= 50;
                            krama = 0;
                        }
                    }else{
                        grace -= 30;
                        level++;
                    }
                }
                break;
            case 4:
                if(grace >= 40){
                    if(level >= 9){
                        if(grace >= 75){
                            addPrescriptGlobally(new Prescripts_finished(-99, "你晋升了，现在是纺织者"));
                            addPrescriptGlobally(new Prescripts_finished(-98, "现在你的指令之路几乎到达了尽头"));
                            addPrescriptGlobally(new Prescripts_finished(-97, "你无法提高更多等阶，但可以依旧提升等级"));
                            rank++;
                            level = 0;
                            grace -= 75;
                            krama = 0;
                        }
                    }else{
                        grace -= 40;
                        level++;
                    }
                }
                break;
            case 5:
                if(grace >= 50){
                    level++;
                    grace -= 50;
                }
                break;
            case 6:
                if(grace >= 100){
                    level++;
                    grace -= 100;
                }
        }

    }

    public String getName(){
        checkLevel();
        switch (rank){
            case 0:
                return "食指编外成员  ";
            case 1:
                return "食指 苦行者 Lv."+level+"  ";
            case 2:
                return "食指 代行者 Lv."+level+"  ";
            case 3:
                return "食指 传令员 Lv."+level+"  ";
            case 4:
                return "食指 神谕代行者 Lv."+level+"  ";
            case 5:
                return "食指 纺织者 Lv."+level+"  ";
            case 6:
                //注:该等阶正常游戏无法通过常规游戏流程获得,只能通过作弊代码获得（尽管作弊代码尚未开发出来）
                return "食指之神 Lv."+level+"  ";
            default:
                return "Bug: OutOfIndexInLevel";
        }
    }
}