package dev.eclipse.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;

/**
 * Moduły automatyzujące akcje (KillAura-test, Surround-test) działają WYŁĄCZNIE w lokalnym,
 * jednoosobowym świecie, który nie został udostępniony w LAN. Na serwerach zdalnych są zablokowane.
 */
public final class Safety {
    private Safety() { }

    public static boolean automationAllowed() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer s = mc.getSingleplayerServer();
        return mc.level != null && mc.player != null && s != null && !s.isPublished();
    }

    public static String deniedReason() {
        return "automatyzacja działa tylko w lokalnym świecie (bez LAN)";
    }
}
