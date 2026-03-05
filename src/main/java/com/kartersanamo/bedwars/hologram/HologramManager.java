package com.kartersanamo.bedwars.hologram;

import com.kartersanamo.bedwars.Bedwars;
import com.kartersanamo.bedwars.api.arena.EGameState;
import com.kartersanamo.bedwars.api.arena.generator.EGeneratorType;
import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.arena.OreGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.*;

/**
 * Manages holograms above diamond and emerald generators.
 */
public final class HologramManager {

    private final Bedwars plugin;
    private final Map<IGenerator, List<ArmorStand>> hologramsByGenerator = new HashMap<>();
    private final Map<IGenerator, ArmorStand> rotatingBlockByGenerator = new HashMap<>();

    public HologramManager(final Bedwars plugin) {
        this.plugin = plugin;
    }

    public void update(final long currentTick) {
        plugin.getArenaManager().getArenas().forEach(arena -> {
            if (arena.getGameState() != EGameState.IN_GAME) {
                return;
            }
            for (IGenerator generator : arena.getGenerators()) {
                final EGeneratorType type = generator.getType();
                if (type != EGeneratorType.DIAMOND && type != EGeneratorType.EMERALD) {
                    continue;
                }

                final List<ArmorStand> stands = hologramsByGenerator.computeIfAbsent(generator, g -> createHologram(g));
                updateHologramLines(stands, generator, currentTick);
                final ArmorStand rotatingBlock = rotatingBlockByGenerator.computeIfAbsent(generator, this::createRotatingBlock);
                if (rotatingBlock != null && !rotatingBlock.isDead()) {
                    final double angleDeg = (currentTick * 4) % 360;
                    rotatingBlock.setHeadPose(new EulerAngle(0, Math.toRadians(angleDeg), 0));
                }
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
        for (ArmorStand stand : rotatingBlockByGenerator.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        rotatingBlockByGenerator.clear();
    }

    private List<ArmorStand> createHologram(final IGenerator generator) {
        final Location base = generator.getLocation();
        final World world = base.getWorld();
        if (world == null) {
            return Collections.emptyList();
        }

        final List<ArmorStand> result = new ArrayList<>(3);
        final double x = base.getBlockX() + 0.5;
        final double y = base.getBlockY() + 4.2;
        final double z = base.getBlockZ() + 0.5;

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

    private ArmorStand createRotatingBlock(final IGenerator generator) {
        final Location base = generator.getLocation();
        final World world = base.getWorld();
        if (world == null) {
            return null;
        }
        final EGeneratorType type = generator.getType();
        final Material block = type == EGeneratorType.EMERALD ? Material.EMERALD_BLOCK : Material.DIAMOND_BLOCK;
        final double x = base.getBlockX() + 0.5;
        final double y = base.getBlockY() + 2.5;
        final double z = base.getBlockZ() + 0.5;
        final ArmorStand stand = world.spawn(new Location(world, x, y, z), ArmorStand.class, armorStand -> {
            armorStand.setMarker(true);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setBasePlate(false);
            armorStand.setSmall(true);
            armorStand.setHelmet(new ItemStack(block));
        });
        return stand;
    }

    private void updateHologramLines(final List<ArmorStand> stands, final IGenerator generator, final long currentTick) {
        if (stands.isEmpty() || !(generator instanceof OreGenerator ore)) {
            return;
        }

        final EGeneratorType type = generator.getType();
        final int tier = type == EGeneratorType.EMERALD
                ? generator.getArena().getEmeraldTier()
                : generator.getArena().getDiamondTier();
        final String tierRoman = toRoman(tier);

        long interval = type == EGeneratorType.EMERALD
                ? generator.getArena().getEffectiveEmeraldIntervalTicks()
                : generator.getArena().getEffectiveDiamondIntervalTicks();
        if (interval <= 0L) {
            interval = 20L;
        }
        final long lastDrop = ore.getLastDropTick();
        // Time until next drop: from last drop (or full interval if never dropped)
        final long elapsed = lastDrop == 0L ? 0L : (currentTick - lastDrop);
        final long remainingTicks = lastDrop == 0L ? interval : Math.max(0L, interval - elapsed);
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

