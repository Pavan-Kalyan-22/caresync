function Load-CareSyncEnv {
    param (
        [Parameter(Mandatory = $true)]
        [string]$EnvFileName,

        [Parameter(Mandatory = $true)]
        [string]$TargetProfile,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedDbName
    )

    $projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
    $envFilePath = Join-Path $projectRoot $EnvFileName

    # Check for specific profile file first, fallback to .env if present
    if (-not (Test-Path $envFilePath)) {
        $fallbackPath = Join-Path $projectRoot ".env"
        if (Test-Path $fallbackPath) {
            $envFilePath = $fallbackPath
        }
    }

    if (Test-Path $envFilePath) {
        Get-Content $envFilePath | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#")) {
                $idx = $line.IndexOf('=')
                if ($idx -gt 0) {
                    $key = $line.Substring(0, $idx).Trim()
                    $val = $line.Substring($idx + 1).Trim()
                    # Strip enclosing quotes if present
                    if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                        if ($val.Length -ge 2) {
                            $val = $val.Substring(1, $val.Length - 2)
                        }
                    }
                    [System.Environment]::SetEnvironmentVariable($key, $val, 'Process')
                }
            }
        }
    }

    # Verify mandatory environment variables
    $missing = @()
    if (-not [System.Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_URL', 'Process')) { $missing += 'SPRING_DATASOURCE_URL' }
    if (-not [System.Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_USERNAME', 'Process')) { $missing += 'SPRING_DATASOURCE_USERNAME' }
    if (-not [System.Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_PASSWORD', 'Process')) { $missing += 'SPRING_DATASOURCE_PASSWORD' }
    if (-not [System.Environment]::GetEnvironmentVariable('JWT_SECRET', 'Process')) { $missing += 'JWT_SECRET' }

    if ($missing.Count -gt 0) {
        Write-Host "==========================================" -ForegroundColor Red
        Write-Host " [ERROR] Missing Required Configuration" -ForegroundColor Red
        Write-Host "==========================================" -ForegroundColor Red
        Write-Host "The following required variable(s) for profile '$TargetProfile' are missing:" -ForegroundColor Red
        $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
        Write-Host ""
        Write-Host "Resolution:" -ForegroundColor Yellow
        Write-Host "  Create local file: $projectRoot\$EnvFileName" -ForegroundColor Yellow
        Write-Host "  Copy from template: .env.example" -ForegroundColor Yellow
        Write-Host "  (This file is git-ignored and safe for local credentials)" -ForegroundColor Gray
        Write-Host "==========================================" -ForegroundColor Red
        exit 1
    }

    # Guardrail: Check database URL matches intended database to prevent cross-environment accidents
    $currentUrl = [System.Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_URL', 'Process')
    if (-not ($currentUrl -match "/$ExpectedDbName(\?|$)")) {
        Write-Host "==========================================" -ForegroundColor Red
        Write-Host " [FATAL ERROR] Profile Database Mismatch!" -ForegroundColor Red
        Write-Host "==========================================" -ForegroundColor Red
        Write-Host "Profile '$TargetProfile' is configured to only connect to: '$ExpectedDbName'" -ForegroundColor Red
        Write-Host "Current SPRING_DATASOURCE_URL is: '$currentUrl'" -ForegroundColor Yellow
        Write-Host "Execution aborted to prevent accidental database corruption!" -ForegroundColor Red
        Write-Host "==========================================" -ForegroundColor Red
        exit 1
    }

    # Safe summary - NEVER print passwords or JWT secrets
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host " CareSync Backend Startup [$TargetProfile]" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host " Profile        : $TargetProfile" -ForegroundColor Green
    Write-Host " Database Target: $ExpectedDbName" -ForegroundColor Green
    Write-Host " Database URL   : $currentUrl" -ForegroundColor Gray
    Write-Host " Database User  : $([System.Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_USERNAME', 'Process'))" -ForegroundColor Gray
    Write-Host " Datasource Pwd : [CONFIGURED - HIDDEN]" -ForegroundColor Gray
    Write-Host " JWT Secret     : [CONFIGURED - HIDDEN]" -ForegroundColor Gray
    Write-Host " Config Source  : $(if (Test-Path $envFilePath) { Split-Path $envFilePath -Leaf } else { 'Process Environment' })" -ForegroundColor Gray
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""

    # Always enforce the active profile
    [System.Environment]::SetEnvironmentVariable('SPRING_PROFILES_ACTIVE', $TargetProfile, 'Process')
}
