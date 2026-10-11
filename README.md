# Score Keeper

[![Java CI with Gradle](https://github.com/headhunter45/ScoreKeeper/actions/workflows/gradle.yml/badge.svg)](https://github.com/headhunter45/ScoreKeeper/actions/workflows/gradle.yml)

Score Keeper tracks player scores in named buckets for server events, competitions,
and companion plugins. Buckets support custom units, initial values, reporting,
archives, and target-based or timed high-score runs. Gameplay does not award points
automatically: use commands or another plugin calling the public API.

Supports **Paper 26.2 and 26.3 only**, with **Java 25**. Bukkit and Spigot servers
are not supported. The display name is Score Keeper; the repository and installed
plugin identifier remain `ScoreKeeper`.

## Installation

1. Obtain a plugin jar from [Modrinth](https://modrinth.com/plugin/scorekeeper)
   or build it using [CONTRIBUTING.md](CONTRIBUTING.md).
2. Stop your Paper server cleanly. Back up existing plugin data.
3. Put `ScoreKeeper-<version>.jar` in the server's `plugins` directory, removing
   older Score Keeper jars so only one version loads.
4. Start the server and verify the plugin enables without errors.

Scores, bucket definitions, reporting, player assignments, and active runs load
from `plugins/ScoreKeeper/scores.yml`; archives load from
`plugins/ScoreKeeper/archives.yml`. Both are saved **on plugin disable/clean
shutdown**, not after every command. Crashes or forced termination can lose
changes since the last successful save. Do not edit these files while running.

## Running

### Commands

For commands with an optional player name, players can omit it to target themselves.
Named targets must be online. Console and RCON must provide a player name for
score get/add/subtract/reset; archive also supports `all` and `bucket` scopes.
Bucket IDs are case-sensitive and allow letters, numbers, `_`, and `-`.
Command-created singular/plural labels each occupy one argument.
Add/subtract accept signed 32-bit integers, including zero and negative amounts.

* `/score-get [playerName]` - Displays the target player's score in their active bucket.
* `/score-add <amount>` - Adds an amount to your score in your active bucket.
* `/score-add <playerName> <amount>` - Adds an amount to the target player's score.
* `/score-subtract <amount>` - Subtracts an amount from your score in your active bucket.
* `/score-subtract <playerName> <amount>` - Subtracts an amount from the target player's score.
* `/score-reset [playerName]` - Resets the target player's score in their active bucket to that bucket's initial value.
* `/score-archive` - Archives and resets your score in your active bucket.
* `/score-archive <playerName>` - Archives and resets the target player's score in their active bucket.
* `/score-archive all` - Archives and resets scores in every bucket.
* `/score-archive bucket <bucketId>` - Archives and resets every score in one bucket.
* `/score-archive-list` - Lists saved archive IDs, newest first.
* `/score-archive-list <archiveId>` - Displays the saved scores in one archive.
* `/score-bucket` - Displays your active bucket.
* `/score-bucket <bucketId>` - Switches your active bucket.
* `/score-bucket create <id> <singular> <plural> <initialValue> [reporting]` - Creates a bucket. Reporting is `admin`, `global`, `player`, or `none` and defaults to `none`.
* `/score-bucket <playerName> <bucketId>` - Switches a player's active bucket.
* `/score-bucket all <bucketId>` - Changes the server's default bucket and clears individual bucket assignments.
* `/score-run start <bucketId> <topN> <true|false> target <score>` - Starts a run that ends when a score reaches the target.
* `/score-run start <bucketId> <topN> <true|false> duration <time>` - Starts a timed run. Use a positive number followed by lowercase `s`, `m`, `h`, `d`, or `w` (for example, `4h`).
* `/score-run start <bucketId> <topN> <true|false> until-disabled` - Starts a run with no automatic end, including across clean restarts.
* `/score-run stop <bucketId>` - Ends an active run.

The reset argument for a run is the literal `true` or `false`, not `reset:true`.
Top N must be at least 1. For example:

```text
/score-bucket create event point points 0 player
/score-bucket event
/score-run start event 3 true duration 4h
/score-add 10
/score-get
/score-run stop event
/score-archive bucket event
```

### Permissions

The `scorekeeper.admin` permission (default: operators) is required for
`score-archive`, bucket creation, switching another player/all players, and run
management. Viewing/selecting your own bucket requires no admin permission.
Score get/add/subtract/reset and archive-list currently have no permission
checks; add, subtract, and reset can target **any online player**. Restrict command
access externally if that is inappropriate for your server's scoring rules.

### Reporting, runs, and archives

When a player's active bucket changes, they are told the bucket name and their current score with its singular or plural unit.

Bucket reporting defaults to `none`; `admin` notifies operators and score admins
and logs the change, `global` notifies all online players, and `player` notifies
only the scored player. Only changed values produce reports. With `none`, a
successful add/subtract/reset can be silent; use `/score-get` to inspect the
numeric score. Scores can go negative and arithmetic overflow is not checked.

Each bucket can have one active run. During it, players receive their own score
changes regardless of the bucket reporting policy; changes by players in the
top N after the update are reported globally. Rankings include all recorded
bucket entries, even offline players. Ties sort by case-insensitive name, then UUID,
and have consecutive places. Finished runs announce/log the top-N table and each
online player's place. Reset runs restore the pre-run bucket scores; non-reset
runs leave earned scores intact. Timed runs use wall-clock time, including time
offline. An existing target can end a non-reset run immediately.

Run results are not automatically archived. Stop the run before archiving its
bucket. Archives snapshot the selected recorded scores and reset them to the
bucket initial value; they become durable at clean shutdown.

## Plugin API

Other plugins can create buckets with
`createBucket(id, singular, plural, initialValue, ScoreReporting)` and use
bucket-aware score methods. Existing no-bucket methods use the player's active
bucket. Runs use `startTargetScoreRun`, `startTimedScoreRun`,
`startUntilDisabledScoreRun`, and `stopHighScoreRun`.

Declare a dependency on plugin ID `ScoreKeeper`, obtain the enabled
`ScoreKeeperPlugin` instance, and call it on Paper's main thread. There is no
separate service registration or automatic gameplay integration. See
[plugin integration and event wiring](docs/plugin-integration-and-event-wiring.md)
for setup, overloads, and failure handling.

## Development

Start with [CONTRIBUTING.md](CONTRIBUTING.md) and the shared rules in
[AGENTS.md](AGENTS.md). The project uses Gradle 9.8.0, Java 25 toolchains, and
Spotless. The compile toolchain can be downloaded automatically, but the wrapper
still needs a compatible JVM to start.

There are currently no automated Java tests; a passing build does not exercise
plugin behavior. Use [human-tests.md](docs/human-tests.md) before releases.
See [the contributor guide's project overview](CONTRIBUTING.md#project-overview-and-source-map) for architecture and
[the modernization plan](docs/Modernize.md) for remaining work.

## License

Score Keeper is licensed under the [GNU AGPL v3 or later](LICENSE).
