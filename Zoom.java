package dev.eclipse.module.render;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.NumberSetting;

/** Zoom przez płynną zmianę opcji FOV; oryginalna wartość jest przywracana po wyłączeniu. */
public final class Zoom extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", "Docelowy FOV", 30, 10, 70, 1));
    private final NumberSetting speed = add(new NumberSetting("Płynność", "Krok FOV na tick", 6, 1, 30, 1));
    private Integer original;

    public Zoom() { super("Zoom", "Przybliżenie widoku", Category.RENDER); }

    @Override protected void onEnable() { original = mc.options.fov().get(); }

    @Override public void onTick() {
        int cur = mc.options.fov().get(), target = fov.i();
        int step = (int) Math.max(1, Math.min(speed.i(), Math.abs(target - cur)));
        if (cur != target) mc.options.fov().set(cur + Integer.signum(target - cur) * step);
    }

    @Override protected void onDisable() { if (original != null) mc.options.fov().set(original); original = null; }
}
