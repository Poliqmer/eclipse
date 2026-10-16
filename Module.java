package dev.eclipse.module;

import dev.eclipse.client.Eclipse;
import dev.eclipse.config.Dirty;
import dev.eclipse.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bazowa klasa modułu. Cały stan modułu musi być sprzątany w {@link #onDisable()} / {@link #onWorldLeave()}. */
public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final boolean experimental;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1; // GLFW key, -1 = brak

    protected Module(String name, String description, Category category) { this(name, description, category, false); }

    protected Module(String name, String description, Category category, boolean experimental) {
        this.name = name; this.description = description; this.category = category; this.experimental = experimental;
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }
    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }

    public boolean isEnabled() { return enabled; }
    public int key() { return key; }
    public void setKey(int k) { this.key = k; Dirty.mark(); }
    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        if (on) {
            enabled = true;
            try { onEnable(); }
            catch (Throwable t) { fail("onEnable", t); return; }
        } else {
            enabled = false;
            try { onDisable(); }
            catch (Throwable t) { Eclipse.LOGGER.error("[{}] błąd w onDisable", name, t); }
        }
        Dirty.mark();
    }

    /** Wyłącza moduł po wyjątku, żeby błąd nie zalewał logów co tick/klatkę. */
    public void fail(String where, Throwable t) {
        Eclipse.LOGGER.error("[{}] wyjątek w {} – moduł wyłączony", name, where, t);
        Eclipse.notifications().push(name + ": błąd, moduł wyłączony (patrz log)", 0xFFE5484D);
        if (enabled) { enabled = false; try { onDisable(); } catch (Throwable ignored) { } Dirty.mark(); }
    }

    public void resetSettings() { for (Setting<?> s : settings) s.reset(); }

    // --- hooki ---
    protected void onEnable() { }
    protected void onDisable() { }
    public void onTick() { }
    public void onRender(GuiGraphics g, float partialTick) { }
    /** Wywoływane przy wyjściu ze świata/serwera – wyczyść referencje do encji i stan. */
    public void onWorldLeave() { }
}
