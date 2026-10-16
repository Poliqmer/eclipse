package dev.eclipse.module;

public enum Category {
    COMBAT("Combat"), RENDER("Render"), PLAYER("Player"), MOVEMENT("Movement"), WORLD("World"), HUD("HUD"), MISC("Misc");
    public final String label;
    Category(String label) { this.label = label; }
}
