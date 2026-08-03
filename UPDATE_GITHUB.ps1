[CmdletBinding()]
param(
    [string] $CommitMessage = "Release CANVAULT 1.9.3",
    [switch] $SkipTag
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

if (-not (Get-Command git.exe -ErrorAction SilentlyContinue)) {
    throw "Git wurde nicht gefunden. Installiere Git for Windows und starte das Script erneut."
}

if (-not (Test-Path -LiteralPath ".git" -PathType Container)) {
    git init
    if ($LASTEXITCODE -ne 0) { throw "Git-Repository konnte nicht initialisiert werden." }
}

git branch -M main
$repositoryUrl = "https://github.com/kronos481/CanVault.git"
$remotes = @(& git.exe remote)
if ($remotes -contains "origin") {
    git remote set-url origin $repositoryUrl
} else {
    git remote add origin $repositoryUrl
}

git add .
if ($LASTEXITCODE -ne 0) { throw "Dateien konnten nicht vorgemerkt werden." }

$pendingChanges = @(& git.exe status --porcelain)
if ($pendingChanges.Count -gt 0) {
    git commit -m $CommitMessage
    if ($LASTEXITCODE -ne 0) { throw "Commit fehlgeschlagen. Prüfe git config user.name und user.email." }
} else {
    Write-Host "Keine neuen lokalen Änderungen zum Committen."
}

git fetch origin
if ($LASTEXITCODE -ne 0) { throw "GitHub konnte nicht abgerufen werden. Prüfe Internet und Anmeldung." }

git pull --rebase origin main
if ($LASTEXITCODE -ne 0) { throw "Remote-Änderungen konnten nicht automatisch integriert werden. Löse den angezeigten Konflikt und starte erneut." }

git push -u origin main
if ($LASTEXITCODE -ne 0) { throw "Push zu GitHub ist fehlgeschlagen." }

if (-not $SkipTag) {
    $tag = "v1.9.3"
    $existingTag = @(& git.exe tag --list $tag)
    if ($existingTag.Count -eq 0) {
        git tag -a $tag -m "CANVAULT 1.9.3"
        if ($LASTEXITCODE -ne 0) { throw "Release-Tag konnte nicht erstellt werden." }
    }
    git push origin $tag
    if ($LASTEXITCODE -ne 0) { throw "Release-Tag konnte nicht übertragen werden." }
}

Write-Host ""
Write-Host "FERTIG: Repository und Tag v1.9.3 sind auf GitHub."
Write-Host "Release: https://github.com/kronos481/CanVault/releases/new?tag=v1.9.3"
Write-Host "Release-Datei: CANVAULT-1.9.3.apk"
