package dev.eclipse.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting<String> {
    private final int maxLength;
    public StringSetting(String name, String desc, String def, int maxLength) { super(name, desc, def); this.maxLength = maxLength; }
    @Override protected String validate(String v) {
        if (v == null) return null;
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) set(e.getAsString());
    }
}
