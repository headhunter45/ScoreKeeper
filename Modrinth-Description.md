**ScoreKeeper Plugin**

ScoreKeeper tracks player scores in configurable buckets. Use it for event points, competitions, or other server-side scoring systems, and integrate it with other plugins through the public API.

## Commands

Players can omit an optional player name to target themselves. Console and RCON must provide a player name for score get/add/subtract/reset commands.

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
* `/score-run start <bucketId> <topN> <reset:true|false> target <score>` - Starts a run that ends when a score reaches the target.
* `/score-run start <bucketId> <topN> <reset:true|false> duration <time>` - Starts a timed run. Use a positive number and lowercase `s`, `m`, `h`, `d`, or `w`, such as `4h`.
* `/score-run start <bucketId> <topN> <reset:true|false> until-disabled` - Starts a run with no automatic end.
* `/score-run stop <bucketId>` - Ends an active run.

## Permissions

The `scorekeeper.admin` permission (default: operators) is required for archive commands, bucket creation/administration, and high-score run management. Score get/add/subtract/reset and archive-list commands currently have no permission checks; add, subtract, and reset can target any online player.

## Features

* Create score buckets with custom singular/plural labels, initial values, and reporting policies.
* Reporting can notify operators/admins, everyone online, only the scored player, or nobody.
* Start target-score, timed, or until-disabled high-score runs with configurable top-N standings and optional score resets.
* Archive player, bucket, or all-bucket scores; archived scores reset to their bucket's initial value.
* Persist bucket definitions, assignments, scores, active runs, and archives in the plugin's data folder.

## API

Other plugins can create buckets with `createBucket(id, singular, plural, initialValue, reporting)`, use bucket-aware score methods, and start or stop high-score runs with `startTargetScoreRun`, `startTimedScoreRun`, `startUntilDisabledScoreRun`, and `stopHighScoreRun`.

## Compatibility

Built exclusively for Paper 26.2 and 26.3 with the Paper API and Adventure APIs. Bukkit and Spigot servers are not supported.
