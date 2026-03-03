package com.kartersanamo.bedwars.api.sidebar;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Factory and lookup for {@link ISidebar} instances.
 */
public interface ISidebarManager {

    ISidebar createSidebar(Player player);

    Optional<ISidebar> getSidebar(Player player);

    void removeSidebar(Player player);
}
