package dev.eclipse.module.combat;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** HUD walki: informacje o celu (pod celownikiem lub cel testowy), cooldown ataku, okrąg zasięgu. */
public final class CombatHud extends Module {
    private final BoolSetting cooldown = add(new BoolSetting("Cooldown ataku", "Pasek naładowania ataku", true));
    private final BoolSetting targetInfo = add(new BoolSetting("Info o celu", "Nazwa, zdrowie, pancerz, dystans", true));
    private final BoolSetting rangeCircle = add(new BoolSetting("Okrąg zasięgu", "Okrąg wokół gracza na ziemi", false));
    private final NumberSetting circleRadius = add(new NumberSetting("Promień okręgu", "Bloki", 3.0, 1.0, 6.0, 0.5));
    private final ColorSetting circleColor = add(new ColorSetting("Kolor okręgu", "", 0xFFF76B15));
    private final BoolSetting stats = add(new BoolSetting("Statystyki", "Liczba trafień modułu testowego", true));

    public CombatHud() { super("Combat HUD", "Informacje o walce i celu", Category.COMBAT); }

    @Override public void onRender(GuiGraphics g, float pt) {
        var p = mc.player;
        if (p == null || mc.level == null) return;
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        if (cooldown.get()) {
            float s = Math.min(1f, p.getAttackStrengthScale(pt));
            g.fill(cx - 20, cy + 14, cx + 20, cy + 17, 0x90101216);
            g.fill(cx - 20, cy + 14, cx - 20 + (int) (40 * s), cy + 17, s >= 1f ? 0xFF46A758 : Eclipse.ui().accent.get());
        }
        if (targetInfo.get()) {
            Entity t = null;
            for (Module m : Eclipse.modules().all()) if (m instanceof KillAuraTest k && m.isEnabled()) t = k.target();
            if (t == null && mc.crosshairPickEntity != null) t = mc.crosshairPickEntity;
            if (t instanceof LivingEntity le && le.isAlive()) {
                int x = cx + 14, y = cy - 18;
                g.fill(x - 3, y - 3, x + 96, y + 34, 0x90101216);
                g.drawString(mc.font, le.getName().getString(), x, y, 0xFFFFFFFF, true);
                float hp = le.getHealth(), max = Math.max(1f, le.getMaxHealth());
                g.fill(x, y + 11, x + 90, y + 14, 0xFF2A2D34);
                g.fill(x, y + 11, x + (int) (90 * Math.min(1f, hp / max)), y + 14, 0xFFE5484D);
                g.drawString(mc.font, String.format("%.1f/%.0f  Pancerz %d", hp, max, le.getArmorValue()), x, y + 17, 0xFFCCCCCC, false);
                g.drawString(mc.font, Math.round(le.distanceTo(p) * 10) / 10.0 + "m", x, y + 26, 0xFFCCCCCC, false);
            }
        }
        if (stats.get()) g.drawString(mc.font, "Trafienia (test): " + KillAuraTest.hits, 4, g.guiHeight() - 12, 0xFFAAAAAA, false);
        if (rangeCircle.get()) {
            Projection.begin(g, pt);
            double x = p.getX(), y = p.getY() + 0.05, z = p.getZ(), r = circleRadius.get();
            int n = 48;
            for (int i = 0; i < n; i++) {
                double a1 = Math.PI * 2 * i / n, a2 = Math.PI * 2 * (i + 1) / n;
                Projection.worldLine(g, x + Math.cos(a1) * r, y, z + Math.sin(a1) * r, x + Math.cos(a2) * r, y, z + Math.sin(a2) * r, 1, circleColor.get());
            }
        }
    }
}
