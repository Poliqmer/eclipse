package dev.eclipse.module.world;

import com.google.gson.*;
import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Waypointy (per wymiar), zapis w config/eclipse/waypoints.json. Komendy: /eclipse wp add|remove|list. */
public final class Waypoints extends Module {
    public record Point(String name, String dim, int x, int y, int z) { }
    private final List<Point> points = new ArrayList<>();
    private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFFE93D82));
    private final NumberSetting maxDistance = add(new NumberSetting("Maks. dystans", "0 = bez limitu", 0, 0, 5000, 50));
    private boolean loaded;

    public Waypoints() { super("Waypoints", "Znaczniki pozycji w świecie", Category.WORLD); }

    private Path file() { return Eclipse.config().root().resolve("waypoints.json"); }
    private String dim() { return mc.level == null ? "" : mc.level.dimension().toString(); }

    @Override protected void onEnable() { if (!loaded) load(); }

    public List<Point> points() { if (!loaded) load(); return points; }

    public boolean add(String name) {
        if (mc.player == null || name == null || name.isBlank() || name.length() > 32) return false;
        if (!loaded) load();
        points.removeIf(p -> p.name.equalsIgnoreCase(name) && p.dim.equals(dim()));
        points.add(new Point(name, dim(), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
        save();
        return true;
    }

    public boolean remove(String name) {
        boolean r = points.removeIf(p -> p.name.equalsIgnoreCase(name) && p.dim.equals(dim()));
        if (r) save();
        return r;
    }

    private void load() {
        loaded = true;
        points.clear();
        try {
            if (!Files.isRegularFile(file())) return;
            for (JsonElement e : JsonParser.parseString(Files.readString(file())).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                points.add(new Point(o.get("name").getAsString(), o.get("dim").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()));
            }
        } catch (Exception ex) { Eclipse.LOGGER.warn("Nie można wczytać waypointów: {}", ex.toString()); }
    }

    private void save() {
        JsonArray arr = new JsonArray();
        for (Point p : points) {
            JsonObject o = new JsonObject();
            o.addProperty("name", p.name); o.addProperty("dim", p.dim);
            o.addProperty("x", p.x); o.addProperty("y", p.y); o.addProperty("z", p.z);
            arr.add(o);
        }
        try { Files.createDirectories(file().getParent()); Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(arr)); }
        catch (IOException e) { Eclipse.LOGGER.error("Zapis waypointów nie powiódł się", e); }
    }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (mc.level == null || mc.player == null) return;
        Projection.begin(g, pt);
        String dim = dim();
        for (Point p : points()) {
            if (!p.dim.equals(dim)) continue;
            double d = Math.sqrt(mc.player.distanceToSqr(p.x + 0.5, p.y + 0.5, p.z + 0.5));
            if (maxDistance.get() > 0 && d > maxDistance.get()) continue;
            if (!Projection.project(p.x + 0.5, p.y + 1.5, p.z + 0.5)) continue;
            String s = p.name + " " + Math.round(d) + "m";
            int x = (int) Projection.sx, y = (int) Projection.sy;
            g.fill(x - 2, y - 2, x + 2, y + 2, color.get());
            g.drawString(mc.font, s, x - mc.font.width(s) / 2, y - 12, 0xFFFFFFFF, true);
        }
    }
}
