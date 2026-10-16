package dev.eclipse.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Kolor ARGB zapisywany jako liczba całkowita. */
public final class ColorSetting extends Setting<Integer> {
    public static final int[] PALETTE = {
            0xFFE5484D, 0xFFF76B15, 0xFFFFC53D, 0xFF46A758, 0xFF12A594, 0xFF3E63DD, 0xFF8E4EC6, 0xFFE93D82, 0xFFFFFFFF
    };
    public ColorSetting(String name, String desc, int argb) { super(name, desc, argb); }
    @Override protected Integer validate(Integer v) { return v; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsInt());
    }
    public int rgb() { return value & 0xFFFFFF; }
    public int withAlpha(int a) { return (Math.max(0, Math.min(255, a)) << 24) | rgb(); }
}
