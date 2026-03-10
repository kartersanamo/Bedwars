package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Handles admin arena browser navigation.
 */
public final class AdminArenaBrowserListener implements Listener {

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof AdminArenaBrowserHolder holder)) {
            return;
        }

        event.setCancelled(true);

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        final int page = holder.getPage();

        if (event.getRawSlot() == 45) {
            AdminArenaBrowserGui.openFor(player, Math.max(0, page - 1));
            return;
        }
        if (event.getRawSlot() == 53) {
            AdminArenaBrowserGui.openFor(player, page + 1);
            return;
        }
        if (event.getRawSlot() == 48) {
            AdminArenaBrowserGui.openFor(player, page);
            return;
        }
        if (event.getRawSlot() == 49) {
            player.closeInventory();
            return;
        }

        // Arena entries: print a quick detail line in chat.
        final String strippedName = clicked.hasItemMeta() && Objects.requireNonNull(clicked.getItemMeta()).hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName())
                : null;
        if (strippedName == null) {
            return;
        }

        final Bedwars plugin = Bedwars.getInstance();
        final List<IArena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        arenas.sort(Comparator.comparing(IArena::getDisplayName).thenComparing(IArena::getId));

        for (IArena arena : arenas) {
            final String key = arena.getDisplayName() + " (" + arena.getId() + ")";
            if (!strippedName.equalsIgnoreCase(key)) {
                continue;
            }
            player.sendMessage(ChatColor.AQUA + "[Arena] " + ChatColor.WHITE + arena.getId()
                    + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + arena.getDisplayName()
                    + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + arena.getGameState().name()
                    + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + arena.getPlayers().size()
                    + ChatColor.GRAY + "/" + ChatColor.GREEN + arena.getMaxPlayers());
            break;
        }
    }
}

