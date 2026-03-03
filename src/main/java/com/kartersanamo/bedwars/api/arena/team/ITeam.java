package com.kartersanamo.bedwars.api.arena.team;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents a team inside an arena.
 */
public interface ITeam {

    String getId();

    ETeamColor getColor();

    Location getSpawnLocation();

    Location getBedLocation();

    boolean isBedDestroyed();

    void setBedDestroyed(boolean destroyed);

    boolean isEliminated();

    Collection<UUID> getMemberIds();

    Collection<Player> getOnlineMembers();

    void addPlayer(Player player);

    void removePlayer(Player player);

    boolean contains(Player player);
}
