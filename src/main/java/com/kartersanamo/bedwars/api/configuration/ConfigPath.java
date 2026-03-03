package com.kartersanamo.bedwars.api.configuration;

/**
 * Centralised configuration path constants used across the plugin.
 *
 * This avoids scattering raw string literals and keeps configuration structure
 * discoverable in a single place. Only paths that are used from Java code
 * should live here.
 */
public final class ConfigPath {

    private ConfigPath() {
    }

    /**
     * Paths for {@code config.yml}.
     */
    public static final class Main {
        public static final String LOBBY_WORLD = "lobby.world";
        public static final String LOBBY_SPAWN = "lobby.spawn";

        public static final String ARENA_MIN_PLAYERS = "arena.defaults.min-players";
        public static final String ARENA_MAX_PLAYERS = "arena.defaults.max-players";
        public static final String ARENA_TEAM_SIZE = "arena.defaults.team-size";

        public static final String GAME_RESPAWN_DELAY_SECONDS = "gameplay.respawn-delay-seconds";
        public static final String GAME_VOID_Y = "gameplay.void-y";

        public static final String DATABASE_TYPE = "database.type";
        public static final String DATABASE_SQLITE_FILE = "database.sqlite.file";

        public static final String DEBUG = "debug.enabled";

        private Main() {
        }
    }

    /**
     * Paths for {@code generators.yml}.
     */
    public static final class Generators {
        public static final String IRON_INTERVAL_TICKS = "iron.interval-ticks";
        public static final String IRON_MAX_ITEMS = "iron.max-items";

        public static final String GOLD_INTERVAL_TICKS = "gold.interval-ticks";
        public static final String GOLD_MAX_ITEMS = "gold.max-items";

        public static final String DIAMOND_INTERVAL_TICKS = "diamond.interval-ticks";
        public static final String DIAMOND_MAX_ITEMS = "diamond.max-items";

        public static final String EMERALD_INTERVAL_TICKS = "emerald.interval-ticks";
        public static final String EMERALD_MAX_ITEMS = "emerald.max-items";

        private Generators() {
        }
    }

    /**
     * Paths for {@code sounds.yml}.
     */
    public static final class Sounds {
        public static final String BED_DESTROYED = "bed-destroyed";
        public static final String KILL = "kill";
        public static final String FINAL_KILL = "final-kill";
        public static final String COUNTDOWN_TICK = "countdown-tick";
        public static final String GAME_START = "game-start";
        public static final String VICTORY = "victory";
        public static final String DEFEAT = "defeat";
        public static final String GENERATOR_PICKUP = "generator-pickup";

        private Sounds() {
        }
    }

    /**
     * Shared keys for arena configuration files in {@code arenas/<id>.yml}.
     */
    public static final class Arena {
        public static final String WORLD = "world";
        public static final String DISPLAY_NAME = "display-name";
        public static final String ENABLED = "enabled";

        public static final String MIN_PLAYERS = "min-players";
        public static final String MAX_PLAYERS = "max-players";
        public static final String TEAM_SIZE = "team-size";

        public static final String LOBBY_SPAWN = "lobby-spawn";
        public static final String SPECTATOR_SPAWN = "spectator-spawn";

        public static final String TEAMS = "teams";
        public static final String TEAM_COLOR = "color";
        public static final String TEAM_SPAWN = "spawn";
        public static final String TEAM_BED = "bed";
        public static final String TEAM_IRON_GENERATORS = "generator-iron";
        public static final String TEAM_GOLD_GENERATORS = "generator-gold";
        public static final String TEAM_SHOP_NPC = "shop-npc";

        public static final String DIAMOND_GENERATORS = "diamond-generators";
        public static final String EMERALD_GENERATORS = "emerald-generators";

        public static final String ARENA_REGION = "arena-region";
        public static final String ARENA_REGION_POS1 = "arena-region.pos1";
        public static final String ARENA_REGION_POS2 = "arena-region.pos2";

        private Arena() {
        }
    }
}
