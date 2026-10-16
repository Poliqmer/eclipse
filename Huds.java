package dev.eclipse.module.hud;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Proste moduły HUD. */
public final class Huds {
    private Huds() { }

    public static final class FpsPing extends HudModule {
        public FpsPing() { super("FPS i ping", "Klatki na sekundę i opóźnienie", 1, 1); }
        @Override protected void lines(List<String> out) {
            out.add(mc.getFps() + " FPS");
            var con = mc.getConnection();
            var info = con == null ? null : con.getPlayerInfo(mc.player.getUUID());
            if (info != null && !mc.hasSingleplayerServer()) out.add(info.getLatency() + " ms");
        }
    }

    public static final class Coordinates extends HudModule {
        private final BoolSetting decimals = add(new BoolSetting("Ułamki", "Pokaż część dziesiętną", false));
        public Coordinates() { super("Koordynaty", "Pozycja gracza", 1, 12); }
        @Override protected void lines(List<String> out) {
            out.add(decimals.get() ? String.format("XYZ %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ())
                    : "XYZ " + mc.player.getBlockX() + " / " + mc.player.getBlockY() + " / " + mc.player.getBlockZ());
        }
    }

    public static final class Direction extends HudModule {
        public Direction() { super("Kierunek świata", "Strona świata i yaw", 1, 18); }
        @Override protected void lines(List<String> out) {
            float yaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot());
            out.add(mc.player.getDirection().getName().toUpperCase() + String.format(" (%.0f°)", yaw));
        }
    }

    public static final class Biome extends HudModule {
        public Biome() { super("Biom", "Aktualny biom", 1, 24); }
        @Override protected void lines(List<String> out) {
            out.add("Biom: " + mc.level.getBiome(mc.player.blockPosition()).getRegisteredName());
        }
    }

    public static final class Clock extends HudModule {
        private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");
        public Clock() { super("Zegar", "Czas systemowy", 99, 1); }
        @Override protected void lines(List<String> out) { out.add(LocalTime.now().format(F)); }
    }

    public static final class ActiveModules extends Module {
        private final BoolSetting experimentalTag = add(new BoolSetting("Oznacz eksperymentalne", "", true));
        private final List<Module> scratch = new ArrayList<>();
        public ActiveModules() { super("Aktywne moduły", "Lista włączonych modułów", Category.HUD); }
        @Override public void onRender(GuiGraphics g, float pt) {
            scratch.clear();
            for (Module m : Eclipse.modules().all()) if (m.isEnabled() && m != this) scratch.add(m);
            scratch.sort(Comparator.comparingInt((Module m) -> -mc.font.width(label(m))));
            int y = 3;
            for (Module m : scratch) {
                String s = label(m);
                int w = mc.font.width(s) + 8, x = g.guiWidth() - w - 2;
                g.fill(x, y, x + w, y + 11, 0x80101216);
                g.fill(x + w - 2, y, x + w, y + 11, Eclipse.ui().accent.get());
                g.drawString(mc.font, s, x + 3, y + 2, 0xFFFFFFFF, true);
                y += 11;
            }
        }
        private String label(Module m) { return m.name + (experimentalTag.get() && m.experimental ? " [exp]" : ""); }
    }

    /** CPS liczone przez odpytywanie stanu przycisków myszy (bez mixinów). */
    public static final class Cps extends HudModule {
        private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
        private boolean lastL, lastR;
        public Cps() { super("CPS", "Kliknięcia na sekundę", 1, 30); }
        @Override public void onTick() {
            long w = mc.getWindow().handle(), now = System.currentTimeMillis();
            boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (l && !lastL) left.addLast(now);
            if (r && !lastR) right.addLast(now);
            lastL = l; lastR = r;
            while (!left.isEmpty() && now - left.peekFirst() > 1000) left.removeFirst();
            while (!right.isEmpty() && now - right.peekFirst() > 1000) right.removeFirst();
        }
        @Override protected void lines(List<String> out) { out.add("CPS " + left.size() + " | " + right.size()); }
        @Override public void onWorldLeave() { left.clear(); right.clear(); }
        @Override protected void onDisable() { left.clear(); right.clear(); }
    }

    public static final class Keystrokes extends Module {
        private final NumberSetting x = add(new NumberSetting("Pozycja X %", "", 3, 0, 100, 1));
        private final NumberSetting y = add(new NumberSetting("Pozycja Y %", "", 70, 0, 100, 1));
        public Keystrokes() { super("Keystrokes", "Wciśnięte klawisze ruchu", Category.HUD); }
        @Override public void onRender(GuiGraphics g, float pt) {
            int px = (int) ((g.guiWidth() - 60) * x.get() / 100), py = (int) ((g.guiHeight() - 60) * y.get() / 100);
            key(g, px + 20, py, "W", mc.options.keyUp.isDown());
            key(g, px, py + 20, "A", mc.options.keyLeft.isDown());
            key(g, px + 20, py + 20, "S", mc.options.keyDown.isDown());
            key(g, px + 40, py + 20, "D", mc.options.keyRight.isDown());
            key(g, px, py + 40, "SPACE", mc.options.keyJump.isDown(), 58);
        }
        private void key(GuiGraphics g, int x, int y, String s, boolean down) { key(g, x, y, s, down, 18); }
        private void key(GuiGraphics g, int x, int y, String s, boolean down, int w) {
            g.fill(x, y, x + w, y + 18, down ? Eclipse.ui().accent.get() : 0x90101216);
            g.drawString(mc.font, s, x + (w - mc.font.width(s)) / 2, y + 5, 0xFFFFFFFF, false);
        }
    }

    public static final class ArmorHud extends Module {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        private final NumberSetting x = add(new NumberSetting("Pozycja X %", "", 50, 0, 100, 1));
        public ArmorHud() { super("Armor HUD", "Pancerz i przedmiot w ręce z wytrzymałością", Category.HUD); }
        @Override public void onRender(GuiGraphics g, float pt) {
            if (mc.player == null) return;
            int px = (int) ((g.guiWidth() - 90) * x.get() / 100), py = g.guiHeight() - 82;
            for (EquipmentSlot s : SLOTS) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (st.isEmpty()) continue;
                g.renderItem(st, px, py);
                g.renderItemDecorations(mc.font, st, px, py);
                px += 18;
            }
        }
    }

    public static final class PotionHud extends HudModule {
        public PotionHud() { super("Potion HUD", "Aktywne efekty i czas", 1, 45); }
        @Override protected void lines(List<String> out) {
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                int s = e.isInfiniteDuration() ? -1 : e.getDuration() / 20;
                out.add(e.getEffect().value().getDisplayName().getString() + " " + (e.getAmplifier() + 1) + (s < 0 ? " ∞" : String.format(" %d:%02d", s / 60, s % 60)));
            }
        }
    }

    public static final class InventoryHud extends Module {
        private final NumberSetting x = add(new NumberSetting("Pozycja X %", "", 99, 0, 100, 1));
        private final NumberSetting y = add(new NumberSetting("Pozycja Y %", "", 40, 0, 100, 1));
        public InventoryHud() { super("Inventory HUD", "Podgląd ekwipunku (27 slotów)", Category.PLAYER); }
        @Override public void onRender(GuiGraphics g, float pt) {
            if (mc.player == null) return;
            int w = 9 * 18 + 4, h = 3 * 18 + 4;
            int px = (int) ((g.guiWidth() - w) * x.get() / 100), py = (int) ((g.guiHeight() - h) * y.get() / 100);
            g.fill(px, py, px + w, py + h, 0x90101216);
            for (int i = 9; i < 36; i++) {
                ItemStack st = mc.player.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                int sx = px + 2 + (i % 9) * 18, sy = py + 2 + (i / 9 - 1) * 18;
                g.renderItem(st, sx, sy);
                g.renderItemDecorations(mc.font, st, sx, sy);
            }
        }
    }

    public static final class CustomCrosshair extends Module {
        private final NumberSetting size = add(new NumberSetting("Rozmiar", "", 5, 1, 20, 1));
        private final NumberSetting gap = add(new NumberSetting("Odstęp", "", 2, 0, 10, 1));
        private final NumberSetting thick = add(new NumberSetting("Grubość", "", 1, 1, 4, 1));
        private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFFFFFFFF));
        public CustomCrosshair() { super("Custom Crosshair", "Własny celownik (rysowany nad vanilla)", Category.HUD); }
        @Override public void onRender(GuiGraphics g, float pt) {
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2, s = size.i(), gp = gap.i(), t = thick.i(), c = color.get();
            int o = t / 2;
            g.fill(cx - gp - s, cy - o, cx - gp, cy - o + t, c);
            g.fill(cx + gp, cy - o, cx + gp + s, cy - o + t, c);
            g.fill(cx - o, cy - gp - s, cx - o + t, cy - gp, c);
            g.fill(cx - o, cy + gp, cx - o + t, cy + gp + s, c);
        }
    }
}
