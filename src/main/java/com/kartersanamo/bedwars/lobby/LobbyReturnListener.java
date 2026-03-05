package com.kartersanamo.bedwars.lobby;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the waiting-lobby \"Return to Lobby\" bed item.
 *
 * Right-click once: start 3 second timer to leave.
 * Right-click again during countdown: cancel and stay.
 */
public final class LobbyReturnListener implements Listener {

    private final Bedwars plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public LobbyReturnListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(final PlayerInteractEvent event) {
        final Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }
        final EGameState state = arena.getGameState();
        if (state != EGameState.LOBBY_WAITING && state != EGameState.STARTING) {
            return;
        }

        final ItemStack item = event.getItem();
        if (!LobbyReturnItem.isLobbyReturnItem(item)) {
            return;
        }

        event.setCancelled(true);

        final UUID uuid = player.getUniqueId();
        final BukkitTask existing = pending.remove(uuid);
        if (existing != null) {
            existing.cancel();
            player.sendMessage(ChatColor.RED + "Return cancelled.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Returning to lobby in 3 seconds. Right-click again to cancel.");
        final BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                pending.remove(uuid);

                final IArena current = plugin.getArenaManager().getArena(player);
                if (current == null) {
                    return;
                }

                // Mirror /bedwars leave behavior.
                current.removePlayer(player, true);
                plugin.getArenaManager().playerLeftArena(player);
                plugin.getSidebarService().removeSidebar(player);
                LobbyReturnItem.removeFrom(player);

                if (plugin.getMainConfig().getLobbySpawn() != null) {
                    player.teleport(plugin.getMainConfig().getLobbySpawn());
                }

                player.sendMessage(ChatColor.GREEN + "You left the arena.");
            }
        }.runTaskLater(plugin, 3 * 20L);

        pending.put(uuid, task);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        cancelPending(event.getPlayer().getUniqueId());
    }

    private void cancelPending(final UUID uuid) {
        final BukkitTask task = pending.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}

