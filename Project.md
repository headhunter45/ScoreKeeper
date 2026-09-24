# ScoreKeeper — Project Reference

> **Purpose of this document.** A fast reference for the team so anyone can
> understand what ScoreKeeper is, how it is built, and what is and isn't done
> — without having to read the whole codebase first. Read this before touching
> the code. Source: everything here is grounded in the current code (see
> "Where things live" / "Status") and the repo's own docs
> (`README.md`, `CONTRIBUTING.md`, `TODO.md`, `.cursor/plans/Modernize.md`).
> Treat the **code** as the single source of truth where any doc disagrees.

---

## 1. What this project is

**ScoreKeeper** is a **Minecraft server plugin** (Bukkit / **PaperMC** plugin
API) that tracks a numeric **score for each player** and is intended to keep a
**high-score table**. It is a small, early-stage Java plugin.

- **It is a plugin** for a Minecraft server (Paper/Spigot/Bukkit). It runs on
  the server and exposes in-game commands.
- **Core capability today:** record, read, add, subtract, and reset a player's
  score via in-game commands.
- **Intended future capability:** a "High Scores" table that persists scores
  (see §6 — this part is **not** implemented yet).

**Audience / job-to-be-done:** server admins and other plugins should be able to
adjust and read player scores. The authors intend the mutating commands to be
**admin-oriented** and expect that, eventually, *another plugin will call the
score methods directly* rather than having players run the commands themselves
(see `README.md`). Permissions to back that are **not yet implemented** (§6).

**License:** AGPL-3.0. Every Java file must carry the AGPL-3.0 license header
(the header text lives in `config/license-header.txt` and is enforced by
Spotless — §5).

---

## 2. Tech stack & key facts

| Item | Value | Source |
|------|-------|--------|
| Language | Java | — |
| Java version | **Java 21**, via Gradle Java Toolchains | `build.gradle`, `gradle.properties` |
| Server API | **PaperMC** (`io.papermc.paper:paper-api`), **`1.21.7-R0.1-SNAPSHOT`** | `build.gradle` |
| Plugin API version (runtime) | `1.21` | `src/main/resources/plugin.yml` |
| Build tool | **Gradle 8.14.3** via wrapper (`./gradlew` / `gradlew.bat`) | `gradle/wrapper/...` |
| Plugin version | **`0.2.2-SNAPSHOT`** | `gradle.properties` |
| Group / coordinates | `com.majinnaibu.minecraft.plugins` / `ScoreKeeper` | `build.gradle` |
| Output artifact | `build/libs/ScoreKeeper-<version>.jar` | `build.gradle` |
| Format/lint | **Spotless** (Google Java Format) | `build.gradle` |
| Releases | `net.researchgate.release` plugin | `build.gradle` |
| Repo remote | `git@github.com:headhunter45/ScoreKeeper.git` | `git remote -v` |

### Java toolchain / "automatic JDK download"
`gradle.properties` sets `org.gradle.java.installations.auto-download=true`, and
`settings.gradle` applies the **Foojay resolver** plugin
(`org.gradle.toolchains.foojay-resolver-convention`). Together these let Gradle
**auto-download a Java 21 JDK** when one isn't present, so a developer doesn't
have to hand-install a JDK just to build. (The plugin *also* still recommends
installing JDK 21 — see `CONTRIBUTING.md`.)

### ⚠️ Version caveat — read this before building
The Paper API version is pinned to **`1.21.7`** in `build.gradle`, but
`README.md` / `CONTRIBUTING.md` reference a **`1.21.8`** server jar
(`paper-1.21.8-25.jar`). These don't match yet. **Confirm which MC/Paper
version is the target before treating either number as authoritative.**
Everything else in this doc is safe; this one mismatch is a real gotcha.

---

## 3. Commands

The plugin registers **five commands**, all declared in `plugin.yml` and all
backed by a dedicated `CommandExecutor` class. Each executor takes
`ScoreKeeperPlugin` via its constructor (dependency injection by
hand), talks to the plugin through its public methods, and replies with
**adventure `Component`** messages (colored text).

| Command | `plugin.yml` usage | What it does | Handler class |
|---------|-------------------|--------------|---------------|
| `/score-get` | `[playerName]` | Shows a player's current score | `ScoreGetCommand` |
| `/score-add` | `[playerName] <amount>` | Adds points to a player's score | `ScoreAddCommand` |
| `/score-subtract` | `[playerName] <amount>` | Subtracts points | `ScoreSubtractCommand` |
| `/score-reset` | `[playerName]` | Resets a player's score to 0 | `ScoreResetCommand` |
| `/score-archive` | `[playerName]` | **Unimplemented** — prints "archive command unimplemented" | `ScoreArchiveCommand` |

**Behavior notes (read these):**
- **`[playerName]` is optional for in-game players.** If a player omits the
  name, the command acts on the **executor/invoker themselves**.
- **RCON / console callers must always pass a player name** — there is no
  "self", so the executors print usage and refuse.
- **Player lookup uses `getServer().getPlayerExact(name)`**, which only matches
  **online** players by exact name. Offline / unknown names produce
  "Can't find a player with that name" — this is a known limitation the team
  has noted (see `Modernize.md` / §6).
- **Amounts are parsed with `Integer.parseInt`**; non-integer amounts produce
  "amount must be an integer" and do not change anything.
- **Messages are prefixed** with a colored `[ScoreKeeper] ` banner
  (`_messagePrefix`), and log lines are prefixed with `[ScoreKeeper] `
  (`_logPrefix`).
- **All mutating commands are admin-oriented** (per `README.md`), but there is
  **no permission check yet** (§6).

---

## 4. Architecture

### 4.1 Where things live
```
ScoreKeeper/
├── build.gradle              # build config, plugins, deps, spotless, release
├── settings.gradle           # project name + Foojay (toolchain auto-download)
├── gradle.properties         # version + auto-download flag
├── config/license-header.txt # AGPL header enforced on all .java files
├── tools/
│   ├── bash/                 # build-plugin.sh, deploy-plugin.sh, start/stop-server.sh
│   └── powershell/           # same 4 scripts, .ps1 form (Windows)
├── .cursor/plans/Modernize.md# old modernization checklist (see §6)
└── src/main/
    ├── java/com/majinnaibu/minecraft/plugins/scorekeeper/
    │   ├── ScoreKeeperPlugin.java   # plugin entry point + score store + methods
    │   └── commands/                # one CommandExecutor per command
    │       ├── ScoreAddCommand.java
    │       ├── ScoreGetCommand.java
    │       ├── ScoreSubtractCommand.java
    │       ├── ScoreResetCommand.java
    │       └── ScoreArchiveCommand.java
    └── resources/
        └── plugin.yml            # plugin metadata + command declarations
```

### 4.2 The one core class
**`ScoreKeeperPlugin`** (extends `JavaPlugin`) is the heart of the plugin:

- **Score store:** `HashMap<UUID, Integer> _playerScores` — keyed by the
  player's **UUID** (not name — this was a deliberate modernization, see §6).
  An unknown player is lazily seeded to `0`.
- **Public command surface** (what the command classes call):
  `addScore`, `subtractScore`, `getScore`, `resetScore`, `setScore`,
  `archiveScore`.
- **Lifecycle:**
  - `onEnable()` wires up all five executors
    (`getCommand("score-…").setExecutor(new …(this))`) and logs that it's on.
  - `onDisable()` currently **does nothing** except log a warning.
- **Logging helpers:** `logInfo`, `logWarning`, `logError` (all via
  `getLogger()` with the `[ScoreKeeper] ` prefix).
- **Messaging:** `sendMessage(CommandSender, Component)`, which prepends the
  `[ScoreKeeper] ` prefix component.

### 4.3 Data flow
```
player/rcon ──► /score-* ──► <Name>Command.onCommand()
                              │  parse args, find Player via getPlayerExact
                              ▼
                     ScoreKeeperPlugin.addScore / getScore / ...
                              │  read/write HashMap<UUID,Integer>
                              ▼
                     reply via sendMessage() (adventure Component, colored)
```
There is **no external storage today** — the map is in-memory only.

---

## 5. Build, test, and deploy

### 5.1 Build
- **Linux/macOS (and WSL):** `./gradlew build`
- **Windows:** `.\gradlew.bat build`
- Produces `build/libs/ScoreKeeper-<version>.jar` (version comes from
  `gradle.properties`).
- **Wrappers exist** for the common tasks:
  - `tools/bash/build-plugin.sh` / `tools/powershell/Build-Plugin.ps1`
  - `tools/bash/deploy-plugin.sh` / `tools/powershell/Deploy-Plugin.ps1`
  - `tools/bash/start-server.sh` / `tools/powershell/Start-Server.ps1`
  - `tools/bash/stop-server.sh` / `tools/powershell/Stop-Server.ps1`

### 5.2 Spotless (format + license check)
The `build.gradle` configures **Spotless** with `googleJavaFormat()`,
`removeUnusedImports()`, a fixed import order
(`java, javax, com, net, org, ''`), and the AGPL license header. `release`
binds `spotlessCheck` before `build`, so formatting is enforced on release.
**Run `./gradlew spotlessApply` to auto-format before committing**; a bad format
will fail the build.

### 5.3 Deploy & server scripts
The helper scripts operate on **two environment variables** you must set:
- `MINECRAFT_SERVER_PATH` — root dir of your Paper server.
- `MINECRAFT_SERVER_JAR` — the server jar filename relative to that path.

`start-server` launches the Paper server; `stop-server` kills it;
`deploy-plugin` copies the newest `build/libs/*.jar` into the server's
`plugins/` dir (and warns if the jar is older than your source files).
Full setup, including the EULA step and client connection, is in
`CONTRIBUTING.md`.

### 5.4 Releases
The `net.researchgate.release` plugin drives versioning: it reads/writes the
version from `gradle.properties`, builds `spotlessCheck` + `build`, tags
releases as `v$version`, and reverts the version bump on failure
(`revertOnFail = true`).

### 5.5 What's NOT tested
There are **no automated tests** in the repo and no test task is configured.
"Testing" today means running the plugin on a local Paper server via the
scripts above. This is a candidate for the team to add (CI, e.g.
`Minebench`/`PaperTests`, is a reasonable first step).

> Note: this reference document was authored without running a build. To confirm
> the build is green, run `./gradlew build` on a dev machine (requires network
> to fetch the Paper API + toolchain JDK on first run) and confirm the jar in
> `build/libs` loads on a Paper `1.21.x` server.

---

## 6. Status & known gaps (what is / isn't done)

This is the **most useful section for the team** — it captures what the
project-aspirations say vs. what the code actually does.

### 6.1 What is done
| Area | State |
|------|-------|
| Five in-game commands (`/score-*`) | ✅ Wired up and functional for get/add/subtract/reset |
| Per-player UUID-keyed score store | ✅ In-memory `HashMap<UUID,Integer>` |
| Modern Paper API, no deprecations | ✅ `getPlayerExact`, `getLogger()`, `@...` (no legacy event code in active path) |
| Java 21 + auto-download toolchain | ✅ |
| Gradle build + wrapper + release plugin + Spotless | ✅ |
| Bash + PowerShell dev scripts | ✅ |
| License headers on all `.java` | ✅ (enforced by Spotless) |

### 6.2 What is NOT done (in code, not just doc)
These are the real "high score table" features the README aspires to but the
code doesn't yet do:

| Gap | Where it shows up | Notes |
|-----|-------------------|-------|
| **`/score-archive` unimplemented** | `ScoreArchiveCommand.onCommand` just sends "archive command unimplemented"; `ScoreKeeperPlugin.archiveScore` only logs a warning | This is the central "high scores" feature and it's a stub |
| **No persistence on save** | `onDisable()` logs `logWarning("Unable to save scores to file. This feature is not implemented yet.")` | All scores lost on restart or `/score-archive` today |
| **No persistence on load** | `onEnable()` logs `logWarning("Unable to load scores from file. ...")` | Same feature, load side |
| **No permissions / access control** | No `permission` fields in `plugin.yml`, no `hasPermission` check in any executor | `README.md` states the mutating commands should be admin-only; that isn't enforced yet |
| **No automated tests** | No test sources / test task | Testing is manual on a running server |
| **Case-sensitive offline lookup** | `getPlayerExact(name)` returns null for offline players | Noted in `Modernize.md`; players must be **online** to be looked up by name |

> **Important re: "High Scores table" in the README.** The README advertises
> `/score-archive` as "Saves the player's score to a 'High Scores' table and
> resets their current score to 0." That table **does not exist yet**. The
> command and the archive method are both stubs. **Do not assume it works.**
> When a teammate asks "does the high-score table exist?", the honest answer
> today is: no, it's planned and stubbed out.

### 6.3 Historical / superseded docs
- **`.cursor/plans/Modernize.md`** is a pre-migration plan (Maven → Gradle,
  `Player` key → `UUID`, old event registration → modern, etc.). The
  substantive modernization is **mostly done and merged** — the TODO checklist at
  the top of that file is checked, and the current code reflects it. Treat the
  appendix (API comparisons, Gradle-vs-Maven notes) as background only.
- **`TODO.md`** (the "Migration TODOs for Paper Modernization") is similarly
  **mostly checked** — the migration is effectively complete. It is a
  historical artifact for the modernization effort, not the active roadmap.
- **`README.md` "Permissions support is coming AFTER I get archive to work."**
  This line still reflects the original intent and remains true: archive is
  still not worked, permissions are still not there.

### 6.4 Suggested next steps (open, not committed)
These are candidates — **not** a decision; surface in grooming/planning:
1. Implement archive + a real high-scores table with on-disk persistence
   (the obvious first product step).
2. Reconcile the `1.21.7` vs `1.21.8` version question (see §2).
3. Add a basic permission check on the mutating commands (`score-add`,
   `score-subtract`, `score-reset`, and later `score-archive`).
4. Introduce a lightweight test harness for the score store and command parsing
   (the pure-logic portions of the `*Command` classes are amenable to
   unit tests even without a running server).
5. Decide offline lookup strategy — `getPlayerExact` requires the player to be
   online.

---

## 7. Conventions & gotchas

- **Formatting:** Spotless with Google Java Format. Run `./gradlew
  spotlessApply` before committing.
- **License header:** Every `.java` file must start with the AGPL-3.0 block
  from `config/license-header.txt`. Spotless enforces it.
- **Log vs. message:** `logInfo`/`logWarning`/`logError` write server-side logs
  (with `[ScoreKeeper] ` prefix). `sendMessage` sends colored in-chat messages
  **from** the `[ScoreKeeper] ` banner.
- **Command design:** each `<Name>Command` class is the pattern. To add a new
  command: add an entry to `plugin.yml`, write a `CommandExecutor` in
  `src/main/java/.../commands/`, register it in
  `ScoreKeeperPlugin.onEnable`, and expose any new behavior via a new
  public method on `ScoreKeeperPlugin`.
- **UUID, not name, is the key.** All internal storage is by UUID. Names are
  only used at the command edge.
- **Adventure `Component`, not raw strings.** Messages are built with
  `net.kyori.adventure.text.Component`; new messages should follow suit.
- **Dev scripts use env vars.** `MINECRAFT_SERVER_PATH` and
  `MINECRAFT_SERVER_JAR` must be set for the `tools/` scripts to work. See
  `CONTRIBUTING.md` for the full setup.
- **Naming:** the plugin's own coordinates, package, and group are
  `com.majinnaibu.*`; the public GitHub repo is
  `headhunter45/ScoreKeeper`. If a teammate is confused by the two names, this
  is why.

---

## 8. Where to read next

| Read this to | Open |
|-------------|------|
| Understand the original intent / use the plugin | `README.md` |
| Set up a dev environment | `CONTRIBUTING.md` |
| See the original modernization plan (historical) | `.cursor/plans/Modernize.md` |
| See the original migration TODO list (historical) | `TODO.md` |
| Understand a specific command's behavior | `src/main/java/.../commands/*.java` |
| Understand the plugin's core logic | `src/main/java/.../ScoreKeeperPlugin.java` |
| Understand what the build does | `build.gradle`, `settings.gradle`, `gradle.properties` |
| Understand the command declarations | `src/main/resources/plugin.yml` |
| See the license | `LICENSE`, `config/license-header.txt` |

---

*This document is a living reference. When you learn something new about the
project, update it — keep it honest (what's done vs. planned), and cite the
source file when something is non-obvious.*
