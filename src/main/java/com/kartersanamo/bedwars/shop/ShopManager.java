package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import com.kartersanamo.bedwars.shop.main.CategoryContent;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import com.kartersanamo.bedwars.shop.tiers.ShopTiers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Shop manager: categories and slot-to-tier lookup for purchases.
 */
public final class ShopManager {

    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    public ShopManager() {
        loadDefaultShop();
    }

    private void loadDefaultShop() {
        // --- Blocks ---
        final ShopCategory blocks = new ShopCategory("blocks", namedItem(Material.WHITE_WOOL, "Blocks"));
        blocks.addContent(new CategoryContent("wool", List.of(ShopTiers.wool(16, 4))));
        blocks.addContent(new CategoryContent("ladder", List.of(ShopTiers.item(Material.LADDER, 8, 4, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("glass", List.of(ShopTiers.item(Material.GLASS, 4, 12, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("clay", List.of(ShopTiers.item(Material.TERRACOTTA, 24, 16, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("endstone", List.of(ShopTiers.item(Material.END_STONE, 12, 24, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("wood", List.of(ShopTiers.item(Material.OAK_PLANKS, 16, 4, Material.GOLD_INGOT))));
        blocks.addContent(new CategoryContent("obsidian", List.of(ShopTiers.item(Material.OBSIDIAN, 4, 4, Material.EMERALD))));
        categories.put(blocks.getId(), blocks);

        // --- Swords (Melee) ---
        final ShopCategory swords = new ShopCategory("swords", namedItem(Material.GOLDEN_SWORD, "Swords"));
        swords.addContent(new CategoryContent("stone_sword", List.of(ShopTiers.sword(Material.STONE_SWORD, 10, Material.IRON_INGOT))));
        swords.addContent(new CategoryContent("iron_sword", List.of(ShopTiers.sword(Material.IRON_SWORD, 7, Material.GOLD_INGOT))));
        swords.addContent(new CategoryContent("diamond_sword", List.of(ShopTiers.diamondSword())));
        categories.put(swords.getId(), swords);

        // --- Tools ---
        final ShopCategory tools = new ShopCategory("tools", namedItem(Material.IRON_PICKAXE, "Tools"));
        tools.addContent(new CategoryContent("axe", List.of(
                ShopTiers.axeUpgrade(ToolTier.WOOD, 10, Material.IRON_INGOT),
                ShopTiers.axeUpgrade(ToolTier.STONE, 10, Material.IRON_INGOT),
                ShopTiers.axeUpgrade(ToolTier.IRON, 3, Material.GOLD_INGOT),
                ShopTiers.axeUpgrade(ToolTier.DIAMOND, 6, Material.GOLD_INGOT)
        )));
        tools.addContent(new CategoryContent("pickaxe", List.of(
                ShopTiers.pickaxeUpgrade(ToolTier.WOOD, 10, Material.IRON_INGOT),
                ShopTiers.pickaxeUpgrade(ToolTier.IRON, 10, Material.IRON_INGOT),
                ShopTiers.pickaxeUpgrade(ToolTier.GOLD, 3, Material.GOLD_INGOT),
                ShopTiers.pickaxeUpgrade(ToolTier.DIAMOND, 6, Material.GOLD_INGOT)
        )));
        tools.addContent(new CategoryContent("shears", List.of(ShopTiers.shears(20))));
        categories.put(tools.getId(), tools);

        // --- Armor ---
        final ShopCategory armor = new ShopCategory("armor", namedItem(Material.LEATHER_CHESTPLATE, "Armor"));
        armor.addContent(new CategoryContent("chainmail", List.of(ShopTiers.armorUpgrade(ArmorTier.CHAINMAIL, 30, Material.IRON_INGOT))));
        armor.addContent(new CategoryContent("iron_armor", List.of(ShopTiers.armorUpgrade(ArmorTier.IRON, 12, Material.GOLD_INGOT))));
        armor.addContent(new CategoryContent("diamond_armor", List.of(ShopTiers.armorUpgrade(ArmorTier.DIAMOND, 6, Material.EMERALD))));
        categories.put(armor.getId(), armor);

        // --- Ranged (Bow) ---
        final ShopCategory ranged = new ShopCategory("ranged", namedItem(Material.BOW, "Ranged"));
        ranged.addContent(new CategoryContent("arrows", List.of(ShopTiers.item(Material.ARROW, 6, 2, Material.GOLD_INGOT))));
        ranged.addContent(new CategoryContent("bow", List.of(ShopTiers.item(Material.BOW, 12, Material.GOLD_INGOT))));
        ranged.addContent(new CategoryContent("power_bow", List.of(ShopTiers.powerBow())));
        ranged.addContent(new CategoryContent("punch_bow", List.of(ShopTiers.punchBow())));
        categories.put(ranged.getId(), ranged);

        // --- Weapons (Knockback Stick) ---
        final ShopCategory weapons = new ShopCategory("weapons", namedItem(Material.STICK, "Weapons"));
        weapons.addContent(new CategoryContent("kb_stick", List.of(ShopTiers.knockbackStick())));
        categories.put(weapons.getId(), weapons);

        // --- Utility ---
        final ShopCategory utility = new ShopCategory("utility", namedItem(Material.GOLDEN_APPLE, "Utility"));
        utility.addContent(new CategoryContent("gapple", List.of(ShopTiers.goldenApple())));
        utility.addContent(new CategoryContent("ender_pearl", List.of(ShopTiers.enderPearl())));
        categories.put(utility.getId(), utility);
    }

    public void openShop(final Player player) {
        final Inventory inventory = Bukkit.createInventory(player, 27, "Item Shop");
        int slot = 0;
        for (ShopCategory category : categories.values()) {
            inventory.setItem(slot++, category.getIcon());
        }
        player.openInventory(inventory);
    }

    public Collection<ShopCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public ShopCategory getCategory(final String id) {
        return categories.get(id != null ? id.toLowerCase() : null);
    }

    /**
     * Returns the tier at the given slot when viewing the given category (same order as openCategory).
     */
    public IContentTier getTierAtSlot(final ShopCategory category, final int slot) {
        if (category == null || slot < 0) return null;
        int index = 0;
        for (CategoryContent content : category.getContents()) {
            for (IContentTier tier : content.getTiers()) {
                if (index == slot) return tier;
                index++;
            }
        }
        return null;
    }

    public void openCategory(final Player player, final ShopCategory category) {
        final String title = "Item Shop - " + category.getId();
        final Inventory inventory = Bukkit.createInventory(player, 54, title);
        int slot = 0;
        for (CategoryContent content : category.getContents()) {
            for (IContentTier tier : content.getTiers()) {
                if (slot < 54) inventory.setItem(slot++, tier.getItem());
            }
        }
        player.openInventory(inventory);
    }

    private ItemStack namedItem(final Material material, final String name) {
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
