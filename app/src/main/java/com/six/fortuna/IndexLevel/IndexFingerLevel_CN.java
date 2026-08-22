package com.six.fortuna.IndexLevel;

import static com.six.fortuna.MainActivity.addPrescriptGlobally;

import android.content.res.Resources;

import java.util.Random;

import com.six.fortuna.R;
import com.six.fortuna.Prescripts.Prescripts_finished;

public class IndexFingerLevel_CN {
    private final Resources res;
    private final Random r = new Random();

    public int grace;
    public int krama;
    public int rank;
    public int level;
    public boolean done;

    // 无参构造（保留兼容性，但会警告）
    @Deprecated
    public IndexFingerLevel_CN() {
        this.res = null;
    }

    public IndexFingerLevel_CN(Resources res) {
        this.res = res;
    }

    public IndexFingerLevel_CN(int grace, int krama, int rank, int level, Resources res) {
        this.grace = grace;
        this.krama = krama;
        this.rank = rank;
        this.level = level;
        this.res = res;
    }

    // 标记本次checkLevel()是否触发了"业过高被处决"的全部归零。
    // 这是一个纯数据标记，不涉及任何UI，UI层（MainActivity）读到true后
    // 负责弹窗，弹完记得把它重置回false，避免重复弹。
    public boolean justExecuted = false;

    public void checkLevel() {
        if (krama >= grace * 0.3 && krama >= 5) {
            rank = 0;
            level = 1;
            grace = 0;
            krama = 0;
            justExecuted = true;
        }
        switch (rank) {
            case 0:
                // 编外成员晋升要求
                if (grace >= 50) {
                    if (r.nextInt(101) <= (grace - 50) * 2) {
                        rank++;
                        addPrescriptGlobally(new Prescripts_finished(-99, getString(R.string.index_promote_penitent)));
                        if (grace >= 100) {
                            grace -= 100;
                        } else {
                            grace = 0;
                        }
                        krama = 0;
                    }
                }
                break;
            case 1:
                if (grace >= 20) {
                    if (level >= 9) {
                        if (grace >= 40) {
                            rank++;
                            addPrescriptGlobally(new Prescripts_finished(-99, getString(R.string.index_promote_procurator)));
                            level = 0;
                            grace -= 40;
                            krama = 0;
                        }
                    } else {
                        grace -= 20;
                        level++;
                    }
                }
                break;
            case 2:
                if (grace >= 30) {
                    if (level >= 9) {
                        if (grace >= 40) {
                            addPrescriptGlobally(new Prescripts_finished(-99, getString(R.string.index_promote_herald)));
                            rank++;
                            level = 0;
                            grace -= 40;
                            krama = 0;
                        }
                    } else {
                        grace -= 30;
                        level++;
                    }
                }
                break;
            case 3:
                if (grace >= 30) {
                    if (level >= 9) {
                        if (grace >= 50) {
                            rank++;
                            addPrescriptGlobally(new Prescripts_finished(-99, getString(R.string.index_promote_oracle)));
                            level = 0;
                            grace -= 50;
                            krama = 0;
                        }
                    } else {
                        grace -= 30;
                        level++;
                    }
                }
                break;
            case 4:
                if (grace >= 40) {
                    if (level >= 9) {
                        if (grace >= 75) {
                            addPrescriptGlobally(new Prescripts_finished(-99, getString(R.string.index_promote_weaver)));
                            addPrescriptGlobally(new Prescripts_finished(-98, getString(R.string.index_promote_note_1)));
                            addPrescriptGlobally(new Prescripts_finished(-97, getString(R.string.index_promote_note_2)));
                            rank++;
                            level = 0;
                            grace -= 75;
                            krama = 0;
                        }
                    } else {
                        grace -= 40;
                        level++;
                    }
                }
                break;
            case 5:
                if (grace >= 50) {
                    level++;
                    grace -= 50;
                }
                break;
            case 6:
                if (grace >= 100) {
                    level++;
                    grace -= 100;
                }
        }
    }

    public String getName() {
        checkLevel();
        // 如果 res 为 null，回退到硬编码（兼容旧调用）
        if (res == null) {
            return fallbackGetName();
        }
        switch (rank) {
            case 0:
                return res.getString(R.string.index_rank_0);
            case 1:
                return String.format(res.getString(R.string.index_rank_1), level);
            case 2:
                return String.format(res.getString(R.string.index_rank_2), level);
            case 3:
                return String.format(res.getString(R.string.index_rank_3), level);
            case 4:
                return String.format(res.getString(R.string.index_rank_4), level);
            case 5:
                return String.format(res.getString(R.string.index_rank_5), level);
            case 6:
                return String.format(res.getString(R.string.index_rank_6), level);
            default:
                return res.getString(R.string.index_rank_bug);
        }
    }

    /**
     * 回退方案：当 res 为 null 时使用硬编码（兼容旧的无参构造调用）
     */
    private String fallbackGetName() {
        switch (rank) {
            case 0:
                return "食指编外成员  ";
            case 1:
                return "食指 苦行者 Lv." + level + "  ";
            case 2:
                return "食指 代行者 Lv." + level + "  ";
            case 3:
                return "食指 传令员 Lv." + level + "  ";
            case 4:
                return "食指 神谕代行者 Lv." + level + "  ";
            case 5:
                return "食指 纺织者 Lv." + level + "  ";
            case 6:
                return "食指之神 Lv." + level + "  ";
            default:
                return "Bug: OutOfIndexInLevel";
        }
    }

    /**
     * 安全获取字符串，如果 res 为 null 则返回 fallback
     */
    private String getString(int resId) {
        if (res == null) {
            return "（资源未加载）";
        }
        return res.getString(resId);
    }
}