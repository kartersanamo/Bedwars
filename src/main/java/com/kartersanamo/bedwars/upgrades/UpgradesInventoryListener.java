package com.kartersanamo.bedwars.upgrades;

import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles clicks in the "Upgrades & Traps" GUI: purchase upgrades/traps with diamonds and refresh display.
 */
public final class UpgradesInventoryListener implements Listener {

    private final UpgradeManager upgradeManager;

    public UpgradesInventoryListener(final UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof UpgradesGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final Arena arena = holder.getArena();
        final ITeam team = holder.getTeam();
        final TeamUpgradeState state = arena.getTeamUpgradeState(team);
        if (state == null) {
            return;
        }

        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }

        final int diamonds = countDiamonds(player);

        // First/last rows and columns, divider row, and trap queue row are all non-interactive.
        // Only specific upgrade/trap slots are clickable.
        switch (slot) {
            case UpgradeManager.SLOT_SHARPNESS -> tryBuySharpness(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_PROTECTION -> tryBuyProtection(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_HASTE -> tryBuyHaste(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_FORGE -> tryBuyForge(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_HEAL_POOL -> tryBuyHealPool(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_DRAGON_BUFF -> tryBuyDragonBuff(player, state, diamonds, arena, team);
            case UpgradeManager.SLOT_TRAP_1 ->
                    tryBuyTrap(player, state, diamonds, TrapType.ITS_A_TRAP, arena, team);
            case UpgradeManager.SLOT_TRAP_2 ->
                    tryBuyTrap(player, state, diamonds, TrapType.COUNTER_OFFENSIVE, arena, team);
            case UpgradeManager.SLOT_TRAP_3 ->
                    tryBuyTrap(player, state, diamonds, TrapType.ALARM, arena, team);
            case UpgradeManager.SLOT_TRAP_4 ->
                    tryBuyTrap(player, state, diamonds, TrapType.MINER_FATIGUE, arena, team);
            default -> { }
        }
    }

    private static int countDiamonds(final Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static boolean takeDiamonds(final Player player, final int amount) {
        if (amount <= 0) return true;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            final ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.DIAMOND) {
                final int take = Math.min(remaining, stack.getAmount());
                remaining -= take;
                if (stack.getAmount() <= take) {
                    player.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(stack.getAmount() - take);
                }
            }
        }
        return remaining <= 0;
    }

    private void tryBuySharpness(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        if (state.hasSharpness()) {
            player.sendMessage(ChatColor.RED + "You already have Sharpened Swords!");
            return;
        }
        final int cost = UpgradeManager.getSharpnessCost(arena);
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setSharpness(true);
        player.sendMessage(ChatColor.GREEN + "Purchased Sharpened Swords!");
        refreshGui(player, arena, team);
    }

    private void tryBuyProtection(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        final int current = state.getProtection();
        if (current >= 4) {
            player.sendMessage(ChatColor.RED + "Protection is already maxed!");
            return;
        }
        final int cost = UpgradeManager.getProtectionCost(current, arena);
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setProtection(current + 1);
        player.sendMessage(ChatColor.GREEN + "Purchased Protection " + (current + 1) + "!");
        refreshGui(player, arena, team);
    }

    private void tryBuyHaste(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        final int current = state.getHaste();
        if (current >= 2) {
            player.sendMessage(ChatColor.RED + "Maniac Miner is already maxed!");
            return;
        }
        final int cost = UpgradeManager.getHasteCost(current, arena);
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setHaste(current + 1);
        player.sendMessage(ChatColor.GREEN + "Purchased Haste " + (current + 1) + "!");
        refreshGui(player, arena, team);
    }

    private void tryBuyForge(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        final int current = state.getForge();
        if (current >= 4) {
            player.sendMessage(ChatColor.RED + "Forge is already maxed!");
            return;
        }
        final int cost = UpgradeManager.getForgeCost(current, arena);
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setForge(current + 1);
        player.sendMessage(ChatColor.GREEN + "Purchased Forge upgrade!");
        refreshGui(player, arena, team);
    }

    private void tryBuyHealPool(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        if (state.hasHealPool()) {
            player.sendMessage(ChatColor.RED + "You already have Heal Pool!");
            return;
        }
        final int cost = UpgradeManager.getHealPoolCost(arena);
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setHealPool(true);
        player.sendMessage(ChatColor.GREEN + "Purchased Heal Pool!");
        refreshGui(player, arena, team);
    }

    private void tryBuyDragonBuff(final Player player, final TeamUpgradeState state, final int diamonds, final Arena arena, final ITeam team) {
        if (state.hasDragonBuff()) {
            player.sendMessage(ChatColor.RED + "You already have Dragon Buff!");
            return;
        }
        final int cost = UpgradeManager.getDragonBuffCost();
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.setDragonBuff(true);
        player.sendMessage(ChatColor.GREEN + "Purchased Dragon Buff!");
        refreshGui(player, arena, team);
    }

    private void tryBuyTrap(final Player player, final TeamUpgradeState state, final int diamonds, final TrapType trap, final Arena arena, final ITeam team) {
        final int cost = state.getTrapCostForNextPurchase();
        if (cost < 0) {
            player.sendMessage(ChatColor.RED + "Maximum traps (3) reached!");
            return;
        }
        if (diamonds < cost) {
            player.sendMessage(ChatColor.RED + "You don't have enough Diamonds!");
            return;
        }
        if (!takeDiamonds(player, cost)) {
            return;
        }
        state.addTrap(trap);
        player.sendMessage(ChatColor.GREEN + "Purchased " + trap.getDisplayName() + "!");
        refreshGui(player, arena, team);
    }

    private void refreshGui(final Player player, final Arena arena, final ITeam team) {
        if (team == null) return;
        upgradeManager.openGui(player, arena, team);
    }
}
