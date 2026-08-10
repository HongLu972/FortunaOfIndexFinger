package com.six.fortuna;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.six.fortuna.combat.engine.FortunaCards;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 局外养成界面：
 *   - 出战牌组（ownedCardKeys，store.saveOwnedCardKeys）：真正会被带进战斗的卡
 *   - 大牌库（collectionKeys，store.saveCardCollection）：所有获得过但没装备的卡，战斗掉落先进这里
 * "移除一张"＝牌组→大牌库（卸下装备）；"装入牌组"＝大牌库→牌组（装备）。都是移动，不会凭空消失。
 */
public class DeckManageActivity extends AppCompatActivity {

    private static final long UPGRADE_HP_COST = 50000L; // 5万眼
    private static final int UPGRADE_HP_AMOUNT = 10;

    private PrescriptStore store;
    private TextView tvEyes, tvBonusHp;
    private Button btnUpgradeHp;
    private Button getBtnUpgradeHpE;
    private LinearLayout containerDeck, containerCollection;

    private List<String> deckKeys = new ArrayList<>();
    private List<String> collectionKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deck_manage);

        store = new PrescriptStore(this);

        tvEyes = findViewById(R.id.tv_eyes);
        tvBonusHp = findViewById(R.id.tv_bonus_hp);
        btnUpgradeHp = findViewById(R.id.btn_upgrade_hp);
        getBtnUpgradeHpE = findViewById(R.id.btn_upgrade_hp_editable);
        containerDeck = findViewById(R.id.container_deck);
        containerCollection = findViewById(R.id.container_collection);

        btnUpgradeHp.setOnClickListener(v -> {
            long eyes = store.loadEyes();
            if (eyes < UPGRADE_HP_COST) {
                Toast.makeText(this, "眼不够，还差 " + ((UPGRADE_HP_COST - eyes) / 10000) + "万", Toast.LENGTH_SHORT).show();
                return;
            }
            store.saveEyes(eyes - UPGRADE_HP_COST);
            store.saveBonusMaxHp(store.loadBonusMaxHp() + UPGRADE_HP_AMOUNT);
            Toast.makeText(this, "升级成功！血量上限 +" + UPGRADE_HP_AMOUNT, Toast.LENGTH_SHORT).show();
            refreshAll();
        });

        getBtnUpgradeHpE.setOnClickListener(v -> {
            long eyes = store.loadEyes();
            final EditText input = new EditText(this);
            input.setHint("重复几次？");
            new android.app.AlertDialog.Builder(this)
                    .setTitle("局外加成·生命值上限")
                    .setMessage("你要重复多少次？")
                    .setView(input)
                    .setCancelable(false)
                    .setPositiveButton("确定", (dialog, which) -> {
                        String raw = input.getText().toString().trim();
                        int times;
                        try {
                            times = Integer.parseInt(raw);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "再这么杵着不给出合法的数字，你的脑袋就要搬家咯", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(times * UPGRADE_HP_COST > eyes){
                            Toast.makeText(this, "既然是来做强化的，就得要有带够钱的觉悟", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        store.saveEyes(eyes - UPGRADE_HP_COST * times);
                        store.saveBonusMaxHp(store.loadBonusMaxHp() + UPGRADE_HP_AMOUNT * times);
                        Toast.makeText(this, "这就是，你的额外生命值么，阿西口...", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("ALL！全部消耗！", (dialog, which) -> {
                        int times = (int) (eyes / UPGRADE_HP_COST);
                        store.saveEyes(eyes - UPGRADE_HP_COST * times);
                        store.saveBonusMaxHp(store.loadBonusMaxHp() + UPGRADE_HP_AMOUNT * times);
                        Toast.makeText(this, "这就是，你的额外生命值么，阿西口...", Toast.LENGTH_SHORT).show();

                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.nav_home).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.nav_stats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));

        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void refreshAll() {
        tvEyes.setText("👁 眼: " + (store.loadEyes() / 10000) + "万");
        tvBonusHp.setText("局外加成：出战血量上限 +" + store.loadBonusMaxHp());
        btnUpgradeHp.setText( (UPGRADE_HP_COST / 10000) + "万眼->血量+" + UPGRADE_HP_AMOUNT);

        List<String> owned = store.loadOwnedCardKeys();
        deckKeys = owned != null ? owned : new ArrayList<>();
        collectionKeys = store.loadCardCollection();

        renderList(containerDeck, deckKeys, "移除一张", this::unequipOneCard);
        renderList(containerCollection, collectionKeys, "装入牌组", this::equipOneCard);
    }

    private interface RowAction {
        void act(String key);
    }

    /** 按key分组统计数量后把每一行渲染进container里，container里的按钮文字和行为由调用方指定 */
    private void renderList(LinearLayout container, List<String> keys, String buttonLabel, RowAction action) {
        container.removeAllViews();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String k : keys) counts.merge(k, 1, Integer::sum);

        if (counts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("（空）");
            empty.setTextColor(0xFF9E9E9E);
            empty.setPadding(8, 8, 8, 8);
            container.addView(empty);
            return;
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_deck_card, container, false);
            TextView tvName = row.findViewById(R.id.tv_card_name);
            Button btnAction = row.findViewById(R.id.btn_remove_one);

            int rareness = FortunaCards.rarenessOf(entry.getKey());
            String qualityTag = rareness == 3 ? "🌟" : rareness == 2 ? "🔷" : rareness == 1 ? "⚪" : "⚙️";
            tvName.setText(qualityTag + " " + FortunaCards.displayName(entry.getKey()) + "  x" + entry.getValue());
            btnAction.setText(buttonLabel);
            btnAction.setOnClickListener(v -> action.act(entry.getKey()));

            container.addView(row);
        }
    }

    /** 牌组 -> 大牌库（卸下） */
    private void unequipOneCard(String key) {
        if (!deckKeys.remove(key)) return; // remove(Object)只删第一个匹配，正好是"一张"
        collectionKeys.add(key);
        store.saveOwnedCardKeys(deckKeys);
        store.saveCardCollection(collectionKeys);
        Toast.makeText(this, "已卸下 " + FortunaCards.displayName(key) + "，放回大牌库", Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    /** 大牌库 -> 牌组（装备） */
    private void equipOneCard(String key) {
        if (!collectionKeys.remove(key)) return;
        deckKeys.add(key);
        store.saveCardCollection(collectionKeys);
        store.saveOwnedCardKeys(deckKeys);
        Toast.makeText(this, "已装备 " + FortunaCards.displayName(key) + "，下次出战会带上", Toast.LENGTH_SHORT).show();
        refreshAll();
    }
}