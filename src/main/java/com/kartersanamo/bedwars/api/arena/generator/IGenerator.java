package com.kartersanamo.bedwars.api.arena.generator;

import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.Location;

/**
 * Represents a single resource generator inside an arena.
 */
public interface IGenerator {

    IArena getArena();

    EGeneratorType getType();

    Location getLocation();

    /**
     * Called periodically to advance the generator timer and optionally
     * spawn new items.
     *
     * @param currentTick global server tick or task iteration counter
     */
    void tick(long currentTick);
}
