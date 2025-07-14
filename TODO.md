# Migration TODOs for Paper Modernization

- [ ] Switch all player score storage to use UUID instead of Player as the key
- [ ] Update logger usage to use getLogger() from JavaPlugin
- [ ] Remove or modernize any old/deprecated event registration (use @EventHandler and registerEvents)
- [ ] Review and update player lookup logic to use getPlayerExact or handle case sensitivity
- [ ] Ensure all commands are properly defined in plugin.yml
- [ ] Initialize Gradle build system
- [ ] Set project metadata in build.gradle
- [ ] Add repositories and dependencies
- [ ] Configure Java version
- [ ] Ensure resource handling for plugin.yml
- [ ] Add Gradle plugins as needed (e.g., Shadow)
- [ ] Update .gitignore for Gradle
- [ ] Remove Maven files
- [ ] Update documentation and scripts
- [ ] Test the Gradle build
