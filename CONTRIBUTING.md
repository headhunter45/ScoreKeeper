# Contributing to Score Keeper

**Start here to work on Score Keeper.** This guide covers the project structure,
setup, validation, and publishing. The display name is **Score Keeper**; the
repository, Gradle project, plugin ID, and jar basename remain `ScoreKeeper`.

## Your first contribution

1. Read [the project overview](#project-overview-and-source-map) below.
   Consult [README.md](README.md) if you need user-facing commands and behavior.
2. Read [AGENTS.md](AGENTS.md) once for the shared rules humans and agents follow.
   This guide owns onboarding; that file owns change discipline and safety rules.
3. Follow [prerequisites and checkout](#prerequisites-and-checkout) to build.
4. Make a scoped change, then use [build and validation tasks](#build-format-and-validation-tasks).
   Set up a local Paper server only if the affected behavior needs runtime testing.
5. Run the relevant [manual checks](docs/human-tests.md) and report what you verified.

You do **not** need to read every document before starting. Use these deeper
references when your task needs them:

- [Domain reference](docs/domain-score-tracking.md): score rules, YAML layout,
  archives, and ranking.
- [Integration reference](docs/plugin-integration-and-event-wiring.md): detailed
  command/lifecycle wiring and API usage, especially for companion-plugin authors.
- [Modernization plan](docs/Modernize.md) and [TODO.md](TODO.md): planned work,
  not additional onboarding requirements.

The old `.cursor` plan and rules are historical context, not the current setup
or validation procedure. In particular, the Maven migration is already complete.

## Project overview and source map

Score Keeper is a command-driven Paper scoring engine: named buckets hold
UUID-keyed integer scores with custom units, initial values, and reporting.
It provides eight commands, score archives, high-score runs, and a public Java
API. Gameplay events do not award scores automatically; companion plugins can
provide that behavior.

| Source | Responsibility |
| --- | --- |
| [ScoreKeeperPlugin.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreKeeperPlugin.java) | Entry point, state, public API, persistence, reporting, ranking, and run completion |
| [ScoreBucket.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreBucket.java) | Immutable bucket definition and validation |
| [ScoreReporting.java](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/ScoreReporting.java) | Reporting policy enum and parsing |
| [commands/](src/main/java/com/majinnaibu/minecraft/plugins/scorekeeper/commands/) | Eight executors: arguments, online targets, selected permission guards, and messages |
| [plugin.yml](src/main/resources/plugin.yml) | Plugin identity, entry point, API version, command declarations, and admin permission |
| [tools/](tools/) | Developer/server/release Bash tooling; per-project values live in [project-settings.sh](tools/project-settings.sh) |
| [.env.example](.env.example) | Bash helper configuration template, not plugin configuration |

### Architecture in one minute

```text
Paper command -> executor parses/checks permissions/resolves online target
             -> ScoreKeeperPlugin selects bucket and updates UUID-keyed state
             -> bucket reporting OR run reporting and target completion

onEnable  -> wire commands -> initialize points -> load scores and archives
          -> check existing runs -> schedule synchronous checks every 20 ticks
onDisable -> save scores.yml (version 3) and archives.yml
```

Most behavior lives in the entry-point class; there is no separate service layer,
custom scoring event API, ServicesManager registration, or standalone API artifact.
State uses nested `LinkedHashMap` collections and is not thread-safe. Keep state
and Paper API calls on the main thread.

There is no plugin `config.yml`: commands/API create buckets and policies,
which are stored in the plugin's data files. Do not edit generated `build/`,
`bin/`, or `.gradle/` outputs.

### Behavior to understand before changing it

- Scores, assignments, active runs, and archives save **on disable**, not every
  mutation. Crashes can lose recent changes; the two YAML writes are not a transaction.
  Loading supports legacy flat UUID scores but is not comprehensive schema validation.
- Score get/add/subtract/reset and archive-list are unguarded; players can modify
  any online player's score. Archive creation, bucket administration, and run
  management require `scorekeeper.admin`; self bucket selection is open.
- Named targets are online-only. `/score-get` reads the target's active bucket,
  lazily creates an entry at its initial value, and shows only the numeric score.
  An explicit bucket argument is pending SK-13; the API already has that overload.
- Amounts are signed Java integers; negatives are allowed and overflow wraps.
  Bucket IDs are case-sensitive.
- `until-disabled` runs are saved/resumed across clean restarts, not ended by
  disable. Reset runs restore the pre-run snapshot. Results are announced/logged,
  not automatically archived; stop a run before archiving its bucket.

The [domain reference](docs/domain-score-tracking.md) explains these rules in
detail. Treat changes to them as deliberate behavior changes, not incidental cleanup.

## Prerequisites and checkout

The project targets **Paper 26.2 and 26.3 only**, compiles against
`io.papermc.paper:paper-api:26.2.build.129-stable`, and uses **Java 25** and the
**Gradle 9.8.0 wrapper**. Bukkit and Spigot servers are not supported.

Install Git and a JDK 25, and set `JAVA_HOME` to the JDK root (not its `bin`
directory). Confirm `java -version` and `javac -version` report 25. The Gradle
toolchain resolver can download the compile JDK, but Gradle itself still needs
a compatible JVM to start. Automatic local JDK detection is disabled in
[gradle.properties](gradle.properties); `JAVA_HOME` is the configured discovery source.

```bash
git clone https://github.com/headhunter45/ScoreKeeper.git
cd ScoreKeeper
./gradlew spotlessCheck build
```

On Windows, use `.\gradlew.bat spotlessCheck build` from PowerShell instead.
No globally installed Gradle or Maven is required.

On macOS/Linux, [tools/build-plugin.sh](tools/build-plugin.sh) is an alternative:
it downloads/caches Temurin 25 under `.gradle/user-home/jdks`, sets `JAVA_HOME`
for that invocation, and runs `build`. It accepts additional Gradle arguments:

```bash
./tools/build-plugin.sh spotlessCheck
```

That JDK selection is local to the script; it does not configure Java for your
terminal, IDE, Paper server, or later direct wrapper invocations.

## Build, format, and validation tasks

Run these from the repository root, using the Windows wrapper where applicable.

| Command | Purpose |
| --- | --- |
| `./gradlew build` | Compile, package, and run configured checks, including Spotless and `test`. |
| `./gradlew assemble` | Package without running the full check lifecycle. |
| `./gradlew spotlessCheck` | Verify Java formatting, imports, and license headers. |
| `./gradlew spotlessApply` | Apply the configured Java formatting; inspect the diff afterward. |
| `./gradlew test` | Run the default test task; currently there are no test sources/dependencies, so this is `NO-SOURCE`. |
| `./gradlew javadoc` | Generate Java API documentation. |
| `./gradlew properties --property version` | Inspect the Git-derived version. |
| `./gradlew publishToMavenLocal` | Publish the Java component to your local Maven repository for companion-plugin development. |

Build configuration lives in [build.gradle](build.gradle) (Groovy DSL), with
`java-library` and `maven-publish`. Paper is a `compileOnly` dependency supplied
by the server; no third-party runtime dependencies are bundled.
Spotless 8.10.3 uses google-java-format 1.36.1, import cleanup/order, and
[the license header](config/license-header.txt). The wrapper version/checksum
are pinned in [wrapper properties](gradle/wrapper/gradle-wrapper.properties);
the Foojay toolchain resolver is configured in [settings.gradle](settings.gradle).

The plugin artifact is `build/libs/ScoreKeeper-<version>.jar`. The version
is derived from `v`-prefixed Git tags (see "Versioning, changelog, and releases"
below). Do not hardcode a release version in the manifest:
[plugin.yml](src/main/resources/plugin.yml) expands `${version}` during resource processing.

There is **no behavioral automated coverage yet** and no `integrationTest` task.
The test infrastructure and coverage are pending SK-08 through SK-12 in
[TODO.md](TODO.md). A successful build is not proof that in-game behavior works.
For code changes, run the smallest relevant existing checks, then exercise the
affected scenarios in [human-tests.md](docs/human-tests.md). Documentation-only
changes need link/metadata checks, not a Paper server.

CI in [gradle.yml](.github/workflows/gradle.yml) runs for pushes and pull requests
to `main` and `develop`, and any `v*` tags. Manual releases are handled via
[release.yml](.github/workflows/release.yml). CI invokes `test`, builds with the
Bash helper, and uploads jar artifacts (test reports only if present). It does
not start Paper or verify the two-version runtime compatibility claim.

## Local Paper server: macOS, Linux, or WSL

The Bash server/deploy/backup/restore/publish scripts read a root `.env`:

```bash
cp .env.example .env
```

Edit it using [the example](.env.example) as the configuration reference:

- `PAPER_VERSION`, `PAPER_BUILD`, and `MINECRAFT_SERVER_JAR` must agree.
  The example pins `26.2`, build `129`, and `paper-26.2-129.jar`. To test 26.3,
  select a build for that version or clear both build and jar values to resolve
  the latest stable build.
- `MINECRAFT_SERVER_PATH` and `MINECRAFT_BACKUP_PATH` accept project-relative
  paths, absolute paths, or `~/` paths. Defaults in the example are `./server`
  and `./backups`.
- Values are literal `KEY=value` data, not shell code. Matching outer quotes
  are removed, but `$HOME`, variable references, and command substitutions are
  **not expanded**. Existing nonempty exported environment variables take precedence.
- Keep `.env` and publishing credentials out of Git.

For server startup, install `curl`, `jq`, `java` (Java 25), and `shasum`.
[start-server.sh](tools/start-server.sh) downloads missing Paper jars via the
Fill v3 API, verifies the supplied SHA-256 checksum, and runs Java in the
foreground with `--nogui`. It does not open a management GUI.

**Process safety:** the Bash start helper calls the stop helper, which matches
all Java processes running `paper-*.jar`, not just the configured server.
Backup/restore helpers also check for any such running server. Do not use these
helpers on a shared host without inspecting the processes and intended paths.
The safest stop is `stop` in your own server console; do not rely on forced
termination to save plugin data.

On an isolated development machine:

```bash
./tools/build-plugin.sh
./tools/deploy-plugin.sh
./tools/start-server.sh
```

Read and accept Minecraft's EULA before setting `eula=true` in the server's
`eula.txt`. The first launch exits until this is done; rerun the start helper
after accepting it. Use the matching Minecraft Java Edition client and connect
to `localhost` (or the server host's address). For WSL, run the client on Windows.

To redeploy, stop the server cleanly, build, copy the jar, and restart. Both
deploy helpers select the newest jar and **only warn** about stale sources;
they do not rebuild or reject stale artifacts. Remove older Score Keeper jars
from the test server's `plugins` directory so only the intended version loads.

### Backup and restore

[backup-server.sh](tools/backup-server.sh) archives plugins, config, world
directories containing `level.dat`, and top-level settings into a timestamped
`.tar.bz2`. Stop the server first; `--force` permits a potentially inconsistent
live backup. Generated Paper remapping caches and world session locks are excluded.

[restore-server.sh](tools/restore-server.sh) defaults to the newest backup,
asks for confirmation, and takes a pre-restore backup unless `--no-backup` is
specified. It **replaces** top-level items present in the archive. Inspect the
archive and resolved paths first, and use only a disposable/test server.

## Windows

The helper scripts in [tools/](tools/) are Bash only. On Windows, use WSL and follow
the Linux instructions above, or build with `.\gradlew.bat build` and set up and run
a Paper 26.2 or 26.3 server manually (see
[Paper downloads](https://papermc.io/downloads/paper)). Prefer typing `stop` in the
server console for a clean shutdown and persistence.

## Publishing

[tools/publish-local.sh](tools/publish-local.sh) bootstraps a local JDK like the
build helper, then runs `publishToMavenLocal`. Coordinates are
`com.majinnaibu.minecraft.plugins:ScoreKeeper:<version>`.

[tools/publish-modrinth.sh](tools/publish-modrinth.sh) uploads a jar version to
Modrinth. Run it **only when intentionally publishing**, after validation:

1. Decide whether this is a tagged stable release or a development build.
   For a stable release, tag the approved commit with `v<version>` and rebuild
   from that tag; inspect Gradle's resulting version and jar metadata.
2. Configure `.env` with `MODRINTH_TOKEN`, project ID, game versions, and loaders.
   Defaults advertise Paper `26.2,26.3` and the `paper` loader; test both before
   publishing that compatibility claim.
3. Build the intended artifact. Optionally set `MODRINTH_FILE` to its exact path;
   otherwise the newest non-`-all.jar` jar is selected.
4. Run the publisher with JDK 25 available to the wrapper. It checks the jar's
   manifest version against Gradle, takes changelog notes from `changelog.md`
   unless `MODRINTH_CHANGELOG` overrides them, and infers `beta` for versions containing `-`, otherwise `release`.
   `MODRINTH_VERSION_TYPE` can override this with `alpha`, `beta`, or `release`;
   `MODRINTH_DEPENDENCIES`, if supplied, must be a JSON array.

The publisher needs `curl`, `jq`, and `unzip`; it creates a version, **not** an
update to the hosted project description. Keep [modrinth-description.md](docs/modrinth-description.md)
and [modrinth.json](docs/modrinth.json) current and update the hosted description
separately when releasing.

## Versioning, changelog, and releases

**Version strings** come from Git tags named `v<major>.<minor>.<patch>[-suffix]` and
are computed by [tools/compute-version.sh](tools/compute-version.sh). Gradle (and so
`plugin.yml`), CI, and the release workflow all call it, so they always agree.
`tools/compute-version.sh --explain` shows how a version was chosen.

- A tagged commit uses its tag. With several tags the greatest release tag (no
  suffix) wins; if all have suffixes, the greatest of those wins.
- An untagged commit looks at every tag reachable from it and takes the greatest.
  If that tag ends in `-dev` the version is that tag; otherwise it is the tag with
  the patch number bumped by one and `-dev` added (`v0.2.2` builds as `0.2.3-dev`).
- With no tags at all the version is `0.0.1-dev`.

**[changelog.md](changelog.md)** holds the release notes for the next release. Its
first-level header names the version (`# Changelog for v0.2.3`). Release builds
rewrite that header to the tagged version in their throwaway checkout, and use
everything under it (HTML comments excluded) as the GitHub and Modrinth release
notes; a release with empty notes is refused. [tools/changelog.sh](tools/changelog.sh)
does the header rewrite and note extraction.

**CI** ([gradle.yml](.github/workflows/gradle.yml)) runs the `Build and Test` job
for pushes and pull requests to `main` and `develop` and for `v*` tags. Make
`Build and Test` a required status check on `main` and `develop`.
Pushing a `v*` tag that is not `-dev` also creates or updates a **draft GitHub
release** with the built jar and the notes from `changelog.md`. Tags ending in `-dev`
never get a release.

**Releasing** ([release.yml](.github/workflows/release.yml), run manually with the
tag): it refuses `-dev` tags, tags whose commit would build as a different version,
and commits without a successful `Build and Test` run, saying why. Otherwise it
fast-forwards `main`, creates `release/<tag>`, rebuilds, publishes to Modrinth, and
publishes the GitHub release (promoting the draft). Tags must be on a commit that
already contains the files above.

**After a release**, commit a reset `changelog.md` with the next expected version
(release version + 0.0.1) and tag that commit `v<next>-dev`.

## Documentation and scraper metadata

Keep [README.md](README.md), [modrinth-description.md](docs/modrinth-description.md),
[project-description.md](docs/project-description.md), and
[project-description.json](docs/project-description.json) consistent with implemented
features. Do not present planned work or manual checks as completed coverage.

The scraper JSON must validate against [projects.schema.json](docs/projects.schema.json).
It is a **slug-keyed object**, currently `{ "score-keeper": { ... } }`, not a
single unwrapped project. Follow [AGENTS.md](AGENTS.md#documentation-and-operational-safety):
the existing key, `id`, `title`, `docId`, and image URLs are fixed in the
consuming project and must remain unchanged during routine metadata updates.
Image paths are relative to that project's root, not this repository.
Preserve the route, repository links, and filenames unless the consumer contract
is intentionally changed. Use "Score Keeper" in descriptive prose.
