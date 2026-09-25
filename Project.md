# ScoreKeeper — Project Reference

> **For a developer joining the team cold.** Read top-to-bottom in ~8 minutes;
> you'll know where to look and what's real vs. planned on day one.

---

## 1. What is ScoreKeeper?

ScoreKeeper is a **Paper Minecraft plugin** (Java 21, Bukkit/Adventure APIs) that manages player
score tracking. It exposes five in-game/console commands (`/score-get`, `/score-add`,
`/score-subtract`, `/score-reset`, `/score-archive`) for manual score manipulation — no events,
no timers, no automation.

**State of the project:** live in-memory scores work today; the \"high-score table\" advertised
in the README name and `/score-archive` description is **not implemented**. Persistence, ranking,
and scoring formulas are all **future scope**. See §4 for the gap between *what exists* and
*what is planned*.

---

## 2. Tech Stack & Build

| Item | Value |
|------|-------|
| Language | Java 21 (Gradle auto-downloads toolchain) |
| Runtime target | Paper 1.21.7 (Bukkit + Adventure APIs) |
| Build tool | Gradle 8.14.3 (`gradlew`) |
| Linting | Spotless (`googleJavaFormat()` + license header from `config/license-header.txt`) |
| Release | `net.researchgate.release` plugin; tags `v$version`; rejects snapshot deps except paper-api |
| External deps | **None at runtime.** `paper-api:1.21.7-R0.1-SNAPSHOT` is `compileOnly` only. |
| CI/release entry point | `./gradlew spotlessCheck build` then `./gradlew release` |

### Build quickly

```bash
./gradlew build          # compiles, runs tests (none yet)
./gradlew spotlessApply  # format to project style
./gradlew assemble       # produces jar in build/libs/
```

Drop the resulting jar onto a Paper server's `plugins/` directory.

---

## 3. Architecture Overview

### Directory layout

```
├── src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/
│   ├── ScoreKeeperPlugin.java        ← entry point, onEnable/onDisable, score CRUD core
│   └── commands/
│       ├── ScoreGetCommand.java      → /score-get [player]
│       ├── ScoreAddCommand.java      → /score-add [player] <amount>
│       ├── ScoreSubtractCommand.java → /score-subtract [player] <amount>
│       ├── ScoreResetCommand.java    → /score-reset [player]
│       └── ScoreArchiveCommand.java  → /score-archive [player] (stub)
├── src/main/resources/
│   └── plugin.yml                    ← command manifest + main-class declare
├── tools/bash/ & tools/powershell/  ← dev helper scripts
├── config/license-header.txt         ← Spotless license header
├── build.gradle                      ← Gradle build config (above)
├── gradle.properties                 ← version, group coordinates
├── CONTRIBUTING.md                   ← dev env setup
└── README.md                         ← user-facing command reference + notes
```

### Data flow (text diagram)

```
ADMIN / CONSOLE / RCON ──▶ types a /score-* command
                              │
                              ▼
                 ┌──────────────────────┐
                 │ 5× Score*Command.java│   executors parse args,
                 │ resolves target      │   resolvePlayerExact(name) (online-only)
                 └──────────┬───────────┘
                            │ calls
                            ▼
              ┌──────────────────────────────┐
              │  ScoreKeeperPlugin            │
              │                                │
              │  addScore / subtractScore     │  read-modify-write
              │  resetScore / setScore        │
              │  getScore (read)              │
              │  archiveScore (STUB — no-op) │
              └──────────┬───────────────────┘
                         │
                         ▼
               HashMap<UUID, Integer>   ← in RAM only
                 _playerScores          ← ScoreKeeperPlugin:38
```

**Key architectural facts:**

- **Single class owns everything.** `ScoreKeeperPlugin` holds the scores map, score CRUD
  methods, logging helpers (logWarning/logInfo/logError), and the Adventure component builder
  for chat color. There is no service layer or boundary separation.
- **No event listeners.** Zero `@EventHandler`/`Listener` registrations across the codebase.
  Game events (join, death, kill) do not affect scores. Only manual commands change state.
- **No scheduler or tick logic.** No periodic tasks, no countdowns, no automated scoring.
- **No permission nodes.** Every command is available to every sender (player, console, RCON).

### File: ScoreKeeperPlugin.java (`ScoreKeeperPlugin`)

- `main` class in `plugin.yml` (Bukkit plugin entry point)
- `HashMap<UUID, Integer> _playerScores` at line 38 — the sole score store
- `onEnable()` (line 52): wires five command executors, logs \"load not implemented\" warning
- `onDisable()` (line 46): logs \"save not implemented\" warning — scores lost on shutdown
- Score CRUD methods: `addScore`, `subtractScore`, `resetScore`, `setScore`, `getScore`,
  `archiveScore` (lines 67–91, 76–86, 72–74)
- Private helpers: `getPlayerScore(Player)` (get-or-create at 0), `setPlayerScore(Player,int)`

---

## 4. Domain Model & Scoring Logic

### 4.1 What exists today (REAL — in code)

| Aspect | Detail | Source |
|--------|--------|--------|
| Score model | Single `int` per player, keyed by UUID | `ScoreKeeperPlugin:38` |
| Storage container | `HashMap<UUID, Integer>` on plugin instance | same file |
| Default value | `0` — lazy-created on first map access | `getPlayerScore:100-106` |
| Live only | **No persistence.** Scores erased on server restart | `onEnable:59`, `onDisable:47` |
| Scoring direction | Any integer (negatives allowed, no floor) | add/subtract are raw `+`/`-` |
| Recording method | Manual commands only — `/score-add`, `/score-subtract` | command executors |
| Auto-scoring | **None** — no events, no timers | proven by grep across src/ |
| Permissions | **None declared.** All commands open to all senders | `plugin.yml`, no permission guard in code |

### 4.2 Open Score Lifecycle (what exists + what is planned)

```
Stage  1. First access — getPlayerScore() auto-creates key at 0   [REAL]
         2. Admin runs /score-add player N or /score-subtract       [REAL]
         3. Player accumulates points over the session             [REAL]
         4. Read via /score-get (read-only, lazily registers)      [REAL]
         5. Server restart — scores LOST on shutdown               [REAL]
         6. Intended: /score-archive freezes score → table        [GAP ✓ not built]
         7. Intended: high-score table display command            [GAP ✓ not built]
```

### 4.3 The "high-score table" — status

| Feature | Status | Details |
|---------|--------|---------|
| `/score-archive` | **Stub** | Command prints \"archive command unimplemented\"; `archiveScore()` method only logs, never writes a table or resets the player's score |
| Persistence (save) | **Not built** | `onDisable()` is a TODO stub — map discarded at shutdown |
| Persistence (load) | **Not built** | `onEnable()` is a TODO stub — map always starts empty `{}` |
| Sorting / ranking | **Not coded** | No sort, no tie-breaking, no entry cap, no decay logic exists |
| Scoring formulas | **Not coded** | Scores are plain integer accumulators (`Σ(adds) − Σ(subtracts)`) |

**Design decisions to be made (none answered by code today):**

- Storage format for the table (YAML per Bukkit convention; JSON? SQLite?)
- What an entry looks like (name + score + timestamp? name is not stored with score today)
- Sort order and tie-breaking strategy
- Max entries / leaderboard cap
- Whether `/score-archive` also resets the live score (README says it does)

---

## 5. Plugin Integration & Reference Table

### 5.1 Command → Handler → Effect on score data

| Trigger | Usage | Handler | Effect |
|---------|-------|---------|--------|
| `/score-get [player]` | Self or other | `ScoreGetCommand.java:38-84`, delegated to `getScore → getPlayerScore` | **Read-only.** Returns integer. Lazily creates entry at `0` if unseen. |
| `/score-add [player] <N>` | Self (omit name) or target others | `ScoreAddCommand.java` | `score += N`. Amount must parse as int. No direction validation (negative N still adds). |
| `/score-subtract [player] <N>` | Same | `ScoreSubtractCommand.java` | `score -= N`. No minimum clamping; negatives freely produced. |
| `/score-reset [player]` | Same | `ScoreResetCommand.java` | `score = 0`. Key created at `0` if absent. |
| `/score-archive [player]` | Same | `ScoreArchiveCommand.java` | **No-op.** Prints \"archive command unimplemented\". Does NOT call the `archiveScore()` method. |

### 5.2 Shared behavior details

Every executor follows this pattern:

1. Parse arguments — if `split.length == 1` and sender is a player, target = self (RCON/console
   requires an explicit `<playerName>` or prints usage).
2. Resolve target player via `server.getPlayerExact(name)` — **exact, case-sensitive, online-only.**
3. Call the corresponding `ScoreKeeperPlugin` method.
4. Echo color-coded result; return `true`.

### 5.3 Inter-plugin / public API surface

`ScoreKeeperPlugin` exposes these public methods that other plugins *could* call if they hold a reference
(but there is **no formal service registration**):

| Method | Visibility | Called by commands? | Notes |
|--------|------------|---------------------|-------|
| `addScore(Player, int)` | `public` | Yes (`/score-add`) | Read-modify-write on `_playerScores` |
| `subtractScore(Player, int)` | `public` | Yes (`/score-subtract`) | Same pattern |
| `getScore(Player)` | `public` | Yes (`/score-get`) | Wrapper around `getPlayerScore` |
| `setScore(Player, int)` | `public` | **No** | Internal write path only; not hooked to any command |
| `resetScore(Player)` | `public` | Yes (`/score-reset`) | Sets to `0` |
| `archiveScore(Player)` | `public` | **No** | Only logs a warning; never called by the archive command |

### 5.4 Concurrency note

The map is a plain `HashMap`. Safe because Paper dispatches commands on the server's single main
thread — but it is **not safe for off-thread use**. Any future event-driven scoring that runs
asynchronously could corrupt state via non-atomic read-modify-write.

---

## 6. Configuration Reference

| File | Format | Purpose |
|------|--------|---------|
| `plugin.yml` | YAML (Bukkit manifest) | Declares main class, api-version, five commands + descriptions/usage strings |
| `build.gradle` | Gradle Kotlin (Groovy DSL) | Dependencies, task config, release/spotless settings |
| `gradle.properties` | Properties | Project version and group coordinates (for Maven publishing) |
| **No `config.yml`** | — | ScoreKeeper has **no player-editable configuration**. |
| **No `permissions:` block** | — | No permission nodes declared; all commands are open. Plans say \"permissions coming after archive works.\" |

---

## 7. Running & Testing Locally

### Build

```bash
./gradlew assemble           # produces ScoreKeeper.jar in build/libs/
```

### Run locally (Paper server)

1. Download Paper 1.21.7 from `https://papermc.io`
2. Copy the built jar into `plugins/`
3. Start the server, verify onEnable logs:
   ```
   [ScoreKeeper] ScoreKeeper version X.Y.Z is enabled.
   [ScoreKeeper] Unable to load scores from file. This feature is not implemented yet.
   ```

### Commands (in-game or via RCON/console)

| Test scenario | Command | Expected output |
|--------------|---------|-----------------|
| Check own score | `/score-get` | \"Your score is 0\" (creates entry at 0) |
| Add points to self | `/score-add 10` | \"You gained 10 points! Now have 10.\" |
| Subtract from self | `/score-subtract 5` | \"You lost 5 points! Now have 5.\" |
| Check another player | `/score-get PlayerName` | \"PlayerName's score is N.\" (must be online) |
| Reset own score | `/score-reset` | \"Your score has been reset to 0.\" |
| Archive (stub) | `/score-archive` | \"archive command unimplemented\" |

### Tests

- **No unit/integration tests exist yet.** `./gradlew test` runs an empty suite. Consider
  adding tests for `ScoreKeeperPlugin`'s private `Player` score state mocking in a future task.

---

## 8. Where to Look First

| I want to understand… | Go to |
|----------------------|-------|
| The entire live data model | `ScoreKeeperPlugin:38` — one `HashMap<UUID, Integer>` field |
| How scores change (write path) | `addScore(/:67-70)`, `subtractScore(/:88-91)`, `setPlayerScore(/:108-110)` |
| How a player is looked up | `getPlayerExact(name)` online only — see any `*Command.java` line ~50 |
| First-time-player behavior | `getPlayerScore(/:100-106)` — auto-inserts `0` on first access |
| High-score / archive table | **Does not exist.** See `archiveScore(/:72-74)` stub; design decisions in §4.3 |
| Persistence (save/load) | `onDisable(/:47)`, `onEnable(/:59)` — both TODO, nothing writes to disk |
| What's declared/intended but not wired | `plugin.yml:18-20` (`/score-archive`), `README.md:11` and README notes |
| Command implementations | `commands/Score*Command.java` (all 5 handlers) |
| Build/runtime config | `build.gradle`, `plugin.yml`, `gradle.properties` |
| Dev env / CONTRIBUTING | `CONTRIBUTING.md` |
---

## Appendix: Risk Summary

| # | Issue | Impact | Section |
|---|-------|--------|---------|
| 1 | **Data loss on restart** — no save/load implemented | Every server boot wipes all scores (§4.1) | §4.1 |
| 2 | **No permissions** — any player can self-add points | Integrity of scoring is unenforceable today (§5.2) | §5.2 |
| 3 | **Race condition on add/subtract** — non-atomic read-modify-write on plain `HashMap` | Corrupt state if future event-driven scoring runs async (§5.4) | §5.4 |
| 4 | **No `int` overflow safety** — Java wrapping semantics apply | Undetectable score corruption near ±2.1 billion | §4.1 |
| 5 | **Name lookups online-only & case-sensitive** | Can't target offline players; \"Alice\" ≠ \"alice\" (§5.2) | §5.2 |
