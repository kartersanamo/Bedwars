package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        sender.sendMessage(ChatColor.YELLOW + "Join through a Bedwars NPC.");
        return true;
    }
}
