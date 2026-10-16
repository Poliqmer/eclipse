package dev.eclipse;

import com.google.gson.JsonPrimitive;
import dev.eclipse.config.Dirty;
import dev.eclipse.setting.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingTest {
    @Test void numberIsClampedAndStepped() {
        NumberSetting n = new NumberSetting("x", "", 5, 0, 10, 0.5);
        n.set(99.0); assertEquals(10.0, n.get());
        n.set(-4.0); assertEquals(0.0, n.get());
        n.set(2.74); assertEquals(2.5, n.get());
        n.set(Double.NaN); assertEquals(2.5, n.get());
    }

    @Test void invalidJsonIsIgnored() {
        NumberSetting n = new NumberSetting("x", "", 5, 0, 10, 1);
        n.fromJson(new JsonPrimitive("abc")); assertEquals(5.0, n.get());
        n.fromJson(new JsonPrimitive(7)); assertEquals(7.0, n.get());
        BoolSetting b = new BoolSetting("b", "", false);
        b.fromJson(new JsonPrimitive(1)); assertFalse(b.get());
    }

    @Test void modeRejectsUnknownAndCycles() {
        ModeSetting m = new ModeSetting("m", "", "A", "A", "B", "C");
        m.set("Z"); assertEquals("A", m.get());
        m.cycle(); assertEquals("B", m.get());
        m.cycle(); m.cycle(); assertEquals("A", m.get());
        assertThrows(IllegalArgumentException.class, () -> new ModeSetting("m", "", "Q", "A"));
    }

    @Test void changeMarksDirtyAndResetRestores() {
        Dirty.consume();
        BoolSetting b = new BoolSetting("b", "", false);
        b.set(true); assertTrue(Dirty.consume());
        b.reset(); assertFalse(b.get());
    }
}
