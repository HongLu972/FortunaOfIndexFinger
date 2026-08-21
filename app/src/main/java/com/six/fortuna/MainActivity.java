package com.six.fortuna;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.six.fortuna.IndexLevel.IndexFingerLevel_CN;
import com.six.fortuna.Prescripts.Prescripts;
import com.six.fortuna.Prescripts.Prescripts_CN;
import com.six.fortuna.Prescripts.Prescripts_finished;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // UI
    private FrameLayout stackContainer;
    private Button btnAdd;
    private Button btnAdd100;
    private boolean wasPlayingBeforeStop = false;
    private TextView tvKarma, tvBlessing, tvTitle;

    // 数据
    public int coolDown = 0*1000; // 获取指令的冷却时长（毫秒，基准值，实际会加随机浮动）
    public Prescripts prescripts;
    private MediaPlayer mediaPlayer;
    public IndexFingerLevel_CN indexFingerLevel;
    private final Random r = new Random();

    // 持久化
    private PrescriptStore store;

    // 冷却：绝对时间戳，0表示当前没有在冷却
    public long cooldownDeadline = 0L;

    // 每张卡片对应的数据模型（View -> Prescripts_finished），用于读取deadline/id
    private final Map<View, Prescripts_finished> cardData = new HashMap<>();
    private final Handler tickHandler = new Handler(Looper.getMainLooper());

    public String codename; // 玩家给自己起的代号

    private void askForCodename(){
        final EditText input = new EditText(this);
        input.setHint("给自己起一个代号");
        new AlertDialog.Builder(this)
                .setTitle("初次相遇")
                .setMessage("你要如何被称呼？")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        askForCodename(); // 不给空代号，重新问
                    }else if(name.matches("凯瑟琳") || name.matches("凯茜")){
                        codename = "口口口";
                        store.saveCodename(codename);
                        updateStats();
                    } else {
                        codename = name;
                        store.saveCodename(codename);
                        updateStats();
                    }
                })
                .show();
    }
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshAllCardTimers();
            refreshCooldownUI();
            tickHandler.postDelayed(this, 1000);
        }
    };

    public static MainActivity instance;

    private ArrayList<View> cardViews = new ArrayList<>();

    /**
     * 排序规则：剩余时间越少，越应该排在“视觉最上层”。
     * 注意 stackContainer 是 FrameLayout —— 最后 addView 的会盖在最上面。
     * 所以这里要按“剩余时间从多到少”重新排列列表，
     * 这样剩余时间最少的那张会是列表里最后一个，addView时自然盖在最上面。
     * （之前排序方向反了，导致视觉上看起来像“排序失效”。）
     */
    private void reorderCards() {
        Collections.sort(cardViews, (v1, v2) -> {
            long t1 = cardData.get(v1).deadline;
            long t2 = cardData.get(v2).deadline;
            return Long.compare(t2, t1); // 降序：deadline大（剩余时间多）的排前面，排后面的最先到期
        });
        stackContainer.removeAllViews();
        for (View v : cardViews) {
            stackContainer.addView(v);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;  // 便于全局调用

        // 获取视图
        stackContainer = findViewById(R.id.stack_container);
        btnAdd = findViewById(R.id.btn_add);
        btnAdd100 = findViewById(R.id.add_butten);
        tvKarma = findViewById(R.id.tv_karma);
        tvBlessing = findViewById(R.id.tv_blessing);
        tvTitle = findViewById(R.id.tv_title);



        // 初始化持久化存储
        store = new PrescriptStore(this);
        codename = store.loadCodename();
        if (codename == null || codename.trim().isEmpty()) {
            askForCodename();
        }
        //音乐
        mediaPlayer = MediaPlayer.create(this, R.raw.childrenofthecity);
        mediaPlayer.setVolume(store.loadVolume(), store.loadVolume());
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        // 初始化职阶（先建对象，再用存储覆盖数值——没存过就保持默认0）
        indexFingerLevel = new IndexFingerLevel_CN();
        store.loadStats(indexFingerLevel);

        // 恢复冷却状态（绝对时间戳，杀进程也不会丢）
        cooldownDeadline = store.loadCooldownDeadline();

        // 初始化指令池
        prescripts = new Prescripts_CN(getResources()); // 使用中文指令

        // 更新UI
        updateStats();

        // 恢复上次未完成的挂起指令（deadline已经钉死，不会重新计时）
        for (Prescripts_finished saved : store.loadCards()) {
            renderCard(saved);
        }

        // 点击新增
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCoolingDown()) return;
                // 从指令池随机取一条添加
                Prescripts_finished item = prescripts.getPrescripts();
                if (item != null) {
                    addNewPrescript(item);
                }
                startCooldown();
            }
        });

        btnAdd100.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("100次？")
                .setMessage("确定要连续接收100次指令么，性能不好的话可能会出现卡顿乃至于闪退")
                .setPositiveButton("我确定", (dialog, which)->{
                    if (isCoolingDown()) return;
                    for (int i = 0; i < 100; i++) {
                        // 从指令池随机取一条添加
                        Prescripts_finished item = prescripts.getPrescripts();
                        if (item != null) {
                            addNewPrescript(item);
                        }
                    }
                    startCooldown();
                })
                .setNegativeButton("算...算了", null)
                .show()
        );

        findViewById(R.id.nav_home).setOnClickListener(v ->
            Toast.makeText(this, "你点击这个有何意义？", Toast.LENGTH_SHORT).show());
        findViewById(R.id.nav_stats).setOnClickListener(v ->{
            startActivity(new Intent(this, StatsActivity.class));
        });
        findViewById(R.id.nav_setting).setOnClickListener(v ->{
            startActivity(new Intent(this, SettingActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时立刻刷新一次，并启动心跳
        refreshAllCardTimers();
        refreshCooldownUI();
        tickHandler.removeCallbacks(ticker);
        tickHandler.postDelayed(ticker, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 不在前台没必要刷UI，省电；反正deadline是绝对时间戳，不需要在后台继续跑
        tickHandler.removeCallbacks(ticker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickHandler.removeCallbacks(ticker);

        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private boolean isCoolingDown() {
        return cooldownDeadline > System.currentTimeMillis();
    }

    /**
     * 开始一次冷却：钉死绝对截止时间并存盘，UI立刻刷新一次。
     */
    public void startCooldown() {
        cooldownDeadline = System.currentTimeMillis()
                + (long) (coolDown * (1 + 0.01 * (r.nextInt(101) - 50)));
        store.saveCooldownDeadline(cooldownDeadline);
        refreshCooldownUI();
    }

    /**
     * 根据冷却的绝对截止时间刷新“获取指令”按钮的状态与文字。
     * 每次心跳、每次onResume都会调用一次，所以就算app被杀掉又重开，
     * 冷却进度也会用真实剩余时间正确恢复，不会“白嫖”一次冷却。
     */
    private void refreshCooldownUI() {
        long remainMs = cooldownDeadline - System.currentTimeMillis();
        if (remainMs > 0) {
            btnAdd.setEnabled(false);
            btnAdd.setBackgroundTintList(ColorStateList.valueOf(getColor(android.R.color.darker_gray)));
            long remainSec = (remainMs + 999) / 1000;
            btnAdd.setText("冷却中 (" + remainSec + "s)");
        } else {
            btnAdd.setEnabled(true);
            btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00FFCE")));
            btnAdd.setText("获取指令");
        }
    }

    /**
     * 检查职阶数据里的“processed标记”，如果本次操作触发了“业过高，全部归零”，
     * 就弹一个提示框，然后把标记清掉（避免下次误触发重复弹窗）。
     *
     * 注意：updateStats() 内部会调用 getName() -> checkLevel()，这是第二次调用，
     * 但此时krama/grace已经被清零了，不会再次满足条件，所以标记不会被二次置true，
     * 这里读到的永远是“这次操作本身”是否触发了处决，不会有重复弹窗的问题。
     */
    private void checkAndShowExecutionDialog() {
        if (indexFingerLevel.justExecuted) {
            indexFingerLevel.justExecuted = false;
            new AlertDialog.Builder(this)
                    .setTitle("处决")
                    .setMessage("由于业(福尔图娜)过高，你被处决了")
                    .setCancelable(false)
                    .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    /**
     * 更新所有UI（业、护、称号）
     */
    public void updateStats() {
        tvKarma.setText(getString(R.string.krama_ex) +  ": " + indexFingerLevel.krama);
        tvBlessing.setText(getString(R.string.grace_ex)+": " + indexFingerLevel.grace);
        String name = indexFingerLevel.getName();
        if (codename != null && !codename.isEmpty()) {
            name += " " + codename;
        }
        tvTitle.setText(name);
    }

    /**
     * 新抽到一条指令：钉死绝对截止时间，渲染卡片，并写入持久化。
     */
    public boolean addNewPrescript(Prescripts_finished item) {
        if (item == null) {
            item = new Prescripts_finished(0, "今日无事");
        }
        item.activateDeadline(); // 只在第一次挂起时钉死deadline
        renderCard(item);
        persistCards();
        return indexFingerLevel.done;
    }

    /**
     * 把一个已经有id/deadline的Prescripts_finished渲染成卡片View。
     * 恢复历史卡片、和新增卡片，都走这个方法。
     */
    private void renderCard(Prescripts_finished item) {
        final View card = getLayoutInflater().inflate(R.layout.item_prescript, stackContainer, false);
        cardViews.add(card);
        cardData.put(card, item);

        TextView tvPrescript = card.findViewById(R.id.tv_prescript);
        TextView tvTime = card.findViewById(R.id.tv_time);


        String prefix = (codename != null && !codename.isEmpty()) ? ("致 " + codename + "：") : "";
        tvPrescript.setText(prefix + item.getPrescripts());

        tvPrescript.setText(item.getPrescripts());

        ImageView ivCustom = card.findViewById(R.id.iv_custom);
        ivCustom.setImageResource(R.drawable.index_icon);

        Button btnSuccess = card.findViewById(R.id.btn_success);
        Button btnFail = card.findViewById(R.id.btn_fail);

        updateCardTimeText(item, tvTime);
        updateCardButtonState(item, btnSuccess, btnFail);

        reorderCards();

        // 成功：加护+1，检查晋升，更新UI，移除纸条
        btnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!item.isReady()) return; // 时间没到禁止点击（双重保险，按钮本身也会disable）
                indexFingerLevel.grace++;
                indexFingerLevel.checkLevel();   // 检查晋升
                updateStats();
                store.saveStats(indexFingerLevel);
                removeCard(card);
                indexFingerLevel.done = true;
                checkAndShowExecutionDialog();
            }
        });

        // 失败：业+1，检查晋升，更新UI，移除纸条
        btnFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!item.isReady()) return;
                indexFingerLevel.krama++;
                indexFingerLevel.checkLevel();
                updateStats();
                store.saveStats(indexFingerLevel);
                removeCard(card);
                indexFingerLevel.done = false;
                checkAndShowExecutionDialog();
            }
        });
        // 堆叠效果
        float offsetX = (float) (Math.random() * 30 - 15);
        float offsetY = (float) (Math.random() * 30 - 15);
        float rotation = (float) (Math.random() * 12 - 6);
        card.setTranslationX(offsetX);
        card.setTranslationY(offsetY);
        card.setRotation(rotation);
    }

    /**
     * 心跳：刷新每张卡片的剩余时间文字，并在到期时解锁按钮。
     */
    private void refreshAllCardTimers() {
        for (View card : cardViews) {
            Prescripts_finished item = cardData.get(card);
            if (item == null) continue;
            TextView tvTime = card.findViewById(R.id.tv_time);
            Button btnSuccess = card.findViewById(R.id.btn_success);
            Button btnFail = card.findViewById(R.id.btn_fail);
            updateCardTimeText(item, tvTime);
            updateCardButtonState(item, btnSuccess, btnFail);
        }
    }

    private void updateCardTimeText(Prescripts_finished item, TextView tvTime) {
        if (item.isReady()) {
            tvTime.setText("耗时：" + item.getTime_taken() + "秒（已可结算）");
        } else {
            if(item.getTime_taken() >= 61 && item.getRemainingSeconds() >= 61){
                tvTime.setText("耗时：" + item.getTime_taken()/60 + "分钟 | 剩余：" + item.getRemainingSeconds()/60 + "分钟");
            }if(item.getTime_taken() >= 61 && item.getRemainingSeconds() <= 61){
                tvTime.setText("耗时：" + item.getTime_taken()/60 + "分钟 | 剩余：" + item.getRemainingSeconds() + "秒");
            }if(item.getTime_taken() <= 61 && item.getRemainingSeconds() >= 61){
                tvTime.setText("耗时：" + item.getTime_taken() + "秒 | 剩余：" + item.getRemainingSeconds()/60 + "分钟");
            }
            tvTime.setText("耗时：" + item.getTime_taken() + "秒 | 剩余：" + item.getRemainingSeconds() + "秒");
        }
    }

    private void updateCardButtonState(Prescripts_finished item, Button btnSuccess, Button btnFail) {
        boolean ready = item.isReady();
        btnSuccess.setEnabled(ready);
        btnFail.setEnabled(ready);
        btnSuccess.setAlpha(ready ? 1f : 0.4f);
        btnFail.setAlpha(ready ? 1f : 0.4f);
    }

    private void removeCard(View card) {
        cardViews.remove(card);
        cardData.remove(card);
        if (card.getParent() != null) {
            ((FrameLayout) card.getParent()).removeView(card);
        }
        reorderCards();
        persistCards();
    }

    /**
     * 把当前所有挂起卡片的数据整体写回SharedPreferences。
     */
    private void persistCards() {
        ArrayList<Prescripts_finished> list = new ArrayList<>();
        for (View v : cardViews) {
            Prescripts_finished item = cardData.get(v);
            if (item != null) list.add(item);
        }
        store.replaceAllCards(list);
    }

    /**
     * 全局静态方法：供外部调用添加指令
     */
    public static void addPrescriptGlobally(Prescripts_finished item) {
        if (instance != null) {
            instance.addNewPrescript(item);
        }
    }

    public void changeMusic(int musicResId){
        if (mediaPlayer != null) {
            mediaPlayer.stop();   // 停止
            mediaPlayer.release(); // 释放
        }

        // 加载
        mediaPlayer = MediaPlayer.create(this, musicResId);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 当 Activity 进入后台（不可见）时执行
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            wasPlayingBeforeStop = true; // 记录状态：本来正在播放
            mediaPlayer.pause();        // 暂停音乐
        } else {
            wasPlayingBeforeStop = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 当 Activity 重新回到前台（可见）时执行
        if (wasPlayingBeforeStop && mediaPlayer != null) {
            mediaPlayer.start();        // 恢复播放
        }
    }

}