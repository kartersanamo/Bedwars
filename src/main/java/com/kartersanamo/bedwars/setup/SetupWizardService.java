package com.kartersanamo.bedwars.setup;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import com.kartersanamo.bedwars.api.configuration.ConfigPath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * In-world setup wizard: swaps the player's hotbar for tools that write arena YAML.
 */
public final class SetupWizardService {

    public enum ToolKind {
        MAIN_DIAMOND_GEN,
        MAIN_EMERALD_GEN,
        MAIN_LOBBY_SPAWN,
        MAIN_SPEC_SPAWN,
        MAIN_LOBBY_P1,
        MAIN_LOBBY_P2,
        MAIN_ARENA_P1,
        MAIN_ARENA_P2,
        MAIN_TEAM_PICKER,
        TEAM_SPAWN,
        TEAM_BED,
        TEAM_SHOP,
        TEAM_UPGRADE,
        TEAM_IRON_GEN,
        TEAM_GOLD_GEN,
        TEAM_BACK,
        TEAM_SAVE_RELOAD,
        TEAM_EXIT
    }

    private static final class Session {
        final String arenaId;
        final File file;
        final YamlConfiguration yaml;
        final ItemStack[] inventoryBackup;

        ETeamColor editingTeam;

        Session(final String arenaId, final File file, final YamlConfiguration yaml, final ItemStack[] inventoryBackup) {
            this.arenaId = arenaId;
            this.file = file;
            this.yaml = yaml;
            this.inventoryBackup = inventoryBackup;
        }
    }

    private static final class PendingModeSelection {
        final String arenaId;
        final File file;
        final YamlConfiguration yaml;
        final boolean createNew;
        final EnumSet<BedwarsSetupMode> selected = BedwarsSetupMode.allEnabledByDefault();

        PendingModeSelection(final String arenaId, final File file, final YamlConfiguration yaml, final boolean createNew) {
            this.arenaId = arenaId;
            this.file = file;
            this.yaml = yaml;
            this.createNew = createNew;
        }
    }

    private final Bedwars plugin;
    private final NamespacedKey toolKey;
    private final NamespacedKey modeWizardKey;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, PendingModeSelection> pendingModeSelection = new HashMap<>();

    public SetupWizardService(final Bedwars plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "setup_tool");
        this.modeWizardKey = new NamespacedKey(plugin, "setup_mode_wizard");
    }

    public boolean isInSetup(final Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public ToolKind readToolKind(final ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        final String raw = stack.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return ToolKind.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void startOrResume(final Player player, final String arenaIdRaw, final boolean createNew) throws IOException {
        if (sessions.containsKey(player.getUniqueId())) {
            exitWithoutReload(player);
        }
        abandonPendingModeSelectionSilently(player);
        final String arenaId = arenaIdRaw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        final File file = new File(plugin.getDataFolder(), "arenas/" + arenaId + ".yml");
        final YamlConfiguration yaml = ArenaSetupYamlIO.loadOrCreate(file);
        final boolean missingWorld = !yaml.contains(ConfigPath.Arena.WORLD);
        final boolean freshScaffold = createNew || missingWorld;
        if (freshScaffold) {
            if (missingWorld) {
                applyBaseArenaScaffold(player, yaml, arenaId);
            }
            yaml.set(ConfigPath.Arena.MODES, null);
            pendingModeSelection.put(player.getUniqueId(), new PendingModeSelection(arenaId, file, yaml, createNew));
            openModeSelectionGui(player);
            player.sendMessage(ChatColor.GREEN + "New arena '" + arenaId + "' — choose which game modes to support.");
            player.sendMessage(ChatColor.GRAY + "All four are enabled by default (typical public map). Click wool to toggle. "
                    + "Lime concrete confirms and gives you setup tools; barrier cancels.");
            ArenaSetupYamlIO.save(yaml, file);
            return;
        }
        enterSetupWithTools(player, arenaId, file, yaml);
    }

    /**
     * Writes world/display/root defaults only; {@code modes} is filled after the mode GUI confirms.
     */
    private static void applyBaseArenaScaffold(final Player player, final YamlConfiguration yaml, final String arenaId) {
        yaml.set(ConfigPath.Arena.WORLD, player.getWorld().getName());
        yaml.set(ConfigPath.Arena.DISPLAY_NAME, arenaId);
        yaml.set(ConfigPath.Arena.ENABLED, false);
        // Player limits live under modes.* only (same layout as reference arena.yml files).
    }

    private static void applyModesSection(final YamlConfiguration yaml, final EnumSet<BedwarsSetupMode> selected) {
        yaml.set(ConfigPath.Arena.MODES, null);
        for (final BedwarsSetupMode mode : BedwarsSetupMode.values()) {
            if (!selected.contains(mode)) {
                continue;
            }
            final String base = ConfigPath.Arena.MODES + "." + mode.getYamlKey();
            yaml.set(base + "." + ConfigPath.Arena.MIN_PLAYERS, mode.getMinPlayers());
            yaml.set(base + "." + ConfigPath.Arena.MAX_PLAYERS, mode.getMaxPlayers());
            yaml.set(base + "." + ConfigPath.Arena.TEAM_SIZE, mode.getTeamSize());
        }
    }

    private void enterSetupWithTools(final Player player, final String arenaId, final File file, final YamlConfiguration yaml) throws IOException {
        final ItemStack[] backup = cloneContents(player.getInventory().getContents());
        sessions.put(player.getUniqueId(), new Session(arenaId, file, yaml, backup));
        applyMainHotbar(player);
        player.sendMessage(ChatColor.GREEN + "Setup wizard for arena '" + arenaId + "'. Your inventory was backed up.");
        player.sendMessage(ChatColor.GRAY + "Right-click with each tool to set points. Use the book to pick a team. Barrier exits without saving reload.");
        save(player);
    }

    public void exitWithoutReload(final Player player) {
        if (pendingModeSelection.containsKey(player.getUniqueId())) {
            cancelPendingModeSelection(player, true);
        }
        final Session s = sessions.remove(player.getUniqueId());
        if (s == null) {
            return;
        }
        restoreInventory(player, s);
        player.sendMessage(ChatColor.YELLOW + "Left setup wizard. Use /bw reload to apply YAML changes if you saved.");
    }

    public void exitAndReload(final Player player) {
        final Session s = sessions.remove(player.getUniqueId());
        if (s == null) {
            return;
        }
        try {
            ArenaSetupYamlIO.save(s.yaml, s.file);
        } catch (IOException ex) {
            player.sendMessage(ChatColor.RED + "Failed to save: " + ex.getMessage());
            restoreInventory(player, s);
            return;
        }
        restoreInventory(player, s);
        plugin.reloadArenas();
        player.sendMessage(ChatColor.GREEN + "Arena YAML saved and arenas reloaded.");
    }

    public void onQuit(final Player player) {
        cancelPendingModeSelection(player, false);
        final Session s = sessions.remove(player.getUniqueId());
        if (s != null) {
            restoreInventory(player, s);
        }
    }

    private void restoreInventory(final Player player, final Session s) {
        player.getInventory().setContents(s.inventoryBackup);
        player.updateInventory();
    }

    public static final String TEAM_PICKER_TITLE = ChatColor.DARK_PURPLE + "Pick team to edit";
    public static final String MODE_SELECTION_TITLE = ChatColor.DARK_GREEN + "Arena game modes";

    public boolean hasPendingModeSelection(final Player player) {
        return pendingModeSelection.containsKey(player.getUniqueId());
    }

    private void abandonPendingModeSelectionSilently(final Player player) {
        final PendingModeSelection pending = pendingModeSelection.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (pending.createNew && pending.file.exists()) {
            pending.file.delete();
        } else {
            try {
                revertIncompleteModeDraft(pending);
            } catch (final IOException ignored) {
            }
        }
    }

    private static void revertIncompleteModeDraft(final PendingModeSelection pending) throws IOException {
        final ConfigurationSection ms = pending.yaml.getConfigurationSection(ConfigPath.Arena.MODES);
        final boolean modesEmpty = ms == null || ms.getKeys(false).isEmpty();
        if (modesEmpty) {
            pending.yaml.set(ConfigPath.Arena.WORLD, null);
            pending.yaml.set(ConfigPath.Arena.MODES, null);
            ArenaSetupYamlIO.save(pending.yaml, pending.file);
        }
    }

    /**
     * @param notifyIfCancelled when {@code true}, tells the player the new-arena draft was discarded.
     */
    public void cancelPendingModeSelection(final Player player, final boolean notifyIfCancelled) {
        final PendingModeSelection pending = pendingModeSelection.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (pending.createNew && pending.file.exists()) {
            pending.file.delete();
        } else {
            try {
                revertIncompleteModeDraft(pending);
            } catch (final IOException ignored) {
            }
        }
        if (notifyIfCancelled) {
            player.sendMessage(ChatColor.YELLOW + "Cancelled arena mode setup (draft reverted or removed).");
        }
    }

    public void openModeSelectionGui(final Player player) {
        final PendingModeSelection pending = pendingModeSelection.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        final Inventory inv = plugin.getServer().createInventory(null, 27, MODE_SELECTION_TITLE);
        fillModeSelectionGui(inv, pending);
        player.openInventory(inv);
    }

    private void fillModeSelectionGui(final Inventory inv, final PendingModeSelection pending) {
        final ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }
        inv.setItem(10, modeToggleStack(BedwarsSetupMode.SOLO, pending.selected.contains(BedwarsSetupMode.SOLO)));
        inv.setItem(12, modeToggleStack(BedwarsSetupMode.DOUBLES, pending.selected.contains(BedwarsSetupMode.DOUBLES)));
        inv.setItem(14, modeToggleStack(BedwarsSetupMode.THREES, pending.selected.contains(BedwarsSetupMode.THREES)));
        inv.setItem(16, modeToggleStack(BedwarsSetupMode.FOURS, pending.selected.contains(BedwarsSetupMode.FOURS)));
        inv.setItem(4, actionStack(Material.BARRIER, ChatColor.RED + "Cancel", List.of(
                ChatColor.GRAY + "Closes and removes this new arena draft.",
                ChatColor.DARK_GRAY + "Same as pressing Esc (cancel)."
        ), "ACTION:CANCEL"));
        inv.setItem(22, actionStack(Material.LIME_CONCRETE, ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm & start setup", List.of(
                ChatColor.GRAY + "Writes the selected modes to the arena file",
                ChatColor.GRAY + "and gives you the setup tools."
        ), "ACTION:CONFIRM"));
    }

    private ItemStack modeToggleStack(final BedwarsSetupMode mode, final boolean enabled) {
        final ItemStack stack = new ItemStack(mode.getIconMaterial());
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + mode.getLabel()
                    + (enabled ? ChatColor.GREEN + "  —  ON" : ChatColor.DARK_GRAY + "  —  OFF"));
            final List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "Team size: " + mode.getTeamSize());
            lore.add(ChatColor.GRAY + "Players: " + mode.getMinPlayers() + " – " + mode.getMaxPlayers());
            lore.add("");
            lore.add(ChatColor.WHITE + mode.getBlurb());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to turn " + (enabled ? "off" : "on"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(modeWizardKey, PersistentDataType.STRING, "MODE:" + mode.name());
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack actionStack(final Material mat, final String name, final List<String> lore, final String pdc) {
        final ItemStack stack = new ItemStack(mat);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(modeWizardKey, PersistentDataType.STRING, pdc);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void handleModeSelectionClick(final Player player, final ItemStack stack) {
        final PendingModeSelection pending = pendingModeSelection.get(player.getUniqueId());
        if (pending == null || stack == null || !stack.hasItemMeta()) {
            return;
        }
        final String tag = stack.getItemMeta().getPersistentDataContainer().get(modeWizardKey, PersistentDataType.STRING);
        if (tag == null) {
            return;
        }
        if (tag.startsWith("MODE:")) {
            final BedwarsSetupMode mode;
            try {
                mode = BedwarsSetupMode.valueOf(tag.substring("MODE:".length()));
            } catch (final IllegalArgumentException ex) {
                return;
            }
            if (pending.selected.contains(mode)) {
                if (pending.selected.size() <= 1) {
                    player.sendMessage(ChatColor.RED + "Enable at least one game mode.");
                    return;
                }
                pending.selected.remove(mode);
            } else {
                pending.selected.add(mode);
            }
            refreshOpenModeSelectionGui(player, pending);
            return;
        }
        if ("ACTION:CONFIRM".equals(tag)) {
            try {
                confirmModeSelection(player, pending);
            } catch (final IOException ex) {
                player.sendMessage(ChatColor.RED + "Could not save arena: " + ex.getMessage());
            }
            return;
        }
        if ("ACTION:CANCEL".equals(tag)) {
            player.closeInventory();
            cancelPendingModeSelection(player, true);
        }
    }

    private void refreshOpenModeSelectionGui(final Player player, final PendingModeSelection pending) {
        if (!MODE_SELECTION_TITLE.equals(player.getOpenInventory().getTitle())) {
            return;
        }
        fillModeSelectionGui(player.getOpenInventory().getTopInventory(), pending);
    }

    private void confirmModeSelection(final Player player, final PendingModeSelection pending) throws IOException {
        if (pending.selected.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Select at least one mode.");
            return;
        }
        pendingModeSelection.remove(player.getUniqueId());
        applyModesSection(pending.yaml, pending.selected);
        ArenaSetupYamlIO.save(pending.yaml, pending.file);
        player.closeInventory();
        final StringBuilder summary = new StringBuilder();
        for (final BedwarsSetupMode m : BedwarsSetupMode.values()) {
            if (pending.selected.contains(m)) {
                if (summary.length() > 0) {
                    summary.append(ChatColor.DARK_GRAY).append(", ");
                }
                summary.append(ChatColor.AQUA).append(m.getLabel());
            }
        }
        player.sendMessage(ChatColor.GREEN + "Modes saved: " + summary);
        enterSetupWithTools(player, pending.arenaId, pending.file, pending.yaml);
    }

    /**
     * Called one tick after the mode GUI closes: if the player still had a pending new arena and did not confirm,
     * discard the draft (same as cancel).
     */
    public void onModeSelectionInventoryClosed(final Player player) {
        if (!pendingModeSelection.containsKey(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingModeSelection.containsKey(player.getUniqueId())) {
                cancelPendingModeSelection(player, true);
            }
        }, 1L);
    }

    public void openTeamPicker(final Player player) {
        final Inventory inv = plugin.getServer().createInventory(null, 27, TEAM_PICKER_TITLE);
        int slot = 10;
        for (final ETeamColor color : ETeamColor.values()) {
            final ItemStack wool = new ItemStack(color.getWoolMaterial());
            final ItemMeta meta = wool.getItemMeta();
            if (meta != null) {
                final String teamLabel = "" + color.name().charAt(0)
                        + color.name().substring(1).toLowerCase(Locale.ROOT) + " team";
                meta.setDisplayName(color.getChatColor() + teamLabel);
                wool.setItemMeta(meta);
            }
            inv.setItem(slot++, wool);
            if (slot == 17) {
                slot = 19;
            }
        }
        inv.setItem(22, named(Material.BARRIER, ChatColor.RED + "Back", null));
        player.openInventory(inv);
    }

    public void onTeamPick(final Player player, final ETeamColor color) {
        final Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        s.editingTeam = color;
        final String tid = color.name().toLowerCase(Locale.ROOT);
        ArenaSetupYamlIO.ensureTeam(s.yaml, color.name(), color.name());
        applyTeamHotbar(player, color);
        player.closeInventory();
        try {
            save(player);
        } catch (IOException ignored) {
        }
        player.sendMessage(ChatColor.GREEN + "Editing " + color.getChatColor() + tid + ChatColor.GREEN + " team — use your hotbar tools.");
    }

    public void backToMainHotbar(final Player player) {
        final Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        s.editingTeam = null;
        applyMainHotbar(player);
        try {
            save(player);
        } catch (IOException ignored) {
        }
    }

    public void handleToolUse(final Player player, final ToolKind kind) throws IOException {
        final Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        final Location loc = targetLocation(player);
        switch (kind) {
            case MAIN_DIAMOND_GEN -> ArenaSetupYamlIO.appendDiamondGenerator(s.yaml, loc);
            case MAIN_EMERALD_GEN -> ArenaSetupYamlIO.appendEmeraldGenerator(s.yaml, loc);
            case MAIN_LOBBY_SPAWN -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.LOBBY_SPAWN, loc);
            case MAIN_SPEC_SPAWN -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.SPECTATOR_SPAWN, loc);
            case MAIN_LOBBY_P1 -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.LOBBY_REGION_POS1, loc);
            case MAIN_LOBBY_P2 -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.LOBBY_REGION_POS2, loc);
            case MAIN_ARENA_P1 -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.ARENA_REGION_POS1, loc);
            case MAIN_ARENA_P2 -> ArenaSetupYamlIO.writeLocation(s.yaml, ConfigPath.Arena.ARENA_REGION_POS2, loc);
            case MAIN_TEAM_PICKER -> {
                openTeamPicker(player);
                return;
            }
            case TEAM_SPAWN -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.writeLocation(s.yaml, teamPath(s, ConfigPath.Arena.TEAM_SPAWN), loc);
            }
            case TEAM_BED -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.writeLocation(s.yaml, teamPath(s, ConfigPath.Arena.TEAM_BED), loc);
            }
            case TEAM_SHOP -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.writeLocation(s.yaml, teamPath(s, ConfigPath.Arena.TEAM_SHOP_NPC), loc);
            }
            case TEAM_UPGRADE -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.writeLocation(s.yaml, teamPath(s, ConfigPath.Arena.TEAM_UPGRADE_NPC), loc);
            }
            case TEAM_IRON_GEN -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.appendTeamGenerator(s.yaml, s.editingTeam.name(),
                        ConfigPath.Arena.TEAM_IRON_GENERATORS, loc);
            }
            case TEAM_GOLD_GEN -> {
                if (s.editingTeam == null) {
                    player.sendMessage(ChatColor.RED + "Pick a team first (book in main hotbar).");
                    return;
                }
                ArenaSetupYamlIO.appendTeamGenerator(s.yaml, s.editingTeam.name(),
                        ConfigPath.Arena.TEAM_GOLD_GENERATORS, loc);
            }
            case TEAM_BACK -> {
                backToMainHotbar(player);
                return;
            }
            case TEAM_SAVE_RELOAD -> {
                exitAndReload(player);
                return;
            }
            case TEAM_EXIT -> {
                exitWithoutReload(player);
                return;
            }
            default -> {
                return;
            }
        }
        save(player);
        final String facing = ArenaSetupYamlIO.describeCardinalFacing(loc.getYaw(), loc.getPitch());
        player.sendMessage(ChatColor.GREEN + "Updated arena '" + s.arenaId + "' (" + kind + "). "
                + ChatColor.GRAY + "Facing saved (cardinal snap): " + facing + ".");
    }

    private static String teamPath(final Session s, final String field) {
        return ConfigPath.Arena.TEAMS + "." + s.editingTeam.name() + "." + field;
    }

    /**
     * Position from targeted block (or feet on block), with the player's current look direction
     * so yaw/pitch are saved for spawns and NPCs.
     */
    private Location targetLocation(final Player player) {
        final Location feet = player.getLocation();
        final Block b = player.getTargetBlockExact(8);
        if (b != null) {
            final Location out = b.getLocation().clone().add(0.5, 0.0, 0.5);
            out.setYaw(feet.getYaw());
            out.setPitch(feet.getPitch());
            return out;
        }
        final Location out = feet.getBlock().getLocation().clone().add(0.5, 0.0, 0.5);
        out.setYaw(feet.getYaw());
        out.setPitch(feet.getPitch());
        return out;
    }

    private void save(final Player player) throws IOException {
        final Session s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        ArenaSetupYamlIO.save(s.yaml, s.file);
    }

    private void applyMainHotbar(final Player player) {
        player.getInventory().setItem(0, tag(ToolKind.MAIN_DIAMOND_GEN, Material.DIAMOND, ChatColor.AQUA + "Add diamond generator", "Right-click at generator block."));
        player.getInventory().setItem(1, tag(ToolKind.MAIN_EMERALD_GEN, Material.EMERALD, ChatColor.GREEN + "Add emerald generator", "Right-click at generator block."));
        player.getInventory().setItem(2, tag(ToolKind.MAIN_LOBBY_SPAWN, Material.RED_BED, ChatColor.RED + "Lobby spawn", "Right-click where players wait."));
        player.getInventory().setItem(3, tag(ToolKind.MAIN_SPEC_SPAWN, Material.ENDER_EYE, ChatColor.LIGHT_PURPLE + "Spectator spawn", "Right-click spectator join point."));
        player.getInventory().setItem(4, tag(ToolKind.MAIN_LOBBY_P1, Material.IRON_BLOCK, ChatColor.GRAY + "Lobby region corner 1", ""));
        player.getInventory().setItem(5, tag(ToolKind.MAIN_LOBBY_P2, Material.GOLD_BLOCK, ChatColor.GRAY + "Lobby region corner 2", ""));
        player.getInventory().setItem(6, tag(ToolKind.MAIN_ARENA_P1, Material.STONE, ChatColor.DARK_GRAY + "Arena region corner 1", ""));
        player.getInventory().setItem(7, tag(ToolKind.MAIN_ARENA_P2, Material.POLISHED_ANDESITE, ChatColor.DARK_GRAY + "Arena region corner 2", ""));
        player.getInventory().setItem(8, tag(ToolKind.MAIN_TEAM_PICKER, Material.WRITABLE_BOOK, ChatColor.YELLOW + "Team editor",
                "Right-click: pick team. Shift-right: exit wizard."));
        player.updateInventory();
    }

    private void applyTeamHotbar(final Player player, final ETeamColor color) {
        player.getInventory().setItem(0, tag(ToolKind.TEAM_SPAWN, color.getWoolMaterial(), color.getChatColor() + "Team spawn", ""));
        player.getInventory().setItem(1, tag(ToolKind.TEAM_BED, bedMaterial(color), color.getChatColor() + "Team bed", ""));
        player.getInventory().setItem(2, tag(ToolKind.TEAM_SHOP, Material.EMERALD, ChatColor.GREEN + "Item shop NPC", ""));
        player.getInventory().setItem(3, tag(ToolKind.TEAM_UPGRADE, Material.ENCHANTING_TABLE, ChatColor.LIGHT_PURPLE + "Upgrades NPC", ""));
        player.getInventory().setItem(4, tag(ToolKind.TEAM_IRON_GEN, Material.IRON_INGOT, ChatColor.WHITE + "Add iron generator", ""));
        player.getInventory().setItem(5, tag(ToolKind.TEAM_GOLD_GEN, Material.GOLD_INGOT, ChatColor.GOLD + "Add gold generator", ""));
        player.getInventory().setItem(6, tag(ToolKind.TEAM_BACK, Material.BOOK, ChatColor.YELLOW + "Back to main tools", ""));
        player.getInventory().setItem(7, tag(ToolKind.TEAM_SAVE_RELOAD, Material.NETHER_STAR, ChatColor.GREEN + "Save + reload arenas", ""));
        player.getInventory().setItem(8, tag(ToolKind.TEAM_EXIT, Material.BARRIER, ChatColor.RED + "Exit wizard", "Does not auto-reload."));
        player.updateInventory();
    }

    private static Material bedMaterial(final ETeamColor color) {
        return switch (color) {
            case RED -> Material.RED_BED;
            case BLUE -> Material.BLUE_BED;
            case GREEN -> Material.LIME_BED;
            case YELLOW -> Material.YELLOW_BED;
            case AQUA -> Material.LIGHT_BLUE_BED;
            case WHITE -> Material.WHITE_BED;
            case PINK -> Material.PINK_BED;
            case GRAY -> Material.GRAY_BED;
        };
    }

    private ItemStack tag(final ToolKind kind, final Material mat, final String name, final String loreLine) {
        final ItemStack stack = new ItemStack(mat);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLine != null && !loreLine.isEmpty()) {
                meta.setLore(java.util.List.of(ChatColor.GRAY + loreLine));
            }
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, kind.name());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack[] cloneContents(final ItemStack[] contents) {
        final ItemStack[] out = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            out[i] = contents[i] == null ? null : contents[i].clone();
        }
        return out;
    }

    private static ItemStack named(final Material mat, final String name, final String lore) {
        final ItemStack stack = new ItemStack(mat);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(java.util.List.of(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public NamespacedKey getToolKey() {
        return toolKey;
    }
}
