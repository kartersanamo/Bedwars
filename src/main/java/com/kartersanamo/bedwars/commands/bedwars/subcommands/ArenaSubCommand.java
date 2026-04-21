package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Toggle {@code enabled:} on an arena YAML file and reload arenas (so per-mode worlds are created).
 */
public final class ArenaSubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "arena";
    }

    @Override
    public String getDescription() {
        return "Enable or disable an arena in its YAML file, then reload arenas.";
    }

    @Override
    public String getUsage() {
        return "/bedwars arena enable <arenaId> | /bedwars arena disable <arenaId>";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.reload";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + getUsage());
            return true;
        }
        final String action = args[0].toLowerCase(Locale.ROOT);
        if (!"enable".equals(action) && !"disable".equals(action)) {
            sender.sendMessage(ChatColor.YELLOW + "Use enable or disable. " + getUsage());
            return true;
        }
        final String rawId = args[1];
        if (rawId == null || rawId.isBlank()) {
            sender.sendMessage(ChatColor.YELLOW + getUsage());
            return true;
        }
        final String safeId = rawId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        final Bedwars plugin = Bedwars.getInstance();
        final File file = new File(plugin.getDataFolder(), "arenas/" + safeId + ".yml");
        if (!file.isFile()) {
            sender.sendMessage(ChatColor.RED + "No arena file: plugins/Bedwars/arenas/" + safeId + ".yml");
            return true;
        }
        final boolean enable = "enable".equals(action);
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set(ConfigPath.Arena.ENABLED, enable);
        try {
            yaml.save(file);
        } catch (final IOException ex) {
            sender.sendMessage(ChatColor.RED + "Could not save arena file: " + ex.getMessage());
            return true;
        }
        plugin.reloadArenas();
        sender.sendMessage(ChatColor.GREEN + "Arena '" + safeId + "' set to enabled=" + enable + " and arenas reloaded.");
        return true;
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            final String p = args[0].toLowerCase(Locale.ROOT);
            final List<String> out = new ArrayList<>();
            if ("enable".startsWith(p)) {
                out.add("enable");
            }
            if ("disable".startsWith(p)) {
                out.add("disable");
            }
            return out;
        }
        if (args.length == 2) {
            final Bedwars plugin = Bedwars.getInstance();
            final File dir = new File(plugin.getDataFolder(), "arenas");
            final File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files == null) {
                return Collections.emptyList();
            }
            final String p = args[1].toLowerCase(Locale.ROOT);
            final List<String> out = new ArrayList<>();
            for (final File f : files) {
                final String id = f.getName().substring(0, f.getName().length() - ".yml".length());
                if (id.toLowerCase(Locale.ROOT).startsWith(p)) {
                    out.add(id);
                }
            }
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }
}
