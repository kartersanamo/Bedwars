package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.sidebar.ISidebar;
import com.kartersanamo.bedwars.api.sidebar.ISidebarManager;
import com.kartersanamo.bedwars.database.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Sidebar manager: waiting-lobby scoreboard (map, players, status, mode, version)
 * and in-game scoreboard (team bed status).
 */
public final class SidebarService implements ISidebarManager {

    private final Map<UUID, ISidebar> sidebars = new HashMap<>();
    private final Bedwars plugin;

    public SidebarService(final Bedwars plugin) {
        this.plugin = plugin;
    }

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
        player.setPlayerListHeaderFooter("", "");
    }

    public void updateForArena(final IArena arena) {
        final String title = ChatColor.YELLOW.toString() + ChatColor.BOLD + "BED WARS";
        final List<String> lines;
        final EGameState state = arena.getGameState();
        if (state == EGameState.LOBBY_WAITING || state == EGameState.STARTING) {
            lines = buildLobbyLines(arena);
        } else {
            lines = buildGameLines(arena);
        }

        for (Player player : arena.getPlayers()) {
            if (!player.getWorld().equals(arena.getWorld())) {
                removeSidebar(player);
                continue;
            }
            final ISidebar sidebar = createSidebar(player);
            sidebar.setTitle(title);
            sidebar.setLines(lines);
            updateTabList(player);
        }
    }

    /**
     * Sets the tab list header and footer for a player in an arena.
     * Header: "You are playing on PLAY.KARTERSANAMO.COM"
     * Footer: "Kills: X Final Kills: Y Beds Broken: Z" and "Ranks, Boosters & MORE! STORE.KARTERSANAMO.COM"
     */
    private void updateTabList(final Player player) {
        final String header = ChatColor.AQUA + "You are playing on " + ChatColor.YELLOW + ChatColor.BOLD + "PLAY.KARTERSANAMO.COM";
        int kills = 0, finalKills = 0, bedsBroken = 0;
        try {
            final PlayerStats stats = plugin.getDatabase().getCachedStats(player.getUniqueId(), player.getName());
            kills = stats.getKills();
            finalKills = stats.getFinalKills();
            bedsBroken = stats.getBedsBroken();
        } catch (Exception ignored) {
        }
        final String footer = ChatColor.GRAY + "Kills: " + ChatColor.WHITE + kills
                + ChatColor.GRAY + " Final Kills: " + ChatColor.WHITE + finalKills
                + ChatColor.GRAY + " Beds Broken: " + ChatColor.WHITE + bedsBroken
                + "\n"
                + ChatColor.GREEN + "Ranks, Boosters & MORE! " + ChatColor.RED + ChatColor.BOLD + "STORE.KARTERSANAMO.COM";
        player.setPlayerListHeaderFooter(header, footer);
    }

    private List<String> buildLobbyLines(final IArena arena) {
        final List<String> lines = new ArrayList<>();
        final String date = new SimpleDateFormat("MM/dd/yy").format(new Date());
        final String shortId = arena.getId().length() >= 4 ? arena.getId().substring(0, 4).toUpperCase(Locale.ROOT) : arena.getId().toUpperCase(Locale.ROOT);
        lines.add(ChatColor.GRAY + date + " " + shortId);

        lines.add(ChatColor.WHITE + "Map: " + ChatColor.GREEN + arena.getDisplayName());
        final int current = arena.getPlayers().size();
        final int max = arena.getMaxPlayers();
        lines.add(ChatColor.GRAY + "Players: " + current + "/" + max);

        final String status = arena.getGameState() == EGameState.STARTING
                ? ChatColor.GRAY + "Starting..."
                : ChatColor.GRAY + "Waiting...";
        lines.add(status);

        final String modeStr = modeName(arena.getTeamSize());
        lines.add(ChatColor.WHITE + "Mode: " + ChatColor.GREEN + modeStr);

        final String version = "v" + plugin.getDescription().getVersion();
        lines.add(ChatColor.GRAY + "Version: " + version);

        lines.add(ChatColor.GRAY + " ");
        lines.add(ChatColor.RED + "play.kartersanamo.com");
        return lines;
    }

    private static String modeName(final int teamSize) {
        return switch (teamSize) {
            case 1 -> "Solo";
            case 2 -> "Doubles";
            case 3 -> "3s";
            case 4 -> "4s";
            default -> teamSize + "s";
        };
    }

    private List<String> buildGameLines(final IArena arena) {
        final List<String> lines = new ArrayList<>();
        final String date = new SimpleDateFormat("MM/dd/yy").format(new Date());
        lines.add(ChatColor.GRAY + date);
        lines.add(arena.getNextTierUpgradeMessage());
        lines.add(ChatColor.GRAY + " ");
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
                    final long alive = team.getOnlineMembers().stream()
                            .filter(p -> arena.contains(p) && !p.isDead())
                            .count();
                    status = alive > 0 ? ChatColor.YELLOW.toString() + alive : ChatColor.RED + "✖";
                }
            }
            final String line = color.getChatColor() + prefixLetter + " "
                    + color.getChatColor() + colorDisplayName + ChatColor.WHITE + ": " + status;
            lines.add(line);
        }
        lines.add(ChatColor.GRAY + "  ");
        lines.add(ChatColor.GOLD + "play.kartersanamo.com");
        return lines;
    }

    private String capitalize(final String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
