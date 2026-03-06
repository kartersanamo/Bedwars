package com.kartersanamo.bedwars.arena;

import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks players who disconnect from arenas so they can use /rejoin.
 * <p>
 * This manager is intentionally lightweight: it records which arena the player
 * was in and when they disconnected. All validation (bed destroyed, timeout,
 * game over) happens at rejoin time.
 */
public final class RejoinManager {

    public static final long OFFLINE_LIMIT_MILLIS = 2 * 60 * 1000L; // 2 minutes

    public static final class Entry {
        public final IArena arena;
        public final long disconnectedAt;

        private Entry(final IArena arena, final long disconnectedAt) {
            this.arena = arena;
            this.disconnectedAt = disconnectedAt;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public void recordDisconnect(final Player player, final IArena arena) {
        if (player == null || arena == null) {
            return;
        }
        entries.put(player.getUniqueId(), new Entry(arena, System.currentTimeMillis()));
    }

    public Entry getEntry(final Player player) {
        if (player == null) {
            return null;
        }
        return entries.get(player.getUniqueId());
    }

    public void clearEntry(final Player player) {
        if (player == null) {
            return;
        }
        entries.remove(player.getUniqueId());
    }
}

