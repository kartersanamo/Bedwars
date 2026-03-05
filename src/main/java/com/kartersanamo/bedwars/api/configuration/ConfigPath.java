package com.kartersanamo.bedwars.api.configuration;

/**
 * Centralised configuration path constants used across the plugin.
 * <p>
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

        public static final String GENERATOR_CAPS_SOLO_IRON = "gameplay.generator-caps.solo.iron";
        public static final String GENERATOR_CAPS_SOLO_GOLD = "gameplay.generator-caps.solo.gold";
        public static final String GENERATOR_CAPS_SOLO_DIAMOND = "gameplay.generator-caps.solo.diamond";
        public static final String GENERATOR_CAPS_SOLO_EMERALD = "gameplay.generator-caps.solo.emerald";

        public static final String GENERATOR_CAPS_DOUBLES_IRON = "gameplay.generator-caps.doubles.iron";
        public static final String GENERATOR_CAPS_DOUBLES_GOLD = "gameplay.generator-caps.doubles.gold";
        public static final String GENERATOR_CAPS_DOUBLES_DIAMOND = "gameplay.generator-caps.doubles.diamond";
        public static final String GENERATOR_CAPS_DOUBLES_EMERALD = "gameplay.generator-caps.doubles.emerald";

        public static final String GENERATOR_CAPS_THREES_IRON = "gameplay.generator-caps.threes.iron";
        public static final String GENERATOR_CAPS_THREES_GOLD = "gameplay.generator-caps.threes.gold";
        public static final String GENERATOR_CAPS_THREES_DIAMOND = "gameplay.generator-caps.threes.diamond";
        public static final String GENERATOR_CAPS_THREES_EMERALD = "gameplay.generator-caps.threes.emerald";

        public static final String GENERATOR_CAPS_FOURS_IRON = "gameplay.generator-caps.fours.iron";
        public static final String GENERATOR_CAPS_FOURS_GOLD = "gameplay.generator-caps.fours.gold";
        public static final String GENERATOR_CAPS_FOURS_DIAMOND = "gameplay.generator-caps.fours.diamond";
        public static final String GENERATOR_CAPS_FOURS_EMERALD = "gameplay.generator-caps.fours.emerald";

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

        /** Tier upgrade event times (seconds from game start). Order: Diamond II, Emerald II, Diamond III, Emerald III, Bed Break, Sudden Death, Game Over. */
        public static final String TIER_UPGRADE_DIAMOND_2 = "tier-upgrades.diamond-2-seconds";
        public static final String TIER_UPGRADE_EMERALD_2 = "tier-upgrades.emerald-2-seconds";
        public static final String TIER_UPGRADE_DIAMOND_3 = "tier-upgrades.diamond-3-seconds";
        public static final String TIER_UPGRADE_EMERALD_3 = "tier-upgrades.emerald-3-seconds";
        public static final String TIER_UPGRADE_BED_BREAK = "tier-upgrades.bed-break-seconds";
        public static final String TIER_UPGRADE_SUDDEN_DEATH = "tier-upgrades.sudden-death-seconds";
        public static final String TIER_UPGRADE_GAME_OVER = "tier-upgrades.game-over-seconds";

        /** Interval ticks per tier (diamond/emerald). Tier 2/3 use these; tier 1 uses base interval. */
        public static final String DIAMOND_TIER_2_INTERVAL_TICKS = "diamond.tier-2-interval-ticks";
        public static final String DIAMOND_TIER_3_INTERVAL_TICKS = "diamond.tier-3-interval-ticks";
        public static final String EMERALD_TIER_2_INTERVAL_TICKS = "emerald.tier-2-interval-ticks";
        public static final String EMERALD_TIER_3_INTERVAL_TICKS = "emerald.tier-3-interval-ticks";

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
        public static final String TEAM_UPGRADE_NPC = "upgrade-npc";

        public static final String DIAMOND_GENERATORS = "diamond-generators";
        public static final String EMERALD_GENERATORS = "emerald-generators";

        public static final String ARENA_REGION = "arena-region";
        public static final String ARENA_REGION_POS1 = "arena-region.pos1";
        public static final String ARENA_REGION_POS2 = "arena-region.pos2";

        public static final String LOBBY_REGION = "lobby-region";
        public static final String LOBBY_REGION_POS1 = "lobby-region.pos1";
        public static final String LOBBY_REGION_POS2 = "lobby-region.pos2";

        private Arena() {
        }
    }
}
