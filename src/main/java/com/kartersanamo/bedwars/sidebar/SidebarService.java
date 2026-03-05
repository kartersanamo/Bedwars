package com.kartersanamo.bedwars.sidebar;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.sidebar.ISidebar;
import com.kartersanamo.bedwars.api.sidebar.ISidebarManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sidebar manager: waiting-lobby scoreboard (map, players, status, mode, version)
 * and in-game scoreboard (team bed status).
 */
public final class SidebarService implements ISidebarManager {

    private final Map<UUID, ISidebar> sidebars = new HashMap<>();
    private final Map<UUID, BossBar> lobbyBossBars = new HashMap<>();
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
        removeLobbyBossBar(player);
        player.setPlayerListHeaderFooter("", "");
    }

    public void updateForArena(final IArena arena) {
        final String title = ChatColor.YELLOW.toString() + ChatColor.BOLD + "BED WARS";
        final EGameState state = arena.getGameState();

        for (Player viewer : arena.getPlayers()) {
            if (!viewer.getWorld().equals(arena.getWorld())) {
                removeSidebar(viewer);
                continue;
            }

            final List<String> lines = (state == EGameState.LOBBY_WAITING || state == EGameState.STARTING)
                    ? buildLobbyLines(arena)
                    : buildGameLines(arena, viewer);

            final ISidebar sidebar = createSidebar(viewer);
            sidebar.setTitle(title);
            sidebar.setLines(lines);

            updateTabList(viewer, arena);
            updateTabPlayerNamesAndHealth(viewer, arena);

            if (state == EGameState.LOBBY_WAITING || state == EGameState.STARTING) {
                ensureLobbyBossBar(viewer);
            } else {
                removeLobbyBossBar(viewer);
            }
        }
    }

    /**
     * Sets the tab list header and footer for a player in an arena.
     * Header: "You are playing on PLAY.KARTERSANAMO.COM"
     * Footer: "Kills: X Final Kills: Y Beds Broken: Z" (this game when in game/ending, else 0) and "Ranks, Boosters & MORE! STORE.KARTERSANAMO.COM"
     */
    private void updateTabList(final Player player, final IArena arena) {
        final String header = ChatColor.AQUA + "You are playing on " + ChatColor.YELLOW + ChatColor.BOLD + "PLAY.KARTERSANAMO.COM";
        int kills, finalKills, bedsBroken;
        final EGameState state = arena.getGameState();
        if (state == EGameState.IN_GAME || state == EGameState.ENDING) {
            kills = arena.getKillsThisGame(player.getUniqueId());
            finalKills = arena.getFinalKillsThisGame(player.getUniqueId());
            bedsBroken = arena.getBedsBrokenThisGame(player.getUniqueId());
        } else {
            kills = 0;
            finalKills = 0;
            bedsBroken = 0;
        }
        final String footer = ChatColor.AQUA + "Kills: " + ChatColor.YELLOW + kills
                + ChatColor.AQUA + " Final Kills: " + ChatColor.YELLOW + finalKills
                + ChatColor.AQUA + " Beds Broken: " + ChatColor.YELLOW + bedsBroken
                + "\n"
                + ChatColor.GREEN + "Ranks, Boosters & MORE! " + ChatColor.RED + ChatColor.BOLD + "STORE.KARTERSANAMO.COM";
        player.setPlayerListHeaderFooter(header, footer);
    }

    /**
     * Updates tablist player names (team-colored with leading letter) and health numbers on the right.
     */
    private void updateTabPlayerNamesAndHealth(final Player viewer, final IArena arena) {
        final Scoreboard scoreboard = viewer.getScoreboard();
        Objective healthObj = scoreboard.getObjective("bw_health");
        if (healthObj == null) {
            healthObj = scoreboard.registerNewObjective("bw_health", "dummy", ChatColor.YELLOW + "HP");
            healthObj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Player p : arena.getPlayers()) {
            // Team-colored tab name with bold first letter.
            final ITeam team = arena.getTeam(p).orElse(null);
            if (team != null) {
                final ETeamColor color = team.getColor();
                final String prefixLetter = color.name().substring(0, 1);
                final String listName = color.getChatColor() + "" + ChatColor.BOLD + prefixLetter + " "
                        + color.getChatColor() + p.getName();
                p.setPlayerListName(listName);
            } else {
                p.setPlayerListName(p.getName());
            }

            // Health value on the right side (integer hearts *2 or raw health).
            final int health = (int) Math.round(p.getHealth());
            healthObj.getScore(p.getName()).setScore(health);
        }
    }

    /**
     * Shows a static boss bar in the waiting lobby:
     * "Playing BED WARS on PLAY.KARTERSANAMO.COM".
     */
    private void ensureLobbyBossBar(final Player player) {
        final UUID uuid = player.getUniqueId();
        BossBar bar = lobbyBossBars.get(uuid);
        final String title = ChatColor.YELLOW + "Playing " + ChatColor.WHITE + "" + ChatColor.BOLD + "BED WARS "
                + ChatColor.YELLOW + "on " + ChatColor.AQUA + ChatColor.BOLD + "PLAY.KARTERSANAMO.COM";
        if (bar == null) {
            bar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
            bar.setProgress(1.0);
            bar.addPlayer(player);
            bar.setVisible(true);
            lobbyBossBars.put(uuid, bar);
        } else {
            bar.setTitle(title);
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
            bar.setVisible(true);
        }
    }

    private void removeLobbyBossBar(final Player player) {
        final UUID uuid = player.getUniqueId();
        final BossBar bar = lobbyBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    private List<String> buildLobbyLines(final IArena arena) {
        final List<String> lines = new ArrayList<>();
        lines.add(blank(0));

        lines.add(ChatColor.WHITE + "Map: " + ChatColor.GREEN + arena.getDisplayName());
        final int current = arena.getPlayers().size();
        final int max = arena.getMaxPlayers();
        lines.add(ChatColor.WHITE + "Players: " + ChatColor.GREEN + current + "/" + max);

        lines.add(blank(1));

        final String status = arena.getGameState() == EGameState.STARTING
                ? ChatColor.WHITE + "Starting..."
                : ChatColor.WHITE + "Waiting...";
        lines.add(status);

        lines.add(blank(2));

        final String modeStr = modeName(arena.getTeamSize());
        lines.add(ChatColor.WHITE + "Mode: " + ChatColor.GREEN + modeStr);

        final String version = "v" + plugin.getDescription().getVersion();
        lines.add(ChatColor.WHITE + "Version: " + ChatColor.GRAY + version);

        // Blank spacer before server line.
        lines.add(blank(3));
        lines.add(ChatColor.WHITE + "Server: " + ChatColor.GREEN + arena.getId());
        lines.add(blank(4));
        lines.add(ChatColor.YELLOW + "play.kartersanamo.com");
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

    private List<String> buildGameLines(final IArena arena, final Player viewer) {
        final List<String> lines = new ArrayList<>();
        final String date = new SimpleDateFormat("MM/dd/yy").format(new Date());
        final String shortId = arena.getId().length() >= 4 ? arena.getId().substring(0, 4).toUpperCase(Locale.ROOT) : arena.getId().toUpperCase(Locale.ROOT);
        lines.add(ChatColor.GRAY + date + " " + ChatColor.DARK_GRAY + shortId);
        lines.add(arena.getNextTierUpgradeMessage());
        lines.add(blank(0));
        final List<ITeam> arenaTeams = arena.getTeams();
        final ITeam viewerTeam = arena.getTeam(viewer).orElse(null);
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
                    final Set<UUID> activePlayerIds = arena.getPlayers().stream()
                            .map(Player::getUniqueId)
                            .collect(Collectors.toSet());
                    final long alive = team.getOnlineMembers().stream()
                            .filter(p -> activePlayerIds.contains(p.getUniqueId()))
                            .count();
                    status = alive > 0 ? ChatColor.YELLOW.toString() + alive : ChatColor.RED + "✖";
                }
            }
            if (viewerTeam != null && maybeTeam.isPresent() && maybeTeam.get() == viewerTeam) {
                status = status + ChatColor.GRAY + " YOU";
            }
            final String line = color.getChatColor() + prefixLetter + " "
                    + color.getChatColor() + colorDisplayName + ChatColor.WHITE + ": " + status;
            lines.add(line);
        }
        lines.add(blank(1));

        final int kills = arena.getKillsThisGame(viewer.getUniqueId());
        final int finalKills = arena.getFinalKillsThisGame(viewer.getUniqueId());
        final int bedsBroken = arena.getBedsBrokenThisGame(viewer.getUniqueId());

        lines.add(ChatColor.WHITE + "Kills: " + ChatColor.GREEN + kills);
        lines.add(ChatColor.WHITE + "Final Kills: " + ChatColor.GREEN + finalKills);
        lines.add(ChatColor.WHITE + "Beds Broken: " + ChatColor.GREEN + bedsBroken);

        lines.add(blank(2));
        lines.add(ChatColor.YELLOW + "play.kartersanamo.com");
        return lines;
    }

    /**
     * Returns a visually blank but scoreboard-unique line using different color codes.
     * Scoreboard requires each entry to be unique; using raw color codes achieves this
     * while rendering as an empty spacer line.
     */
    private String blank(final int index) {
        final ChatColor[] colors = ChatColor.values();
        final ChatColor color = colors[index % colors.length];
        return color.toString();
    }

    private String capitalize(final String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
