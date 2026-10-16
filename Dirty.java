package dev.eclipse.config;

/** Minimalna flaga "zmiany do zapisu" – bez zależności od Minecrafta (testowalna). */
public final class Dirty {
    private static volatile boolean dirty;
    private Dirty() {}
    public static void mark() { dirty = true; }
    public static boolean consume() { boolean d = dirty; dirty = false; return d; }
}
