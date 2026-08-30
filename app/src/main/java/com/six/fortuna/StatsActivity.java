package com.six.fortuna;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.dragonbones.animation.Animation;
import com.dragonbones.animation.WorldClock;
import com.dragonbones.armature.Armature;
import com.dragonbones.model.AnimationData;
import com.dragonbones.model.ArmatureData;
import com.dragonbones.model.DragonBonesData;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private String END_TURN_LABEL;
    private String NEXT_BATTLE_LABEL;
    private String RETRY_LABEL;
    private static final int HAND_SIZE = 5;

    private int currentSongResId;
    private boolean wasPlayingBeforeStop = false;
    private boolean playerArmatureLoadFailed = false;
    private MediaPlayer mediaPlayer;

    private static final String[] STARTER_DECK = {
            "cogito", "cogito", "ququ"
    };

    private TextView tvTitle, tvKarma, tvBlessing;
    private TextView tvHP, tvSanity, tvEXP, tvLevel, tvLight;
    private TextView tvCombatLog;
    private TextView tvEnemyName, tvEnemyIntent, tvEnemyStatus, tvPlayerStatus;
    private ProgressBar enemyHpBar;
    private Spinner spinnerCards;
    private static final String PREFS_NAME = "fortuna_settings";
    private static final String KEY_ANIMATED_BATTLE = "animated_battle_mode";
    private boolean animatedMode;

    private com.six.fortuna.dragonbones.DragonBonesView playerDragonBonesView;
    private com.dragonbones.armature.Armature playerArmature;
    private com.six.fortuna.dragonbones.DragonBonesView enemyDragonBonesView;
    private com.dragonbones.armature.Armature enemyArmature;
    private boolean enemyArmatureLoadFailed = false;
    private final android.os.Handler clashHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.ArrayDeque<String> logQueue = new java.util.ArrayDeque<>();
    private boolean isPlayingLog = false;
    private Button btnConfirm;
    private ProgressBar hpBar, sanBar, expBar;
    private ArrayAdapter<String> spinnerAdapter;

    private PrescriptStore store;
    private IndexFingerLevel_CN indexFingerLevel;

    public Mechanics mechanics;
    private CardDeck deck;
    private FortunaCards cardDefs;
    private FortunaEnemies enemyDefs;
    private Entity player;
    private Entity enemy;

    private boolean battleOver = false;
    private boolean isAttackAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);
        END_TURN_LABEL = "🔚 " + getString(R.string.end_turn);
        NEXT_BATTLE_LABEL = "🔁 " + getString(R.string.nextbattle);
        RETRY_LABEL = "🔁 " + getString(R.string.restart);

        store = new PrescriptStore(this);
        indexFingerLevel = new IndexFingerLevel_CN();
        store.loadStats(indexFingerLevel);
        mediaPlayer = MediaPlayer.create(this, R.raw.lobotomy_1);
        currentSongResId = R.raw.lobotomy_1;
        mediaPlayer.setVolume(store.loadVolume(), store.loadVolume());
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        bindViews();
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        animatedMode = prefs.getBoolean(KEY_ANIMATED_BATTLE, false);

        Switch switchAnimatedBattle = findViewById(R.id.switch_animated_battle);
        switchAnimatedBattle.setChecked(animatedMode);
        applyBattleModeUI();

        switchAnimatedBattle.setOnCheckedChangeListener((btn, checked) -> {
            animatedMode = checked;
            prefs.edit().putBoolean(KEY_ANIMATED_BATTLE, checked).apply();
            applyBattleModeUI();
            if (checked && !animatedMode) {
                switchAnimatedBattle.setChecked(false);
            }
        });
        tvCombatLog.setMovementMethod(new ScrollingMovementMethod());

        mechanics = new Mechanics(this::appendLog, getResources());
        cardDefs = new FortunaCards(mechanics, this);
        enemyDefs = new FortunaEnemies(mechanics, this);
        deck = new CardDeck(mechanics);

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCards.setAdapter(spinnerAdapter);

        btnConfirm.setOnClickListener(v -> {
            if (isPlayingLog) {
                revealNextLine();
                return;
            }
            if (battleOver) {
                startNewBattle();
                return;
            }
            if (isAttackAnimating) {
                Toast.makeText(this, "动画播放中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerCards.getSelectedItemPosition();
            List<String> names = currentSpinnerNames();
            if (pos < 0 || pos >= names.size()) {
                Toast.makeText(this, getString(R.string.noChoose), Toast.LENGTH_SHORT).show();
                return;
            }
            if (names.get(pos).equals(END_TURN_LABEL)) {
                endTurn();
                return;
            }
            if (deck.hand.isEmpty() || pos >= deck.hand.size()) {
                Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_SHORT).show();
                return;
            }
            if (player.stagger_panic_term > 0) {
                Toast.makeText(this, getString(R.string.onStagger), Toast.LENGTH_SHORT).show();
                return;
            }
            useCard(deck.hand.get(pos));
        });

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
        findViewById(R.id.nav_stats).setOnClickListener(v -> {});
        findViewById(R.id.nav_setting).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingActivity.class));
        });
        Button navDeck = findViewById(R.id.nav_deck);
        if (navDeck != null) {
            navDeck.setOnClickListener(v -> {
                startActivity(new Intent(this, DeckManageActivity.class));
            });
        }

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
        playerDragonBonesView = findViewById(R.id.player_dragon_bones_view);
        enemyDragonBonesView = findViewById(R.id.enemy_dragon_bones_view);
        makeTransparentAndLayered(playerDragonBonesView, /*onTop=*/false);
        makeTransparentAndLayered(enemyDragonBonesView, /*onTop=*/true);
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
        expBar = findViewById(R.id.EXPBar);
    }

    // ===================== 开局 / 重开 =====================

    private void startNewBattle() {
        battleOver = false;
        initPlayer();
        initDeckFromOwnedCards();
        spawnByDifficulty();
        playSong(this, R.raw.lobotomy_1, mediaPlayer);
        enemy.current_intent = computeEnemyIntent();
        appendLog(String.format(getString(R.string.log_battle_start), getString(R.string.battlestart), player.difficulty));
        startTurn();
    }

    private void initPlayer() {
        player = new Entity(mechanics, this, getResources());
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
            appendLog(String.format(getString(R.string.log_restrictions_note), player.difficulty));
        } else if (player.difficulty <= 50) {
            if (mechanics.randint(100, 799) <= 325) {
                NormalBattle();
            }
            EliteBattle();
            player.difficulty += mechanics.randint(1, 5);
            appendLog(String.format(getString(R.string.log_restrictions_elite), player.difficulty));
        } else {
            player.difficulty += mechanics.randint(4, 12);
            appendLog(String.format(getString(R.string.log_restrictions_boss), player.difficulty));
            if (mechanics.randint(1, 100) < player.difficulty - 50 && !(mechanics.randint(1, 100) == 1)) {
                player.restrictions = (int) ((1 + 0.01 * mechanics.randint(1, 50)) * (player.difficulty / 10));
                BossBattle();
            } else {
                player.restrictions = (int) ((1 + 0.01 * mechanics.randint(1, 50)) * (player.difficulty / 10));
                if (mechanics.randint(225, 325) == 325) {
                    NormalBattle();
                } else {
                    EliteBattle();
                }
            }
        }
        store.saveBattleDifficulty(player.difficulty);
    }

    private EnemyAction computeEnemyIntent() {
        switch (enemy.EntityId) {
            case 1:
                return enemyDefs.brainHajimi(enemy, player);
            case 2:
                playSong(this, R.raw.albina, mediaPlayer);
                return enemyDefs.brainAlbina(enemy, player);
            case 3:
                playSong(this, R.raw.saikai, mediaPlayer);
                return enemyDefs.brainRein(enemy, player);
            case 4:
                playSong(this, R.raw.libraryabnormality_1, mediaPlayer);
                return enemyDefs.barinForgottenKiller(enemy, player);
            case 5:
                playSong(this, R.raw.valencina, mediaPlayer);
                return enemyDefs.brainValencina(enemy, player);
            case 6:
                playSong(this, R.raw.libraryabnormality_1, mediaPlayer);
                return enemyDefs.hearts(enemy, player);
            case 7:
                playSong(this, R.raw.araya, mediaPlayer);
                return enemyDefs.brainYoshide(enemy, player);
            case 8:
                return enemyDefs.brainQuQu(enemy, player);
            case 9:
                return enemyDefs.brainThunderSpirit(enemy, player);
            case 10:
                return enemyDefs.brainGOD(enemy, player);
            case 11:
                EnemyAction temp = enemyDefs.brainKromo(enemy, player);
                if (enemy.countC == 2) {
                    playSong(this, R.raw.betweentwoworlds_2, mediaPlayer);
                } else {
                    playSong(this, R.raw.betweentwoworlds_1, mediaPlayer);
                }
                return temp;
            default:
                return null;
        }
    }

    private void NormalBattle() {
        switch (mechanics.randint(1, 3)) {
            case 1:
                spawnHajimi();
                break;
            case 2:
                spawnEnemy(getString(R.string.enemy_heart), 10000, new double[]{0.3, 0.2, 0.1}, 6);
                break;
            case 3:
                spawnEnemy(getString(R.string.enemy_thunder_spirit), 10000, new double[]{0.6, 0.4, 0.2}, 9);
                break;
        }
    }

    private void spawnHajimi() {
        enemy = new Entity(mechanics, this, getResources());
        enemy.name = getString(R.string.enemy_hajimi);
        enemy.max_hp = 10000;
        enemy.EntityId = 1;
        enemy.outside_max_hp = 10000;
        enemy.hp = enemy.outside_max_hp;
        enemy.health = enemy.hp;
        enemy.strength = 2;
        enemy.swift = 0;
        enemy.staggerLine = new double[]{0.75, 0.5, 0.25};
    }

    private void EliteBattle() {
        switch (mechanics.randint(1, 3)) {
            case 1:
                spawnAlbina();
                break;
            case 2:
                spawnEnemy(getString(R.string.enemy_forgotten_killer), 300000, new double[]{0.2, 0.4, 0.6}, 4);
                break;
            case 3:
                spawnEnemy(getString(R.string.enemy_ailan_worm), 100000, new double[]{0, 0, 0}, 8);
                break;
        }
    }

    private void spawnAlbina() {
        spawnEnemy(getString(R.string.enemy_albina), 20000, new double[]{0.8, 0.6, 0.4}, 2);
    }

    private void BossBattle() {
        switch (mechanics.randint(1, 5)) {
            case 1:
                spawnRein();
                break;
            case 2:
                spawnEnemy(getString(R.string.enemy_valencina), 800000, new double[]{0.9, 0.5, 0.2}, 5);
                break;
            case 3:
                spawnEnemy(getString(R.string.enemy_yoshide), 1000000, new double[]{-0.2, -0.5, -1.0}, 7);
                break;
            case 4:
                spawnEnemy(getString(R.string.enemy_god), 1000000, new double[]{0.8, 0.6, 0.3}, 10);
                break;
            case 5:
                spawnEnemy(getString(R.string.enemy_kromer), 1000000, new double[]{0.7, 0.3, 0.0}, 11);
                break;
        }
    }

    private void spawnRein() {
        spawnEnemy(getString(R.string.enemy_rein), 1000000, new double[]{0.7, 0.5, 0.2}, 3);
    }

    private void spawnEnemy(String name, int outside_max_hp, double[] staggerLine, int id) {
        enemy = new Entity(mechanics, this, getResources());
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
        appendLog(getString(R.string.log_turn_start));
        appendLog(String.format(getString(R.string.log_deck_size), deck.drawPile.size()));
        appendLog(String.format(getString(R.string.log_discard_size), deck.discardPile.size()));
        if (player.buff.UnlockedHealth > 0 || player.hp < 0) {
            player.buff.UnlockedHealth = 0;
            player.buff.lockedHealth--;
            player.hp = (int) (player.max_hp * 0.5);
            if (player.buff.bloodstainedTears > 0) {
                deck.addCard(cardDefs.BloodstainedTears_finale(), mechanics.logger);
            }
        }
        player.buff.turnStartActivate(enemy);
        enemy.buff.turnStartActivate(player);
        player.this_turn_strength = 0;
        enemy.this_turn_strength = 0;
        player.energy += player.gain_energy;
        player.this_turn_strength = 0;
        deck.drawCards(HAND_SIZE - deck.hand.size(), player, this::appendLog);
        if (player.energy >= player.max_energy) {
            player.energy = player.max_energy;
        }
        if (player.restrictions >= 50) {
            player.energy--;
        }
        if (player.restrictions >= 100) {
            for (int i = 0; i < Math.min(Math.max(1, player.restrictions / 300), 3); i++) {
                deck.addCard(new Card("null", "制约", 1, null, -1, 1), mechanics.logger);
            }
        }
        if (player.sanity <= -45) {
            player.stagger_panic_term++;
            player.sanity = 0;
        }
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();
    }

    private void endTurn() {
        appendLog(getString(R.string.log_turn_end));
        deck.discardHand();
        player.buff.turnEndActivate();
        enemy.buff.turnEndActivate();

        if (player.energy >= player.max_energy) {
            player.energy = player.max_energy;
        }

        if ((enemy.buff.lockedHealth > 0 || enemy.hp > 0) && (enemy.buff.UnlockedHealth == 0) && enemy.current_intent != null && enemy.current_intent.execute != null) {
            appendLog(String.format(getString(R.string.log_enemy_action), enemy.name, enemy.current_intent.description));
            playEnemyAttackAnimation();
            enemy.current_intent.execute.execute(enemy, player);
            if (enemy.buff.cibei > 0) {
                if (enemy.countB == 1) {
                    Toast.makeText(this, getString(R.string.toast_ailan_not_district), Toast.LENGTH_SHORT).show();
                    appendLog(getString(R.string.log_compassion_mass));
                    for (int i = 0; i < enemy.buff.cibei; i++) {
                        appendLog(getString(R.string.log_compassion_trigger));
                        enemy.this_turn_strength += 3;
                        if (mechanics.dealDamage(30, player, enemy)) {
                            if (animatedMode) showDamageNumber(playerDragonBonesView, 30, false);
                            player.tremor_term += 3;
                            player.tremor_strength += 5 + enemy.strength;
                            mechanics.amplitudeConversion(player, 4);
                            mechanics.tremorBurst(player);
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.toast_grace_compassion), Toast.LENGTH_SHORT).show();
                    appendLog(getString(R.string.log_compassion_eternal));
                    for (int i = 0; i < enemy.buff.cibei; i++) {
                        enemy.this_turn_strength--;
                        if (enemy.buff.cibei > 15) {
                            enemy.this_turn_strength += enemy.buff.cibei - 15;
                            player.this_turn_strength -= enemy.buff.cibei - 15;
                            enemy.buff.cibei = 15;
                        }
                        appendLog(getString(R.string.log_compassion_open_source));
                        if (mechanics.dealDamage(30, player, enemy)) {
                            if (animatedMode) showDamageNumber(playerDragonBonesView, 30, false);
                            player.tremor_term += 3;
                            player.tremor_strength += 5 + enemy.strength;
                            mechanics.amplitudeConversion(player, 4);
                            mechanics.tremorBurst(player);
                        }
                        if (i > 15) {
                            break;
                        }
                    }
                }
            }
        }

        if (player.stagger_panic_term > 0) {
            player.stagger_panic_term--;
        }
        if (enemy.stagger_panic_term > 0) {
            enemy.stagger_panic_term--;
        }

        mechanics.burn(player);
        mechanics.burn(enemy);
        if (player.tremor_term > 0) {
            player.tremor_term--;
        }
        if (enemy.tremor_term > 0) {
            enemy.tremor_term--;
        }

        mechanics.ifStaggered(player);
        mechanics.ifStaggered(enemy);

        refreshAllUI();

        if (player.hp <= 0 && player.buff.lockedHealth <= 0) {
            player.hp = 0;
            onDefeat();
            return;
        }
        if (enemy.hp <= 0) {
            if (enemy.EntityId == 7 && enemy.block > 0) {
                enemy.max_hp = enemy.block;
                enemy.hp = enemy.max_hp;
                enemy.block = 0;
            } else {
                if (enemy.buff.lockedHealth > 0) {
                    enemy.buff.UnlockedHealth++;
                } else {
                    enemy.hp = 0;
                    onVictory();
                    return;
                }
            }
        }

        if (enemy.stagger_panic_term <= 0) enemy.current_intent = computeEnemyIntent();
        startTurn();
    }

    private void useCard(Card card) {
        if (player.energy < card.cost) {
            Toast.makeText(this, String.format(getString(R.string.toast_energy_insufficient), card.cost), Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.sanity <= -45) {
            player.stagger_panic_term++;
            player.sanity = 0;
        }
        if (player.stagger_panic_term > 0) {
            appendLog(getString(R.string.toast_staggered));
            return;
        }
        player.energy -= card.cost;
        playPlayerAttackAnimation();
        card.play.play(player, enemy);
        if (player.buff.cibei > 0) {
            Toast.makeText(this, getString(R.string.toast_chase_laugh), Toast.LENGTH_SHORT).show();
            appendLog(getString(R.string.log_compassion_seeing));
            for (int i = 0; i < player.buff.cibei; i++) {
                appendLog(getString(R.string.log_compassion_trigger));
                useCard(cardDefs.zhuizhui(), 1);
            }
        }
        deck.playFromHand(card);
        store.saveBattleDifficulty(player.difficulty);
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();

        appendLog(String.format(getString(R.string.log_deck_size), deck.drawPile.size()));
        appendLog(String.format(getString(R.string.log_discard_size), deck.discardPile.size()));

        if (enemy.hp <= 0) {
            if (enemy.EntityId == 7 && enemy.block > 0) {
                enemy.max_hp = enemy.block;
                enemy.hp = enemy.max_hp;
                enemy.block = 0;
            } else {
                if (enemy.buff.lockedHealth > 0) return;
                enemy.hp = 0;
                onVictory();
            }
        }
    }

    // 重载：只执行效果，不播放动画（用于连击）
    private void useCard(Card card, int i) {
        if (player.energy < card.cost) {
            Toast.makeText(this, String.format(getString(R.string.toast_energy_insufficient), card.cost), Toast.LENGTH_SHORT).show();
            return;
        }
        player.energy -= card.cost;
        // 不播放动画
        card.play.play(player, enemy);
        deck.playFromHand(card);
        store.saveBattleDifficulty(player.difficulty);
        updateSpinner();
        refreshAllUI();
        persistBattleSnapshot();

        appendLog(String.format(getString(R.string.log_deck_size), deck.drawPile.size()));
        appendLog(String.format(getString(R.string.log_discard_size), deck.discardPile.size()));

        if (enemy.hp <= 0) {
            enemy.hp = 0;
            onVictory();
        }
    }

    // ===================== 胜负结算 =====================

    private void onVictory() {
        Toast.makeText(this, String.format(getString(R.string.toast_victory), enemy.name), Toast.LENGTH_LONG).show();
        appendLog(getString(R.string.log_victory));

        int graceGain = Math.max(1, player.difficulty) / 10;
        indexFingerLevel.grace += graceGain;
        store.saveStats(indexFingerLevel);
        appendLog(String.format(getString(R.string.log_grace_gain), graceGain));

        long eyeGain = (long) Math.max(1, player.difficulty) * 10000L;
        long totalEyes = store.loadEyes() + eyeGain;
        store.saveEyes(totalEyes);
        appendLog(String.format(getString(R.string.log_eye_gain), eyeGain / 10000, totalEyes / 10000));

        int dropChance = Math.min(90, 20 + graceGain * 3);
        if (mechanics.randint(1, 100) <= dropChance) {
            String dropKey = rollRewardCardKey(player.difficulty);
            List<String> collection = store.loadCardCollection();
            collection.add(dropKey);
            store.saveCardCollection(collection);
            String qualityTag = FortunaCards.rarenessOf(dropKey) == 3 ? "🌟金卡"
                    : FortunaCards.rarenessOf(dropKey) == 2 ? "🔷稀有" : FortunaCards.rarenessOf(dropKey) == 1 ? "⚪普通" : "⚙️测试";
            String displayName = cardDefs.displayName(dropKey, getResources());
            appendLog(String.format(getString(R.string.log_new_card), qualityTag, displayName));
            Toast.makeText(this, String.format(getString(R.string.toast_new_card), displayName), Toast.LENGTH_LONG).show();
        }

        store.clearBattleSnapshot();
        battleOver = true;
        showBattleOverSpinner(NEXT_BATTLE_LABEL);
        refreshAllUI();
    }

    private String rollRewardCardKey(int difficulty) {
        if (difficulty > 50) difficulty = 50;
        int goldWeight = Math.min(3, difficulty / 75);
        int silverWeight = Math.min(40, difficulty);
        int commonWeight = Math.max(1, 100 - goldWeight - silverWeight);
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
            candidates.addAll(java.util.Arrays.asList(FortunaCards.REWARD_POOL_KEYS));
        }
        return candidates.get(mechanics.randint(0, candidates.size() - 1));
    }

    private void onDefeat() {
        Toast.makeText(this, getString(R.string.toast_defeat), Toast.LENGTH_LONG).show();
        appendLog(getString(R.string.log_defeat));
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

            enemy.current_intent = computeEnemyIntent();
            player.buff.m = mechanics;
            enemy.buff.m = mechanics;
            player.m = mechanics;
            enemy.m = mechanics;
            appendLog(getString(R.string.log_restored_battle));
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
        btnConfirm.setText(getString(R.string.button_use_card));
    }

    public void appendLog(String msg) {
        if (animatedMode) {
            android.util.Log.d("BattleLog", msg);
            return;
        }
        logQueue.add(msg);
        if (!isPlayingLog) {
            startLogPlayback();
        }
    }

    private void startLogPlayback() {
        isPlayingLog = true;
        spinnerCards.setEnabled(false);
        while (isPlayingLog) {
            revealNextLine();
        }
    }

    private void revealNextLine() {
        if (logQueue.isEmpty()) {
            isPlayingLog = false;
            spinnerCards.setEnabled(true);
            if (!battleOver) {
                btnConfirm.setText(getString(R.string.button_use_card));
            }
            return;
        }

        String msg = logQueue.poll();
        if (tvCombatLog != null) {
            String current = tvCombatLog.getText().toString();
            if (current.equals("rnfmabj")) current = "";
            tvCombatLog.setText(current + "\n" + msg);
            final int scrollAmount = tvCombatLog.getLayout() != null
                    ? tvCombatLog.getLayout().getLineTop(tvCombatLog.getLineCount()) - tvCombatLog.getHeight()
                    : 0;
            tvCombatLog.scrollTo(0, Math.max(scrollAmount, 0));
        }

        if (!logQueue.isEmpty()) {
            btnConfirm.setText("▶ " + getString(R.string.next_step));
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ========== 脚下半椭圆血条 ==========

    /**
     * 半椭圆血条：用一个高度翻倍的完整椭圆，view自身高度只截到上半部分，
     * 视觉上就是"半个椭圆"贴在角色脚下当血条阴影用。
     * hpFraction 决定填色部分的宽度（从左往右扫，而不是径向），血量越低填的越窄。
     */
    private static class HalfEllipseHpBar extends android.view.View {
        private float hpFraction = 1f;
        private final android.graphics.Paint bgPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint fgPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        HalfEllipseHpBar(android.content.Context ctx) {
            super(ctx);
            bgPaint.setColor(0xAA1A1A1A);
            fgPaint.setColor(0xFF50D050);
        }

        void setHpFraction(float f) {
            hpFraction = Math.max(0f, Math.min(1f, f));
            invalidate();
        }

        void setFgColor(int color) {
            fgPaint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;
            android.graphics.RectF fullOval = new android.graphics.RectF(0, 0, w, h * 2f);
            canvas.drawOval(fullOval, bgPaint);
            if (hpFraction > 0f) {
                canvas.save();
                canvas.clipRect(0, 0, w * hpFraction, h);
                canvas.drawOval(fullOval, fgPaint);
                canvas.restore();
            }
        }
    }

    private HalfEllipseHpBar playerHpEllipse, enemyHpEllipse;

    /**
     * 把血条椭圆加到 DragonBonesView 的父容器里（要求父容器是 FrameLayout 才能用绝对坐标摆放），
     * 横向对齐用跟角色相同的 STAND_X，纵向摆在父容器底部往上一点点当"脚下"的位置。
     * 只调用一次；宽高、纵向偏移自己按实际角色贴图大小调 ELLIPSE_* 那几个常量。
     */
    private static final int ELLIPSE_WIDTH_DP = 140;
    private static final int ELLIPSE_HEIGHT_DP = 30;
    private static final int ELLIPSE_BOTTOM_MARGIN_DP = 40; // 距容器底部多远，也就是"脚底"大概在哪

    private void ensureHpEllipsesCreated() {
        if (playerHpEllipse != null || playerDragonBonesView == null) return;
        android.view.ViewParent parentRaw = playerDragonBonesView.getParent();
        if (!(parentRaw instanceof android.widget.FrameLayout)) {
            // 父容器不是 FrameLayout 就没法用 translationX/Y 摆放，兜底不加，避免崩溃
            return;
        }
        android.widget.FrameLayout parent = (android.widget.FrameLayout) parentRaw;

        playerHpEllipse = new HalfEllipseHpBar(this);
        enemyHpEllipse = new HalfEllipseHpBar(this);
        enemyHpEllipse.setFgColor(0xFFE05050); // 敌人血条用红色，跟玩家的绿色区分

        android.widget.FrameLayout.LayoutParams lp1 = new android.widget.FrameLayout.LayoutParams(
                dp(ELLIPSE_WIDTH_DP), dp(ELLIPSE_HEIGHT_DP));
        lp1.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        lp1.bottomMargin = dp(ELLIPSE_BOTTOM_MARGIN_DP);
        playerHpEllipse.setLayoutParams(lp1);
        playerHpEllipse.setTranslationX(PLAYER_STAND_X);

        android.widget.FrameLayout.LayoutParams lp2 = new android.widget.FrameLayout.LayoutParams(
                dp(ELLIPSE_WIDTH_DP), dp(ELLIPSE_HEIGHT_DP));
        lp2.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        lp2.bottomMargin = dp(ELLIPSE_BOTTOM_MARGIN_DP);
        enemyHpEllipse.setLayoutParams(lp2);
        enemyHpEllipse.setTranslationX(ENEMY_STAND_X);

        // 加在 DragonBonesView 之前（zIndex更低），这样角色脚踩在椭圆上面而不是盖住椭圆看不见
        parent.addView(playerHpEllipse, parent.indexOfChild(playerDragonBonesView));
        parent.addView(enemyHpEllipse, parent.indexOfChild(enemyDragonBonesView));
    }

    private void updateHpEllipses() {
        if (!animatedMode) return;
        ensureHpEllipsesCreated();
        if (playerHpEllipse != null) {
            playerHpEllipse.setHpFraction(player.max_hp > 0 ? (float) player.hp / player.max_hp : 0f);
        }
        if (enemyHpEllipse != null && enemy != null) {
            enemyHpEllipse.setHpFraction(enemy.max_hp > 0 ? (float) enemy.hp / enemy.max_hp : 0f);
        }
    }


    // ========== 伤害数字跳字 ==========

    /**
     * 在 anchorView 所在位置附近弹一个飘字，往上飘、渐隐后自动移除。
     * @param anchorView 从哪个view的位置弹出来（比如 enemyDragonBonesView，打谁就传谁）
     * @param amount 伤害/治疗数值，正数按伤害显示，负数按治疗显示（自己按需传）
     * @param isCrit 是否暴击/强化攻击，暴击字更大、颜色更亮、带一点缩放弹跳
     */
    private void showDamageNumber(android.view.View anchorView, int amount, boolean isCrit) {
        if (anchorView == null) return;
        android.view.ViewGroup rootView = findViewById(android.R.id.content);
        if (!(rootView instanceof android.widget.FrameLayout)) {
            // content root 不是 FrameLayout 就不弹了，避免崩溃（正常Activity默认就是FrameLayout，一般不会走到这）
            return;
        }
        android.widget.FrameLayout overlay = (android.widget.FrameLayout) rootView;

        int[] anchorLoc = new int[2];
        anchorView.getLocationOnScreen(anchorLoc);
        int[] overlayLoc = new int[2];
        overlay.getLocationOnScreen(overlayLoc);
        float startX = anchorLoc[0] - overlayLoc[0] + anchorView.getWidth() / 2f
                + (float) (Math.random() * dp(30) - dp(15)); // 加点随机横向抖动，连续命中不会完全重叠
        float startY = anchorLoc[1] - overlayLoc[1] + anchorView.getHeight() / 3f;

        TextView tv = new TextView(this);
        boolean isHeal = amount < 0;
        String text = isHeal ? ("+" + (-amount)) : String.valueOf(amount);
        tv.setText(text);
        tv.setTextSize(isCrit ? 26f : 18f);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setTextColor(isHeal ? 0xFF60E060 : (isCrit ? 0xFFFF5030 : 0xFFFFD060));
        if (isCrit) {
            tv.setShadowLayer(6f, 0f, 0f, 0xFFFF2000);
        }

        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        tv.setX(startX);
        tv.setY(startY);
        tv.setAlpha(0f);
        tv.setScaleX(isCrit ? 0.6f : 1f);
        tv.setScaleY(isCrit ? 0.6f : 1f);
        overlay.addView(tv);

        tv.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(isCrit ? 140 : 100)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.5f))
                .withEndAction(() -> tv.animate()
                        .translationY(-dp(70))
                        .alpha(0f)
                        .setStartDelay(180)
                        .setDuration(500)
                        .withEndAction(() -> overlay.removeView(tv))
                        .start())
                .start();
    }

    private String buildStatusChips(Entity e) {
        StringBuilder sb = new StringBuilder();
        sb.append("🧠" + getString(R.string.sanity) + " ").append(e.sanity).append("  ");
        sb.append(e.getAmmo()).append("  ");
        if (e.stagger_count < 3) {
            sb.append("😖" + getString(R.string.next_stagger)).append(e.health - e.staggerLine[e.stagger_count] * e.outside_max_hp).append("伤害  ");
        }
        if (e.block > 0) sb.append("🛡" + getString(R.string.shield)).append(e.block).append("  ");
        if (e.strength != 0) sb.append("💪" + getString(R.string.strength)).append(e.strength).append("  ");
        if (e.this_turn_strength != 0) sb.append("⚡" + getString(R.string.this_turn_strength)).append(e.this_turn_strength).append("  ");
        if (e.swift != 0) sb.append("⬆" + getString(R.string.haste)).append(e.swift).append("  ");
        if (e.burn_term > 0) sb.append("🔥" + getString(R.string.burn)).append(e.burn_strength).append("x").append(e.burn_term).append("  ");
        if (e.bleed_term > 0) sb.append("🩸" + getString(R.string.bleed)).append(e.bleed_strength).append("x").append(e.bleed_term).append("  ");
        if (e.tremor_term > 0) sb.append("🌀" + getString(R.string.tremor)).append(e.tremor_strength).append("x").append(e.tremor_term).append("  ");
        if (e.sinking_term > 0) sb.append("💧" + getString(R.string.sinking)).append(e.sinking_strength).append("x").append(e.sinking_term).append("  ");
        if (e.rapture_term > 0) sb.append("💔" + getString(R.string.rapture)).append(e.rapture_strength).append("x").append(e.rapture_term).append("  ");
        if (e.poise_term > 0) sb.append("🌫️" + getString(R.string.poise)).append(e.poise_strength).append("x").append(e.poise_term).append("  ");
        if (e.blade > 0) sb.append("🗡" + getString(R.string.blade)).append(e.blade).append("  ");
        if (e.shin > 0) sb.append("💛" + getString(R.string.shin)).append(e.shin).append("  ");
        if (e.charge_term > 0 || e.charge_strength > 1) sb.append("🔋" + getString(R.string.charge)).append(e.charge_strength).append("-").append(e.charge_term).append("  ");
        if (e.stagger_panic_term > 0) sb.append("😵" + getString(R.string.stagger)).append(e.stagger_panic_term).append("  ");
        if (e.restrictions > 0) sb.append("⛓" + getString(R.string.restrictions)).append(e.restrictions).append("  ");
        if (e.krama > 0) sb.append("‼️" + getString(R.string.krama)).append(e.krama).append("  ");
        if (e.EntityId == 3) sb.append("⚜️" + getString(R.string.Hermes)).append(e.grace).append("  ");
        if (e.tianjiustarblade > 0) sb.append("🔪" + getString(R.string.TianjiuStarBlade)).append(e.tianjiustarblade).append("  ");
        ArrayList<String> a = e.buff.getString();
        for (int i = 0; i < a.size(); i++) {
            sb.append(a.get(i));
        }
        if (sb.length() == 0) sb.append(getString(R.string.NoEffect));
        return sb.toString().trim();
    }

    /**
     * 状态key -> 真图标drawable资源id 的映射表。
     * 没配置的key会自动落回原来的emoji，不用担心图标做一半时另一半状态显示空白/报错。
     * 每个图标条目都单独包一层 try-catch（见 appendIconOrEmoji），单个资源出问题不会拖垮整个UI。
     */
    private static final java.util.Map<String, Integer> STATUS_ICON_RES = new java.util.HashMap<>();
    static {
        STATUS_ICON_RES.put("burn", R.drawable.ic_status_burn);
        STATUS_ICON_RES.put("strength", R.drawable.ic_status_strength);
        STATUS_ICON_RES.put("bleed", R.drawable.ic_status_bleed);
        STATUS_ICON_RES.put("tremor", R.drawable.ic_status_tremor);
        STATUS_ICON_RES.put("sinking", R.drawable.ic_status_sinking);
        STATUS_ICON_RES.put("rapture", R.drawable.ic_status_rapture);
        STATUS_ICON_RES.put("poise", R.drawable.ic_status_poise);
        STATUS_ICON_RES.put("shin", R.drawable.ic_status_shin);
        STATUS_ICON_RES.put("charge", R.drawable.ic_status_charge);
        //弹药
        STATUS_ICON_RES.put("ammo", R.drawable.ic_status_ammo);
        STATUS_ICON_RES.put("ammo_speed", R.drawable.ic_status_speedammo);
        STATUS_ICON_RES.put("ammo_butterfly", R.drawable.ic_status_butterflyammo);
        STATUS_ICON_RES.put("ammo_magicbullet", R.drawable.ic_status_magicbulletammo);
        //BUFF

    }

    private String getAmmoIconKey(int ammoType) {
        switch (ammoType) {
            case 0: return "ammo";
            case 1: return "ammo_speed";
            case 2: return "ammo_butterfly";
            case 3: return "ammo_magicbullet";
            case 4: return "ammo_tiger";
            case 5: return "ammo_angry_tiger";
            default: return "ammo";
        }
    }

    /** 状态图标在文字里显示的边长（sp转px），固定死这个尺寸，不用drawable自己的原始像素尺寸，
     *  防止图片没压缩、原图很大时把整行TextView的行高撑爆、挤走其它UI。 */
    private static final int STATUS_ICON_SIZE_DP = 16;

    /**
     * key在STATUS_ICON_RES里配了图就用ImageSpan画真图标，没配就照旧append emoji文字。
     * 整个过程包了try-catch：资源缺失/解码失败/InflateException之类的问题只会让这一个图标
     * 静默回退成emoji，不会导致 refreshAllUI() 抛出未捕获异常炸掉整个Activity。
     */
    private void appendIconOrEmoji(android.text.SpannableStringBuilder sb, String key, String emojiFallback) {
        Integer resId = STATUS_ICON_RES.get(key);
        if (resId != null) {
            try {
                android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, resId);
                if (drawable == null) throw new android.content.res.Resources.NotFoundException("drawable is null for key=" + key);
                int sizePx = dp(STATUS_ICON_SIZE_DP);
                drawable.setBounds(0, 0, sizePx, sizePx); // 强制固定尺寸，不用原图的intrinsic大小
                int start = sb.length();
                sb.append(" "); // 占位一个字符，图标就画在这个字符的位置上
                android.text.style.ImageSpan span = new android.text.style.ImageSpan(
                        drawable, android.text.style.ImageSpan.ALIGN_BOTTOM);
                sb.setSpan(span, start, start + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return;
            } catch (Throwable t) {
                // 资源有问题（找不到/解码失败/xml格式错误等）就静默退回emoji，绝不让这一个图标崩掉整个UI
                t.printStackTrace();
            }
        }
        sb.append(emojiFallback);
    }

    /** 跟 buildStatusChips 逐行对应，只是把纯文本换成带ImageSpan的SpannableStringBuilder */
    private android.text.SpannableStringBuilder buildStatusSpannable(Entity e) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        appendIconOrEmoji(sb, "sanity", "🧠");
        sb.append(getString(R.string.sanity)).append(" ").append(String.valueOf(e.sanity)).append("  ");

        appendIconOrEmoji(sb, getAmmoIconKey(e.ammoType), "🧨");
        sb.append(" ").append((char) e.ammo).append("/").append((char) e.totalAmmo).append("  ");

        if (e.stagger_count < 3) {
            appendIconOrEmoji(sb, "next_stagger", "😖");
            sb.append(getString(R.string.next_stagger))
                    .append(String.valueOf(e.health - e.staggerLine[e.stagger_count] * e.outside_max_hp))
                    .append("伤害  ");
        }
        if (e.block > 0) {
            appendIconOrEmoji(sb, "shield", "🛡");
            sb.append(getString(R.string.shield)).append(String.valueOf(e.block)).append("  ");
        }
        if (e.strength != 0) {
            appendIconOrEmoji(sb, "strength", "💪");
            sb.append(getString(R.string.strength)).append(String.valueOf(e.strength)).append("  ");
        }
        if (e.this_turn_strength != 0) {
            appendIconOrEmoji(sb, "this_turn_strength", "⚡");
            sb.append(getString(R.string.this_turn_strength)).append(String.valueOf(e.this_turn_strength)).append("  ");
        }
        if (e.swift != 0) {
            appendIconOrEmoji(sb, "haste", "⬆");
            sb.append(getString(R.string.haste)).append(String.valueOf(e.swift)).append("  ");
        }
        if (e.burn_term > 0) {
            appendIconOrEmoji(sb, "burn", "🔥");
            sb.append(getString(R.string.burn)).append(String.valueOf(e.burn_strength)).append("x").append(String.valueOf(e.burn_term)).append("  ");
        }
        if (e.bleed_term > 0) {
            appendIconOrEmoji(sb, "bleed", "🩸");
            sb.append(getString(R.string.bleed)).append(String.valueOf(e.bleed_strength)).append("x").append(String.valueOf(e.bleed_term)).append("  ");
        }
        if (e.tremor_term > 0) {
            appendIconOrEmoji(sb, "tremor", "🌀");
            sb.append(getString(R.string.tremor)).append(String.valueOf(e.tremor_strength)).append("x").append(String.valueOf(e.tremor_term)).append("  ");
        }
        if (e.sinking_term > 0) {
            appendIconOrEmoji(sb, "sinking", "💧");
            sb.append(getString(R.string.sinking)).append(String.valueOf(e.sinking_strength)).append("x").append(String.valueOf(e.sinking_term)).append("  ");
        }
        if (e.rapture_term > 0) {
            appendIconOrEmoji(sb, "rapture", "💔");
            sb.append(getString(R.string.rapture)).append(String.valueOf(e.rapture_strength)).append("x").append(String.valueOf(e.rapture_term)).append("  ");
        }
        if (e.poise_term > 0) {
            appendIconOrEmoji(sb, "poise", "🌫️");
            sb.append(getString(R.string.poise)).append(String.valueOf(e.poise_strength)).append("x").append(String.valueOf(e.poise_term)).append("  ");
        }
        if (e.blade > 0) {
            appendIconOrEmoji(sb, "blade", "🗡");
            sb.append(getString(R.string.blade)).append(String.valueOf(e.blade)).append("  ");
        }
        if (e.shin > 0) {
            appendIconOrEmoji(sb, "shin", "💛");
            sb.append(getString(R.string.shin)).append(String.valueOf(e.shin)).append("  ");
        }
        if (e.charge_term > 0 || e.charge_strength > 1) {
            appendIconOrEmoji(sb, "charge", "🔋");
            sb.append(getString(R.string.charge)).append(String.valueOf(e.charge_strength)).append("-").append(String.valueOf(e.charge_term)).append("  ");
        }
        if (e.stagger_panic_term > 0) {
            appendIconOrEmoji(sb, "stagger", "😵");
            sb.append(getString(R.string.stagger)).append(String.valueOf(e.stagger_panic_term)).append("  ");
        }
        if (e.restrictions > 0) {
            appendIconOrEmoji(sb, "restrictions", "⛓");
            sb.append(getString(R.string.restrictions)).append(String.valueOf(e.restrictions)).append("  ");
        }
        if (e.krama > 0) {
            appendIconOrEmoji(sb, "krama", "‼️");
            sb.append(getString(R.string.krama)).append(String.valueOf(e.krama)).append("  ");
        }
        if (e.EntityId == 3) {
            appendIconOrEmoji(sb, "hermes", "⚜️");
            sb.append(getString(R.string.Hermes)).append(String.valueOf(e.grace)).append("  ");
        }
        if (e.tianjiustarblade > 0) {
            appendIconOrEmoji(sb, "tianjiu", "🔪");
            sb.append(getString(R.string.TianjiuStarBlade)).append(String.valueOf(e.tianjiustarblade)).append("  ");
        }

        ArrayList<String> buffEntriesNew = e.buff.getString();
        for (String entry : buffEntriesNew) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                appendIconOrEmoji(sb, key, "📌");
                sb.append(" ").append(value).append("  ");
            } else {
                sb.append(entry).append("  ");
            }
        }

        if (sb.length() == 0) sb.append(getString(R.string.NoEffect));
        // 去掉结尾多余空格（Spannable不能直接trim，手动裁）
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') end--;
        return (android.text.SpannableStringBuilder) sb.delete(end, sb.length());
    }

    public void refreshAllUI() {
        tvKarma.setText(getString(R.string.krama_ex) + ": " + indexFingerLevel.krama);
        tvBlessing.setText(getString(R.string.grace_ex) + ": " + indexFingerLevel.grace);
        String name = indexFingerLevel.getName();
        String codename = store.loadCodename();
        if (codename != null && !codename.isEmpty()) name += " " + codename;
        tvTitle.setText(name);

        tvHP.setText(getString(R.string.hp) + ": " + player.hp + "/" + player.max_hp);

        if (hpBar != null) {
            hpBar.setMax(Math.max(1, player.max_hp));
            hpBar.setProgress(Math.max(0, Math.min(player.hp, player.max_hp)));
        }

        int exp = indexFingerLevel.grace;
        int maxExp = 100;
        tvEXP.setText(getString(R.string.exp) + exp + "/" + maxExp);
        tvLevel.setText(getString(R.string.level) + indexFingerLevel.level);
        if (expBar != null) {
            expBar.setMax(maxExp);
            expBar.setProgress(Math.max(0, Math.min(exp, maxExp)));
        }

        tvLight.setText(getString(R.string.light) + ": \n" + getLight());
        if (tvPlayerStatus != null) tvPlayerStatus.setText(buildStatusSpannable(player));
        updateHpEllipses();

        if (enemy != null) {
            tvEnemyName.setText(enemy.name + "  " + enemy.hp + "/" + enemy.max_hp
                    + (enemy.block > 0 ? getString(R.string.shield) + "  :" + enemy.block : ""));
            String intentText = (enemy.current_intent != null) ? enemy.current_intent.description : "——";
            tvEnemyIntent.setText(getString(R.string.intent) + "：" + (battleOver ? getString(R.string.battleover) : intentText));
            if (tvEnemyStatus != null) tvEnemyStatus.setText(buildStatusSpannable(enemy));
            if (enemyHpBar != null) {
                enemyHpBar.setMax(Math.max(1, enemy.max_hp));
                enemyHpBar.setProgress(Math.max(0, enemy.hp));
            }
        }
    }

    /**
     * 兜底修透明度问题：这两个 DragonBonesView 如果本质是 SurfaceView（很多骨骼动画库为了
     * 性能这么写），默认是不透明的、会在窗口上"打洞"，光设 background 没用，必须单独：
     * 1) setZOrderOnTop(true) 让它按 View 树的 Z 序正常叠加，而不是永远扣在最底层的洞里；
     * 2) getHolder().setFormat(PixelFormat.TRANSLUCENT) 让这个洞允许透明合成。
     * 如果它只是普通 View / GLSurfaceView，这段对 SurfaceView 的逻辑会被 instanceof 挡掉，
     * 不会有副作用，只会走下面的 setBackgroundColor(TRANSPARENT) 兜底。
     *
     * onTop 目前没有实际去调整绘制顺序（Android 里子 View 的层级由添加顺序 / elevation决定，
     * 这里的两个 view 层级本来就是玩家先加、敌人后加，敌人自然叠在上面），这个参数留着是为了
     * 以后如果要用 setZOrderMediaOverlay 精细控制两个 SurfaceView 之间谁盖谁时方便扩展。
     */
    private void makeTransparentAndLayered(android.view.View view, boolean onTop) {
        if (view == null) return;
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if (view instanceof android.view.SurfaceView) {
            android.view.SurfaceView sv = (android.view.SurfaceView) view;
            sv.setZOrderOnTop(onTop);
            android.view.SurfaceHolder holder = sv.getHolder();
            if (holder != null) {
                holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT);
            }
        }
    }

    private void applyBattleModeUI() {
        if (animatedMode) {
            playerDragonBonesView.setVisibility(android.view.View.VISIBLE);
            ensurePlayerArmatureLoaded();
            if (enemyDragonBonesView != null) {
                enemyDragonBonesView.setVisibility(android.view.View.VISIBLE);
                ensureEnemyArmatureLoaded();
            }
        } else {
            playerDragonBonesView.setVisibility(android.view.View.GONE);
            if (enemyDragonBonesView != null) {
                enemyDragonBonesView.setVisibility(android.view.View.GONE);
            }
        }
    }

    private String findFirstArmatureName(com.six.fortuna.dragonbones.AndroidFactory factory) {
        java.util.Map<String, DragonBonesData> all = factory.getAllDragonBonesData();
        for (DragonBonesData data : all.values()) {
            if (data.armatureNames.size() > 0) {
                return data.armatureNames.get(0);
            }
        }
        return null;
    }

    // 双方站位：玩家站左，敌人站右，别让两个view都停在(0,0)叠成一个人
    private static final float PLAYER_STAND_X = -260f;
    private static final float ENEMY_STAND_X = 260f;

    private void ensurePlayerArmatureLoaded() {
        if (playerArmature != null) {
            playerDragonBonesView.setArmature(playerArmature);
            playerDragonBonesView.setTranslationX(PLAYER_STAND_X);
            playerDragonBonesView.play();
            return;
        }
        if (playerArmatureLoadFailed) return;
        try {
            com.six.fortuna.dragonbones.AndroidFactory factory = com.six.fortuna.dragonbones.AndroidFactory.getInstance();
            factory.loadFromAssets(getAssets(),
                    "dragonbones/PlayerAttackBlunt/Player_ske.json",
                    "dragonbones/PlayerAttackBlunt/Player_tex.json",
                    "dragonbones/PlayerAttackBlunt/Player_tex.png");
            String armatureName = findFirstArmatureName(factory);
            playerArmature = armatureName != null ? factory.buildArmature(armatureName) : null;
            if (playerArmature != null) {
                playerArmature.getAnimation().play("Idle");
                playerDragonBonesView.setArmature(playerArmature);
                playerDragonBonesView.setTranslationX(PLAYER_STAND_X);
                playerDragonBonesView.play();
            } else {
                playerArmatureLoadFailed = true;
                Toast.makeText(this, "buildArmature 失败", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            playerArmatureLoadFailed = true;
            animatedMode = false;
            playerDragonBonesView.setVisibility(android.view.View.GONE);
        }
    }

    private void ensureEnemyArmatureLoaded() {
        if (enemyDragonBonesView == null) return;
        if (enemyArmature != null) {
            enemyDragonBonesView.setArmature(enemyArmature);
            enemyDragonBonesView.setTranslationX(ENEMY_STAND_X);
            enemyDragonBonesView.play();
            return;
        }
        if (enemyArmatureLoadFailed) return;
        try {
            com.six.fortuna.dragonbones.AndroidFactory factory = com.six.fortuna.dragonbones.AndroidFactory.getInstance();
            String armatureName = findFirstArmatureName(factory);
            if (armatureName == null) {
                factory.loadFromAssets(getAssets(),
                        "dragonbones/PlayerAttackBlunt/Player_ske.json",
                        "dragonbones/PlayerAttackBlunt/Player_tex.json",
                        "dragonbones/PlayerAttackBlunt/Player_tex.png");
                armatureName = findFirstArmatureName(factory);
            }
            enemyArmature = armatureName != null ? factory.buildArmature(armatureName) : null;
            if (enemyArmature != null) {
                enemyArmature.getAnimation().play("Idle");
                enemyDragonBonesView.setArmature(enemyArmature);
                enemyDragonBonesView.setTranslationX(ENEMY_STAND_X);
                enemyDragonBonesView.setScaleX(-1f);
                enemyDragonBonesView.play();
            } else {
                enemyArmatureLoadFailed = true;
                Toast.makeText(this, "敌人 buildArmature 失败", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            enemyArmatureLoadFailed = true;
        }
    }

    /**
     * 更有力道的打击停顿：冻结时间 + 被打中一侧的挤压反馈(只动scaleY，不碰scaleX，
     * 避免跟角色朝向翻转的-1/1打架) + 整个战斗区域短促左右晃动。
     */
    private void doHitstop(android.view.View impactView) {
        WorldClock.clock.timeScale = 0f;

        if (impactView != null) {
            impactView.animate().cancel();
            final float baseScaleY = 1f;
            impactView.setScaleY(baseScaleY);
            impactView.animate()
                    .scaleY(baseScaleY * 0.72f)
                    .setDuration(50)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> impactView.animate()
                            .scaleY(baseScaleY * 1.12f)
                            .setDuration(70)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(3f))
                            .withEndAction(() -> impactView.animate()
                                    .scaleY(baseScaleY)
                                    .setDuration(90)
                                    .start())
                            .start())
                    .start();
        }

        android.view.View shakeTarget = (playerDragonBonesView != null
                && playerDragonBonesView.getParent() instanceof android.view.View)
                ? (android.view.View) playerDragonBonesView.getParent() : null;
        if (shakeTarget != null) {
            shakeTarget.animate().cancel();
            android.animation.ObjectAnimator shake = android.animation.ObjectAnimator.ofFloat(
                    shakeTarget, "translationX", 0f, -24f, 22f, -16f, 12f, -6f, 0f);
            shake.setDuration(220);
            shake.start();
        }

        clashHandler.postDelayed(() -> WorldClock.clock.timeScale = 1f, 130);
    }

    // ========== 攻击动画（修正版） ==========

    private void playPlayerAttackAnimation() {
        playPlayerAttackAnimation(false, Integer.MIN_VALUE);
    }

    private void playPlayerAttackAnimation(boolean enhanced) {
        playPlayerAttackAnimation(enhanced, Integer.MIN_VALUE);
    }

    /**
     * @param damageDealt 这次命中造成的伤害数值，传 Integer.MIN_VALUE 表示不弹跳字（不知道具体伤害数的场合用两参版本）。
     *                    真实伤害数要从你算伤害的地方（比如 mechanics.dealDamage 那一层）传进来，这里没法自己猜。
     */
    private void playPlayerAttackAnimation(boolean enhanced, int damageDealt) {
        if (!animatedMode || playerArmature == null || isAttackAnimating) return;
        isAttackAnimating = true;

        final float speedScale;
        if (player.buff.animaionTimes > 1) {
            float originalDuration = getActualDuration(playerArmature);
            if (originalDuration > 0) {
                float totalTime = 1.2f;
                float eachTime = totalTime / player.buff.animaionTimes;
                speedScale = Math.min(originalDuration / eachTime, 5.0f);
            } else {
                speedScale = 1.0f;
            }
            player.buff.animaionTimes = 0;
        } else {
            speedScale = 1.0f;
        }

        final float originalScaleX = playerDragonBonesView.getScaleX();
        playerDragonBonesView.setScaleX(-1f);

        playerArmature.getAnimation().play("Move", 1);
        clashHandler.postDelayed(() -> {
            if (playerArmature == null) {
                isAttackAnimating = false;
                return;
            }
            playerArmature.getAnimation().timeScale = speedScale;
            playerArmature.getAnimation().play(enhanced ? "Attack_Enhanced" : "Attack", 1);

            final int hitDelay = (int) ((enhanced ? 400 : 300) / speedScale);
            clashHandler.postDelayed(() -> {
                if (enemyArmature != null) {
                    enemyArmature.getAnimation().play("Onhit", 1);
                    clashHandler.postDelayed(() -> {
                        if (enemyArmature != null) {
                            enemyArmature.getAnimation().play("Idle", 1);
                        }
                    }, 200);
                }
                doHitstop(enemyDragonBonesView);
                if (damageDealt != Integer.MIN_VALUE) {
                    showDamageNumber(enemyDragonBonesView, damageDealt, enhanced);
                }
            }, hitDelay);

            final int totalDuration = (int) ((enhanced ? 700 : 600) / speedScale);
            clashHandler.postDelayed(() -> {
                if (playerArmature != null) {
                    playerArmature.getAnimation().timeScale = 1.0f;
                    playerArmature.getAnimation().play("Idle", 1);
                }
                playerDragonBonesView.setScaleX(originalScaleX);
                isAttackAnimating = false;
            }, totalDuration);
        }, 200);
    }

    private void playEnemyAttackAnimation() {
        playEnemyAttackAnimation(false, Integer.MIN_VALUE);
    }

    private void playEnemyAttackAnimation(boolean enhanced) {
        playEnemyAttackAnimation(enhanced, Integer.MIN_VALUE);
    }

    private void playEnemyAttackAnimation(boolean enhanced, int damageDealt) {
        if (!animatedMode || enemyArmature == null || isAttackAnimating) return;
        isAttackAnimating = true;

        final float speedScale = 1.0f;
        final float originalScaleX = enemyDragonBonesView.getScaleX();
        enemyDragonBonesView.setScaleX(1f);

        enemyArmature.getAnimation().timeScale = speedScale;
        enemyArmature.getAnimation().play(enhanced ? "Attack_Enhanced" : "Attack", 1);

        final int hitDelay = (int) ((enhanced ? 400 : 300) / speedScale);
        clashHandler.postDelayed(() -> {
            if (playerArmature != null) {
                playerArmature.getAnimation().play("Onhit", 1);
                clashHandler.postDelayed(() -> {
                    if (playerArmature != null) {
                        playerArmature.getAnimation().play("Idle", 1);
                    }
                }, 200);
            }
            doHitstop(playerDragonBonesView);
            if (damageDealt != Integer.MIN_VALUE) {
                showDamageNumber(playerDragonBonesView, damageDealt, enhanced);
            }
        }, hitDelay);

        final int totalDuration = (int) ((enhanced ? 700 : 600) / speedScale);
        clashHandler.postDelayed(() -> {
            if (enemyArmature != null) {
                enemyArmature.getAnimation().timeScale = 1.0f;
                enemyArmature.getAnimation().play("Idle", 1);
            }
            enemyDragonBonesView.setScaleX(originalScaleX);
            isAttackAnimating = false;
        }, totalDuration);
    }

    public String getLight() {
        String output = "";
        for (int i = 0; i < player.energy && i < player.max_energy; i++) {
            output += "🟡";
        }
        if (player.max_energy <= player.energy) {
            for (int i = 0; i < player.energy - player.max_energy; i++) {
                output += "🟣";
            }
        } else {
            for (int i = 0; i < player.max_energy - player.energy; i++) {
                output += "⚪";
            }
        }
        return output;
    }

    public void playSong(Context context, int newSongResId, MediaPlayer mediaPlayer) {
        if (newSongResId == currentSongResId) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } else {
            try {
                currentSongResId = newSongResId;
                mediaPlayer.reset();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(newSongResId);
                if (afd != null) {
                    mediaPlayer.setDataSource(
                            afd.getFileDescriptor(),
                            afd.getStartOffset(),
                            afd.getLength()
                    );
                    afd.close();
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(mp -> mp.start());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clashHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            wasPlayingBeforeStop = true;
            mediaPlayer.pause();
        } else {
            wasPlayingBeforeStop = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (wasPlayingBeforeStop && mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public float getActualDuration(Armature armature) {
        Animation animation = armature.getAnimation();
        String animName = animation.getLastAnimationName();
        if (animName == null) {
            return -1f;
        }
        ArmatureData armatureData = armature.armatureData;
        AnimationData animData = armatureData.getAnimation(animName);
        if (animData == null) {
            return -1f;
        }
        float originalDurationInSeconds = animData.duration;
        float speed = animation.timeScale * WorldClock.clock.timeScale;
        if (speed == 0) {
            return Float.POSITIVE_INFINITY;
        }
        return originalDurationInSeconds / speed;
    }
}