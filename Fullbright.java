package dev.eclipse.module.render;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.ModeSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Tryb "Gamma": płynnie podnosi opcję jasności (maks. wartość suwaka vanilla = 1.0) i przywraca oryginał.
 * Tryb "Efekt": lokalny, kliencki efekt widzenia w nocy (serwer o nim nie wie; usuwany przy wyłączeniu).
 * Żadnych zmian w świecie.
 */
public final class Fullbright extends Module {
    private final ModeSetting mode = add(new ModeSetting("Tryb", "Gamma = opcja jasności, Efekt = lokalny night vision", "Gamma", "Gamma", "Efekt"));
    private final NumberSetting brightness = add(new NumberSetting("Jasność", "Docelowa jasność (tryb Gamma)", 1.0, 0.0, 1.0, 0.05));
    private final NumberSetting speed = add(new NumberSetting("Płynność", "Krok zmiany na tick", 0.08, 0.01, 1.0, 0.01));
    private Double original;
    private boolean addedEffect;

    public Fullbright() { super("Fullbright", "Rozjaśnia świat bez zmian w świecie", Category.RENDER); }

    @Override protected void onEnable() { original = mc.options.gamma().get(); }

    @Override public void onTick() {
        if (mc.player == null) return;
        if (mode.is("Gamma")) {
            removeEffect();
            double cur = mc.options.gamma().get();
            double target = brightness.get();
            double next = cur + Math.max(-speed.get(), Math.min(speed.get(), target - cur));
            mc.options.gamma().set(Math.max(0, Math.min(1, next)));
        } else {
            if (original != null) mc.options.gamma().set(original);
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                addedEffect = true;
            }
        }
    }

    private void removeEffect() {
        if (addedEffect && mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
        addedEffect = false;
    }

    @Override protected void onDisable() {
        removeEffect();
        if (original != null) mc.options.gamma().set(original);
        original = null;
    }

    @Override public void onWorldLeave() { addedEffect = false; }
}
