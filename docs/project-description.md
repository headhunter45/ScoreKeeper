# Score Keeper

Score Keeper is a reusable scoring engine for Paper 26.2 and 26.3. It gives events,
competitions, and companion plugins a shared language for points: named buckets
can have custom singular/plural labels, initial values, reporting policies,
archives, and high-score runs.

The plugin is both a player-facing command system and an extension point.
Administrators can start target-based, timed, or manually stopped score runs,
while other plugins can create buckets, award points, archive results, and build
richer game loops through the public API. Gameplay does not automatically award
scores. Scores, assignments, active runs, and archives persist across clean server
restarts through shutdown saves. Bukkit and Spigot servers are not supported.

Run standings include deterministic tie-breaking, top-N announcements, personal
places, and optional restoration of pre-run scores. Separate archives snapshot
player, bucket, or all-bucket scores and reset the live values.

## Engineering Notes

- Built with Gradle and Java 25 for modern Paper.
- Uses UUID-based player identity and YAML-backed persistence.
- Exposes a main-thread Java API for companion plugins; no service registration or built-in gameplay listeners.
- Has no automated Java test coverage yet; manual pre-release scenarios are documented, with test infrastructure planned.
- Uses the display name "Score Keeper" while retaining `ScoreKeeper` as the repository and plugin identifier.
- Licensed under GNU AGPL v3 or later.

Score get/add/subtract/reset commands currently have no permission checks and can
target online players; archive, bucket administration, and run management use
`scorekeeper.admin`. Saves happen on disable, not every mutation, so crashes may
lose recent state.
