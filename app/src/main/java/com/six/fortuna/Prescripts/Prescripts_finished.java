package com.six.fortuna.Prescripts;

import java.util.UUID;

public class Prescripts_finished {
    public int time_taken;
    public String prescripts;

    // 新增：唯一id（用于持久化定位这张卡片）
    public String id;
    // 新增：绝对截止时间戳（毫秒，System.currentTimeMillis()基准）
    // 0 表示尚未设置（还没真正被“抽出来挂起”）
    public long deadline;

    public Prescripts_finished(){
        //这个构造函数正常来说不会用到
    }

    public Prescripts_finished(int time_taken, String prescripts){
        this.time_taken = time_taken;
        this.prescripts = prescripts;
        this.id = UUID.randomUUID().toString();
        this.deadline = 0L;
    }

    public void setTime_taken(int time_taken) {
        this.time_taken = time_taken;
    }

    public int getTime_taken() {
        return time_taken;
    }

    public String getPrescripts() {
        return prescripts;
    }

    public void setPrescripts(String prescripts) {
        this.prescripts = prescripts;
    }

    /**
     * 激活这张卡片的倒计时：把截止时间钉死在“现在 + 耗时”。
     * 只应在第一次挂起时调用一次；从存储恢复时不要再调用（否则等于重新计时）。
     */
    public void activateDeadline(){
        if(this.deadline == 0L){
            this.deadline = System.currentTimeMillis() + (long) time_taken * 1000L;
        }
    }

    /**
     * 剩余秒数，向上取整。已到期返回0（不会是负数）。
     */
    public long getRemainingSeconds(){
        long remainMs = deadline - System.currentTimeMillis();
        if(remainMs <= 0) return 0;
        return (remainMs + 999) / 1000;
    }

    /**
     * 是否已经真正“熬过了”这段耗时，可以点击成功/失败。
     */
    public boolean isReady(){
        return deadline != 0L && System.currentTimeMillis() >= deadline;
    }
}