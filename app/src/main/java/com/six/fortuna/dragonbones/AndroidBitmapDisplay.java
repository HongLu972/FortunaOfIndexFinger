package com.six.fortuna.dragonbones;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * 对应 GdxFactory 里的 EgretBitmap：每个 Slot 持有一个这样的实例，
 * 由 AndroidSlot 在 _updateFrame / _updateTransform 里写入贴图和变换矩阵，
 * 由 DragonBonesView 在 onDraw 里读出来绘制。
 */
public class AndroidBitmapDisplay {
    /** 已经裁剪好、方向已还原（未旋转）的贴图，null 表示当前不显示任何图。 */
    public Bitmap bitmap;
    /** 相对于骨架坐标系的绘制矩阵，已经把 pivot 偏移叠加进去。 */
    public final Matrix drawMatrix = new Matrix();
    public boolean visible = true;
    public float alpha = 1f;
}
