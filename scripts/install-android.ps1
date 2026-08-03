[CmdletBinding()]
param(
  [Parameter(Mandatory = $false)]
  [string] $ApkPath,

  [Parameter(Mandatory = $false)]
  [string] $Serial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($ApkPath) {
  Write-Output 'Hinweis: -ApkPath wird nicht mehr benoetigt. CANVAULT wird nativ gebaut und automatisch installiert.'
}

$installer = Join-Path $PSScriptRoot 'build-and-install-android.ps1'
if ($Serial) {
  & $installer -AcceptAndroidLicenses -Serial $Serial
} else {
  & $installer -AcceptAndroidLicenses
}
