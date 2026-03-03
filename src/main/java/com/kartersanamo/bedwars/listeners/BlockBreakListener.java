package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
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

        // Bed detection: if the broken block matches a team's bed location
        for (ITeam team : concreteArena.getTeams()) {
            if (team.getBedLocation().getBlock().equals(block)) {
                // Prevent breaking own bed.
                if (concreteArena.getTeam(player).map(t -> t == team).orElse(false)) {
                    event.setCancelled(true);
                    return;
                }

                concreteArena.handleBedDestroyed(team, player);
                // TODO: fire custom PlayerBedBreakEvent and play sounds / messages.
                break;
            }
        }

        final InternalAdapter adapter = plugin.getInternalAdapter();
        adapter.markModified(arena, block);
    }
}
