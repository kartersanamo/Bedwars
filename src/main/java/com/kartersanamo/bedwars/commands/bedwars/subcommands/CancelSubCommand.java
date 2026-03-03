package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public final class CancelSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "cancel";
    }

    @Override
    public String getDescription() {
        return "Cancel the current lobby (only for games that have not started yet).";
    }

    @Override
    public String getUsage() {
        return "/bedwars cancel";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.cancel";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        final Bedwars plugin = Bedwars.getInstance();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "You are not currently in a Bedwars arena.");
            return true;
        }

        final EGameState state = arena.getGameState();
        if (state == EGameState.IN_GAME || state == EGameState.ENDING || state == EGameState.RESETTING) {
            sender.sendMessage(ChatColor.RED + "The game has already started. Use /bw stop to force stop.");
            return true;
        }

        if (state != EGameState.LOBBY_WAITING && state != EGameState.STARTING) {
            sender.sendMessage(ChatColor.RED + "There is no waiting lobby to cancel.");
            return true;
        }

        if (!(arena instanceof Arena concreteArena)) {
            sender.sendMessage(ChatColor.RED + "Could not cancel this arena.");
            return true;
        }

        concreteArena.cancelCountdown();

        final Location lobbySpawn = plugin.getMainConfig().getLobbySpawn();
        final Location teleportTo = lobbySpawn != null ? lobbySpawn : player.getWorld().getSpawnLocation();

        final Collection<Player> toRemove = new ArrayList<>(arena.getPlayers());
        for (Player p : toRemove) {
            p.teleport(teleportTo);
            arena.removePlayer(p, false);
            plugin.getArenaManager().playerLeftArena(p);
            plugin.getSidebarService().removeSidebar(p);
        }

        arena.resetAfterGame();

        sender.sendMessage(ChatColor.GREEN + "Lobby cancelled. All players have been sent to the lobby.");
        return true;
    }
}
