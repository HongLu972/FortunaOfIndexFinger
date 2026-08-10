package com.six.fortuna.combat.engine;

/**
 * 对应 types.h 里的：
 *   typedef void (*CardEffectFunc)(Entity *player, Entity *enemy);
 *   typedef void (*ActionFunc)(Entity *self, Entity *player);
 *   typedef struct { char description[130]; ActionFunc execute; } EnemyAction;
 */
public class CardTypes {

    @FunctionalInterface
    public interface CardEffect {
        void play(Entity player, Entity enemy);
    }

    @FunctionalInterface
    public interface ActionFunc {
        void execute(Entity self, Entity player);
    }

    public static class Card {
        public String name;
        public String key;       // 持久化用的唯一标识，比如"strike"、"beheading"
        public int cost;
        public CardEffect play;
        public int rareness;     // 1 common, 2 silver, 3 gold
        public int consumption;  // 1 = 消耗（不进弃牌堆），0 = 不消耗

        public Card(String key, String name, int cost, CardEffect play, int rareness, int consumption) {
            this.key = key;
            this.name = name;
            this.cost = cost;
            this.play = play;
            this.rareness = rareness;
            this.consumption = consumption;
        }
    }
}