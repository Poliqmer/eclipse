package dev.eclipse.gui;

import dev.eclipse.client.Eclipse;
import dev.eclipse.client.UiSettings;
import dev.eclipse.config.ConfigManager;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Draw;
import dev.eclipse.setting.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Eclipse Click GUI. Tryb natychmiastowy: podczas rysowania budowana jest lista obszarów klikalnych ({@link Hit}).
 * Układ liczony we współrzędnych wirtualnych i skalowany ustawieniem "Skala GUI" – mieści się w 16:9 i 16:10.
 */
public final class ClickGuiScreen extends Screen {
    private interface Click { void run(int button, double vx); }
    private record Hit(int x1, int y1, int x2, int y2, Click click, boolean draggable) { }

    private static final int SETTINGS_TAB = -1;
    private static int tab = 0;                 // indeks kategorii lub SETTINGS_TAB (pamiętany między otwarciami)
    private static float scrollMem;

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Float> hover = new IdentityHashMap<>();
    private final Set<Module> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final StringBuilder search = new StringBuilder();
    private Hit dragging;
    private float scroll = scrollMem, maxScroll;
    private float anim;                         // 0..1 animacja otwarcia
    private boolean closing;
    private long lastNanos = System.nanoTime();
    private Object listening;                  // Module | "gui" | "panic"
    private double vmx, vmy;
    private float scale = 1f;

    public ClickGuiScreen() { super(Component.literal("Eclipse")); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { closing = true; }

    @Override public void removed() { scrollMem = scroll; Eclipse.config().save(); }

    // ---------------------------------------------------------------- render
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        float k = 1f - (float) Math.exp(-dt * 14);
        anim += ((closing ? 0f : 1f) - anim) * k;
        if (closing && anim < 0.03f) { minecraft.setScreen(null); return; }

        UiSettings ui = Eclipse.ui();
        scale = ui.scale.f();
        vmx = mouseX / scale; vmy = mouseY / scale;
        float vw = width / scale, vh = height / scale;
        int W = (int) Math.min(500, vw - 16), H = (int) Math.min(320, vh - 16);
        int px = (int) ((vw - W) / 2), py = (int) ((vh - H) / 2 + (1 - anim) * 14);
        float a = anim * ui.opacity.f();
        int accent = ui.accent.get();

        g.fill(0, 0, width, height, Draw.alpha(0x000000, 0.45f * anim)); // przyciemnienie tła

        hits.clear();
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);

        Draw.rounded(g, px - 1, py - 1, px + W + 1, py + H + 1, 6, Draw.alpha(accent, 0.35f * anim));
        Draw.rounded(g, px, py, px + W, py + H, 6, Draw.alpha(0x14161B, a));
        g.fillGradient(px + 6, py + 1, px + W - 6, py + 40, Draw.alpha(accent, 0.16f * anim), 0x00000000);

        // --- pasek boczny
        int sbW = 100;
        g.drawString(font, "ECLIPSE", px + 14, py + 14, Draw.mulAlpha(0xFFFFFFFF, anim), true);
        g.fill(px + 14, py + 26, px + 14 + 26, py + 28, accent);
        Category[] cats = Category.values();
        int ty = py + 40;
        for (int i = 0; i < cats.length; i++) { sideTab(g, px + 8, ty, sbW - 12, cats[i].label, i, count(cats[i]), accent); ty += 22; }
        sideTab(g, px + 8, py + H - 50, sbW - 12, "Ustawienia", SETTINGS_TAB, -1, accent);
        g.drawString(font, "Aktywne: " + Eclipse.modules().activeCount(), px + 14, py + H - 20, 0xFF8B93A1, false);

        // --- nagłówek / wyszukiwarka
        int cx1 = px + sbW + 6, cx2 = px + W - 10, cy1 = py + 38, cy2 = py + H - 24;
        Draw.rounded(g, cx1, py + 10, cx2, py + 28, 4, Draw.alpha(0x23262E, a));
        String shown = search.length() == 0 ? "Szukaj modułów..." : search.toString();
        g.drawString(font, shown + (search.length() > 0 && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""), cx1 + 7, py + 16, search.length() == 0 ? 0xFF6B7280 : 0xFFFFFFFF, false);

        // --- zawartość
        int y = cy1 - (int) scroll;
        if (tab == SETTINGS_TAB && search.length() == 0) y = settingsTab(g, cx1, cx2, y, cy1, cy2, accent, a);
        else y = moduleList(g, cx1, cx2, y, cy1, cy2, accent, a, cats);
        int content = y + (int) scroll - cy1;
        maxScroll = Math.max(0, content - (cy2 - cy1));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        if (maxScroll > 0) { // pasek przewijania
            int bh = Math.max(16, (int) ((cy2 - cy1) * (cy2 - cy1) / (float) content));
            int by = cy1 + (int) ((cy2 - cy1 - bh) * (scroll / maxScroll));
            g.fill(cx2 + 2, by, cx2 + 4, by + bh, Draw.alpha(accent, 0.8f));
        }
        g.drawString(font, "Prawy klik: ustawienia  |  Klik na klawisz: bind (Backspace = usuń)  |  Profil: " + Eclipse.config().profile(),
                px + sbW + 6, py + H - 14, 0xFF6B7280, false);
        g.pose().popMatrix();
    }

    private int count(Category c) { return Eclipse.modules().in(c).size(); }

    private float hov(Object key, boolean on) {
        float v = hover.getOrDefault(key, 0f);
        v += ((on ? 1f : 0f) - v) * 0.25f;
        hover.put(key, v);
        return v;
    }

    private boolean over(int x1, int y1, int x2, int y2) { return vmx >= x1 && vmx < x2 && vmy >= y1 && vmy < y2; }

    private void sideTab(GuiGraphics g, int x, int y, int w, String label, int idx, int n, int accent) {
        boolean sel = tab == idx && search.length() == 0;
        float h = hov(label, over(x, y, x + w, y + 18));
        if (sel) Draw.rounded(g, x, y, x + w, y + 18, 4, Draw.alpha(accent, 0.30f * anim));
        else if (h > 0.02f) Draw.rounded(g, x, y, x + w, y + 18, 4, Draw.alpha(0xFFFFFF, 0.07f * h * anim));
        g.drawString(font, label, x + 8, y + 5, sel ? 0xFFFFFFFF : Draw.lerpColor(0xFF8B93A1, 0xFFFFFFFF, h), false);
        if (n >= 0) g.drawString(font, String.valueOf(n), x + w - 6 - font.width(String.valueOf(n)), y + 5, 0xFF6B7280, false);
        hits.add(new Hit(x, y, x + w, y + 18, (b, vx) -> { tab = idx; search.setLength(0); scroll = 0; }, false));
    }

    // ---------------------------------------------------------------- moduły
    private int moduleList(GuiGraphics g, int x1, int x2, int y, int top, int bottom, int accent, float a, Category[] cats) {
        List<Module> list = new ArrayList<>();
        String q = search.toString().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) list.addAll(Eclipse.modules().in(cats[Math.max(0, Math.min(tab, cats.length - 1))]));
        else for (Module m : Eclipse.modules().all()) if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) list.add(m);
        if (list.isEmpty()) g.drawString(font, "Brak modułów", x1 + 4, y + 4, 0xFF6B7280, false);
        for (Module m : list) y = moduleRow(g, m, x1, x2, y, top, bottom, accent, a);
        return y;
    }

    private int moduleRow(GuiGraphics g, Module m, int x1, int x2, int y, int top, int bottom, int accent, float a) {
        int rh = 22;
        boolean vis = y >= top && y + rh <= bottom;
        if (vis) {
            boolean hv = over(x1, y, x2, y + rh) && over(x1, top, x2, bottom);
            float h = hov(m, hv);
            int base = Draw.lerpColor(0xFF1E2128, 0xFF272B34, h);
            Draw.rounded(g, x1, y, x2, y + rh, 4, Draw.mulAlpha(base, anim * Eclipse.ui().opacity.f()));
            if (m.isEnabled()) g.fill(x1, y + 4, x1 + 2, y + rh - 4, accent);
            g.drawString(font, m.name + (m.experimental ? " §6[exp]" : ""), x1 + 8, y + 3, m.isEnabled() ? 0xFFFFFFFF : 0xFFC4C9D4, false);
            String d = font.plainSubstrByWidth(m.description, x2 - x1 - 130);
            g.drawString(font, d, x1 + 8, y + 12, 0xFF6B7280, false);
            // klawisz
            String kn = listening == m ? "..." : keyName(m.key());
            int kw = font.width(kn) + 8, kx = x2 - 40 - kw;
            Draw.rounded(g, kx, y + 5, kx + kw, y + 17, 3, listening == m ? Draw.alpha(accent, 0.5f) : 0xFF2A2D34);
            g.drawString(font, kn, kx + 4, y + 7, 0xFFB4BAC6, false);
            hits.add(new Hit(kx, y + 5, kx + kw, y + 17, (b, vx) -> listening = m, false));
            // przełącznik
            int sx = x2 - 32;
            Draw.rounded(g, sx, y + 6, sx + 24, y + 16, 5, m.isEnabled() ? accent : 0xFF3A3F4A);
            int knob = m.isEnabled() ? sx + 15 : sx + 1;
            g.fill(knob, y + 7, knob + 8, y + 15, 0xFFFFFFFF);
            hits.add(new Hit(x1, y, kx - 2, y + rh, (b, vx) -> { if (b == 1) { if (!expanded.remove(m)) expanded.add(m); } else m.toggle(); }, false));
            hits.add(new Hit(sx, y, x2, y + rh, (b, vx) -> m.toggle(), false));
        }
        y += rh + 2;
        if (expanded.contains(m)) {
            for (Setting<?> s : m.settings()) if (s.isVisible()) y = settingRow(g, s, x1 + 10, x2 - 2, y, top, bottom, accent);
            int by = y;
            if (by >= top && by + 16 <= bottom) {
                Draw.rounded(g, x1 + 10, by, x1 + 80, by + 16, 3, 0xFF2A2D34);
                g.drawString(font, "Resetuj", x1 + 18, by + 4, 0xFFE5484D, false);
                hits.add(new Hit(x1 + 10, by, x1 + 80, by + 16, (b, vx) -> { m.resetSettings(); m.setKey(-1); }, false));
            }
            y += 20;
        }
        return y;
    }

    private int settingRow(GuiGraphics g, Setting<?> s, int x1, int x2, int y, int top, int bottom, int accent) {
        int h = (s instanceof NumberSetting) ? 22 : (s instanceof ColorSetting ? 22 : 18);
        if (y < top || y + h > bottom) return y + h + 1;
        boolean tip = over(x1, y, x2, y + h);
        g.drawString(font, s.name, x1 + 2, y + 4, tip ? 0xFFFFFFFF : 0xFFB4BAC6, false);
        int vx1 = x1 + 130, vx2 = x2 - 6;
        if (s instanceof BoolSetting b) {
            Draw.rounded(g, vx2 - 22, y + 4, vx2, y + 14, 5, b.get() ? accent : 0xFF3A3F4A);
            int kx = b.get() ? vx2 - 9 : vx2 - 21;
            g.fill(kx, y + 5, kx + 8, y + 13, 0xFFFFFFFF);
            hits.add(new Hit(x1, y, x2, y + h, (bt, vx) -> b.toggle(), false));
        } else if (s instanceof NumberSetting n) {
            int tx1 = vx1, tx2 = vx2 - 40;
            g.fill(tx1, y + 9, tx2, y + 12, 0xFF2A2D34);
            int fx = tx1 + (int) ((tx2 - tx1) * n.fraction());
            g.fill(tx1, y + 9, fx, y + 12, accent);
            g.fill(fx - 2, y + 6, fx + 2, y + 15, 0xFFFFFFFF);
            String v = n.step >= 1 ? String.valueOf(n.i()) : String.format("%.2f", n.get());
            g.drawString(font, v, vx2 - font.width(v), y + 7, 0xFFFFFFFF, false);
            hits.add(new Hit(tx1 - 4, y, tx2 + 4, y + h, (bt, vx) -> n.set(n.min + (n.max - n.min) * Math.max(0, Math.min(1, (vx - tx1) / (double) (tx2 - tx1)))), true));
        } else if (s instanceof ModeSetting m) {
            String v = "‹ " + m.get() + " ›";
            Draw.rounded(g, vx2 - font.width(v) - 10, y + 2, vx2, y + 16, 3, 0xFF2A2D34);
            g.drawString(font, v, vx2 - font.width(v) - 5, y + 5, 0xFFFFFFFF, false);
            hits.add(new Hit(x1, y, x2, y + h, (bt, vx) -> m.cycle(), false));
        } else if (s instanceof ColorSetting c) {
            int sx = vx1;
            for (int col : ColorSetting.PALETTE) {
                g.fill(sx, y + 5, sx + 11, y + 16, col);
                if ((c.get() & 0xFFFFFF) == (col & 0xFFFFFF)) Draw.outline(g, sx - 1, y + 4, sx + 12, y + 17, 1, 0xFFFFFFFF);
                final int fcol = col;
                hits.add(new Hit(sx, y + 4, sx + 12, y + 17, (bt, vx) -> c.set(fcol), false));
                sx += 15;
            }
        } else if (s instanceof StringSetting st) {
            g.drawString(font, font.plainSubstrByWidth(st.get(), vx2 - vx1) + " (edytuj w pliku profilu)", vx1, y + 5, 0xFF8B93A1, false);
        }
        return y + h + 1;
    }

    // ---------------------------------------------------------------- ustawienia globalne / profile
    private int settingsTab(GuiGraphics g, int x1, int x2, int y, int top, int bottom, int accent, float a) {
        UiSettings ui = Eclipse.ui();
        g.drawString(font, "Wygląd", x1 + 4, y + 2, accent, false); y += 14;
        for (Setting<?> s : ui.all()) y = settingRow(g, s, x1 + 4, x2 - 2, y, top, bottom, accent);
        y += 6;
        g.drawString(font, "Klawisze", x1 + 4, y, accent, false); y += 14;
        y = keyRow(g, "Otwórz GUI", "gui", ui.guiKey, x1, x2, y, top, bottom);
        y = keyRow(g, "Panic (wyłącz wszystko)", "panic", ui.panicKey, x1, x2, y, top, bottom);
        y += 6;
        g.drawString(font, "Profile", x1 + 4, y, accent, false); y += 14;
        ConfigManager cm = Eclipse.config();
        for (String p : cm.listProfiles()) {
            if (y >= top && y + 18 <= bottom) {
                boolean act = p.equals(cm.profile());
                Draw.rounded(g, x1 + 4, y, x1 + 160, y + 16, 3, act ? Draw.alpha(accent, 0.45f) : 0xFF23262E);
                g.drawString(font, p + (act ? "  (aktywny)" : ""), x1 + 10, y + 4, 0xFFFFFFFF, false);
                hits.add(new Hit(x1 + 4, y, x1 + 160, y + 16, (b, vx) -> cm.switchProfile(p), false));
            }
            y += 18;
        }
        if (y >= top && y + 18 <= bottom) {
            Draw.rounded(g, x1 + 4, y, x1 + 100, y + 16, 3, 0xFF23262E);
            g.drawString(font, "+ Nowy profil", x1 + 10, y + 4, 0xFF46A758, false);
            hits.add(new Hit(x1 + 4, y, x1 + 100, y + 16, (b, vx) -> {
                for (int i = 1; i < 100; i++) if (cm.createProfile("profil" + i)) { Eclipse.notifications().push("Utworzono profil" + i, accent); break; }
            }, false));
            Draw.rounded(g, x1 + 106, y, x1 + 200, y + 16, 3, 0xFF23262E);
            g.drawString(font, "Resetuj wygląd", x1 + 112, y + 4, 0xFFE5484D, false);
            hits.add(new Hit(x1 + 106, y, x1 + 200, y + 16, (b, vx) -> ui.reset(), false));
        }
        y += 22;
        g.drawString(font, "Eksport/import: /eclipse export <nazwa>, /eclipse import <nazwa>", x1 + 4, y, 0xFF6B7280, false);
        return y + 14;
    }

    private int keyRow(GuiGraphics g, String label, String id, int key, int x1, int x2, int y, int top, int bottom) {
        if (y >= top && y + 18 <= bottom) {
            g.drawString(font, label, x1 + 6, y + 4, 0xFFB4BAC6, false);
            String kn = listening == id ? "..." : keyName(key);
            int w = font.width(kn) + 10;
            Draw.rounded(g, x1 + 170, y, x1 + 170 + w, y + 16, 3, 0xFF2A2D34);
            g.drawString(font, kn, x1 + 175, y + 4, 0xFFFFFFFF, false);
            hits.add(new Hit(x1 + 170, y, x1 + 170 + w, y + 16, (b, vx) -> listening = id, false));
        }
        return y + 19;
    }

    // ---------------------------------------------------------------- wejście
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        listening = null;
        double vx = e.x() / scale, vy = e.y() / scale;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (vx >= h.x1 && vx < h.x2 && vy >= h.y1 && vy < h.y2) {
                if (h.draggable) dragging = h;
                h.click.run(e.button(), vx);
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != null) { dragging.click.run(0, e.x() / scale); return true; }
        return super.mouseDragged(e, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) { dragging = null; return super.mouseReleased(e); }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) sy * 22));
        return true;
    }

    @Override public boolean charTyped(CharacterEvent e) {
        if (listening != null) return true;
        String s = e.codepointAsString();
        if (!s.isEmpty() && s.codePointAt(0) >= 32 && search.length() < 24) { search.append(s); scroll = 0; return true; }
        return false;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        UiSettings ui = Eclipse.ui();
        if (listening != null) {
            Object l = listening;
            listening = null;
            if (key == GLFW.GLFW_KEY_ESCAPE) return true;
            boolean clear = key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
            if (l instanceof Module m) m.setKey(clear ? -1 : key);
            else if (!clear && "gui".equals(l)) { ui.guiKey = key; dev.eclipse.config.Dirty.mark(); }
            else if (!clear && "panic".equals(l)) { ui.panicKey = key; dev.eclipse.config.Dirty.mark(); }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (search.length() > 0) search.setLength(0); else onClose(); return true; }
        if (key == ui.guiKey && search.length() == 0) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && search.length() > 0) { search.setLength(search.length() - 1); return true; }
        return super.keyPressed(e);
    }

    static String keyName(int key) {
        if (key < 0) return "—";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char) key);
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char) key);
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT"; case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-CTRL"; case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-ALT"; case GLFW.GLFW_KEY_LEFT_ALT -> "L-ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE"; case GLFW.GLFW_KEY_END -> "END"; case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_INSERT -> "INS"; case GLFW.GLFW_KEY_PAGE_UP -> "PGUP"; case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP"; case GLFW.GLFW_KEY_DOWN -> "DOWN"; case GLFW.GLFW_KEY_LEFT -> "LEFT"; case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_KP_0 -> "NUM0"; case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            default -> "K" + key;
        };
    }
}
