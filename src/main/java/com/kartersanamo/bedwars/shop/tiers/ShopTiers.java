package com.kartersanamo.bedwars.shop.tiers;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Factory and implementations for shop tiers (item, wool, armor upgrade, tools, etc.).
 */
public final class ShopTiers {

    private ShopTiers() {}

    public static IContentTier item(final Material material, final int amount, final int cost, final Material currency) {
        return new ItemTier(new ItemStack(material, amount), cost, currency);
    }

    public static IContentTier item(final Material material, final int cost, final Material currency) {
        return item(material, 1, cost, currency);
    }

    public static IContentTier wool(final int amount, final int cost) {
        return new WoolTier(amount, cost);
    }

    public static IContentTier armorUpgrade(final ArmorTier tier, final int cost, final Material currency) {
        return new ArmorUpgradeTier(tier, cost, currency);
    }

    public static IContentTier axeUpgrade(final ToolTier tier, final int cost, final Material currency) {
        return new AxeUpgradeTier(tier, cost, currency);
    }

    public static IContentTier pickaxeUpgrade(final ToolTier tier, final int cost, final Material currency) {
        return new PickaxeUpgradeTier(tier, cost, currency);
    }

    public static IContentTier shears(final int cost) {
        return new ShearsTier(cost);
    }

    public static IContentTier sword(final Material swordType, final int cost, final Material currency) {
        return new SwordUpgradeTier(swordType, cost, currency);
    }

    public static IContentTier powerBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.POWER, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            bow.setItemMeta(meta);
        }
        return new ItemTier(bow, 20, Material.GOLD_INGOT);
    }

    public static IContentTier punchBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PUNCH, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            bow.setItemMeta(meta);
        }
        return new ItemTier(bow, 6, Material.EMERALD);
    }

    public static IContentTier goldenApple() {
        ItemStack gapple = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = gapple.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            gapple.setItemMeta(meta);
        }
        return new ItemTier(gapple, 3, Material.GOLD_INGOT);
    }

    public static IContentTier enderPearl() {
        return item(Material.ENDER_PEARL, 4, Material.EMERALD);
    }

    /** Diamond sword: 3 diamonds in 3's/4's, 4 in solos/doubles. */
    public static IContentTier diamondSword() {
        return new DiamondSwordTier();
    }

    public static IContentTier knockbackStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            stick.setItemMeta(meta);
        }
        return new ItemTier(stick, 5, Material.GOLD_INGOT);
    }

    public static IContentTier tnt() {
        final ItemStack item = new ItemStack(Material.TNT);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.RED + "TNT");
            item.setItemMeta(meta);
        }
        return new ItemTier(item, 4, Material.GOLD_INGOT);
    }

    public static IContentTier speedPotion() {
        final ItemStack item = new ItemStack(Material.POTION);
        final PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.SWIFTNESS);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 45 * 20, 1), true);
            meta.setDisplayName(org.bukkit.ChatColor.WHITE + "Speed II Potion");
            item.setItemMeta(meta);
        }
        return new ItemTier(item, 1, Material.EMERALD);
    }

    public static IContentTier invisibilityPotion() {
        final ItemStack item = new ItemStack(Material.POTION);
        final PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.INVISIBILITY);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30 * 20, 0), true);
            meta.setDisplayName(org.bukkit.ChatColor.WHITE + "Invisibility Potion");
            item.setItemMeta(meta);
        }
        return new ItemTier(item, 2, Material.EMERALD);
    }

    public static IContentTier jumpPotion() {
        final ItemStack item = new ItemStack(Material.POTION);
        final PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.LEAPING);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 45 * 20, 4), true);
            meta.setDisplayName(ChatColor.WHITE + "Jump V Potion");
            item.setItemMeta(meta);
        }
        return new ItemTier(item, 1, Material.EMERALD);
    }

    public static IContentTier bedbug() {
        return namedItem(Material.SILVERFISH_SPAWN_EGG, 1, ChatColor.GREEN + "Bedbug", 40, Material.IRON_INGOT);
    }

    public static IContentTier dreamDefender() {
        return namedItem(Material.IRON_GOLEM_SPAWN_EGG, 1, ChatColor.GREEN + "Dream Defender", 120, Material.IRON_INGOT);
    }

    public static IContentTier fireball() {
        return namedItem(Material.FIRE_CHARGE, 1, ChatColor.GREEN + "Fireball", 40, Material.IRON_INGOT);
    }

    public static IContentTier bridgeEgg() {
        return namedItem(Material.EGG, 1, ChatColor.GREEN + "Bridge Egg", 1, Material.EMERALD);
    }

    public static IContentTier magicMilk() {
        return namedItem(Material.MILK_BUCKET, 1, ChatColor.GREEN + "Magic Milk", 4, Material.GOLD_INGOT);
    }

    public static IContentTier sponge() {
        return namedItem(Material.SPONGE, 4, ChatColor.GREEN + "Sponge", 3, Material.GOLD_INGOT);
    }

    public static IContentTier popupTower() {
        return namedItem(Material.CHEST, 1, ChatColor.GREEN + "Compact Pop-Up Tower", 24, Material.IRON_INGOT);
    }

    private static IContentTier namedItem(final Material material,
                                          final int amount,
                                          final String displayName,
                                          final int cost,
                                          final Material currency) {
        final ItemStack item = new ItemStack(material, amount);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return new ItemTier(item, cost, currency);
    }

    private static ItemStack unbreakable(final ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static final class ItemTier implements IContentTier {
        private final ItemStack reward;
        private final int cost;
        private final Material currency;

        ItemTier(final ItemStack reward, final int cost, final Material currency) {
            this.reward = reward.clone();
            this.cost = cost;
            this.currency = currency;
        }

        @Override
        public ItemStack getItem() {
            return reward.clone();
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return currency;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            player.getInventory().addItem(reward.clone());
        }
    }

    private static final class WoolTier implements IContentTier {
        private final int amount;
        private final int cost;

        WoolTier(final int amount, final int cost) {
            this.amount = amount;
            this.cost = cost;
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.WHITE_WOOL, amount);
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return Material.IRON_INGOT;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            Material wool = Material.WHITE_WOOL;
            if (arena != null) {
                ITeam team = arena.getTeam(player).orElse(null);
                if (team != null) wool = team.getColor().getWoolMaterial();
            }
            player.getInventory().addItem(new ItemStack(wool, amount));
        }
    }

    private static final class ArmorUpgradeTier implements IContentTier {
        private final ArmorTier tier;
        private final int cost;
        private final Material currency;

        ArmorUpgradeTier(final ArmorTier tier, final int cost, final Material currency) {
            this.tier = tier;
            this.cost = cost;
            this.currency = currency;
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(tier.getBoots());
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return currency;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                return a.getPlayerArmorTier(player).ordinal() < tier.ordinal();
            }
            return false;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            if (arena instanceof Arena a && a.setPlayerArmorTier(player, tier)) {
                a.reapplyArmorIfNeeded(player);
            }
        }
    }

    private static final class AxeUpgradeTier implements IContentTier {
        private final ToolTier tier;
        private final int cost;
        private final Material currency;

        AxeUpgradeTier(final ToolTier tier, final int cost, final Material currency) {
            this.tier = tier;
            this.cost = cost;
            this.currency = currency;
        }

        @Override
        public ItemStack getItem() {
            return unbreakable(new ItemStack(tier.getAxeMaterial()));
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return currency;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                // Only allow buying if this tier is strictly higher than current.
                return a.getPlayerAxeTier(player).ordinal() < tier.ordinal();
            }
            return false;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                a.setPlayerAxeTier(player, tier);
                player.getInventory().addItem(unbreakable(new ItemStack(tier.getAxeMaterial())));
            }
        }
    }

    private static final class PickaxeUpgradeTier implements IContentTier {
        private final ToolTier tier;
        private final int cost;
        private final Material currency;

        PickaxeUpgradeTier(final ToolTier tier, final int cost, final Material currency) {
            this.tier = tier;
            this.cost = cost;
            this.currency = currency;
        }

        @Override
        public ItemStack getItem() {
            return unbreakable(new ItemStack(tier.getPickaxeMaterial()));
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return currency;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                // Only allow buying if this tier is strictly higher than current.
                return a.getPlayerPickaxeTier(player).ordinal() < tier.ordinal();
            }
            return false;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                a.setPlayerPickaxeTier(player, tier);
                player.getInventory().addItem(unbreakable(new ItemStack(tier.getPickaxeMaterial())));
            }
        }
    }

    private static final class ShearsTier implements IContentTier {
        private final int cost;

        ShearsTier(final int cost) {
            this.cost = cost;
        }

        @Override
        public ItemStack getItem() {
            return unbreakable(new ItemStack(Material.SHEARS));
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return Material.IRON_INGOT;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                // Permanent shears: only buy once.
                return !a.hasShears(player);
            }
            return false;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            if (arena instanceof Arena a) {
                a.setPlayerHasShears(player, true);
                player.getInventory().addItem(unbreakable(new ItemStack(Material.SHEARS)));
            }
        }
    }

    private static final class DiamondSwordTier implements IContentTier {
        @Override
        public ItemStack getItem() {
            return unbreakable(new ItemStack(Material.DIAMOND_SWORD));
        }

        @Override
        public int getCost() {
            return 4;
        }

        @Override
        public int getCostFor(final IArena arena) {
            return arena != null && arena.getTeamSize() >= 3 ? 3 : 4;
        }

        @Override
        public Material getCurrency() {
            return Material.DIAMOND;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            // Only allow if current best sword is weaker than diamond.
            final int bestTier = bestSwordTier(player.getInventory());
            final int thisTier = swordTierIndex(Material.DIAMOND_SWORD);
            return bestTier < thisTier;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            replaceFirstSwordWith(player.getInventory(), unbreakable(new ItemStack(Material.DIAMOND_SWORD)));
        }
    }

    private static final Material[] SWORD_MATERIALS = {
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD
    };

    private static boolean isSword(final Material type) {
        for (Material m : SWORD_MATERIALS) {
            if (m == type) return true;
        }
        return false;
    }

    private static int swordTierIndex(final Material type) {
        for (int i = 0; i < SWORD_MATERIALS.length; i++) {
            if (SWORD_MATERIALS[i] == type) {
                return i;
            }
        }
        return -1;
    }

    private static int bestSwordTier(final Inventory inv) {
        int best = -1;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getType().isAir()) continue;
            final int idx = swordTierIndex(stack.getType());
            if (idx > best) best = idx;
        }
        return best;
    }

    /** Replaces the first sword in the inventory with the new sword; if none found, adds the item. */
    private static void replaceFirstSwordWith(final Inventory inv, final ItemStack newSword) {
        for (int i = 0; i < inv.getSize(); i++) {
            final ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir() && isSword(stack.getType())) {
                inv.setItem(i, newSword);
                return;
            }
        }
        inv.addItem(newSword);
    }

    private static final class SwordUpgradeTier implements IContentTier {
        private final ItemStack reward;
        private final int cost;
        private final Material currency;

        SwordUpgradeTier(final Material swordType, final int cost, final Material currency) {
            this.reward = unbreakable(new ItemStack(swordType));
            this.cost = cost;
            this.currency = currency;
        }

        @Override
        public ItemStack getItem() {
            return reward.clone();
        }

        @Override
        public int getCost() {
            return cost;
        }

        @Override
        public Material getCurrency() {
            return currency;
        }

        @Override
        public boolean canPurchase(final Player player, final IArena arena) {
            // Prevent buying the same sword tier again or downgrading.
            final int bestTier = bestSwordTier(player.getInventory());
            final int thisTier = swordTierIndex(reward.getType());
            return thisTier > bestTier;
        }

        @Override
        public void giveReward(final Player player, final IArena arena) {
            replaceFirstSwordWith(player.getInventory(), reward.clone());
        }
    }
}
