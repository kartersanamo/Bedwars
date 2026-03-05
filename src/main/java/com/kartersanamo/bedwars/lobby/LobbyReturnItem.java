package com.kartersanamo.bedwars.lobby;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

/**
 * Utility for the \"Return to Lobby\" bed item shown in the waiting lobby.
 */
public final class LobbyReturnItem {

    private static final String DISPLAY_NAME = ChatColor.RED + "Return to Lobby " + ChatColor.GRAY + "(Right Click)";
    private static final String LORE_LINE = ChatColor.GRAY + "Right-click to leave to the lobby!";

    private LobbyReturnItem() {
    }

    public static ItemStack create() {
        final ItemStack stack = new ItemStack(Material.RED_BED);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DISPLAY_NAME);
            meta.setLore(Collections.singletonList(LORE_LINE));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static boolean isLobbyReturnItem(final ItemStack stack) {
        if (stack == null || stack.getType() != Material.RED_BED) {
            return false;
        }
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        return DISPLAY_NAME.equals(meta.getDisplayName());
    }

    /**
     * Gives the lobby return item to the player's hotbar (slot 8) if not already present.
     */
    public static void giveTo(final Player player) {
        final PlayerInventory inv = player.getInventory();
        if (contains(inv)) {
            return;
        }
        inv.setItem(8, create());
    }

    public static void removeFrom(final Player player) {
        final PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            final ItemStack current = inv.getItem(i);
            if (isLobbyReturnItem(current)) {
                inv.clear(i);
            }
        }
    }

    private static boolean contains(final PlayerInventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (isLobbyReturnItem(item)) {
                return true;
            }
        }
        return false;
    }
}

