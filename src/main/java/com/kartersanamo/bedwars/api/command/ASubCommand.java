package com.kartersanamo.bedwars.api.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Base class for sub-commands under the main /bedwars command.
 */
public abstract class ASubCommand {

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract String getPermission();

    public boolean canExecute(final CommandSender sender) {
        final String permission = getPermission();
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    public abstract boolean execute(CommandSender sender, String[] args);

    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        return Collections.emptyList();
    }
}
