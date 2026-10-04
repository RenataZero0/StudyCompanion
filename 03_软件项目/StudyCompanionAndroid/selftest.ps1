# Rebuild the APK and then run the same parsing code on the desktop JVM.
# Usage: powershell -ExecutionPolicy Bypass -File selftest.ps1
# NOTE: keep this file ASCII-only (PowerShell 5.1 reads .ps1 as ANSI without a BOM).
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$JDK  = "D:\Program Files\Java\jdk-21"
$AJAR = "D:\android-sdk\platforms\android-34\android.jar"

& powershell -ExecutionPolicy Bypass -File "$here\build.ps1"

Write-Host ""
Write-Host "=== running the same parsing code on the desktop JVM ==="
$cp = "$AJAR;$here\build\classes;$here\build\gen"
& "$JDK\bin\javac.exe" -nowarn -encoding UTF-8 -cp $cp -d "$here\build\classes" "$here\tools\SelfTest.java"
if ($LASTEXITCODE -ne 0) { throw "SelfTest compile failed" }

Push-Location $here
& "$JDK\bin\java.exe" "-Dfile.encoding=UTF-8" -cp $cp SelfTest "$here\assets\Schedule.xlsx"
Pop-Location
Write-Host "(full output also written to selftest.txt)"
