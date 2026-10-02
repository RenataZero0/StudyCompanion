# Build StudyCompanion.exe
# Usage: powershell -ExecutionPolicy Bypass -File build.ps1
#
# NOTE: keep this file ASCII-only. PowerShell 5.1 decodes .ps1 files as ANSI
# when there is no BOM, which corrupts non-ASCII characters and breaks parsing.
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$csc  = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $csc)) { $csc = "C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe" }
if (-not (Test-Path $csc)) { throw "csc.exe not found (need .NET Framework 4.x)" }

$src = Get-ChildItem "$here\src\*.cs" | Where-Object { $_.Name -ne "_test.cs" } | ForEach-Object { $_.FullName }
$out = "$here\StudyCompanion.exe"
$ico = "$here\assets\app.ico"

# /win32icon  -> Explorer shows this icon for the exe
# /resource   -> the app can pick the right size at runtime (16 for tray, 32 for title bar)
$iconArgs = @()
if (Test-Path $ico) {
    $iconArgs = @("/win32icon:$ico", "/resource:$ico,StudyCompanion.app.ico")
} else {
    Write-Host "WARN: assets\app.ico not found - the exe will have no icon (run tools\make_icon.py)" -ForegroundColor Yellow
}

& $csc /nologo /codepage:65001 /target:winexe /optimize+ /out:$out `
    /r:System.dll /r:System.Drawing.dll /r:System.Windows.Forms.dll `
    /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll `
    /r:System.Xml.Linq.dll /r:System.Core.dll /r:System.Web.Extensions.dll /r:System.Security.dll @iconArgs $src

if ($LASTEXITCODE -eq 0) { Write-Host "OK -> $out" -ForegroundColor Green }
else { Write-Host "BUILD FAILED (exit $LASTEXITCODE)" -ForegroundColor Red }
