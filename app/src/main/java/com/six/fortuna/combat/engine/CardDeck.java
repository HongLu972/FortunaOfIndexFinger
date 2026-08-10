package com.six.fortuna.combat.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.six.fortuna.combat.engine.CardTypes.Card;

/**
 * 对应 cards.c 里 push_stack/pop_stack/drawCard/addCard/discardCard/shuffle_deck 那一套。
 * C版用链表栈实现 draw_stack_top / discard_stack_top，这里语义等价地换成 List：
 *   drawPile   <-> draw_stack_top
 *   discardPile<-> discard_stack_top
 *   hand       <-> hand[10] + hand_count
 * 洗牌规则、抽牌规则（抽牌堆空则把弃牌堆洗回来、都空则扣tireness）完全保留。
 */
public class CardDeck {
    public final List<Card> drawPile = new ArrayList<>();
    public final List<Card> discardPile = new ArrayList<>();
    public final List<Card> hand = new ArrayList<>();
    private final Random random = new Random();
    private final Mechanics mechanics;

    public CardDeck(Mechanics mechanics) {
        this.mechanics = mechanics;
    }

    /** 对应 shuffle_deck(player)：Fisher-Yates 洗牌，弃牌堆整体洗回抽牌堆 */
    public void shuffleDiscardIntoDraw(Mechanics.Logger logger) {
        if (discardPile.isEmpty()) return;
        drawPile.addAll(discardPile);
        discardPile.clear();
        Collections.shuffle(drawPile, random);
        if (logger != null) logger.log("洗牌！弃牌堆已并入抽牌堆。");
    }

    /** 对应 drawCard(player)：抽牌堆空则先洗牌，都空则扣tireness自伤 */
    public void drawCard(Entity owner, Mechanics.Logger logger) {
        if (drawPile.isEmpty()) {
            if (discardPile.isEmpty()) {
                if (hand.isEmpty()) {
                    if (logger != null) logger.log("抽牌堆已空！");
                    owner.tireness *= 2;
                    mechanics.dealDamage(owner.tireness, owner, owner);
                }
                return;
            } else {
                shuffleDiscardIntoDraw(logger);
            }
        }
        if (hand.size() >= 10) {
            if (logger != null) logger.log("手牌已满");
            return;
        }
        if (!drawPile.isEmpty()) {
            hand.add(drawPile.remove(drawPile.size() - 1));
        }
    }

    public void drawCards(int count, Entity owner, Mechanics.Logger logger) {
        for (int i = 0; i < count; i++) drawCard(owner, logger);
    }

    /** 对应 addCard：直接塞进手牌，不走抽牌堆 */
    public void addCard(Card card, Mechanics.Logger logger) {
        if (hand.size() >= 10) {
            if (logger != null) logger.log("手牌已满");
            return;
        }
        hand.add(card);
    }

    /** 打出一张牌：从手牌移除，消耗牌不进弃牌堆，非消耗牌进弃牌堆 */
    public void playFromHand(Card card) {
        hand.remove(card);
        if (card.consumption == 0) {
            discardPile.add(card);
        }
        // consumption == 1 的消耗牌直接消失，不进任何堆
    }

    /** 回合结束：剩余手牌进弃牌堆 */
    public void discardHand() {
        discardPile.addAll(hand);
        hand.clear();
    }
}