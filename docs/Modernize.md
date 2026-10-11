# Score Keeper Modernization Plan

Current baseline: October 2026. This replaces
[the historical Cursor plan](../.cursor/plans/Modernize.md) as the maintained
roadmap. It is a plan, not authorization to change runtime behavior or publish.
Read [CONTRIBUTING.md](../CONTRIBUTING.md) and [AGENTS.md](../AGENTS.md) first.

## Baseline: already implemented

The old Maven/Java 17/older Minecraft migration is no longer pending.

| Area | Current implementation |
| --- | --- |
| Build | Gradle 9.8.0 wrapper, Java 25 toolchain, Foojay resolver, configuration cache, Spotless |
| Paper target | API `26.2.build.129-stable`, manifest API `26.2`, documented Paper 26.2/26.3 support |
| Identity and messaging | UUID-based scores, Paper/Adventure messages, plugin logger, online `getPlayerExact` lookup |
| Domain | Named buckets, initial values, four reporting policies, default/individual assignments |
| Persistence | Version-3 scores YAML, archives YAML, legacy flat UUID score loading, shutdown save/enable load |
| Competitions | Target/timed/manual runs, scheduled checks, top-N rankings, score snapshots/restoration |
| Tooling | Bash build/deploy/server/backup/restore/local and Modrinth publishing; legacy PowerShell subset |
| Documentation | Contributor entry point, shared agent rules, current domain/integration references, manual release checklist, schema-shaped project metadata |

Implemented is not the same as verified on every advertised server version.
There are no automated Java tests yet; the CI `test` task is currently `NO-SOURCE`.
There is no real-server integration task and no automatic publishing workflow.

## Phase 1: reliable regression coverage (SK-08 through SK-11)

These are prerequisites for changing persistence, permissions, arithmetic, or
run behavior. Keep the default test task server-free.

- [ ] Add JUnit 5 infrastructure and an in-memory Paper harness compatible with
  the current Java/Paper API (for example, MockBukkit if compatible). Use Mockito
  only for narrow boundaries where a harness is unnecessary; verify compatibility
  before choosing/pinning dependencies.
- [ ] Cover bucket ID/label/reporting validation, duplicates, initial values,
  UUID isolation, lazy reads, active/default selection, all-player assignment
  clearing, and signed add/subtract/reset semantics.
- [ ] Cover YAML round trips in temporary data directories: buckets, scores,
  assignments, runs/snapshots, archives, and legacy flat scores. Include invalid
  UUIDs, unknown buckets, malformed values, and empty archive buckets.
- [ ] Cover all eight command executors: sender/self/online-target resolution,
  literal reset booleans, duration units/overflow, usage/errors, and existing
  permission boundaries.
- [ ] Verify Adventure reporting audiences, changed-vs-unchanged scores, unit
  labels, self/other bucket notices, and no duplicate run change notifications.
- [ ] Introduce a small controllable clock/scheduler seam where necessary for
  deterministic run tests; avoid a wholesale architecture rewrite.
- [ ] Verify descending/name/UUID run ties, archive UUID ties, offline/unranked
  entries, top-N boundaries, immediate targets, duration expiration, manual stop,
  resumed runs, and exact pre-run score restoration (including new run entrants).
- [ ] Make CI produce real test reports and fail on these regressions once tests
  exist; do not mistake missing reports for tested coverage.

## Phase 2: explicit real-server validation (SK-12)

- [ ] Add an opt-in `integrationTest` source set/task only for behavior requiring
  actual Paper. Keep it out of normal `test`/`build`, use disposable directories,
  and own/stop only the processes it starts.
- [ ] Load the built jar on both Paper 26.2 and 26.3 with Java 25; verify real
  command registration/dispatch, Adventure output, scheduler completion, API
  integration/load order, and persistence across clean restarts.
- [ ] Run [human-tests.md](human-tests.md) on both versions before release.
  Record artifact version, Paper build, Java version, observations, and logs.
- [ ] Review startup/runtime deprecation warnings and unsupported API calls
  found by builds/runtime tests; update compatibility claims only with evidence.

Do not broaden support to Spigot, Bukkit, Folia, or future Paper versions by
documentation changes alone.

## Phase 3: persistence and correctness hardening

Choose behavior changes explicitly and add regression tests first.

- [ ] Design safe periodic/dirty-state saves or explicit saves so crashes do not
  lose all changes since shutdown. Decide save cadence and I/O threading while
  snapshotting mutable state on the main thread.
- [ ] Design atomic replacement/recovery and consistency between score resets
  and archives across the two YAML files. Preserve or migrate legacy data, and
  ensure save failures are visible to administrators/callers.
- [ ] Define validation rules for malformed YAML rather than relying on defaulting
  getters. Cover invalid integer types, assignments, missing archive fields,
  empty snapshots, and recovery without overwriting good data.
- [ ] Decide whether integer overflow should fail, clamp, or use a wider score
  type. Preserve negative-score use cases and provide an explicit data/API
  migration if the type changes.
- [ ] Review the misleading `until-disabled` mode name: document current
  resume-across-disable behavior until any new end-on-disable mode is deliberately
  designed. Do not silently rename the command or change saved lifetime values.

## Phase 4: command and integration ergonomics

- [ ] Design permission nodes/defaults for score get/add/subtract/reset and archive
  listing; define self-vs-other access and a migration strategy before restricting
  currently open commands.
- [ ] Implement SK-13: explicit bucket selection for `/score-get`, with unambiguous
  syntax, validation, help, docs, and tests. Preserve existing optional-player usage.
- [ ] Align manifest/executor help with supported archive scopes, reporting
  arguments, and literal reset booleans; consider tab completion as a separate
  tested improvement.
- [ ] Evaluate a smaller API/domain boundary only where needed for tested
  behavior or companion integration. Retain public overload compatibility and
  avoid unnecessary service abstractions.

## Phase 5: safer developer and release tooling

- [ ] Scope Bash start/stop/backup/restore process checks to the configured server
  or a verified owned PID; never stop another project's Paper process.
- [ ] Verify the bootstrap helpers select an actual Java 25 JDK, not merely the
  first cached Java executable, and document supported hosts.
- [ ] Bring PowerShell setup/configuration/process management to deliberate
  parity with Bash or clearly retain a reduced supported surface. Prefer graceful
  shutdown over forced termination.
- [ ] Make deployment select/verify the intended artifact and version instead of
  relying only on newest modification time and a stale-source warning.
- [ ] Add lightweight local-link and JSON Schema checks for docs/metadata to CI,
  keeping the scraper's slug-keyed contract and existing filenames.
- [ ] Rehearse a tagged release: rebuild after tagging, verify manifest/version,
  run manual checks, review changelog and compatibility, upload deliberately, and
  update hosted Modrinth description separately.

## Completion criteria

A modernization item is done only when its behavior and failure modes have
appropriate tests or recorded manual evidence, directly related docs are current,
and existing data/API contracts are preserved or explicitly migrated. Use
[TODO.md](../TODO.md) for SK feature/task status; this plan organizes the broader
work and must not mark unchecked tests or future infrastructure as implemented.
