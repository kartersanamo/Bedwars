package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.lobby.LobbyAnnouncementUtil;
import com.kartersanamo.bedwars.lobby.LobbyReturnItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Listener for Map Selector GUI interactions.
 */
public final class MapSelectorGuiListener implements Listener {

    private final Bedwars plugin;

    public MapSelectorGuiListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MapSelectorGuiHolder holder)) return;

        event.setCancelled(true);

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        final EGameMode gameMode = holder.getGameMode();

        if (clicked.getType() == Material.ARROW) {
            // Back arrow - go back to Play Bedwars GUI
            PlayBedwarsGui.openFor(player, gameMode);
        } else if (clicked.getType() == Material.FIREWORK_STAR) {
            // Map item - join the map
            handleMapClick(player, clicked, gameMode);
        }
    }

    /**
     * Handles clicking on a map item to join a specific map.
     */
    private void handleMapClick(final Player player, final ItemStack mapItem, final EGameMode gameMode) {
        if (!mapItem.hasItemMeta() || !Objects.requireNonNull(mapItem.getItemMeta()).hasDisplayName()) return;

        final String mapName = ChatColor.stripColor(mapItem.getItemMeta().getDisplayName());

        final IArena currentArena = plugin.getArenaManager().getArena(player);
        if (currentArena != null) {
            player.sendMessage(ChatColor.RED + "You are already in a Bedwars arena. Use /bedwars leave first.");
            return;
        }

        final IArena bestArena = findBestArenaForMap(mapName, gameMode);
        if (bestArena == null) {
            player.sendMessage(ChatColor.RED + "Could not find a joinable game for map: " + mapName);
            return;
        }

        if (!bestArena.addPlayer(player)) {
            player.sendMessage(ChatColor.RED + "Could not join this arena.");
            return;
        }

        plugin.getArenaManager().playerJoinedArena(player, bestArena);

        // Give the waiting-lobby "Return to Lobby" item.
        LobbyReturnItem.giveTo(player);

        player.teleport(bestArena.getLobbySpawn());
        LobbyAnnouncementUtil.broadcastJoin(bestArena, player);

        bestArena.tryStartCountdown();
        player.closeInventory();
    }

    /**
     * Finds the best arena for a given map and game mode, preferring arenas with available slots.
     */
    private IArena findBestArenaForMap(final String mapName, final EGameMode gameMode) {
        IArena bestWaiting = null;
        int bestWaitingPlayers = -1;
        IArena emptyLobby = null;

        for (IArena arena : getCandidateArenas(gameMode)) {
            if (!arena.getDisplayName().equalsIgnoreCase(mapName)) {
                continue;
            }

            final EGameState state = arena.getGameState();
            final int size = arena.getPlayers().size();

            if ((state == EGameState.LOBBY_WAITING || state == EGameState.STARTING)
                    && size > 0 && size < arena.getMaxPlayers()) {
                if (size > bestWaitingPlayers) {
                    bestWaitingPlayers = size;
                    bestWaiting = arena;
                }
            }

            if (state == EGameState.LOBBY_WAITING && size == 0 && emptyLobby == null) {
                emptyLobby = arena;
            }
        }

        if (bestWaiting != null) {
            return bestWaiting;
        }
        return emptyLobby;
    }

    /**
     * Gets a list of candidate arenas for the given game mode, filtering by team size.
     */
    private List<IArena> getCandidateArenas(final EGameMode gameMode) {
        final List<IArena> result = new ArrayList<>();
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (!arena.isEnabled()) {
                continue;
            }
            if (arena.getTeamSize() != gameMode.getTeamSize()) {
                continue;
            }
            result.add(arena);
        }
        return result;
    }
}
