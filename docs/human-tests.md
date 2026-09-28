# ScoreKeeper Human Testing

Use this checklist on a disposable Paper 26.2 or 26.3 server. ScoreKeeper stores data in its plugin folder and interacts with online players, command senders, and scheduled high-score runs; a passing build alone does not verify these in-game workflows.

## Test Setup

- Use Java 25 or newer and a backed-up test server. Keep the server log available throughout testing.
- Create at least two test players: one operator/admin and one ordinary player. Use a console or RCON session for console-sender cases.
- Before each run, preserve `plugins/ScoreKeeper/scores.yml` and `archives.yml`, or use a fresh test server. Archive, reset, and bucket-wide commands intentionally change scores.
- Record command sender, arguments, expected and observed messages, and score values before and after each operation.

## Human Checklist

### Startup and score commands

- Start Paper and confirm ScoreKeeper enables, registers every documented command, and loads existing data without errors.
- As a player, run `/score-get`, `/score-add`, `/score-subtract`, and `/score-reset`; check the displayed values and the configured initial value. Repeat with console/RCON, supplying a player name where required.
- Try zero, negative, very large, missing, and non-numeric amounts. Confirm invalid input is handled clearly and does not unexpectedly alter a score.
- Use two players to verify each player's score is independent. Change a player's Minecraft name if practical, then confirm their score remains associated with their UUID.
- Check singular/plural unit labels at values of 1, 0, and multiple units, and confirm messages show the correct bucket and value.

### Buckets and reporting

- Create a test bucket with a nonzero initial value and distinctive singular/plural labels. Verify invalid IDs, blank labels, and duplicate IDs are rejected cleanly.
- Switch one player to the bucket, add and subtract points, and confirm other players remain in their own active bucket. Switch back and verify the original score is unchanged.
- Change the server-wide default with `/score-bucket all <bucketId>`. Confirm individual assignments are cleared, online players receive the appropriate change notice, and new players use the new default.
- For each reporting policy (`none`, `player`, `admin`, `global`), change a score and verify exactly the intended audience receives the report. Test with an operator, a `scorekeeper.admin` player, an ordinary player, and the scored player.
- Exercise bucket creation, switching, and administration as authorized and unauthorized senders. The README says archive, bucket administration, and run management require `scorekeeper.admin`; verify the behavior on the target server.
- Note that the README says score get/add/subtract/reset currently have no permission checks and can target any online player. Confirm that exposure is acceptable for the server before enabling these commands publicly.

### Archives

- Archive one player's active-bucket score, then inspect `/score-archive-list` and `/score-archive-list <archiveId>`. Confirm the saved value and bucket are correct and the live score returns to that bucket's initial value.
- Repeat for `/score-archive bucket <bucketId>` and `/score-archive all`. Confirm only the selected bucket or all buckets are archived and reset as requested.
- Check archive IDs are listed newest first, then restart and confirm archives and their contents remain available.
- Try an unknown archive ID or bucket ID and verify a useful failure without unrelated scores being changed.

### High-score runs

- Start a target-score run and a timed run using a test bucket and at least three players. Confirm the run starts with the requested top-N and reset setting.
- Change scores for players inside and outside the current top-N. Verify individual score-change messages and global reporting for current top-N players match the run behavior.
- End runs by reaching the target, expiration, and `/score-run stop`. Also start an `until-disabled` run and stop it manually.
- Check final ranking order, ties, each online player's reported place, and the displayed top-N cutoff.
- With reset enabled, verify scores begin at the bucket initial value and pre-run scores are restored after completion. With reset disabled, confirm scores remain as earned.
- Restart while a run is active. Confirm the run and reset snapshot survive, timed runs expire at the intended time, and stopping or completing the run produces one final result rather than duplicate results.

### Persistence and upgrades

- Restart after changing scores, bucket definitions, reporting policies, the default bucket, and per-player bucket assignments. Verify all values and assignments persist.
- Test a clean data folder and an existing/older `scores.yml` or `archives.yml` on a backed-up server. Confirm startup handles defaults, known legacy data, and malformed or unknown entries without silently corrupting valid data.
- Check saved files after a clean shutdown and after a forced test restart; distinguish expected last-save behavior from data loss.

## Automated Coverage

There are currently no Java test sources or test dependencies in this project, and no integration-test source set. The Gradle `test` task therefore does not currently exercise ScoreKeeper behavior. All scenarios above should be treated as unverified until tested in-game; the “Ready” test tasks in `TODO.md` describe work that has not yet been added.
