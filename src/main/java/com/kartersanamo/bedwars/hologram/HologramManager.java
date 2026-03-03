package com.kartersanamo.bedwars.hologram;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.arena.OreGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

import java.util.*;

/**
 * Manages holograms above diamond and emerald generators.
 */
public final class HologramManager {

    private final Bedwars plugin;
    private final Map<IGenerator, List<ArmorStand>> hologramsByGenerator = new HashMap<>();

    public HologramManager(final Bedwars plugin) {
        this.plugin = plugin;
    }

    public void update(final long currentTick) {
        plugin.getArenaManager().getArenas().forEach(arena -> {
            for (IGenerator generator : arena.getGenerators()) {
                final EGeneratorType type = generator.getType();
                if (type != EGeneratorType.DIAMOND && type != EGeneratorType.EMERALD) {
                    continue;
                }

                final List<ArmorStand> stands = hologramsByGenerator.computeIfAbsent(generator, g -> createHologram(g));
                updateHologramLines(stands, generator, currentTick);
            }
        });
    }

    public void clearAll() {
        for (List<ArmorStand> stands : hologramsByGenerator.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
        hologramsByGenerator.clear();
    }

    private List<ArmorStand> createHologram(final IGenerator generator) {
        final Location base = generator.getLocation();
        final World world = base.getWorld();
        if (world == null) {
            return Collections.emptyList();
        }

        final List<ArmorStand> result = new ArrayList<>(3);
        final double x = base.getX() + 0.5;
        double y = base.getY() + 2.2;
        final double z = base.getZ() + 0.5;

        for (int i = 0; i < 3; i++) {
            final Location loc = new Location(world, x, y - (0.25 * i), z);
            final ArmorStand stand = world.spawn(loc, ArmorStand.class, armorStand -> {
                armorStand.setMarker(true);
                armorStand.setInvisible(true);
                armorStand.setGravity(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setSmall(true);
            });
            result.add(stand);
        }
        return result;
    }

    private void updateHologramLines(final List<ArmorStand> stands, final IGenerator generator, final long currentTick) {
        if (stands.isEmpty() || !(generator instanceof OreGenerator ore)) {
            return;
        }

        final EGeneratorType type = generator.getType();

        // TODO: hook into real tier progression. For now, default everything to Tier I.
        final int tier = 1;
        final String tierRoman = toRoman(tier);

        long interval = ore.getIntervalTicks();
        if (interval <= 0L) {
            interval = 20L;
        }
        long lastDrop = ore.getLastDropTick();
        long elapsed = lastDrop == 0L ? 0L : currentTick - lastDrop;
        long remainingTicks = Math.max(0L, interval - elapsed);
        final int remainingSeconds = (int) ((remainingTicks + 19L) / 20L);

        final String line1 = ChatColor.YELLOW + "Tier " + ChatColor.RED + tierRoman;
        final String line2;
        if (type == EGeneratorType.EMERALD) {
            line2 = ChatColor.DARK_GREEN.toString() + ChatColor.BOLD + "Emerald";
        } else {
            line2 = ChatColor.AQUA.toString() + ChatColor.BOLD + "Diamond";
        }
        final String line3 = ChatColor.YELLOW + "Spawns in " + ChatColor.RED + remainingSeconds
                + ChatColor.YELLOW + " seconds";

        if (stands.size() >= 1) {
            stands.get(0).setCustomName(line1);
        }
        if (stands.size() >= 2) {
            stands.get(1).setCustomName(line2);
        }
        if (stands.size() >= 3) {
            stands.get(2).setCustomName(line3);
        }
    }

    private String toRoman(final int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(value);
        };
    }
}

