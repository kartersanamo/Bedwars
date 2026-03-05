package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * Basic implementation of a resource generator.
 * Iron and gold generators may have a forward target (team spawn) for block-placement protection.
 */
public final class OreGenerator implements IGenerator {

    private final IArena arena;
    private final EGeneratorType type;
    private final Location location;

    private final Location forwardTarget;

    private final int intervalTicks;
    private final int maxItems;

    private long lastDropTick;

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems) {
        this(arena, type, location, intervalTicks, maxItems, null);
    }

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems,
                        final Location forwardTarget) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.type = Objects.requireNonNull(type, "type");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.intervalTicks = intervalTicks;
        this.maxItems = maxItems;
        this.forwardTarget = forwardTarget != null ? forwardTarget.clone() : null;
    }

    /**
     * For iron/gold generators, the location (e.g. team spawn) that defines "forward" for protection.
     */
    public Optional<Location> getForwardTarget() {
        return forwardTarget == null ? Optional.empty() : Optional.of(forwardTarget.clone());
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

    public int getIntervalTicks() {
        return intervalTicks;
    }

    private int getEffectiveIntervalTicks() {
        if (type == EGeneratorType.DIAMOND || type == EGeneratorType.EMERALD) {
            return type == EGeneratorType.DIAMOND
                    ? arena.getEffectiveDiamondIntervalTicks()
                    : arena.getEffectiveEmeraldIntervalTicks();
        }
        return intervalTicks;
    }

    public long getLastDropTick() {
        return lastDropTick;
    }

    @Override
    public void tick(final long currentTick) {
        final int effectiveInterval = getEffectiveIntervalTicks();
        if (effectiveInterval <= 0) {
            return;
        }

        if (currentTick - lastDropTick < effectiveInterval) {
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
            lastDropTick = currentTick;
            return;
        }

        final Item item = world.dropItem(location, new ItemStack(type.getDropMaterial(), 1));
        item.setVelocity(new org.bukkit.util.Vector(0.0, 0.0, 0.0));
        lastDropTick = currentTick;
    }
}
