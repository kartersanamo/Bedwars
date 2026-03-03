package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StartSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Force start the current Bedwars arena.";
    }

    @Override
    public String getUsage() {
        return "/bedwars start";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.start";
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
            sender.sendMessage("You are not currently in a Bedwars arena.");
            return true;
        }

        if (arena.getGameState() == EGameState.IN_GAME) {
            sender.sendMessage("The game is already running.");
            return true;
        }

        arena.forceStart();
        sender.sendMessage("Forced game start.");
        return true;
    }
}
