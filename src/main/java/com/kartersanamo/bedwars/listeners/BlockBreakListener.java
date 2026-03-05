package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles block breaking inside arenas, including bed detection.
 */
public final class BlockBreakListener implements Listener {

    private final Bedwars plugin;

    public BlockBreakListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (!(arena instanceof Arena concreteArena)) {
            return;
        }

        final Block block = event.getBlock();
        final Location location = block.getLocation();
        if (!concreteArena.isInsideRegion(location)) {
            return;
        }

        final InternalAdapter adapter = plugin.getInternalAdapter();

        // First, handle beds (both head and foot) specially.
        final BlockData data = block.getBlockData();
        if (data instanceof Bed bedData) {
            final Block otherHalf = getOtherBedHalf(block, bedData);

                    for (ITeam team : concreteArena.getTeams()) {
                        final Block teamBedBlock = team.getBedLocation().getBlock();
                        if (teamBedBlock.equals(block) || teamBedBlock.equals(otherHalf)) {
                            // Prevent breaking own bed.
                            if (concreteArena.getTeam(player).map(t -> t == team).orElse(false)) {
                                event.setCancelled(true);
                                player.sendMessage(org.bukkit.ChatColor.RED + "You can't destroy your own bed!");
                                return;
                            }

                    // Enemy bed: mark as destroyed and prevent item drops.
                    concreteArena.handleBedDestroyed(team, player);
                    event.setDropItems(false);

                    adapter.markModified(arena, block);
                    adapter.markModified(arena, otherHalf);
                    return;
                }
            }
        }

        // Non-bed blocks: only allow breaking if this position was originally air
        // in the snapshot (i.e., player-placed blocks such as bridges).
        if (!adapter.isOriginallyAir(arena, block)) {
            event.setCancelled(true);
            return;
        }

        adapter.markModified(arena, block);
    }

    private Block getOtherBedHalf(final Block block, final Bed bedData) {
        final org.bukkit.block.BlockFace facing = bedData.getFacing();
        final org.bukkit.block.BlockFace offset;
        if (bedData.getPart() == Bed.Part.HEAD) {
            offset = facing.getOppositeFace();
        } else {
            offset = facing;
        }
        return block.getRelative(offset);
    }
}
