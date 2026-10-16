package dev.eclipse.setting;

import com.google.gson.JsonElement;
import dev.eclipse.config.Dirty;

import java.util.function.BooleanSupplier;

/** Typowane ustawienie modułu z walidacją i serializacją JSON. */
public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;
    public final T defaultValue;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, String description, T def) {
        this.name = name;
        this.description = description;
        this.defaultValue = def;
        this.value = def;
    }

    public T get() { return value; }

    public void set(T v) {
        T valid = validate(v);
        if (valid == null || valid.equals(value)) return;
        value = valid;
        Dirty.mark();
    }

    public void reset() { set(defaultValue); }

    /** Zwraca poprawną wartość (np. przycinoną do zakresu) albo null, gdy wejście jest nie do uratowania. */
    protected abstract T validate(T v);

    public abstract JsonElement toJson();

    /** Wczytuje wartość z JSON; błędne dane są ignorowane (zostaje wartość domyślna/aktualna). */
    public abstract void fromJson(JsonElement e);

    public void showIf(BooleanSupplier s) { this.visible = s; }
    public boolean isVisible() { return visible.getAsBoolean(); }
}
