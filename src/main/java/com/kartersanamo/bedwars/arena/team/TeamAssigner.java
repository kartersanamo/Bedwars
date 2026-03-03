package com.kartersanamo.bedwars.arena.team;

import com.kartersanamo.bedwars.api.arena.IArena;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import com.kartersanamo.bedwars.api.arena.team.ITeamAssigner;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Optional;

/**
 * Default team assignment strategy: fills the smallest team first.
 */
public final class TeamAssigner implements ITeamAssigner {

    @Override
    public Optional<ITeam> assignPlayerToTeam(final IArena arena, final Player player) {
        return arena.getTeams()
                .stream()
                .min(Comparator.comparingInt(team -> team.getMemberIds().size()))
                .map(team -> {
                    team.addPlayer(player);
                    return team;
                });
    }
}
