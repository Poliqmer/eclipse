package dev.eclipse.client;

import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import dev.eclipse.setting.Setting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Globalne ustawienia interfejsu (część profilu). */
public final class UiSettings {
    public final NumberSetting scale = new NumberSetting("Skala GUI", "Skala okna Click GUI", 1.0, 0.6, 1.6, 0.05);
    public final NumberSetting opacity = new NumberSetting("Przezroczystość", "Przezroczystość paneli", 0.92, 0.4, 1.0, 0.02);
    public final ColorSetting accent = new ColorSetting("Kolor akcentu", "Paleta kolorów GUI i HUD", 0xFF3E63DD);
    public int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public int panicKey = GLFW.GLFW_KEY_END;

    public List<Setting<?>> all() { return List.of(scale, opacity, accent); }
    public void reset() {
        scale.reset(); opacity.reset(); accent.reset();
        guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT; panicKey = GLFW.GLFW_KEY_END;
    }
}
