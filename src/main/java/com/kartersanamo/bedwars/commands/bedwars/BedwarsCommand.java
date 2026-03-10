package com.kartersanamo.bedwars.commands.bedwars;

import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.api.command.IParentCommand;
import com.kartersanamo.bedwars.commands.bedwars.subcommands.*;
import com.kartersanamo.bedwars.gui.GameModeGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class BedwarsCommand implements IParentCommand {

    private final Map<String, ASubCommand> subCommands = new HashMap<>();

    public BedwarsCommand() {
        registerSubCommand(new JoinSubCommand());
        registerSubCommand(new LeaveSubCommand());
        registerSubCommand(new StartSubCommand());
        registerSubCommand(new ForceStartSubCommand());
        registerSubCommand(new StopSubCommand());
        registerSubCommand(new ListSubCommand());
        registerSubCommand(new GUISubCommand());
        registerSubCommand(new CancelSubCommand());
        registerSubCommand(new NPCSubCommand());
    }

    private void registerSubCommand(final ASubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            if (sender instanceof org.bukkit.entity.Player player) {
                GameModeGui.openFor(player);
            } else {
                sender.sendMessage("Usage: /" + label + " <join|leave|start|forcestart|stop|cancel|list|gui|npc>");
            }
            return true;
        }

        final ASubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.sendMessage("Unknown subcommand. Use /" + label + " <join|leave|start|forcestart|stop|cancel|list|gui|npc>");
            return true;
        }

        if (!sub.canExecute(sender)) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            final List<String> result = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(prefix)) {
                    result.add(name);
                }
            }
            return result;
        }

        final ASubCommand sub = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            return Collections.emptyList();
        }
        final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.tabComplete(sender, subArgs);
    }
}