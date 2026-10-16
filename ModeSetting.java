package dev.eclipse.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    public final List<String> options;
    public ModeSetting(String name, String desc, String def, String... options) {
        super(name, desc, def);
        this.options = List.of(options);
        if (!this.options.contains(def)) throw new IllegalArgumentException("Domyślna wartość spoza listy: " + def);
    }
    @Override protected String validate(String v) { return v != null && options.contains(v) ? v : null; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) set(e.getAsString());
    }
    public boolean is(String s) { return value.equals(s); }
    public void cycle() { set(options.get((options.indexOf(value) + 1) % options.size())); }
}
