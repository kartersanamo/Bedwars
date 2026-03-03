package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.maprestore.InternalAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Tracks block placements inside arenas so they can be restored later.
 */
public final class BlockPlaceListener implements Listener {

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
        if (!concreteArena.isInsideRegion(location)) {
            return;
        }

        final InternalAdapter adapter = plugin.getInternalAdapter();
        adapter.markModified(arena, block);
    }
}
