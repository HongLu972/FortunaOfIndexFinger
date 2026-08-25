package com.six.fortuna.dragonbones;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dragonbones.armature.Armature;
import com.dragonbones.armature.Slot;
import com.dragonbones.core.BaseObject;
import com.dragonbones.core.DragonBones;
import com.dragonbones.factory.BaseFactory;
import com.dragonbones.factory.BuildArmaturePackage;
import com.dragonbones.model.DisplayData;
import com.dragonbones.model.SlotData;
import com.dragonbones.model.TextureAtlasData;
import com.dragonbones.util.Array;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 对应 GdxFactory，是 BaseFactory 在 Android/Canvas 渲染下的具体实现。
 * 通常整个 App 共用一个全局实例（跟官方各语言版本的用法一致）。
 */
public class AndroidFactory extends BaseFactory {

    private static AndroidFactory _instance;

    public static AndroidFactory getInstance() {
        if (_instance == null) {
            _instance = new AndroidFactory();
        }
        return _instance;
    }

    private final DragonBones _dragonBonesCore;

    private AndroidFactory() {
        super();
        // DragonBones 核心需要一个全局事件管理器；这里随便用一个 AndroidArmatureDisplay 占位即可，
        // 它不会被当作任何具体骨架的容器使用。
        this._dragonBonesCore = new DragonBones(new AndroidArmatureDisplay());
    }

    @Override
    protected boolean _isSupportMesh() {
        // 当前 AndroidSlot 没有实现网格形变，返回 false 让 BaseFactory 始终走"普通贴图"分支。
        return false;
    }

    @Override
    protected TextureAtlasData _buildTextureAtlasData(@Nullable TextureAtlasData textureAtlasData, Object textureAtlas) {
        AndroidTextureAtlasData data = textureAtlasData instanceof AndroidTextureAtlasData
                ? (AndroidTextureAtlasData) textureAtlasData
                : BaseObject.borrowObject(AndroidTextureAtlasData.class);

        if (textureAtlas instanceof Bitmap) {
            data.atlasBitmap = (Bitmap) textureAtlas;
        }

        return data;
    }

    @Override
    protected Armature _buildArmature(BuildArmaturePackage dataPackage) {
        Armature armature = BaseObject.borrowObject(Armature.class);
        AndroidArmatureDisplay display = new AndroidArmatureDisplay();
        armature.init(dataPackage.armature, display, display, this._dragonBonesCore);
        return armature;
    }

    @Override
    protected Slot _buildSlot(BuildArmaturePackage dataPackage, SlotData slotData, Array<DisplayData> displays, Armature armature) {
        AndroidSlot slot = BaseObject.borrowObject(AndroidSlot.class);
        AndroidBitmapDisplay display = new AndroidBitmapDisplay();
        // _isSupportMesh() 为 false，BaseFactory 只会用到 getRawDisplay()，
        // 这里 rawDisplay/meshDisplay 传同一个实例即可。
        slot.init(slotData, displays, display, display);
        return slot;
    }

    // ---------------- 资源加载 ----------------

    /**
     * 从 assets 目录加载一套 DragonBones 导出资源（骨架 json + 贴图集 json + 合图 png）。
     * 例如导出的是 Player_ske.json / Player_tex.json / Player_tex.png，放在 assets/dragonbones/ 下：
     * loadFromAssets(getAssets(), "dragonbones/Player_ske.json", "dragonbones/Player_tex.json", "dragonbones/Player_tex.png")
     */
    public void loadFromAssets(AssetManager assets, String skePath, String texJsonPath, String texPngPath) throws IOException {
        String skeJson = readAssetText(assets, skePath);
        String texJson = readAssetText(assets, texJsonPath);

        Bitmap atlasBitmap;
        try (InputStream pngStream = assets.open(texPngPath)) {
            atlasBitmap = BitmapFactory.decodeStream(pngStream);
        }
        if (atlasBitmap == null) {
            throw new IOException("无法解码贴图: " + texPngPath);
        }

        Object skeData;
        Object texData;
        try {
            skeData = JsonUtil.parse(skeJson);
            texData = JsonUtil.parse(texJson);
        } catch (org.json.JSONException e) {
            throw new IOException("解析 DragonBones JSON 失败", e);
        }

        this.parseDragonBonesData(skeData);
        this.parseTextureAtlasData(texData, atlasBitmap);
    }

    private static String readAssetText(AssetManager assets, String path) throws IOException {
        try (InputStream is = assets.open(path)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        }
    }
}
