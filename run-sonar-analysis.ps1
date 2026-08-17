# PowerShell script to run SonarQube analysis on backend project
Param(
    [string]$SonarHost = "http://localhost:9000",
    [string]$SonarToken = ""
)

$backendDir = Join-Path $PSScriptRoot "restaurant-qr-menu-backend"
Write-Host "Navigating to $backendDir..." -ForegroundColor Cyan
Set-Location -Path $backendDir

$cmd = "mvn clean verify sonar:sonar '-Dsonar.host.url=$SonarHost' '-Dsonar.projectKey=restaurant-qr-backend' '-Dsonar.projectName=Restaurant QR Menu Backend' '-Dmaven.test.failure.ignore=true'"

if ($SonarToken) {
    $cmd += " '-Dsonar.token=$SonarToken'"
}

Write-Host "Running SonarQube Analysis command:" -ForegroundColor Yellow
Write-Host $cmd -ForegroundColor Gray

Invoke-Expression $cmd
