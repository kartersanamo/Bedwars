package com.kartersanamo.bedwars.api.arena;

import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * High-level arena contract used by the rest of the plugin.
 */
public interface IArena {

    String getId();

    String getDisplayName();

    World getWorld();

    EGameState getGameState();

    void setGameState(EGameState gameState);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getMinPlayers();

    int getMaxPlayers();

    int getTeamSize();

    Location getLobbySpawn();

    Location getSpectatorSpawn();

    List<ITeam> getTeams();

    Optional<ITeam> getTeam(Player player);

    Collection<Player> getPlayers();

    Collection<Player> getSpectators();

    List<IGenerator> getGenerators();

    /**
     * Attempts to add the given player to this arena.
     *
     * @return true if the player was added, false if the arena was full or not joinable
     */
    boolean addPlayer(Player player);

    void removePlayer(Player player, boolean toLobby);

    boolean contains(Player player);

    /**
     * Starts or restarts the lobby countdown, if possible.
     */
    void tryStartCountdown();

    /**
     * Immediately starts the game, bypassing normal countdown conditions.
     */
    void forceStart();

    /**
     * Handles a player death inside this arena, triggering respawn or elimination.
     */
    void handlePlayerDeath(Player player, Player killer);

    /**
     * Returns whether the player currently has spawn protection (invulnerability after respawn).
     */
    boolean hasSpawnProtection(Player player);

    /**
     * Called when a team's bed has been destroyed.
     */
    void handleBedDestroyed(ITeam victimTeam, Player breaker);

    /**
     * Resets the arena back to the lobby state after a game finishes.
     */
    void resetAfterGame();
}
