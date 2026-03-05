package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Allows players to punch chests / ender chests to quickly deposit the item in their hand.
 * Tools (axes/pickaxes/shears) cannot be deposited. Swords can only be deposited if at
 * least one sword remains afterward (same rule as dropping).
 */
public final class ChestDepositListener implements Listener {

    private final Bedwars plugin;

    public ChestDepositListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPunchChest(final PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        final Material type = block.getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST && type != Material.ENDER_CHEST) {
            return;
        }

        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }
        if (arena.getGameState() != EGameState.IN_GAME && arena.getGameState() != EGameState.ENDING) {
            return;
        }

        final ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType().isAir()) {
            return;
        }

        final Material handType = inHand.getType();
        // Disallow tools except swords (handled specially).
        if (isNonSwordTool(handType)) {
            return;
        }

        // Swords can only be deposited if at least one remains after.
        if (SwordAndArmorEnforcementListener.isSword(handType)) {
            final int swords = SwordAndArmorEnforcementListener.countSwords(player.getInventory());
            if (swords - inHand.getAmount() < 1) {
                return;
            }
        }

        final Inventory target;
        if (type == Material.ENDER_CHEST) {
            target = player.getEnderChest();
        } else {
            if (!(block.getState() instanceof Chest chest)) {
                return;
            }
            target = chest.getBlockInventory();
        }

        // Deposit a clone of the stack in hand into the chest.
        final ItemStack toDeposit = inHand.clone();
        final HashMap<Integer, ItemStack> leftover = target.addItem(toDeposit);
        if (leftover.isEmpty()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            // There should only be at most one remainder stack.
            final ItemStack remaining = leftover.values().iterator().next();
            player.getInventory().setItemInMainHand(remaining);
        }
        player.updateInventory();
        event.setCancelled(true);

        // Ensure the deposit hologram is present above this chest.
        plugin.getHologramManager().ensureDepositHologram(block.getLocation());
    }

    private boolean isNonSwordTool(final Material type) {
        if (SwordAndArmorEnforcementListener.isSword(type)) {
            return false;
        }
        final String name = type.name();
        return name.endsWith("_AXE") || name.endsWith("_PICKAXE") || type == Material.SHEARS;
    }
}

