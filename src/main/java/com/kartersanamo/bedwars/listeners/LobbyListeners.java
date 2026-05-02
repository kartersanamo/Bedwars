package com.kartersanamo.bedwars.listeners;

import com.kartersanamo.bedwars.Bedwars;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Objects;

public class LobbyListeners implements Listener {

    private boolean isLobbyWorld(World world) {
        return world.getName().equals(
                Bedwars.getInstance().getMainConfig().getLobbyWorldName()
        );
    }

    private boolean inLobby(Player player) {
        return isLobbyWorld(player.getWorld());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!inLobby(player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.teleport(player.getWorld().getSpawnLocation());
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!inLobby(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!inLobby(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!inLobby(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!inLobby(event.getPlayer())) return;
        if (event.getClickedBlock() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!inLobby(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!inLobby(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFluid(BlockFromToEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!isLobbyWorld(Objects.requireNonNull(event.getLocation().getWorld()))) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!inLobby(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!inLobby(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!inLobby(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isLobbyWorld(Objects.requireNonNull(event.getLocation().getWorld()))) return;
        event.blockList().clear();
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (!isLobbyWorld(event.getWorld())) return;
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent event) {
        if (!isLobbyWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }



}
