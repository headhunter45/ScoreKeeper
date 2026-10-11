# Score Keeper Tasks

**Done** means implemented in source, not validated by automated behavioral tests.
**Ready** means not yet implemented. See [docs/Modernize.md](docs/Modernize.md)
for the broader roadmap and [docs/human-tests.md](docs/human-tests.md) for manual
pre-release validation.

| ID | Status | Title |
| --- | --- | --- |
| SK-01 | Done | Save scores between clean server launches using enable-time load and disable-time save. Crash-safe/periodic persistence is still future work. |
| SK-02 | Done | Track independent UUID-keyed scores by named bucket with singular/plural units and initial values; support default and individual assignments. |
| SK-03 | Done | Add per-bucket `admin`, `global`, `player`, and `none` reporting policies. |
| SK-04 | Done | Notify affected online players when their active bucket changes, including current score and units. |
| SK-05 | Done | Implement target-score, timed, and manually stopped bucket runs with configurable top N, optional initial reset/snapshot restoration, global leader reporting, and final personal standings. Active runs persist across clean restarts. |
| SK-06 | Done | Archive player, bucket, or all-bucket scores and reset live entries to initial values; list archive IDs and contents. |
| SK-07 | Done | Replace the release plugin with Git-sensitive semantic versioning (minimum `0.2.1`, `v` tag prefix) and re-enable Gradle configuration cache. Versions are Git-derived, not a hardcoded snapshot. |
| SK-08 | Ready | Add JUnit 5 test infrastructure and keep the default Gradle `test` task server-free. Use a compatible in-memory Paper harness such as MockBukkit; use Mockito only for narrow boundaries where a harness is unnecessary. |
| SK-09 | Ready | Add fast score/bucket tests: validation and duplicates, initial values, UUID isolation, lazy entries, default/per-player selection, and signed add/subtract/reset behavior. |
| SK-10 | Ready | Test YAML round trips, assignments, active runs/snapshots, archive contents/resets, legacy loading, and malformed/unknown entries using temporary data directories. |
| SK-11 | Ready | Test command validation/dispatch/permissions, reporting/messages, bucket switches, ranking/ties, top-N thresholds, target/duration/manual completion, and restoration. Use controllable time/scheduling where needed. |
| SK-12 | Ready | Add a separate opt-in `integrationTest` source set/task for real Paper loading, command dispatch, companion API wiring, and restart persistence. Keep it outside normal `test`/`build`. |
| SK-13 | Ready | Add an option to `/score-get` for a specific bucket, preserving current optional-player syntax. |
