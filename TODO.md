## Migration TODOs for Paper Modernization

- [x] Switch all player score storage to use UUID instead of Player as the key
- [x] Update logger usage to use getLogger() from JavaPlugin
- [x] Remove or modernize any old/deprecated event registration (use @EventHandler and registerEvents)
- [x] Review and update player lookup logic to use getPlayerExact or handle case sensitivity
- [x] Ensure all commands are properly defined in plugin.yml
- [x] Initialize Gradle build system
- [x] Set project metadata in build.gradle
- [x] Add repositories and dependencies
- [x] Configure Java version
- [x] Ensure resource handling for plugin.yml
- [x] Add Gradle plugins as needed (e.g., Shadow)
- [x] Update .gitignore for Gradle
- [x] Remove Maven files
- [x] Update documentation and scripts
- [x] Test the Gradle build

## Tasks

|  ID   |  Status  | Title |
|:-----:|:--------:|:------|
| SK-01 |  Done    | Save scores between server launches. |
| SK-02 |  Done    | Allow tracking scores by bucket. An admin or other plugin should be able to create types of scores with named other than points. { id: "Bread", singular: "loaf", plural: "loaves", initialValue: 0 } Each player's score will be tracked in a bucket by id. Players may be all switched to a bucket or individually switched. |\
| SK-03 |  Done    | Add optional score reporting to buckets with these four values. { id: "Bread", singular: "loaf", plural: "loaves", initialValue: 0, reporting:"admin|global|player|none" } admin means ops/admin only, global means everyone on the server, player means only tell the player, and none means don't report changes.
| SK-04 |  Done    | Tell a player when their bucket changes. Something like "You are now tracking ${id}. You have ${score} ${plural or singular depending in score}." |
| SK-05 |  Done    | Enable high score runs for a bucket. Admins or pugins should be able to start a run on a certain kind of score. The top n (configurable) players will have their scores reported globally for this bucket and the individual player will have their score reported to them when it changes. Scores will be optionally reset to their initialValue with the old score for this bucket saved. High score runs will have a lifetime of a target high score, a time like 4 hours or 1 week, or until-disabled. When the high score run is finished all players will be messaged a table with the top n places and their scores along with the individual user's place and score. The scores will be reset to their previous values if they were reset to their initialValue at the beginning otherwise they will be left alone. |
| SK-06 |  Done    | Score archiving. Scores either all or within a given bucket should be able to be archived and reset to their initial value. The archived scores should be able to be listed via a command. |
| SK-07 |  Ready   | Replace 'net.researchgate.release' with org.danilopianini.git-sensitive-semantic-versioning' for our versioning in our build.gradle file. Turn org.gradle.configuration-cache back on after this. Our current version is 0.2.1-SNAPSHOT. Use that as our default or 0.2.1 and tag the commit to update to 0.2.2. |
