[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. "$scriptDir\common.ps1"

$projectRoot = (Resolve-Path "$scriptDir\..").Path
$prodEnvFile = Join-Path $projectRoot ".env.prod"

if (-not (Test-Path $prodEnvFile)) {
    Write-Host "==========================================" -ForegroundColor Red
    Write-Host " [BLOCKED] PROD Startup Blocked" -ForegroundColor Red
    Write-Host "==========================================" -ForegroundColor Red
    Write-Host "Production configuration file '.env.prod' does not exist." -ForegroundColor Yellow
    Write-Host "Dedicated PROD database user (caresync_prod_user) is not yet configured." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Pre-requisites for production launch:" -ForegroundColor Gray
    Write-Host "  1. Provision 'caresync_prod_user' in production MySQL." -ForegroundColor Gray
    Write-Host "  2. Migrate the production schema in 'caresync_db' (ddl-auto: validate)." -ForegroundColor Gray
    Write-Host "  3. Configure production credentials securely in '.env.prod' or environment." -ForegroundColor Gray
    Write-Host "==========================================" -ForegroundColor Red
    exit 1
}

Load-CareSyncEnv -EnvFileName ".env.prod" -TargetProfile "prod" -ExpectedDbName "caresync_db"

Push-Location $projectRoot
try {
    Write-Host "Launching CareSync Backend [PROD Profile]..." -ForegroundColor Cyan
    mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
} finally {
    Pop-Location
}
