# Score Keeper - Score-Tracking Domain

This documents implemented behavior in
[ScoreKeeperPlugin.java](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreKeeperPlugin.java),
[ScoreBucket.java](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreBucket.java),
and [ScoreReporting.java](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreReporting.java).
For command syntax see [README.md](../README.md); for runtime wiring see
[the integration reference](plugin-integration-and-event-wiring.md).

## Buckets and live scores

- A bucket has an immutable ID, singular label, plural label, initial integer
  value, and reporting policy. IDs match `[A-Za-z0-9_-]+`, are case-sensitive,
  and must be unique. Labels must be nonblank; reporting cannot be null.
- A fresh server has `points` (`point`/`points`, initial value `0`, reporting `none`).
  Loading bucket data restores this bucket if it is missing.
- `_bucketScores` maps bucket ID to `UUID -> int`. Players can have independent
  scores in multiple buckets. Names are not used as identity keys.
- The server default bucket and optional UUID-specific assignments determine a
  player's active bucket. Selecting the default removes their override.
  `switchAllPlayers` changes the default and clears **all** assignments,
  including offline players' assignments, without deleting scores.
- `getScore` lazily registers a missing score at the bucket's initial value.
  Joining alone does not create an entry. Reads therefore affect who appears
  in a later archive or run ranking.
- Add/subtract perform ordinary Java integer arithmetic. Zero/negative amounts
  are allowed, scores can be negative, and overflow wraps. There are no bonuses,
  weighting, decay, or automatic gameplay scoring.
- Reset writes the bucket initial value, not necessarily zero. Existing
  no-bucket API methods use the active bucket; explicit-bucket overloads do not
  change the player's assignment.

## Reporting and bucket changes

For a changed value written through `setScore`, outside a run:

| Policy | Audience |
| --- | --- |
| `none` (default) | Nobody |
| `player` | Scored player |
| `global` | All online players |
| `admin` | Online operators or holders of `scorekeeper.admin`, plus the server log |

Writing the same value sends no score-change report. Commands that change scores
do not independently promise success messages; inspect `/score-get` when reporting
is `none`. A bucket change tells an affected online player the ID and current score.
Unit formatting uses the singular label for exactly `1`, plural otherwise.
`/score-get` itself displays only the number, without bucket or unit labels.

Run reporting overrides the bucket policy. Archive resets and run start/restore
directly update maps and do not send ordinary per-score change reports.

## High-score runs

Each bucket can have at most one active run; different buckets may run concurrently.
Top N must be at least one. There are three lifetime modes:

- **Target score:** finishes when any recorded score reaches/exceeds the target.
  With reset enabled, the target must exceed the initial value. Without reset,
  existing scores can finish a run immediately when it starts.
- **Duration:** a positive duration, stored as an absolute epoch-millisecond
  expiration. Time offline counts toward expiration.
- **Until disabled:** no automatic end; stop explicitly. Plugin disable saves the
  run rather than finishing it, so it resumes on restart.

Starting with reset enabled snapshots existing bucket entries and sets them to
the initial value. During the run, a changed score is always reported to its
player. If that player is in the top N **after the change**, the report is sent
to all other online players too, once per recipient.

Completion is checked immediately at start/load, synchronously every 20 ticks,
and on a changed target-run score. Manual stop also completes the run.
The scheduled check uses wall-clock time; under server lag it need not fire at
an exact real-time second.

### Ranking and results

Rankings include all recorded entries in that bucket, including offline players
and entries read/created before the run. The order is:

1. Descending score.
2. Case-insensitive player name, using Paper's offline-player lookup.
3. UUID string to break remaining ties.

Unknown/blank names fall back to the UUID string. Ties have distinct consecutive
places, not shared ranks. Top N limits display/reporting, not stored entries.
Every online player receives the final top-N table and their own full-ranking
place/score, or an unranked result at the initial value. The final table is logged.

With reset enabled, completion replaces the bucket map with its pre-run snapshot.
Players first recorded during the run are removed from that map and get the initial
value on their next read. Personal results refer to the finished run, not restored
scores. Without reset, earned scores remain. Run completion does **not** create a
score archive.

## Archives

Player, single-bucket, and all-bucket archives create immutable snapshots with a
random UUID ID, scope text, epoch-millisecond creation time, and nested
`bucket ID -> UUID -> int` scores. A player archive uses their active bucket;
bucket/all archives include all recorded entries, even offline players.

Live archived entries reset to their respective initial values. Archiving fails
before changing any scores if a selected bucket has an active run; stop the run
first. Archives do not cap or prune entries.

[ScoreArchiveListCommand.java](../src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/commands/ScoreArchiveListCommand.java)
lists archives newest first. Within an archive, scores display descending with
UUID-string tie-breaking (not the run's name tie-break). Names and unit labels are
resolved at display time, not captured in the snapshot; missing bucket definitions
use the generic unit `scores`.

## Persistence

Both files live in `plugins/ScoreKeeper/`. Loading occurs on enable and saving
on disable. There is no periodic autosave or immediate archive write despite
the command's "Saved score archive" acknowledgement; a crash can lose recent
state. Back up only after a successful clean shutdown.

### `scores.yml` (currently writes version 3)

```yaml
version: 3
default-bucket: points
buckets:
  points:
    singular: point
    plural: points
    initial-value: 0
    reporting: none
    scores:
      "00000000-0000-0000-0000-000000000001": 10
player-buckets:
  "00000000-0000-0000-0000-000000000002": points
high-score-runs:
  points:
    top-n: 3
    reset-scores: true
    lifetime: duration
    target-score: 0
    expires-at: 1800000000000
    previous-scores:
      "00000000-0000-0000-0000-000000000001": 25
```

This illustrates field shapes, not a file to install unchanged. Overrides equal
to the default bucket are normally omitted. Saved lifetime strings are
`target_score`, `duration`, and `until_disabled`. The loader reads structure,
not a version-dispatch migration table.

If `buckets` is absent, the loader treats top-level UUID/integer entries as legacy
`points` scores. Invalid bucket definitions/UUIDs/runs are skipped with warnings;
invalid reporting falls back to `none`, and an unknown default falls back to
`points`. Unknown bucket assignments are ignored. Bukkit YAML getters supply
defaults for some missing/wrongly typed values: do not assume every malformed
value is rejected or logged.

### `archives.yml`

```yaml
archives:
  "00000000-0000-0000-0000-000000000003":
    scope: bucket points
    created-at: 1800000000000
    buckets:
      points:
        scores:
          "00000000-0000-0000-0000-000000000001": 10
```

This file has no explicit format version. Empty bucket score maps may be omitted
on serialization because only individual score entries are written.
The two files are saved separately; there is no cross-file transaction or atomic
replacement guarantee. Save I/O errors use the plugin logger. Never edit live
data while the server is running: shutdown can overwrite it.

## Validation status

These are source-level behavior descriptions, not claims of passing automated
tests. Pending coverage is tracked in [TODO.md](../TODO.md). Before release,
use [human-tests.md](human-tests.md), particularly ranking ties, empty buckets,
restoration, malformed data, and restart/upgrade cases.
