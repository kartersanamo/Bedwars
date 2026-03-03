package com.kartersanamo.bedwars.shop.main;

import com.kartersanamo.bedwars.api.arena.shop.IBuyItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Minimal implementation of a purchasable shop item.
 */
public final class BuyItem implements IBuyItem {

    private final ItemStack displayItem;
    private final int cost;
    private final ItemStack currency;
    private final ItemStack reward;

    public BuyItem(final ItemStack displayItem, final int cost, final Material currencyType, final ItemStack reward) {
        this.displayItem = displayItem;
        this.cost = cost;
        this.currency = new ItemStack(currencyType);
        this.reward = reward;
    }

    @Override
    public ItemStack getDisplayItem() {
        return displayItem.clone();
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public ItemStack getCurrencyItem() {
        return currency.clone();
    }

    @Override
    public boolean canPurchase(final Player player) {
        return getCurrencyAmount(player) >= cost;
    }

    @Override
    public void purchase(final Player player) {
        if (!canPurchase(player)) {
            return;
        }

        removeCurrency(player, cost);
        player.getInventory().addItem(reward.clone());
    }

    private int getCurrencyAmount(final Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency.getType()) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeCurrency(final Player player, final int amount) {
        int remaining = amount;
        final PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            final ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() != currency.getType()) {
                continue;
            }
            if (stack.getAmount() > remaining) {
                stack.setAmount(stack.getAmount() - remaining);
                inv.setItem(i, stack);
                return;
            } else {
                remaining -= stack.getAmount();
                inv.clear(i);
                if (remaining <= 0) {
                    return;
                }
            }
        }
    }
}
