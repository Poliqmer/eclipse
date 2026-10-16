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
import net.minecraft.world.phys.AABB;

public final class Esp extends Module {
    private final TargetFilter filter = new TargetFilter(this::add, false, 96);
    private final ModeSetting style = add(new ModeSetting("Styl", "Ramka 2D lub obrys 3D", "2D", "2D", "3D"));
    private final NumberSetting width = add(new NumberSetting("Szerokość", "Grubość linii", 1, 1, 4, 1));
    private final NumberSetting alpha = add(new NumberSetting("Krycie", "Przezroczystość", 0.9, 0.1, 1.0, 0.05));
    private final BoolSetting names = add(new BoolSetting("Nazwy", "Pokaż nazwy", true));
    private final BoolSetting distance = add(new BoolSetting("Dystans", "Pokaż odległość", true));

    public Esp() { super("ESP", "Ramki 2D / obrysy 3D wokół encji", Category.RENDER); }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!filter.accepts(e, mc.player)) continue;
            AABB b = interpolatedBox(e, pt);
            int col = Draw.mulAlpha(filter.color(e), alpha.f());
            if (style.is("3D")) Projection.wireBox(g, b, width.f(), col);
            else if (Projection.screenRect(b)) {
                Draw.outline(g, (int) Projection.rect[0], (int) Projection.rect[1], (int) Projection.rect[2], (int) Projection.rect[3], Math.max(1, width.i()), col);
            }
            if ((names.get() || distance.get()) && Projection.project((b.minX + b.maxX) / 2, b.maxY + 0.2, (b.minZ + b.maxZ) / 2)) {
                String label = (names.get() ? e.getName().getString() : "") + (distance.get() ? (names.get() ? " " : "") + Math.round(e.distanceTo(mc.player)) + "m" : "");
                g.drawString(mc.font, label, (int) Projection.sx - mc.font.width(label) / 2, (int) Projection.sy - 9, 0xFFFFFFFF, true);
            }
        }
    }

    /** Bounding box przesunięty do interpolowanej pozycji (stabilny względem kamery, bez drgań). */
    public static AABB interpolatedBox(Entity e, float pt) {
        double dx = Mth.lerp(pt, e.xOld, e.getX()) - e.getX();
        double dy = Mth.lerp(pt, e.yOld, e.getY()) - e.getY();
        double dz = Mth.lerp(pt, e.zOld, e.getZ()) - e.getZ();
        return e.getBoundingBox().move(dx, dy, dz);
    }
}
