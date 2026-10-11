**Score Keeper**

Score Keeper tracks player scores in configurable buckets. Use it for event points,
competitions, or other server-side scoring systems, and integrate it with other
plugins through the public API. Points are awarded by commands or companion
plugins, not by built-in gameplay event listeners.

## Installation

Requires Java 25 and Paper 26.2 or 26.3. Stop your server cleanly, back up plugin
data, and place the downloaded jar in `plugins`, removing older Score Keeper jars.
Restart and check the server log. The installed plugin ID and data folder remain
`ScoreKeeper`; the display name is Score Keeper.

## Commands

Players can omit an optional player name to target themselves. Named targets must
be online. Console and RCON must provide a player name for score get/add/subtract/reset;
archive also accepts `all` and `bucket` scopes. Add/subtract accept signed 32-bit
integers, including zero and negative amounts. Bucket IDs are case-sensitive and
allow letters, numbers, `_`, and `-`; each command-created unit label is one argument.

* `/score-get [playerName]` - Displays the target player's score in their active bucket.
* `/score-add <amount>` - Adds an amount to your score in your active bucket.
* `/score-add <playerName> <amount>` - Adds an amount to the target player's score.
* `/score-subtract <amount>` - Subtracts an amount from your score in your active bucket.
* `/score-subtract <playerName> <amount>` - Subtracts an amount from the target player's score.
* `/score-reset [playerName]` - Resets the target player's active-bucket score to that bucket's initial value.
* `/score-archive` - Archives and resets your score in your active bucket.
* `/score-archive <playerName>` - Archives and resets the target player's score in their active bucket.
* `/score-archive all` - Archives and resets scores in every bucket.
* `/score-archive bucket <bucketId>` - Archives and resets every score in one bucket.
* `/score-archive-list` - Lists saved archive IDs, newest first.
* `/score-archive-list <archiveId>` - Displays the saved scores in one archive.
* `/score-bucket` - Displays your active bucket.
* `/score-bucket <bucketId>` - Switches your active bucket.
* `/score-bucket create <id> <singular> <plural> <initialValue> [reporting]` - Creates a bucket. Reporting can be `admin`, `global`, `player`, or `none` (default).
* `/score-bucket <playerName> <bucketId>` - Switches a player's active bucket.
* `/score-bucket all <bucketId>` - Changes the server's default bucket and clears individual bucket assignments.
* `/score-run start <bucketId> <topN> <true|false> target <score>` - Starts a run that ends when a score reaches the target.
* `/score-run start <bucketId> <topN> <true|false> duration <time>` - Starts a timed run. Use a positive number and lowercase `s`, `m`, `h`, `d`, or `w`, such as `4h`.
* `/score-run start <bucketId> <topN> <true|false> until-disabled` - Starts a run with no automatic end, including across clean restarts.
* `/score-run stop <bucketId>` - Ends an active run.

## Permissions

The `scorekeeper.admin` permission (default: operators) is required for
`score-archive`, bucket creation, switching another player/all players, and run
management. Viewing/selecting your own bucket is open. Score get/add/subtract/reset
and archive-list have no permission checks; add, subtract, and reset can target
**any online player**. Restrict access externally if this does not fit your server.

## Features

* Create score buckets with custom singular/plural labels, initial values, and reporting policies.
* Reporting can notify operators/admins, everyone online, only the scored player, or nobody.
* Start target-score, timed, or until-disabled high-score runs with configurable top-N standings and optional score resets.
* Archive player, bucket, or all-bucket scores; archived scores reset to their bucket's initial value.
* Persist bucket definitions, assignments, scores, active runs, and archives in the plugin's data folder.

Reporting defaults to `none`, so successful score mutations can be silent; use
`/score-get` to inspect the number. Changed scores use singular units for exactly
1, plural otherwise. Scores may be negative and overflow is not checked.

Runs require top N of at least 1 and a literal `true`/`false` reset argument.
For example, `/score-run start points 3 true duration 4h`. Run reporting overrides
the bucket policy: the scored player always receives changes, and current top-N
players' changes are reported globally. Rankings include all recorded bucket
entries, even offline players; ties use case-insensitive name then UUID and
consecutive places. Final results include each online player's place. Reset runs
restore previous scores, while non-reset runs keep earned scores. Timed runs use
wall-clock time, including time offline; an existing target can finish a non-reset
run immediately.

Stop a run before archiving its bucket. Final run results are announced/logged,
not automatically archived. Archives and live state are saved to
`plugins/ScoreKeeper/archives.yml` and `scores.yml` **on clean shutdown/plugin
disable**, not after every command. Crashes can lose changes since the last save.
Do not edit data files while the server is running.

## API

Other plugins can create buckets with
`createBucket(id, singular, plural, initialValue, ScoreReporting)`, use bucket-aware
score methods, and manage runs with `startTargetScoreRun`, `startTimedScoreRun`,
`startUntilDisabledScoreRun`, and `stopHighScoreRun`. Declare a dependency on
`ScoreKeeper` and call the enabled plugin API on Paper's main thread.
See the [integration reference](https://github.com/headhunter45/ScoreKeeper/blob/develop/docs/plugin-integration-and-event-wiring.md).

## Compatibility

Built exclusively for Paper 26.2 and 26.3 with the Paper API and Adventure APIs. Bukkit and Spigot servers are not supported.

Source and development instructions are in the
[repository](https://github.com/headhunter45/ScoreKeeper). Licensed under GNU AGPL v3 or later.
