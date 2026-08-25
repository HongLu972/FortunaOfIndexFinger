package com.six.fortuna.dragonbones;

import android.graphics.Bitmap;
import com.dragonbones.core.BaseObject;
import com.dragonbones.model.TextureAtlasData;
import com.dragonbones.model.TextureData;

public class AndroidTextureAtlasData extends TextureAtlasData {
    /** _tex.png 解码出来的整张合图。 */
    public Bitmap atlasBitmap;

    @Override
    public TextureData createTexture() {
        return BaseObject.borrowObject(AndroidTextureData.class);
    }

    @Override
    protected void _onClear() {
        super._onClear();
        this.atlasBitmap = null;
    }
}
