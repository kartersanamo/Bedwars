package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.arena.OreGenerator;
import com.kartersanamo.bedwars.configuration.ArenaConfig;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Tracks block placements inside arenas so they can be restored later.
 * Prevents placing blocks near generators (2 blocks horizontal, 5 up, 2 down).
 * For iron/gold: also prevents placing 11 blocks in front (toward bed/spawn) and 7 blocks up.
 */
public final class BlockPlaceListener implements Listener {

    private static final int RADIUS_H = 2;
    private static final int RANGE_UP = 5;
    private static final int RANGE_DOWN = 2;

    private static final int IRON_GOLD_FORWARD_BLOCKS = 11;
    private static final int IRON_GOLD_UP_BLOCKS = 7;

    private static final int NPC_PROTECTION_RADIUS = 3;

    private final Bedwars plugin;

    public BlockPlaceListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (!(arena instanceof Arena concreteArena)) {
            return;
        }

        final Block block = event.getBlockPlaced();
        final Location location = block.getLocation();

        // In the waiting lobby, disallow placing blocks outside the configured lobby-region
        // bounding box for this arena.
        if (arena.getGameState() == com.kartersanamo.bedwars.api.arena.EGameState.LOBBY_WAITING
                || arena.getGameState() == com.kartersanamo.bedwars.api.arena.EGameState.STARTING) {
            if (!isInsideLobbyRegion(location, concreteArena)) {
                event.setCancelled(true);
                return;
            }
        }

        // Disallow placing blocks outside the configured arena-region for this arena.
        if (!concreteArena.isInsideRegion(location)) {
            event.setCancelled(true);
            return;
        }

        if (isInsideGeneratorProtection(location, arena)) {
            event.setCancelled(true);
            return;
        }
        if (isInsideIronGoldForwardProtection(location, arena)) {
            event.setCancelled(true);
            return;
        }
        if (isWithinBlocksOfNpc(location, arena)) {
            event.setCancelled(true);
            return;
        }

        final InternalAdapter adapter = plugin.getInternalAdapter();
        adapter.markModified(arena, block);
    }

    private boolean isInsideLobbyRegion(final Location location, final Arena arena) {
        if (location == null || location.getWorld() == null || !location.getWorld().equals(arena.getWorld())) {
            return false;
        }

        final java.util.Optional<ArenaConfig.Region> lobbyRegionOpt = arena.getLobbyRegion();
        if (lobbyRegionOpt.isEmpty()) {
            return true; // No lobby-region configured; don't restrict placement.
        }

        final ArenaConfig.Region lobbyRegion = lobbyRegionOpt.get();
        final Location pos1 = lobbyRegion.getPos1();
        final Location pos2 = lobbyRegion.getPos2();

        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    private boolean isInsideGeneratorProtection(final Location blockLoc, final IArena arena) {
        if (blockLoc.getWorld() == null || !blockLoc.getWorld().equals(arena.getWorld())) {
            return false;
        }
        final int bx = blockLoc.getBlockX();
        final int by = blockLoc.getBlockY();
        final int bz = blockLoc.getBlockZ();

        for (IGenerator gen : arena.getGenerators()) {
            final Location genLoc = gen.getLocation();
            if (genLoc.getWorld() == null || !genLoc.getWorld().equals(blockLoc.getWorld())) {
                continue;
            }
            final int gx = genLoc.getBlockX();
            final int gy = genLoc.getBlockY();
            final int gz = genLoc.getBlockZ();

            if (bx >= gx - RADIUS_H && bx <= gx + RADIUS_H
                    && by >= gy - RANGE_DOWN && by <= gy + RANGE_UP
                    && bz >= gz - RADIUS_H && bz <= gz + RADIUS_H) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the block is in the protected lane: 11 blocks in front of an iron/gold
     * generator (toward bed/spawn) and 7 blocks up from the generator's Y.
     */
    private boolean isInsideIronGoldForwardProtection(final Location blockLoc, final IArena arena) {
        if (blockLoc.getWorld() == null || !blockLoc.getWorld().equals(arena.getWorld())) {
            return false;
        }
        final int bx = blockLoc.getBlockX();
        final int by = blockLoc.getBlockY();
        final int bz = blockLoc.getBlockZ();

        for (IGenerator gen : arena.getGenerators()) {
            final EGeneratorType type = gen.getType();
            if (type != EGeneratorType.IRON && type != EGeneratorType.GOLD) {
                continue;
            }
            if (!(gen instanceof OreGenerator ore)) {
                continue;
            }
            final var forwardOpt = ore.getForwardTarget();
            if (forwardOpt.isEmpty()) {
                continue;
            }
            final Location genLoc = ore.getLocation();
            final Location targetLoc = forwardOpt.get();
            if (genLoc.getWorld() == null || !genLoc.getWorld().equals(blockLoc.getWorld())) {
                continue;
            }
            final int gx = genLoc.getBlockX();
            final int gy = genLoc.getBlockY();
            final int gz = genLoc.getBlockZ();

            final double tx = targetLoc.getX();
            final double tz = targetLoc.getZ();
            final int dx = Math.abs(tx - gx) >= Math.abs(tz - gz) ? (tx > gx ? 1 : (tx < gx ? -1 : 0)) : 0;
            final int dz = (dx == 0) ? (tz > gz ? 1 : (tz < gz ? -1 : 0)) : 0;
            if (dx == 0 && dz == 0) {
                continue;
            }

            if (by < gy || by > gy + IRON_GOLD_UP_BLOCKS) {
                continue;
            }
            if (dx != 0) {
                final int step = bx - gx;
                if (step * dx >= 1 && step * dx <= IRON_GOLD_FORWARD_BLOCKS && bz == gz) {
                    return true;
                }
            }
            if (dz != 0) {
                final int step = bz - gz;
                if (step * dz >= 1 && step * dz <= IRON_GOLD_FORWARD_BLOCKS && bx == gx) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWithinBlocksOfNpc(final Location blockLoc, final IArena arena) {
        final World world = arena.getWorld();
        if (world == null || !world.equals(blockLoc.getWorld())) {
            return false;
        }
        final int bx = blockLoc.getBlockX();
        final int by = blockLoc.getBlockY();
        final int bz = blockLoc.getBlockZ();

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            final Location npcLoc = villager.getLocation();
            final int dx = Math.abs(bx - npcLoc.getBlockX());
            final int dy = Math.abs(by - npcLoc.getBlockY());
            final int dz = Math.abs(bz - npcLoc.getBlockZ());
            if (dx <= NPC_PROTECTION_RADIUS && dy <= NPC_PROTECTION_RADIUS && dz <= NPC_PROTECTION_RADIUS) {
                return true;
            }
        }
        return false;
    }
}
