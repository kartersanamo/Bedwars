package com.kartersanamo.bedwars.upgrades;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
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
 * Opens and fills the "Upgrades & Traps" GUI. Costs follow Hypixel: 1s/2s vs 3s/4s.
 * Layout: slots 0-5 = 6 upgrades, 6-9 = 4 traps, 18-20 = 3 active trap slots (glass).
 */
public final class UpgradeManager {

    private static final String TITLE = "Upgrades & Traps";
    private static final int SIZE = 27;

    private static final int SLOT_SHARPNESS = 0;
    private static final int SLOT_PROTECTION = 1;
    private static final int SLOT_HASTE = 2;
    private static final int SLOT_FORGE = 3;
    private static final int SLOT_HEAL_POOL = 4;
    private static final int SLOT_DRAGON_BUFF = 5;
    private static final int SLOT_TRAP_1 = 6;
    private static final int SLOT_TRAP_2 = 7;
    private static final int SLOT_TRAP_3 = 8;
    private static final int SLOT_TRAP_4 = 9;
    private static final int SLOT_ACTIVE_TRAP_1 = 18;
    private static final int SLOT_ACTIVE_TRAP_2 = 19;
    private static final int SLOT_ACTIVE_TRAP_3 = 20;

    public void openGui(final Player player, final Arena arena, final ITeam team) {
        final TeamUpgradeState state = arena.getTeamUpgradeState(team);
        if (state == null) {
            return;
        }
        final UpgradesGuiHolderImpl holder = new UpgradesGuiHolderImpl(arena, team);
        final Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inv);

        final boolean smallMode = arena.getTeamSize() <= 2;

        // Upgrades (slots 0-5)
        inv.setItem(SLOT_SHARPNESS, iconSharpness(state, smallMode));
        inv.setItem(SLOT_PROTECTION, iconProtection(state, smallMode));
        inv.setItem(SLOT_HASTE, iconHaste(state, smallMode));
        inv.setItem(SLOT_FORGE, iconForge(state, smallMode));
        inv.setItem(SLOT_HEAL_POOL, iconHealPool(state, smallMode));
        inv.setItem(SLOT_DRAGON_BUFF, iconDragonBuff(state));

        // Traps (slots 6-9) - 4 trap types
        inv.setItem(SLOT_TRAP_1, iconTrap(TrapType.ITS_A_TRAP, state));
        inv.setItem(SLOT_TRAP_2, iconTrap(TrapType.COUNTER_OFFENSIVE, state));
        inv.setItem(SLOT_TRAP_3, iconTrap(TrapType.ALARM, state));
        inv.setItem(SLOT_TRAP_4, iconTrap(TrapType.MINER_FATIGUE, state));

        // Active traps (slots 18-20)
        final List<TrapType> queue = state.getTrapQueue();
        inv.setItem(SLOT_ACTIVE_TRAP_1, glassForTrap(queue.size() > 0 ? queue.get(0) : null, 1));
        inv.setItem(SLOT_ACTIVE_TRAP_2, glassForTrap(queue.size() > 1 ? queue.get(1) : null, 2));
        inv.setItem(SLOT_ACTIVE_TRAP_3, glassForTrap(queue.size() > 2 ? queue.get(2) : null, 3));

        // Fill empty slots with barrier or gray glass so only our slots are clickable
        final ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
        }
        filler.setItemMeta(fillerMeta);
        for (int i = 10; i <= 17; i++) {
            inv.setItem(i, filler);
        }
        for (int i = 21; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    private static ItemStack iconSharpness(final TeamUpgradeState state, final boolean smallMode) {
        final ItemStack stack = new ItemStack(Material.IRON_SWORD);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Sharpened Swords");
            final int cost = smallMode ? 4 : 8;
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Your team permanently gains Sharpness I on swords!");
            lore.add("");
            lore.add(ChatColor.WHITE + "Tier 1: Sharpness I - " + ChatColor.AQUA + cost + " Diamonds");
            if (state.hasSharpness()) {
                lore.add(ChatColor.GREEN + "Already purchased!");
            } else {
                lore.add(ChatColor.AQUA + "Cost: " + cost + " Diamonds");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconProtection(final TeamUpgradeState state, final boolean smallMode) {
        final ItemStack stack = new ItemStack(Material.IRON_CHESTPLATE);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Reinforced Armor");
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Your team permanently gains Protection on armor!");
            lore.add("");
            final int[] costs = smallMode ? new int[]{2, 4, 8, 16} : new int[]{5, 10, 20, 30};
            for (int i = 0; i < 4; i++) {
                final int tier = i + 1;
                lore.add(ChatColor.WHITE + "Tier " + tier + ": Protection " + tier + " - " + ChatColor.AQUA + costs[i] + " Diamonds");
            }
            if (state.getProtection() >= 4) {
                lore.add(ChatColor.GREEN + "Maxed!");
            } else {
                final int nextCost = costs[state.getProtection()];
                lore.add(ChatColor.AQUA + "Next: " + nextCost + " Diamonds");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconHaste(final TeamUpgradeState state, final boolean smallMode) {
        final ItemStack stack = new ItemStack(Material.GOLDEN_PICKAXE);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Maniac Miner");
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Your team permanently gains Haste!");
            lore.add("");
            final int c1 = smallMode ? 2 : 4;
            final int c2 = smallMode ? 4 : 6;
            lore.add(ChatColor.WHITE + "Tier 1: Haste I - " + ChatColor.AQUA + c1 + " Diamonds");
            lore.add(ChatColor.WHITE + "Tier 2: Haste II - " + ChatColor.AQUA + c2 + " Diamonds");
            if (state.getHaste() >= 2) {
                lore.add(ChatColor.GREEN + "Maxed!");
            } else {
                final int nextCost = state.getHaste() == 0 ? c1 : c2;
                lore.add(ChatColor.AQUA + "Next: " + nextCost + " Diamonds");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconForge(final TeamUpgradeState state, final boolean smallMode) {
        final ItemStack stack = new ItemStack(Material.FURNACE);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Forge");
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Upgrade your generator speed!");
            lore.add("");
            final int[] costs = smallMode ? new int[]{2, 4, 6, 8} : new int[]{4, 8, 12, 16};
            lore.add(ChatColor.WHITE + "Iron Forge: +50% - " + ChatColor.AQUA + costs[0] + " Diamonds");
            lore.add(ChatColor.WHITE + "Golden Forge: +100% - " + ChatColor.AQUA + costs[1] + " Diamonds");
            lore.add(ChatColor.WHITE + "Emerald Forge: +emeralds - " + ChatColor.AQUA + costs[2] + " Diamonds");
            lore.add(ChatColor.WHITE + "Molten Forge: +200% - " + ChatColor.AQUA + costs[3] + " Diamonds");
            if (state.getForge() >= 4) {
                lore.add(ChatColor.GREEN + "Maxed!");
            } else {
                lore.add(ChatColor.AQUA + "Next: " + costs[state.getForge()] + " Diamonds");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconHealPool(final TeamUpgradeState state, final boolean smallMode) {
        final ItemStack stack = new ItemStack(Material.BEACON);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Heal Pool");
            final int cost = smallMode ? 1 : 3;
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Creates a regeneration field at your base!");
            lore.add(ChatColor.WHITE + "You and teammates gain Regeneration I at base.");
            lore.add("");
            lore.add(ChatColor.WHITE + "Cost: " + ChatColor.AQUA + cost + " Diamonds");
            if (state.hasHealPool()) {
                lore.add(ChatColor.GREEN + "Already purchased!");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconDragonBuff(final TeamUpgradeState state) {
        final ItemStack stack = new ItemStack(Material.ENCHANTING_TABLE);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Dragon Buff");
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Spawns an extra dragon for your team in sudden death.");
            lore.add("");
            lore.add(ChatColor.WHITE + "Cost: " + ChatColor.AQUA + "5 Diamonds");
            if (state.hasDragonBuff()) {
                lore.add(ChatColor.GREEN + "Already purchased!");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack iconTrap(final TrapType trap, final TeamUpgradeState state) {
        final ItemStack stack = new ItemStack(trap.getIcon());
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + trap.getDisplayName());
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + trap.getDescription());
            lore.add("");
            lore.add(ChatColor.WHITE + "Purchasing a trap will queue it. Cost scales by queue size.");
            final int cost = state.getTrapCostForNextPurchase();
            if (cost < 0) {
                lore.add(ChatColor.RED + "Maximum traps (3) reached!");
            } else {
                lore.add(ChatColor.AQUA + "Next trap: " + cost + " Diamond(s)");
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack glassForTrap(final TrapType trap, final int slotNumber) {
        final ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (trap != null) {
                meta.setDisplayName(ChatColor.GRAY + "Trap #" + slotNumber + ": " + trap.getDisplayName());
                meta.setLore(List.of(
                        ChatColor.WHITE + "The " + ordinal(slotNumber) + " enemy to walk into your base will trigger this trap!",
                        "",
                        ChatColor.WHITE + "Purchasing a trap will queue it here. Cost scales by queue size."
                ));
            } else {
                meta.setDisplayName(ChatColor.RED + "Trap #" + slotNumber + ": No Trap!");
                meta.setLore(List.of(
                        ChatColor.WHITE + "The " + ordinal(slotNumber) + " enemy to walk into your base will trigger this trap!",
                        "",
                        ChatColor.WHITE + "Purchasing a trap will queue it here. Cost scales by queue size.",
                        ChatColor.AQUA + "Next trap: 1 Diamond"
                ));
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static String ordinal(final int n) {
        return switch (n) {
            case 1 -> "first";
            case 2 -> "second";
            case 3 -> "third";
            default -> n + "th";
        };
    }

    /** Cost for Sharpness: 4 (1s/2s) or 8 (3s/4s). */
    public static int getSharpnessCost(final IArena arena) {
        return arena.getTeamSize() <= 2 ? 4 : 8;
    }

    /** Cost for next Protection tier (0->1, 1->2, 2->3, 3->4). */
    public static int getProtectionCost(final int currentTier, final IArena arena) {
        final int[] costs = arena.getTeamSize() <= 2 ? new int[]{2, 4, 8, 16} : new int[]{5, 10, 20, 30};
        return currentTier < 4 ? costs[currentTier] : -1;
    }

    /** Cost for next Haste tier (0->1, 1->2). */
    public static int getHasteCost(final int currentTier, final IArena arena) {
        if (currentTier >= 2) return -1;
        return arena.getTeamSize() <= 2 ? (currentTier == 0 ? 2 : 4) : (currentTier == 0 ? 4 : 6);
    }

    /** Cost for next Forge tier (0->1, 1->2, 2->3, 3->4). */
    public static int getForgeCost(final int currentTier, final IArena arena) {
        final int[] costs = arena.getTeamSize() <= 2 ? new int[]{2, 4, 6, 8} : new int[]{4, 8, 12, 16};
        return currentTier < 4 ? costs[currentTier] : -1;
    }

    /** Heal Pool: 1 (1s/2s) or 3 (3s/4s). */
    public static int getHealPoolCost(final IArena arena) {
        return arena.getTeamSize() <= 2 ? 1 : 3;
    }

    /** Dragon Buff: 5. */
    public static int getDragonBuffCost() {
        return 5;
    }
}
