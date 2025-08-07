#!/usr/bin/env pwsh

# Set strict mode for better error handling
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Check required environment variables
if (-not $env:MINECRAFT_SERVER_PATH -or -not $env:MINECRAFT_SERVER_JAR) {
    Write-Error "Error: MINECRAFT_SERVER_PATH and MINECRAFT_SERVER_JAR must be set."
    exit 1
}

# Find running server process (java with the server jar)
$ServerProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { 
    $_.CommandLine -like "*$($env:MINECRAFT_SERVER_JAR)*" 
}

if ($ServerProcess) {
    Write-Host "Minecraft server is running (PID: $($ServerProcess.Id)). Stopping it..."
    Stop-Process -Id $ServerProcess.Id -Force
    Start-Sleep -Seconds 5
    Write-Host "Minecraft server stopped."
} else {
    Write-Host "Minecraft server is not running."
}
