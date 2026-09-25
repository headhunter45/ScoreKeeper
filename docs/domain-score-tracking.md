# ScoreKeeper — Score-Tracking & "High-Score Table" Domain Analysis

> **Scope of this document:** the domain logic that tracks player scores and (intended) the
> high-score table. Written for a new team member who must understand the scoring system
> *without opening the source*. Every claim below is traced to a file/line so it can be
> re-verified.
>
> **Bottom line up front (read this first):** ScoreKeeper currently tracks **live scores
> only, in memory, for the running server session**. It has **no persistence** and **no
> high-score table**. The high-score table is *described as a planned feature* in the
> README and is where the `/score-archive` command is *intended* to land scores, but
> **that code path is an unimplemented stub today.** There is no sorting, no tie-breaking,
> no entry cap, no decay, and no scoring formula/weighting anywhere in the codebase.
> This is a greenfield/early-stage domain — the write-up documents the *real* state and
> flags the gap, because documenting features that don't exist as if they did would mislead
> the next engineer.

---

## 0. How to read the rest of this doc against the task's four questions

The task asks for (1) the score-entry data model + persistence format, (2) how scores are
recorded, (3) the high-score-table algorithm, and (4) scoring formulas/weighting.
For each question the answer is: **the mechanism that exists vs. the mechanism the project
intends**, so readers never conflate the two.

| # | Question | Short answer |
|---|----------|--------------|
| 1 | Score-entry data model & persistence | Live score = `UUID -> int` in a `HashMap`. **No persistence** (load/save are `TODO` stubs). |
| 2 | How scores are recorded | **Manual, event-free:** the *administrator* runs a command. No game events, no timers. |
| 3 | High-score-table algorithm | **Not implemented.** No sort/tie-break/cap/decay. Only an aspirational `/score-archive`. |
| 4 | Scoring formulas / weighting | **None.** Scores are plain integer accumulators; no weighting, no decay, no bonuses. |

---

## 1. The score-entry data model & persistence  *(task Q1)*

### 1.1 What a "score entry" actually is (current reality)

The entire live-score domain is **one field** in the plugin's entry-point class:

- **File:** `src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreKeeperPlugin.java:38`
- **Declaration:** `private final HashMap<UUID, Integer> _playerScores = new HashMap<UUID, Integer>();`

That is the whole model:

| Aspect | Value |
|--------|-------|
| Record shape | A single `Integer` (the score), keyed by a player's `UUID`. |
| Key type | `java.util.UUID` — the player's `UniqueId`, *not* the name string. |
| Value type | `int` (Java `Integer`), autoboxed into the map. |
| Container | one `java.util.HashMap` on the plugin instance. |
| Uniqueness | one entry per `UUID`. A player can have exactly one live score at a time. |
| Default for a new player | `0` — see §3.1 ("first-time player" edge case). |

There is **no `Score`/`ScoreEntry`/`PlayerStats` value class**, no timestamp, no
name stored alongside the score (name is looked up from the live server only — see §2.3),
and no secondary index.

### 1.2 Persistence format: **none exists**

Persistence is *intended* but *not built*. Three concrete stubs prove this:

1. **On disable (server stop):** `ScoreKeeperPlugin.java:46-49` `onDisable()` logs
   *"Unable to save scores to file. This feature is not implemented yet."* — the map
   is **discarded** on shutdown; nothing is written.
2. **On enable (server start):** `ScoreKeeperPlugin.java:59-60` `onEnable()` logs
   *"Unable to load scores from file. This feature is not implemented yet."* — the map
   always starts **empty** `{}` every server start.
3. **Archive:** `ScoreKeeperPlugin.java:72-74` `archiveScore(Player)` logs
   *"Unable to archive score for <name>."* and returns — the "save to high-score table"
   action is a no-op.

So there is **no SQLite, no JSON, no YAML, no flat file, no config.yml**. The only file on
disk that concerns scores is `plugin.yml` (the command manifest — it lists the commands but
stores no scores). A grep of the whole tree for `sql / json / yaml / saveResource /
YamlConfiguration / File / Files. / Gson / Jackson` finds **zero hits** other than the
TODO/log strings.

**Persistence format today = "in RAM only, lost on server restart."**

> Implication for a new engineer: any "who's the top player" question answered *across
> restarts* is impossible today. Scores reset to the map being empty on every server boot.

---

## 2. How scores are recorded  *(task Q2)*

### 2.1 The recording model is **manual and command-driven — no events, no timers**

Scores move only when an administrator (or console/RCON) **types a command**. There is:

- **No** `implements Listener`, no `@EventHandler`, no `registerEvents(...)`, no
  `getServer().getPluginManager().registerEvents(...)`.
- **No** scheduler: no `getScheduler()`, no `runTask`, no `Timer`.
- **No** automatic hook to game events (e.g. a player killing a mob or reaching a goal
  awards points automatically — *nothing like that exists*).

A grep for `EventListener|@EventHandler|Listener|Scheduler|runTask` across `src/` returns
**zero**.

So "scoring" happens **on demand**, by a person, via one of the four mutating commands:

| Command | Effect on the map | Source |
|---------|-------------------|--------|
| `/score-add [player] <amount>` | `score += amount` | `ScoreKeeperPlugin.addScore` `:67-70` |
| `/score-subtract [player] <amount>` | `score -= amount` | `ScoreKeeperPlugin.subtractScore` `:88-91` |
| `/score-reset [player]` | `score = 0` | `ScoreKeeperPlugin.resetScore` `:80-82` |
| `/score-archive [player]` | **intended** to snapshot to a high-score table and reset to 0; **actually a no-op** | `ScoreKeeperPlugin.archiveScore` `:72-74` |

`/score-get [player]` is read-only (returns the score, §2.4).

### 2.2 The add/subtract primitives (the only real "write" path)

Both funnel through one private setter:

```
addScore(player, n)    ->  old = getPlayerScore(player); setPlayerScore(player, old + n)   :67-70
subtractScore(player,n)->  old = getPlayerScore(player); setPlayerScore(player, old - n)   :88-91
setScore(player, s)    ->  setPlayerScore(player, s)                                       :84-86
resetScore(player)     ->  setPlayerScore(player, 0)                                        :80-82
setPlayerScore(p, v)   ->  _playerScores.put(p.getUniqueId(), v)                            :108-110
```

Notes a new engineer must internalize:

- **Read-modify-write.** `addScore`/`subtractScore` read the current value, compute, write
  back in two map operations. (There is no concurrency control — see §5 risks.)
- **`int` math.** Values are Java `int`; a score can go **negative** (there is no floor at
  0 — `/score-subtract 50` on a 10-score yields `-40`), and can overflow `int` only after
  `~2.1e9` points, which is not a practical concern.
- **No validation of the direction.** The only validation is *syntactic*: the amount must
  parse as an integer (see §2.3).

### 2.3 Command surface (who/what is allowed to record a score)

Every command executor lives in `src/main/java/.../scorekeeper/commands/` and shares one
shape: parse args, resolve the target player, call the `ScoreKeeperPlugin` method, return
`true` (consumed). Two behavioral details matter:

- **Self-target default:** if a *player* runs a command and omits the name, they target
  *themselves*: `targetPlayer = (Player) sender`. (e.g. `ScoreGetCommand.java:44`,
  `ScoreAddCommand.java:48`, `ScoreResetCommand.java:47-49`.)
- **RCON/console path:** if the sender is **not** a player (console/RCON), a player name
  is **required** — omitting it prints usage instead of acting
  (`ScoreGetCommand.java:46-48`, `ScoreAddCommand.java:45-47`,
  `ScoreResetCommand.java:44-46`).
- **Name resolution:** `getServer().getPlayerExact(name)` — **exact, case-sensitive**
  lookup by the *currently online* name (`ScoreGetCommand.java:50`,
  `ScoreAddCommand.java:57`, `ScoreResetCommand.java:50`). A name that is not online
  yields `null` → "Can't find a player with that name". Note the *map key is a UUID*, but
  *lookup by name only works while that player is online* — you cannot change an offline
  player's score by name today.
- **No permission nodes anywhere.** `plugin.yml` declares no `permission:`/`permissions:`
  and the code checks none. The README's note ("the commands aside from get are intended
  for admins … Permissions support is coming") is **intent, not enforcement** — a vanilla
  player can run `/score-add` today.

### 2.4 Reading a score (`/score-get`)

`ScoreGetCommand.java:38-84` → `ScoreKeeperPlugin.getScore(player)` → `getPlayerScore(player)`
(`ScoreKeeperPlugin.java:76-78, 100-106`). It returns the live integer (0 for a known-but-
unwritten player, which also *materializes* a 0 entry, see §3.1). Output formatting
("Your score is N" vs. "PLAYER's score is N") depends only on whether the sender is the
target or not — it carries no ranking or table.

---

## 3. The high-score-table algorithm  *(task Q3)*

### 3.1 What exists: **nothing. It is aspirational.**

There is **no high-score table, no ranking structure, no sort, no tie-break rule, no max
entry count, and no decay/rotation** anywhere in the codebase. The task's premise ("the
high-score table") reflects the **product intent** in the README, not the **code**. Concretely:

- The README (`README.md:11`) says `/score-archive` *"Saves the player's score to a
  'High Scores' table and resets their current score to 0."*
- The manifest (`plugin.yml:18-20`) declares `/score-archive` with that very description.
- But the command is a stub: `ScoreArchiveCommand.java:37-39` (`onCommand` body) replies
   *"archive command unimplemented"*, and the underlying `archiveScore`
  (`ScoreKeeperPlugin.java:72-74`) only logs and returns — it neither writes a table nor
  resets anything.

Therefore the "algorithm" is: **no-op.** A new engineer asked to "implement the high-score
table" is starting from zero; the design questions in §3.2 are **open**, not answered by code.

### 3.2 Open design questions the table must settle (no code today decides these)

Since no implementation exists, these are *decisions to be made*, recorded here so the
implementation task doesn't reinvent the debate:

- **Storage/persistence format.** The load/save TODOs (`:47`, `:59`) say "to file";
  `plugin.yml`/Bukkit convention favors YAML (`YamlConfiguration`), but JSON/SQLite are all
  viable. **Unchosen.** Persisting to `data/` via `getDataFolder()` is the idiomatic Bukkit
  path.
- **What an entry is.** Today a live score is just `{uuid -> int}`. A high-score entry
  likely needs **player name + score + (timestamp?) + (date earned?)**. Name is *not*
  currently stored with the score, so a durable table must capture the name at archive
  time (a UUID-only entry can't render a leaderboard without the name being resolvable).
- **Sorting / ordering.** Almost certainly **descending by score**.
- **Tie-breaking.** Undefined. Candidate rules: by *score only*; by *earliest* archived
  first (stable, time-ordered); by *name* (alphabetical); or "first to reach that score."
- **Max entries / cap.** Undefined. A leaderboard needs a cap (e.g. top 10 / top 30);
  today there is no cap because there is no table.
- **Decay / rotation / time window.** None, and none described in docs — so the safe
  default is **"scores are permanent, no decay."**
- **Reset semantics on archive.** README promises archive *resets to 0* *and* records.
  `ScoreArchiveCommand` must decide: does archiving also reset? Does it allow multi-entries
  (a player appearing more than once) or one row per player? All open.

### 3.3 Full lifecycle of a score: creation → (intended) display on the high-score table

This is the acceptance-criterion walk-through. Each stage is marked **[REAL]** (in code now)
or **[GAP]** (intended, not built).

1. **Player joins / acts** — a score *doesn't* exist yet. First interaction with the map is
   lazy (see stage 4a). **[REAL]**
2. **Admin records a score** — `admin: /score-add Alice 10`. Parsed in `ScoreAddCommand`,
   routed to `addScore`. The first time this runs for Alice, `getPlayerScore` **creates**
   her entry at 0 and returns it; then `10` is written. **[REAL]**
3. **Score accumulates** — later `/score-add Alice 5` → 15; `/score-subtract Alice 2` → 13;
   `/score-reset Alice` → 0. All in the in-memory map, live only. **[REAL]**
4. **Read** — `/score-get Alice` → "Alice's score is 13". No ranking shown. **[REAL]**
5. **Server restart** — `onDisable` *cannot* save (stub), `onEnable` *cannot* load (stub);
   the map is **lost**. Alice's 13 is gone; every score starts empty again. **[REAL] —
   this is the current data-loss reality.**
6. **Intended: archive to the high-score table** — `admin: /score-archive Alice` should
   freeze Alice's 13 into a durable, sorted leaderboard and reset her live score to 0, so
   it survives restarts. **[GAP]** — currently returns "archive command unimplemented".
7. **Intended: display on the high-score table** — a command (not yet built) renders the
   capped, sorted list. **[GAP]** — no such command, no structure to render.

**Edge cases (explicitly, per the acceptance criteria):**

- **First-time player / first write.** A player has no map entry until `getPlayerScore` is
  called, which **inserts `0` on first access** (`ScoreKeeperPlugin.java:100-106`). So a
  first `/score-add X 10` yields **10, not** `10 - 0` ambiguity, and a first `/score-subtract
  X 3` yields **−3** (no minimum-0 clamp). A first `/score-get X` returns **0** and
  *materializes* the entry at 0. This `get-or-create-0` behavior is the de-facto "first-time
  player" rule. **[REAL]**
- **Ties.** **Undefined** because no table exists. When the table is built, decide a
  tie-break (§3.2). No code today can observe or sort ties — `HashMap` gives no order. **[GAP]**
- **Table full.** **Undefined** because no cap exists. When a cap is introduced, decide
  what happens to the displaced entry: drop it, or "rotate" it into overflow. No code
  today enforces a maximum. **[GAP]**
- **Offline / unknown name.** `getPlayerExact(name)` returns `null` for anyone not
  currently online or any misspelling → the command prints "Can't find a player with that
  name" and changes nothing. You **cannot** archive or reset an *offline* player by name
  today; by-UUID writes (if a future table stores names+UUIDs) would fix that. **[REAL]**
- **Negative / large scores.** No clamp; negatives allowed, `int` overflow only near
  ~2.1 billion. **[REAL]**
- **Double-spawn / relogin.** Keyed by `UUID`, **not name**, so a name change or two
  players sharing a name can't collide on the *live* map; but a *name* is never stored,
  so a future table that only persists the UUID can later fail to render a name.

---

## 4. Scoring formulas & weighting  *(task Q4)*

**None.** There is:

- No formula, no multiplier, no combo/bonus/streak logic.
- No weighting between action types.
- No time-based decay or "points per game" normalization.

Scores are **pure integer accumulators**: `score = Σ(adds) − Σ(subtracts)` over the session,
resettable to 0 by `/score-reset` (intended: on `/score-archive`). The only "arithmetic" in
the code is the `+`/`−` in `addScore`/`subtractScore` and the `0` floor written by
`resetScore`. `int` semantics apply (wrapping overflow; no checked arithmetic). Any
formula/weighting is a **future-design** decision, not encoded today.

---

## 5. Data-flow diagram (text)

```
   ADMINISTRATOR / CONSOLE / RCON        (no game events, no scheduler, no auto-scoring)
        │  types a command
        ▼
  ┌─────────────────────────────┐
  │  command executors          │  commands/Score*Command.java
  │  parse args + resolve player│  getPlayerExact(name)  ── must be ONLINE (name→UUID)
  │  (self-target if player)    │  no permission checks
  └──────────────┬──────────────┘
                 │ calls
                 ▼
  ┌───────────────────────────────────────────────┐
  │  ScoreKeeperPlugin (entry point; plugin.yml main)│
  │                                                 │
  │  addScore / subtractScore / resetScore / setScore │  read-modify-write on...
  │  getScore (read)               archiveScore (STUB)│
  └──────────────┬───────────────────────┬──────────┘
                 │ read-modify-write       │ (intended: snapshot→table, reset 0)
                 ▼                        ▼
   live map: HashMap<UUID, int>            ╳ NOT IMPLEMENTED
   "score entry = one int per UUID"        high-score table:
   default 0, no clamp, in RAM only       ─ no structure, no sort,
        │                                  ─ no persistence,
        ├─ READ: /score-get → "N"          ─ no cap/decay
        │
        └─ LIFECYCLE:
            onEnable  :  load from file  → TODO (stays {} ; lost on restart)
            onDisable :  save to file    → TODO (map discarded at shutdown)

   PERSISTENCE:  none  │  SCORE FORMULA: none  │  EVENTS/TIMERS: none  │
   PERMISSIONS: none  │  HIGH-SCORE TABLE: aspirational stub only
```

---

## 6. Quick reference — "where to look first"

| I want to understand… | Go to |
|-----------------------|-------|
| The entire live data model | `ScoreKeeperPlugin.java:38` (one `HashMap<UUID,Integer>`) |
| How a score changes | `addScore`/`subtractScore`/`resetScore`/`setScore` `:67-91` |
| How a score is read | `getScore` `:76-78` → `getPlayerScore` `:100-106` (note get-or-create-0) |
| The "high-score table" | **Doesn't exist** — see `archiveScore` stub `:72-74`; design open in §3.2 |
| Persistence (save/load) | `onDisable` `:46-49`, `onEnable` `:59-60` — both `TODO`, not built |
| Command behaviors | `commands/Score*Command.java` (self-target default; RCON requires a name; exact online-name lookup) |
| What's declared/intended but not wired | `plugin.yml:18-20` (`/score-archive`), `README.md:11`, `README.md` notes |
| Build/runtime target | `build.gradle` (paper-api 1.21.7, Java 21), `plugin.yml` (`api-version: 1.21`) |

---

## 7. Risks a new engineer should know (correctness gaps in the *current* live domain)

These are real, not future-work — flagging because the doc's job is to prevent a new
developer from assuming the system is more mature than it is:

1. **Data loss on restart.** No save/load (`:47`, `:59`). Every server boot wipes all
   scores. Any "leaderboard across sessions" is broken by design.
2. **No permissions.** Anyplayer can self-add points. README says this is by intent
   ("coming after I get archive to work") but it is not enforced.
3. **Race condition on add/subtract.** `read → compute → write` is not atomic and the
   `HashMap` is not thread-safe; concurrent command executions could lose an update.
   (Low likelihood today since commands are serial on one thread, but it's a latent bug for
   any async/event-driven scoring that gets added.)
4. **No minimum-0 / no validation of amount sign.** Subtracts produce negatives freely.
5. **Name lookups are online-only & exact.** Can't target offline players by name;
   case-sensitive; "Alice" ≠ "alice".
6. **No high-score table.** The flagship advertised feature (`/score-archive` → "High
   Scores" table) is a no-op; a new engineer must not expect ranking/persistence to work.

---

## 8. Summary for the synthesizer (`Project.md` § "domain model & scoring logic")

Distilled, code-grounded, and honest:

- **Model:** one `int` per player `UUID`, held in `ScoreKeeperPlugin._playerScores`
  (`HashMap`), default `0`.
- **Recording:** manual, admin-driven commands; **no events/timers**; no permission guard.
- **Persistence:** **none** — load/save are `TODO`; scores are lost on restart.
- **High-score table:** **does not exist** — `/score-archive` is a stub; sorting/tie-break/
  cap/decay are **undecided design items** (listed in §3.2).
- **Formula/weighting:** **none** — plain `int` accumulation.
- **Lifecycle:** create-on-first-access (0) → add/subtract/reset (live only) → read via
  `/score-get` → *intended* archive+reset + table display **not built** → lost on restart.
- **Edge cases:** first-time/first-write ⇒ 0-or-delta, no floor; ties/table-full ⇒ undefined
  (no table yet); offline/unknown name ⇒ "can't find", unchanged.

Everything below the "high-score table" line of the README is **future work**, and the
write-up's job is to make that boundary explicit so the next team member isn't misled.
