package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class NPCRepairSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "repair";
    }

    @Override
    public String getDescription() {
        return "Rebuild NPC mappings from persisted world entities.";
    }

    @Override
    public String getUsage() {
        return "/bw npc repair";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.npc";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        final Bedwars plugin = Bedwars.getInstance();
        final int before = plugin.getNpcManager().getAllNPCs().size();
        final int after = plugin.getNpcManager().repairRuntimeMappings();

        sender.sendMessage(ChatColor.GREEN + "NPC repair complete.");
        sender.sendMessage(ChatColor.GRAY + "Before: " + ChatColor.YELLOW + before + ChatColor.GRAY
                + " | After: " + ChatColor.YELLOW + after);
        if (after == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No NPC entities were found to map.");
        }
        return true;
    }
}

