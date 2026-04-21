package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public final class SetupSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription() {
        return "Open the in-world arena setup wizard (new arenas pick game modes in a GUI first).";
    }

    @Override
    public String getUsage() {
        return "/bedwars setup <arenaId> | /bedwars setup new <arenaId> | /bedwars setup exit";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.setup";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use the setup wizard.");
            return true;
        }
        final Bedwars plugin = Bedwars.getInstance();
        if (plugin.getArenaManager().getArena(player) != null) {
            sender.sendMessage(ChatColor.RED + "Leave your current arena before using setup.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + getUsage());
            return true;
        }
        if ("exit".equalsIgnoreCase(args[0])) {
            plugin.getSetupWizardService().exitWithoutReload(player);
            return true;
        }
        final boolean createNew = "new".equalsIgnoreCase(args[0]);
        final String arenaId = createNew
                ? (args.length > 1 ? args[1] : null)
                : args[0];
        if (arenaId == null || arenaId.isBlank()) {
            sender.sendMessage(ChatColor.YELLOW + getUsage());
            return true;
        }
        if (createNew) {
            final String safeId = arenaId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            final File file = new File(plugin.getDataFolder(), "arenas/" + safeId + ".yml");
            if (file.exists()) {
                sender.sendMessage(ChatColor.RED + "An arena file already exists for '" + safeId + "'. Use /bedwars setup " + safeId + " to edit it.");
                return true;
            }
        }
        try {
            plugin.getSetupWizardService().startOrResume(player, arenaId, createNew);
        } catch (final Exception ex) {
            sender.sendMessage(ChatColor.RED + "Could not start setup: " + ex.getMessage());
        }
        return true;
    }
}
