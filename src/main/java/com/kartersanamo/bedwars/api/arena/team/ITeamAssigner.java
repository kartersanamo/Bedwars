package com.kartersanamo.bedwars.api.arena.team;

import com.kartersanamo.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Strategy interface for assigning players to teams when they join an arena.
 */
public interface ITeamAssigner {

    /**
     * Assigns the player to a team in the given arena, if possible.
     *
     * @return the team the player was assigned to, or empty if no suitable team was found
     */
    Optional<ITeam> assignPlayerToTeam(IArena arena, Player player);
}
