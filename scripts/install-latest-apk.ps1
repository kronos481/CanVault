[CmdletBinding()]
param(
  [Parameter(Mandatory = $false)]
  [string] $ApkPath,

  [Parameter(Mandatory = $false)]
  [string] $Serial,

  [Parameter(Mandatory = $false)]
  [switch] $NoLaunch
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

if ($ApkPath) {
  $resolvedApk = (Resolve-Path -LiteralPath $ApkPath -ErrorAction Stop).Path
} else {
  $releaseCandidates = @(
    Get-ChildItem -LiteralPath $projectRoot -Filter 'CANVAULT-*.apk' -File |
      Sort-Object LastWriteTime -Descending
  )
  $builtApk = Join-Path $projectRoot 'native-android\app\build\outputs\apk\release\app-release.apk'

  if ($releaseCandidates.Count -gt 0) {
    $resolvedApk = $releaseCandidates[0].FullName
  } elseif (Test-Path -LiteralPath $builtApk -PathType Leaf) {
    $resolvedApk = $builtApk
  } else {
    throw "Keine CANVAULT-APK gefunden. Lege die APK in $projectRoot oder nutze -ApkPath."
  }
}

if (-not (Test-Path -LiteralPath $resolvedApk -PathType Leaf)) {
  throw "APK nicht gefunden: $resolvedApk"
}

$localAppData = [Environment]::GetFolderPath('LocalApplicationData')
$adbCandidates = @(
  $(if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' })
  $(if ($env:ANDROID_SDK_ROOT) { Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' })
  (Join-Path $localAppData 'CANVAULT\AndroidSdk\platform-tools\adb.exe')
  (Join-Path $localAppData 'Android\Sdk\platform-tools\adb.exe')
)
$adb = $adbCandidates |
  Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) } |
  Select-Object -First 1

if (-not $adb) {
  throw 'ADB wurde nicht gefunden. Starte einmal scripts\build-and-install-android.ps1 oder installiere Android Platform Tools.'
}

Write-Output "APK: $resolvedApk"
Write-Output 'Pruefe angeschlossene Android-Geraete ...'
& $adb start-server | Out-Null

$deviceLines = @(& $adb devices -l | Select-Object -Skip 1)
$authorized = @(
  foreach ($line in $deviceLines) {
    if ($line -match '^\s*(\S+)\s+device(?:\s|$)') {
      $deviceSerial = $Matches[1]
      $model = if ($line -match 'model:(\S+)') { $Matches[1] } else { 'Android' }
      [PSCustomObject]@{
        Serial = $deviceSerial
        Model = $model
        IsEmulator = $deviceSerial -match '^emulator-'
      }
    }
  }
)

if ($authorized.Count -eq 0) {
  if ($deviceLines -match '\sunauthorized(?:\s|$)') {
    throw 'Handy erkannt, aber nicht freigegeben. Handy entsperren und die USB-Debugging-Abfrage bestaetigen.'
  }
  throw 'Kein freigegebenes Android-Handy erkannt. USB-Debugging aktivieren und das USB-Kabel pruefen.'
}

if ($Serial) {
  $selected = $authorized | Where-Object Serial -eq $Serial | Select-Object -First 1
  if (-not $selected) {
    throw "Geraet '$Serial' ist nicht verfuegbar. Erkannt: $($authorized.Serial -join ', ')"
  }
} else {
  $physicalPhones = @($authorized | Where-Object { -not $_.IsEmulator })
  if ($physicalPhones.Count -eq 1) {
    $selected = $physicalPhones[0]
  } elseif ($physicalPhones.Count -gt 1) {
    $choices = $physicalPhones | ForEach-Object { "$($_.Serial) ($($_.Model))" }
    throw "Mehrere echte Handys erkannt. Skript erneut mit -Serial starten: $($choices -join ', ')"
  } elseif ($authorized.Count -eq 1) {
    $selected = $authorized[0]
  } else {
    throw "Mehrere Emulatoren erkannt. Skript erneut mit -Serial starten: $($authorized.Serial -join ', ')"
  }
}

Write-Output "Installiere CANVAULT auf $($selected.Model) [$($selected.Serial)] ..."
$installOutput = @(& $adb -s $selected.Serial install -r $resolvedApk 2>&1)
$installExitCode = $LASTEXITCODE
$installOutput | Out-Host

if ($installExitCode -ne 0) {
  if (($installOutput -join "`n") -match 'INSTALL_FAILED_USER_RESTRICTED') {
    $phoneApk = '/sdcard/Download/CANVAULT.apk'
    & $adb -s $selected.Serial push $resolvedApk $phoneApk | Out-Host
    throw 'Android hat die USB-Installation blockiert. CANVAULT.apk wurde deshalb in den Download-Ordner des Handys kopiert; dort antippen und installieren.'
  }
  throw "Installation fehlgeschlagen (ADB-Code $installExitCode)."
}

if (-not $NoLaunch) {
  & $adb -s $selected.Serial shell am start -n 'com.canvault.app/.MainActivity' | Out-Host
  if ($LASTEXITCODE -ne 0) {
    throw 'Die App wurde installiert, konnte aber nicht automatisch gestartet werden.'
  }
}

Write-Output 'FERTIG: CANVAULT wurde aktualisiert, bestehende App-Daten bleiben erhalten.'
