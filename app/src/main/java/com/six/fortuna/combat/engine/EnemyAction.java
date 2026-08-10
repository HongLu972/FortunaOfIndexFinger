package com.six.fortuna.combat.engine;

public class EnemyAction {
    public String description;
    public CardTypes.ActionFunc execute;

    public EnemyAction(String description, CardTypes.ActionFunc execute) {
        this.description = description;
        this.execute = execute;
    }

    @FunctionalInterface
    public interface BrainFunc {
        EnemyAction choose(Entity self, Entity player);
    }
}