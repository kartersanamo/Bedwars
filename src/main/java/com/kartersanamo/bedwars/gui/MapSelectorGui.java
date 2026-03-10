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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting a specific map for a given game mode.
 */
public final class MapSelectorGui {

    private static final String TITLE_FORMAT = "Bed Wars";

    private MapSelectorGui() {
    }

    /**
     * Opens the Map Selector GUI for a specific game mode.
     */
    public static void openFor(final Player player, final EGameMode gameMode) {
        final Bedwars plugin = Bedwars.getInstance();

        final MapSelectorGuiHolder holder = new MapSelectorGuiHolder(gameMode);
        final Inventory inventory = Bukkit.createInventory(holder, 54,
                TITLE_FORMAT + " " + gameMode.getDisplayName());
        holder.setInventory(inventory);

        final Map<String, List<IArena>> arenasByName = new LinkedHashMap<>();
        for (IArena arena : getCandidateArenas(plugin, gameMode)) {
            arenasByName.computeIfAbsent(arena.getDisplayName(), k -> new ArrayList<>()).add(arena);
        }

        final List<Integer> mapSlots = collectMapSlots();
        int index = 0;
        for (Map.Entry<String, List<IArena>> entry : arenasByName.entrySet()) {
            if (index >= mapSlots.size()) {
                break;
            }
            addMapItem(inventory, mapSlots.get(index), entry.getKey(), gameMode, entry.getValue());
            index++;
        }

        addBackArrowItem(inventory);

        player.openInventory(inventory);
    }

    private static List<IArena> getCandidateArenas(final Bedwars plugin, final EGameMode gameMode) {
        final List<IArena> result = new ArrayList<>();
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (!arena.isEnabled()) {
                continue;
            }
            if (arena.getTeamSize() != gameMode.getTeamSize()) {
                continue;
            }
            result.add(arena);
        }
        return result;
    }

    private static List<Integer> collectMapSlots() {
        final List<Integer> result = new ArrayList<>();
        for (int row = 1; row <= 4; row++) { // rows 2..5 in a 6-row inventory
            for (int col = 1; col <= 7; col++) { // cols 2..8
                result.add((row * 9) + col);
            }
        }
        return result;
    }

    /**
     * Adds a map item to the inventory.
     */
    private static void addMapItem(final Inventory inventory,
                                   final int slot,
                                   final String mapName,
                                   final EGameMode gameMode,
                                   final List<IArena> arenas) {
        final ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.GREEN + mapName);

        int currentGames = 0;
        int openLobbies = 0;
        int timesJoined = 0;

        for (IArena arena : arenas) {
            if (arena.getGameState() == EGameState.IN_GAME) {
                currentGames++;
            } else if ((arena.getGameState() == EGameState.LOBBY_WAITING || arena.getGameState() == EGameState.STARTING)
                    && !arena.getPlayers().isEmpty()) {
                openLobbies++;
            }
        }

        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + gameMode.getDisplayName());
        lore.add("");
        lore.add(ChatColor.GRAY + "Current Games: " + ChatColor.GREEN + currentGames);
        lore.add(ChatColor.GRAY + "Open Lobbies: " + ChatColor.GREEN + openLobbies);
        lore.add(ChatColor.GRAY + "Times Joined: " + ChatColor.GREEN + timesJoined);
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to Play!");

        meta.setLore(lore);
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }

    /**
     * Adds the back arrow item to the inventory.
     */
    private static void addBackArrowItem(final Inventory inventory) {
        final ItemStack item = new ItemStack(Material.ARROW);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName(ChatColor.GREEN + "Go Back");
        meta.setLore(List.of(ChatColor.GRAY + "To Play Bed Wars"));
        item.setItemMeta(meta);

        inventory.setItem(49, item);
    }
}