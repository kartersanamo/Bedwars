package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NPCRemoveSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Enter remove mode to delete NPCs by clicking them.";
    }

    @Override
    public String getUsage() {
        return "/bw npc remove";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.npc";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        final Bedwars plugin = Bedwars.getInstance();
        plugin.getNpcManager().setRemoveMode(player, !plugin.getNpcManager().isInRemoveMode(player));

        if (plugin.getNpcManager().isInRemoveMode(player)) {
            sender.sendMessage("" + ChatColor.YELLOW + ChatColor.BOLD + "Remove Mode ENABLED!");
            sender.sendMessage(ChatColor.GRAY + "Right-click an NPC to remove it. Type " + ChatColor.AQUA + "/bw npc remove" +
                    ChatColor.GRAY + " again to disable.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Remove Mode disabled.");
        }

        return true;
    }
}
