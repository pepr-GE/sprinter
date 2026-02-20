# restart.ps1 – Sestaví a restartuje aplikaci Sprinter
# Použití: .\restart.ps1           (build + restart)
#          .\restart.ps1 -SkipBuild (pouze restart bez buildu)

param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
$Port       = 8090
$War        = "$ProjectDir\target\sprinter.war"

# ── 1. Zastavení běžící instance ──────────────────────────────────────────────
Write-Host ""
Write-Host ">>> Hledám běžící instanci na portu $Port..." -ForegroundColor Cyan

$listener = netstat -ano | Select-String ":$Port\s+\S+\s+LISTENING" | Select-Object -First 1
if ($listener) {
    $targetPid = ($listener -split '\s+' | Where-Object { $_ -match '^\d+$' } | Select-Object -Last 1)
    Write-Host "    Zastavuji proces PID $targetPid..." -ForegroundColor Yellow
    Stop-Process -Id ([int]$targetPid) -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "    Proces zastaven." -ForegroundColor Green
} else {
    Write-Host "    Žádná běžící instance nenalezena." -ForegroundColor Gray
}

# ── 2. Maven build ────────────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host ">>> Sestavuji aplikaci (mvn package -DskipTests)..." -ForegroundColor Cyan
    Push-Location $ProjectDir
    mvn package -DskipTests -q
    $buildResult = $LASTEXITCODE
    Pop-Location
    if ($buildResult -ne 0) {
        Write-Host "    BUILD SELHAL! Spuštění přerušeno." -ForegroundColor Red
        exit 1
    }
    Write-Host "    Build úspěšný." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host ">>> Build přeskočen (-SkipBuild)." -ForegroundColor Gray
}

# ── 3. Spuštění aplikace ──────────────────────────────────────────────────────
if (-not (Test-Path $War)) {
    Write-Host "    WAR soubor nenalezen: $War" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host ">>> Spouštím Sprinter..." -ForegroundColor Cyan
Start-Process -FilePath "java" `
              -ArgumentList "-jar", "`"$War`"" `
              -WorkingDirectory $ProjectDir `
              -WindowStyle Normal

Write-Host "    Aplikace se spouští."
Write-Host "    Dostupná na: http://localhost:$Port/sprinter" -ForegroundColor Green
Write-Host ""
