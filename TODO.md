# Migration TODOs for Paper Modernization

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
