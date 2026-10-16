package dev.eclipse.module.combat;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ModeSetting;
import dev.eclipse.setting.NumberSetting;
import dev.eclipse.module.render.Esp;
import dev.eclipse.util.Safety;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/**
 * EKSPERYMENTALNE środowisko testowe. Działa wyłącznie w lokalnym świecie (Safety.automationAllowed()).
 * Nie celuje w graczy, wymaga linii widoczności (brak ataków przez ściany), brak ukrywania/omijania czegokolwiek.
 */
public final class KillAuraTest extends Module {
    private final ModeSetting targets = add(new ModeSetting("Cele", "Atrapa = stojak na zbroję", "Atrapa", "Atrapa", "Moby", "Atrapa + moby"));
    private final NumberSetting range = add(new NumberSetting("Zasięg", "Zasięg testowy (bloki)", 3.0, 1.0, 6.0, 0.1));
    private final NumberSetting strength = add(new NumberSetting("Siła ataku", "Minimalny poziom naładowania ataku (szybkość symulacji)", 1.0, 0.3, 1.0, 0.05));
    private final BoolSetting show = add(new BoolSetting("Pokaż cel", "Obrys wybranego celu", true));

    private LivingEntity target;
    public static int hits;

    public KillAuraTest() { super("KillAura (test)", "Test automatyzacji ataku – tylko lokalny świat", Category.COMBAT, true); }

    public LivingEntity target() { return target; }

    @Override protected void onEnable() {
        if (!Safety.automationAllowed()) {
            Eclipse.notifications().push("KillAura (test): " + Safety.deniedReason(), 0xFFF76B15);
            setEnabled(false);
        }
        hits = 0;
    }

    @Override public void onTick() {
        if (!Safety.automationAllowed()) { Eclipse.notifications().push("KillAura (test) wyłączony: " + Safety.deniedReason(), 0xFFF76B15); setEnabled(false); return; }
        var p = mc.player;
        if (mc.screen != null || p.isUsingItem() || !p.isAlive()) { return; }
        if (!valid(target)) target = pick();
        if (target == null) return;
        if (p.getAttackStrengthScale(0f) >= strength.get()) {
            mc.gameMode.attack(p, target);
            p.swing(InteractionHand.MAIN_HAND);
            hits++;
        }
    }

    private boolean valid(Entity e) {
        return e instanceof LivingEntity le && le.isAlive() && !le.isRemoved() && matches(le)
                && le.distanceTo(mc.player) <= range.get() && mc.player.hasLineOfSight(le);
    }

    private boolean matches(LivingEntity le) {
        if (le instanceof Player || le == mc.player) return false;
        boolean dummy = le instanceof ArmorStand;
        return switch (targets.get()) { case "Atrapa" -> dummy; case "Moby" -> !dummy; default -> true; };
    }

    private LivingEntity pick() {
        LivingEntity best = null; double bd = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || !valid(le)) continue;
            double d = le.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = le; }
        }
        return best;
    }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (!show.get() || target == null || target.isRemoved()) return;
        Projection.begin(g, pt);
        Projection.wireBox(g, Esp.interpolatedBox(target, pt), 2, 0xFFE5484D);
    }

    @Override protected void onDisable() { target = null; }
    @Override public void onWorldLeave() { target = null; }
}
