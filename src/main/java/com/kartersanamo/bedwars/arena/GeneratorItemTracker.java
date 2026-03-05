package com.kartersanamo.bedwars.arena;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks dropped items that belong to generators. When an item is picked up or
 * removed (merge/despawn), the generator's count is decremented so it can spawn again.
 */
public final class GeneratorItemTracker implements Listener {

    private final JavaPlugin plugin;

    /** Item entity UUID -> generator that spawned it. */
    private final Map<UUID, OreGenerator> itemToGenerator = new ConcurrentHashMap<>();

    public GeneratorItemTracker(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a dropped item as belonging to the given generator. Call immediately after the generator spawns the item.
     */
    public void track(final OreGenerator generator, final Item item) {
        if (generator == null || item == null) return;
        itemToGenerator.put(item.getUniqueId(), generator);
        generator.registerDroppedItem(item.getUniqueId());
    }

    /**
     * Called when an item entity is no longer on the ground (picked up, merged, or despawned).
     */
    public void onItemRemoved(final UUID itemEntityId) {
        final OreGenerator generator = itemToGenerator.remove(itemEntityId);
        if (generator != null) {
            generator.onDroppedItemRemoved(itemEntityId);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        onItemRemoved(event.getItem().getUniqueId());
    }

    /**
     * Removes entries for item entities that no longer exist (merged or despawned).
     * Call periodically from a task.
     */
    public void cleanupRemovedEntities() {
        final Iterator<Map.Entry<UUID, OreGenerator>> it = itemToGenerator.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<UUID, OreGenerator> entry = it.next();
            final UUID itemId = entry.getKey();
            if (findEntity(itemId) == null) {
                it.remove();
                final OreGenerator gen = entry.getValue();
                if (gen != null) gen.onDroppedItemRemoved(itemId);
            }
        }
    }

    private org.bukkit.entity.Entity findEntity(final UUID uuid) {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid) && entity.isValid()) {
                    return entity;
                }
            }
        }
        return null;
    }
}
