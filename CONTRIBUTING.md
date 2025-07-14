# Contributing to ScoreKeeper

Thank you for your interest in contributing to the ScoreKeeper plugin! This guide will help you set up your development environment and follow best practices for working with the project.

## Development Environment Setup

To develop and test the ScoreKeeper plugin, you will need:
- A Paper Minecraft server (for plugin deployment and testing)
- The official Minecraft Java Edition client (for connecting to your test server)

### 1. Setting up the Paper Server (macOS/Linux/WSL)
1. Download the latest Paper server jar from [https://papermc.io/downloads](https://papermc.io/downloads).
2. Create a directory for your test server and place the Paper jar inside.
3. Set the following environment variables in your shell profile (e.g., `.bashrc`, `.zshrc`, or manually in your terminal):
   ```bash
   export MINECRAFT_SERVER_PATH=/path/to/your/server
   export MINECRAFT_SERVER_JAR=paper-<version>.jar  # Can be relative to MINECRAFT_SERVER_PATH or absolute
   ```
4. Use the provided scripts to manage your server:
   - To start (or restart) the server: `tools/bash/start-server.sh`
   - To stop the server: `tools/bash/stop-server.sh`
   These scripts use the environment variables above to locate and manage your server.
5. On first run, accept the EULA by editing `eula.txt` and setting `eula=true` in your server directory.
6. Place your built plugin jar in the `plugins/` directory.
7. Use the start script again to restart the server and load the plugin.

### 2. Setting up the Minecraft Client
- **macOS/Linux:**
  - Download and install the official Minecraft Launcher from [minecraft.net](https://www.minecraft.net/en-us/download).
  - Log in with your Mojang/Microsoft account and launch the Java Edition client.
- **WSL (Windows Subsystem for Linux):**
  - For best performance, run the Minecraft client directly under Windows, not inside WSL.
  - Download and install the official Minecraft Launcher for Windows from [minecraft.net](https://www.minecraft.net/en-us/download).
  - Use WSL for server and plugin development, and connect to your test server from the Windows client.

### 3. Connecting the Client to Your Server
- Start your Paper server (see above).
- In the Minecraft client, add a new server with the address:
  - `localhost` (if running both client and server on the same machine)
  - Or use your machine's IP address if connecting across devices.

### 4. Notes
- You can automate server startup and plugin deployment with bash scripts for faster testing.
- For automated testing or CI, run the Paper server in a headless Linux environment (including WSL, Docker, or CI runners).
- Always use the provided scripts and environment variables for consistent server management. 