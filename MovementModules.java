package dev.eclipse.module.movement;

import dev.eclipse.module.Category;
import dev.eclipse.module.hud.HudModule;
import dev.eclipse.module.Module;
import dev.eclipse.render.Projection;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class MovementModules {
    private MovementModules() { }

    /** Prędkość pozioma/pionowa + diagnostyka (na ziemi, w locie). */
    public static final class SpeedInfo extends HudModule {
        private double lx, lz, speed;
        public SpeedInfo() { super("Prędkość", "Prędkość pozioma (bloki/s) i diagnostyka ruchu", 1, 55); }
        @Override public void onTick() {
            if (mc.player == null) return;
            double dx = mc.player.getX() - lx, dz = mc.player.getZ() - lz;
            speed = Math.hypot(dx, dz) * 20.0;
            lx = mc.player.getX(); lz = mc.player.getZ();
        }
        @Override protected void lines(List<String> out) {
            out.add(String.format("Prędkość: %.2f b/s", speed));
            Vec3 v = mc.player.getDeltaMovement();
            out.add(String.format("Vy: %.3f  %s%s", v.y, mc.player.onGround() ? "ziemia" : "powietrze", mc.player.isSprinting() ? "  sprint" : ""));
        }
    }

    /** Linia kierunku ruchu (wektor prędkości) rzutowana na ekran. */
    public static final class MoveDirection extends Module {
        private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFF12A594));
        private final NumberSetting scale = add(new NumberSetting("Skala", "Mnożnik długości wektora", 8, 2, 30, 1));
        public MoveDirection() { super("Kierunek ruchu", "Wizualizacja wektora ruchu", Category.MOVEMENT); }
        @Override public void onRender(GuiGraphics g, float pt) {
            if (mc.player == null) return;
            Projection.begin(g, pt);
            Vec3 v = mc.player.getDeltaMovement();
            double x = Mth.lerp(pt, mc.player.xOld, mc.player.getX()), y = Mth.lerp(pt, mc.player.yOld, mc.player.getY()) + 0.1, z = Mth.lerp(pt, mc.player.zOld, mc.player.getZ());
            Projection.worldLine(g, x, y, z, x + v.x * scale.get(), y + v.y * scale.get(), z + v.z * scale.get(), 2, color.get());
        }
    }

    /** Tryb treningowy: lokalna symulacja lotu rzucanego przedmiotu (perła, śnieżka, jajko). To tylko podgląd. */
    public static final class Trajectory extends Module {
        private final ColorSetting color = add(new ColorSetting("Kolor", "", 0xFFFFC53D));
        public Trajectory() { super("Trajektoria", "Podgląd toru rzucanego przedmiotu (perła/śnieżka/jajko)", Category.MOVEMENT); }

        @Override public void onRender(GuiGraphics g, float pt) {
            var p = mc.player;
            if (p == null || mc.level == null) return;
            Item it = p.getMainHandItem().getItem();
            if (!(it == Items.ENDER_PEARL || it == Items.SNOWBALL || it == Items.EGG)) return;
            Projection.begin(g, pt);
            double yaw = Math.toRadians(p.getViewYRot(pt)), pitch = Math.toRadians(p.getViewXRot(pt));
            double speed = 1.5;
            double vx = -Math.sin(yaw) * Math.cos(pitch) * speed, vy = -Math.sin(pitch) * speed, vz = Math.cos(yaw) * Math.cos(pitch) * speed;
            double x = Projection.eyeX(), y = Projection.eyeY() - 0.1, z = Projection.eyeZ();
            for (int i = 0; i < 120; i++) {
                double nx = x + vx, ny = y + vy, nz = z + vz;
                Projection.worldLine(g, x, y, z, nx, ny, nz, 1, color.get());
                BlockPos bp = BlockPos.containing(nx, ny, nz);
                if (!mc.level.getBlockState(bp).getCollisionShape(mc.level, bp).isEmpty()) break;
                x = nx; y = ny; z = nz;
                vx *= 0.99; vy = vy * 0.99 - 0.03; vz *= 0.99;
            }
        }
    }
}
