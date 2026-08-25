package com.six.fortuna;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dragonbones.armature.Armature;
import com.dragonbones.model.DragonBonesData;
import com.six.fortuna.dragonbones.AndroidFactory;
import com.six.fortuna.dragonbones.DragonBonesView;

import java.io.IOException;
import java.util.Map;

/**
 * 临时的展示 Activity：加载导出的 DragonBones 资源并播放动画，用来验证渲染管线是否work。
 *
 * 资源放置方式：把 Player_ske.json / Player_tex.json / Player_tex.png 三个文件
 * 拷贝到 app/src/main/assets/dragonbones/ 目录下（没有 assets 目录就自己新建一个）。
 *
 * 用完之后这个 Activity 可以直接删掉，不影响正式的战斗界面代码。
 */
public class DragonBonesTestActivity extends AppCompatActivity {

    private static final String TAG = "DragonBonesTest";

    // 按你实际导出的文件名改这三个路径。
    private static final String SKE_PATH = "dragonbones/PlayerAttackBlunt/Player_ske.json";
    private static final String TEX_JSON_PATH = "dragonbones/PlayerAttackBlunt/Player_tex.json";
    private static final String TEX_PNG_PATH = "dragonbones/PlayerAttackBlunt/Player_tex.png";

    private DragonBonesView dragonBonesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dragon_bones_test);

        dragonBonesView = findViewById(R.id.dragon_bones_view);

        try {
            AndroidFactory factory = AndroidFactory.getInstance();
            factory.loadFromAssets(getAssets(), SKE_PATH, TEX_JSON_PATH, TEX_PNG_PATH);

            String armatureName = findFirstArmatureName(factory);
            if (armatureName == null) {
                Toast.makeText(this, "没有解析到任何骨架数据", Toast.LENGTH_LONG).show();
                return;
            }

            Armature armature = factory.buildArmature(armatureName);
            if (armature == null) {
                Toast.makeText(this, "buildArmature 失败: " + armatureName, Toast.LENGTH_LONG).show();
                return;
            }

            // 播放该骨架的第一个动画（如果知道具体动画名，可以换成 armature.getAnimation().play("动画名")）。
            armature.getAnimation().play();

            dragonBonesView.setArmature(armature);
            dragonBonesView.play();

            Log.i(TAG, "Loaded armature: " + armatureName);
        } catch (IOException e) {
            Log.e(TAG, "加载 DragonBones 资源失败", e);
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dragonBonesView != null) {
            dragonBonesView.pause();
        }
    }

    /** 从已解析的龙骨数据里取出第一个骨架的名字，避免还要手动去 json 里确认拼写。 */
    private static String findFirstArmatureName(AndroidFactory factory) {
        Map<String, DragonBonesData> all = factory.getAllDragonBonesData();
        for (DragonBonesData data : all.values()) {
            if (data.armatureNames.size() > 0) {
                return data.armatureNames.get(0);
            }
        }
        return null;
    }
}
