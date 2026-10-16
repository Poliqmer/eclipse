package dev.eclipse.module;

import dev.eclipse.client.Eclipse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Boolean> keyState = new IdentityHashMap<>();

    public void register(Module m) { modules.add(m); }
    public List<Module> all() { return Collections.unmodifiableList(modules); }

    public List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public Optional<Module> byName(String n) {
        for (Module m : modules) if (m.name.equalsIgnoreCase(n)) return Optional.of(m);
        return Optional.empty();
    }

    public int activeCount() { int n = 0; for (Module m : modules) if (m.isEnabled()) n++; return n; }

    public void disableAll() { for (Module m : modules) if (m.isEnabled()) m.setEnabled(false); }

    public void tick(Minecraft mc) {
        pollKeys(mc);
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try { m.onTick(); } catch (Throwable t) { m.fail("onTick", t); }
        }
    }

    public void render(GuiGraphics g, float pt) {
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try { m.onRender(g, pt); } catch (Throwable t) { m.fail("onRender", t); }
        }
    }

    public void worldLeave() {
        for (Module m : modules) {
            try { m.onWorldLeave(); } catch (Throwable t) { Eclipse.LOGGER.error("[{}] błąd w onWorldLeave", m.name, t); }
        }
    }

    private void pollKeys(Minecraft mc) {
        if (mc.screen != null) { keyState.clear(); return; } // skróty działają tylko w grze
        long window = mc.getWindow().handle();
        for (Module m : modules) {
            if (m.key() < 0) continue;
            boolean down = GLFW.glfwGetKey(window, m.key()) == GLFW.GLFW_PRESS;
            boolean was = keyState.getOrDefault(m, false);
            if (down && !was) m.toggle();
            keyState.put(m, down);
        }
    }
}
