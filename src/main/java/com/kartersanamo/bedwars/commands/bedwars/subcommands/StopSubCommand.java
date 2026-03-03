package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.arena.tasks.GameRestartingTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StopSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Force stop the current Bedwars game and reset the arena.";
    }

    @Override
    public String getUsage() {
        return "/bedwars stop";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.stop";
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

        if (arena.getGameState() == EGameState.LOBBY_WAITING) {
            sender.sendMessage("There is no running game to stop.");
            return true;
        }

        arena.setGameState(EGameState.ENDING);
        new GameRestartingTask(plugin, arena, 0).runTask(plugin);
        sender.sendMessage("Stopped the current Bedwars game and started cleanup.");
        return true;
    }
}

