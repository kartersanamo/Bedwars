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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public final class GameModeGuiListener implements Listener {

    private final Bedwars plugin;

    public GameModeGuiListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(GameModeGui.TITLE)) return;

        event.setCancelled(true);

        final ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        final EGameMode mode = modeFromItem(clicked);
        if (mode == null) return;

        if (event.getClick() == ClickType.RIGHT) {
            player.sendMessage(ChatColor.AQUA + "Map selection is not implemented yet.");
            return;
        }

        // Left-click: join the appropriate arena for this mode.
        // Ensure they aren't already in a game
        final IArena currentArena = plugin.getArenaManager().getArena(player);
        if (currentArena != null) {
            player.sendMessage(ChatColor.RED + "You are already in a Bedwars arena. Use /bedwars leave first.");
            return;
        }

        final Optional<IArena> best = findBestArenaForMode(mode);
        if (best.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No arenas are available for " + mode.getDisplayName() + ".");
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
        final String joinMessage = ChatColor.GRAY + player.getName() + ChatColor.YELLOW + " has joined "
                + "(" + ChatColor.AQUA + current + ChatColor.YELLOW + "/" + ChatColor.AQUA + max + ChatColor.YELLOW + ")!";
        for (Player other : arena.getPlayers()) {
            other.sendMessage(joinMessage);
        }

        arena.tryStartCountdown();
        player.closeInventory();
    }

    private Optional<IArena> findBestArenaForMode(final EGameMode mode) {
        // Store the current "best" arena
        IArena bestWaiting = null;
        int bestWaitingPlayers = -1;

        IArena emptyLobby = null;

        // Loop over every arena to find the best one
        for (IArena arena : plugin.getArenaManager().getArenas()) {
            if (!arena.isEnabled()) continue;
            if (arena.getTeamSize() != mode.getTeamSize()) continue;

            final EGameState state = arena.getGameState();
            final int size = arena.getPlayers().size();

            if ((state == EGameState.LOBBY_WAITING || state == EGameState.STARTING)
                    && size > 0 && size < arena.getMaxPlayers()) {
                // New best arena
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

    private EGameMode modeFromItem(final ItemStack item) {
        if (!item.hasItemMeta() || !Objects.requireNonNull(item.getItemMeta()).hasDisplayName()) return null;
        final String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        for (EGameMode mode : EGameMode.values()) {
            if (mode.getDisplayName().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }
}

