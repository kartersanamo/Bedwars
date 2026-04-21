package com.kartersanamo.bedwars.setup;

import com.kartersanamo.bedwars.api.arena.team.ETeamColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles setup wizard interactions.
 */
public final class SetupWizardListener implements Listener {

    private final SetupWizardService service;

    public SetupWizardListener(final SetupWizardService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (!service.isInSetup(player)) {
            return;
        }
        if (!event.getAction().name().startsWith("RIGHT_CLICK")) {
            return;
        }
        final ItemStack item = event.getItem();
        final SetupWizardService.ToolKind kind = service.readToolKind(item);
        if (kind == null) {
            return;
        }
        event.setCancelled(true);
        if (kind == SetupWizardService.ToolKind.MAIN_TEAM_PICKER && event.getPlayer().isSneaking()) {
            service.exitWithoutReload(player);
            return;
        }
        try {
            service.handleToolUse(player, kind);
        } catch (final Exception ex) {
            player.sendMessage(ChatColor.RED + "Setup error: " + ex.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        if (service.isInSetup(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (SetupWizardService.MODE_SELECTION_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }
            if (service.hasPendingModeSelection(player)) {
                service.handleModeSelectionClick(player, event.getCurrentItem());
            }
            return;
        }
        if (!service.isInSetup(player)) {
            return;
        }
        if (SetupWizardService.TEAM_PICKER_TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }
            final ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                return;
            }
            for (final ETeamColor color : ETeamColor.values()) {
                if (clicked.getType() == color.getWoolMaterial()) {
                    service.onTeamPick(player, color);
                    return;
                }
            }
            return;
        }
        // Prevent rearranging inventory while in setup (hotbar tools must stay).
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!SetupWizardService.MODE_SELECTION_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        service.onModeSelectionInventoryClosed(player);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        service.onQuit(event.getPlayer());
    }
}
