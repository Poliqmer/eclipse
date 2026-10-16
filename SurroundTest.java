package dev.eclipse.module.combat;

import dev.eclipse.client.Eclipse;
import dev.eclipse.module.Category;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ModeSetting;
import dev.eclipse.setting.NumberSetting;
import dev.eclipse.util.Safety;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Trening: otaczanie nieruchomej atrapy (stojak na zbroję / mob) obsydianem i pajęczyną w LOKALNYM świecie.
 * Planowanie ({@link #plan}) jest oddzielone od wykonania ({@link #executeNext}). Cele nigdy nie są graczami,
 * wykonanie jest zablokowane poza lokalnym światem (Safety).
 */
public final class SurroundTest extends Module {
    public enum Status { READY, DONE, OCCUPIED, OUT_OF_REACH, NO_SUPPORT, BLOCKED, NO_ITEM }
    public record Planned(BlockPos pos, Block block, Status status, Direction support) { }

    private final ModeSetting targets = add(new ModeSetting("Cel", "Atrapa = stojak na zbroję", "Atrapa", "Atrapa", "Moby"));
    private final BoolSetting webFeet = add(new BoolSetting("Pajęczyna u stóp", "Pajęczyna w komórce celu", true));
    private final BoolSetting webHead = add(new BoolSetting("Pajęczyna nad głową", "Pajęczyna nad celem", false));
    private final BoolSetting execute = add(new BoolSetting("Wykonaj", "Wyłączone = tylko podgląd", false));
    private final NumberSetting delay = add(new NumberSetting("Opóźnienie (ticki)", "Przerwa między blokami", 3, 1, 20, 1));
    private final NumberSetting reach = add(new NumberSetting("Zasięg", "Maks. dystans do bloku", 4.5, 2, 5.5, 0.1));

    private Entity target;
    private List<Planned> plan = List.of();
    private int cooldown, previousSlot = -1;
    private String lastReason = "";

    public SurroundTest() { super("Surround/Cobweb (test)", "Trening otaczania atrapy obsydianem i pajęczyną", Category.COMBAT, true); }

    @Override protected void onEnable() {
        if (!Safety.automationAllowed() && execute.get()) {
            Eclipse.notifications().push("Surround (test): wykonanie niedostępne – " + Safety.deniedReason(), 0xFFF76B15);
            execute.set(false);
        }
    }

    // ---------- PLANOWANIE (czyste odczyty, nic nie zmienia) ----------
    public List<Planned> plan(Entity t) {
        List<Planned> out = new ArrayList<>();
        BlockPos feet = t.blockPosition();
        for (Direction d : Direction.Plane.HORIZONTAL) out.add(check(feet.relative(d), Blocks.OBSIDIAN, t));
        if (webFeet.get()) out.add(check(feet, Blocks.COBWEB, t));
        if (webHead.get()) out.add(check(feet.above(), Blocks.COBWEB, t));
        return out;
    }

    private Planned check(BlockPos pos, Block block, Entity t) {
        BlockState cur = mc.level.getBlockState(pos);
        if (cur.is(block)) return new Planned(pos, block, Status.DONE, null);
        if (!cur.canBeReplaced()) return new Planned(pos, block, Status.OCCUPIED, null);
        if (hotbarSlot(block) < 0) return new Planned(pos, block, Status.NO_ITEM, null);
        Vec3 c = Vec3.atCenterOf(pos);
        if (mc.player.getEyePosition().distanceTo(c) > reach.get()) return new Planned(pos, block, Status.OUT_OF_REACH, null);
        if (!mc.level.isUnobstructed(block.defaultBlockState(), pos, CollisionContext.empty())) return new Planned(pos, block, Status.BLOCKED, null);
        for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP}) {
            BlockPos n = pos.relative(d);
            if (!mc.level.getBlockState(n).canBeReplaced()) return new Planned(pos, block, Status.READY, d);
        }
        return new Planned(pos, block, Status.NO_SUPPORT, null);
    }

    public static boolean feasible(List<Planned> plan) {
        for (Planned p : plan) if (p.status != Status.READY && p.status != Status.DONE) return false;
        return true;
    }

    private int hotbarSlot(Block b) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).getItem() == b.asItem()) return i;
        return -1;
    }

    private Entity findTarget() {
        Entity best = null; double bd = 64;
        for (Entity e : mc.level.entitiesForRendering()) {
            boolean ok = targets.is("Atrapa") ? e instanceof ArmorStand : e instanceof Mob;
            if (!ok || e.isRemoved()) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    // ---------- WYKONANIE ----------
    @Override public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (target == null || target.isRemoved()) { target = findTarget(); }
        if (target == null) { plan = List.of(); return; }
        plan = plan(target);
        if (!execute.get()) return;
        if (!Safety.automationAllowed()) { fail(Safety.deniedReason()); return; }
        boolean pending = false;
        for (Planned p : plan) if (p.status != Status.DONE) pending = true;
        if (!pending) { restoreSlot(); return; }
        if (!feasible(plan)) {
            StringBuilder sb = new StringBuilder();
            for (Planned p : plan) if (p.status != Status.READY && p.status != Status.DONE) sb.append(p.pos.toShortString()).append('=').append(p.status).append(' ');
            fail("układ niemożliwy: " + sb.toString().trim());
            return;
        }
        if (cooldown-- > 0) return;
        cooldown = delay.i();
        for (Planned p : plan) if (p.status == Status.READY) { executeNext(p); break; }
    }

    private void executeNext(Planned p) {
        int slot = hotbarSlot(p.block);
        if (slot < 0) return;
        if (previousSlot < 0) previousSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        BlockPos support = p.pos.relative(p.support);
        Direction face = p.support.getOpposite();
        Vec3 hitLoc = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        var result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitLoc, face, support, false));
        if (result.consumesAction()) mc.player.swing(InteractionHand.MAIN_HAND);
        // serwer może odrzucić operację – kolejny tick ponownie zaplanuje układ ze stanu świata
    }

    private void fail(String reason) {
        if (!reason.equals(lastReason)) {
            Eclipse.LOGGER.info("[Surround test] anulowano: {}", reason);
            Eclipse.notifications().push("Surround: " + reason, 0xFFF76B15);
            lastReason = reason;
        }
        restoreSlot();
    }

    private void restoreSlot() {
        if (previousSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }

    @Override public void onRender(GuiGraphics g, float pt) {
        if (plan.isEmpty()) return;
        Projection.begin(g, pt);
        for (Planned p : plan) {
            int col = switch (p.status) { case READY -> 0xFF46A758; case DONE -> 0x66AAAAAA; default -> 0xFFE5484D; };
            Projection.wireBox(g, new AABB(p.pos), 1, col);
        }
    }

    @Override protected void onDisable() { restoreSlot(); target = null; plan = List.of(); lastReason = ""; }
    @Override public void onWorldLeave() { target = null; plan = List.of(); previousSlot = -1; }
}
