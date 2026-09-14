[CmdletBinding()]
param(
    [switch]$TestSuite
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. "$scriptDir\common.ps1"

Load-CareSyncEnv -EnvFileName ".env.test" -TargetProfile "test" -ExpectedDbName "caresync_test"

$projectRoot = (Resolve-Path "$scriptDir\..").Path
Push-Location $projectRoot

try {
    if ($TestSuite) {
        Write-Host "Running automated test suite with TEST environment..." -ForegroundColor Cyan
        mvn test
    } else {
        Write-Host "Launching CareSync Backend [TEST Profile]..." -ForegroundColor Cyan
        mvn spring-boot:run "-Dspring-boot.run.profiles=test"
    }
} finally {
    Pop-Location
}
