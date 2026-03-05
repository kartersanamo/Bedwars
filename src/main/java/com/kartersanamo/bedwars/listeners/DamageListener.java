package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Ensures damage logic is only customised for players inside arenas.
 * Cancels all damage to players in the waiting lobby (LOBBY_WAITING or STARTING).
 * For in-game players: if damage would be lethal, cancels the death and runs arena death
 * (instant teleport to spectator, no death screen).
 */
public final class DamageListener implements Listener {

    private final Bedwars plugin;

    public DamageListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        final EGameState state = arena.getGameState();
        if (state == EGameState.LOBBY_WAITING || state == EGameState.STARTING) {
            event.setCancelled(true);
            return;
        }

        if (arena.hasSpawnProtection(player)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethalDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        if (arena.getGameState() != EGameState.IN_GAME && arena.getGameState() != EGameState.ENDING) {
            return;
        }

        final double healthAfter = player.getHealth() - event.getFinalDamage();
        if (healthAfter > 0) {
            return;
        }

        event.setCancelled(true);

        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player damager) {
                killer = damager;
            }
        }

        DeathListener.handleArenaDeath(plugin, player, killer, arena);
    }
}
