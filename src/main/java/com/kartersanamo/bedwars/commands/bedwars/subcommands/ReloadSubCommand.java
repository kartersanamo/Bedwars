package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class ReloadSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload all arenas from disk.";
    }

    @Override
    public String getUsage() {
        return "/bedwars reload";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.reload";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        final Bedwars plugin = Bedwars.getInstance();
        plugin.reloadArenas();
        sender.sendMessage(ChatColor.GREEN + "Bedwars arenas reloaded.");
        return true;
    }
}
