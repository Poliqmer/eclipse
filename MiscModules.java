package dev.eclipse.module.misc;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MiscModules {
    private MiscModules() { }

    /** Natychmiast wyłącza wszystkie moduły (klawisz paniki w ustawieniach). */
    public static final class Panic extends Module {
        public Panic() { super("Panic", "Wyłącza wszystkie moduły", Category.MISC); }
        @Override protected void onEnable() {
            Eclipse.modules().disableAll();
            if (mc.screen instanceof dev.eclipse.gui.ClickGuiScreen) mc.setScreen(null);
            Eclipse.notifications().push("Panic: wszystkie moduły wyłączone", 0xFFE5484D);
        }
    }

    /** Dźwięk i powiadomienie, gdy inny gracz pojawi się w zasięgu lub zdrowie jest niskie. */
    public static final class SoundNotifications extends Module {
        private final NumberSetting range = add(new NumberSetting("Zasięg", "Dystans wykrycia gracza", 48, 8, 128, 4));
        private final BoolSetting players = add(new BoolSetting("Gracz w pobliżu", "", true));
        private final BoolSetting lowHealth = add(new BoolSetting("Niskie zdrowie", "", true));
        private final NumberSetting hearts = add(new NumberSetting("Próg zdrowia (serca)", "", 4, 1, 10, 0.5));
        private final Set<UUID> seen = new HashSet<>();
        private boolean lowFlag;

        public SoundNotifications() { super("Sound Notifications", "Dźwięki dla zdarzeń", Category.MISC); }

        @Override public void onTick() {
            if (mc.player == null || mc.level == null) return;
            if (players.get()) {
                Set<UUID> now = new HashSet<>();
                for (Player p : mc.level.players()) {
                    if (p == mc.player || p.distanceTo(mc.player) > range.get()) continue;
                    now.add(p.getUUID());
                    if (seen.add(p.getUUID())) { ping(); Eclipse.notifications().push("Gracz w pobliżu: " + p.getName().getString(), 0xFFFFC53D); }
                }
                seen.retainAll(now);
            }
            if (lowHealth.get()) {
                boolean low = mc.player.getHealth() <= hearts.get() * 2;
                if (low && !lowFlag) { ping(); Eclipse.notifications().push("Niskie zdrowie!", 0xFFE5484D); }
                lowFlag = low;
            }
        }
        private void ping() { mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f)); }
        @Override protected void onDisable() { seen.clear(); lowFlag = false; }
        @Override public void onWorldLeave() { seen.clear(); lowFlag = false; }
    }

    /** Overlay diagnostyczny: liczba aktywnych modułów, profil, czas klatki. */
    public static final class DebugOverlay extends Module {
        public DebugOverlay() { super("Debug overlay", "Diagnostyka klienta", Category.MISC); }
        @Override public void onRender(GuiGraphics g, float pt) {
            String[] l = {
                    Eclipse.NAME + " | profil: " + Eclipse.config().profile(),
                    "moduły: " + Eclipse.modules().activeCount() + "/" + Eclipse.modules().all().size(),
                    "FPS: " + mc.getFps()
            };
            int y = g.guiHeight() - 40;
            for (String s : l) { g.drawString(mc.font, s, 4, y, 0xFFB0B0B0, true); y += 10; }
        }
    }
}
