package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.npc.NPCSpawner;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NPCDeleteSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete an NPC by its ID.";
    }

    @Override
    public String getUsage() {
        return "/bw npc delete <npc-id>";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.npc";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bw npc delete <npc-id>");
            return true;
        }

        final String npcId = args[0];
        final Bedwars plugin = Bedwars.getInstance();

        try {
            UUID fullUuid = null;
            for (NPCSpawner spawner : plugin.getNpcManager().getAllNPCs()) {
                if (spawner.getUuid().toString().startsWith(npcId)) {
                    fullUuid = spawner.getUuid();
                    break;
                }
            }

            if (fullUuid == null) {
                sender.sendMessage(ChatColor.RED + "NPC not found with ID: " + npcId);
                return true;
            }

            if (plugin.getNpcManager().removeNPC(fullUuid)) {
                sender.sendMessage(ChatColor.GREEN + "NPC deleted successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Could not delete NPC.");
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid NPC ID format.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            final List<String> result = new ArrayList<>();
            final String prefix = args[0].toLowerCase();
            final Bedwars plugin = Bedwars.getInstance();
            for (NPCSpawner spawner : plugin.getNpcManager().getAllNPCs()) {
                final String shortId = spawner.getUuid().toString().substring(0, 8);
                if (shortId.startsWith(prefix)) {
                    result.add(shortId);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
