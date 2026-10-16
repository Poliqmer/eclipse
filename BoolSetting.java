package dev.eclipse.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String desc, boolean def) { super(name, desc, def); }
    @Override protected Boolean validate(Boolean v) { return v; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) set(e.getAsBoolean());
    }
    public void toggle() { set(!value); }
}
