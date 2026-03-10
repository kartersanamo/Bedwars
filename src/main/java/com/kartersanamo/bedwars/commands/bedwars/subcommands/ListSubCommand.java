package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.gui.AdminArenaBrowserGui;
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
        return "Open the admin arena browser.";
    }

    @Override
    public String getUsage() {
        return "/bw list";
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

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.GOLD + "Bedwars arenas loaded: " + ChatColor.GREEN + arenas.size());
            sender.sendMessage(ChatColor.GRAY + "Run this command in-game to open the admin arena browser GUI.");
            return true;
        }

        AdminArenaBrowserGui.openFor(player, 0);
        return true;
    }
}