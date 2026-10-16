package dev.eclipse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Proste powiadomienia w rogu ekranu. */
public final class Notifications {
    private record Entry(String text, int color, long until) { }
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public void push(String text, int argb) {
        entries.addLast(new Entry(text, argb, System.currentTimeMillis() + 3500));
        while (entries.size() > 5) entries.removeFirst();
    }

    public void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) if (it.next().until < now) it.remove();
        int y = g.guiHeight() - 48;
        for (Entry e : entries) {
            int w = mc.font.width(e.text) + 12;
            int x = g.guiWidth() - w - 6;
            g.fill(x, y, x + w, y + 14, 0xCC101216);
            g.fill(x, y, x + 2, y + 14, e.color | 0xFF000000);
            g.drawString(mc.font, e.text, x + 7, y + 3, 0xFFFFFFFF, false);
            y -= 17;
        }
    }
    public void clear() { entries.clear(); }
}
