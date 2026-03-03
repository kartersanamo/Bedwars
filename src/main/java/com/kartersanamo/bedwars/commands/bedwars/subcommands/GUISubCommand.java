package com.kartersanamo.bedwars.commands.bedwars.subcommands;

import com.kartersanamo.bedwars.api.command.ASubCommand;
import com.kartersanamo.bedwars.gui.GameModeGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GUISubCommand extends ASubCommand {

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "Open the Bedwars gamemode selector.";
    }

    @Override
    public String getUsage() {
        return "/bedwars gui";
    }

    @Override
    public String getPermission() {
        return "bedwars.join";
    }

    @Override
    public boolean execute(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        GameModeGui.openFor(player);
        return true;
    }
}