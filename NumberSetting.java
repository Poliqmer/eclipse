package dev.eclipse.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, String desc, double def, double min, double max, double step) {
        super(name, desc, clamp(def, min, max));
        this.min = min; this.max = max; this.step = step;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    @Override protected Double validate(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return null;
        double r = v;
        if (step > 0) r = Math.round((r - min) / step) * step + min;
        return clamp(r, min, max);
    }

    public float f() { return value.floatValue(); }
    public int i() { return (int) Math.round(value); }
    public double fraction() { return max == min ? 0 : (value - min) / (max - min); }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
    }
}
