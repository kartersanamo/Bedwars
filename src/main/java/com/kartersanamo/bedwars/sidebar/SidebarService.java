package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.sidebar.ISidebar;
import com.kartersanamo.bedwars.api.sidebar.ISidebarManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
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
        final String title = ChatColor.YELLOW.toString() + ChatColor.BOLD + "BED WARS";

        final List<String> lines = new ArrayList<>();

        // Top date line similar to Hypixel style.
        final String date = new SimpleDateFormat("MM/dd/yy").format(new Date());
        lines.add(ChatColor.GRAY + date);

        lines.add(" ");

        // Placeholder for future NextEvent implementation.
        lines.add(ChatColor.WHITE + "Bed gone in " + ChatColor.GREEN + "3:51");

        lines.add(" ");

        // Team lines in canonical color order to mirror Hypixel.
        final List<ITeam> arenaTeams = arena.getTeams();
        for (ETeamColor color : ETeamColor.values()) {
            final Optional<ITeam> maybeTeam = arenaTeams.stream()
                    .filter(team -> team.getColor() == color)
                    .findFirst();

            final String colorDisplayName = capitalize(color.name().toLowerCase(Locale.ROOT));
            final String prefixLetter = color.name().substring(0, 1);

            String status;
            if (maybeTeam.isEmpty()) {
                status = ChatColor.RED + "✖";
            } else {
                final ITeam team = maybeTeam.get();
                if (!team.isBedDestroyed()) {
                    status = ChatColor.GREEN + "✔";
                } else {
                    // Bed destroyed: show number of alive players remaining, or X if none.
                    final long alive = team.getOnlineMembers().stream()
                            .filter(p -> arena.contains(p) && !p.isDead())
                            .count();
                    if (alive > 0) {
                        status = ChatColor.YELLOW.toString() + alive;
                    } else {
                        status = ChatColor.RED + "✖";
                    }
                }
            }

            final String line = color.getChatColor() + prefixLetter + " "
                    + color.getChatColor() + colorDisplayName + ChatColor.WHITE + ": "
                    + status;
            lines.add(line);
        }

        lines.add(" ");
        lines.add(ChatColor.GOLD + "play.kartersanamo.com");

        for (Player player : arena.getPlayers()) {
            if (!player.getWorld().equals(arena.getWorld())) {
                removeSidebar(player);
                continue;
            }
            final ISidebar sidebar = createSidebar(player);
            sidebar.setTitle(title);
            sidebar.setLines(lines);
        }
    }

    private String capitalize(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
