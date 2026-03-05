package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Enforces: players always have at least one sword; cannot drop or stash the last sword;
 * permanent armor cannot be dropped or taken off.
 */
public final class SwordAndArmorEnforcementListener implements Listener {

    private static final Material[] SWORDS = {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD
    };

    private final Bedwars plugin;

    public SwordAndArmorEnforcementListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    private static boolean isSword(final Material type) {
        for (Material m : SWORDS) {
            if (m == type) return true;
        }
        return false;
    }

    private static boolean isPermanentArmor(final ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        String n = stack.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    private static int countSwords(final Inventory inv) {
        int count = 0;
        for (ItemStack s : inv.getContents()) {
            if (s != null && isSword(s.getType())) count += s.getAmount();
        }
        return count;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDrop(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) return;
        if (arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.IN_GAME
                && arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.ENDING) {
            return;
        }

        final ItemStack dropped = event.getItemDrop().getItemStack();
        if (isPermanentArmor(dropped)) {
            event.setCancelled(true);
            return;
        }
        if (isSword(dropped.getType())) {
            final int after = countSwords(player.getInventory()) - dropped.getAmount();
            if (after < 1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) return;
        if (arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.IN_GAME
                && arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.ENDING) {
            return;
        }

        // Don't interfere with shop or upgrades GUI
        final String title = event.getView().getTitle();
        if (title != null && (title.startsWith("Item Shop") || title.startsWith("Upgrades & Traps"))) return;

        final int rawSlot = event.getRawSlot();
        final Inventory top = event.getView().getTopInventory();
        final ItemStack current = event.getCurrentItem();
        final ItemStack cursor = event.getCursor();
        final boolean isArmorSlot = event.getView().getSlotType(rawSlot) == InventoryType.SlotType.ARMOR;

        // Prevent any move that would take permanent armor off (works for any inventory view, e.g. E menu or chest)
        if (current != null && isPermanentArmor(current) && (isArmorSlot || isArmorSlotOf(current, player.getInventory()))) {
            event.setCancelled(true);
            return;
        }
        if (cursor != null && isPermanentArmor(cursor) && !isArmorSlot) {
            event.setCancelled(true);
            return;
        }

        // Prevent putting last sword or armor into top inventory (chest, barrel, etc.)
        if (top.getSize() >= 27) {
            final ItemStack moved = event.isShiftClick() ? current : cursor;
            if (moved != null && isSword(moved.getType())) {
                int swordsInPlayer = countSwords(player.getInventory());
                if (event.isShiftClick()) {
                    swordsInPlayer -= moved.getAmount();
                }
                if (swordsInPlayer < 1) {
                    event.setCancelled(true);
                }
            }
            if (moved != null && isPermanentArmor(moved)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) return;
        if (arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.IN_GAME
                && arena.getGameState() != com.kartersanamo.bedwars.api.arena.EGameState.ENDING) {
            return;
        }
        final String title = event.getView().getTitle();
        if (title != null && (title.startsWith("Item Shop") || title.startsWith("Upgrades & Traps"))) return;

        for (int rawSlot : event.getRawSlots()) {
            final ItemStack item = event.getView().getItem(rawSlot);
            if (item != null && isPermanentArmor(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static boolean isArmorSlotOf(final ItemStack armor, final org.bukkit.inventory.PlayerInventory inv) {
        if (armor == null) return false;
        return inv.getHelmet() == armor || inv.getChestplate() == armor
                || inv.getLeggings() == armor || inv.getBoots() == armor;
    }
}
