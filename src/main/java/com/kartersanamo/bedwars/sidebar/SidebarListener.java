package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Ensures sidebars are created/removed with player lifecycle.
 */
public final class SidebarListener implements Listener {

    private final SidebarService sidebarService;

    public SidebarListener(final SidebarService sidebarService) {
        this.sidebarService = sidebarService;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        // Sidebar will be populated once the player joins an arena.
        sidebarService.createSidebar(event.getPlayer());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        sidebarService.removeSidebar(event.getPlayer());
    }

    public void refreshArena(final IArena arena) {
        sidebarService.updateForArena(arena);
    }
}
