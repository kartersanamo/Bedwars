package com.kartersanamo.bedwars.shop.tiers;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.arena.Arena;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        return new ItemTier(unbreakable(new ItemStack(swordType)), cost, currency);
    }

    public static IContentTier bow(final ItemStack bow, final int cost, final Material currency) {
        return new ItemTier(bow, cost, currency);
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
            return new ItemStack(tier.getLeggings());
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
        public void giveReward(final Player player, final IArena arena) {
            player.getInventory().addItem(unbreakable(new ItemStack(Material.DIAMOND_SWORD)));
        }
    }
}
