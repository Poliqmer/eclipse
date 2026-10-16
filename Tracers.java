package dev.eclipse.module.render;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Draw;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ModeSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class Tracers extends Module {
    private final TargetFilter filter = new TargetFilter(this::add, false, 64);
    private final ModeSetting origin = add(new ModeSetting("Punkt startu", "Skąd wychodzą linie", "Celownik", "Celownik", "Dół ekranu"));
    private final NumberSetting thickness = add(new NumberSetting("Grubość", "Grubość linii (px)", 1, 1, 4, 1));
    private final NumberSetting alpha = add(new NumberSetting("Krycie", "Przezroczystość linii", 0.85, 0.1, 1.0, 0.05));
    private final BoolSetting distance = add(new BoolSetting("Dystans", "Wyświetl odległość przy końcu linii", false));

    public Tracers() { super("Tracers", "Linie do wybranych encji", Category.RENDER); }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        double ox = g.guiWidth() / 2.0, oy = origin.is("Dół ekranu") ? g.guiHeight() : g.guiHeight() / 2.0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!filter.accepts(e, mc.player)) continue;
            double x = Mth.lerp(pt, e.xOld, e.getX()), y = Mth.lerp(pt, e.yOld, e.getY()) + e.getBbHeight() / 2, z = Mth.lerp(pt, e.zOld, e.getZ());
            Projection.projectClamped(x, y, z);
            int col = Draw.mulAlpha(filter.color(e), alpha.f());
            Draw.line(g, ox, oy, Projection.sx, Projection.sy, thickness.f(), col);
            if (distance.get() && Projection.camZ > Projection.NEAR) {
                String s = Math.round(e.distanceTo(mc.player)) + "m";
                g.drawString(mc.font, s, (int) Projection.sx + 3, (int) Projection.sy - 4, col | 0xFF000000, true);
            }
        }
    }
}
