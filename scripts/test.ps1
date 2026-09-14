[CmdletBinding()]
param()

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& "$scriptDir\run-test.ps1" -TestSuite
