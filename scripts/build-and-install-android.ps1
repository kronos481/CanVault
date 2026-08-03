[CmdletBinding()]
param(
  [Parameter(Mandatory = $false)]
  [switch] $AcceptAndroidLicenses,

  [Parameter(Mandatory = $false)]
  [string] $Serial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $AcceptAndroidLicenses) {
  throw 'Noch einmal mit -AcceptAndroidLicenses starten. Damit stimmst du den Android-SDK-Lizenzen zu.'
}

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$nativeRoot = Join-Path $projectRoot 'native-android'
$gradleWrapper = Join-Path $nativeRoot 'gradlew.bat'

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
  throw "Das native CANVAULT-Android-Projekt fehlt: $nativeRoot"
}

if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
  throw 'Java fehlt. Installiere JDK 17 oder neuer und starte den Befehl erneut.'
}

$localAppData = [Environment]::GetFolderPath('LocalApplicationData')
$sdkRoot = Join-Path $localAppData 'CANVAULT\AndroidSdk'
$commandLineRoot = Join-Path $sdkRoot 'cmdline-tools\bootstrap'
$sdkManager = Join-Path $commandLineRoot 'cmdline-tools\bin\sdkmanager.bat'
$downloadRoot = Join-Path $localAppData 'CANVAULT\Downloads'
$commandLineArchive = Join-Path $downloadRoot 'commandlinetools-win-15859902_latest.zip'

$commandLineUrl = 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip'
$commandLineSha256 = '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a'

if (-not (Test-Path -LiteralPath $sdkManager -PathType Leaf)) {
  Write-Output '1/5 Lade Googles offizielle Android-Werkzeuge (einmalig, ca. 156 MB) ...'
  New-Item -ItemType Directory -Path $downloadRoot,$commandLineRoot -Force | Out-Null
  Invoke-WebRequest -Uri $commandLineUrl -OutFile $commandLineArchive

  $actualHash = (Get-FileHash -LiteralPath $commandLineArchive -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actualHash -ne $commandLineSha256) {
    throw "Die Pruefsumme der Android-Werkzeuge stimmt nicht. Erwartet: $commandLineSha256; erhalten: $actualHash"
  }
  Expand-Archive -LiteralPath $commandLineArchive -DestinationPath $commandLineRoot -Force
}

$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:Path = "$sdkRoot\platform-tools;$env:Path"
# Googles Windows-Batchdatei liest Oracles gueltigen Versionsstring "21" als Zahl 21
# statt 210 und lehnt ihn irrtuemlich als kleiner als Java 17 ab.
$env:SKIP_JDK_VERSION_CHECK = '1'

Write-Output '2/5 Richte Android SDK 36 ein ...'
1..30 | ForEach-Object { 'y' } | & $sdkManager "--sdk_root=$sdkRoot" --licenses | Out-Host
if ($LASTEXITCODE -ne 0) {
  throw "Android-Lizenzen konnten nicht eingerichtet werden (Fehlercode $LASTEXITCODE)."
}

& $sdkManager `
  "--sdk_root=$sdkRoot" `
  'platform-tools' `
  'platforms;android-36' `
  'build-tools;36.0.0'
if ($LASTEXITCODE -ne 0) {
  throw "Android-SDK-Komponenten konnten nicht installiert werden (Fehlercode $LASTEXITCODE)."
}

Write-Output '3/5 Baue die native CANVAULT-App ...'
Push-Location $nativeRoot
try {
  & $gradleWrapper app:assembleRelease --no-daemon
  if ($LASTEXITCODE -ne 0) {
    throw "Der Android-Build ist fehlgeschlagen (Fehlercode $LASTEXITCODE)."
  }
} finally {
  Pop-Location
}

$builtApk = Join-Path $nativeRoot 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path -LiteralPath $builtApk -PathType Leaf)) {
  throw "Der Build war erfolgreich, aber die APK fehlt: $builtApk"
}

$downloads = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'Downloads'
$downloadedApk = Join-Path $downloads 'CANVAULT.apk'
Copy-Item -LiteralPath $builtApk -Destination $downloadedApk -Force
Write-Output "4/5 APK fertig: $downloadedApk"

$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
  throw 'ADB wurde nicht gefunden.'
}

& $adb start-server | Out-Null
Write-Output 'Bitte Handy entsperren und USB-Debugging zulassen. Warte auf das Geraet ...'

$deadline = (Get-Date).AddSeconds(45)
$authorized = @()
$unauthorized = @()
do {
  $deviceLines = @(
    & $adb devices |
      Select-Object -Skip 1 |
      Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
  )
  $authorized = @($deviceLines | Where-Object { $_ -match "\tdevice$" })
  $unauthorized = @($deviceLines | Where-Object { $_ -match "\tunauthorized$" })
  if ($authorized.Count -eq 0) { Start-Sleep -Seconds 2 }
} while ($authorized.Count -eq 0 -and (Get-Date) -lt $deadline)

if ($authorized.Count -eq 0) {
  $hint = if ($unauthorized.Count -gt 0) {
    'Die USB-Abfrage auf dem entsperrten Handy bestaetigen und den Befehl erneut starten.'
  } else {
    'USB-Kabel, USB-Modus und die Entwickleroption USB-Debugging pruefen.'
  }
  throw "Die APK ist fertig, aber kein freigegebenes Handy wurde erkannt. $hint APK: $downloadedApk"
}

if ($Serial) {
  $selectedSerial = $Serial
  $knownSerials = @($authorized | ForEach-Object { ($_ -split '\s+')[0] })
  if ($selectedSerial -notin $knownSerials) {
    throw "Geraet '$selectedSerial' nicht gefunden. Verfuegbar: $($knownSerials -join ', ')"
  }
} elseif ($authorized.Count -eq 1) {
  $selectedSerial = ($authorized[0] -split '\s+')[0]
} else {
  $knownSerials = @($authorized | ForEach-Object { ($_ -split '\s+')[0] })
  throw "Mehrere Geraete erkannt. Befehl mit -Serial wiederholen. Verfuegbar: $($knownSerials -join ', ')"
}

Write-Output "5/5 Installiere CANVAULT auf $selectedSerial ..."
$installOutput = @(& $adb -s $selectedSerial install -r $downloadedApk 2>&1)
$installOutput | Out-Host
if ($LASTEXITCODE -ne 0) {
  if (($installOutput -join "`n") -match 'INSTALL_FAILED_USER_RESTRICTED') {
    Write-Output 'HyperOS/Android blockiert Install via USB. Kopiere APK in den Handy-Downloadordner ...'
    & $adb -s $selectedSerial push $downloadedApk '/sdcard/Download/CANVAULT.apk' | Out-Host
    & $adb -s $selectedSerial shell am start -n 'com.mi.android.globalFileexplorer/com.android.fileexplorer.FileExplorerTabActivity' | Out-Host
    throw 'CANVAULT.apk liegt auf dem Handy unter Downloads. Dort die Datei antippen und Installieren bestaetigen.'
  }
  throw "Die APK-Installation ist fehlgeschlagen (Fehlercode $LASTEXITCODE). APK: $downloadedApk"
}

& $adb -s $selectedSerial shell am start -n 'com.canvault.app/.MainActivity' | Out-Host
if ($LASTEXITCODE -ne 0) {
  throw 'CANVAULT wurde installiert, konnte aber nicht automatisch gestartet werden.'
}

Write-Output 'FERTIG: CANVAULT wurde installiert und auf dem Handy gestartet.'
