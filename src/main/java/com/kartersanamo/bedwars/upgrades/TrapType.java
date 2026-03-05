package com.kartersanamo.bedwars.upgrades;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Trap types purchasable from the Upgrades & Traps GUI.
 * Cost scales: 1st = 1 diamond, 2nd = 2, 3rd = 4. Max 3 queued.
 */
public enum TrapType {

    ITS_A_TRAP("It's A Trap!", "Blindness and Slowness for 8 seconds.", Material.IRON_SHOVEL),
    COUNTER_OFFENSIVE("Counter-Offensive Trap", "Gives you and nearby teammates Speed I and Jump Boost II for 10 seconds.", Material.FEATHER),
    ALARM("Alarm Trap", "Reveals invisible players and plays a sound.", Material.REDSTONE_TORCH),
    MINER_FATIGUE("Miner Fatigue Trap", "Inflict Mining Fatigue for 10 seconds.", Material.GOLDEN_PICKAXE);

    private final String displayName;
    private final String description;
    private final Material icon;

    TrapType(final String displayName, final String description, final Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public ItemStack toItemStack() {
        final ItemStack stack = new ItemStack(icon);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(List.of(description, "", "Cost: 1 Diamond (scales by queue)"));
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
