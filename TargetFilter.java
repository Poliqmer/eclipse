package dev.eclipse.module.render;

import dev.eclipse.module.Module;
import dev.eclipse.setting.BoolSetting;
import dev.eclipse.setting.ColorSetting;
import dev.eclipse.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** Wspólny filtr encji + kolory kategorii dla ESP/Tracers/FullBox. */
public final class TargetFilter {
    public enum Kind { PLAYER, HOSTILE, PASSIVE, ITEM, OTHER }

    public final BoolSetting players, hostile, passive, items, invisible;
    public final ColorSetting cPlayer, cHostile, cPassive, cItem;
    public final NumberSetting range;

    public TargetFilter(ModuleAdder m, boolean withItems, double defRange) {
        players = m.add(new BoolSetting("Gracze", "Pokaż graczy", true));
        hostile = m.add(new BoolSetting("Wrogie", "Pokaż wrogie moby", true));
        passive = m.add(new BoolSetting("Pasywne", "Pokaż zwierzęta i pozostałe", false));
        items = withItems ? m.add(new BoolSetting("Przedmioty", "Pokaż przedmioty na ziemi", false)) : new BoolSetting("Przedmioty", "", false);
        invisible = m.add(new BoolSetting("Niewidzialne", "Pokaż niewidzialne encje", true));
        cPlayer = m.add(new ColorSetting("Kolor graczy", "", 0xFF3E63DD));
        cHostile = m.add(new ColorSetting("Kolor wrogich", "", 0xFFE5484D));
        cPassive = m.add(new ColorSetting("Kolor pasywnych", "", 0xFF46A758));
        cItem = m.add(new ColorSetting("Kolor przedmiotów", "", 0xFFFFC53D));
        range = m.add(new NumberSetting("Zasięg", "Maksymalny dystans (bloki)", defRange, 8, 256, 1));
    }

    /** Pozwala TargetFilter dodać ustawienia do modułu (add() jest protected). */
    public interface ModuleAdder { <S extends dev.eclipse.setting.Setting<?>> S add(S s); }

    public static Kind kind(Entity e) {
        if (e instanceof Player) return Kind.PLAYER;
        if (e instanceof ItemEntity) return Kind.ITEM;
        if (e instanceof LivingEntity le) return le.getType().getCategory() == MobCategory.MONSTER ? Kind.HOSTILE : Kind.PASSIVE;
        return Kind.OTHER;
    }

    public boolean accepts(Entity e, Entity self) {
        if (e == self || e.isRemoved()) return false;
        if (!invisible.get() && e.isInvisible()) return false;
        double d = e.distanceToSqr(self);
        if (d > range.get() * range.get()) return false;
        return switch (kind(e)) {
            case PLAYER -> players.get();
            case HOSTILE -> hostile.get();
            case PASSIVE -> passive.get();
            case ITEM -> items.get();
            case OTHER -> false;
        };
    }

    public int color(Entity e) {
        return switch (kind(e)) {
            case PLAYER -> cPlayer.get();
            case HOSTILE -> cHostile.get();
            case PASSIVE -> cPassive.get();
            case ITEM -> cItem.get();
            case OTHER -> 0xFFFFFFFF;
        };
    }
}
