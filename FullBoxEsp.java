package dev.eclipse.module.render;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Draw;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Pełna ramka przestrzenna 3D (12 krawędzi bounding boxa). To wizualizacja – nie stawia żadnych bloków. */
public final class FullBoxEsp extends Module {
    private final TargetFilter filter = new TargetFilter(this::add, false, 96);
    private final NumberSetting width = add(new NumberSetting("Grubość", "Grubość linii", 1, 1, 4, 1));
    private final BoolSetting fill = add(new BoolSetting("Wypełnienie", "Półprzezroczyste wypełnienie ramki", false));
    private final NumberSetting fillAlpha = add(new NumberSetting("Krycie wypełnienia", "", 0.15, 0.05, 0.6, 0.05));
    private final NumberSetting alpha = add(new NumberSetting("Krycie linii", "", 0.95, 0.1, 1.0, 0.05));

    public FullBoxEsp() {
        super("FullBox ESP", "Pełna ramka 3D wokół encji (tylko wizualizacja)", Category.RENDER);
        fillAlpha.showIf(fill::get);
    }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!filter.accepts(e, mc.player)) continue;
            AABB b = Esp.interpolatedBox(e, pt);
            int base = filter.color(e);
            if (fill.get() && Projection.screenRect(b)) {
                g.fill((int) Projection.rect[0], (int) Projection.rect[1], (int) Projection.rect[2], (int) Projection.rect[3], Draw.mulAlpha(base, fillAlpha.f()));
            }
            Projection.wireBox(g, b, width.f(), Draw.mulAlpha(base, alpha.f()));
        }
    }
}
