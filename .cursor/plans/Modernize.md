# Modernize ScoreKeeper Minecraft Plugin

This checklist will guide the process of updating the ScoreKeeper plugin for compatibility with modern Minecraft Java servers (e.g., PaperMC, Spigot, or Bukkit 1.19+).

## Modernization Plan

- [ ] **Analyze current codebase**
  - Review all source files and dependencies
  - Identify deprecated or removed APIs
- [ ] **Update build system**
  - Update `pom.xml` to use a modern Bukkit/Spigot/PaperMC API version (e.g., 1.19+)
  - Remove or update old repositories
  - Ensure Java version compatibility (Java 17+ for latest servers)
- [ ] **Update plugin.yml**
  - Ensure all required fields are present and up to date
  - Update API version and commands
- [ ] **Refactor deprecated API usage**
  - Replace removed or changed Bukkit/Spigot API calls
  - Update event listeners and command registration
- [ ] **Test on a modern server**
  - Build the plugin
  - Run on a local PaperMC/Spigot server (latest LTS)
  - Use the provided scripts (`tools/bash/start-server.sh` and `tools/bash/stop-server.sh`) to manage the server, and set the `MINECRAFT_SERVER_PATH` and `MINECRAFT_SERVER_JAR` environment variables as described in CONTRIBUTING.md.
  - Check for errors and warnings
- [ ] **Fix bugs and incompatibilities**
  - Address any issues found during testing
- [ ] **Add/update documentation**
  - Update README with new usage instructions
  - Document any new features or changes
- [ ] **Optional: Add new features or improvements**
  - Consider adding quality-of-life improvements or new features

---

Check off each step as you complete it to track your progress! 

---

## Appendix: Gradle vs. Maven

### Maven
**Pros:**
- Stability & Maturity: Very stable, predictable builds.
- Convention over Configuration: Standard structure and lifecycle.
- Dependency Management: Handles dependencies well, large central repository.
- Documentation: Extensive documentation and community support.
- IDE Support: Excellent integration with Java IDEs.
- Widely Used: Many Minecraft plugins and tutorials use Maven.

**Cons:**
- Verbose Configuration: `pom.xml` can become large and hard to read.
- Less Flexible: Custom build logic is harder to implement.
- Slower for Large Projects: Can be slower than Gradle for complex builds.

### Gradle
**Pros:**
- Performance: Generally faster builds, supports incremental builds and caching.
- Flexibility: Build scripts in Groovy/Kotlin allow complex logic.
- Concise Configuration: `build.gradle` files are usually shorter and easier to read.
- Modern Tooling: Better support for modern build features.
- Growing Popularity: Increasingly popular in the Java ecosystem.

**Cons:**
- Learning Curve: More complex for beginners, especially for custom logic.
- Less Convention: More freedom can lead to less consistency.
- Slightly Less Documentation: Not as much Minecraft-specific documentation as Maven.

### Which is Most Common for Minecraft Plugins?
- **Maven** is still the most common for Bukkit, Spigot, and Paper plugins, with most guides and examples using it.
- **Gradle** is gaining popularity, especially for newer projects or those needing more flexibility and speed.

**Summary:**
- For maximum compatibility with community resources, Maven is the safest choice.
- For a modern, flexible, and fast build system, Gradle is a great option if you're comfortable with it. 

---

## Appendix: Choosing a Plugin API

### Bukkit
**Description:**
The original plugin API for Minecraft servers, now largely unmaintained. Most modern APIs are built on or forked from Bukkit.

**Pros:**
- Huge legacy plugin library.
- Simple, well-documented API.
- Good for very basic plugins.

**Cons:**
- No longer actively maintained.
- Lacks support for modern Minecraft features.
- Not recommended for new projects.

### Spigot
**Description:**
A high-performance fork of Bukkit, Spigot is the most widely used server software for plugins. It adds performance improvements and bug fixes.

**Pros:**
- Actively maintained and widely used.
- Large plugin ecosystem.
- Good documentation and community support.
- Compatible with most Bukkit plugins.

**Cons:**
- Lags behind the latest Minecraft features compared to Paper.
- Fewer advanced features than Paper.

### Paper
**Description:**
A fork of Spigot with additional performance optimizations, bug fixes, and new API features. Paper is now the de facto standard for modern plugin development.

**Pros:**
- Actively maintained and very popular.
- Superior performance and stability.
- Adds many new API features not in Spigot/Bukkit.
- Backwards compatible with most Spigot/Bukkit plugins.
- Large, active community.

**Cons:**
- Some Paper-specific APIs may not work on Spigot (if you ever want to support both).
- Slightly faster update cycle may require more frequent plugin updates.

### Purpur
**Description:**
A fork of Paper with even more features, configuration options, and experimental changes. Aimed at server owners who want maximum customization.

**Pros:**
- All benefits of Paper, plus more features and config options.
- Great for highly customized servers.

**Cons:**
- Some features are experimental and may be unstable.
- Smaller community than Paper/Spigot.
- Plugins using Purpur-specific features may not work elsewhere.

### Sponge
**Description:**
A completely separate API and server implementation, not based on Bukkit/Spigot/Paper. Aimed at modded servers (Forge) but also works standalone.

**Pros:**
- Designed for both plugins and mods (Forge integration).
- Modern, flexible API.
- Good for modded servers.

**Cons:**
- Much smaller plugin ecosystem for vanilla servers.
- Not compatible with Bukkit/Spigot/Paper plugins.
- Less relevant for standard server-only plugins.

### Summary & Recommendation
- **Paper** is the best choice for most modern server-only plugins: it’s fast, stable, actively maintained, and has the richest API.
- **Spigot** is a safe fallback if you want maximum compatibility, but Paper is almost always preferred now.
- **Purpur** is great for highly customized servers, but not necessary unless you want its extra features.
- **Sponge** is only recommended if you want to support modded servers or need its unique API.

**For your use case (server-only, modern, not needing experimental features):**
**Paper** is the best option. It gives you the most features, best performance, and widest compatibility for new plugin development. 

---

## Appendix: Development Setup

### Minecraft Launchers (Java Edition)

**Official Minecraft Launcher**
- Fully supported by Mojang/Microsoft.
- Works on Windows, macOS, and Linux (including WSL with GUI support).
- Easiest for vanilla play and account management.

**Prism Launcher** (formerly PolyMC)
- Open source, cross-platform (Windows, macOS, Linux).
- Great for managing multiple Minecraft instances, modpacks, and versions.
- Works well in Linux environments.

**MultiMC**
- Similar to Prism Launcher, but older and less actively maintained.
- Good for managing multiple instances.

**Recommendation:**
- Use the Official Minecraft Launcher for playing and account management.
- Use Prism Launcher for advanced instance/modpack management, especially on Linux/macOS.

### Server Software for Plugin Development

**Paper**
- Most popular for plugin development.
- Fast, stable, and actively maintained.
- Easy to run in headless/automated environments (Linux containers, CI).
- [Download Paper](https://papermc.io/downloads)

**Spigot**
- Still widely used, but less feature-rich than Paper.
- Requires building with BuildTools ([spigotmc.org/wiki/buildtools](https://www.spigotmc.org/wiki/buildtools/))

**Purpur**
- Fork of Paper with more options.
- [Download Purpur](https://purpurmc.org/downloads)

**Recommendation:**
- Use Paper for your main development and testing server.
- Download the latest Paper jar and run it directly in your Linux environment or containers.

### Automated Testing & Linux Containers
- Paper runs perfectly in headless Linux environments (including WSL, Docker, CI/CD).
- You can script server startup, plugin deployment, and automated plugin tests using bash scripts.
- For CI/CD, use GitHub Actions, GitLab CI, or any Linux-based runner.

### Summary Table

| Use Case                | Recommended Launcher         | Recommended Server |
|-------------------------|-----------------------------|-------------------|
| Playing Minecraft       | Official Launcher / Prism   | N/A               |
| Plugin Development      | N/A                         | Paper             |
| Automated Testing/CI    | N/A                         | Paper             |
| Multi-instance/modpacks | Prism Launcher              | Paper             |

### Next Steps
- Download the Official Minecraft Launcher for playing.
- Download Prism Launcher if you want advanced management.
- Download the latest Paper server jar for plugin development and testing. 

#### Server Management Scripts and Environment Variables
- Use the `tools/bash/start-server.sh` and `tools/bash/stop-server.sh` scripts to start and stop your Paper server for development and testing.
- Set the following environment variables in your shell:
  - `MINECRAFT_SERVER_PATH`: The root path of your Paper server directory.
  - `MINECRAFT_SERVER_JAR`: The path to your Paper server jar (relative to `MINECRAFT_SERVER_PATH` or absolute).
- See `CONTRIBUTING.md` for detailed setup instructions. 