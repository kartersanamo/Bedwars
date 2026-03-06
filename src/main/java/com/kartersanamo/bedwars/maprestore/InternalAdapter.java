package com.kartersanamo.bedwars.maprestore;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.*;

/**
 * In-memory arena restore adapter.
 * <p>
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
     * Returns {@code true} if the snapshot recorded this position as air (or an
     * air-equivalent block). This allows us to distinguish between original map
     * blocks and blocks placed by players during the game.
     */
    public boolean isOriginallyAir(final IArena arena, final Block block) {
        final Map<BlockPosition, BlockData> snapshot = arenaSnapshots.get(arena.getId());
        if (snapshot == null || snapshot.isEmpty()) {
            return false;
        }

        final BlockPosition position = BlockPosition.fromBlock(block);
        final BlockData data = snapshot.get(position);
        if (data == null) {
            return false;
        }

        return data.getMaterial().isAir();
    }

    /**
     * Restores all blocks in the snapshotted arena region back to their original state.
     * Also removes all dropped items (Item entities) within the snapshot region.
     * <p>
     * Originally this adapter tried to restore only modified blocks, but that can
     * miss changes from explosions or external plugins. For reliability we now
     * restore the full snapshot each time, which is still fast enough for typical
     * Bed Wars arenas.
     */
    public void restoreArena(final IArena arena) {
        final Map<BlockPosition, BlockData> snapshot = arenaSnapshots.get(arena.getId());
        final World world = arena.getWorld();
        if (world == null) {
            return;
        }

        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        clearDroppedItemsInSnapshotBounds(world, snapshot.keySet());

        for (Map.Entry<BlockPosition, BlockData> entry : snapshot.entrySet()) {
            final BlockPosition position = entry.getKey();
            final BlockData data = entry.getValue();
            final Block block = world.getBlockAt(position.x(), position.y(), position.z());
            block.setBlockData(data, false);
        }

        final Set<BlockPosition> modified = modifiedBlocks.get(arena.getId());
        if (modified != null) {
            modified.clear();
        }
    }

    private void clearDroppedItemsInSnapshotBounds(final World world, final Set<BlockPosition> positions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPosition p : positions) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            minZ = Math.min(minZ, p.z());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
            maxZ = Math.max(maxZ, p.z());
        }
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Item)) {
                continue;
            }
            final Location loc = entity.getLocation();
            final int bx = loc.getBlockX();
            final int by = loc.getBlockY();
            final int bz = loc.getBlockZ();
            if (bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ) {
                entity.remove();
            }
        }
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
