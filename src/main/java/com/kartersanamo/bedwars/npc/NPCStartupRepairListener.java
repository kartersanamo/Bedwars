package com.kartersanamo.bedwars.npc;

import com.kartersanamo.bedwars.Bedwars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps trying to map persisted NPC entities after chunks/worlds become active.
 * <p>
 * At plugin enable, lobby chunks often are not loaded yet, so the first scan sees 0 NPCs.
 * This listener schedules a debounced {@link NPCManager#repairRuntimeMappings()} whenever
 * a player joins while no NPCs are mapped, so the same fix as {@code /bw npc repair} runs
 * automatically without admin action.
 */
public final class NPCStartupRepairListener implements Listener {

    private static final long JOIN_REPAIR_DELAY_TICKS = 60L;
    /** Minimum wall time between join-triggered repair schedules (coalesce rapid joins). */
    private static final long DEBOUNCE_MS = 2000L;

    private final Bedwars plugin;
    private BukkitTask pendingJoinRepair;
    private long lastJoinRepairScheduleMs;

    public NPCStartupRepairListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (plugin.getNpcManager() == null) {
            return;
        }
        if (!plugin.getNpcManager().getAllNPCs().isEmpty()) {
            return;
        }
        final long nowMs = System.currentTimeMillis();
        if (nowMs - lastJoinRepairScheduleMs < DEBOUNCE_MS) {
            return;
        }
        lastJoinRepairScheduleMs = nowMs;

        if (pendingJoinRepair != null) {
            pendingJoinRepair.cancel();
            pendingJoinRepair = null;
        }

        final Player player = event.getPlayer();
        pendingJoinRepair = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingJoinRepair = null;
            if (plugin.getNpcManager() == null) {
                return;
            }
            if (!plugin.getNpcManager().getAllNPCs().isEmpty()) {
                return;
            }
            final int after = plugin.getNpcManager().repairRuntimeMappings();
            if (after > 0) {
                plugin.getLogger().info("NPC auto-repair after join by " + player.getName() + " — loaded " + after + " NPC(s).");
            }
        }, JOIN_REPAIR_DELAY_TICKS);
    }
}
