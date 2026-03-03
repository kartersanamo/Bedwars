package com.kartersanamo.bedwars.api.arena;

public enum EGameState {
    /**
     * Waiting in lobby for enough players to start a countdown.
     */
    LOBBY_WAITING,

    /**
     * Countdown before the game actually starts.
     */
    STARTING,

    /**
     * Active gameplay.
     */
    IN_GAME,

    /**
     * Game finished, winners have been determined and celebrations are in progress.
     */
    ENDING,

    /**
     * Arena is restoring its world state and preparing for the next game.
     */
    RESETTING,

    /**
     * Arena is disabled and cannot be joined.
     */
    DISABLED
}
