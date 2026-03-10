package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.npc.NPCSpawner;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class NPCListSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all spawned NPCs.";
    }

    @Override
    public String getUsage() {
        return "/bw npc list";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.npc";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        final Bedwars plugin = Bedwars.getInstance();
        final List<NPCSpawner> npcs = new ArrayList<>(plugin.getNpcManager().getAllNPCs());

        if (npcs.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No NPCs are currently spawned.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Spawned NPCs:");
        for (NPCSpawner spawner : npcs) {
            if (spawner.isValid()) {
                final String id = spawner.getUuid().toString().substring(0, 8);
                final String gameMode = spawner.getGameMode().getDisplayName();
                final String world = spawner.getLocation().getWorld() != null ? spawner.getLocation().getWorld().getName() : "Unknown";
                sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.AQUA + id + ChatColor.GRAY + " | " +
                        ChatColor.GREEN + gameMode + ChatColor.GRAY + " | " +
                        ChatColor.YELLOW + world);
            }
        }

        return true;
    }
}
