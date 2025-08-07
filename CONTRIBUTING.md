# Contributing to ScoreKeeper
**TODO:** Add info about the various gradle tasks.

Thank you for your interest in contributing to the ScoreKeeper plugin! This guide will help you set up your development environment and follow best practices for working with the project.

# Getting Started
Pick a folder to hold your minecraft server and this project I'm going to use `$HOME/Projects`. My Minecraft server will be in `$HOME/Projects/MCServer` and this project will be in `$HOME/Projects/ScoreKeeper`.

Setup our project folder.

**Linux, macOS, and WSL**
```bash
PROJECTS_DIR="$HOME/Projects"
MINECRAFT_SERVER_PATH="$PROJECTS_DIR/MCServer"
# Set this to the latest download url on the Paper downloads page [https://papermc.io/downloads](https://papermc.io/downloads) and update the jar name.
MINECRAFT_SERVER_DOWNLOAD_URI="https://fill-data.papermc.io/v1/objects/fb73c7e310215016955617ab957022d9e1d47aeba206df3a98c5ecb43756527c/paper-1.21.8-25.jar"
MINECRAFT_SERVER_JAR="paper-1.21.8-25.jar"
mkdir -p "$MINECRAFT_SERVER_PATH"
# Keep this shell open we will use these variables later.
```

**Windows**
```powershell
$projectsDir="$HOME\Projects"
$minecraftServerDir="$projectsDir\MCServer"
# Set this to the latest download url on the Paper downloads page [https://papermc.io/downloads](https://papermc.io/downloads) and update the jar name.
$minecraftServerDownloadUri="https://fill-data.papermc.io/v1/objects/fb73c7e310215016955617ab957022d9e1d47aeba206df3a98c5ecb43756527c/paper-1.21.8-25.jar"
$minecraftServerJar="paper-1.21.8-25.jar"
New-Item -Type Directory $projectsDir
New-Item -Type Directory $minecraftServerDir
# Keep this shell open we will use these variables later.
```

## Install the JDK
**Linux / WSL**
```bash
apt install openjdk-21-jdk
```

**macOS**
```zsh
brew install --cask microsoft-openjdk@21
```

**Windows**
From Powershell
```powershell
winget install --id Microsoft.OpenJDK.21 --source winget
```

## Setup a PaperMC Server
**Linux, macOS, and WSL**
```bash
# You're probably still here, but if not go to the server dir.
cd "$MINECRAFT_SERVER_PATH"
curl -O "$MINECRAFT_SERVER_DOWNLOAD_URI"
java -jar "$MINECRAFT_SERVER_JAR"
nano eula.txt # Change eula=false to eula=true, then save and exit.
java -jar "$MINECRAFT_SERVER_JAR"
```

**Windows**
```powershell
# You're probably still here, but if not go to the server dir.
Set-Location $minecraftServerDir
Invoke-WebRequest -Uri $minecraftServerDownloadUri -OutFile (Split-Path $minecraftServerDownloadUri -Leaf)
java -jar $minecraftServerJar
notepad eula.txt
# Change eula=false to eula=true, then save and close the file.
java -jar $minecraftServerJar
```

If you get windows security popups about java with the little java dude allow them. It's because the server is listening on the network. Now you should have the server running and be dropped at a prompt like `> `. Type `stop` and hit enter to kill the server. You can also try CTRL + C.

## Get the Code
**Linux, macOS, and WSL**
```bash
cd "$PROJECTS_DIR"
git clone https://github.com/headhunter45/ScoreKeeper.git
code ScoreKeeper
```

```powershell
Set-Location $projectsDir
git clone https://github.com/headhunter45/ScoreKeeper.git
# Then to open VSCode
code ScoreKeeper
```

## Building the Plugin
This should download the preferred version of our build tool (gradle) and use it to build the project. Among other intermediate files it will create `build/libs/ScoreKeeper-${Version}.jar`. This is the plugin jar that you copy to the minecraft server to install the plugin. From now on you will use the utility scripts in `tools\powershell\*.ps1` or `tools/bash/*.sh` to build and deploy the plugin to our test server.

**Linux, macOS, and WSL**
```bash
./gradlew build
```

**Windows**
```powershell
.\gradlew.bat build
```

## Using the Utility Scripts
Our scripts depend on two environment variables. You can set them each time you open a shell or set them once for every shell your user opens. The variables are `MINECRAFT_SERVER_PATH` and `MINECRAFT_SERVER_JAR`. They are used by the scripts to know where the server is so they can control it and deploy the plugin.

**Linux, macOS, and WSL**

To set them for the current session run these commands. To set for every new session add these commands to your `.bashrc` or `.zshrc` file.
```bash
MINECRAFT_SERVER_PATH="$HOME/Projects/MCServer"
MINECRAFT_SERVER_JAR="paper-1.21.8-25.jar"
```

**Windows**

To set them for a single session run these commands.
```powershell
$env:MINECRAFT_SERVER_PATH = "$HOME\Projects\MCServer"
$env:MINECRAFT_SERVER_JAR = "paper-1.21.8-25.jar"
```

If you want them available in all of your powershell sessions you can set them permanently like this.
```powershell
[Environment]::SetEnvironmentVariable("MINECRAFT_SERVER_PATH", "$HOME\Projects\MCServer", "User")
[Environment]::SetEnvironmentVariable("MINECRAFT_SERVER_JAR", "paper-1.21.8-25.jar", "User")
```

### Start-Server.ps1 and start-server.sh
Run `tools\powershell\Start-Server.ps1` or `tools/bash/start-server.sh` to start the server and open a GUI to manage it.

### Stop-Server.ps1 and stop-server.sh
Run `tools\powershell\Stop-Server.ps1` or `tools/bash/stop-server.sh` to stop the server. You can also safely close the GUI.

### Build-Plugin.ps1 and build-plugin.sh
Run `tools\powershell\Build-Plugin.ps1` or `tools/bash/build-plugin.sh` to build the plugin. This builds the plugin in `build/libs` as a jar named `ScoreKeeper-${VERSION}.jar`. To install the plugin manually you can copy this to `$env:MINECRAFT_SERVER_PATH\plugins` or `$MINECRAFT_SERVER_PATH/plugins` and restart the server.

### Deploy-Plugin.ps1 and deploy-plugin.sh
Run `tools\powershell\Deploy-Plugin.ps1` or `tools/bash/deploy-plugin.sh` to copy the plugin to the server plugins directory. This checks if the built plugin is up to date with the source code. If it is, then the script copies the plugin to the plugins directory to install it. To use the new plugin restart the server.

# Setting up the Minecraft Client
- **macOS / Linux:**
  - Download and install the official Minecraft Launcher from [minecraft.net](https://www.minecraft.net/en-us/download).
  - Log in with your Mojang/Microsoft account and launch the Java Edition client.
- **Windows / WSL (Windows Subsystem for Linux):**
  - For best performance, run the Minecraft client directly under Windows, not inside WSL.
  - Download and install the official Minecraft Launcher for Windows from [minecraft.net](https://www.minecraft.net/en-us/download).
  - Use WSL for server and plugin development, and connect to your test server from the Windows client.

# Connecting the Client to Your Server
- Start your Paper server (see above).
- In the Minecraft client, add a new server with the address:
  - `localhost` (if running both client and server on the same machine)
  - Or use your machine's IP address if connecting across devices.

# Notes
- Always use the provided scripts and environment variables for consistent server management.
