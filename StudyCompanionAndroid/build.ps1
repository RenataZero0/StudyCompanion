# Build StudyCompanion.apk  (no Gradle / no Android Studio needed)
#
# NOTE: build-tools 34.0.0 ships a d8 that crashes with an internal NullPointerException
#       on classes nested two levels deep (e.g. MainActivity). 35+ is fine -> use 36.0.0.
# Usage: powershell -ExecutionPolicy Bypass -File build.ps1
#
# NOTE: keep this file ASCII-only. PowerShell 5.1 decodes .ps1 as ANSI when there
# is no BOM, which corrupts non-ASCII text and breaks parsing.
$ErrorActionPreference = "Stop"

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$SDK  = "D:\android-sdk"
$BT   = "$SDK\build-tools\36.0.0"
$AJAR = "$SDK\platforms\android-34\android.jar"
$JDK  = "D:\Program Files\Java\jdk-21"

foreach ($p in @("$BT\aapt2.exe", "$BT\d8.bat", "$BT\zipalign.exe", "$BT\apksigner.bat", $AJAR, "$JDK\bin\javac.exe")) {
    if (-not (Test-Path $p)) { throw "missing toolchain component: $p" }
}
$env:JAVA_HOME = $JDK

# Version comes from VERSION_TAG in GitHub.java so it can never drift from the release.
# (The manifest used to hardcode versionCode=1 / versionName=1.0.)
$ghjava = Get-Content "$here\src\com\studycompanion\GitHub.java" -Raw
if ($ghjava -match 'VERSION_TAG\s*=\s*"v?(\d+)\.(\d+)(?:\.(\d+))?"') {
    $maj = [int]$Matches[1]
    $min = [int]$Matches[2]
    $pat = if ($Matches[3]) { [int]$Matches[3] } else { 0 }
    $verName = "$maj.$min.$pat"
    $verCode = $maj * 10000 + $min * 100 + $pat
} else {
    throw "cannot parse VERSION_TAG from GitHub.java"
}
Write-Host ("version: {0}  (versionCode {1})" -f $verName, $verCode)

# Drop the previous APK up front: if any later step fails we must not leave a
# stale package behind that looks like a successful build.
$apkPath = "$here\StudyCompanion.apk"
if (Test-Path $apkPath) { Remove-Item $apkPath -Force }


# CHANGELOG.md lives at the repo root. Copy it into assets as the offline fallback;
# at runtime the app prefers the copy it fetched from GitHub.
$cl = Join-Path (Split-Path -Parent $here) "CHANGELOG.md"
if (Test-Path $cl) {
    Copy-Item $cl (Join-Path $here "assets\CHANGELOG.md") -Force
    Write-Host "changelog: copied from repo root"
}

$out = "$here\build"
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\dex" | Out-Null

Write-Host "[1/7] aapt2 compile resources ..."
& "$BT\aapt2.exe" compile --dir "$here\res" -o "$out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "[2/7] aapt2 link (manifest + resources + assets) ..."
# NOTE: the compiled resources zip is a POSITIONAL argument (main resource set).
# -R is for overlays; using it here fails with "does not override an existing resource".
& "$BT\aapt2.exe" link `
    -o "$out\base.apk" `
    -I $AJAR `
    --manifest "$here\AndroidManifest.xml" `
    -A "$here\assets" `
    --java "$out\gen" `
    --min-sdk-version 21 --target-sdk-version 34 `
    --version-code $verCode --version-name $verName `
    "$out\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[3/7] javac ..."
$srcs = @(Get-ChildItem "$here\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$gen  = @(Get-ChildItem "$out\gen" -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
& "$JDK\bin\javac.exe" --release 8 -nowarn -cp $AJAR -d "$out\classes" @($srcs + $gen)
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[4/7] d8 (dex) ..."
$classes = @(Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& "$BT\d8.bat" --lib $AJAR --min-api 21 --output "$out\dex" @classes
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "[5/7] add classes.dex into apk ..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
Copy-Item "$out\base.apk" "$out\unsigned.apk" -Force
$zip = [System.IO.Compression.ZipFile]::Open("$out\unsigned.apk", 'Update')
$dex = "$out\dex\classes.dex"
if (-not (Test-Path $dex)) { $zip.Dispose(); throw "classes.dex not produced" }
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $dex, "classes.dex") | Out-Null
$zip.Dispose()

Write-Host "[6/7] zipalign ..."
& "$BT\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[7/7] sign ..."
$ks = "$here\debug.keystore"
if (-not (Test-Path $ks)) {
    & "$JDK\bin\keytool.exe" -genkeypair -v `
        -keystore $ks -storepass android -keypass android `
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=StudyCompanion, OU=Dev, O=StudyCompanion, L=Shanghai, C=CN"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}
$apk = $apkPath
& "$BT\apksigner.bat" sign `
    --ks $ks --ks-pass pass:android --key-pass pass:android `
    --v1-signing-enabled true --v2-signing-enabled true `
    --out $apk "$out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host ""
Write-Host "OK -> $apk" -ForegroundColor Green
# Guard: verify the produced APK really carries the version we intended.
# (Once javac failed silently behind a filtered log and the APK stayed stale.)
$badged = & "$BT\aapt2.exe" dump badging $apk 2>&1 | Select-Object -First 1
if ($badged -notmatch [regex]::Escape("versionName='$verName'")) {
    Write-Host "!! APK version mismatch (expected $verName)" -ForegroundColor Red
    Write-Host "   $badged" -ForegroundColor Red
    throw "APK version check failed"
}
Write-Host ("size: {0:N0} KB" -f ((Get-Item $apk).Length / 1KB))
