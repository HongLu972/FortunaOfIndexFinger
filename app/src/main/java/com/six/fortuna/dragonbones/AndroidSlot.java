package com.six.fortuna.dragonbones;

import android.graphics.Matrix;
import com.dragonbones.armature.Armature;
import com.dragonbones.armature.Slot;

/**
 * 对应 GdxFactory 里的 GdxSlot。
 * 这是一个最小实现：只支持普通图片贴图（DisplayType.Image），
 * 不支持网格（Mesh）、混合模式（BlendMode）、颜色滤镜，也不支持嵌套子骨架。
 * 对于"把导出的骨骼动画显示出来"这个目标已经够用；这几个能力如果以后需要，
 * 可以在 _updateMesh / _updateBlendMode / _updateColor 里继续补。
 */
public class AndroidSlot extends Slot {

    private AndroidBitmapDisplay _renderDisplay;

    public AndroidBitmapDisplay getRenderDisplay() {
        return this._renderDisplay;
    }

    @Override
    protected void _onClear() {
        super._onClear();
        this._renderDisplay = null;
    }

    @Override
    protected void _initDisplay(Object value) {
        // AndroidBitmapDisplay 在 AndroidFactory._buildSlot 里已经创建好了，这里不需要额外分配。
    }

    @Override
    protected void _disposeDisplay(Object value) {
        // 没有需要主动释放的原生资源（Bitmap 由 AndroidTextureData 持有和缓存）。
    }

    @Override
    protected void _onUpdateDisplay() {
        Object display = this._display != null ? this._display : this._rawDisplay;
        this._renderDisplay = (AndroidBitmapDisplay) display;
    }

    @Override
    protected void _addDisplay() {
        AndroidArmatureDisplay container = (AndroidArmatureDisplay) this._armature.getDisplay();
        container.addChild(this);
    }

    @Override
    protected void _replaceDisplay(Object value) {
        // 每个 Slot 固定对应一个 AndroidBitmapDisplay 实例，容器里不需要做替换。
    }

    @Override
    protected void _removeDisplay() {
        AndroidArmatureDisplay container = (AndroidArmatureDisplay) this._armature.getDisplay();
        if (container != null) {
            container.removeChild(this);
        }
    }

    @Override
    protected void _updateZOrder() {
        AndroidArmatureDisplay container = (AndroidArmatureDisplay) this._armature.getDisplay();
        int index = container.getChildIndex(this);
        if (index == (int) this._zOrder) {
            return;
        }
        container.addChildAt(this, (int) this._zOrder);
    }

    @Override
    public void _updateVisible() {
        if (this._renderDisplay != null) {
            this._renderDisplay.visible = this._parent.getVisible();
        }
    }

    @Override
    protected void _updateBlendMode() {
        // 暂不支持混合模式，统一按 Normal 处理。
    }

    @Override
    protected void _updateColor() {
        if (this._renderDisplay != null) {
            this._renderDisplay.alpha = this._colorTransform.alphaMultiplier;
        }
    }

    @Override
    protected void _updateFrame() {
        if (this._renderDisplay == null) {
            return;
        }

        AndroidTextureData textureData = this._textureData instanceof AndroidTextureData
                ? (AndroidTextureData) this._textureData
                : null;

        if (this._displayIndex >= 0 && this._display != null && textureData != null) {
            this._renderDisplay.bitmap = textureData.getDisplayBitmap();
        } else {
            this._renderDisplay.bitmap = null;
        }
    }

    @Override
    protected void _updateMesh() {
        // 暂不支持网格形变（本工厂 _isSupportMesh() 返回 false，正常情况下这里不会被调用到）。
    }

    @Override
    protected void _updateTransform(boolean isSkinnedMesh) {
        if (this._renderDisplay == null) {
            return;
        }

        com.dragonbones.geom.Matrix g = this.globalTransformMatrix;

        Matrix m = new Matrix();
        // DragonBones Matrix 的点变换公式是 x' = a*x + c*y + tx, y' = b*x + d*y + ty，
        // 对应 Android Matrix 的 setValues 数组顺序是
        // {MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y, 0, 0, 1}。
        m.setValues(new float[]{
                g.a, g.c, g.tx,
                g.b, g.d, g.ty,
                0f, 0f, 1f
        });

        // pivot 是"贴图左上角相对于插槽原点的偏移"（未旋转、未缩放前的像素单位），
        // 需要在应用骨骼变换之前先把局部坐标平移过去。
        m.preTranslate(-this._pivotX, -this._pivotY);

        this._renderDisplay.drawMatrix.set(m);
    }
}
