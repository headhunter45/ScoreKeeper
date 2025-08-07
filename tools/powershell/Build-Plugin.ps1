#!/usr/bin/env pwsh

# Set strict mode for better error handling
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Find project root (two directories above this script)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)

# Save current location and change to project root
Push-Location $ProjectRoot

try {
    # Build the project using Gradle
    & ".\gradlew.bat" build
} finally {
    # Always restore the original location
    Pop-Location
}