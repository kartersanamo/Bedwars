package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.sidebar.ISidebar;
import com.kartersanamo.bedwars.api.sidebar.ISidebarManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Simple sidebar manager that shows arena state and team bed status.
 */
public final class SidebarService implements ISidebarManager {

    private final Map<UUID, ISidebar> sidebars = new HashMap<>();

    @Override
    public ISidebar createSidebar(final Player player) {
        return sidebars.computeIfAbsent(player.getUniqueId(), uuid -> new BWSidebar(player));
    }

    @Override
    public Optional<ISidebar> getSidebar(final Player player) {
        return Optional.ofNullable(sidebars.get(player.getUniqueId()));
    }

    @Override
    public void removeSidebar(final Player player) {
        final ISidebar sidebar = sidebars.remove(player.getUniqueId());
        if (sidebar != null) {
            sidebar.remove();
        }
    }

    public void updateForArena(final IArena arena) {
        final String title = arena.getDisplayName();

        final List<String> lines = new ArrayList<>();
        lines.add("Players: " + arena.getPlayers().size());

        for (ITeam team : arena.getTeams()) {
            final String status = team.isBedDestroyed() ? "✖" : "✔";
            lines.add(team.getColor().getChatColor() + team.getId() + ": " + status);
        }

        for (Player player : arena.getPlayers()) {
            final ISidebar sidebar = createSidebar(player);
            sidebar.setTitle(title);
            sidebar.setLines(lines);
        }
    }
}
