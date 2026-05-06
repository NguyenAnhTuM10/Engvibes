# ============================================================
#  run-backend.ps1 - Start Engvibes backend (chay tu root)
#  Usage:  .\run-backend.ps1          (build neu chua co JAR)
#          .\run-backend.ps1 -Build   (force rebuild truoc khi chay)
# ============================================================
param([switch]$Build)

$root    = $PSScriptRoot
$backend = "$root\backend"
$jar     = "$backend\build\libs\backend-0.0.1-SNAPSHOT.jar"

function Write-Step($msg) { Write-Host "" ; Write-Host ">> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "   [OK] $msg" -ForegroundColor Green }
function Write-Err($msg)  { Write-Host "   [!!] $msg" -ForegroundColor Red }

# 1. Docker services
Write-Step "Kiem tra Docker services..."
$dockerUp = docker compose -f "$root\docker-compose.yml" ps --services --filter status=running 2>$null
$needed   = @("postgres","redis","minio")
$missing  = $needed | Where-Object { $dockerUp -notcontains $_ }

if ($missing.Count -gt 0) {
    Write-Host "   Chua co: $($missing -join ', ') - dang start..." -ForegroundColor Yellow
    docker compose -f "$root\docker-compose.yml" up -d
    Write-Host "   Cho services healthy..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
} else {
    Write-Ok "Postgres, Redis, MinIO dang chay"
}

# 2. Build neu can
if ($Build -or (-not (Test-Path $jar))) {
    if (-not (Test-Path $jar)) {
        Write-Step "Chua co JAR - dang build..."
    } else {
        Write-Step "Force rebuild (-Build flag)..."
    }
    Push-Location $backend
    .\gradlew.bat build -x test
    $buildOk = ($LASTEXITCODE -eq 0)
    Pop-Location
    if (-not $buildOk) {
        Write-Err "Build that bai. Xem log o tren."
        exit 1
    }
    Write-Ok "Build thanh cong"
} else {
    Write-Ok "Dung JAR cu: $jar"
}

# 3. Kill process dang chiem port 8080
$pid8080 = (netstat -ano | Select-String ":8080 " | Select-String "LISTENING" |
            ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
if ($pid8080) {
    Write-Step "Port 8080 bi chiem (PID $pid8080) - dang kill..."
    Stop-Process -Id $pid8080 -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
    Write-Ok "Da giai phong port 8080"
}

# 4. Start backend
Write-Step "Khoi dong backend (profile=local)..."
Write-Host "   Swagger : http://localhost:8080/swagger-ui.html" -ForegroundColor DarkGray
Write-Host "   Health  : http://localhost:8080/api/health" -ForegroundColor DarkGray
Write-Host "   (Ctrl+C de dung)" -ForegroundColor DarkGray
Write-Host ""

java -jar $jar --spring.profiles.active=local