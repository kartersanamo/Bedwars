package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.upgrades.TeamUpgradeState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic implementation of a resource generator.
 * Tracks how many of its dropped items are still on the ground; does not spawn when at cap.
 * Iron and gold generators may have a forward target (team spawn) for block-placement protection.
 */
public final class OreGenerator implements IGenerator {

    private final IArena arena;
    private final EGeneratorType type;
    private final Location location;

    private final Location forwardTarget;

    private final int intervalTicks;
    private final int maxItems;

    private final GeneratorItemTracker itemTracker;

    /** UUIDs of item entities we have dropped that are still on the ground (not yet picked up or removed). */
    private final Set<UUID> droppedItemIds = ConcurrentHashMap.newKeySet();

    private long lastDropTick;

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems) {
        this(arena, type, location, intervalTicks, maxItems, null, null);
    }

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems,
                        final Location forwardTarget) {
        this(arena, type, location, intervalTicks, maxItems, forwardTarget, null);
    }

    public OreGenerator(final IArena arena,
                        final EGeneratorType type,
                        final Location location,
                        final int intervalTicks,
                        final int maxItems,
                        final Location forwardTarget,
                        final GeneratorItemTracker itemTracker) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.type = Objects.requireNonNull(type, "type");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.intervalTicks = intervalTicks;
        this.maxItems = maxItems;
        this.forwardTarget = forwardTarget != null ? forwardTarget.clone() : null;
        this.itemTracker = itemTracker;
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

    /**
     * Number of this generator's dropped items still on the ground (tracked globally, not just nearby).
     */
    public int getCurrentDroppedCount() {
        return droppedItemIds.size();
    }

    /**
     * Called by {@link GeneratorItemTracker} when this generator drops an item.
     */
    public void registerDroppedItem(final UUID itemEntityId) {
        droppedItemIds.add(itemEntityId);
    }

    /**
     * Called when an item we dropped is picked up, merged, or despawned.
     */
    public void onDroppedItemRemoved(final UUID itemEntityId) {
        droppedItemIds.remove(itemEntityId);
    }

    @Override
    public void tick(final long currentTick) {
        int effectiveInterval = getEffectiveIntervalTicks();
        if (effectiveInterval <= 0) {
            return;
        }

        // Apply per-team Forge upgrades to iron/gold generators (faster spawn rates).
        int forgeTierForThisTick = 0;
        if (type == EGeneratorType.IRON || type == EGeneratorType.GOLD) {
            forgeTierForThisTick = getForgeTier();
            if (forgeTierForThisTick > 0) {
                final double multiplier = switch (forgeTierForThisTick) {
                    case 1 -> 1.5D;  // Iron Forge: +50%
                    case 2 -> 2.0D;  // Golden Forge: +100%
                    case 3 -> 2.0D;  // Emerald Forge: same speed as Golden (emerald effect handled separately)
                    case 4 -> 3.0D;  // Molten Forge: +200% (3x)
                    default -> 1.0D;
                };
                effectiveInterval = (int) Math.max(1, Math.round(effectiveInterval / multiplier));
            }
        }

        if (currentTick - lastDropTick < effectiveInterval) {
            return;
        }

        final World world = location.getWorld();
        if (world == null) {
            return;
        }

        // Enforce per-generator item caps based on the TOTAL number of items in stacks
        // near this generator, not just the number of item entities. This prevents
        // stacked items from bypassing the cap (e.g. 64 iron in one stack).
        final int nearbyItems = world.getNearbyEntities(location, 1.5, 1.5, 1.5).stream()
                .filter(entity -> entity instanceof Item it && it.getItemStack().getType() == type.getDropMaterial())
                .mapToInt(entity -> ((Item) entity).getItemStack().getAmount())
                .sum();
        if (nearbyItems >= maxItems) {
            lastDropTick = currentTick;
            return;
        }

        final Item item = world.dropItem(location, new ItemStack(type.getDropMaterial(), 1));
        item.setVelocity(new org.bukkit.util.Vector(0.0, 0.0, 0.0));
        lastDropTick = currentTick;

        if (itemTracker != null) {
            itemTracker.track(this, item);
        }
    }

    /**
     * Returns the Forge tier (0-4) for this generator's owning team, if any.
     * Only applies to iron/gold generators that were created with a forwardTarget
     * (team spawn); mid generators are unaffected.
     */
    private int getForgeTier() {
        if (!(arena instanceof Arena concrete)) {
            return 0;
        }
        if (forwardTarget == null) {
            return 0;
        }
        ITeam owningTeam = null;
        for (ITeam team : concrete.getTeams()) {
            final Location spawn = team.getSpawnLocation();
            if (spawn.getWorld() != null
                    && spawn.getWorld().equals(forwardTarget.getWorld())
                    && spawn.distanceSquared(forwardTarget) < 1.0D) {
                owningTeam = team;
                break;
            }
        }
        if (owningTeam == null) {
            return 0;
        }
        final TeamUpgradeState state = concrete.getTeamUpgradeState(owningTeam);
        return state != null ? state.getForge() : 0;
    }
}
