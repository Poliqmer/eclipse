package dev.eclipse.config;

import com.google.gson.*;
import dev.eclipse.client.Eclipse;
import dev.eclipse.client.UiSettings;
import dev.eclipse.module.Module;
import dev.eclipse.module.ModuleManager;
import dev.eclipse.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Profile konfiguracji: config/eclipse/profiles/&lt;nazwa&gt;.json (+ .bak z poprzednią wersją).
 * Zapis atomowy (plik tymczasowy + move). Brak haseł/tokenów – tylko ustawienia modułów.
 */
public final class ConfigManager {
    public static final int CONFIG_VERSION = 1;
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final ModuleManager modules;
    private final UiSettings ui;
    private final Path root = FabricLoader.getInstance().getConfigDir().resolve("eclipse");
    private final Path profilesDir = root.resolve("profiles");
    private String profile = "default";
    private int ticksSinceSave;

    public ConfigManager(ModuleManager modules, UiSettings ui) { this.modules = modules; this.ui = ui; }

    public String profile() { return profile; }
    public Path root() { return root; }

    public static boolean validName(String n) { return n != null && NAME.matcher(n).matches(); }

    private Path file(String name) { return profilesDir.resolve(name + ".json"); }

    public void init() {
        try { Files.createDirectories(profilesDir); } catch (IOException e) { Eclipse.LOGGER.error("Nie można utworzyć {}", profilesDir, e); return; }
        Path meta = root.resolve("meta.json");
        if (Files.exists(meta)) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(meta)).getAsJsonObject();
                String p = o.has("profile") ? o.get("profile").getAsString() : "default";
                if (validName(p)) profile = p;
            } catch (Exception e) { Eclipse.LOGGER.warn("Uszkodzony meta.json – używam profilu 'default'", e); }
        }
        load(profile);
    }

    /** Wywoływane co tick: autozapis, gdy coś się zmieniło (max co ~2 s). */
    public void tick() {
        if (++ticksSinceSave < 40) return;
        ticksSinceSave = 0;
        if (Dirty.consume()) save();
    }

    public List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(profilesDir)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).filter(ConfigManager::validName).sorted().forEach(out::add);
        } catch (IOException ignored) { }
        if (!out.contains(profile)) out.add(profile);
        return out;
    }

    public boolean save() { return write(profile, file(profile)); }

    private boolean write(String name, Path target) {
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) Files.copy(target, target.resolveSibling(target.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(serialize()), StandardCharsets.UTF_8);
            try { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
            Files.writeString(root.resolve("meta.json"), "{\"profile\":\"" + profile + "\"}");
            return true;
        } catch (IOException e) {
            Eclipse.LOGGER.error("Nie udało się zapisać konfiguracji '{}'", name, e);
            Eclipse.notifications().push("Błąd zapisu konfiguracji", 0xFFE5484D);
            return false;
        }
    }

    private JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("configVersion", CONFIG_VERSION);
        JsonObject u = new JsonObject();
        for (Setting<?> s : ui.all()) u.add(s.name, s.toJson());
        u.addProperty("guiKey", ui.guiKey);
        u.addProperty("panicKey", ui.panicKey);
        root.add("ui", u);
        JsonObject mods = new JsonObject();
        for (Module m : modules.all()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("key", m.key());
            JsonObject so = new JsonObject();
            for (Setting<?> s : m.settings()) so.add(s.name, s.toJson());
            mo.add("settings", so);
            mods.add(m.name, mo);
        }
        root.add("modules", mods);
        return root;
    }

    /** Wczytuje profil. Przy błędzie próbuje kopii .bak; jeśli nie ma nic – zostają wartości domyślne. */
    public boolean load(String name) {
        if (!validName(name)) return false;
        Path f = file(name);
        JsonObject o = readJson(f);
        if (o == null) o = readJson(f.resolveSibling(f.getFileName() + ".bak"));
        modules.disableAll();
        for (Module m : modules.all()) { m.resetSettings(); m.setKey(-1); }
        ui.reset();
        profile = name;
        if (o == null) { Dirty.mark(); return Files.notExists(f); }
        apply(o);
        Dirty.consume();
        return true;
    }

    private JsonObject readJson(Path p) {
        if (!Files.isRegularFile(p)) return null;
        try {
            JsonElement e = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception ex) {
            Eclipse.LOGGER.warn("Nie można odczytać {} – {}", p, ex.toString());
            return null;
        }
    }

    private void apply(JsonObject o) {
        int ver = o.has("configVersion") && o.get("configVersion").isJsonPrimitive() ? o.get("configVersion").getAsInt() : 0;
        if (ver > CONFIG_VERSION) Eclipse.LOGGER.warn("Konfiguracja z nowszej wersji ({}); nieznane pola zostaną pominięte", ver);
        // migracje: if (ver < 2) { ... }
        if (o.has("ui") && o.get("ui").isJsonObject()) {
            JsonObject u = o.getAsJsonObject("ui");
            for (Setting<?> s : ui.all()) if (u.has(s.name)) s.fromJson(u.get(s.name));
            if (u.has("guiKey") && u.get("guiKey").isJsonPrimitive()) ui.guiKey = u.get("guiKey").getAsInt();
            if (u.has("panicKey") && u.get("panicKey").isJsonPrimitive()) ui.panicKey = u.get("panicKey").getAsInt();
        }
        if (!o.has("modules") || !o.get("modules").isJsonObject()) return;
        JsonObject mods = o.getAsJsonObject("modules");
        for (Module m : modules.all()) {
            if (!mods.has(m.name) || !mods.get(m.name).isJsonObject()) continue;
            JsonObject mo = mods.getAsJsonObject(m.name);
            if (mo.has("settings") && mo.get("settings").isJsonObject()) {
                JsonObject so = mo.getAsJsonObject("settings");
                for (Setting<?> s : m.settings()) if (so.has(s.name)) s.fromJson(so.get(s.name));
            }
            if (mo.has("key") && mo.get("key").isJsonPrimitive()) m.setKey(mo.get("key").getAsInt());
        }
        // włączamy dopiero po wczytaniu ustawień
        for (Module m : modules.all()) {
            JsonElement mo = mods.get(m.name);
            if (mo != null && mo.isJsonObject() && mo.getAsJsonObject().has("enabled")
                    && mo.getAsJsonObject().get("enabled").getAsBoolean() && !m.experimental) m.setEnabled(true);
        }
    }

    public boolean switchProfile(String name) {
        if (!validName(name)) return false;
        save();
        boolean ok = load(name);
        Dirty.mark();
        return ok;
    }

    public boolean createProfile(String name) {
        if (!validName(name) || Files.exists(file(name))) return false;
        profile = name;           // nowy profil startuje od aktualnych ustawień
        return write(name, file(name));
    }

    public boolean export(Path target) { return write(profile, target); }

    public boolean importFrom(Path source, String asName) {
        if (!validName(asName)) return false;
        JsonObject o = readJson(source);
        if (o == null || !o.has("modules")) return false;
        try { Files.createDirectories(profilesDir); Files.copy(source, file(asName), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException e) { Eclipse.LOGGER.error("Import nie powiódł się", e); return false; }
        return load(asName);
    }
}
