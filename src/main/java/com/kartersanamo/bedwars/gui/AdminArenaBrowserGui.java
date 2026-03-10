package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
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
import java.util.Comparator;
import java.util.List;

/**
 * Admin-only arena browser for /bw list.
 */
public final class AdminArenaBrowserGui {

    public static final String TITLE = "Bed Wars Admin Browser";
    private static final List<Integer> ARENA_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );

    private AdminArenaBrowserGui() {
    }

    public static void openFor(final Player player, final int page) {
        final Bedwars plugin = Bedwars.getInstance();
        final List<IArena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        arenas.sort(Comparator.comparing(IArena::getDisplayName).thenComparing(IArena::getId));

        final int safePage = Math.max(0, page);
        final int perPage = ARENA_SLOTS.size();
        final int totalPages = Math.max(1, (int) Math.ceil(arenas.size() / (double) perPage));
        final int clampedPage = Math.min(safePage, totalPages - 1);

        final AdminArenaBrowserHolder holder = new AdminArenaBrowserHolder(clampedPage);
        final Inventory inventory = Bukkit.createInventory(holder, 54,
                TITLE + ChatColor.GRAY + " [" + (clampedPage + 1) + "/" + totalPages + "]");
        holder.setInventory(inventory);

        paintFrame(inventory);
        placeSummaryItems(inventory, arenas);

        final int start = clampedPage * perPage;
        final int end = Math.min(start + perPage, arenas.size());
        for (int i = start; i < end; i++) {
            final IArena arena = arenas.get(i);
            final int slot = ARENA_SLOTS.get(i - start);
            inventory.setItem(slot, createArenaItem(arena));
        }

        inventory.setItem(45, createButton(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                ChatColor.GRAY + "Go to page " + Math.max(1, clampedPage)));
        inventory.setItem(53, createButton(Material.ARROW, ChatColor.YELLOW + "Next Page",
                ChatColor.GRAY + "Go to page " + Math.min(totalPages, clampedPage + 2)));
        inventory.setItem(49, createButton(Material.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu"));
        inventory.setItem(48, createButton(Material.CLOCK, ChatColor.AQUA + "Refresh", ChatColor.GRAY + "Reload arena statuses"));

        player.openInventory(inventory);
    }

    private static void paintFrame(final Inventory inventory) {
        final ItemStack filler = createButton(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private static void placeSummaryItems(final Inventory inventory, final List<IArena> arenas) {
        int totalPlayers = 0;
        int activeGames = 0;
        int waitingLobbies = 0;

        for (IArena arena : arenas) {
            totalPlayers += arena.getPlayers().size();
            if (arena.getGameState() == EGameState.IN_GAME) {
                activeGames++;
            }
            if (arena.getGameState() == EGameState.LOBBY_WAITING || arena.getGameState() == EGameState.STARTING) {
                waitingLobbies++;
            }
        }

        inventory.setItem(1, createButton(Material.NETHER_STAR,
                ChatColor.GREEN + "Arenas Loaded: " + arenas.size(),
                ChatColor.GRAY + "All configured mode instances"));
        inventory.setItem(4, createButton(Material.PLAYER_HEAD,
                ChatColor.AQUA + "Players Online In Arenas: " + totalPlayers,
                ChatColor.GRAY + "Across all games and lobbies"));
        inventory.setItem(7, createButton(Material.DIAMOND_SWORD,
                ChatColor.GOLD + "In-Game: " + activeGames,
                ChatColor.GRAY + "Waiting/Starting: " + waitingLobbies));
    }

    private static ItemStack createArenaItem(final IArena arena) {
        final Material icon = switch (arena.getGameState()) {
            case IN_GAME -> Material.LIME_DYE;
            case STARTING -> Material.ORANGE_DYE;
            case LOBBY_WAITING -> Material.YELLOW_DYE;
            case ENDING, RESETTING -> Material.RED_DYE;
            default -> Material.GRAY_DYE;
        };

        final ItemStack item = new ItemStack(icon);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.AQUA + arena.getDisplayName() + ChatColor.DARK_GRAY + " (" + arena.getId() + ")");

        final List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "State: " + colorState(arena.getGameState()) + arena.getGameState().name());
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.GREEN + arena.getPlayers().size() + ChatColor.GRAY + "/" + ChatColor.GREEN + arena.getMaxPlayers());
        lore.add(ChatColor.GRAY + "Mode: " + ChatColor.GREEN + modeName(arena.getTeamSize()));
        lore.add(ChatColor.GRAY + "World: " + ChatColor.GREEN + (arena.getWorld() != null ? arena.getWorld().getName() : "Unknown"));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to print quick details in chat");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ChatColor colorState(final EGameState state) {
        return switch (state) {
            case IN_GAME -> ChatColor.GREEN;
            case STARTING -> ChatColor.GOLD;
            case LOBBY_WAITING -> ChatColor.YELLOW;
            case ENDING, RESETTING -> ChatColor.RED;
            default -> ChatColor.GRAY;
        };
    }

    private static String modeName(final int teamSize) {
        return switch (teamSize) {
            case 1 -> "Solo";
            case 2 -> "Doubles";
            case 3 -> "3s";
            case 4 -> "4s";
            default -> teamSize + "s";
        };
    }

    private static ItemStack createButton(final Material material, final String name, final String... loreLines) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            meta.setLore(List.of(loreLines));
        }
        item.setItemMeta(meta);
        return item;
    }
}