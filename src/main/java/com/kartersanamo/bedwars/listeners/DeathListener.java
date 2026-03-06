package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.database.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Handles player deaths inside arenas and delegates to arena logic.
 * Prevents death drops; gives victim's ores (iron, gold, diamond, emerald) to the killer.
 */
public final class DeathListener implements Listener {

    private static final Material[] ORE_MATERIALS = {
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.EMERALD
    };

    private final Bedwars plugin;

    public DeathListener(final Bedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final IArena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            return;
        }

        event.getDrops().clear();
        event.setDeathMessage(null);
        handleArenaDeath(plugin, player, player.getKiller(), arena);
    }

    /**
     * Performs full arena death handling: transfer ores to killer, arena death logic, stats.
     * Call from DeathListener (real death) or DamageListener (fake death when damage would be lethal).
     */
    public static void handleArenaDeath(final Bedwars plugin, final Player victim, final Player killer, final IArena arena) {
        if (killer != null && killer != victim && plugin.getArenaManager().getArena(killer) == arena) {
            transferOresToKiller(victim, killer);
        }
        arena.handlePlayerDeath(victim, killer);
        updateDeathStats(plugin, arena, victim, killer);
        if (killer != null && killer != victim) {
            arena.recordKill(killer.getUniqueId());
            final boolean finalKill = arena.getTeam(victim).map(ITeam::isBedDestroyed).orElse(false);
            if (finalKill) {
                arena.recordFinalKill(killer.getUniqueId());
            }
            broadcastKillMessage(arena, victim, killer);
        }
    }

    private static void broadcastKillMessage(final IArena arena, final Player victim, final Player killer) {
        final boolean finalKill = arena.getTeam(victim).map(ITeam::isBedDestroyed).orElse(false);
        ChatColor victimColor = ChatColor.WHITE;
        ChatColor killerColor = ChatColor.WHITE;
        if (arena.getTeam(victim).isPresent()) victimColor = arena.getTeam(victim).get().getColor().getChatColor();
        if (arena.getTeam(killer).isPresent()) killerColor = arena.getTeam(killer).get().getColor().getChatColor();
        final String message = victimColor + victim.getName() + ChatColor.GRAY + " was killed by " + killerColor + killer.getName() + "."
                + (finalKill ? " " + ChatColor.AQUA + ChatColor.BOLD + "FINAL KILL!" : "");
        for (Player p : arena.getPlayers()) {
            p.sendMessage(message);
        }
        for (Player p : arena.getSpectators()) {
            p.sendMessage(message);
        }
    }

    private static void updateDeathStats(final Bedwars plugin, final IArena arena, final Player victim, final Player killer) {
        try {
            final PlayerStats victimStats = plugin.getDatabase().getCachedStats(victim.getUniqueId(), victim.getName());
            victimStats.incrementDeaths();
            if (killer != null && killer != victim) {
                final PlayerStats killerStats = plugin.getDatabase().getCachedStats(killer.getUniqueId(), killer.getName());
                killerStats.incrementKills();
                final boolean finalKill = arena.getTeam(victim).map(ITeam::isBedDestroyed).orElse(false);
                if (finalKill) {
                    killerStats.incrementFinalKills();
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to update kill/death stats: " + ex.getMessage());
        }
    }

    private static void transferOresToKiller(final Player victim, final Player killer) {
        final List<ItemStack> ores = new ArrayList<>();
        final HashMap<Material, Integer> gained = new HashMap<>();

        for (int i = 0; i < victim.getInventory().getStorageContents().length; i++) {
            final ItemStack stack = victim.getInventory().getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            for (Material ore : ORE_MATERIALS) {
                if (stack.getType() == ore) {
                    ores.add(stack.clone());
                    victim.getInventory().setItem(i, null);
                    gained.merge(ore, stack.getAmount(), Integer::sum);
                    break;
                }
            }
        }

        final Location dropLocation = victim.getLocation();
        for (ItemStack stack : ores) {
            final HashMap<Integer, ItemStack> overflow = killer.getInventory().addItem(stack);
            for (ItemStack remainder : overflow.values()) {
                if (remainder != null && !remainder.getType().isAir()) {
                    Objects.requireNonNull(dropLocation.getWorld()).dropItemNaturally(dropLocation, remainder);
                }
            }
        }

        // Send per-ore gain messages to the killer, e.g. "+5 Iron".
        for (Material ore : ORE_MATERIALS) {
            final Integer amount = gained.get(ore);
            if (amount == null || amount <= 0) {
                continue;
            }
            final String oreName;
            final ChatColor nameColor;
            switch (ore) {
                case IRON_INGOT -> {
                    oreName = "Iron";
                    nameColor = ChatColor.WHITE;
                }
                case GOLD_INGOT -> {
                    oreName = "Gold";
                    nameColor = ChatColor.GOLD;
                }
                case DIAMOND -> {
                    oreName = "Diamond";
                    nameColor = ChatColor.AQUA;
                }
                case EMERALD -> {
                    oreName = "Emerald";
                    nameColor = ChatColor.GREEN;
                }
                default -> {
                    continue;
                }
            }
            killer.sendMessage(nameColor + "+" + amount + " " + oreName);
        }
    }
}
