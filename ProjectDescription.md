# ScoreKeeper

ScoreKeeper is a Paper Minecraft plugin for tracking player scores during server activities, events, or games. It keeps scores per player in memory and exposes commands for viewing, changing, resetting, and archiving scores.

## Details

- Built for Paper API 1.21.7 with Java 21 and Gradle.
- Registers `/score-get`, `/score-add`, `/score-subtract`, `/score-reset`, and `/score-archive`.
- Commands use the executing player when a player name is omitted.
- Scores are currently held in memory and are not persisted when the server stops.
- Score archiving is planned, but the current implementation reports that archiving is unavailable.
- The project is packaged as a Java plugin with a `plugin.yml` descriptor.

## Image

No project image asset is included in the repository. The JSON description includes a prompt-backed image URL for a future project thumbnail.
