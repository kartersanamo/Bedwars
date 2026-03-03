package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GameModeGui {

    public static final String TITLE = ChatColor.DARK_GREEN + "Bedwars Gamemodes";

    private GameModeGui() {
    }

    public static void openFor(final Player player) {
        final Bedwars plugin = Bedwars.getInstance();

        final Inventory inventory = Bukkit.createInventory(player, 27, TITLE);

        addModeItem(inventory, 10, EGameMode.SOLO, Material.IRON_SWORD, plugin);
        addModeItem(inventory, 12, EGameMode.DOUBLES, Material.GOLDEN_SWORD, plugin);
        addModeItem(inventory, 14, EGameMode.THREES, Material.DIAMOND_SWORD, plugin);
        addModeItem(inventory, 16, EGameMode.FOURS, Material.NETHERITE_SWORD, plugin);

        player.openInventory(inventory);
    }

    private static void addModeItem(final Inventory inventory,
                                    final int slot,
                                    final EGameMode mode,
                                    final Material material,
                                    final Bedwars plugin) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.GREEN + mode.getDisplayName());

        int activeGames = 0;
        int waitingLobbies = 0;
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getTeamSize() != mode.getTeamSize()) {
                continue;
            }
            if (arena.getGameState() == EGameState.IN_GAME) {
                activeGames++;
            } else if ((arena.getGameState() == EGameState.LOBBY_WAITING
                    || arena.getGameState() == EGameState.STARTING)
                    && !arena.getPlayers().isEmpty()) {
                waitingLobbies++;
            }
        }

        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Fast-paced " + mode.getDisplayName() + " Bedwars matches.");
        lore.add(ChatColor.GRAY + "Fight, bridge and clutch your way to victory.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Active Games: " + ChatColor.GREEN + activeGames);
        lore.add(ChatColor.YELLOW + "Waiting Lobbies: " + ChatColor.GREEN + waitingLobbies);
        lore.add("");
        lore.add(ChatColor.GREEN + "Left-Click to Play " + mode.getDisplayName() + "!");
        lore.add(ChatColor.AQUA + "Right-Click to Select a Map!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }
}

