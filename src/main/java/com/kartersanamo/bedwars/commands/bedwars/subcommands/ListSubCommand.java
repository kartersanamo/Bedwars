package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class ListSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all Bedwars arenas and their current state.";
    }

    @Override
    public String getUsage() {
        return "/bedwars list";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.list";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        final Bedwars plugin = Bedwars.getInstance();
        final Collection<IArena> arenas = plugin.getArenaManager().getArenas();

        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No Bedwars arenas are currently loaded.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Bedwars arenas:");
        for (IArena arena : arenas) {
            final EGameState state = arena.getGameState();
            final int playerCount = arena.getPlayers().size();
            sender.sendMessage(ChatColor.AQUA + "- " + arena.getId() + ChatColor.GRAY + " (" + arena.getDisplayName() + ")" +
                    ChatColor.DARK_GRAY + " [" + state.name() + ", " + playerCount + " players]");

            final boolean inLobby = state == EGameState.LOBBY_WAITING || state == EGameState.STARTING;
            if (inLobby) {
                // No teams yet: list all players in dark gray.
                final StringBuilder list = new StringBuilder();
                boolean first = true;
                for (Player p : arena.getPlayers()) {
                    if (!first) {
                        list.append(ChatColor.DARK_GRAY).append(", ");
                    }
                    list.append(ChatColor.DARK_GRAY).append(p.getName());
                    first = false;
                }
                sender.sendMessage("  " + (list.length() > 0 ? list.toString() : ChatColor.DARK_GRAY + "(no players)"));
            } else {
                // In-game: list by team with team-colored names and bed status.
                for (ITeam team : arena.getTeams()) {
                    final String bedStatus = team.isBedDestroyed() ? ChatColor.RED + "✖" : ChatColor.GREEN + "✔";
                    final StringBuilder members = new StringBuilder();
                    boolean first = true;
                    for (Player member : team.getOnlineMembers()) {
                        if (!first) {
                            members.append(ChatColor.DARK_GRAY).append(", ");
                        }
                        members.append(team.getColor().getChatColor()).append(member.getName());
                        first = false;
                    }
                    sender.sendMessage("  " + team.getColor().getChatColor() + team.getId()
                            + ChatColor.DARK_GRAY + " [Bed: " + bedStatus + ChatColor.DARK_GRAY + "] "
                            + (members.length() > 0 ? members.toString() : ChatColor.DARK_GRAY + "(no online players)"));
                }
            }
        }

        return true;
    }
}