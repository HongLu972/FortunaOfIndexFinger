package com.six.fortuna.dragonbones;

import com.dragonbones.animation.Animation;
import com.dragonbones.armature.Armature;
import com.dragonbones.armature.IArmatureProxy;
import com.dragonbones.event.EventObject;
import com.dragonbones.event.EventStringType;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * 对应 GdxFactory 里的 GdxArmatureDisplay：既是骨架的"显示容器"（Armature.init 的 display 参数），
 * 也是骨架的事件管理器（Armature.init 的 proxy 参数）。
 * 这里不做真正的事件分发（DragonBonesTestActivity 目前不需要监听动画事件），
 * 全部实现成空方法即可满足接口要求。
 *
 * 同时维护一份按 zOrder 排好的 AndroidSlot 列表，供 DragonBonesView 绘制时使用。
 */
public class AndroidArmatureDisplay implements IArmatureProxy {

    private Armature _armature;
    public final ArrayList<AndroidSlot> children = new ArrayList<>();

    @Override
    public void init(Armature armature) {
        this._armature = armature;
    }

    @Override
    public void clear() {
        this.children.clear();
        this._armature = null;
    }

    @Override
    public void dispose(boolean disposeProxy) {
        clear();
    }

    @Override
    public void debugUpdate(boolean isEnabled) {
        // no-op
    }

    @Override
    public Armature getArmature() {
        return this._armature;
    }

    @Override
    public Animation getAnimation() {
        return this._armature != null ? this._armature.getAnimation() : null;
    }

    // ---- IEventDispatcher：本 Activity 暂不需要事件回调，留空即可 ----

    @Override
    public void _dispatchEvent(EventStringType type, EventObject eventObject) {
    }

    @Override
    public boolean hasEvent(EventStringType type) {
        return false;
    }

    @Override
    public void addEvent(EventStringType type, Consumer<Object> listener, Object target) {
    }

    @Override
    public void removeEvent(EventStringType type, Consumer<Object> listener, Object target) {
    }

    // ---- 供 AndroidSlot 调用的容器管理方法（对应 GdxArmatureDisplay 的 addChild/removeChild 等） ----

    void addChild(AndroidSlot slot) {
        if (!this.children.contains(slot)) {
            this.children.add(slot);
        }
    }

    void addChildAt(AndroidSlot slot, int index) {
        this.children.remove(slot);
        if (index < 0) index = 0;
        if (index > this.children.size()) index = this.children.size();
        this.children.add(index, slot);
    }

    void removeChild(AndroidSlot slot) {
        this.children.remove(slot);
    }

    int getChildIndex(AndroidSlot slot) {
        return this.children.indexOf(slot);
    }
}
