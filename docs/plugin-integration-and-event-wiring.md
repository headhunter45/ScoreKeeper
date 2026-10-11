# Score Keeper - Plugin Integration and Event Wiring

New Score Keeper contributors should start with
[CONTRIBUTING.md](../CONTRIBUTING.md), which covers architecture, setup, and validation.
This is an optional detailed reference for runtime wiring and companion-plugin
authors, not a second onboarding guide. [domain-score-tracking.md](domain-score-tracking.md)
describes state and [README.md](../README.md) gives full command syntax.

## Paper lifecycle

The entry point is
[ScoreKeeperPlugin](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreKeeperPlugin.java),
declared in [plugin.yml](../src/main/resources/plugin.yml) under the plugin ID
`ScoreKeeper`. Compilation targets Paper `26.2.build.129-stable` on Java 25;
supported server versions are Paper 26.2 and 26.3.

| Trigger | Handler / effect |
| --- | --- |
| Plugin enable | Register eight executors, initialize `points`, load scores/archives, check existing runs, schedule synchronous checks every 20 ticks |
| Scheduled check | `checkHighScoreRuns`: finish expired duration runs or runs whose target is reached |
| Plugin disable | Serialize bucket definitions, scores, assignments, active runs/snapshots, and archives to two YAML files |
| Minecraft join/quit/death/kill | No built-in listeners or automatic score awards |
| Companion-plugin API call | Perform requested score/bucket/archive/run operation |

There are no `Listener` implementations, `@EventHandler` methods, custom scoring
events, or event registrations. The scheduler manages runs, not gameplay scoring.
Expired saved runs finish during enable; `until-disabled` runs persist and resume.

## Command wiring

Each manifest command is explicitly wired in `onEnable` using `getCommand` and
`setExecutor`. A missing declaration fails startup via `Objects.requireNonNull`.

Handlers are in [commands/](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/commands/).

| Command | Executor | Delegation / effect | Permission |
| --- | --- | --- | --- |
| `score-get` | `ScoreGetCommand` | `getScore`, active bucket, lazy entry creation | None |
| `score-add` | `ScoreAddCommand` | `addScore`, signed integer addition | None |
| `score-subtract` | `ScoreSubtractCommand` | `subtractScore`, signed integer subtraction | None |
| `score-reset` | `ScoreResetCommand` | `resetScore`, active bucket initial value | None |
| `score-archive` | `ScoreArchiveCommand` | `archivePlayerScore`, `archiveBucketScores`, or `archiveAllScores` | `scorekeeper.admin` |
| `score-archive-list` | `ScoreArchiveListCommand` | `getScoreArchives` or `getScoreArchive` | None |
| `score-bucket` | `ScoreBucketCommand` | View/select own bucket, create bucket, switch another/all players | Admin for create/other/all; none for self |
| `score-run` | `ScoreRunCommand` | Start target/timed/manual run, or stop it | `scorekeeper.admin` |

`scorekeeper.admin` defaults to operators in the manifest. Guards are implemented
inside selected executors, not per-command manifest permission fields. Public API
calls do not enforce sender permissions; the caller must enforce its own policy.

Named player targets use Paper's `getPlayerExact` and must be online. Players
may omit the optional name to target themselves; non-player senders have no
self context. Console/RCON can use archive `all`/`bucket` forms without a player.
Bucket IDs are case-sensitive even though command keywords such as `all` are not.

Parse/validation failures are returned as Adventure usage/error messages.
Handlers consume commands by returning `true`. Score mutation commands rely on
bucket/run reporting instead of unconditional success messages; the default
`none` policy can make a successful mutation silent.

## Companion-plugin integration

Score Keeper declares no plugin dependencies of its own. A companion that requires
it should compile against the matching jar as a provided/compile-only dependency,
and declare `depend: [ScoreKeeper]` in its own Bukkit-style manifest so it loads
after Score Keeper. For optional integration, use `softdepend` and handle absence
or disabled state explicitly.

There is no ServicesManager registration or standalone API module. Obtain the
enabled plugin by ID and check its type, for example during the companion's enable:

```java
Plugin candidate = getServer().getPluginManager().getPlugin("ScoreKeeper");
if (!(candidate instanceof ScoreKeeperPlugin scoreKeeper) || !candidate.isEnabled()) {
  getLogger().severe("Score Keeper is required but is not enabled.");
  getServer().getPluginManager().disablePlugin(this);
  return;
}
```

Imports for this example are `org.bukkit.plugin.Plugin` and
`com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin`. Keep the
reference for your integration lifecycle, not as a global singleton.
Do not shade a second copy of Score Keeper into the companion jar.

### Public operations

| Area | Methods |
| --- | --- |
| Bucket definition | `createBucket(id, singular, plural, initialValue[, ScoreReporting])`, `getBucket(id)`, `getBuckets()` |
| Active selection | `getPlayerBucket(Player)`, `switchPlayerBucket(Player, id)`, `switchAllPlayers(id)` |
| Score CRUD | `getScore(Player[, id])`, `setScore(Player[, id], score)`, `addScore(Player[, id], amount)`, `subtractScore(Player[, id], amount)`, `resetScore(Player[, id])` |
| Archives | `archivePlayerScore(Player)`, `archiveBucketScores(id)`, `archiveAllScores()`, `getScoreArchives()`, `getScoreArchive(archiveId)` |
| Compatibility wrapper | `archiveScore(Player)` delegates to player archive creation but discards the returned ID |
| Runs | `startTargetScoreRun(id, topN, resetScores, targetScore)`, `startTimedScoreRun(id, topN, resetScores, Duration)`, `startUntilDisabledScoreRun(id, topN, resetScores)`, `stopHighScoreRun(id)`, `hasActiveHighScoreRun(id)` |
| Messaging/logging | `sendMessage(CommandSender, Component)`, `logInfo`, `logWarning`, `logError` |

The bracket notation above describes overloads, not literal Java syntax.
`getBucket` and `getScoreArchive` return null when unknown; mutating/score methods
validate bucket existence and throw `IllegalArgumentException` for invalid
operations. Do not swallow errors or report success after a failure.
Bucket maps and archive snapshots exposed by getters are immutable copies/views.

Example explicit-bucket award, with imports for `ScoreReporting` and `Player`:

```java
if (scoreKeeper.getBucket("event") == null) {
  scoreKeeper.createBucket("event", "point", "points", 0, ScoreReporting.PLAYER);
}
scoreKeeper.addScore(player, "event", 10);
```

Only create a shared bucket if its existing definition meets your integration's
requirements; duplicate creation throws. Explicit-bucket scoring does not switch
the player's active bucket.

### Threading and ownership

All calls touching state, players, or reporting must execute on Paper's main
server thread. The plugin's `LinkedHashMap` state and read-modify-write operations
are not synchronized. Marshal asynchronous event/work results to a synchronous
Paper task before calling this API. Score Keeper does not provide Folia/region
scheduler support.

Do not mutate private maps or write the YAML files from another plugin.
Use the API so reporting, validation, target completion, and snapshots remain
consistent. API calls change in-memory state; durability is deferred until
Score Keeper disables successfully.

## Verification

There is currently no automated plugin harness or real-server test task.
Validate load ordering, command dispatch, messages, run scheduler behavior, and
clean-restart persistence on disposable Paper servers using
[human-tests.md](human-tests.md). Do not assume a successful compilation verifies
companion load order or runtime API compatibility.
