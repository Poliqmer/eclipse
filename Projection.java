package dev.eclipse.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Rzutowanie punktów świata na ekran GUI (bez dostępu do macierzy renderera – tylko pozycja oczu,
 * yaw/pitch i FOV z opcji). Zakłada widok z pierwszej osoby; w trzeciej osobie ramki mogą być przesunięte.
 * Statyczne bufory – brak alokacji w pętli renderowania.
 */
public final class Projection {
    public static final double NEAR = 0.05;
    private static double ex, ey, ez;
    private static double fx, fy, fz, rx, rz, ux, uy, uz;
    private static double tanV, tanH;
    private static int sw, sh;
    private static float pt;
    /** Wyniki ostatniego project(): ekranowe x, y; camZ = głębokość. */
    public static double sx, sy, camZ;
    private static double cx, cy, cz;

    private Projection() { }

    public static void begin(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        pt = partialTick;
        ex = Mth.lerp(partialTick, cam.xOld, cam.getX());
        ey = Mth.lerp(partialTick, cam.yOld, cam.getY()) + cam.getEyeHeight();
        ez = Mth.lerp(partialTick, cam.zOld, cam.getZ());
        double yaw = Math.toRadians(cam.getViewYRot(partialTick));
        double pitch = Math.toRadians(cam.getViewXRot(partialTick));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cyw = Math.cos(yaw), syw = Math.sin(yaw);
        fx = -syw * cp; fy = -sp; fz = cyw * cp;
        rx = -cyw; rz = -syw;
        // up = right x forward (ry = 0)
        ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
        sw = g.guiWidth(); sh = g.guiHeight();
        tanV = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        tanH = tanV * sw / (double) sh;
    }

    public static float partialTick() { return pt; }
    public static double eyeX() { return ex; }
    public static double eyeY() { return ey; }
    public static double eyeZ() { return ez; }

    private static void toCam(double x, double y, double z) {
        double dx = x - ex, dy = y - ey, dz = z - ez;
        cx = dx * rx + dz * rz;
        cy = dx * ux + dy * uy + dz * uz;
        cz = dx * fx + dy * fy + dz * fz;
    }

    /** @return true, jeśli punkt jest przed kamerą (wynik w sx/sy). */
    public static boolean project(double x, double y, double z) {
        toCam(x, y, z);
        camZ = cz;
        if (cz < NEAR) return false;
        sx = sw / 2.0 + (cx / cz) / tanH * sw / 2.0;
        sy = sh / 2.0 - (cy / cz) / tanV * sh / 2.0;
        return true;
    }

    /** Punkt za kamerą: rzut "odbity" na krawędź ekranu (do tracerów). Wynik w sx/sy. */
    public static void projectClamped(double x, double y, double z) {
        toCam(x, y, z);
        camZ = cz;
        if (cz >= NEAR) { sx = sw / 2.0 + (cx / cz) / tanH * sw / 2.0; sy = sh / 2.0 - (cy / cz) / tanV * sh / 2.0; return; }
        double dx = -cx, dy = -cy;
        double len = Math.max(1e-6, Math.hypot(dx, dy));
        sx = sw / 2.0 + dx / len * sw * 2; sy = sh / 2.0 - dy / len * sh * 2;
    }

    /** Linia świata → ekran z obcięciem do płaszczyzny near. */
    public static void worldLine(GuiGraphics g, double ax, double ay, double az, double bx, double by, double bz, float thick, int argb) {
        toCam(ax, ay, az); double ax_ = cx, ay_ = cy, az_ = cz;
        toCam(bx, by, bz); double bx_ = cx, by_ = cy, bz_ = cz;
        if (az_ < NEAR && bz_ < NEAR) return;
        if (az_ < NEAR) { double t = (NEAR - az_) / (bz_ - az_); ax_ += (bx_ - ax_) * t; ay_ += (by_ - ay_) * t; az_ = NEAR; }
        else if (bz_ < NEAR) { double t = (NEAR - bz_) / (az_ - bz_); bx_ += (ax_ - bx_) * t; by_ += (ay_ - by_) * t; bz_ = NEAR; }
        double x1 = sw / 2.0 + (ax_ / az_) / tanH * sw / 2.0, y1 = sh / 2.0 - (ay_ / az_) / tanV * sh / 2.0;
        double x2 = sw / 2.0 + (bx_ / bz_) / tanH * sw / 2.0, y2 = sh / 2.0 - (by_ / bz_) / tanV * sh / 2.0;
        Draw.line(g, x1, y1, x2, y2, thick, argb);
    }

    /** Krawędzie AABB (12 linii). */
    public static void wireBox(GuiGraphics g, AABB b, float thick, int argb) {
        double x0 = b.minX, y0 = b.minY, z0 = b.minZ, x1 = b.maxX, y1 = b.maxY, z1 = b.maxZ;
        worldLine(g, x0, y0, z0, x1, y0, z0, thick, argb); worldLine(g, x1, y0, z0, x1, y0, z1, thick, argb);
        worldLine(g, x1, y0, z1, x0, y0, z1, thick, argb); worldLine(g, x0, y0, z1, x0, y0, z0, thick, argb);
        worldLine(g, x0, y1, z0, x1, y1, z0, thick, argb); worldLine(g, x1, y1, z0, x1, y1, z1, thick, argb);
        worldLine(g, x1, y1, z1, x0, y1, z1, thick, argb); worldLine(g, x0, y1, z1, x0, y1, z0, thick, argb);
        worldLine(g, x0, y0, z0, x0, y1, z0, thick, argb); worldLine(g, x1, y0, z0, x1, y1, z0, thick, argb);
        worldLine(g, x1, y0, z1, x1, y1, z1, thick, argb); worldLine(g, x0, y0, z1, x0, y1, z1, thick, argb);
    }

    /**
     * Ekranowy prostokąt otaczający AABB. @return false, jeśli którykolwiek róg jest za kamerą.
     * Wynik w {@link #rect} = {minX, minY, maxX, maxY}.
     */
    public static final double[] rect = new double[4];
    public static boolean screenRect(AABB b) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? b.minX : b.maxX, y = (i & 2) == 0 ? b.minY : b.maxY, z = (i & 4) == 0 ? b.minZ : b.maxZ;
            if (!project(x, y, z)) return false;
            minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
        }
        rect[0] = minX; rect[1] = minY; rect[2] = maxX; rect[3] = maxY;
        return true;
    }
}
