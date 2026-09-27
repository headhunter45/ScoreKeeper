# ScoreKeeper Plugin

## Running

### Commands

* `/score-get [playerName]` - Displays the player's score.
* `/score-add [playerName] <amount>` - Add amount points to the player's score.
* `/score-subtract [playerName] <amount>` - Subtracts amount points from the player's score.
* `/score-reset [playerName]` - Resets the player's score to 0.
* `/score-archive [playerName]` - Saves the player's score to a "High Scores" table and resets their current score to 0.
* `/score-bucket [bucketId]` - Switches your active bucket.
* `/score-bucket create <id> <singular> <plural> <initialValue> [admin|global|player|none]` - Creates a bucket (operators).
* `/score-bucket <playerName|all> <bucketId>` - Switches a player or the server default (operators).

NOTES:
* All commands will use the executing player in place of [playerName] if it is omitted.
* Permissions support is coming AFTER I get archive to work.
* The commands aside from get are intended for admins and whatnot as some other plugin should be using the methods instead of letting player's execute commands.

Bucket reporting defaults to `none`; `admin` notifies operators and score admins, `global` notifies all online players, and `player` notifies only the scored player. Other plugins can create buckets with `createBucket(id, singular, plural, initialValue, reporting)` and use the bucket-aware score methods. Existing score methods operate on the player's active bucket. Bucket definitions, reporting policies, assignments, and scores are saved in `plugins/ScoreKeeper/scores.yml`.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for development environment setup instructions.

Tooling note:
* Gradle is configured with Java Toolchains and will automatically download an appropriate JDK when needed during builds.
