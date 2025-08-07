#!/usr/bin/env pwsh

# Set strict mode for better error handling
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Check required environment variable
if (-not $env:MINECRAFT_SERVER_PATH) {
    Write-Error "Error: MINECRAFT_SERVER_PATH environment variable is not set."
    exit 1
}

# Find project root (two directories above this script)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)

# Find the most recent plugin jar file
$LibsPath = Join-Path $ProjectRoot "build\libs"
$PluginJar = Get-ChildItem -Path "$LibsPath\*.jar" -ErrorAction SilentlyContinue | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 1

if (-not $PluginJar) {
    Write-Error "Error: No plugin jar found in $LibsPath. Build the plugin first."
    exit 1
}

# Optional: Warn if jar is older than any source file
$SourcePaths = @(
    (Join-Path $ProjectRoot "src\main\java"),
    (Join-Path $ProjectRoot "src\main\resources")
)

$NewerSourceFiles = Get-ChildItem -Path $SourcePaths -Recurse -File -ErrorAction SilentlyContinue | 
    Where-Object { $_.LastWriteTime -gt $PluginJar.LastWriteTime }

if ($NewerSourceFiles) {
    Write-Host "Warning: The built plugin jar is older than some source files. Consider rebuilding." -ForegroundColor Yellow
}

# Create plugins directory if it doesn't exist
$PluginsPath = Join-Path $env:MINECRAFT_SERVER_PATH "plugins"
if (-not (Test-Path $PluginsPath)) {
    New-Item -ItemType Directory -Path $PluginsPath -Force | Out-Null
}

# Copy the plugin jar to the plugins directory
Copy-Item -Path $PluginJar.FullName -Destination $PluginsPath -Force
Write-Host "Deployed $($PluginJar.Name) to $PluginsPath\"
