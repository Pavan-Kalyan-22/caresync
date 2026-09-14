[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. "$scriptDir\common.ps1"

Load-CareSyncEnv -EnvFileName ".env.dev" -TargetProfile "dev" -ExpectedDbName "caresync_dev"

$projectRoot = (Resolve-Path "$scriptDir\..").Path
Push-Location $projectRoot

try {
    Write-Host "Launching CareSync Backend [DEV Profile]..." -ForegroundColor Cyan
    mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
} finally {
    Pop-Location
}
