package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.api.command.ASubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * /bw npc parent subcommand.
 */
public final class NPCSubCommand extends ASubCommand {

    private final Map<String, ASubCommand> npcSubCommands = new HashMap<>();

    public NPCSubCommand() {
        register(new NPCSpawnSubCommand());
        register(new NPCDeleteSubCommand());
        register(new NPCListSubCommand());
        register(new NPCRemoveSubCommand());
    }

    private void register(final ASubCommand subCommand) {
        npcSubCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
    }

    @Override
    public String getName() {
        return "npc";
    }

    @Override
    public String getDescription() {
        return "NPC management under /bw.";
    }

    @Override
    public String getUsage() {
        return "/bw npc <spawn|delete|list|remove>";
    }

    @Override
    public String getPermission() {
        return "bedwars.admin.npc";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + getUsage());
            return true;
        }

        final ASubCommand sub = npcSubCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            sender.sendMessage(ChatColor.RED + "Unknown npc subcommand. Use /bw npc <spawn|delete|list|remove>");
            return true;
        }

        if (!sub.canExecute(sender)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        final String[] delegatedArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, delegatedArgs);
    }

    @Override
    public List<String> tabComplete(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            final List<String> result = new ArrayList<>();
            for (String name : npcSubCommands.keySet()) {
                if (name.startsWith(prefix)) {
                    result.add(name);
                }
            }
            return result;
        }

        final ASubCommand sub = npcSubCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            return List.of();
        }

        final String[] delegatedArgs = Arrays.copyOfRange(args, 1, args.length);
        return sub.tabComplete(sender, delegatedArgs);
    }
}