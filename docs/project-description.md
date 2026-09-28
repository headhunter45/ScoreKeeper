# ScoreKeeper

ScoreKeeper is a reusable scoring engine for Paper 26.2 and 26.3. It gives events, competitions, and companion plugins a shared language for points: named buckets can have custom singular/plural labels, initial values, reporting policies, archives, and high-score runs.

The plugin is both a player-facing command system and an extension point. Administrators can start target-based, timed, or manually stopped score runs, while other plugins can create buckets, award points, archive results, and build richer game loops through the public API. Scores, assignments, runs, and archives persist across server restarts. Bukkit and Spigot servers are not supported.

## Engineering Notes

- Built with Gradle and Java 25 for modern Paper.
- Uses UUID-based player identity and YAML-backed persistence.
- Includes server-free JUnit coverage for domain rules, commands, reporting, archives, and lifecycle behavior.
- Designed as the scoring foundation used by companion plugins such as MobScores.
