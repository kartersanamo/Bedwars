package com.kartersanamo.bedwars.slimejumps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public class SlimeJump {
    private final String name;
    private final double multiplier;
    private final Location location;

    private final int radius = 2;

    public SlimeJump(String name, Location location, double multiplier) {
        this.name = name;
        this.location = location;
        this.multiplier = multiplier;
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Location playerLoc = player.getLocation();
        Location blockBelow = playerLoc.clone().subtract(0, 1, 0);

        if (isNear(blockBelow, location)) {

            // Get direction player is facing
            org.bukkit.util.Vector direction = player.getLocation()
                    .getDirection()
                    .setY(0)          // remove vertical component
                    .normalize();

            // Apply boost strength
            double forwardBoost = multiplier;
            double upBoost = 0.8; // tweak this

            org.bukkit.util.Vector velocity = direction.multiply(forwardBoost);
            velocity.setY(upBoost);

            player.setVelocity(velocity);
        }
    }

    private boolean isNear(Location a, Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.distanceSquared(b) <= radius * radius;
    }
}