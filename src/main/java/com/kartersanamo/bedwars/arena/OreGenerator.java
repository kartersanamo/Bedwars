package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Basic implementation of a resource generator.
 */
public final class OreGenerator implements IGenerator {

    private final IArena arena;
    private final EGeneratorType type;
    private final Location location;

    private final int intervalTicks;
    private final int maxItems;

    private long lastDropTick;

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.type = Objects.requireNonNull(type, "type");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.intervalTicks = intervalTicks;
        this.maxItems = maxItems;
    }

    @Override
    public IArena getArena() {
        return arena;
    }

    @Override
    public EGeneratorType getType() {
        return type;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public void tick(final long currentTick) {
        if (intervalTicks <= 0) {
            return;
        }

        if (currentTick - lastDropTick < intervalTicks) {
            return;
        }

        final World world = location.getWorld();
        if (world == null) {
            return;
        }

        final long nearbyCount = world.getNearbyEntities(location, 1.5, 1.5, 1.5).stream()
                .filter(entity -> entity instanceof Item item && item.getItemStack().getType() == type.getDropMaterial())
                .count();

        if (nearbyCount >= maxItems) {
            return;
        }

        world.dropItemNaturally(location, new ItemStack(type.getDropMaterial(), 1));
        lastDropTick = currentTick;
    }
}
