package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.arena.team.TeamAssigner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class JoinSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Join a Bedwars arena.";
    }

    @Override
    public String getUsage() {
        return "/bedwars join [arenaId]";
    }

    @Override
    public String getPermission() {
        return "bedwars.join";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        final Bedwars plugin = Bedwars.getInstance();
        final IArena currentArena = plugin.getArenaManager().getArena(player);
        if (currentArena != null) {
            sender.sendMessage("You are already in a Bedwars arena. Use /bedwars leave before joining another one.");
            return true;
        }

        final IArena targetArena;

        if (args.length >= 1) {
            targetArena = plugin.getArenaManager().getArena(args[0]);
            if (targetArena == null) {
                sender.sendMessage("Unknown arena: " + args[0]);
                return true;
            }
        } else {
            final Optional<IArena> best = plugin.getArenaManager().findBestJoinableArena();
            if (best.isEmpty()) {
                sender.sendMessage("No joinable arenas available.");
                return true;
            }
            targetArena = best.get();
        }

        if (!targetArena.addPlayer(player)) {
            sender.sendMessage("Could not join this arena (it may be full or not joinable).");
            return true;
        }

        plugin.getArenaManager().playerJoinedArena(player, targetArena);

        // Assign to a team.
        new TeamAssigner().assignPlayerToTeam(targetArena, player);

        player.teleport(targetArena.getLobbySpawn());
        final int current = targetArena.getPlayers().size();
        final int max = targetArena.getMaxPlayers();
        for (Player other : targetArena.getPlayers()) {
            other.sendMessage(player.getName() + " joined the game (" + current + "/" + max + ").");
        }

        targetArena.tryStartCountdown();
        return true;
    }
}
