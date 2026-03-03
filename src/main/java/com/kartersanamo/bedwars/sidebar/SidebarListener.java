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
        // Do not create a sidebar on join. Sidebar is only created when the player is in an arena
        // (see SidebarUpdateTask -> updateForArena), so players outside Bedwars never see "Bedwars" in the sidebar.
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        sidebarService.removeSidebar(event.getPlayer());
    }

    public void refreshArena(final IArena arena) {
        sidebarService.updateForArena(arena);
    }
}
