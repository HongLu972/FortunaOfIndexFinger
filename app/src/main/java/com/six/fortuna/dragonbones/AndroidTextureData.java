package com.six.fortuna.dragonbones;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.dragonbones.model.TextureData;

public class AndroidTextureData extends TextureData {
    /** 延迟计算并缓存：从合图裁剪出来、方向已还原的小贴图。 */
    private Bitmap _cachedDisplayBitmap;

    @Override
    protected void _onClear() {
        super._onClear();
        this._cachedDisplayBitmap = null;
    }

    /**
     * 返回这个贴图区域对应的、方向已经还原（未旋转）的 Bitmap。
     * TexturePacker/DragonBones 导出贴图集时，为了省空间可能把某些小图整体旋转 90°
     * 塞进合图（region 记录的是旋转后的排布），rotated=true 就表示这种情况，
     * 这里裁剪出来后再转回去，这样上层只需要按未旋转的正常方向绘制即可。
     */
    public Bitmap getDisplayBitmap() {
        if (this._cachedDisplayBitmap != null) {
            return this._cachedDisplayBitmap;
        }

        if (!(this.parent instanceof AndroidTextureAtlasData)) {
            return null;
        }

        AndroidTextureAtlasData atlasData = (AndroidTextureAtlasData) this.parent;
        if (atlasData.atlasBitmap == null) {
            return null;
        }

        int x = (int) this.region.x;
        int y = (int) this.region.y;
        int w = (int) this.region.width;
        int h = (int) this.region.height;
        if (w <= 0 || h <= 0) {
            return null;
        }

        // 边界保护，避免因浮点取整越界导致 createBitmap 抛异常。
        Bitmap atlas = atlasData.atlasBitmap;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + w > atlas.getWidth()) w = atlas.getWidth() - x;
        if (y + h > atlas.getHeight()) h = atlas.getHeight() - y;
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap crop;
        try {
            crop = Bitmap.createBitmap(atlas, x, y, w, h);
        } catch (Exception e) {
            return null;
        }

        if (this.rotated) {
            Matrix rotateMatrix = new Matrix();
            rotateMatrix.postRotate(-90f);
            try {
                crop = Bitmap.createBitmap(crop, 0, 0, crop.getWidth(), crop.getHeight(), rotateMatrix, true);
            } catch (Exception e) {
                // 如果发现动画方向不对，把上面这行的 -90f 改成 90f 试试（旋转方向和导出工具有关）。
            }
        }

        this._cachedDisplayBitmap = crop;
        return this._cachedDisplayBitmap;
    }
}
