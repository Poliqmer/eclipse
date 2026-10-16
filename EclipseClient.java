package dev.eclipse.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.eclipse.gui.ClickGuiScreen;
import dev.eclipse.module.combat.CombatHud;
import dev.eclipse.module.combat.KillAuraTest;
import dev.eclipse.module.combat.SurroundTest;
import dev.eclipse.module.hud.Huds;
import dev.eclipse.module.misc.MiscModules;
import dev.eclipse.module.movement.MovementModules;
import dev.eclipse.module.player.PlayerModules;
import dev.eclipse.module.render.*;
import dev.eclipse.module.world.ChunkBorders;
import dev.eclipse.module.world.Waypoints;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public final class EclipseClient implements ClientModInitializer {
    private static boolean guiKeyDown, panicKeyDown;
    private static boolean openGuiRequested;

    @Override public void onInitializeClient() {
        var m = Eclipse.modules();
        // Render
        m.register(new Fullbright()); m.register(new Tracers()); m.register(new Esp()); m.register(new FullBoxEsp());
        m.register(new ItemEsp()); m.register(new Zoom());
        // World
        m.register(new ChunkBorders()); m.register(new Waypoints());
        // HUD
        m.register(new Huds.FpsPing()); m.register(new Huds.Coordinates()); m.register(new Huds.Direction()); m.register(new Huds.Biome());
        m.register(new Huds.Clock()); m.register(new Huds.ActiveModules()); m.register(new Huds.Cps()); m.register(new Huds.Keystrokes());
        m.register(new Huds.ArmorHud()); m.register(new Huds.PotionHud()); m.register(new Huds.CustomCrosshair());
        // Player
        m.register(new PlayerModules.ToggleSprint()); m.register(new PlayerModules.AutoTool()); m.register(new Huds.InventoryHud());
        // Movement
        m.register(new MovementModules.SpeedInfo()); m.register(new MovementModules.MoveDirection()); m.register(new MovementModules.Trajectory());
        // Combat
        m.register(new CombatHud()); m.register(new KillAuraTest()); m.register(new SurroundTest());
        // Misc
        m.register(new MiscModules.Panic()); m.register(new MiscModules.SoundNotifications()); m.register(new MiscModules.DebugOverlay());

        Eclipse.config().init();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { Eclipse.modules().worldLeave(); Eclipse.notifications().clear(); });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Eclipse.config().save());
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath("eclipse_client", "hud"),
                (graphics, deltaTracker) -> {
                    Eclipse.modules().render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
                    Eclipse.notifications().render(graphics);
                });
        ClientCommandRegistrationCallback.EVENT.register((d, access) -> d.register(ClientCommandManager.literal("eclipse")
                .executes(c -> { openGuiRequested = true; return 1; })
                .then(ClientCommandManager.literal("profile")
                        .then(ClientCommandManager.literal("list").executes(c -> { msg("Profile: " + String.join(", ", Eclipse.config().listProfiles()) + " (aktywny: " + Eclipse.config().profile() + ")"); return 1; }))
                        .then(ClientCommandManager.literal("load").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
                            String n = StringArgumentType.getString(c, "name");
                            msg(Eclipse.config().switchProfile(n) ? "Wczytano profil " + n : "Nie można wczytać profilu " + n); return 1; })))
                        .then(ClientCommandManager.literal("new").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
                            String n = StringArgumentType.getString(c, "name");
                            msg(Eclipse.config().createProfile(n) ? "Utworzono profil " + n : "Niepoprawna nazwa lub profil istnieje"); return 1; }))))
                .then(ClientCommandManager.literal("export").then(ClientCommandManager.argument("file", StringArgumentType.word()).executes(c -> {
                    Path p = Eclipse.config().root().resolve("export").resolve(StringArgumentType.getString(c, "file") + ".json");
                    msg(Eclipse.config().export(p) ? "Wyeksportowano do " + p : "Eksport nie powiódł się"); return 1; })))
                .then(ClientCommandManager.literal("import").then(ClientCommandManager.argument("file", StringArgumentType.word()).executes(c -> {
                    String f = StringArgumentType.getString(c, "file");
                    Path p = Eclipse.config().root().resolve("export").resolve(f + ".json");
                    msg(Eclipse.config().importFrom(p, f) ? "Zaimportowano profil " + f : "Import nie powiódł się (plik w config/eclipse/export/)"); return 1; })))
                .then(ClientCommandManager.literal("wp")
                        .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
                            msg(wp().add(StringArgumentType.getString(c, "name")) ? "Dodano waypoint" : "Nie można dodać waypointa"); return 1; })))
                        .then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
                            msg(wp().remove(StringArgumentType.getString(c, "name")) ? "Usunięto" : "Nie znaleziono"); return 1; })))
                        .then(ClientCommandManager.literal("list").executes(c -> {
                            wp().points().forEach(p -> msg(p.name() + " " + p.x() + " " + p.y() + " " + p.z())); return 1; })))
                .then(ClientCommandManager.literal("panic").executes(c -> { Eclipse.modules().disableAll(); msg("Wszystkie moduły wyłączone"); return 1; }))));
        Eclipse.LOGGER.info("{} załadowany ({} modułów). Menu: Right Shift lub /eclipse", Eclipse.NAME, m.all().size());
    }

    private static Waypoints wp() { return (Waypoints) Eclipse.modules().byName("Waypoints").orElseThrow(); }
    private static void msg(String s) { Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Eclipse] " + s)); }

    private void tick(Minecraft mc) {
        Eclipse.config().tick();
        if (mc.getWindow() == null) return;
        long w = mc.getWindow().handle();
        boolean g = GLFW.glfwGetKey(w, Eclipse.ui().guiKey) == GLFW.GLFW_PRESS;
        boolean p = GLFW.glfwGetKey(w, Eclipse.ui().panicKey) == GLFW.GLFW_PRESS;
        if (mc.screen == null && mc.level != null) {
            if ((g && !guiKeyDown) || openGuiRequested) mc.setScreen(new ClickGuiScreen());
            if (p && !panicKeyDown) { Eclipse.modules().disableAll(); Eclipse.notifications().push("Panic: moduły wyłączone", 0xFFE5484D); }
        }
        openGuiRequested = false;
        guiKeyDown = g; panicKeyDown = p;
        if (mc.level != null && mc.player != null) Eclipse.modules().tick(mc);
    }
}
