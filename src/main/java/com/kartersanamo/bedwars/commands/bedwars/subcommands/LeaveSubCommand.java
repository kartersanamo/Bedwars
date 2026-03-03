package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LeaveSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leave your current Bedwars arena.";
    }

    @Override
    public String getUsage() {
        return "/bedwars leave";
    }

    @Override
    public String getPermission() {
        return "bedwars.leave";
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

        arena.removePlayer(player, true);
        plugin.getArenaManager().playerLeftArena(player);

        if (plugin.getMainConfig().getLobbySpawn() != null) {
            player.teleport(plugin.getMainConfig().getLobbySpawn());
        }

        sender.sendMessage("You left the arena.");
        return true;
    }
}
