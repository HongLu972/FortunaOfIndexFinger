package com.six.fortuna.dragonbones;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import com.dragonbones.armature.Armature;

/**
 * 一个最小可用的 DragonBones 展示 View：
 * - 用 Choreographer 每帧驱动 armature.advanceTime()
 * - onDraw 里按 AndroidArmatureDisplay.children（已按 zOrder 排好）依次把每个 slot 的贴图画出来
 *
 * 用法（Activity 里）：
 *   dragonBonesView.setArmature(armature);
 *   dragonBonesView.play(); // 开始驱动动画
 */
public class DragonBonesView extends View implements Choreographer.FrameCallback {

    private Armature _armature;
    private final Paint _paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private long _lastFrameTimeNanos = 0L;
    private boolean _playing = false;

    /** 骨架坐标系原点在 View 里的位置；默认画在 View 中心，可以按需要调整。 */
    public float originX = -1f; // -1 表示"使用 View 宽度的一半"
    public float originY = -1f;
    public float scale = 1f;

    public DragonBonesView(Context context) {
        super(context);
    }

    public DragonBonesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setArmature(Armature armature) {
        this._armature = armature;
        invalidate();
    }

    public Armature getArmature() {
        return this._armature;
    }

    public void play() {
        if (this._playing) return;
        this._playing = true;
        this._lastFrameTimeNanos = 0L;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void pause() {
        this._playing = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!this._playing) return;

        if (this._lastFrameTimeNanos != 0L && this._armature != null) {
            float deltaSeconds = (frameTimeNanos - this._lastFrameTimeNanos) / 1_000_000_000f;
            // 防止切后台回来后一次性推进一个巨大的 deltaTime。
            if (deltaSeconds > 0.1f) deltaSeconds = 1f / 60f;
            this._armature.advanceTime(deltaSeconds);
        }
        this._lastFrameTimeNanos = frameTimeNanos;

        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (this._armature == null) {
            return;
        }

        Object displayObj = this._armature.getDisplay();
        if (!(displayObj instanceof AndroidArmatureDisplay)) {
            return;
        }
        AndroidArmatureDisplay display = (AndroidArmatureDisplay) displayObj;

        float ox = (this.originX >= 0f) ? this.originX : getWidth() / 2f;
        float oy = (this.originY >= 0f) ? this.originY : getHeight() / 2f;

        int saveCount = canvas.save();
        canvas.translate(ox, oy);
        if (this.scale != 1f) {
            canvas.scale(this.scale, this.scale);
        }

        for (AndroidSlot slot : display.children) {
            AndroidBitmapDisplay render = slot.getRenderDisplay();
            if (render == null || render.bitmap == null || !render.visible) {
                continue;
            }

            int slotSave = canvas.save();
            canvas.concat(render.drawMatrix);
            _paint.setAlpha(Math.round(clamp01(render.alpha) * 255));
            canvas.drawBitmap(render.bitmap, 0f, 0f, _paint);
            canvas.restoreToCount(slotSave);
        }

        canvas.restoreToCount(saveCount);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
