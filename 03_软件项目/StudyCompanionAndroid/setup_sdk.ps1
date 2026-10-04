# 下载并安装 Android SDK 命令行工具 + platform-34 + build-tools;34.0.0
# 只用于编译 StudyCompanion 的 APK，装完可整个目录删掉
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$SDK   = "D:\android-sdk"
$JDK   = "D:\Program Files\Java\jdk-21"
$DL    = "$SDK\_download"
New-Item -ItemType Directory -Force $DL | Out-Null

$url = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$zip = "$DL\cmdline-tools.zip"

if (-not (Test-Path $zip) -or (Get-Item $zip).Length -lt 50MB) {
    Write-Host "[1/4] 下载 commandline-tools ..."
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
}
Write-Host ("      下载完成 {0:N1} MB" -f ((Get-Item $zip).Length / 1MB))

Write-Host "[2/4] 解压 ..."
if (Test-Path "$SDK\cmdline-tools\latest") { Remove-Item -Recurse -Force "$SDK\cmdline-tools\latest" }
New-Item -ItemType Directory -Force "$SDK\cmdline-tools" | Out-Null
Expand-Archive -Path $zip -DestinationPath "$SDK\cmdline-tools\tmp" -Force
Move-Item "$SDK\cmdline-tools\tmp\cmdline-tools" "$SDK\cmdline-tools\latest" -Force
Remove-Item -Recurse -Force "$SDK\cmdline-tools\tmp"

$sdkmanager = "$SDK\cmdline-tools\latest\bin\sdkmanager.bat"

Write-Host "[3/4] 接受许可 ..."
$yesses = ("y`r`n" * 60)
$yesses | & $sdkmanager --sdk_root=$SDK --licenses 2>&1 | Select-Object -Last 3

Write-Host "[4/4] 安装 platform-34 与 build-tools;34.0.0 ..."
& $sdkmanager --sdk_root=$SDK "platforms;android-34" "build-tools;34.0.0" 2>&1 | Select-Object -Last 6

Write-Host ""
Write-Host "=== 结果 ==="
foreach ($p in @("$SDK\platforms\android-34\android.jar", "$SDK\build-tools\34.0.0\aapt2.exe",
                 "$SDK\build-tools\34.0.0\d8.bat", "$SDK\build-tools\34.0.0\zipalign.exe",
                 "$SDK\build-tools\34.0.0\apksigner.bat")) {
    Write-Host ("  {0}  {1}" -f (Test-Path $p), $p)
}
