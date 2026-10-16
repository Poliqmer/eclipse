package dev.eclipse.render;

import net.minecraft.client.gui.GuiGraphics;

/** Proste prymitywy 2D na GuiGraphics (linie, ramki, kolory). */
public final class Draw {
    private Draw() { }

    public static int alpha(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mulAlpha(int argb, float f) {
        int a = Math.max(0, Math.min(255, (int) (((argb >>> 24) & 0xFF) * f)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = (a >>> 24) & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = (b >>> 24) & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int c) {
        g.fill(x1, y1, x2, y1 + t, c);
        g.fill(x1, y2 - t, x2, y2, c);
        g.fill(x1, y1 + t, x1 + t, y2 - t, c);
        g.fill(x2 - t, y1 + t, x2, y2 - t, c);
    }

    /** Panel z "zaokrąglonymi" rogami (ścięte narożniki, bez dodatkowych tekstur). */
    public static void rounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int c) {
        if (r <= 0 || x2 - x1 < 2 * r + 1 || y2 - y1 < 2 * r + 1) { g.fill(x1, y1, x2, y2, c); return; }
        g.fill(x1 + r, y1, x2 - r, y2, c);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (double) (r - i - 0.5) * (r - i - 0.5)));
            g.fill(x1 + inset, y1 + i, x1 + r, y1 + i + 1, c);
            g.fill(x2 - r, y1 + i, x2 - inset, y1 + i + 1, c);
            g.fill(x1 + inset, y2 - i - 1, x1 + r, y2 - i, c);
            g.fill(x2 - r, y2 - i - 1, x2 - inset, y2 - i, c);
        }
    }

    // Liang–Barsky: obcięcie do ekranu, potem rysowanie krokami co ~1 px.
    public static void line(GuiGraphics g, double x1, double y1, double x2, double y2, float thick, int argb) {
        double w = g.guiWidth(), h = g.guiHeight();
        double dx = x2 - x1, dy = y2 - y1, t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x1, w - x1, y1, h - y1};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) { if (q[i] < 0) return; }
            else {
                double r = q[i] / p[i];
                if (p[i] < 0) { if (r > t1) return; if (r > t0) t0 = r; }
                else { if (r < t0) return; if (r < t1) t1 = r; }
            }
        }
        double ax = x1 + dx * t0, ay = y1 + dy * t0, bx = x1 + dx * t1, by = y1 + dy * t1;
        int n = (int) Math.min(Math.max(Math.abs(bx - ax), Math.abs(by - ay)), 4000);
        int t = Math.max(1, Math.round(thick));
        if (n == 0) { g.fill((int) ax, (int) ay, (int) ax + t, (int) ay + t, argb); return; }
        double sxs = (bx - ax) / n, sys = (by - ay) / n;
        int off = t / 2;
        for (int i = 0; i <= n; i++) {
            int px = (int) (ax + sxs * i) - off, py = (int) (ay + sys * i) - off;
            g.fill(px, py, px + t, py + t, argb);
        }
    }
}
