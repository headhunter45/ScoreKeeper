# ScoreKeeper — Plugin Integration & Event/Command Reference

> Analysis for the ScoreKeeper codebase. Scope: how the plugin hooks into the
> Minecraft (Paper) server runtime — event listeners, command registrations,
> scheduled/tick logic, and inter-plugin/API surface — with a table mapping each
> trigger → handler class → effect on score data.
>
> Platform: **Paper** (`io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT`), Java 21,
> Gradle. Main class: `com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin`.

---

## 1. TL;DR for a new developer

ScoreKeeper is almost entirely **command-driven**. It registers **no Bukkit event
listeners**, declares **no `@EventHandler` methods, no `Listener` implementations,
and calls `getPluginManager().registerEvents(...)` nowhere**. It also schedules
**no timed tasks** (no `BukkitScheduler`/`BukkitRunnable`/tick logic).

The only things that touch score state are **five console/chat commands**
(`score-get`, `score-add`, `score-subtract`, `score-reset`, `score-archive`).
Score state lives in an **in-memory `HashMap<UUID, Integer>`** and is **not
persisted** — `onEnable`/`onDisable` have `TODO` stubs, so data is lost on restart.

There is a **`highscore` / high-score table in the project name and the task
prompt, but no such command or logic exists yet** — it is unimplemented.

---

## 2. Event listeners (Bukkit/Adventure)

**None.** The plugin listens to no server events. Confirmed by absence of any of:
`@EventHandler`, `implements Listener`, `registerEvents(`, `PlayerJoinEvent`,
`PlayerQuitEvent`, `PlayerDeathEvent`, or any other event import/registration.

| Server event | Handler | Effect on score data |
|---|---|---|
| PlayerJoin | *none* | none |
| PlayerQuit | *none* | none |
| PlayerDeath | *none* | none |
| (all other Bukkit events) | *none* | none |
| Custom/plugin-injected events | *none* | none |

**Implication:** nothing about a player's score changes as a result of gameplay
events (joining, quitting, dying, scoring points in-game, etc.). The only way
score data changes is through the commands in §3.

---

## 3. Command registrations & permissions

Commands are declared in `src/main/resources/plugin.yml` (no `aliases`, no
`permissions:` block, and no per-command `permission:` — so **every command is
available to every sender with no permission gating**). Each is wired to an
executor in `ScoreKeeperPlugin.onEnable()`.

Argument-resolution idiom shared by every handler:
`boolean rcon = !(sender instanceof Player);` — an RCON caller has no self
context, so the "target = self" shortcut is unavailable and a `<playerName>`
must be supplied.

`<playerName>` is resolved with `server.getPlayerExact(name)` — an **exact,
case-sensitive lookup of currently-online players only**. Score operations
therefore apply to **online players**, and an unknown/offline name yields a
"Can't find a player with that name" error (except where noted).

### Reference table — command → handler class → effect on score data

| Trigger (command) | Usage | Handler class | Delegates to | Effect on score data |
|---|---|---|---|---|
| `score-get` | `/score-get [player]` | `ScoreGetCommand` | `ScoreKeeperPlugin.getScore(Player)` → `getPlayerScore` | **Read only.** Returns the player's current score. Side effect: lazily registers the player at `0` if unseen. No persistent change. |
| `score-add` | `/score-add [player] <amount>` | `ScoreAddCommand` | `ScoreKeeperPlugin.addScore(Player,int)` | `score = old + amount` (integer `amount` in `[1..]`). |
| `score-subtract` | `/score-subtract [player] <amount>` | `ScoreSubtractCommand` | `ScoreKeeperPlugin.subtractScore(Player,int)` | `score = old - amount` (can go negative). |
| `score-reset` | `/score-reset [player]` | `ScoreResetCommand` | `ScoreKeeperPlugin.resetScore(Player)` | `score = 0` (writes 0; key created if absent). |
| `score-archive` | `/score-archive [player]` | `ScoreArchiveCommand` | *nothing* | **No state change.** Handler prints `"archive command unimplemented"`. It does **not** call `ScoreKeeperPlugin.archiveScore(...)` — see §5. |

### Command behaviour details

- **Sender vs. target.** Single-arg form means "operate on the invoking player"
  (`split.length == 1` → `targetPlayer = sender`). Two-arg form means
  `<playerName> <amount>` (add/subtract) or `<playerName>` (get/reset). For an
  RCON sender, the single-arg form is treated as missing a target and prints
  usage instead.
- **Amount parsing.** `score-add`/`score-subtract` require an integer
  `amount`; a non-integer prints `"amount must be an integer"`.
- **Error/usage messaging.** `echoError` (red) and `echoUsage` (colour-coded)
  differ between player and RCON call sites (`/score-add` vs `score-add`).
  All handlers `return true` (command handled).
- **Permissions.** None declared → no operator/permission requirement; any
  player or RCON can run them.

---

## 4. Scheduled tasks / tick-based logic

**None.** Confirmed by absence of `getScheduler()`, `runTask`,
`BukkitRunnable`, `BukkitTask`, `scheduleSync*`, or any periodic/repeating
logic. ScoreKeeper runs no background or tick-driven work.

---

## 5. Inter-plugin dependencies & API exposure

- **Declared dependencies:** plugin.yml has no `depend`, `softdepend`, `load`,
  or `load-before` → **no declared inter-plugin coupling**.
- **Build dependency:** the only third-party dependency is
  `io.papermc.paper:paper-api` (marked `compileOnly`, and explicitly ignored as a
  snapshot by the release plugin). No other plugin/API is referenced.
- **Public API surface.** `ScoreKeeperPlugin` is a plain `JavaPlugin` exposing
  `public` methods that other plugins *could* call if they hold a reference:
  - `void addScore(Player, int)`
  - `void subtractScore(Player, int)`
  - `int getScore(Player)`
  - `void setScore(Player, int)` — **public but used by no command** (internal
    write path; the only writer not reachable via a command).
  - `void resetScore(Player)`
  - `void archiveScore(Player)` — **public but called by nothing**; it only logs
    a warning. The `/score-archive` command does **not** invoke it.
  - `void sendMessage(CommandSender, Component)`, `logInfo/logWarning/logError`
- **No formal service registration.** There is no `registerService`/`asService`
  (ServiceLoader), no dedicated API artifact, and no `api:` block in plugin.yml.
  Exposure is by **public method surface only** — informal and not discoverable
  by other plugins.
- **Persistence (unimplemented).** `onEnable` logs a warning that load-from-file
  is unimplemented; `onDisable` logs that save-to-file is unimplemented. The
  `HashMap<UUID,Integer>` is the sole state store and is **forgotten on restart**.

---

## 6. Lifecycle

| Hook | What it does |
|---|---|
| `onEnable()` | Wires the five `score-*` command executors; logs the "load not implemented" warning and an enable log line. |
| `onDisable()` | Logs the "save not implemented" warning. **All in-memory scores are lost on shutdown.** |

---

## 7. State model (quick reference)

- **Store:** `private HashMap<UUID,Integer> _playerScores` on `ScoreKeeperPlugin`
  (one map field for the whole plugin).
- **Keying:** by `Player.getUniqueId()` (UUID), so scores are per-player and
  survive name changes *within a single server run*; lost on restart.
- **Lazy init:** any read (including `get`/`score-get`) auto-creates the key at `0`.
- **Writers:** `addScore`, `subtractScore`, `resetScore`, `setScore`, all funneled
  through `setPlayerScore(player, value)`.
- **Concurrency:** plain `HashMap`; safe today only because command dispatch
  runs on the server's main thread. Not thread-safe for off-thread use.

---

## 8. "Which server trigger causes which state change" — summary

| Trigger | State change |
|---|---|
| `/score-add [player] <amount>` | `_playerScores[uuid] += amount` |
| `/score-subtract [player] <amount>` | `_playerScores[uuid] -= amount` |
| `/score-reset [player]` | `_playerScores[uuid] = 0` |
| `/score-get [player]` | none (reads; lazily registers at 0) |
| `/score-archive [player]` | none (not implemented) |
| Any Minecraft/Bukkit event (join/quit/death/etc.) | **none — no listeners exist** |
| Server tick / scheduled task | **none — no scheduler exists** |
| Server enable | no data change (registration + log only) |
| Server disable | in-memory scores discarded (no persistence) |
| `addScore`/`subtractScore`/`setScore`/`archiveScore` via the public API | same table where called; only reachable by code holding a plugin reference, not by in-game events or commands (except add/subtract/reset which do have commands) |
