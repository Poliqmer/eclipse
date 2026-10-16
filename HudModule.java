package dev.eclipse.module.hud;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Moduł HUD z pozycją w procentach ekranu (niezależną od skali GUI) i tłem. */
public abstract class HudModule extends Module {
    protected final NumberSetting posX, posY;
    protected final BoolSetting background = add(new BoolSetting("Tło", "Półprzezroczyste tło", true));
    private final List<String> scratch = new ArrayList<>();

    protected HudModule(String name, String desc, double x, double y) {
        super(name, desc, Category.HUD);
        posX = add(new NumberSetting("Pozycja X %", "Pozycja pozioma", x, 0, 100, 1));
        posY = add(new NumberSetting("Pozycja Y %", "Pozycja pionowa", y, 0, 100, 1));
    }

    protected abstract void lines(List<String> out);

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.player == null || mc.level == null) return;
        scratch.clear();
        lines(scratch);
        if (scratch.isEmpty()) return;
        drawBlock(g, scratch);
    }

    protected void drawBlock(GuiGraphics g, List<String> lines) {
        int w = 0;
        for (String s : lines) w = Math.max(w, mc.font.width(s));
        int h = lines.size() * 10 + 2;
        int x = (int) ((g.guiWidth() - w - 8) * posX.get() / 100.0);
        int y = (int) ((g.guiHeight() - h - 2) * posY.get() / 100.0);
        if (background.get()) {
            g.fill(x, y, x + w + 8, y + h + 2, 0x90101216);
            g.fill(x, y, x + 2, y + h + 2, Eclipse.ui().accent.get());
        }
        int ty = y + 2;
        for (String s : lines) { g.drawString(mc.font, s, x + 5, ty, 0xFFFFFFFF, true); ty += 10; }
    }
}
