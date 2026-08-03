Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Write-Output 'CANVAULT Native Android Installer'
Write-Output 'Beim Fortfahren werden die offiziellen Android-SDK-Lizenzen akzeptiert.'

& (Join-Path $PSScriptRoot 'scripts\build-and-install-android.ps1') -AcceptAndroidLicenses
