package com.kartersanamo.bedwars.gui;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameMode;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
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
import java.util.Optional;

/**
 * Listener for Play Bedwars GUI interactions.
 */
public final class PlayBedwarsGuiListener implements Listener {

    private final Bedwars plugin;

    public PlayBedwarsGuiListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof PlayBedwarsGuiHolder holder)) return;

        event.setCancelled(true);

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        final EGameMode gameMode = holder.getGameMode();

        if (clicked.getType() == Material.RED_BED) {
            // Bed Wars item - join a game
            handleBedWarsClick(player, gameMode);
        } else if (clicked.getType() == Material.OAK_SIGN) {
            // Map Selector item - open map selector GUI
            MapSelectorGui.openFor(player, gameMode);
        } else if (clicked.getType() == Material.BARRIER) {
            // Close item
            player.closeInventory();
        }
    }

    /**
     * Handles clicking on the Bed Wars item (joins a game).
     */
    private void handleBedWarsClick(final Player player, final EGameMode gameMode) {
        // Ensure they aren't already in a game
        final IArena currentArena = plugin.getArenaManager().getArena(player);
        if (currentArena != null) {
            player.sendMessage(ChatColor.RED + "You are already in a Bedwars arena. Use /bedwars leave first.");
            return;
        }

        final Optional<IArena> best = findBestArenaForMode(gameMode);
        if (best.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No arenas are available for " + gameMode.getDisplayName() + ".");
            return;
        }

        final IArena arena = best.get();
        if (!arena.addPlayer(player)) {
            player.sendMessage(ChatColor.RED + "Could not join this arena.");
            return;
        }

        plugin.getArenaManager().playerJoinedArena(player, arena);

        // Give the waiting-lobby "Return to Lobby" item.
        LobbyReturnItem.giveTo(player);

        player.teleport(arena.getLobbySpawn());
        final int current = arena.getPlayers().size();
        final int max = arena.getMaxPlayers();
        final String joinMessage = ChatColor.WHITE + player.getName() + ChatColor.YELLOW + " has joined "
                + "(" + ChatColor.AQUA + current + ChatColor.YELLOW + "/" + ChatColor.AQUA + max + ChatColor.YELLOW + ")!";
        for (Player other : arena.getPlayers()) {
            other.sendMessage(joinMessage);
        }

        arena.tryStartCountdown();
        player.closeInventory();
    }

    /**
     * Finds the best arena for the given game mode.
     */
    private Optional<IArena> findBestArenaForMode(final EGameMode mode) {
        IArena bestWaiting = null;
        int bestWaitingPlayers = -1;

        IArena emptyLobby = null;

        for (IArena arena : getCandidateArenas(mode)) {
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
            return Optional.of(bestWaiting);
        }
        return Optional.ofNullable(emptyLobby);
    }

    private List<IArena> getCandidateArenas(final EGameMode mode) {
        final List<IArena> result = new ArrayList<>();
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (!arena.isEnabled()) {
                continue;
            }
            if (arena.getTeamSize() != mode.getTeamSize()) {
                continue;
            }
            result.add(arena);
        }
        return result;
    }
}
