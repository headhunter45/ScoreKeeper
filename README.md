# ScoreKeeper Plugin

## Running

### Commands

For commands with an optional player name, players can omit it to target themselves. Console and RCON must provide a player name for score get/add/subtract/reset commands.

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
* `/score-run start <bucketId> <topN> <reset:true|false> target <score>` - Starts a run that ends when a score reaches the target.
* `/score-run start <bucketId> <topN> <reset:true|false> duration <time>` - Starts a timed run. Use a positive number followed by lowercase `s`, `m`, `h`, `d`, or `w` (for example, `4h`).
* `/score-run start <bucketId> <topN> <reset:true|false> until-disabled` - Starts a run with no automatic end.
* `/score-run stop <bucketId>` - Ends an active run.

The `scorekeeper.admin` permission (default: operators) is required for archive commands, bucket creation/administration, and high-score run management. Score get/add/subtract/reset and archive-list commands currently have no permission checks; add, subtract, and reset can target any online player.

When a player's active bucket changes, they are told the bucket name and their current score with its singular or plural unit.

Bucket reporting defaults to `none`; `admin` notifies operators and score admins, `global` notifies all online players, and `player` notifies only the scored player. Other plugins can create buckets with `createBucket(id, singular, plural, initialValue, reporting)` and use the bucket-aware score methods. Existing score methods operate on the player's active bucket. Bucket definitions, reporting policies, assignments, and scores are saved in `plugins/ScoreKeeper/scores.yml`; archives are saved separately in `plugins/ScoreKeeper/archives.yml`.

Other plugins can start runs with `startTargetScoreRun`, `startTimedScoreRun`, or `startUntilDisabledScoreRun`, and end one with `stopHighScoreRun`. During a run, players receive their own score changes, while changes by current top-`n` players are reported globally. Finished runs publish a deterministic score-ordered table and each online player's place; reset runs restore the pre-run bucket scores.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for development environment setup instructions.

Tooling note:
* Gradle is configured with Java Toolchains and will automatically download an appropriate JDK when needed during builds.
