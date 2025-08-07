#!/usr/bin/env pwsh

# Set strict mode for better error handling
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Check required environment variables
if (-not $env:MINECRAFT_SERVER_PATH -or -not $env:MINECRAFT_SERVER_JAR) {
    Write-Error "Error: MINECRAFT_SERVER_PATH and MINECRAFT_SERVER_JAR must be set."
    exit 1
}

Push-Location $env:MINECRAFT_SERVER_PATH

# Find running server process (java with the server jar)
$ServerProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { 
    $_.CommandLine -like "*$($env:MINECRAFT_SERVER_JAR)*" 
}

if ($ServerProcess) {
    Write-Host "Minecraft server is running (PID: $($ServerProcess.Id)). Stopping it..."
    Stop-Process -Id $ServerProcess.Id -Force
    Start-Sleep -Seconds 5
} else {
    Write-Host "Minecraft server is not running."
}

# Start the server in a new window
try {
    $ServerJob = Start-Process -FilePath "java" -ArgumentList "-jar", $env:MINECRAFT_SERVER_JAR -PassThru
    Write-Host "Minecraft server started in new window with PID: $($ServerJob.Id)"
} finally {
    Pop-Location
}
