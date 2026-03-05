package com.kartersanamo.bedwars.shop;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.shop.IContentTier;
import com.kartersanamo.bedwars.arena.kit.ArmorTier;
import com.kartersanamo.bedwars.arena.kit.ToolTier;
import com.kartersanamo.bedwars.shop.main.CategoryContent;
import com.kartersanamo.bedwars.shop.main.ShopCategory;
import com.kartersanamo.bedwars.shop.tiers.ShopTiers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Shop manager: 54-slot GUI with top row categories (Quick Buy, Blocks, Melee, Armor, Tools, Ranged, Potions, Utility, Rotating Items).
 */
public final class ShopManager {

    private static final int CONTENT_START = 9;
    private static final int CONTENT_SLOTS = 45;

    /** Nav order: slot 0 = Quick Buy, 1 = Blocks, 2 = Melee, 3 = Armor, 4 = Tools, 5 = Ranged, 6 = Potions, 7 = Utility, 8 = Rotating Items. */
    private static final String[] NAV_ORDER = {
            "quick_buy", "blocks", "melee", "armor", "tools", "ranged", "potions", "utility", "rotating"
    };

    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private final List<IContentTier> defaultQuickBuyTiers = new ArrayList<>();

    public ShopManager() {
        loadDefaultShop();
        buildDefaultQuickBuy();
    }

    private void loadDefaultShop() {
        // --- Blocks ---
        final ShopCategory blocks = new ShopCategory("blocks", navIcon(Material.TERRACOTTA, "Blocks"));
        blocks.addContent(new CategoryContent("wool", List.of(ShopTiers.wool(16, 4))));
        blocks.addContent(new CategoryContent("terracotta", List.of(ShopTiers.item(Material.TERRACOTTA, 16, 16, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("glass", List.of(ShopTiers.item(Material.GLASS, 4, 12, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("sand", List.of(ShopTiers.item(Material.SAND, 12, 12, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("ladder", List.of(ShopTiers.item(Material.LADDER, 8, 4, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("wood", List.of(ShopTiers.item(Material.OAK_PLANKS, 16, 4, Material.GOLD_INGOT))));
        blocks.addContent(new CategoryContent("obsidian", List.of(ShopTiers.item(Material.OBSIDIAN, 4, 4, Material.EMERALD))));
        blocks.addContent(new CategoryContent("packed_ice", List.of(ShopTiers.item(Material.PACKED_ICE, 8, 8, Material.IRON_INGOT))));
        blocks.addContent(new CategoryContent("endstone", List.of(ShopTiers.item(Material.END_STONE, 12, 24, Material.IRON_INGOT))));
        categories.put(blocks.getId(), blocks);

        // --- Melee ---
        final ShopCategory melee = new ShopCategory("melee", navIcon(Material.GOLDEN_SWORD, "Melee"));
        melee.addContent(new CategoryContent("stone_sword", List.of(ShopTiers.sword(Material.STONE_SWORD, 10, Material.IRON_INGOT))));
        melee.addContent(new CategoryContent("iron_sword", List.of(ShopTiers.sword(Material.IRON_SWORD, 7, Material.GOLD_INGOT))));
        melee.addContent(new CategoryContent("diamond_sword", List.of(ShopTiers.diamondSword())));
        categories.put(melee.getId(), melee);

        // --- Armor ---
        final ShopCategory armor = new ShopCategory("armor", navIcon(Material.LEATHER_BOOTS, "Armor"));
        armor.addContent(new CategoryContent("chainmail", List.of(ShopTiers.armorUpgrade(ArmorTier.CHAINMAIL, 30, Material.IRON_INGOT))));
        armor.addContent(new CategoryContent("iron_armor", List.of(ShopTiers.armorUpgrade(ArmorTier.IRON, 12, Material.GOLD_INGOT))));
        armor.addContent(new CategoryContent("diamond_armor", List.of(ShopTiers.armorUpgrade(ArmorTier.DIAMOND, 6, Material.EMERALD))));
        categories.put(armor.getId(), armor);

        // --- Tools ---
        final ShopCategory tools = new ShopCategory("tools", navIcon(Material.WOODEN_PICKAXE, "Tools"));
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

        // --- Ranged ---
        final ShopCategory ranged = new ShopCategory("ranged", navIcon(Material.BOW, "Ranged"));
        ranged.addContent(new CategoryContent("arrows", List.of(ShopTiers.item(Material.ARROW, 6, 2, Material.GOLD_INGOT))));
        ranged.addContent(new CategoryContent("bow", List.of(ShopTiers.item(Material.BOW, 12, Material.GOLD_INGOT))));
        ranged.addContent(new CategoryContent("power_bow", List.of(ShopTiers.powerBow())));
        ranged.addContent(new CategoryContent("punch_bow", List.of(ShopTiers.punchBow())));
        categories.put(ranged.getId(), ranged);

        // --- Potions (empty for now / seasonal) ---
        final ShopCategory potions = new ShopCategory("potions", navIcon(Material.POTION, "Potions"));
        categories.put(potions.getId(), potions);

        // --- Utility ---
        final ShopCategory utility = new ShopCategory("utility", navIcon(Material.TNT, "Utility"));
        utility.addContent(new CategoryContent("gapple", List.of(ShopTiers.goldenApple())));
        utility.addContent(new CategoryContent("ender_pearl", List.of(ShopTiers.enderPearl())));
        utility.addContent(new CategoryContent("kb_stick", List.of(ShopTiers.knockbackStick())));
        utility.addContent(new CategoryContent("water_bucket", List.of(ShopTiers.item(Material.WATER_BUCKET, 1, 1, Material.EMERALD))));
        categories.put(utility.getId(), utility);

        // --- Rotating Items (empty for now / seasonal) ---
        final ShopCategory rotating = new ShopCategory("rotating", navIcon(Material.GRAY_STAINED_GLASS_PANE, "Rotating Items"));
        categories.put(rotating.getId(), rotating);
    }

    private void buildDefaultQuickBuy() {
        defaultQuickBuyTiers.clear();
        final ShopCategory blocks = getCategory("blocks");
        final ShopCategory melee = getCategory("melee");
        final ShopCategory armor = getCategory("armor");
        final ShopCategory tools = getCategory("tools");
        final ShopCategory ranged = getCategory("ranged");
        final ShopCategory utility = getCategory("utility");
        collectFirstTier(blocks, defaultQuickBuyTiers, 1);
        defaultQuickBuyTiers.add(getTierAtSlot(melee, 0));
        defaultQuickBuyTiers.add(getTierAtSlot(armor, 0));
        defaultQuickBuyTiers.add(getTierAtSlot(tools, 0));
        defaultQuickBuyTiers.add(getTierAtSlot(ranged, 0));
        defaultQuickBuyTiers.add(getTierAtSlot(utility, 0));
        defaultQuickBuyTiers.add(getTierAtSlot(utility, 1));
        defaultQuickBuyTiers.add(getTierAtSlot(utility, 2));
        defaultQuickBuyTiers.add(getTierAtSlot(utility, 3));
        defaultQuickBuyTiers.removeIf(Objects::isNull);
    }

    private static void collectFirstTier(final ShopCategory category, final List<IContentTier> out, final int maxPerContent) {
        if (category == null) return;
        for (CategoryContent content : category.getContents()) {
            int n = 0;
            for (IContentTier tier : content.getTiers()) {
                if (n++ >= maxPerContent) break;
                out.add(tier);
            }
        }
    }

    /** Opens the shop; default view is Quick Buy. */
    public void openShop(final Player player, final IArena arena) {
        openView(player, "quick_buy", arena);
    }

    /** Opens a specific view (quick_buy or category id). */
    public void openView(final Player player, final String viewId, final IArena arena) {
        final String title = viewTitle(viewId);
        final ShopCategory category = "quick_buy".equals(viewId) ? null : getCategory(viewId);
        final List<IContentTier> contentTiers = "quick_buy".equals(viewId)
                ? defaultQuickBuyTiers
                : getContentTiersList(category);
        final ShopInventoryHolder holder = new ShopInventoryHolderImpl(viewId, category, contentTiers);
        final Inventory inv = Bukkit.createInventory(holder, 54, title);

        for (int i = 0; i < NAV_ORDER.length; i++) {
            inv.setItem(i, navItemForSlot(i));
        }
        for (int i = 0; i < contentTiers.size() && i < CONTENT_SLOTS; i++) {
            inv.setItem(CONTENT_START + i, displayItem(contentTiers.get(i), arena));
        }
        player.openInventory(inv);
    }

    private static String viewTitle(final String viewId) {
        if ("quick_buy".equals(viewId)) return "Quick Buy";
        return switch (viewId) {
            case "blocks" -> "Blocks";
            case "melee" -> "Melee";
            case "armor" -> "Armor";
            case "tools" -> "Tools";
            case "ranged" -> "Ranged";
            case "potions" -> "Potions";
            case "utility" -> "Utility";
            case "rotating" -> "Rotating Items";
            default -> viewId;
        };
    }

    private ItemStack navItemForSlot(final int slot) {
        if (slot == 0) {
            return navIcon(Material.NETHER_STAR, "Quick Buy");
        }
        final String id = NAV_ORDER[slot];
        final ShopCategory cat = getCategory(id);
        if (cat == null) return navIcon(Material.GRAY_STAINED_GLASS_PANE, viewTitle(id));
        final ItemStack icon = cat.getIcon().clone();
        final ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(ChatColor.YELLOW + "Click to view!"));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack displayItem(final IContentTier tier, final IArena arena) {
        final ItemStack item = tier.getItem().clone();
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            final int cost = tier.getCostFor(arena);
            final String currencyName = formatCurrency(tier.getCurrency());
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Cost: " + cost + " " + currencyName);
            lore.add(ChatColor.AQUA + "Shift Click to add to Quick Buy");
            meta.setLore(lore);
            if (meta.hasDisplayName()) {
                meta.setDisplayName(ChatColor.RED + ChatColor.stripColor(meta.getDisplayName()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatCurrency(final Material m) {
        if (m == Material.IRON_INGOT) return "Iron";
        if (m == Material.GOLD_INGOT) return "Gold";
        if (m == Material.DIAMOND) return "Diamond";
        if (m == Material.EMERALD) return "Emerald";
        final String n = m.name().toLowerCase().replace("_", " ");
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private List<IContentTier> getContentTiersList(final ShopCategory category) {
        if (category == null) return Collections.emptyList();
        final List<IContentTier> list = new ArrayList<>();
        for (CategoryContent content : category.getContents()) {
            list.addAll(content.getTiers());
        }
        return list;
    }

    public Collection<ShopCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    public ShopCategory getCategory(final String id) {
        return categories.get(id != null ? id.toLowerCase() : null);
    }

    public String[] getNavOrder() {
        return NAV_ORDER.clone();
    }

    /** Returns the tier at content slot index (0-based) for the given category. */
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

    private static ItemStack navIcon(final Material material, final String name) {
        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
