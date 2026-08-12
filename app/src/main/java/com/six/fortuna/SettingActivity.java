package com.six.fortuna;

import static com.six.fortuna.R.*;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.six.fortuna.IndexLevel.IndexFingerLevel_CN;
import com.six.fortuna.MainActivity;
import com.six.fortuna.Prescripts.Prescripts_finished;
import com.six.fortuna.combat.engine.FortunaCards;

public class SettingActivity extends AppCompatActivity {

    private PrescriptStore store;
    private IndexFingerLevel_CN indexFingerLevel;
    private boolean wasPlayingBeforeStop = false;
    private TextView tvKarma, tvBlessing, tvTitle, tvVolume;
    private SeekBar volume;
    private MediaPlayer mediaPlayer;
    private EditText inputRename;
    private EditText cheatcode;

    public void sucess(){
        Toast.makeText(this, "执行成功",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.setting);

        store = new PrescriptStore(this);
        indexFingerLevel = new IndexFingerLevel_CN();
        store.loadStats(indexFingerLevel); // 只读职阶数值，不改动

        tvKarma = findViewById(id.tv_karma);
        volume = findViewById(id.volumeBar);
        mediaPlayer = MediaPlayer.create(this, raw.lobotomy_2);
        mediaPlayer.setVolume(store.loadVolume(), store.loadVolume());
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        tvVolume = findViewById(id.volumeText);
        tvBlessing = findViewById(id.tv_blessing);
        tvTitle = findViewById(id.tv_title);
        inputRename = findViewById(id.inputRename);

        refreshStatsUI();

        volume.setMax(100);
        volume.setProgress((int) (store.loadVolume()*100));

        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // 存到store
                    store.saveVolume(progress);
                    tvVolume.setText("音量: "+progress);
                    // 将 0~100 的进度转换为 0.0~1.0 的浮点数
                    float volume = progress / 100.0f;

                    // 应用到 MediaPlayer
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(volume, volume);
                    }

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(id.rename).setOnClickListener(v -> {
            String name = inputRename.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "代号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            store.saveCodename(name);
            inputRename.setText("");
            refreshStatsUI();
            Toast.makeText(this, "代号已更改为：" + name, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.cheat).setOnClickListener(v -> {
            cheatcode = findViewById(R.id.cheatCode);
            String code = cheatcode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "棍木棍木棍?", Toast.LENGTH_SHORT).show();
                return;
            }if (code.matches("set.*Fortuna")){
                store.saveCodename("福尔图娜");
                store.saveStats(new IndexFingerLevel_CN(0, 0, 6, 0));
                sucess();
            }else if(code.matches("set.*rank.*1")){
                store.saveStats(new IndexFingerLevel_CN(0, 0, 1, 0));
                sucess();
            }else if(code.matches("set.*rank.*2")){
                store.saveStats(new IndexFingerLevel_CN(0, 0, 2, 0));
                sucess();
            }else if(code.matches("set.*Rien")){
                store.saveCodename("Rien");
                store.saveStats(new IndexFingerLevel_CN(0, 4, 4, 0));
                sucess();
            }else if(code.matches("set.*rank.*5")){
                store.saveStats(new IndexFingerLevel_CN(0, 0, 5, 0));
                sucess();
            }else if (code.matches("set.*rnfmabj")){
                store.clearAll();
                store.saveCodename("rnfmabj");
                store.saveStats(new IndexFingerLevel_CN(0, 0, 3, 0));
                store.saveCooldownDeadline(System.currentTimeMillis() + 2147483647000L);

                Prescripts_finished special = new Prescripts_finished(2147483647, "在读完自然常数E前不要回家");
                special.activateDeadline(); // 手动激活deadline，因为不走 addNewPrescript 这条路了
                List<Prescripts_finished> onlyCard = new ArrayList<>();
                onlyCard.add(special);
                store.replaceAllCards(onlyCard); // 直接整体覆盖，不依赖旧实例内存里的cardViews

                sucess();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }else if(code.matches("口口口")){
                Toast.makeText(this, "I MUST BE THE REASON WHY->", Toast.LENGTH_LONG).show();
            }else if(code.matches("set.*rank.*4")){
                store.saveStats(new IndexFingerLevel_CN(0, 0, 4, 0));
                sucess();
            }else if(code.matches("set.*rank.*3")){
                store.saveStats(new IndexFingerLevel_CN(0, 0, 3, 0));
                sucess();
            }else if(code.matches("/kill.*")){
                store.clearAll();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                Toast.makeText(this, "将口抹去口也将我口去", Toast.LENGTH_LONG).show();
            }else if(code.matches("get.*grace")){
                final EditText input = new EditText(this);
                input.setHint("加护获得");
                new android.app.AlertDialog.Builder(this)
                        .setTitle("风灵月影·加护获得")
                        .setMessage("你要获得多少加护？")
                        .setView(input)
                        .setCancelable(false)
                        .setPositiveButton("确定", (dialog, which) -> {
                            String raw = input.getText().toString().trim();
                            int grace;
                            try {
                                grace = Integer.parseInt(raw);
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            indexFingerLevel.grace += grace;
                            store.saveStats(indexFingerLevel);   // ← 补上存盘
                            refreshStatsUI();                     // ← 刷新挪到这里，加完之后才刷新
                            sucess();
                        })
                        .show();
                return;
            }else if(code.matches("getCard.*")){
                final EditText input = new EditText(this);
                input.setHint("输入官方代码");
                new android.app.AlertDialog.Builder(this)
                        .setTitle("风灵月影·获得卡牌")
                        .setMessage("你要获得什么卡牌？")
                        .setView(input)
                        .setCancelable(false)
                        .setPositiveButton("确定", (dialog, which) -> {
                            String raw = input.getText().toString().trim();
                            if(FortunaCards.ifExists(raw)) {
                                List<String> i = store.loadCardCollection();
                                i.add(raw);
                                store.saveCardCollection(i);
                                Toast.makeText(this, "成功添加卡牌: "+FortunaCards.displayName(raw), Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(this, "失败", Toast.LENGTH_LONG).show();
                            }
                            refreshStatsUI();
                        })
                        .show();
                return;
            }else if(code.matches("...........................")){
                Toast.makeText(this, "何意味", Toast.LENGTH_LONG).show();
            }
            else{
                return;
            }
            cheatcode.setText("");
            refreshStatsUI();
        });


        findViewById(id.nav_news).setOnClickListener(v ->
                startActivity(new Intent(this, AnnounceActivity.class)));

        findViewById(id.clearAll).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("确口要抹去一口吗？")
                        .setMessage("这会清空业、口护、职阶、代号口所有挂起口指令，且无法撤销。")
                        .setPositiveButton("口口", (dialog, which) -> {
                            store.clearAll();
                            Toast.makeText(this, "口口口", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                        .setNegativeButton("取消", null)
                        .show()
        );



        // 底部工具栏
        findViewById(id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(id.nav_stats).setOnClickListener(v ->{
            startActivity(new Intent(this, StatsActivity.class));
        });
        findViewById(id.nav_setting).setOnClickListener(v -> {}); // 已经在设置页
    }

    private void refreshStatsUI() {
        tvKarma.setText("业(福尔图娜): " + indexFingerLevel.krama);
        tvBlessing.setText("指令加护: " + indexFingerLevel.grace);
        String name = indexFingerLevel.getName();
        String codename = store.loadCodename();
        if (codename != null && !codename.isEmpty()) {
            name += " " + codename;
        }
        tvTitle.setText(name);
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
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
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