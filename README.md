## Bedwars

A modern, configurable BedWars minigame for Spigot/Paper 1.21, written in Java 21 with a local SQLite database for persistent player statistics.

### Features (MVP)

- **Full game loop**
  - Join from a lobby using `/bedwars join`
  - Lobby waiting + countdown
  - Teleport to team islands on start with starting gear
  - Resource generators (iron, gold, diamond, emerald)
  - Bed destruction, respawns, and final kills
  - Automatic win detection and victory sequence
  - Arena reset between games using snapshot-based world restore

- **World & arena handling**
  - Uses a prebuilt void world containing all islands
  - Per-arena configuration via `arenas/<id>.yml`
  - Efficient arena reset by snapshotting an axis-aligned region and restoring only modified blocks

- **Teams**
  - Configurable teams with colors, spawns, beds, and shop NPCs
  - Simple team balancer (`TeamAssigner`) to keep teams even

- **Generators**
  - Iron & gold: team islands
  - Diamonds: side islands
  - Emeralds: center islands
  - Timings and item caps configured in `generators.yml`

- **Shop (MVP)**
  - Simple in‑game shop GUI
  - Opened by interacting with shop villagers
  - Example items (e.g., wool blocks) with iron cost
  - Designed so more categories/items can be added later

- **Sidebar UI**
  - Per-player scoreboard sidebar
  - Shows arena name, player count, and bed status per team

- **Statistics (SQLite)**
  - Local `player_stats` table using `sqlite-jdbc`
  - Tracks wins, games played, kills, deaths (extendable to beds broken/final kills)
  - Cached in memory during runtime and flushed safely to disk

---

## Requirements

- **Server**: Spigot or Paper 1.21.x or compatible ([Spigot API docs][spigot-api])
- **Java**: Java 21 ([Java 21 API][java21])
- **Database**: SQLite (embedded via `sqlite-jdbc`, schema created automatically) ([SQLite docs][sqlite-docs])

[spigot-api]: https://hub.spigotmc.org/javadocs/spigot/
[java21]: https://docs.oracle.com/en/java/javase/21/docs/api/index.html
[sqlite-docs]: https://sqlite.org/docs.html

---

## Installation

1. **Build the plugin**

   ```bash
   mvn clean package
   ```

   The shaded plugin JAR will be created under `target/`.

2. **Install on your server**

   - Copy the JAR from `target/` into your server’s `plugins/` folder.
   - Start (or restart) the server.
   - The plugin will generate default configuration files in `plugins/Bedwars/`.

3. **Verify startup**

   - Check the console for `Bedwars` startup logs.
   - If no arenas are configured, you will see a warning about missing arenas.

---

## Configuration

All configuration files live under `plugins/Bedwars/`:

- `config.yml` – global settings:
  - Lobby world and optional lobby spawn location
  - Default min/max players and team size per arena
  - Respawn delay, void Y‑level
  - Database (SQLite file name) and debug flag

- `generators.yml` – generator timings and caps:
  - `iron.interval-ticks`, `iron.max-items`
  - `gold.interval-ticks`, `gold.max-items`
  - `diamond.interval-ticks`, `diamond.max-items`
  - `emerald.interval-ticks`, `emerald.max-items`

- `sounds.yml` – logical sounds (bed destroyed, kills, countdown, start, victory, defeat, generator pickup)

### Arena configuration

Arenas are defined one per file in `plugins/Bedwars/arenas/`, for example:

```yaml
world: bedwars_world
display-name: "Example Arena"
enabled: true

min-players: 2
max-players: 16
team-size: 4

lobby-spawn:
  world: bedwars_world
  x: 0.5
  y: 65.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

spectator-spawn:
  world: bedwars_world
  x: 0.5
  y: 90.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

teams:
  RED:
    color: RED
    spawn:
      world: bedwars_world
      x: 32.5
      y: 65.0
      z: 0.5
      yaw: 180.0
      pitch: 0.0
    bed:
      world: bedwars_world
      x: 32.0
      y: 65.0
      z: 0.0
      yaw: 0.0
      pitch: 0.0
    generator-iron:
      iron1:
        world: bedwars_world
        x: 32.5
        y: 64.0
        z: 4.5
        yaw: 0.0
        pitch: 0.0
    generator-gold:
      gold1:
        world: bedwars_world
        x: 32.5
        y: 64.0
        z: 3.5
        yaw: 0.0
        pitch: 0.0
    shop-npc:
      world: bedwars_world
      x: 28.5
      y: 65.0
      z: 0.5
      yaw: 90.0
      pitch: 0.0

  BLUE:
    color: BLUE
    # ... same structure as RED team

diamond-generators:
  d1:
    world: bedwars_world
    x: 0.5
    y: 65.0
    z: 32.5
    yaw: 0.0
    pitch: 0.0
  d2:
    world: bedwars_world
    x: 0.5
    y: 65.0
    z: -32.5
    yaw: 0.0
    pitch: 0.0

emerald-generators:
  e1:
    world: bedwars_world
    x: 0.5
    y: 65.0
    z: 8.5
    yaw: 0.0
    pitch: 0.0
  e2:
    world: bedwars_world
    x: 0.5
    y: 65.0
    z: -8.5
    yaw: 0.0
    pitch: 0.0

arena-region:
  pos1:
    world: bedwars_world
    x: -64
    y: 0
    z: -64
    yaw: 0.0
    pitch: 0.0
  pos2:
    world: bedwars_world
    x: 64
    y: 128
    z: 64
    yaw: 0.0
    pitch: 0.0
```

Make sure:

- The `world` exists and is a void world with islands.
- Each team has valid `spawn` and `bed` coordinates.
- `arena-region` fully encloses all playable blocks.

---

## Commands

- **`/bedwars join [arena]`**
  - Join a specific arena or the best available one.
  - Permission: `bedwars.join`

- **`/bedwars leave`**
  - Leave the current arena and return to the lobby.
  - Permission: `bedwars.leave`

- **`/bedwars start`**
  - Force start the arena you are in (skips the countdown).
  - Permission: `bedwars.admin.start`

(Additional admin and utility commands can be added later as needed.)

---

## Database

- The plugin creates `bedwars.db` (configurable) under `plugins/Bedwars/`.
- Table: `player_stats`
  - `uuid TEXT PRIMARY KEY`
  - `name TEXT`
  - `wins`, `losses`, `kills`, `deaths`, `final_kills`, `beds_broken`, `games_played` (all `INTEGER NOT NULL DEFAULT 0`)
- Stats are cached in memory while the server is running and flushed:
  - At the end of each game.
  - On plugin disable.

You can inspect the DB with any SQLite client:

```bash
sqlite3 plugins/Bedwars/bedwars.db
```

---

## Building from source

1. Install Java 21 and Maven.
2. Clone this repository.
3. Run:

   ```bash
   mvn clean package
   ```

4. Take the resulting JAR from `target/` and place it into your server’s `plugins/` directory.

---

## License

This project is licensed under the MIT License. See `LICENSE` for details.
