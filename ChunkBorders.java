package dev.eclipse.module.world;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;

public final class ChunkBorders extends Module {
    private final NumberSetting radius = add(new NumberSetting("Zasięg chunków", "Ile chunków wokół gracza", 1, 0, 3, 1));
    private final NumberSetting height = add(new NumberSetting("Wysokość", "Pół-wysokość rysowanych granic (bloki)", 24, 8, 128, 8));
    private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFF12A594));

    public ChunkBorders() { super("Chunk Borders", "Granice chunków wokół gracza", Category.WORLD); }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
        double y0 = mc.player.getY() - height.get(), y1 = mc.player.getY() + height.get();
        int r = radius.i();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double x = (cx + dx) * 16.0, z = (cz + dz) * 16.0;
                Projection.wireBox(g, new AABB(x, y0, z, x + 16, y1, z + 16), 1, dx == 0 && dz == 0 ? color.get() : (color.get() & 0x00FFFFFF) | 0x66000000);
            }
    }
}
