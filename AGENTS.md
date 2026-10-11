# Score Keeper: Shared Working Rules

These rules apply to agents and human contributors. Start with
[CONTRIBUTING.md](CONTRIBUTING.md) for setup, tasks, server safety, and release
procedures; [README.md](README.md) is the user-facing behavior reference.

## Quick orientation

- Java 25, Gradle 9.8.0 wrapper, Paper 26.2/26.3 only; no Bukkit/Spigot target.
- [ScoreKeeperPlugin.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreKeeperPlugin.java)
  owns lifecycle, buckets, UUID-keyed scores, YAML persistence, archives,
  reporting, and scheduled high-score runs.
- [ScoreBucket.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreBucket.java)
  validates immutable definitions; [ScoreReporting.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreReporting.java)
  defines audiences. [commands/](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/commands/)
  contains eight executors wired in `onEnable` and
  [plugin.yml](src/main/resources/plugin.yml).
- [CONTRIBUTING.md](CONTRIBUTING.md#project-overview-and-source-map) owns the source
  map and architecture overview. Consult the [domain reference](docs/domain-score-tracking.md)
  or [integration reference](docs/plugin-integration-and-event-wiring.md) only when
  the task needs their detail. There are no built-in gameplay listeners or registered API service.

## Change discipline

- Inspect the source and current diff before editing. Preserve unrelated work;
  make scoped, complete changes and document intentional behavior changes.
- Use **Score Keeper** in human-facing prose/display text. Keep `ScoreKeeper`
  for repository URLs, plugin ID/data folder, Gradle/artifact names, and Java
  symbols. Preserve scraper identifiers exactly as currently stored in
  [project-description.json](docs/project-description.json).
  Ask before renaming existing files or changing these integration identifiers.
- Follow existing Java patterns, Adventure components, UUID identity, and
  plugin logging. Use Spotless rather than hand-formatting Java; preserve the
  [AGPL header](config/license-header.txt). Do not add silent error recovery or
  success messages for failed operations.
- Keep state and Paper API calls on the main thread. Do not introduce async
  map access; marshal asynchronous results through Paper's synchronous scheduler.
- Wire command/API changes across executors, manifest, lifecycle, reporting,
  persistence, and documentation as applicable. Preserve existing public overloads
  and legacy YAML loading unless a migration is explicitly agreed.
- Preserve documented quirks until intentionally changed: unguarded score
  mutations, signed integer arithmetic, lazy score entries, case-sensitive bucket
  IDs, shutdown-only saves, and run snapshot restoration. Do not "fix" these as
  incidental cleanup.
- Prefer existing helpers and dependencies. Do not introduce a service layer,
  test harness, runtime dependency, or supported platform without a task need.
- Edit source, not generated `build/`, `bin/`, or `.gradle/` files. Never read,
  expose, or commit `.env` credentials, server player data, or publishing tokens.

## Validation

- With JDK 25/JAVA_HOME configured, use `./gradlew spotlessCheck build` for Java
  changes (`.\gradlew.bat` on Windows). For macOS/Linux bootstrapping, use
  `./tools/build-plugin.sh spotlessCheck`. Run narrower relevant tasks when enough.
- `./gradlew spotlessApply` applies Java style; review the resulting diff and
  avoid unrelated formatting churn.
- There are currently **no Java test sources/dependencies**; `test` is
  `NO-SOURCE`, and no `integrationTest` task exists. Never claim a build proves
  behavior or claim JUnit coverage. When tests are added, keep normal tests
  server-free; real Paper integration tests should be explicit/opt-in.
- Use [human-tests.md](docs/human-tests.md) for affected runtime scenarios and
  pre-release testing. Report checks actually run and any remaining unverified
  behavior. For docs-only work, check relative links and metadata instead of building.

## Documentation and operational safety

- Keep [README.md](README.md), [Modrinth description](docs/modrinth-description.md),
  [project-description.md](docs/project-description.md), and
  [project-description.json](docs/project-description.json) aligned with source.
  Validate the JSON as a slug-keyed object against
  [projects.schema.json](docs/projects.schema.json). Keep manual scenarios
  separate from automated coverage claims.
- When updating [project-description.json](docs/project-description.json),
  preserve the existing top-level project key, `id`, `title`, `docId`, and all
  image URLs (`image.url` and `headerImage.url`, when present). These are fixed
  in the consuming project; routine metadata updates must not reset them.
  Preserve a null or absent `headerImage` as well. Image paths such as
  `/projects/score-keeper/logo.png` are relative to the consuming project's
  root, not this repository: do not resolve them locally, rewrite them as
  repository URLs, or replace them with generated/remote image URLs.
  Change these protected fields only when the user explicitly requests it.
- New project plans belong in [docs/](docs/); use checkboxes for actionable
  work only. [Modernize.md](docs/Modernize.md) supersedes the historical `.cursor`
  plan/rules. Planning/documentation tasks should not opportunistically implement
  the plan's code changes.
- Do not run server start/stop, deploy, restore, tagging, or publishing operations
  without explicit authorization. Bash start/stop currently match all local
  Java `paper-*.jar` processes; restore replaces archived top-level items.
  PowerShell start/stop can force termination. Prefer a clean console `stop`
  and inspect exact server paths/processes on isolated test machines.
