package dev.eclipse.module.render;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Draw;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemEsp extends Module {
    private final NumberSetting range = add(new NumberSetting("Zasięg", "Maksymalny dystans", 48, 8, 128, 1));
    private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFFFFC53D));
    private final BoolSetting names = add(new BoolSetting("Nazwy", "Pokaż nazwę i liczbę", true));

    public ItemEsp() { super("Item ESP", "Podświetla przedmioty leżące na ziemi", Category.RENDER); }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        double r2 = range.get() * range.get();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity ie) || ie.isRemoved() || ie.distanceToSqr(mc.player) > r2) continue;
            var b = Esp.interpolatedBox(ie, pt).inflate(0.05);
            Projection.wireBox(g, b, 1, color.get());
            if (names.get() && Projection.project((b.minX + b.maxX) / 2, b.maxY + 0.15, (b.minZ + b.maxZ) / 2)) {
                var st = ie.getItem();
                String s = st.getHoverName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : "");
                g.drawString(mc.font, s, (int) Projection.sx - mc.font.width(s) / 2, (int) Projection.sy - 8, 0xFFFFFFFF, true);
            }
        }
    }
}
