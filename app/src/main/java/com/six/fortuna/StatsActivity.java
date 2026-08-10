package com.six.fortuna;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.six.fortuna.IndexLevel.IndexFingerLevel_CN;
import com.six.fortuna.combat.engine.CardDeck;
import com.six.fortuna.combat.engine.CardTypes.Card;
import com.six.fortuna.combat.engine.Entity;
import com.six.fortuna.combat.engine.EnemyAction;
import com.six.fortuna.combat.engine.EntitySerializer;
import com.six.fortuna.combat.engine.FortunaCards;
import com.six.fortuna.combat.engine.FortunaEnemies;
import com.six.fortuna.combat.engine.Mechanics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private static final String END_TURN_LABEL = "🔚 结束回合";
    private static final String NEXT_BATTLE_LABEL = "🔁 开始下一场战斗";
    private static final String RETRY_LABEL = "🔁 重新开始（危险度已重置）";
    private static final int HAND_SIZE = 5;

    // 局外初始牌组（第一次进游戏时发的牌，之后掉落卡会往里加）
    private static final String[] STARTER_DECK = {
            "cogito", "cogito", "ququ"
    };

    // UI
    private TextView tvTitle, tvKarma, tvBlessing;
    private TextView tvHP, tvSanity, tvEXP, tvLevel, tvLight;
    private TextView tvCombatLog;
    private TextView tvEnemyName, tvEnemyIntent, tvEnemyStatus, tvPlayerStatus;
    private ProgressBar enemyHpBar;
    private Spinner spinnerCards;
    private Button btnConfirm;
    private ProgressBar hpBar, sanBar, expBar;
    private ArrayAdapter<String> spinnerAdapter;

    private PrescriptStore store;
    private IndexFingerLevel_CN indexFingerLevel;

    // ---- 引擎层 ----
    public Mechanics mechanics;
    private CardDeck deck;
    private FortunaCards cardDefs;
    private FortunaEnemies enemyDefs;
    private Entity player;
    private Entity enemy;

    // 战斗是否已经结束（胜利/失败），结束后Spinner只显示"开始下一场"
    private boolean battleOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

        store = new PrescriptStore(this);
        indexFingerLevel = new IndexFingerLevel_CN();
        store.loadStats(indexFingerLevel);

        bindViews();
        tvCombatLog.setMovementMethod(new ScrollingMovementMethod());

        mechanics = new Mechanics(this::appendLog);
        cardDefs = new FortunaCards(mechanics, this);
        enemyDefs = new FortunaEnemies(mechanics, this);
        deck = new CardDeck(mechanics);

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCards.setAdapter(spinnerAdapter);

        btnConfirm.setOnClickListener(v -> {
            if (battleOver) {
                startNewBattle();
                return;
            }
            int pos = spinnerCards.getSelectedItemPosition();
            List<String> names = currentSpinnerNames();
            if (pos < 0 || pos >= names.size()) {
                Toast.makeText(this, "请选择一个操作", Toast.LENGTH_SHORT).show();
                return;
            }
            if (names.get(pos).equals(END_TURN_LABEL)) {
                endTurn();
                return;
            }
            if (deck.hand.isEmpty() || pos >= deck.hand.size()) {
                Toast.makeText(this, "没有可用的卡牌", Toast.LENGTH_SHORT).show();
                return;
            }
            if(player.stagger_panic_term > 0){
                Toast.makeText(this, "你已经陷入混乱了☺️🌸", Toast.LENGTH_SHORT).show();
                return;
            }
            useCard(deck.hand.get(pos));
        });

        findViewById(R.id.nav_home).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.nav_stats).setOnClickListener(v -> {});
        findViewById(R.id.nav_setting).setOnClickListener(v ->
                startActivity(new Intent(this, SettingActivity.class)));
        Button navDeck = findViewById(R.id.nav_deck);
        if (navDeck != null) {
            navDeck.setOnClickListener(v ->
                    startActivity(new Intent(this, DeckManageActivity.class)));
        }

        // 优先尝试恢复上次没打完的战斗快照，没有的话才走全新开局
        if (!restoreBattleSnapshot()) {
            startNewBattle();
        } else {
            updateSpinner();
            refreshAllUI();
        }
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvKarma = findViewById(R.id.tv_karma);
        tvBlessing = findViewById(R.id.tv_blessing);
        tvHP = findViewById(R.id.HP);
//        tvSanity = findViewById(R.id.Sanity);
        tvEXP = findViewById(R.id.EXPNumber);
        tvLevel = findViewById(R.id.level);
        tvLight = findViewById(R.id.LTAmount);
        tvCombatLog = findViewById(R.id.CombatText);
        tvEnemyName = findViewById(R.id.tv_enemy_name);
        tvEnemyIntent = findViewById(R.id.tv_enemy_intent);
        tvEnemyStatus = findViewById(R.id.tv_enemy_status);
        tvPlayerStatus = findViewById(R.id.tv_player_status);
        enemyHpBar = findViewById(R.id.EnemyHPBar);
        spinnerCards = findViewById(R.id.BookPagesChoice);
        btnConfirm = findViewById(R.id.ComfirmUsing);
        hpBar = findViewById(R.id.HPBar);
//        sanBar = findViewById(R.id.SanBar);
        expBar = findViewById(R.id.EXPBar);
    }

    // ===================== 开局 / 重开 =====================

    /** 全新开一场战斗：血量按局外养成回满，危险度接着存档里的走，掉落的卡也已经在牌组里了 */
    private void startNewBattle() {
        battleOver = false;
        initPlayer();
        initDeckFromOwnedCards();
        spawnByDifficulty();
        enemy.current_intent = computeEnemyIntent();
        appendLog("========== 新的战斗开始，当前危险度: " + player.difficulty + " ==========");
        startTurn();
    }

    private void initPlayer() {
        player = new Entity(mechanics, this);
        player.name = store.loadCodename();
        int bonusHp = store.loadBonusMaxHp();
        player.outside_max_hp = 200 + bonusHp;
        player.max_hp = player.outside_max_hp;
        player.hp = player.outside_max_hp;
        player.max_energy = 12;
        player.gain_energy = 3;
        player.energy = 5;
        player.EntityId = 0;
        player.swift = 0;
        player.strength = 0;
        player.sanity = 0;
        player.tireness = 1;
        player.staggerLine = new double[]{0.6, 0.4, 0.2};
        player.buff.reset();
        player.ammoType = 0;
        player.totalAmmo = 6;
        player.reload();
        player.health = player.outside_max_hp;
        player.difficulty = store.loadBattleDifficulty();
    }

    /** 从局外持久卡组（store里存的）构建这场战斗要用的抽牌堆，第一次进游戏发STARTER_DECK */
    private void initDeckFromOwnedCards() {
        List<String> owned = store.loadOwnedCardKeys();
        if (owned == null) {
            owned = new ArrayList<>();
            for (String k : STARTER_DECK) owned.add(k);
            store.saveOwnedCardKeys(owned);
        }
        deck.drawPile.clear();
        deck.discardPile.clear();
        deck.hand.clear();
        for (String key : owned) {
            deck.drawPile.add(cardDefs.createByKey(key, deck, this::appendLog));
        }
        java.util.Collections.shuffle(deck.drawPile);
    }

    private void spawnByDifficulty() {
        if (player.difficulty <= 10) {
            NormalBattle();
            player.difficulty += mechanics.randint(1, 2);
            appendLog("当前危险度: " + player.difficulty + "[注:高于10可能引发精英战斗]");
        } else if (player.difficulty <= 50) {
            if(mechanics.randint(100, 799) <= 325) {
                NormalBattle();
            }
            EliteBattle();
            player.difficulty += mechanics.randint(1, 5);
            appendLog("当前危险度: " + player.difficulty + "[注:高于50可能引发BOSS战]");
        } else {
            player.difficulty += mechanics.randint(4, 12);
            appendLog("当前危险度: " + player.difficulty + "[注:可能触发BOSS战，制约开始出现]");
            if(mechanics.randint(1, 100) < player.difficulty - 50 && !(mechanics.randint(1, 100) == 1)) {
                player.restrictions = (int) ((1 + 0.01 * mechanics.randint(1, 50)) * (player.difficulty / 10));
                BossBattle();
            }else{
                player.restrictions = (int) ((1 + 0.01 * mechanics.randint(1, 50)) * (player.difficulty / 10));
                if(mechanics.randint(225, 325) == 325){
                    NormalBattle();
                }else {
                    EliteBattle();
                }
            }
        }
        store.saveBattleDifficulty(player.difficulty);
    }

    private EnemyAction computeEnemyIntent() {
        switch (enemy.EntityId){
            case 1:
                return enemyDefs.brainHajimi(enemy, player);
            case 2:
                return enemyDefs.brainAlbina(enemy, player);
            case 3:
                return enemyDefs.brainRein(enemy, player);
            case 4:
                return enemyDefs.barinForgottenKiller(enemy, player);
            case 5:
                return enemyDefs.brainValencina(enemy, player);
            case 6:
                return enemyDefs.hearts(enemy, player);
            case 7:
                return enemyDefs.brainYoshide(enemy, player);
            case 8:
                return enemyDefs.brainQuQu(enemy, player);
            case 9:
                return enemyDefs.brainThunderSpirit(enemy, player);
            default:
                return null;
        }
    }

    private void NormalBattle(){
        switch (mechanics.randint(1, 2)){
            case 1:
                spawnHajimi();
            case 2:
                spawnEnemy("?!心脏?!", 1000, new double[] {0.3, 0.2, 0.1}, 6);
            case 3:
                spawnEnemy("雷元素精灵", 1000, new double[] {0.6, 0.4, 0.2}, 9);
        }
    }

    private void spawnHajimi() {
        enemy = new Entity(mechanics, this);
        enemy.name = "哈基米";
        enemy.max_hp = 1000;
        enemy.EntityId = 1;
        enemy.outside_max_hp = 1000;
        enemy.hp = enemy.outside_max_hp;
        enemy.health = enemy.hp;
        enemy.strength = 2;
        enemy.swift = 0;
        enemy.staggerLine = new double[]{0.75, 0.5, 0.25};
    }

    private void EliteBattle(){
        switch (mechanics.randint(1, 3)){
            case 1:
                spawnAlbina();
                break;
            case 2:
                spawnEnemy("被遗弃的杀人魔", 3000, new double[] {0.2, 0.4, 0.6}, 4);
            case 3:
                spawnEnemy("艾兰虫", 10000, new double[] {0, 0, 0}, 8);
        }
    }

    private void spawnAlbina(){
        spawnEnemy("阿尔比娜", 2000, new double[]{0.8, 0.6, 0.4}, 2);
    }

    private void BossBattle(){
        switch (mechanics.randint(1, 3)){
            case 1:
                spawnRein();
                break;
            case 2:
                spawnEnemy("瓦伦希娜", 8000, new double[]{0.9, 0.5, 0.2}, 5);
                break;
            case 3:
                spawnEnemy("无我良秀", 10000, new double[]{-0.2, -0.5, -1.0}, 7);
        }
    }
    private void spawnRein(){
        spawnEnemy("里恩", 10000, new double[]{0.7, 0.5, 0.2}, 3);
    }

    private void spawnEnemy(String name, int outside_max_hp, double[] staggerLine, int id){
        enemy = new Entity(mechanics, this);
        enemy.name = name;
        enemy.EntityId = id;
        enemy.outside_max_hp = outside_max_hp;
        enemy.max_hp = outside_max_hp;
        enemy.hp = outside_max_hp;
        enemy.health = outside_max_hp;
        enemy.staggerLine = staggerLine;
        enemy.strength = 0;
        enemy.swift = 0;
    }

    // ===================== 回合循环 =====================

    private void startTurn() {
        appendLog("——— 回合开始 ———");
        appendLog("牌库："+deck.drawPile.size());
        appendLog("弃牌堆："+deck.discardPile.size());
        player.buff.turnStartActivate(enemy);
        enemy.buff.turnStartActivate(player);
        player.this_turn_strength = 0;
        enemy.this_turn_strength = 0;
        player.energy += player.gain_energy;
        player.this_turn_strength = 0;
        deck.drawCards(HAND_SIZE - deck.hand.size(), player, this::appendLog);
        if(player.energy >= player.max_energy){
            player.energy = player.max_energy;
        }
        if(player.restrictions >= 50){
            player.energy--;
        }
        if(player.sanity <= -45){
            player.stagger_panic_term++;
            player.sanity = 0;
        }
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();
    }

    private void endTurn() {
        appendLog("——— 回合结束 ———");
        deck.discardHand();
        player.buff.turnEndActivate();
        enemy.buff.turnEndActivate();

        if(player.energy >= player.max_energy){
            player.energy = player.max_energy;
        }

        if (enemy.hp > 0 && enemy.current_intent != null && enemy.current_intent.execute != null) {
            appendLog(enemy.name + ": " + enemy.current_intent.description);
            enemy.current_intent.execute.execute(enemy, player);
            if(enemy.buff.cibei > 0){
                if(enemy.countB == 1){
                    Toast.makeText(this, "艾兰不市区", Toast.LENGTH_SHORT).show();
                    appendLog("我开源了追击，你们怎么可以笑我！");
                    for(int i = 0; i < player.buff.cibei; i++){
                        appendLog("？！慈悲？！");
                        enemy.this_turn_strength += 3;
                        useCard(cardDefs.zhuizhui());
                    }
                }else{
                    Toast.makeText(this, "纵使此身被区之名覆盖...", Toast.LENGTH_SHORT).show();
                    appendLog("所有笑我的人和小金，一同和我化为永恒的区吧！");
                    for(int i = 0; i < enemy.buff.cibei; i++){
                        if(enemy.buff.cibei > 15){
                            enemy.this_turn_strength += enemy.buff.cibei - 15;
                            enemy.buff.cibei = 15;
                        }
                        appendLog("就由我来开源追击！");
                        useCard(cardDefs.Elanzhuizhui());
                        if(i > 15){
                            break;
                        }
                    }
                }
            }
        }

        if(player.stagger_panic_term > 0){
            player.stagger_panic_term--;
        }
        if(enemy.stagger_panic_term > 0){
            enemy.stagger_panic_term--;
        }

        mechanics.burn(player);
        mechanics.burn(enemy);
        if(player.tremor_term > 0) {
            player.tremor_term--;
        }
        if(enemy.tremor_term > 0){
            enemy.tremor_term--;
        }

        mechanics.ifStaggered(player);
        mechanics.ifStaggered(enemy);

        refreshAllUI();

        if (player.hp <= 0) {
            player.hp = 0;
            onDefeat();
            return;
        }
        if (enemy.hp <= 0) {
            if(enemy.EntityId == 7 && enemy.block > 0){
                enemy.max_hp = enemy.block;
                enemy.hp = enemy.max_hp;
                enemy.block = 0;
            }else {
                if(enemy.countB < 2 && enemy.EntityId == 7){
                    enemy.max_hp = (int) ((int) (enemy.outside_max_hp * 0.2)*(1 + 0.03 * player.restrictions));
                    enemy.countB++;
                    enemy.hp = enemy.max_hp;
                }else {
                    enemy.hp = 0;
                    onVictory();
                    return;
                }
            }
        }

        if(enemy.stagger_panic_term <= 0) enemy.current_intent = computeEnemyIntent();
        startTurn();
    }

    private void useCard(Card card) {
        if (player.energy < card.cost) {
            Toast.makeText(this, "光芒不足！需要 " + card.cost + " 点", Toast.LENGTH_SHORT).show();
            return;
        }
        if(player.sanity <= -45){
            player.stagger_panic_term++;
            player.sanity = 0;
        }
        if(player.stagger_panic_term > 0){
            appendLog("混乱中。。。");
            return;
        }
        player.energy -= card.cost;
        card.play.play(player, enemy);
        if(player.buff.cibei > 0){
            Toast.makeText(this, "见·追·笑，艾兰市区", Toast.LENGTH_SHORT).show();
            appendLog("既见区区，为何不笑？");
            for(int i = 0; i < player.buff.cibei; i++){
                appendLog("？！慈悲？！");
                useCard(cardDefs.zhuizhui(), 1);
            }
        }
        deck.playFromHand(card);
        store.saveBattleDifficulty(player.difficulty);
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();

        appendLog("牌库："+deck.drawPile.size());
        appendLog("弃牌堆："+deck.discardPile.size());

        if (enemy.hp <= 0) {
            if(enemy.EntityId == 7 && enemy.block > 0){
                enemy.max_hp = enemy.block;
                enemy.hp = enemy.max_hp;
                enemy.block = 0;
            }else {
                if(enemy.EntityId == 7 && enemy.countB < 2) return;
                enemy.hp = 0;
                onVictory();
            }
        }
    }

    private void useCard(Card card, int i) {
        if (player.energy < card.cost) {
            Toast.makeText(this, "光芒不足！需要 " + card.cost + " 点", Toast.LENGTH_SHORT).show();
            return;
        }
        player.energy -= card.cost;
        card.play.play(player, enemy);
        deck.playFromHand(card);
        store.saveBattleDifficulty(player.difficulty);
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();

        appendLog("牌库："+deck.drawPile.size());
        appendLog("弃牌堆："+deck.discardPile.size());

        if (enemy.hp <= 0) {
            enemy.hp = 0;
            onVictory();
        }
    }

    // ===================== 胜负结算 =====================

    private void onVictory() {
        Toast.makeText(this, "🎉 击败了 " + enemy.name, Toast.LENGTH_LONG).show();
        appendLog("🎉 战斗胜利！");

        // 1. 指令加护：按危险度 1:1 发放
        int graceGain = Math.max(1, player.difficulty) / 10;
        indexFingerLevel.grace += graceGain;
        store.saveStats(indexFingerLevel);
        appendLog("🏵 获得指令加护 +" + graceGain);

        // 2. "眼"：按危险度换算，1点危险度=1万眼
        long eyeGain = (long) Math.max(1, player.difficulty) * 10000L;
        long totalEyes = store.loadEyes() + eyeGain;
        store.saveEyes(totalEyes);
        appendLog("👁 获得「眼」+" + (eyeGain / 10000) + "万，当前共 " + (totalEyes / 10000) + "万");

        // 3. 掉卡：掉落概率、掉落质量都跟危险度走，掉的卡进"大牌库"（不直接进出战牌组，得去养成界面手动装备）
        int dropChance = Math.min(90, 20 + graceGain * 3);
        if (mechanics.randint(1, 100) <= dropChance) {
            String dropKey = rollRewardCardKey(player.difficulty);
            List<String> collection = store.loadCardCollection();
            collection.add(dropKey);
            store.saveCardCollection(collection);
            String qualityTag = FortunaCards.rarenessOf(dropKey) == 3 ? "🌟金卡"
                    : FortunaCards.rarenessOf(dropKey) == 2 ? "🔷稀有" : FortunaCards.rarenessOf(dropKey) == 1 ? "⚪普通" : "⚙️测试";
            appendLog("🃏 获得新卡：" + qualityTag + " " + FortunaCards.displayName(dropKey) + "（已放入牌库，去养成界面装备）");
            Toast.makeText(this, "获得新卡：" + FortunaCards.displayName(dropKey), Toast.LENGTH_LONG).show();
        }

        // 战斗结束，快照没意义了，清掉；危险度已经在spawnByDifficulty时存过，留着给下一场用
        store.clearBattleSnapshot();
        battleOver = true;
        showBattleOverSpinner(NEXT_BATTLE_LABEL);
        refreshAllUI();
    }

    /** 按危险度加权抽一张奖励卡的key：危险度越高，抽到稀有/金卡的权重越大 */
    private String rollRewardCardKey(int difficulty) {
        if(difficulty > 50) difficulty = 50;
        int goldWeight = Math.min(50, difficulty);          // 危险度50时金卡权重封顶50
        int silverWeight = Math.min(70, 20 + difficulty);    // 稀有权重起步20，随危险度涨
        int commonWeight = Math.max(10, 100 - goldWeight - silverWeight);
        int totalWeight = goldWeight + silverWeight + commonWeight;

        int roll = mechanics.randint(1, totalWeight);
        int targetRareness;
        if (roll <= goldWeight) targetRareness = 3;
        else if (roll <= goldWeight + silverWeight) targetRareness = 2;
        else targetRareness = 1;

        List<String> candidates = new ArrayList<>();
        for (String key : FortunaCards.REWARD_POOL_KEYS) {
            if (FortunaCards.rarenessOf(key) == targetRareness) candidates.add(key);
        }
        if (candidates.isEmpty()) {
            // 兜底：万一某个稀有度暂时没卡，退化成从整个奖励池里随便抽
            candidates.addAll(java.util.Arrays.asList(FortunaCards.REWARD_POOL_KEYS));
        }
        return candidates.get(mechanics.randint(0, candidates.size() - 1));
    }

    private void onDefeat() {
        Toast.makeText(this, "💀 你被击倒了", Toast.LENGTH_LONG).show();
        appendLog("💀 战斗失败，危险度重置为0");
        store.saveBattleDifficulty(0);
        store.clearBattleSnapshot();
        battleOver = true;
        showBattleOverSpinner(RETRY_LABEL);
        refreshAllUI();
    }

    private void showBattleOverSpinner(String label) {
        spinnerAdapter.clear();
        spinnerAdapter.add(label);
        spinnerAdapter.notifyDataSetChanged();
        btnConfirm.setText(label);
    }

    // ===================== 存档：战斗快照 =====================

    /** 把当前战斗现场（玩家/敌人属性 + 手牌/抽牌堆/弃牌堆的key列表）整体写入store */
    private void persistBattleSnapshot() {
        try {
            JSONObject root = new JSONObject();
            root.put("player", EntitySerializer.toJson(player));
            root.put("enemy", EntitySerializer.toJson(enemy));
            root.put("hand", keysOf(deck.hand));
            root.put("drawPile", keysOf(deck.drawPile));
            root.put("discardPile", keysOf(deck.discardPile));
            store.saveBattleSnapshot(root.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONArray keysOf(List<Card> cards) {
        JSONArray arr = new JSONArray();
        for (Card c : cards) arr.put(c.key);
        return arr;
    }

    /** 有快照就整体恢复现场，返回true；没有快照返回false交给调用方走全新开局 */
    private boolean restoreBattleSnapshot() {
        String raw = store.loadBattleSnapshot();
        if (raw == null) return false;
        try {
            JSONObject root = new JSONObject(raw);
            player = EntitySerializer.fromJson(root.getJSONObject("player"), mechanics, this);
            enemy = EntitySerializer.fromJson(root.getJSONObject("enemy"), mechanics, this);

            deck.hand.clear();
            deck.drawPile.clear();
            deck.discardPile.clear();
            loadKeysInto(root.optJSONArray("hand"), deck.hand);
            loadKeysInto(root.optJSONArray("drawPile"), deck.drawPile);
            loadKeysInto(root.optJSONArray("discardPile"), deck.discardPile);

            // current_intent里的方法引用没法存，恢复时重新算一次（会有一次轻微的副作用重复，可接受）
            enemy.current_intent = computeEnemyIntent();
            player.buff.m = mechanics;
            enemy.buff.m = mechanics;
            player.m = mechanics;
            player.m = mechanics;
            appendLog("========== 已恢复上次未完成的战斗 ==========");
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadKeysInto(JSONArray arr, List<Card> target) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String key = arr.optString(i, "strike");
            target.add(cardDefs.createByKey(key, deck, this::appendLog));
        }
    }

    // ===================== UI辅助 =====================

    private List<String> currentSpinnerNames() {
        List<String> names = new ArrayList<>();
        for (Card c : deck.hand) {
            names.add(c.name + " (⚡" + c.cost + ")");
        }
        if (names.isEmpty()) names.add("（无手牌）");
        names.add(END_TURN_LABEL);
        return names;
    }

    public void updateSpinner() {
        if (battleOver) return;
        List<String> names = currentSpinnerNames();
        spinnerAdapter.clear();
        spinnerAdapter.addAll(names);
        spinnerAdapter.notifyDataSetChanged();
        btnConfirm.setText("确认使用书页");
    }

    public void appendLog(String msg) {
        if (tvCombatLog == null) return;
        String current = tvCombatLog.getText().toString();
        if (current.equals("rnfmabj")) current = "";
        tvCombatLog.setText(current + "\n" + msg);
        final int scrollAmount = tvCombatLog.getLayout() != null
                ? tvCombatLog.getLayout().getLineTop(tvCombatLog.getLineCount()) - tvCombatLog.getHeight()
                : 0;
        tvCombatLog.scrollTo(0, Math.max(scrollAmount, 0));
    }

    private String buildStatusChips(Entity e) {
        StringBuilder sb = new StringBuilder();
        sb.append("🧠理智 ").append(e.sanity).append("  ");
        sb.append(e.getAmmo()).append("  ");
        if (e.stagger_count < 3){
            sb.append("😖下次混乱还需造成").append(e.health - e.staggerLine[e.stagger_count]*e.outside_max_hp).append("伤害  ");
        }
        if (e.block > 0) sb.append("🛡护盾").append(e.block).append("  ");
        if (e.strength != 0) sb.append("💪力量").append(e.strength).append("  ");
        if (e.this_turn_strength != 0) sb.append("⚡本回合力量").append(e.this_turn_strength).append("  ");
        if (e.swift != 0) sb.append("⬆迅捷").append(e.swift).append("  ");
        if (e.burn_term > 0) sb.append("🔥灼烧").append(e.burn_strength).append("x").append(e.burn_term).append("  ");
        if (e.bleed_term > 0) sb.append("🩸流血").append(e.bleed_strength).append("x").append(e.bleed_term).append("  ");
        if (e.tremor_term > 0) sb.append("🌀震颤").append(e.tremor_strength).append("x").append(e.tremor_term).append("  ");
        if (e.sinking_term > 0) sb.append("💧沉沦").append(e.sinking_strength).append("x").append(e.sinking_term).append("  ");
        if (e.rapture_term > 0) sb.append("💔破裂").append(e.rapture_strength).append("x").append(e.rapture_term).append("  ");
        if (e.poise_term > 0) sb.append("🌫️呼吸法").append(e.poise_strength).append("x").append(e.poise_term).append("  ");
        if (e.blade > 0) sb.append("🗡剑痕").append(e.blade).append("  ");
        if (e.shin > 0) sb.append("💛心").append(e.shin).append("  ");
        if (e.charge_term > 0 || e.charge_strength > 1) sb.append("🔋充能").append(e.charge_strength).append("-").append(e.charge_term).append("  ");
        if (e.stagger_panic_term > 0) sb.append("😵混乱层数").append(e.stagger_panic_term).append("  ");
        if (e.restrictions > 0) sb.append("⛓制约").append(e.restrictions).append("  ");
        if (e.krama > 0) sb.append("‼️业").append(e.krama).append("  ");
        if (e.EntityId == 3) sb.append("⚜️代行:赫尔墨斯").append(e.grace).append("  ");
        if (e.tianjiustarblade > 0) sb.append("🔪天究星刀").append(e.tianjiustarblade).append("  ");
        ArrayList<String> a = e.buff.getString();
        for(int i = 0; i < a.size(); i++){
            sb.append(a.get(i));
        }
        if (sb.length() == 0) sb.append("（无异常状态）");
        return sb.toString().trim();
    }

    public void refreshAllUI() {
        tvKarma.setText("业(福尔图娜): " + indexFingerLevel.krama);
        tvBlessing.setText("指令加护: " + indexFingerLevel.grace);
        String name = indexFingerLevel.getName();
        String codename = store.loadCodename();
        if (codename != null && !codename.isEmpty()) name += " " + codename;
        tvTitle.setText(name);

        tvHP.setText("生命值: " + player.hp + "/" + player.max_hp);
        //tvSanity.setText("理智值: " + player.sanity);

        if (hpBar != null) {
            hpBar.setMax(Math.max(1, player.max_hp));
            hpBar.setProgress(Math.max(0, Math.min(player.hp, player.max_hp)));
        }
//        if (sanBar != null) {
//            sanBar.setMax(90);
//            sanBar.setProgress(Math.max(0, Math.min(player.sanity + 45, 90)));
//        }

        int exp = indexFingerLevel.grace;
        int maxExp = 100;
        tvEXP.setText("EXP: " + exp + "/" + maxExp);
        tvLevel.setText("Lv. " + indexFingerLevel.level);
        if (expBar != null) {
            expBar.setMax(maxExp);
            expBar.setProgress(Math.max(0, Math.min(exp, maxExp)));
        }

        tvLight.setText("光芒: \n" + getLight());
        if (tvPlayerStatus != null) tvPlayerStatus.setText(buildStatusChips(player));

        if (enemy != null) {
            tvEnemyName.setText("敌人：" + enemy.name + "  " + enemy.hp + "/" + enemy.max_hp
                    + (enemy.block > 0 ? "  护盾:" + enemy.block : ""));
            String intentText = (enemy.current_intent != null) ? enemy.current_intent.description : "——";
            tvEnemyIntent.setText("意图：" + (battleOver ? "战斗已结束" : intentText));
            if (tvEnemyStatus != null) tvEnemyStatus.setText(buildStatusChips(enemy));
            if (enemyHpBar != null) {
                enemyHpBar.setMax(Math.max(1, enemy.max_hp));
                enemyHpBar.setProgress(Math.max(0, enemy.hp));
            }
        }
    }

    public String getLight(){
        String output = "";
        for(int i = 0; i < player.energy && i < player.max_energy; i++){
            output += "🟡";
        }
        if (player.max_energy <= player.energy){
            for(int i = 0; i < player.energy - player.max_energy; i++){
                output += "🟣";
            }
        }else{
            for(int i = 0; i < player.max_energy - player.energy; i++){
                output += "⚪";
            }
        }
        return output;
    }
}