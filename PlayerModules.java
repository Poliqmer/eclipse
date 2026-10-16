package dev.eclipse.module.player;

import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.setting.BoolSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PlayerModules {
    private PlayerModules() { }

    /** Utrzymuje sprint podczas ruchu do przodu. Nie zmienia globalnych opcji. */
    public static final class ToggleSprint extends Module {
        public ToggleSprint() { super("Toggle Sprint", "Automatycznie utrzymuje sprint podczas biegu do przodu", Category.PLAYER); }
        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.screen != null) return;
            if (mc.options.keyUp.isDown() && !p.isShiftKeyDown() && !p.isUsingItem() && !p.horizontalCollision
                    && p.getFoodData().getFoodLevel() > 6 && !p.isSprinting()) p.setSprinting(true);
        }
    }

    /** Podczas kopania wybiera najszybsze narzędzie z paska; po puszczeniu przycisku wraca do poprzedniego slotu. */
    public static final class AutoTool extends Module {
        private final BoolSetting restore = add(new BoolSetting("Przywróć slot", "Wróć do poprzedniego slotu po kopaniu", true));
        private int previous = -1;
        public AutoTool() { super("Auto Tool", "Wybiera najlepsze narzędzie do kopanego bloku", Category.PLAYER); }

        @Override public void onTick() {
            var p = mc.player;
            if (p == null || mc.level == null || mc.screen != null) return;
            boolean mining = mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK;
            if (mining) {
                var state = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
                int best = p.getInventory().getSelectedSlot();
                float bestSpeed = p.getInventory().getItem(best).getDestroySpeed(state);
                for (int i = 0; i < 9; i++) {
                    ItemStack s = p.getInventory().getItem(i);
                    float sp = s.getDestroySpeed(state);
                    if (sp > bestSpeed + 0.01f) { bestSpeed = sp; best = i; }
                }
                if (best != p.getInventory().getSelectedSlot()) {
                    if (previous < 0) previous = p.getInventory().getSelectedSlot();
                    p.getInventory().setSelectedSlot(best);
                }
            } else restorePrevious();
        }

        private void restorePrevious() {
            if (previous >= 0 && mc.player != null && restore.get()) mc.player.getInventory().setSelectedSlot(previous);
            previous = -1;
        }
        @Override protected void onDisable() { restorePrevious(); }
        @Override public void onWorldLeave() { previous = -1; }
    }
}
