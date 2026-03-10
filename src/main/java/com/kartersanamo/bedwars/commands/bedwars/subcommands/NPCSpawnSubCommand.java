package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.npc.NPCSpawner;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class NPCSpawnSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Spawn an NPC for a specific game mode at your location.";
    }

    @Override
    public String getUsage() {
        return "/bw npc spawn <gamemode>";
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

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bw npc spawn <gamemode>");
            return true;
        }

        final EGameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            sender.sendMessage(ChatColor.RED + "Invalid game mode. Available: " + String.join(", ", getGameModeNames()));
            return true;
        }

        final Bedwars plugin = Bedwars.getInstance();
        if (!plugin.getNpcManager().getNPCsForMode(gameMode).isEmpty()) {
            sender.sendMessage("" + ChatColor.YELLOW + ChatColor.BOLD + "WARNING: An NPC for " + gameMode.getDisplayName() +
                    " already exists! Are you sure you want to spawn another one?");
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.AQUA + "/bw npc list" + ChatColor.YELLOW +
                    " to view all NPCs or " + ChatColor.AQUA + "/bw npc remove" + ChatColor.YELLOW + " to remove one.");
        }

        final NPCSpawner spawner = plugin.getNpcManager().spawnNPC(player.getLocation(), gameMode);
        sender.sendMessage(ChatColor.GREEN + "NPC spawned for " + gameMode.getDisplayName() + " at your location!");
        sender.sendMessage(ChatColor.GRAY + "NPC ID: " + ChatColor.AQUA + spawner.getUuid().toString().substring(0, 8));
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            final List<String> result = new ArrayList<>();
            final String prefix = args[0].toLowerCase();
            for (String name : getGameModeNames()) {
                if (name.startsWith(prefix)) {
                    result.add(name);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private EGameMode parseGameMode(final String input) {
        try {
            return EGameMode.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> getGameModeNames() {
        final List<String> names = new ArrayList<>();
        for (EGameMode mode : EGameMode.values()) {
            names.add(mode.name().toLowerCase());
        }
        return names;
    }
}
