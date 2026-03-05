package com.kartersanamo.bedwars.api.arena;

import com.kartersanamo.bedwars.api.arena.generator.IGenerator;
import com.kartersanamo.bedwars.api.arena.team.ITeam;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * Records a kill for the given killer (for end-of-game summary). Call when a player gets a kill.
     */
    void recordKill(UUID killerUniqueId);

    /**
     * Records a final kill for the given killer (this game only). Call when a player gets a final kill.
     */
    void recordFinalKill(UUID killerUniqueId);

    /**
     * Returns this game's kill count for the player (0 if not in game or no kills).
     */
    int getKillsThisGame(UUID playerUniqueId);

    /**
     * Returns this game's final kill count for the player (0 if not in game or none).
     */
    int getFinalKillsThisGame(UUID playerUniqueId);

    /**
     * Returns this game's beds broken count for the player (0 if not in game or none).
     */
    int getBedsBrokenThisGame(UUID playerUniqueId);

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

    /**
     * Broadcasts the game-over summary (Bed Wars title, winner, top killers) to all players and spectators.
     * Call when the game transitions to ENDING.
     *
     * @param winningTeam the last remaining team, or null if draw
     */
    void broadcastGameOverSummary(ITeam winningTeam);

    /** Current diamond generator tier (1, 2, or 3). */
    int getDiamondTier();

    /** Current emerald generator tier (1, 2, or 3). */
    int getEmeraldTier();

    /** Effective spawn interval in ticks for diamond generators at current tier. */
    int getEffectiveDiamondIntervalTicks();

    /** Effective spawn interval in ticks for emerald generators at current tier. */
    int getEffectiveEmeraldIntervalTicks();

    /** Next tier upgrade line for scoreboard (e.g. "Diamond II in 5:36"); updates every second. */
    String getNextTierUpgradeMessage();
}
