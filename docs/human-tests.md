# Score Keeper Human Testing

Use this manual pre-release checklist on disposable Paper 26.2 and 26.3 servers
(test both before advertising both). Score Keeper stores data in its plugin folder
and interacts with players, command senders, and scheduled runs; a passing build
alone does not verify these workflows. These are test scenarios, not passing results.
See [CONTRIBUTING.md](../CONTRIBUTING.md) for setup and helper-script safety, and
[the domain reference](domain-score-tracking.md) for expected behavior.

## Test Setup

- Use Java 25 and a backed-up test server. Keep the server log available throughout testing.
- Use at least three test players for rankings: an operator, a non-operator with `scorekeeper.admin`, and an ordinary player. Use a console or RCON session for non-player sender cases.
- Before each run, preserve `plugins/ScoreKeeper/scores.yml` and `archives.yml`, or use a fresh test server. Archive, reset, and bucket-wide commands intentionally change scores.
- Record plugin artifact/version, Paper version/build, Java version, command sender, arguments, expected and observed messages, and score values before and after each operation. Save pass/fail notes and relevant logs outside player data committed to the repo.
- Ensure only the intended Score Keeper jar is installed. Stop with `stop` in the server console so disable-time saves run. Do not use helper scripts that can stop other Paper processes on a shared host.

## Human Checklist

### Startup and score commands

- Start Paper and confirm Score Keeper enables, registers every documented command, and loads existing data without errors.
- As a player, run `/score-get`, `/score-add 10`, `/score-subtract 3`, and `/score-reset`; inspect `/score-get` after each mutation. Repeat with console/RCON, supplying a player name where required. Missing targets from non-player senders must produce usage, not self-scoring.
- Try zero and negative amounts: they are valid signed integers (for example, adding `-3` decreases the score). Try missing/non-numeric/out-of-range amounts: these should produce usage/errors without changing scores. At integer boundaries, record the current wrapping behavior; there is no overflow guard.
- Target offline/unknown names and supply extra arguments; verify errors/usage without unrelated changes. Confirm gameplay such as mob kills does not award points unless a companion plugin is installed.
- Use two players to verify each player's score is independent. Change a player's Minecraft name if practical, then confirm their score remains associated with their UUID.
- Check singular/plural labels at 1, 0, negative, and multiple values in reporting, bucket-change, and run/archive messages. `/score-get` only displays a number; it does not show bucket/units.

### Buckets and reporting

- Create a test bucket with a nonzero initial value and distinctive singular/plural labels, for example `/score-bucket create Bread loaf loaves 5 player`. Verify invalid/duplicate IDs, invalid initial values, and unknown reporting policies produce errors. IDs are case-sensitive; command labels each occupy one argument. Use a companion-plugin API test if checking blank/null labels that cannot be supplied naturally as command arguments.
- Switch one player to the bucket, add and subtract points, and confirm other players remain in their own active bucket. Switch back and verify the original score is unchanged.
- Change the server-wide default with `/score-bucket all <bucketId>`. Confirm individual assignments are cleared, online players receive the appropriate change notice, and new players use the new default.
- For each reporting policy (`none`, `player`, `admin`, `global`), change a score and verify exactly the intended audience receives the report. Test with an operator, a `scorekeeper.admin` player, an ordinary player, and the scored player.
- Exercise authorized/unauthorized bucket creation and switching another/all players: these require `scorekeeper.admin`. An ordinary player must still be able to view/select their own bucket. Selecting the already-active bucket should not produce a bucket-change notice.
- Set reporting to `none` and verify a successful add/subtract/reset can be silent. Writing the same score should not send a change report under any policy. `admin` reports should also reach the server log.
- Note that the README says score get/add/subtract/reset currently have no permission checks and can target any online player. Confirm that exposure is acceptable for the server before enabling these commands publicly.

### Archives

- Archive one player's active-bucket score as an admin, then inspect `/score-archive-list` and `/score-archive-list <archiveId>`. Confirm the snapshot value/bucket and the live score returns to that bucket's initial value. Ordinary players must be denied archive creation but can list/view archives.
- Repeat for `/score-archive bucket <bucketId>` and `/score-archive all`. Confirm only the selected bucket or all buckets are archived and reset as requested.
- Check archive IDs are listed newest first, then restart and confirm archives and their contents remain available.
- Try an unknown archive ID or bucket ID and verify a useful failure without unrelated scores being changed.
- Include recorded offline players and ties: entries sort descending by score then UUID, not run-style name ties. Check empty bucket archives before/after restart and record any omitted empty bucket sections.
- Attempt player/bucket/all archiving while a selected bucket has an active run. It must fail before changing any selected scores; stop the run and retry. Archive resets should not send normal per-score reporting.

### High-score runs

- Start target/timed runs with at least three players. Use literal booleans, for example `/score-run start Bread 2 true duration 10s`. Top N must be at least 1. Reject invalid booleans, zero/negative/overflowing duration, uppercase time suffixes, unknown buckets, and duplicate runs.
- A reset target must exceed the initial value. Without reset, a pre-existing score at/above the target may finish immediately; verify this rather than assuming every start remains active.
- Change scores for players inside and outside the current top-N. Verify individual score-change messages and global reporting for current top-N players match the run behavior.
- End runs by reaching the target, expiration, and `/score-run stop`. Also start an `until-disabled` run and stop it manually.
- Check descending score, case-insensitive name then UUID tie-breaking, distinct consecutive places for ties, each online player's place, unranked players, and the top-N cutoff. Include recorded offline players; top N limits display, not stored entries.
- With reset enabled, verify scores begin at the initial value and pre-run entries are restored exactly. A player first recorded during the run should return to the initial value on the next read after completion. With reset disabled, scores remain as earned. Final personal results must show run scores, not restored scores.
- Restart cleanly while a run is active. Confirm snapshots survive, timed expiration includes time offline, overdue runs finish on enable, and `until-disabled` resumes until explicitly stopped. Confirm completion happens once and no automatic archive is created.
- Run two buckets concurrently and confirm independent completion/restoration. Observe timed completion under lag: checks run every 20 ticks, not at an exact wall-clock second.

### Persistence and upgrades

- Restart after changing scores, bucket definitions, reporting policies, the default bucket, and per-player bucket assignments. Verify all values and assignments persist.
- Test a clean data folder and copies of older files, including flat top-level UUID scores (loaded into `points`). Verify default/reporting fallbacks and invalid UUID/bucket/run warnings. For malformed types/unknown assignments, record actual getter defaults/ignored values; current loading is not comprehensive validation.
- Check `scores.yml` version 3 and the archive fields after a clean shutdown. Both files save only on disable, not each mutation. On an explicitly authorized disposable server, compare a forced crash with the last clean save: loss of recent changes is a known limitation, not successful durability.
- If using a companion plugin, verify `depend: [ScoreKeeper]` load order, main-thread awards into an explicit bucket, and visible failure when Score Keeper is absent/disabled. Verify no unintended active-bucket switch.

## Automated Coverage

There are currently no Java test sources or test dependencies in this project,
and no integration-test source set. Gradle `test` is `NO-SOURCE` and does not
exercise Score Keeper behavior. Scenarios remain unverified until actually
tested; SK-08 through SK-12 in [TODO.md](../TODO.md) and
[the modernization plan](Modernize.md) describe pending automated coverage.
