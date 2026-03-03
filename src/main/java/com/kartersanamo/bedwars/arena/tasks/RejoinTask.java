package com.kartersanamo.bedwars.arena.tasks;

import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who disconnect during a game and allows them to rejoin
 * within a short configurable window.
 *
 * For the MVP this is intentionally minimal and only keeps track of players
 * at the time of their disconnect; the actual rejoin handling can be wired
 * into join listeners.
 */
public final class RejoinTask extends BukkitRunnable {

    private final Map<UUID, Long> expiryByPlayer = new ConcurrentHashMap<>();
    private final long rejoinWindowMillis;

    public RejoinTask(final long rejoinWindowMillis) {
        this.rejoinWindowMillis = rejoinWindowMillis;
    }

    public void markLeaver(final Player player) {
        expiryByPlayer.put(player.getUniqueId(), System.currentTimeMillis() + rejoinWindowMillis);
    }

    public boolean canRejoin(final Player player) {
        final Long expiry = expiryByPlayer.get(player.getUniqueId());
        return expiry != null && System.currentTimeMillis() <= expiry;
    }

    @Override
    public void run() {
        final long now = System.currentTimeMillis();
        expiryByPlayer.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
