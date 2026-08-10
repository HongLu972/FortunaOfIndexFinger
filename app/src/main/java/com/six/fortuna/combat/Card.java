package com.six.fortuna.combat;

public class Card {
    public String name;
    public int cost;
    public String description;
    public int cardId; // 用于标识，后续可扩展

    public Card(String name, int cost, String description) {
        this.name = name;
        this.cost = cost;
        this.description = description;
    }
}