package com.kartersanamo.bedwars.maprestore;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory arena restore adapter.
 *
 * This implementation:
 * - takes an initial snapshot of all blocks in the configured arena region
 * - tracks which blocks were modified during gameplay
 * - restores only modified blocks when resetting the arena
 */
public final class InternalAdapter {

    private final Map<String, Map<BlockPosition, BlockData>> arenaSnapshots = new HashMap<>();
    private final Map<String, Set<BlockPosition>> modifiedBlocks = new HashMap<>();

    public void snapshotArena(final IArena arena, final ArenaConfig.Region region) {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(region, "region");

        final World world = arena.getWorld();
        if (world == null) {
            return;
        }

        final Map<BlockPosition, BlockData> snapshot = new HashMap<>();
        final Location min = min(region.getPos1(), region.getPos2());
        final Location max = max(region.getPos1(), region.getPos2());

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final BlockData data = block.getBlockData().clone();
                    snapshot.put(new BlockPosition(x, y, z), data);
                }
            }
        }

        arenaSnapshots.put(arena.getId(), snapshot);
        modifiedBlocks.put(arena.getId(), new HashSet<>());
    }

    /**
     * Marks the given block as modified so that it will be restored on reset.
     */
    public void markModified(final IArena arena, final Block block) {
        final Set<BlockPosition> modified = modifiedBlocks.computeIfAbsent(arena.getId(), key -> new HashSet<>());
        modified.add(BlockPosition.fromBlock(block));
    }

    /**
     * Restores all modified blocks in the arena region back to their snapshot state.
     */
    public void restoreArena(final IArena arena) {
        final Map<BlockPosition, BlockData> snapshot = arenaSnapshots.get(arena.getId());
        final Set<BlockPosition> modified = modifiedBlocks.get(arena.getId());

        if (snapshot == null || modified == null || snapshot.isEmpty() || modified.isEmpty()) {
            return;
        }

        final World world = arena.getWorld();
        if (world == null) {
            return;
        }

        for (BlockPosition position : modified) {
            final BlockData data = snapshot.get(position);
            if (data == null) {
                continue;
            }
            final Block block = world.getBlockAt(position.x(), position.y(), position.z());
            block.setBlockData(data, false);
        }

        modified.clear();
    }

    private static Location min(final Location a, final Location b) {
        return new Location(
                a.getWorld(),
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
    }

    private static Location max(final Location a, final Location b) {
        return new Location(
                a.getWorld(),
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );
    }

    /**
     * Simple immutable block position key.
     */
    public record BlockPosition(int x, int y, int z) {
        public static BlockPosition fromBlock(final Block block) {
            return new BlockPosition(block.getX(), block.getY(), block.getZ());
        }
    }
}
