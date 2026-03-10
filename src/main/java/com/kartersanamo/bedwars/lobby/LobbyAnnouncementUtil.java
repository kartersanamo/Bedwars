package com.kartersanamo.bedwars.lobby;

import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Shared waiting-lobby join/leave broadcast formatting.
 */
public final class LobbyAnnouncementUtil {

    private LobbyAnnouncementUtil() {
    }

    public static void broadcastJoin(final IArena arena, final Player player) {
        if (!isWaitingLobby(arena)) {
            return;
        }
        final int current = arena.getPlayers().size();
        final int max = arena.getMaxPlayers();
        final String message = ChatColor.GRAY + player.getName() + ChatColor.YELLOW + " has joined "
                + "(" + ChatColor.AQUA + current + ChatColor.YELLOW + "/" + ChatColor.AQUA + max + ChatColor.YELLOW + ")!";
        for (Player other : arena.getPlayers()) {
            other.sendMessage(message);
        }
    }

    public static void broadcastLeave(final IArena arena, final Player player) {
        if (!isWaitingLobby(arena)) {
            return;
        }
        final int current = arena.getPlayers().size();
        final int max = arena.getMaxPlayers();
        final String message = ChatColor.GRAY + player.getName() + ChatColor.YELLOW + " has left "
                + "(" + ChatColor.AQUA + current + ChatColor.YELLOW + "/" + ChatColor.AQUA + max + ChatColor.YELLOW + ")!";
        for (Player other : arena.getPlayers()) {
            other.sendMessage(message);
        }
    }

    private static boolean isWaitingLobby(final IArena arena) {
        final EGameState state = arena.getGameState();
        return state == EGameState.LOBBY_WAITING || state == EGameState.STARTING;
    }
}
