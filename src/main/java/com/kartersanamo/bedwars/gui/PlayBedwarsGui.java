package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
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

/**
 * GUI for selecting a game mode or browsing maps when clicking an NPC.
 */
public final class PlayBedwarsGui {

    private static final String TITLE = "Play Bed Wars";

    private PlayBedwarsGui() {
    }

    /**
     * Opens the Play Bedwars GUI for a specific game mode.
     */
    public static void openFor(final Player player, final EGameMode gameMode) {
        final Bedwars plugin = Bedwars.getInstance();

        final PlayBedwarsGuiHolder holder = new PlayBedwarsGuiHolder(gameMode);
        final Inventory inventory = Bukkit.createInventory(holder, 36, TITLE);
        holder.setInventory(inventory);

        // Bed Wars item at row 2, column 3
        addBedWarsItem(inventory, gameMode, plugin);

        // Map Selector item at row 2, column 5
        addMapSelectorItem(inventory, gameMode);

        // Close item at row 4, column 5
        addCloseItem(inventory);

        player.openInventory(inventory);
    }

    /**
     * Adds the Bed Wars item to the inventory.
     */
    private static void addBedWarsItem(final Inventory inventory,
                                       final EGameMode gameMode,
                                       final Bedwars plugin) {
        final ItemStack item = new ItemStack(Material.RED_BED);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.GREEN + "Bed Wars (" + gameMode.getDisplayName() + ")");

        int configuredMaxPlayers = gameMode.getTeamSize() * 4;
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getTeamSize() == gameMode.getTeamSize()) {
                configuredMaxPlayers = arena.getMaxPlayers();
                break;
            }
        }

        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Play a game of Bed Wars " + gameMode.getDisplayName() + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "Fight against " + Math.max(0, configuredMaxPlayers - 1) + " other players!");
        lore.add(ChatColor.GRAY + "Destroy enemy beds to stop them");
        lore.add(ChatColor.GRAY + "from respawning!");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to play!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(12, item);
    }

    /**
     * Adds the Map Selector item to the inventory.
     */
    private static void addMapSelectorItem(final Inventory inventory,
                                           final EGameMode gameMode) {
        final ItemStack item = new ItemStack(Material.OAK_SIGN);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.GREEN + "Map Selector (" + gameMode.getDisplayName() + ")");

        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Pick which map you want to play from");
        lore.add(ChatColor.GRAY + "a list of available games.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to browse!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(14, item);
    }

    /**
     * Adds the Close item to the inventory.
     */
    private static void addCloseItem(final Inventory inventory) {
        final ItemStack item = new ItemStack(Material.BARRIER);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.RED + "Close");

        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);

        inventory.setItem(31, item);
    }
}